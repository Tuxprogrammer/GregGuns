package com.gregguns.worldgen.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.compat.BartWorksCulture;
import com.gregguns.entity.npc.NpcItems;
import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.item.part.ItemTank;
import com.gregguns.item.part.TankMath;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Registers the {@code gregguns:*} structure categories with Forge's {@link ChestGenHooks} and fills inventories from
 * them. Common side, server thread, after every mod's items exist (postInit).
 *
 * <p>
 * The categories are GregGuns' own because TooMuchLoot loads every vanilla category with {@code OVERRIDE}, which
 * wipes anything added from code (GGS-11-19). This class never reads or writes {@link ChestGenHooks#DUNGEON_CHEST}
 * or any other vanilla category. A pack author can still retune ours through TML, so {@link #fill} reads the
 * registered contents, not the {@link LootCategoryDefs} data.
 *
 * <p>
 * Pack items are soft: a line whose item is not in the pack (its mod is missing) is skipped at registration, and a
 * gun whose ammo is missing keeps its line without the companion ({@link #resolvable}).
 *
 * <p>
 * Usage: {@code LootCategories.register()} once in postInit; {@code LootCategories.fill(chest, name, richness, rand)}
 * with the structure's seeded {@code Random} (GGS-11-20).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-19..23, GGS-11-26);
// docs/gregguns-spec/06-items/pack-loot.md (GGS-06-02, GGS-06-03)
public final class LootCategories {

    private static final String GT = "gt:";

    /** Registered content to the line it came from, so {@link #fill} can apply richness weights and companions. */
    private static final Map<WeightedRandomChestContent, LootEntry> SOURCE = new IdentityHashMap<>();
    /**
     * The same lines by value (category, item, damage, count range). TooMuchLoot's {@code /loot reload} rebuilds every
     * content object, so the identity lookup misses afterwards and this one keeps the companions and the top-tier
     * weights.
     */
    private static final Map<String, LootEntry> BY_VALUE = new HashMap<>();
    private static final Map<String, Tier> TOP = new HashMap<>();
    /** Each category's ceiling; {@link #fill} hands out nothing above it or above {@code loot.maxTier} (D364). */
    private static final Map<String, Tier> CEILING = new HashMap<>();
    private static final List<String> SKIPPED = new ArrayList<>();
    private static boolean registered;

    private LootCategories() {}

    /**
     * Registers every category: count range, then one content per line whose item resolves. A line whose item is
     * missing (its mod is not installed, or GT has no such part) is skipped, never added as null (GGS-06-03).
     * Idempotent.
     */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        for (LootCategory cat : LootCategoryDefs.all()) {
            ChestGenHooks info = ChestGenHooks.getInfo(cat.name());
            info.setMin(cat.minItems());
            info.setMax(cat.maxItems());
            TOP.put(cat.name(), LootDraw.topTier(cat));
            CEILING.put(cat.name(), cat.ceiling());
            for (LootEntry line : resolvable(cat, LootCategories::present)) {
                ItemStack stack = stack(line.ref(), 1);
                WeightedRandomChestContent content = new WeightedRandomChestContent(
                    stack,
                    line.drop()
                        .min(),
                    line.drop()
                        .max(),
                    line.weight());
                info.addItem(content);
                SOURCE.put(content, line);
                BY_VALUE.put(valueKey(cat.name(), content), line);
            }
        }
        if (!SKIPPED.isEmpty()) {
            GregGuns.LOG.info(
                "Structure loot skips {} item references this pack does not have (first: {})",
                SKIPPED.size(),
                SKIPPED.get(0));
        }
    }

    /**
     * The lines of a category that can be registered: a line whose item is absent is dropped, and a line whose
     * companion is absent keeps its own item without the companion. Pure apart from the test.
     *
     * @param category a category
     * @param present  whether an item reference resolves in this pack
     * @return the kept lines, in order
     */
    public static List<LootEntry> resolvable(LootCategory category, Predicate<String> present) {
        List<LootEntry> out = new ArrayList<>();
        for (LootEntry line : category.entries()) {
            if (!present.test(line.ref())) {
                continue;
            }
            LootDrop companion = line.companion();
            out.add(companion != null && !present.test(companion.ref()) ? line.withoutCompanion() : line);
        }
        return out;
    }

    private static boolean present(String ref) {
        if (stack(ref, 1) != null) {
            return true;
        }
        synchronized (SKIPPED) {
            if (!SKIPPED.contains(ref)) {
                SKIPPED.add(ref);
                GregGuns.LOG.debug("Structure loot skips {}: not in this pack", ref);
            }
        }
        return false;
    }

    private static String valueKey(String category, WeightedRandomChestContent c) {
        ItemStack s = c.theItemId;
        String name = s == null || s.getItem() == null ? "?"
            : String.valueOf(Item.itemRegistry.getNameForObject(s.getItem()));
        return category + '|'
            + name
            + '|'
            + (s == null ? 0 : s.getItemDamage())
            + '|'
            + c.theMinimumChanceToGenerateItem
            + '|'
            + c.theMaximumChanceToGenerateItem;
    }

    /** @return the references registration skipped because the pack does not have them, for scenarios */
    public static List<String> skipped() {
        synchronized (SKIPPED) {
            return new ArrayList<>(SKIPPED);
        }
    }

    /**
     * Fills an inventory from a category. Same seed, same contents. Richness changes only the pick count and the
     * weight of the category's top-tier lines (GGS-11-23); a content added by someone else (a TML file) keeps its
     * own weight. A gun brings its ammo in the same pick (GGS-11-26). No stack overwrites another.
     *
     * @param inv      the chest
     * @param category a {@code gregguns:*} name
     * @param richness 0..2
     * @param rand     the structure's seeded random
     */
    public static void fill(IInventory inv, String category, int richness, Random rand) {
        ChestGenHooks info = ChestGenHooks.getInfo(category);
        Tier ceilingTier = CEILING.get(category);
        int ceiling = ceilingTier == null ? Integer.MAX_VALUE - 1 : ceilingTier.voltageIndex();
        // A content whose whole crafting chain sits above the cap (or that nobody vouched for, such as a line another
        // mod added) never rolls; it is dropped before the draw so it takes no pick (D364).
        List<WeightedRandomChestContent> base = new ArrayList<>();
        for (WeightedRandomChestContent c : info.getItems(rand)) {
            if (ObtainTier.allowed(c.theItemId, ceiling)) {
                base.add(c);
            }
        }
        WeightedRandomChestContent[] scaled = new WeightedRandomChestContent[base.size()];
        Tier top = TOP.get(category);
        for (int i = 0; i < scaled.length; i++) {
            WeightedRandomChestContent c = base.get(i);
            LootEntry line = SOURCE.get(c);
            if (line == null) {
                line = BY_VALUE.get(valueKey(category, c));
            }
            int weight = line == null || top == null ? c.itemWeight
                : LootDraw.weight(line.withWeight(c.itemWeight), top, richness);
            LootDrop companion = line == null ? null : line.companion();
            scaled[i] = companion == null
                ? new WeightedRandomChestContent(
                    c.theItemId,
                    c.theMinimumChanceToGenerateItem,
                    c.theMaximumChanceToGenerateItem,
                    weight)
                : new WithCompanion(
                    c.theItemId,
                    c.theMinimumChanceToGenerateItem,
                    c.theMaximumChanceToGenerateItem,
                    weight,
                    companion);
        }
        int n = LootDraw.pickCount(info.getMin(), info.getMax(), richness, rand);
        // Forge drops each stack into a random slot and overwrites whatever is there, which loses picks and can
        // separate a gun from its ammo; the wrapper moves such a stack to the next empty slot instead.
        if (scaled.length == 0) {
            return;
        }
        WeightedRandomChestContent.generateChestContents(rand, scaled, new NoOverwrite(inv, ceiling), n);
    }

    /**
     * An inventory view whose {@link #setInventorySlotContents} never overwrites: a stack aimed at an occupied slot
     * goes to the next empty one (wrapping around), and is dropped only when the chest is full. Deterministic, so the
     * same seed still gives the same chest. A stack above the ceiling or {@code loot.maxTier} (a companion, say) is
     * dropped (D364). Everything else passes through.
     */
    static final class NoOverwrite implements IInventory {

        private final IInventory inv;
        private final int ceiling;

        /** An ungated view, for the unit test of the slot logic. */
        NoOverwrite(IInventory inv) {
            this(inv, -1);
        }

        /** @param ceiling the chest's ceiling (voltage index), or -1 for no tier gate */
        NoOverwrite(IInventory inv, int ceiling) {
            this.inv = inv;
            this.ceiling = ceiling;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            if (ceiling >= 0 && stack != null && !ObtainTier.allowed(stack, ceiling)) {
                return;
            }
            int size = inv.getSizeInventory();
            for (int i = 0; i < size; i++) {
                int s = (slot + i) % size;
                if (inv.getStackInSlot(s) == null) {
                    inv.setInventorySlotContents(s, stack);
                    return;
                }
            }
        }

        @Override
        public int getSizeInventory() {
            return inv.getSizeInventory();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inv.getStackInSlot(slot);
        }

        @Override
        public ItemStack decrStackSize(int slot, int count) {
            return inv.decrStackSize(slot, count);
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            return inv.getStackInSlotOnClosing(slot);
        }

        @Override
        public String getInventoryName() {
            return inv.getInventoryName();
        }

        @Override
        public boolean hasCustomInventoryName() {
            return inv.hasCustomInventoryName();
        }

        @Override
        public int getInventoryStackLimit() {
            return inv.getInventoryStackLimit();
        }

        @Override
        public void markDirty() {
            inv.markDirty();
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
            return inv.isUseableByPlayer(player);
        }

        @Override
        public void openInventory() {
            inv.openInventory();
        }

        @Override
        public void closeInventory() {
            inv.closeInventory();
        }

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return inv.isItemValidForSlot(slot, stack);
        }
    }

    /** A chest content that adds its companion stack (a gun's ammo) to whatever it generates. */
    private static final class WithCompanion extends WeightedRandomChestContent {

        private final LootDrop companion;

        WithCompanion(ItemStack stack, int min, int max, int weight, LootDrop companion) {
            super(stack, min, max, weight);
            this.companion = companion;
        }

        @Override
        protected ItemStack[] generateChestContent(Random random, IInventory inventory) {
            ItemStack[] own = super.generateChestContent(random, inventory);
            ItemStack extra = stack(companion.ref(), 1);
            if (extra == null) {
                return own;
            }
            // Same split rule as Forge's own content: a count above the stack limit becomes several stacks.
            ItemStack[] more = ChestGenHooks.generateStacks(random, extra, companion.min(), companion.max());
            ItemStack[] out = new ItemStack[own.length + more.length];
            System.arraycopy(own, 0, out, 0, own.length);
            System.arraycopy(more, 0, out, own.length, more.length);
            return out;
        }
    }

    /**
     * @param ref   a {@link LootEntry} reference
     * @param count the stack size
     * @return the stack, or null when the item is not there (or a {@code ~full} tank cannot be filled)
     */
    public static @Nullable ItemStack stack(String ref, int count) {
        if (ref.endsWith(Arsenal.FULL)) {
            ItemStack tank = stack(ref.substring(0, ref.length() - Arsenal.FULL.length()), count);
            return tank == null ? null : fillTank(tank);
        }
        if (ref.startsWith(GT)) {
            String spec = ref.substring(GT.length());
            int dot = spec.indexOf('.');
            OrePrefixes prefix = OrePrefixes.getPrefix(spec.substring(0, dot));
            Materials material = Materials.get(spec.substring(dot + 1));
            if (prefix == null || material == null || material == Materials._NULL) {
                return null;
            }
            // Source:
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L284
            return GTOreDictUnificator.get(prefix, material, count);
        }
        return NpcItems.stack(ref, count);
    }

    /**
     * Fills a GregGuns tank with its first accepted fluid (Fuel for the Fuel Tank; the E. coli culture, else
     * Biomass, for the Bio Tank), so a tank in loot is ammo and not an empty container.
     *
     * @param tank a tank stack
     * @return the filled stack, or null when it is not a tank or no fluid fits
     */
    private static @Nullable ItemStack fillTank(ItemStack tank) {
        if (!(tank.getItem() instanceof ItemTank)) {
            return null;
        }
        ItemTank item = (ItemTank) tank.getItem();
        List<FluidStack> candidates = new ArrayList<>();
        Fluid culture = BartWorksCulture.eColiFluid();
        if (culture != null) {
            candidates.add(new FluidStack(culture, TankMath.CAPACITY));
        }
        for (String name : item.acceptedMaterials()) {
            Materials m = Materials.get(name);
            if (m == null || m == Materials._NULL) {
                continue;
            }
            FluidStack fluid = m.getFluid(TankMath.CAPACITY);
            if (fluid == null) {
                fluid = m.getGas(TankMath.CAPACITY);
            }
            if (fluid != null) {
                candidates.add(fluid);
            }
        }
        for (FluidStack f : candidates) {
            if (item.fill(tank, f, true) > 0) {
                return tank;
            }
        }
        return null;
    }

    /**
     * @param ref a {@link LootEntry} reference
     * @return the item's own declared tier when it is a GregGuns {@link Tiered}, else null
     */
    public static @Nullable Tier itemTier(String ref) {
        if (ref.startsWith(GT)) {
            return null;
        }
        String bare = ref.endsWith(Arsenal.FULL) ? ref.substring(0, ref.length() - Arsenal.FULL.length()) : ref;
        Item item = NpcItems.item(bare);
        return item instanceof Tiered ? ((Tiered) item).getTier() : null;
    }

    /** @return the category names that were registered, for tests */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        for (LootCategory c : LootCategoryDefs.all()) {
            out.add(c.name());
        }
        return out;
    }
}
