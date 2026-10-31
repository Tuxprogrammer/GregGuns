package com.gregguns.client.icbm;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Redraws a Carpenter's Blocks garage door piece shortly after it opens or shuts (D345). The complexes' blast doors
 * (D335) are wide Carpenter's garage doors, and in the real pack a client sometimes kept drawing a door in its old
 * state for many seconds after a toggle, in whole or along a chunk line, although the piece's data had already
 * arrived; a re-render of its section put the picture right. The client watches the open bit of the garage door
 * pieces near the player and, when one flips, marks that piece's section for a re-render twice: half a second and two
 * seconds later, after any rebuild that was already running. Client side; installed only when Carpenter's Blocks is
 * loaded. A piece read without the field (another Carpenter's version) is never nudged.
 */
// Source:
// https://github.com/GTNewHorizons/CarpentersBlocks/blob/3.7.3-GTNH/src/main/java/com/carpentersblocks/tileentity/TECarpentersGarageDoor.java
// https://github.com/GTNewHorizons/CarpentersBlocks/blob/3.7.3-GTNH/src/main/java/com/carpentersblocks/data/GarageDoor.java#L28-L135
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Blast doors); D345
public final class GarageDoorRedraw {

    /** Carpenter's garage door tile class, by name (no compile dependency). */
    static final String DOOR_TILE = "com.carpentersblocks.tileentity.TECarpentersGarageDoor";
    /** Ticks between scans of the pieces near the player. */
    static final int SCAN_EVERY = 4;
    /** Chunks scanned around the player, each way. */
    static final int RADIUS = 6;
    /** Ticks after a flip at which the piece's section is re-rendered. */
    static final int[] NUDGES = { 10, 40 };

    private final Tracker tracker = new Tracker();
    private Field data;
    private Class<?> doorClass;
    private World lastWorld;
    private long tick;

    private GarageDoorRedraw() {}

    /** Installs the tick handler when Carpenter's Blocks is loaded. */
    static void install() {
        if (!Loader.isModLoaded("CarpentersBlocks")) return;
        GarageDoorRedraw r = new GarageDoorRedraw();
        try {
            r.doorClass = Class.forName(DOOR_TILE);
            Class<?> c = r.doorClass;
            while (c != null && r.data == null) {
                try {
                    r.data = c.getDeclaredField("cbMetadata");
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            if (r.data == null) throw new NoSuchFieldException("cbMetadata");
            r.data.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.info("Carpenter's garage door redraw is off: {}", e.toString());
            return;
        }
        FMLCommonHandler.instance()
            .bus()
            .register(r);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null || mc.thePlayer == null) {
            lastWorld = null;
            return;
        }
        if (world != lastWorld) {
            tracker.clear();
            lastWorld = world;
        }
        tick++;
        for (long[] due : tracker.due(tick)) {
            world.markBlockRangeForRenderUpdate(
                (int) due[0],
                (int) due[1],
                (int) due[2],
                (int) due[0],
                (int) due[1],
                (int) due[2]);
        }
        if (tick % SCAN_EVERY != 0) return;
        int pcx = (int) Math.floor(mc.thePlayer.posX) >> 4, pcz = (int) Math.floor(mc.thePlayer.posZ) >> 4;
        for (int cx = pcx - RADIUS; cx <= pcx + RADIUS; cx++) {
            for (int cz = pcz - RADIUS; cz <= pcz + RADIUS; cz++) {
                if (!world.getChunkProvider()
                    .chunkExists(cx, cz)) continue;
                Chunk c = world.getChunkFromChunkCoords(cx, cz);
                for (Object o : new ArrayList<Object>(c.chunkTileEntityMap.values())) {
                    if (o.getClass() != doorClass) continue;
                    TileEntity t = (TileEntity) o;
                    try {
                        tracker.observe(t, t.xCoord, t.yCoord, t.zCoord, data.getInt(t), tick);
                    } catch (IllegalAccessException e) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Which pieces flipped and when their sections are due a re-render. Pure apart from the weak map keyed by the
     * piece, so a piece unloaded with its chunk is forgotten.
     */
    static final class Tracker {

        /** The open bit of a garage door piece's data. */
        static final int OPEN = 0x80;

        private final Map<Object, Integer> seen = new WeakHashMap<Object, Integer>();
        private final ArrayDeque<long[]> pending = new ArrayDeque<long[]>();

        /** Records a piece's data; a flip of its open bit since the last look schedules its re-renders. */
        void observe(Object piece, int x, int y, int z, int data, long now) {
            Integer before = seen.put(piece, data);
            if (before == null || ((before ^ data) & OPEN) == 0) return;
            for (int n : NUDGES) pending.add(new long[] { x, y, z, now + n });
        }

        /** @return the pieces due a re-render at this tick, removed from the queue */
        java.util.List<long[]> due(long now) {
            java.util.List<long[]> out = new ArrayList<long[]>();
            int n = pending.size();
            for (int i = 0; i < n; i++) {
                long[] p = pending.poll();
                if (p[3] <= now) out.add(p);
                else pending.add(p);
            }
            return out;
        }

        void clear() {
            seen.clear();
            pending.clear();
        }
    }
}
