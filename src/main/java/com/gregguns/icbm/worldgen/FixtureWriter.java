package com.gregguns.icbm.worldgen;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.GregGuns;
import com.gregguns.icbm.worldgen.ComplexFixtures.BlastDoor;
import com.gregguns.icbm.worldgen.ComplexFixtures.Column;
import com.gregguns.icbm.worldgen.ComplexFixtures.Run;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.layout.ResolvedBlock;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Writes a complex's {@link ComplexFixtures} into one populate window: blast doors as Carpenter's Blocks garage doors
 * covered in the hall's door plate, their Ross128 casing frames, rubble, cable trays of GregTech frame boxes, and
 * GregTech cables and large fluid pipes joined along their runs. Every write goes through the window's sink, so a
 * window writes only its own columns and never loads a neighbor. Server side.
 *
 * <p>
 * Without Carpenter's Blocks the doors are left out (logged once) and their portals stay open; the frames, rubble and
 * runs are written either way.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Blast doors, Service runs)
public final class FixtureWriter {

    /** Carpenter's Blocks' mod id and its garage door's registry name. */
    static final String CARPENTERS = "CarpentersBlocks", GARAGE_DOOR = "blockCarpentersGarageDoor";
    /** The garage door's tile entity id. */
    static final String GARAGE_TILE = "TileEntityCarpentersGarageDoor";

    private final WindowedSink sink;
    private final int ox, oy, oz, ceiling;
    private final IntFunction<ResolvedBlock> plates;

    /**
     * @param sink    the clipped sink
     * @param ox      world X of complex-local x 0
     * @param oy      world Y of complex-local y 0 (the level floor)
     * @param oz      world Z of complex-local z 0
     * @param ceiling the complex's ceiling (a GT voltage index): it picks the casing, the cable and the pipe
     * @param plates  the door plate of a hall section, from its palette style; null or a null answer for the fallback
     */
    public FixtureWriter(WindowedSink sink, int ox, int oy, int oz, int ceiling, IntFunction<ResolvedBlock> plates) {
        this.sink = sink;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.ceiling = ceiling;
        this.plates = plates;
    }

    /** @param f the fixtures of the complex; each one is written where it lies inside the window */
    public void write(ComplexFixtures f) {
        for (BlastDoor d : f.doors()) door(d);
        for (Run r : f.runs()) run(r);
        ItemStack frame = trayFrame();
        Block fb = frame == null ? null : Block.getBlockFromItem(frame.getItem());
        if (fb != null && fb != Blocks.air) {
            for (int[] c : f.fallen()) sink.setBlock(ox + c[0], oy + c[1], oz + c[2], fb, frame.getItemDamage());
        }
    }

    // --- blast doors -------------------------------------------------------------------------------------------

    private void door(BlastDoor d) {
        if (!d.frame()
            .isEmpty()) {
            ItemStack casing = tierCasing(ceiling);
            Block cb = casing == null ? null : Block.getBlockFromItem(casing.getItem());
            if (cb != null && cb != Blocks.air) {
                for (int[] c : d.frame()) sink.setBlock(ox + c[0], oy + c[1], oz + c[2], cb, casing.getItemDamage());
            }
        }
        for (int[] c : d.rubble()) {
            sink.setBlock(
                ox + c[0],
                oy + c[1],
                oz + c[2],
                ((c[0] + c[1] * 3 + c[2]) & 3) == 0 ? Blocks.gravel : Blocks.cobblestone,
                0);
        }
        Block garage = garageDoor();
        if (garage == null) return;
        ItemStack cover = coverOf(plates == null ? null : plates.apply(d.hall()));
        for (int a = d.a0(); a <= d.a1(); a++) {
            int x = ox + d.x(a), z = oz + d.z(a);
            if (!sink.window()
                .contains(x, z)) continue;
            Column col = d.column(a);
            if (col == Column.TORN) continue;
            int from = col == Column.JAMMED ? d.y1() - d.hang(a) + 1 : d.y0();
            boolean open = col == Column.OPEN;
            for (int y = from; y <= d.y1(); y++) {
                int data = pieceData(d.facing(), open, y == d.y1());
                if (sink.setBlock(x, oy + y, z, garage, 0)) {
                    sink.setTile(x, oy + y, z, garageTile(x, oy + y, z, data, cover));
                }
            }
        }
    }

    /**
     * Garage door types: the "siding" look, steel-backed horizontal slats of the cover (Carpenter's
     * {@code GarageDoor}).
     */
    static final int TYPE_SIDING = 3;

    /**
     * The 16-bit data of one garage door piece, in Carpenter's layout: type in bits 0 to 3, the facing (a Forge
     * direction) in 4 to 6, open in 7, rigid (redstone only) in 8, host (the top piece) in 9. Our pieces are never
     * rigid: a player opens and shuts the door by hand, and the whole connected door moves.
     *
     * @param facing 0 north, 1 east, 2 south, 3 west: the way into the hall
     * @param open   whether the piece is rolled up
     * @param host   whether it is the top piece of its column
     */
    // Source:
    // https://github.com/GTNewHorizons/CarpentersBlocks/blob/3.7.3-GTNH/src/main/java/com/carpentersblocks/data/GarageDoor.java#L28-L135
    static int pieceData(int facing, boolean open, boolean host) {
        int dir = FORGE_DIR[facing & 3];
        return TYPE_SIDING | dir << 4 | (open ? 1 : 0) << 7 | (host ? 1 : 0) << 9;
    }

    /** Forge direction ordinals of our facings: north 2, east 5, south 3, west 4. */
    private static final int[] FORGE_DIR = { 2, 5, 3, 4 };

    /**
     * A garage door piece's tile: Carpenter's {@code TEBase} NBT with the piece data and the cover as the attribute on
     * the whole block (attribute 6).
     */
    // Source:
    // https://github.com/GTNewHorizons/CarpentersBlocks/blob/3.7.3-GTNH/src/main/java/com/carpentersblocks/tileentity/TEBase.java#L33-L131
    // https://github.com/GTNewHorizons/CarpentersBlocks/blob/3.7.3-GTNH/src/main/java/com/carpentersblocks/util/Attribute.java
    private static TileEntity garageTile(int x, int y, int z, int data, ItemStack cover) {
        NBTTagCompound t = new NBTTagCompound();
        t.setString("id", GARAGE_TILE);
        t.setInteger("x", x);
        t.setInteger("y", y);
        t.setInteger("z", z);
        t.setInteger("cbMetadata", data);
        NBTTagList attrs = new NBTTagList();
        if (cover != null) {
            NBTTagCompound c = new NBTTagCompound();
            c.setByte("cbAttribute", (byte) 6);
            cover.writeToNBT(c);
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(cover.getItem());
            if (id != null) c.setString("cbUniqueId", id.toString());
            attrs.appendTag(c);
        }
        t.setTag("cbAttrList", attrs);
        t.setString("cbDesign", "");
        t.setString("cbOwner", "");
        return TileEntity.createAndLoadEntity(t);
    }

    /** @return the cover stack of a door plate, or null to leave the pieces bare */
    private static ItemStack coverOf(ResolvedBlock plate) {
        if (plate == null || plate.block == null || plate.block == Blocks.air) return null;
        Item item = Item.getItemFromBlock(plate.block);
        return item == null ? null : new ItemStack(item, 1, plate.meta);
    }

    private static final Object NONE = new Object();
    private static final Map<String, Object> BLOCKS = new ConcurrentHashMap<>();

    /** @return Carpenter's garage door, or null when the mod or the block is absent (logged once) */
    static Block garageDoor() {
        Object o = BLOCKS.get(GARAGE_DOOR);
        if (o == null) {
            Block b = GameRegistry.findBlock(CARPENTERS, GARAGE_DOOR);
            if (b == null) {
                GregGuns.LOG.info("Silo complex: Carpenter's Blocks garage door is absent; blast doors are left out");
            }
            BLOCKS.put(GARAGE_DOOR, b == null ? NONE : b);
            return b;
        }
        return o == NONE ? null : (Block) o;
    }

    /** @return the GregTech machine casing of a voltage tier (D337), or null */
    static ItemStack tierCasing(int voltage) {
        ItemList[] casings = ItemList.MACHINE_CASINGS;
        int i = Math.max(0, Math.min(casings.length - 1, voltage));
        return casings[i].get(1);
    }

    // --- service runs ------------------------------------------------------------------------------------------

    private void run(Run r) {
        int lo = Math.max(
            r.a0(),
            (r.alongX() ? sink.window()
                .minX() - ox
                : sink.window()
                    .minZ() - oz)),
            hi = Math.min(
                r.a1(),
                (r.alongX() ? sink.window()
                    .maxX() - ox
                    : sink.window()
                        .maxZ() - oz));
        int line = r.alongX() ? oz + r.line() : ox + r.line();
        boolean lineIn = r.alongX() ? line >= sink.window()
            .minZ() && line
                <= sink.window()
                    .maxZ()
            : line >= sink.window()
                .minX() && line
                    <= sink.window()
                        .maxX();
        if (lo > hi || !lineIn) return;
        switch (r.service()) {
            case TRAY: {
                ItemStack frame = trayFrame();
                Block fb = frame == null ? null : Block.getBlockFromItem(frame.getItem());
                if (fb == null || fb == Blocks.air) return;
                for (int a = lo; a <= hi; a++) {
                    if (r.has(a)) sink.setBlock(ox + r.x(a), oy + r.y(), oz + r.z(a), fb, frame.getItemDamage());
                }
                break;
            }
            default: {
                int id = pipeId(r.service() == ComplexFixtures.Service.CABLE, ceiling);
                IMetaTileEntity proto = id > 0 && id < GregTechAPI.METATILEENTITIES.length
                    ? GregTechAPI.METATILEENTITIES[id]
                    : null;
                if (proto == null) return;
                for (int a = lo; a <= hi; a++) {
                    if (!r.has(a)) continue;
                    int x = ox + r.x(a), y = oy + r.y(), z = oz + r.z(a);
                    byte mask = (byte) ((r.has(a - 1) ? along(r, -1) : 0) | (r.has(a + 1) ? along(r, 1) : 0));
                    if (sink.setBlock(x, y, z, GregTechAPI.sBlockMachines, proto.getTileEntityBaseType())) {
                        TileEntity tile = pipeTile(id, mask);
                        if (tile != null) sink.setTile(x, y, z, tile);
                    }
                }
                break;
            }
        }
    }

    /** @return the connection bit (a Forge direction's flag) toward the next cell along a run, back or forth */
    static byte along(Run r, int step) {
        // Forge directions: 2 north (-Z), 3 south (+Z), 4 west (-X), 5 east (+X); a side's flag is 1 << ordinal.
        int dir = r.alongX() ? step < 0 ? 4 : 5 : step < 0 ? 2 : 3;
        return (byte) (1 << dir);
    }

    /** NBT templates of fresh pipe tiles, by meta tile entity id. */
    private static final Map<Integer, NBTTagCompound> PIPE_TEMPLATES = new HashMap<>();

    /**
     * A GregTech pipe or cable tile joined on the sides of {@code mask}. Worldgen sets the connections the way
     * BartWorks' ruins set their cables: on the base tile and, through its saved data, on the meta tile entity (GT6
     * pipes and cables load their own connections from the same tag).
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/system/worldgen/MapGenRuins.java#L180-L201
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/BaseMetaPipeEntity.java#L114-L156
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTECable.java#L730-L742
    static TileEntity pipeTile(int id, byte mask) {
        NBTTagCompound template;
        synchronized (PIPE_TEMPLATES) {
            template = PIPE_TEMPLATES.get(id);
            if (template == null) {
                BaseMetaPipeEntity fresh = new BaseMetaPipeEntity();
                fresh.setInitialValuesAsNBT(null, (short) id);
                if (fresh.getMetaTileEntity() == null) return null;
                template = new NBTTagCompound();
                fresh.writeToNBT(template);
                PIPE_TEMPLATES.put(id, template);
            }
        }
        NBTTagCompound tag = (NBTTagCompound) template.copy();
        tag.setByte("mConnections", mask);
        BaseMetaPipeEntity tile = new BaseMetaPipeEntity();
        tile.readFromNBT(tag);
        return tile;
    }

    /** Cable and pipe materials by tier, EV first; a tier past the table takes its last row. */
    static final String[][] SERVICE_MATERIALS = { { "Aluminium", "Steel" }, { "Platinum", "StainlessSteel" },
        { "VanadiumGallium", "Titanium" }, { "Naquadah", "TungstenSteel" }, { "NaquadahAlloy", "Iridium" } };

    private static final Map<String, Integer> IDS = new ConcurrentHashMap<>();

    /**
     * @return the meta tile entity id of the complex's cable (a 4x cable) or large fluid pipe at a ceiling: the tier's
     *         row, stepping down a row while GregTech has no such part; -1 when none resolves
     */
    static int pipeId(boolean cable, int ceiling) {
        String key = (cable ? "c" : "p") + ceiling;
        Integer cached = IDS.get(key);
        if (cached != null) return cached;
        int row = Math.max(0, Math.min(SERVICE_MATERIALS.length - 1, ceiling - 4));
        int id = -1;
        for (int i = row; i >= 0 && id < 0; i--) {
            Materials m = Materials.get(SERVICE_MATERIALS[i][cable ? 0 : 1]);
            ItemStack s = m == null ? null
                : GTOreDictUnificator.get(cable ? OrePrefixes.cableGt04 : OrePrefixes.pipeLarge, m, 1);
            if (s != null && Block.getBlockFromItem(s.getItem()) == GregTechAPI.sBlockMachines) id = s.getItemDamage();
        }
        IDS.put(key, id);
        return id;
    }

    /** @return the cable tray's frame box: steel */
    static ItemStack trayFrame() {
        return GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 1);
    }
}
