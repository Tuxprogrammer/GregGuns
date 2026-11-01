package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.loot.LootCategory;
import com.gregguns.worldgen.loot.LootEntry;
import com.gregguns.worldgen.loot.LootPending;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers the silo complex loot categories with Forge and fills a chest from a {@link Placement}. Chests fill at
 * generation, from a seed made of the world seed, the complex's cell and the chest's index only, so a chest holds the
 * same items whichever way it is first reached (GGS-11-20). Server side, common.
 *
 * <p>
 * A room chest draws its table's live Forge list, so a TooMuchLoot retune takes part, through {@link RoomLootDraw}:
 * picks by the room's state, junk, spoiled food and worn gear in a ruin. No chest is ever empty: when nothing real
 * could be made, the chest gets {@link RoomLootDraw#FALLBACK}.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Tables, Draw by room state); D315, D316, D317
public final class SiloChests {

    private static boolean registered;
    /** Registered content to the line it came from, for spoil rules and companions. */
    private static final Map<WeightedRandomChestContent, LootEntry> SOURCE = new IdentityHashMap<>();
    /** References registration skipped because this pack does not have them. */
    private static final List<String> SKIPPED = new ArrayList<>();

    private SiloChests() {}

    /**
     * Registers every table and ceiling once, and the junk category, in postInit (GT parts resolve then). A line that
     * does not resolve is skipped and listed in {@link LootCategories#skipped()}.
     */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        for (String key : SiloLootTables.KEYS) {
            for (int c = SiloCeiling.EV; c <= SiloCeiling.UMV; c++) {
                register(SiloLootTables.category(key, c));
            }
        }
        register(SiloLootTables.junk());
    }

    private static void register(LootCategory cat) {
        ChestGenHooks info = ChestGenHooks.getInfo(cat.name());
        info.setMin(cat.minItems());
        info.setMax(cat.maxItems());
        for (LootEntry line : LootCategories.resolvable(cat, SiloChests::present)) {
            ItemStack s = LootCategories.stack(line.ref(), 1);
            WeightedRandomChestContent content = new WeightedRandomChestContent(
                s,
                line.drop()
                    .min(),
                line.drop()
                    .max(),
                line.weight());
            info.addItem(content);
            SOURCE.put(content, line);
        }
    }

    private static boolean present(String ref) {
        if (LootCategories.stack(ref, 1) != null) {
            return true;
        }
        if (!SKIPPED.contains(ref)) {
            SKIPPED.add(ref);
            GregGuns.LOG.debug("Silo loot skips {}: not in this pack", ref);
        }
        return false;
    }

    /** @return the references the silo tables skipped because this pack does not have them, for scenarios */
    public static synchronized List<String> skipped() {
        return new ArrayList<>(SKIPPED);
    }

    /**
     * Fills an inventory for a chest placement: the room's draw, then the bonus items in free slots.
     *
     * @param inv       the chest
     * @param p         a {@link Placement.Kind#CHEST} placement
     * @param worldSeed the world seed
     * @param cellX     the complex's grid cell X
     * @param cellZ     the complex's grid cell Z
     */
    public static void fill(IInventory inv, Placement p, long worldSeed, int cellX, int cellZ) {
        if (LootKeys.ARMORY_AMMO.equals(p.key())) {
            fillDisplay(inv, p, worldSeed, cellX, cellZ);
            return;
        }
        Random rand = new LootPending(
            p.category(),
            p.richness(),
            LootPending.seed(worldSeed, cellX, cellZ, p.chestIndex())).random();
        fillRoom(inv, p, rand);
        List<Integer> free = free(inv);
        for (Placement.Bonus b : p.bonuses()) {
            ItemStack s = SiloBonusItems.stack(b, p);
            if (s == null || free.isEmpty()) {
                continue;
            }
            inv.setInventorySlotContents(free.remove(rand.nextInt(free.size())), s);
        }
    }

    /**
     * @param p a chest placement
     * @return the room state the chest draws by: its own, or, for a chest placed without one, the state its richness
     *         stands for
     */
    static RoomState stateOf(Placement p) {
        if (p.roomState() != null) {
            return p.roomState();
        }
        return p.richness() >= 2 ? RoomState.STAFFED : p.richness() == 1 ? RoomState.IN_SERVICE : RoomState.GUTTED;
    }

    /** The draw of a room chest, made into stacks. */
    private static void fillRoom(IInventory inv, Placement p, Random rand) {
        fillRoom(inv, p.category(), p.key(), stateOf(p), rand);
    }

    /**
     * The draw of a room chest of a category, table key and state into any inventory (a chest, or a shelf's slots,
     * D323); it stops when the inventory is full and never leaves it without a real find.
     *
     * @param inv      the inventory
     * @param category the chest category ({@link SiloLootTables#categoryName})
     * @param key      the table key
     * @param state    the room's state
     * @param rand     the draw's random
     */
    static void fillRoom(IInventory inv, String category, String key, RoomState state, Random rand) {
        WeightedRandomChestContent[] table = ChestGenHooks.getInfo(category)
            .getItems(rand);
        WeightedRandomChestContent[] junk = ChestGenHooks.getInfo(SiloLootTables.JUNK)
            .getItems(rand);
        List<RoomLootDraw.Pick> picks = RoomLootDraw
            .draw(lines(table), lines(junk), state, RoomLoot.pickOffset(key), rand);
        boolean real = false;
        for (RoomLootDraw.Pick pick : picks) {
            ItemStack s = make(pick, pick.junk() ? junk : table);
            if (s != null && put(inv, s, rand) && pick.real()) {
                real = true;
            }
        }
        if (!real) {
            ItemStack torch = LootCategories.stack(
                RoomLootDraw.FALLBACK.ref(),
                RoomLootDraw.FALLBACK.drop()
                    .max());
            if (torch != null) {
                put(inv, torch, rand);
            }
            GregGuns.LOG.debug("Silo chest {} drew nothing real; it holds the fallback", category);
        }
    }

    /** The draw's view of a live Forge list: our lines as registered, anything a pack added at its own weight. */
    private static List<LootEntry> lines(WeightedRandomChestContent[] contents) {
        List<LootEntry> out = new ArrayList<>(contents.length);
        for (WeightedRandomChestContent c : contents) {
            LootEntry line = SOURCE.get(c);
            if (line != null) {
                out.add(line.withWeight(c.itemWeight));
            } else {
                out.add(
                    new LootEntry(
                        LootDrop.range(
                            "pack:" + System.identityHashCode(c),
                            Math.max(1, c.theMinimumChanceToGenerateItem),
                            Math.max(1, c.theMaximumChanceToGenerateItem)),
                        Math.max(1, c.itemWeight),
                        Tier.STEAM,
                        "pack",
                        null));
            }
        }
        return out;
    }

    /**
     * @return the stack a pick stands for: a companion or a spoil item by its reference, anything else as a copy of
     *         its registered content; null when it cannot be made this run
     */
    private static ItemStack make(RoomLootDraw.Pick pick, WeightedRandomChestContent[] contents) {
        ItemStack s = null;
        if (pick.companion() || pick.spoiled()) {
            s = LootCategories.stack(pick.ref(), 1);
        }
        if (s == null && !pick.companion() && pick.index() >= 0 && pick.index() < contents.length) {
            ItemStack template = contents[pick.index()].theItemId;
            s = template == null ? null : template.copy();
        }
        if (s == null && pick.index() < 0) {
            s = LootCategories.stack(pick.ref(), 1);
        }
        if (s == null) {
            return null;
        }
        s.stackSize = Math.max(1, Math.min(pick.count(), s.getMaxStackSize()));
        if (pick.wear() > 0 && wears(s)) {
            s.setItemDamage(Math.min(s.getMaxDamage() - 1, (int) (s.getMaxDamage() * pick.wear())));
        }
        return s;
    }

    /** Puts a stack in a random free slot; false when the chest is full. Never overwrites. */
    private static boolean put(IInventory inv, ItemStack s, Random rand) {
        List<Integer> free = free(inv);
        if (free.isEmpty()) {
            return false;
        }
        inv.setInventorySlotContents(free.get(rand.nextInt(free.size())), s);
        return true;
    }

    private static List<Integer> free(IInventory inv) {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            if (inv.getStackInSlot(i) == null) {
                free.add(i);
            }
        }
        return free;
    }

    /**
     * Fills the chest under an armory display gun with that gun's ammo ({@link ArmoryDisplay#stockFor}), one stack per
     * line in random slots. An energy gun, which burns no ammo, gets a draw of the armory table instead.
     */
    private static void fillDisplay(IInventory inv, Placement p, long worldSeed, int cellX, int cellZ) {
        Random rand = new LootPending(
            p.category(),
            p.richness(),
            LootPending.seed(worldSeed, cellX, cellZ, p.chestIndex())).random();
        String gun = ArmoryDisplay.gunAt(p.ceiling(), worldSeed ^ (cellX * 31L + cellZ), p.x(), p.z());
        List<ArmoryDisplay.Stock> stock = ArmoryDisplay.stockFor(gun);
        if (stock.isEmpty()) {
            fillRoom(inv, p, rand);
            return;
        }
        List<Integer> free = free(inv);
        boolean any = false;
        for (ArmoryDisplay.Stock s : stock) {
            Item item = GameRegistry.findItem(GregGuns.MODID, s.item());
            if (item == null || free.isEmpty()) {
                continue;
            }
            int n = s.min() + rand.nextInt(s.max() - s.min() + 1);
            inv.setInventorySlotContents(
                free.remove(rand.nextInt(free.size())),
                new ItemStack(item, Math.min(n, item.getItemStackLimit())));
            any = true;
        }
        if (!any) {
            fillRoom(inv, p, rand); // the ammo item is missing this run: never an empty chest
        }
    }

    /**
     * @param s a stack
     * @return whether the stack is gear that wears in a ruin (D317): armor, tools and blades. Guns and magazines keep
     *         their damage value for their own use.
     */
    public static boolean wears(ItemStack s) {
        net.minecraft.item.Item i = s.getItem();
        if (!s.isItemStackDamageable()) {
            return false;
        }
        if (i instanceof net.minecraft.item.ItemArmor || i instanceof net.minecraft.item.ItemTool
            || i instanceof net.minecraft.item.ItemSword
            || i instanceof net.minecraft.item.ItemHoe
            || i instanceof net.minecraft.item.ItemShears
            || i instanceof net.minecraft.item.ItemFlintAndSteel) {
            return true;
        }
        String n = Item.itemRegistry.getNameForObject(i);
        return "gregguns:crowbar".equals(n) || "gregguns:combat_knife".equals(n);
    }

    /**
     * @param s a stack
     * @return its registry name and meta; a damageable item's wear is not part of it (a ruin's gear is worn)
     */
    public static String itemId(ItemStack s) {
        return Item.itemRegistry.getNameForObject(s.getItem()) + "@" + (wears(s) ? 0 : s.getItemDamage());
    }

    /**
     * @param key     a table key
     * @param ceiling the ceiling
     * @return the {@link #itemId}s a chest of the table may hold ({@link SiloLootTables#chestRefs}), bonuses aside
     */
    public static java.util.Set<String> allowedIds(String key, int ceiling) {
        java.util.Set<String> out = new java.util.HashSet<>();
        for (String ref : SiloLootTables.chestRefs(key, ceiling)) {
            ItemStack s = LootCategories.stack(ref, 1);
            if (s != null) {
                out.add(itemId(s));
            }
        }
        return out;
    }

    /**
     * @param p         a chest placement
     * @param worldSeed the world seed
     * @param cellX     cell X
     * @param cellZ     cell Z
     * @return a new vanilla chest tile, filled
     */
    public static TileEntityChest filledChest(Placement p, long worldSeed, int cellX, int cellZ) {
        TileEntityChest chest = new TileEntityChest();
        fill(chest, p, worldSeed, cellX, cellZ);
        return chest;
    }
}
