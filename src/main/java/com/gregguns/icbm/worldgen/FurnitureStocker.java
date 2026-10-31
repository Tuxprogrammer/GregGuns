package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.loot.LootEntry;
import com.gregguns.worldgen.loot.LootPending;
import com.gregguns.worldgen.loot.LootPools;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Makes a {@link Placement.Kind#STOCK} into items in the piece's tile entity (D323, D324): BiblioCraft's shelves,
 * racks, armor stands, desks, typewriters, clipboards, plates and cookie jars through their inventories, and the
 * vanilla furnace, brewing stand and flower pot. A piece holds what its slots are for: books on a bookcase, potions on
 * a medicine shelf, armor of the right slot on a stand, food on a plate, a typed note in a typewriter. The draw is
 * seeded from the world seed, the complex's cell and the placement's own index, so a piece holds the same whichever
 * way it is reached. Never writes a job in progress. A piece whose tile is missing (its mod is absent) is left alone.
 * Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Furniture stock); D323, D324
// Decompiled: BiblioCraft[v1.11.7][MC1.7.10].jar (GTNH 2.9.0-RC-2; no 1.7.10 source exists, and its copyright.txt
// allows decompiling for private use). Inferred from decompiled bytecode, unverified, and may change when the pack
// updates BiblioCraft. Facts only, no code: inventory sizes and slot uses (bookcase 16, shelf 4, potion shelf 12, tool
// rack 4, armor stand 4 by armor slot 0 helmet to 3 boots, writing desk 10 with slot 0 the open book, typewriter 2
// with slot 0 paper and slot 1 the finished book, clipboard 1, table 3 with slot 0 the item shown, dinner plate 3
// food, cookie jar 8), and the clipboard item "item.BiblioClipboard" with its page NBT ("page1" holding "title",
// "tasks" with "task1".."task9", and "taskStates", nine ints: 0 open, 1 done, 2 crossed out; "currentPage",
// "totalPages").
public final class FurnitureStocker {

    /** BiblioCraft's clipboard item, by registry name. */
    static final String CLIPBOARD_ITEM = "item.BiblioClipboard";

    private FurnitureStocker() {}

    /**
     * @param te        the piece's tile entity
     * @param p         a {@link Placement.Kind#STOCK} placement
     * @param worldSeed the world seed
     * @param cellX     the complex's grid cell X
     * @param cellZ     the complex's grid cell Z
     * @param ceiling   the complex's ceiling
     * @return whether anything was put in
     */
    public static boolean fill(TileEntity te, Placement p, long worldSeed, int cellX, int cellZ, int ceiling) {
        if (te == null) return false;
        Random rand = new Random(LootPending.seed(worldSeed, cellX, cellZ, 0x51C0000 ^ p.chestIndex()));
        RoomState state = p.roomState() == null ? RoomState.STAFFED : p.roomState();
        RoomLoot.Group group = RoomLoot.row(p.roomKind())
            .group();
        long h = rand.nextLong();
        String source = p.key();
        if (FurnitureStock.PLANT.equals(source)) return plant(te, state, rand);
        if (!(te instanceof IInventory)) return false;
        IInventory inv = (IInventory) te;
        int n = Math.max(0, p.variant());
        switch (source) {
            case FurnitureStock.ROOM:
                return room(inv, p, state, rand);
            case FurnitureStock.BOOKS:
                // Notes and manuals, a few plain books among them.
                for (int i = 0; i < n && i < inv.getSizeInventory(); i++) {
                    inv.setInventorySlotContents(
                        i,
                        rand.nextInt(10) < 6 ? book(ComplexWriting.bookKey(state, group, rand.nextLong()))
                            : plainBook(rand));
                }
                return n > 0;
            case FurnitureStock.MEDICINE:
                return spread(inv, n, rand, () -> medicine(state, rand));
            case FurnitureStock.WEAPONS:
                return spread(inv, n, rand, () -> wear(draw(weapons(ceiling), rand, false), state, rand));
            case FurnitureStock.TOOLS:
                return spread(
                    inv,
                    n,
                    rand,
                    () -> wear(draw(pool(SiloPools.tools(), ceiling), rand, false), state, rand));
            case FurnitureStock.ARMOR_COMBAT:
                return armor(inv, pool(SiloPools.combatArmor(), ceiling), n, state, rand);
            case FurnitureStock.ARMOR_HAZMAT:
                return armor(inv, pool(SiloPools.hazmat(), ceiling), n, state, rand);
            case FurnitureStock.ARMOR_CREW:
                return armor(inv, pool(SiloPools.bunkarmor(), ceiling), n, state, rand);
            case FurnitureStock.DESK:
                return desk(inv, n, state, group, rand);
            case FurnitureStock.TYPEWRITER:
                return typewriter(inv, state, group, rand);
            case FurnitureStock.CLIPBOARD:
                return clipboard(inv, state, group, h);
            case FurnitureStock.PLATE:
                return spread(inv, n, rand, () -> food(state, rand));
            case FurnitureStock.COOKIES:
                for (int i = 0; i < n && i < inv.getSizeInventory(); i++) {
                    inv.setInventorySlotContents(i, new ItemStack(Items.cookie, 1 + rand.nextInt(4)));
                }
                return n > 0;
            case FurnitureStock.FURNACE:
                return furnace(inv, p.roomKind(), state, rand);
            case FurnitureStock.BREWING:
                return brewing(inv, n, state, rand);
            default:
                return false;
        }
    }

    private interface Maker {

        ItemStack make();
    }

    /** Puts up to {@code n} made stacks into distinct random slots. */
    private static boolean spread(IInventory inv, int n, Random rand, Maker maker) {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < inv.getSizeInventory(); i++) free.add(i);
        boolean any = false;
        for (int i = 0; i < n && !free.isEmpty(); i++) {
            ItemStack s = maker.make();
            if (s == null) continue;
            inv.setInventorySlotContents(free.remove(rand.nextInt(free.size())), s);
            any = true;
        }
        return any;
    }

    /**
     * A shelf or a table draws the room's chest table, as a chest of its state would (D316, D317), into its own slots;
     * a table shows one thing.
     */
    private static boolean room(IInventory inv, Placement p, RoomState state, Random rand) {
        boolean table = FurniturePiece.WOOD_TABLE.name()
            .equals(p.block());
        int size = table ? 1 : Math.min(4, inv.getSizeInventory());
        InventoryBasic tmp = new InventoryBasic("stock", false, size);
        SiloChests.fillRoom(tmp, p.category(), p.extra(), state, rand);
        boolean any = false;
        for (int i = 0; i < size; i++) {
            ItemStack s = tmp.getStackInSlot(i);
            if (s == null) continue;
            inv.setInventorySlotContents(i, s);
            any = true;
        }
        return any;
    }

    /** @return the lines of a pool at or below the ceiling (ZPM at most, as the tables) */
    static List<LootEntry> pool(LootPools.Pool pool, int ceiling) {
        int cap = Math.min(ceiling, com.gregguns.api.Tier.ZPM.voltageIndex());
        List<LootEntry> out = new ArrayList<>();
        for (LootEntry e : pool.lines()) {
            if (e.tier() == null || e.tier()
                .voltageIndex() <= cap) out.add(e);
        }
        return out;
    }

    /** @return a rack's arms: the complex's issue guns and sidearms, and blades */
    static List<LootEntry> weapons(int ceiling) {
        List<LootEntry> out = new ArrayList<>(pool(SiloArms.guns(ceiling), ceiling));
        out.addAll(pool(SiloArms.sidearms(ceiling), ceiling));
        out.add(
            new LootEntry(
                com.gregguns.entity.npc.loot.LootDrop.one("gregguns:combat_knife"),
                Math.max(1, out.size() / 4),
                com.gregguns.api.Tier.LV));
        return out;
    }

    /** @return one weighted pick of the lines made into a stack, one item unless {@code counted}; null when absent */
    static ItemStack draw(List<LootEntry> lines, Random rand, boolean counted) {
        int total = 0;
        for (LootEntry e : lines) total += Math.max(0, e.weight());
        for (int tries = 0; tries < 4 && total > 0; tries++) {
            int r = rand.nextInt(total);
            for (LootEntry e : lines) {
                r -= Math.max(0, e.weight());
                if (r >= 0) continue;
                ItemStack s = LootCategories.stack(
                    e.ref(),
                    counted ? e.drop()
                        .count(rand) : 1);
                if (s != null) return s;
                break;
            }
        }
        return null;
    }

    /** Wears gear as a ruin's chest does (D317): mothballed half the pieces 10 to 40%, gutted all 40 to 90%. */
    static ItemStack wear(ItemStack s, RoomState state, Random rand) {
        if (s == null || !SiloChests.wears(s)) return s;
        double w = 0;
        if (state == RoomState.MOTHBALLED && rand.nextBoolean()) w = 0.1 + 0.3 * rand.nextDouble();
        if (state == RoomState.GUTTED) w = 0.4 + 0.5 * rand.nextDouble();
        if (w > 0) s.setItemDamage(Math.min(s.getMaxDamage() - 1, (int) (s.getMaxDamage() * w)));
        return s;
    }

    /** An armor stand: {@code n} of its four slots, each holding a piece made for that slot. */
    private static boolean armor(IInventory inv, List<LootEntry> lines, int n, RoomState state, Random rand) {
        List<List<LootEntry>> bySlot = new ArrayList<>();
        for (int i = 0; i < 4; i++) bySlot.add(new ArrayList<LootEntry>());
        for (LootEntry e : lines) {
            ItemStack s = LootCategories.stack(e.ref(), 1);
            if (s != null && s.getItem() instanceof ItemArmor) {
                int t = ((ItemArmor) s.getItem()).armorType;
                if (t >= 0 && t < 4) bySlot.get(t)
                    .add(e);
            }
        }
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 4 && i < inv.getSizeInventory(); i++) if (!bySlot.get(i)
            .isEmpty()) slots.add(i);
        boolean any = false;
        for (int i = 0; i < n && !slots.isEmpty(); i++) {
            int slot = slots.remove(rand.nextInt(slots.size()));
            ItemStack s = wear(draw(bySlot.get(slot), rand, false), state, rand);
            if (s == null) continue;
            inv.setInventorySlotContents(slot, s);
            any = true;
        }
        return any;
    }

    /** A potion shelf: medicine while the room is worked, the empties and dregs once it is shut. */
    private static ItemStack medicine(RoomState state, Random rand) {
        boolean shut = state == RoomState.MOTHBALLED || state == RoomState.GUTTED;
        int r = rand.nextInt(10);
        if (shut && r < (state == RoomState.GUTTED ? 7 : 4)) {
            return r % 2 == 0 ? new ItemStack(Items.glass_bottle) : new ItemStack(Items.potionitem, 1, 0);
        }
        return new ItemStack(Items.potionitem, 1, MEDICINES[rand.nextInt(MEDICINES.length)]);
    }

    /** Vanilla potion metas the sick bay keeps: healing, regeneration, fire resistance, healing II, swiftness. */
    static final int[] MEDICINES = { 8197, 8197, 8193, 8195, 8229, 8226 };

    /** Food on a plate: a meal or a snack, spoiled as a ruin's food spoils (D317). */
    private static ItemStack food(RoomState state, Random rand) {
        List<LootEntry> lines = new ArrayList<>(
            SiloPools.meals()
                .lines());
        lines.addAll(
            SiloPools.snacks()
                .lines());
        ItemStack s = draw(lines, rand, false);
        if (s == null || !(s.getItem() instanceof ItemFood)) s = new ItemStack(Items.bread);
        if (rand.nextDouble() < RoomLootDraw.profile(state)
            .spoilChance()) {
            s = new ItemStack(Items.rotten_flesh);
        }
        return s;
    }

    /** @return a plain book, or now and then a book and quill */
    private static ItemStack plainBook(Random rand) {
        int r = rand.nextInt(4);
        return r == 0 ? new ItemStack(Items.writable_book) : new ItemStack(Items.book);
    }

    /** @return a vanilla written book holding a lang book ({@link ComplexWriting#book}) */
    static ItemStack book(String key) {
        ItemStack s = new ItemStack(Items.written_book);
        ComplexWriting.Book b = key == null ? null : ComplexWriting.book(key);
        if (b == null) return new ItemStack(Items.book);
        NBTTagCompound t = new NBTTagCompound();
        t.setString("title", b.title);
        t.setString("author", b.author);
        NBTTagList pages = new NBTTagList();
        for (String page : b.pages) pages.appendTag(new NBTTagString(page));
        t.setTag("pages", pages);
        s.setTagCompound(t);
        return s;
    }

    /** A writing desk: a note or a manual open on it, books stacked beside it. */
    private static boolean desk(IInventory inv, int n, RoomState state, RoomLoot.Group group, Random rand) {
        inv.setInventorySlotContents(0, book(ComplexWriting.bookKey(state, group, rand.nextLong())));
        for (int i = 0; i < n && 1 + i < 9 && 1 + i < inv.getSizeInventory(); i++) {
            inv.setInventorySlotContents(
                1 + i,
                rand.nextInt(3) == 0 ? book(ComplexWriting.bookKey(state, group, rand.nextLong())) : plainBook(rand));
        }
        return true;
    }

    /** A typewriter: paper in its tray and a typed note waiting in it. */
    private static boolean typewriter(IInventory inv, RoomState state, RoomLoot.Group group, Random rand) {
        int paper = state == RoomState.GUTTED ? 1 + rand.nextInt(4)
            : state == RoomState.MOTHBALLED ? 4 + rand.nextInt(12) : 8 + rand.nextInt(25);
        inv.setInventorySlotContents(0, new ItemStack(Items.paper, paper));
        if (inv.getSizeInventory() > 1) {
            String note = ComplexWriting.noteKey(state, rand.nextLong());
            inv.setInventorySlotContents(1, book(note != null ? note : ComplexWriting.manualKey(group)));
        }
        return true;
    }

    /**
     * A clipboard: BiblioCraft's clipboard item holding one page of the room's list. Tasks a worked room has done
     * are ticked, a mothballed room's shutdown list is all ticked, and a ruin's list stops part way, a few crossed out.
     */
    private static boolean clipboard(IInventory inv, RoomState state, RoomLoot.Group group, long h) {
        Item item = GameRegistry.findItem(com.gregguns.icbm.complex.gen.BiblioFurniture.MOD_ID, CLIPBOARD_ITEM);
        ComplexWriting.Clip clip = ComplexWriting.clip(state, group, h);
        if (item == null || clip == null) return false;
        ItemStack s = new ItemStack(item);
        s.setTagCompound(clipboardTag(clip, state, h));
        inv.setInventorySlotContents(0, s);
        return true;
    }

    /** @return the clipboard item's NBT for one page */
    static NBTTagCompound clipboardTag(ComplexWriting.Clip clip, RoomState state, long h) {
        NBTTagCompound page = new NBTTagCompound();
        NBTTagCompound tasks = new NBTTagCompound();
        int[] states = new int[ComplexWriting.CLIP_TASKS];
        Random r = new Random(h);
        int done = clip.tasks.size() == 0 ? 0 : r.nextInt(clip.tasks.size() + 1);
        for (int i = 0; i < ComplexWriting.CLIP_TASKS; i++) {
            boolean has = i < clip.tasks.size();
            tasks.setString("task" + (i + 1), has ? clip.tasks.get(i) : "");
            if (!has) continue;
            switch (state) {
                case MOTHBALLED:
                    states[i] = 1;
                    break;
                case GUTTED:
                    states[i] = i < done ? (r.nextInt(3) == 0 ? 2 : 1) : 0;
                    break;
                default:
                    states[i] = i < done ? 1 : 0;
            }
        }
        page.setIntArray("taskStates", states);
        page.setTag("tasks", tasks);
        page.setString("title", clip.title);
        NBTTagCompound t = new NBTTagCompound();
        t.setTag("page1", page);
        t.setInteger("currentPage", 1);
        t.setInteger("totalPages", 1);
        return t;
    }

    /**
     * A furnace: fuel, and the room's work. A lit room's furnace is smelting (its input, with the finished pieces
     * already in the output); a shut room's holds only fuel and what it finished before it went cold; a ruin's holds
     * a little charcoal or nothing but its last output.
     */
    private static boolean furnace(IInventory inv, String kind, RoomState state, Random rand) {
        ItemStack in = furnaceInput(kind, rand);
        ItemStack out = in == null ? null
            : FurnaceRecipes.smelting()
                .getSmeltingResult(in);
        boolean lit = FurnitureWear.lit(state);
        if (lit && in != null) inv.setInventorySlotContents(0, in);
        int fuel = lit ? 16 + rand.nextInt(33) : state == RoomState.MOTHBALLED ? 4 + rand.nextInt(13) : rand.nextInt(4);
        if (fuel > 0)
            inv.setInventorySlotContents(1, new ItemStack(Items.coal, fuel, state == RoomState.GUTTED ? 1 : 0));
        // A cold ruin's furnace with no fuel left still holds the last thing it made, so no furnace stands empty.
        if (out != null && (lit || state == RoomState.MOTHBALLED || fuel == 0 || rand.nextBoolean())) {
            ItemStack o = out.copy();
            o.stackSize = Math.min(o.getMaxStackSize(), Math.max(1, (lit ? 4 : 1) + rand.nextInt(12)));
            inv.setInventorySlotContents(2, o);
        }
        return fuel > 0 || inv.getStackInSlot(2) != null;
    }

    /** @return what a room's furnace works on: meat in a galley, scrap ore in a works room, sand elsewhere */
    static ItemStack furnaceInput(String kind, Random rand) {
        int n = 4 + rand.nextInt(13);
        if ("MESS_GALLEY".equals(kind) || "CAFETERIA".equals(kind) || "COLD_STORAGE".equals(kind)) {
            Item[] meat = { Items.beef, Items.porkchop, Items.chicken, Items.potato, Items.fish };
            return new ItemStack(meat[rand.nextInt(meat.length)], n);
        }
        if ("WORKSHOP".equals(kind) || "INCINERATOR".equals(kind) || "VEHICLE_BAY".equals(kind)) {
            return rand.nextBoolean() ? new ItemStack(Blocks.iron_ore, n) : new ItemStack(Blocks.cobblestone, n);
        }
        if ("CHEM_LAB".equals(kind)) return new ItemStack(Items.clay_ball, n);
        return new ItemStack(Blocks.sand, n);
    }

    /** A brewing stand: finished potions in its bottle slots, and an ingredient waiting while the room is worked. */
    private static boolean brewing(IInventory inv, int n, RoomState state, Random rand) {
        boolean shut = state == RoomState.MOTHBALLED || state == RoomState.GUTTED;
        for (int i = 0; i < n && i < 3; i++) {
            ItemStack s = shut && rand.nextBoolean() ? new ItemStack(Items.potionitem, 1, 0)
                : new ItemStack(Items.potionitem, 1, MEDICINES[rand.nextInt(MEDICINES.length)]);
            inv.setInventorySlotContents(i, s);
        }
        if (!shut && inv.getSizeInventory() > 3) {
            Item[] reagents = { Items.nether_wart, Items.redstone, Items.glowstone_dust, Items.sugar,
                Items.speckled_melon };
            inv.setInventorySlotContents(
                3,
                new ItemStack(reagents[rand.nextInt(reagents.length)], 1 + rand.nextInt(4)));
        }
        return n > 0;
    }

    /**
     * A flower pot: a flower, a fern, a sapling or a cactus in a lit room; a dead bush or a mushroom, which grow in the
     * dark, in a shut one. Written as the pot's own NBT ("Item" id, "Data").
     */
    private static boolean plant(TileEntity te, RoomState state, Random rand) {
        if (!(te instanceof TileEntityFlowerPot)) return false;
        Item item;
        int data = 0;
        if (FurnitureWear.lit(state)) {
            int r = rand.nextInt(10);
            if (r < 4) {
                item = Item.getItemFromBlock(Blocks.red_flower);
                data = rand.nextInt(9);
            } else if (r < 5) {
                item = Item.getItemFromBlock(Blocks.yellow_flower);
            } else if (r < 7) {
                item = Item.getItemFromBlock(Blocks.tallgrass);
                data = 2; // fern
            } else if (r < 9) {
                item = Item.getItemFromBlock(Blocks.sapling);
                data = rand.nextInt(6);
            } else {
                item = Item.getItemFromBlock(Blocks.cactus);
            }
        } else {
            item = rand.nextInt(3) == 0 ? Item.getItemFromBlock(Blocks.brown_mushroom)
                : Item.getItemFromBlock(Blocks.deadbush);
        }
        NBTTagCompound t = new NBTTagCompound();
        te.writeToNBT(t);
        t.setInteger("Item", Item.getIdFromItem(item));
        t.setInteger("Data", data);
        te.readFromNBT(t);
        te.markDirty();
        return true;
    }
}
