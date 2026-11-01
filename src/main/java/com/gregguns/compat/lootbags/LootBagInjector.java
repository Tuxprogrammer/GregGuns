package com.gregguns.compat.lootbags;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.core.config.CompatConfig;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Adds the GregGuns loot bag groups to EnhancedLootBags' group list at startup, on both sides, so quests can hand out
 * {@code enhancedlootbags:lootbag} with damage 1701 to 1714 and the pack's {@code LootBags.xml} is never edited.
 *
 * <p>
 * Lifecycle (GregGuns loads after ELB, {@code after:enhancedlootbags}):
 * <ol>
 * <li>preInit, after ELB's own preInit read its file: the groups XML (the jar's, or the pack's
 * {@code config/gregguns-lootbags.xml}) is turned into ELB's own objects and appended. A group whose id or name ELB
 * already has is left out: the pack's copy wins. Every client also does this, because ELB draws the icon, the tooltip
 * and NEI from the local list, not from the server's sync.</li>
 * <li>postInit, once every mod has registered its items: drops naming an item that does not exist are removed, so
 * a missing mod costs a drop, not a re-roll.</li>
 * <li>Later, whenever a group is found missing (an admin's {@code /lootbags reload} re-reads only the pack file),
 * {@link #ensurePresent()} puts the same objects back.</li>
 * </ol>
 * With {@code compat.lootBags} off nothing is added, and {@link LootBagGuard} still keeps a GregGuns bag from crashing
 * ELB. Every method is safe to call when ELB is absent or its internals differ: it then does nothing.
 */
// Source:
// https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/EnhancedLootBags.java#L65-L90
// (ELB reads LootBags.xml once, in its preInit)
// Source:
// https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/core/items/ItemLootBag.java#L71-L79
// (registerIcons reads the local list each texture stitch, so a LootbagIcon in the gregguns domain registers)
// Spec: docs/gregguns-spec/06-items/pack-loot.md (EnhancedLootBags)
public final class LootBagInjector {

    private static ElbAccess elb;
    /** The group headers of the source XML, read even when injection is off (the client stubs need them). */
    private static List<LootBagGroups.Group> headers = Collections.emptyList();
    /** ELB group objects GregGuns owns and keeps putting back, in file order. */
    private static final List<Object> OURS = new ArrayList<>();
    private static Item bagItem;
    /** Scenario switch: while true, {@link #ensurePresent()} puts nothing back. */
    private static boolean suspended;

    private LootBagInjector() {}

    /** @return true once ELB is connected (ELB loaded, 1.3.5 internals found) */
    public static boolean active() {
        return elb != null;
    }

    /**
     * Reads the groups and, with {@code compat.lootBags} on, appends them to ELB. Call in preInit, only when ELB is
     * loaded.
     *
     * @param configDir the Forge config folder, where a pack's {@link LootBagGroups#OVERRIDE_FILE} may sit
     */
    public static synchronized void preInit(File configDir) {
        elb = ElbAccess.connect();
        if (elb == null) return;
        String xml = source(configDir);
        if (xml == null) return;
        try {
            headers = LootBagGroups.parse(xml);
            for (String p : LootBagGroups.problems(headers)) {
                GregGuns.LOG.warn("Loot bag groups: {}", p);
            }
            if (!CompatConfig.lootBags) {
                GregGuns.LOG.info("compat.lootBags is off: no GregGuns loot bag groups added to EnhancedLootBags");
                return;
            }
            List<Object> parsed = elb.unmarshal(xml);
            Set<Integer> ids = new HashSet<>();
            Set<String> names = new HashSet<>();
            for (Object g : elb.table(elb.localGroups())) {
                ids.add(elb.id(g));
                names.add(nameOf(g));
            }
            for (Object g : parsed) {
                int id = elb.id(g);
                if (!LootBagGroups.isReservedId(id)) {
                    GregGuns.LOG.error(
                        "Loot bag group {} is outside {}-{}; left out",
                        id,
                        LootBagGroups.FIRST_ID,
                        LootBagGroups.LAST_ID);
                } else if (ids.contains(id) || names.contains(nameOf(g))) {
                    // The pack's LootBags.xml already has it (an adopter pasted our file, or /lootbags addloot saved
                    // the groups back): the pack's copy wins, ours stays out. Never change a pack group.
                    GregGuns.LOG
                        .warn("EnhancedLootBags already has group {} ({}); keeping the pack's copy", id, nameOf(g));
                } else {
                    ids.add(id);
                    names.add(nameOf(g));
                    OURS.add(g);
                }
            }
            elb.replaceLocal(OURS, Collections.emptyList());
            GregGuns.LOG.info("Added {} GregGuns loot bag groups to EnhancedLootBags", OURS.size());
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            GregGuns.LOG.error("Could not add the GregGuns loot bag groups to EnhancedLootBags", e);
            OURS.clear();
        }
    }

    private static String nameOf(Object group) throws ReflectiveOperationException {
        return (String) group.getClass()
            .getMethod("getGroupName")
            .invoke(group);
    }

    /** @return the override file's text when present, else the jar's groups; null if neither can be read */
    private static String source(File configDir) {
        File override = configDir == null ? null : new File(configDir, LootBagGroups.OVERRIDE_FILE);
        try {
            if (override != null && override.isFile()) {
                GregGuns.LOG.info("Loot bag groups from {}", override);
                return new String(Files.readAllBytes(override.toPath()), StandardCharsets.UTF_8);
            }
            return LootBagGroups.readResource();
        } catch (IOException e) {
            GregGuns.LOG.error("Could not read the GregGuns loot bag groups", e);
            return null;
        }
    }

    /**
     * Removes every drop of our groups whose item is not registered, and refreshes each group's weight sum. A group
     * left with no drop gets one stack of bread, so its bag still opens. Call in postInit.
     */
    public static synchronized void postInit() {
        if (elb == null) return;
        bagItem = GameRegistry.findItem(LootBagGroups.MOD_ID, "lootbag");
        int removed = 0;
        try {
            for (Object g : OURS) {
                List<Object> drops = elb.drops(g);
                for (int i = drops.size() - 1; i >= 0; i--) {
                    String name = elb.itemName(drops.get(i));
                    if (!LootBagGroups.resolvable(name, LootBagInjector::registered)) {
                        GregGuns.LOG.info("Loot bag group {}: no item {}, drop removed", elb.id(g), name);
                        drops.remove(i);
                        removed++;
                    }
                }
                if (drops.isEmpty()) {
                    drops.add(elb.newDrop("minecraft:bread", "gg_fallback_bread", 8));
                }
                elb.updateMaxWeight(g);
            }
        } catch (ReflectiveOperationException e) {
            GregGuns.LOG.error("Could not check the GregGuns loot bag drops", e);
        }
        if (removed > 0) GregGuns.LOG.info("Loot bag groups: {} drops name missing items", removed);
    }

    private static boolean registered(String mod, String name) {
        return GameRegistry.findItem(mod, name) != null || GameRegistry.findBlock(mod, name) != null;
    }

    /**
     * @param stack a stack, or null
     * @return true if it is an ELB bag whose damage is in the GregGuns range
     */
    public static boolean isGregGunsBag(ItemStack stack) {
        return stack != null && bagItem != null
            && stack.getItem() == bagItem
            && LootBagGroups.isReservedId(stack.getItemDamage());
    }

    /**
     * @param id a group id
     * @return true if ELB's local list has a group with this id
     */
    public static synchronized boolean hasGroup(int id) {
        if (elb == null) return false;
        try {
            for (Object g : elb.table(elb.localGroups())) {
                if (elb.id(g) == id) return true;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.error("Could not read EnhancedLootBags' groups", e);
        }
        return false;
    }

    /**
     * The guard's test. A group with no drops is the client-safe header that {@link #ensureClientSafe()} adds: in
     * single player the client and the server share ELB's list, so the header is in the server's list too, and ELB
     * would answer a use with "try again" instead of opening anything.
     *
     * @param id a group id
     * @return true if ELB's local list has a group with this id that has at least one drop
     */
    public static synchronized boolean canOpen(int id) {
        if (elb == null) return false;
        try {
            for (Object g : elb.table(elb.localGroups())) {
                if (elb.id(g) == id && !elb.drops(g)
                    .isEmpty()) return true;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.error("Could not read EnhancedLootBags' groups", e);
        }
        return false;
    }

    /**
     * Puts back every GregGuns group ELB's local list lost (after {@code /lootbags reload}). Does nothing with
     * {@code compat.lootBags} off.
     *
     * @return true if a group was put back
     */
    public static synchronized boolean ensurePresent() {
        if (elb == null || OURS.isEmpty() || suspended) return false;
        try {
            List<Object> missing = new ArrayList<>();
            for (Object g : OURS) {
                if (!hasGroup(elb.id(g))) missing.add(g);
            }
            if (missing.isEmpty()) return false;
            elb.replaceLocal(missing, Collections.emptyList());
            GregGuns.LOG.info("Put {} GregGuns loot bag groups back into EnhancedLootBags", missing.size());
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.error("Could not put the GregGuns loot bag groups back", e);
            return false;
        }
    }

    /**
     * Server side, after {@code /lootbags reload}: puts our groups back and, if any were missing, sends every player
     * the group headers again (ELB's own resend went out without them).
     */
    public static synchronized void afterReload() {
        if (ensurePresent()) {
            try {
                elb.sendClientUpdate();
            } catch (ReflectiveOperationException | RuntimeException e) {
                GregGuns.LOG.warn("Could not resend the loot bag groups to clients", e);
            }
        }
    }

    /**
     * Client side, every second: ELB's tooltip reads a bag's group from the local list and crashes when it is absent.
     * With injection on, missing groups are put back; with it off, each GregGuns group the local list lacks gets an
     * empty header (no drops, no trash group), so hovering a bag shows it instead of crashing.
     */
    // Source:
    // https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/core/items/ItemLootBag.java#L270-L273
    // (addInformation: getGroupByID(meta).getCombineWithTrash() with no null check)
    public static synchronized void ensureClientSafe() {
        if (elb == null) return;
        if (!OURS.isEmpty()) {
            ensurePresent();
            return;
        }
        addStubs();
    }

    /** Appends an empty header for each GregGuns group ELB's local list lacks. */
    private static void addStubs() {
        try {
            List<Object> stubs = new ArrayList<>();
            for (LootBagGroups.Group h : headers) {
                if (LootBagGroups.isReservedId(h.id) && !hasGroup(h.id)) {
                    stubs.add(
                        elb.newGroup(
                            h.id,
                            h.name,
                            EnumRarity.values()[Math.max(0, Math.min(3, h.rarity))],
                            h.minItems,
                            false));
                }
            }
            elb.replaceLocal(stubs, Collections.emptyList());
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.error("Could not add loot bag group headers", e);
        }
    }

    // ---- scenario hooks (dev only) ---------------------------------------------------------------------------------

    /** @return the ids of the groups GregGuns added, in file order */
    public static synchronized List<Integer> ourIds() {
        List<Integer> ids = new ArrayList<>();
        try {
            for (Object g : OURS) {
                ids.add(elb.id(g));
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return ids;
    }

    /** @return the item names of every drop now in our groups, with duplicates */
    public static synchronized List<String> ourItemNames() {
        List<String> names = new ArrayList<>();
        try {
            for (Object g : OURS) {
                for (Object d : elb.drops(g)) {
                    names.add(elb.itemName(d));
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return names;
    }

    /**
     * Scenario hook: while suspended, missing groups are not put back, as with {@code compat.lootBags} off.
     *
     * @param value true to suspend
     */
    public static synchronized void suspendForTest(boolean value) {
        suspended = value;
    }

    /**
     * Scenario hook: adds the client-safe empty headers to ELB's local list, as {@link #ensureClientSafe()} does with
     * {@code compat.lootBags} off (in single player that list is the server's too).
     */
    public static synchronized void stubForTest() {
        if (elb != null) addStubs();
    }

    /**
     * Scenario hook: takes every group in the GregGuns id range out of ELB's local list (ours and stubs alike).
     *
     * @return true if the list changed
     */
    public static synchronized boolean removeReservedForTest() {
        if (elb == null) return false;
        try {
            List<Object> reserved = new ArrayList<>();
            for (Object g : elb.table(elb.localGroups())) {
                if (LootBagGroups.isReservedId(elb.id(g))) reserved.add(g);
            }
            return elb.replaceLocal(Collections.emptyList(), reserved);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Scenario hook: takes our groups out of ELB's local list, as a {@code /lootbags reload} does.
     *
     * @return true if the list changed
     */
    public static synchronized boolean removeForTest() {
        try {
            return elb != null && elb.replaceLocal(Collections.emptyList(), new ArrayList<>(OURS));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
