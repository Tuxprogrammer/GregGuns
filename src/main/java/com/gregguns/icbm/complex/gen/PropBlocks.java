package com.gregguns.icbm.complex.gen;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.worldgen.ComplexApplier;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;

/**
 * The block of each blueprint prop (D305, D306, D308): a pack block where the pack has one (Chisel's crates and
 * vents, GT's frame box), else the vanilla block, so a pack without those mods still reads. Metas come from the
 * piece's facing (0 south, 1 west, 2 north, 3 east) and its variant. Server side; blocks are looked up by registry
 * name at write time and cached.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Props); D305; D306; D308
// Source: Chisel 2.17.34-GTNH Features.java (registry names and metas of the crate, metal crate, vent and futura
// blocks):
// https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java
public final class PropBlocks {

    /** A block and its meta. */
    public static final class Ref {

        public final Block block;
        public final int meta;

        Ref(Block block, int meta) {
            this.block = block;
            this.meta = meta;
        }
    }

    /** Chisel's mod id. */
    static final String CHISEL = "chisel";

    private static final ConcurrentMap<String, Object> FOUND = new ConcurrentHashMap<>();
    private static final Object NONE = new Object();

    private PropBlocks() {}

    /** The Chisel planks block of each wood (0 oak, 1 spruce, 2 birch, 3 jungle, 4 acacia, 5 dark oak). */
    static final String[] PLANKS = { "oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks",
        "dark_oak_planks" };

    /** The Chisel crate metas of a planks block: crate, fancy crate, crate (extra). */
    static final int CRATE_META = 10;

    /** @return a furnace's meta for a front facing {@code facing} (0 south, 1 west, 2 north, 3 east) */
    static int furnaceMeta(int facing) {
        return new int[] { 3, 4, 2, 5 }[facing & 3];
    }

    /**
     * @return an iron door's lower meta for a door that faces {@code facing}: the closed door stands on the cell's
     *         edge on that side (0 the west edge, 1 north, 2 east, 3 south, the vanilla door rule)
     */
    static int doorMeta(int facing) {
        return new int[] { 3, 0, 1, 2 }[facing & 3];
    }

    /**
     * @param key    a furniture mark key
     * @param facing the mark's facing (0 south, 1 west, 2 north, 3 east)
     * @return the prop's block, or null for a piece this class does not write (BiblioCraft furniture, the locker, the
     *         style cubes, the metal prop the contents pass places)
     */
    public static Ref block(String key, int facing) {
        FurniturePiece p = FurniturePiece.of(key);
        if (p == null) return null;
        int v = Math.max(0, FurniturePiece.variantOf(key));
        int f = facing & 3;
        switch (p) {
            case BED_FOOT:
                return new Ref(Blocks.bed, f);
            case BED_HEAD:
                return new Ref(Blocks.bed, f | 8);
            case CRATE: {
                int wood = (v & 7) % PLANKS.length;
                Ref r = chisel(PLANKS[wood], CRATE_META + Math.min(2, v >> 3));
                return r != null ? r : new Ref(Blocks.planks, wood);
            }
            case CONTAINER: {
                Ref r = v == 2 ? chisel("iron_block", 10) : chisel(v == 1 ? "aluminumblock" : "steelblock", 1);
                return r != null ? r : new Ref(Blocks.iron_block, 0);
            }
            case PACKED_ICE:
                return new Ref(Blocks.packed_ice, 0);
            case HAY:
                return new Ref(Blocks.hay_block, 0);
            case FURNACE:
                return new Ref(Blocks.furnace, furnaceMeta(f));
            case CAULDRON:
                return new Ref(Blocks.cauldron, 0);
            case BREWING_STAND:
                return new Ref(Blocks.brewing_stand, 0);
            case IRON_BARS:
            case CELL_BARS:
                return new Ref(Blocks.iron_bars, 0);
            case PANE:
                return new Ref(Blocks.stained_glass_pane, 0);
            case GLASS:
                return new Ref(Blocks.glass, 0);
            case VENT_RISER: {
                Ref r = chisel("factoryblock2", 1);
                return r != null ? r : new Ref(Blocks.iron_bars, 0);
            }
            case CASING_FRAME: {
                Ref r = casingFrame();
                return r != null ? r : new Ref(Blocks.iron_bars, 0);
            }
            case COAL_BUNKER:
                return new Ref(Blocks.coal_block, 0);
            case ASH_HEAP:
                return new Ref((v & 1) == 1 ? Blocks.soul_sand : Blocks.gravel, 0);
            case CRAFTING_TABLE:
                return new Ref(Blocks.crafting_table, 0);
            case ANVIL:
                // The low two bits turn the anvil along the facing's axis, the next two hold the damage.
                return new Ref(Blocks.anvil, (f & 1) | Math.min(2, v) << 2);
            case MUSHROOM_STEM:
                return new Ref(Blocks.brown_mushroom_block, 10);
            case JUKEBOX:
                return new Ref(Blocks.jukebox, 0);
            case SCREEN: {
                Ref r = chisel("futura", 0);
                return r != null ? r : new Ref(Blocks.stained_hardened_clay, 15);
            }
            case POOL_TABLE:
                return new Ref(Blocks.planks, 5);
            case POOL_FELT:
                return new Ref(Blocks.carpet, 13);
            case RUG:
                return new Ref(Blocks.carpet, v & 15);
            case LAUNDRY:
                return new Ref(Blocks.wool, (v & 1) == 1 ? 8 : 0);
            case FENCE_POST:
                return new Ref(Blocks.fence, 0);
            case TARGET:
                return new Ref(Blocks.wool, 14);
            case IRON_DOOR:
                return new Ref(Blocks.iron_door, doorMeta(f));
            case IRON_DOOR_TOP:
                return new Ref(Blocks.iron_door, 8);
            case SHOWER_HEAD:
                return new Ref(Blocks.tripwire_hook, f);
            case COBWEB:
                return new Ref(Blocks.web, 0);
            case FLOWER_POT:
                return new Ref(Blocks.flower_pot, 0); // the plant is the contents pass's (D323)
            case FARMLAND_CROP:
                return new Ref(Blocks.farmland, 7);
            case WATER_INLAY:
                return new Ref(Blocks.water, 0);
            case MYCELIUM_BED:
                return new Ref(Blocks.mycelium, 0);
            case POOL_WATER:
                return new Ref(Blocks.water, 0);
            case POOL_LINER:
                // GT Light Concrete, the silo's own finish: a clean, pale tank lining (D321).
                return new Ref(GregTechAPI.sBlockConcretes, 8);
            default:
                return null;
        }
    }

    /** @return the mushroom a mycelium bed grows (variant 0 brown, 1 red) */
    static Ref mushroom(String key) {
        return new Ref(FurniturePiece.variantOf(key) == 1 ? Blocks.red_mushroom : Blocks.brown_mushroom, 0);
    }

    private static Ref chisel(String name, int meta) {
        Block b = find(CHISEL, name);
        return b == null ? null : new Ref(b, meta);
    }

    private static Ref casingFrame() {
        ItemStack frame = ComplexApplier.casingFrame();
        if (frame == null) return null;
        Block b = Block.getBlockFromItem(frame.getItem());
        return b == null || b == Blocks.air ? null : new Ref(b, frame.getItemDamage());
    }

    /** @return the block registered under {@code mod:name}, or null (cached) */
    static Block find(String mod, String name) {
        String k = mod + ":" + name;
        Object o = FOUND.get(k);
        if (o == null) {
            Block b = GameRegistry.findBlock(mod, name);
            FOUND.put(k, b == null ? NONE : b);
            return b;
        }
        return o == NONE ? null : (Block) o;
    }
}
