package com.gregguns.icbm.complex.gen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import com.gregguns.GregGuns;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.PopulateGuard;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.WindowedHeightProbe;
import com.gregguns.worldgen.WorldColumnReader;
import com.gregguns.worldgen.WorldTarget;

/**
 * {@code /gregguns locate rebuild <cellX> <cellZ>} (ops only): clears a built silo complex's whole box and writes it
 * again with the code the server runs now, at the activity level and depth its ledger entry recorded. Two passes, one
 * chunk (then one populate window) per server tick, through {@link LocateGeneration}'s tick driver, with a progress
 * line
 * every {@link #PROGRESS_EVERY} steps:
 * <ol>
 * <li><b>Clear.</b> Every column of the box, from {@link ComplexPlacer#MIN_FLOOR} (the lowest level floor any complex
 * has, above the bedrock) up to the column's natural surface, takes the blocks the dimension's terrain generator makes
 * there, caves and water included: whatever an older complex built is gone, rooms, shafts, ladder and all. Above the
 * surface only the complex's own pieces go: the radar huts' casings and their GT machines, and silo concrete just over
 * the ground. Ores and other populate features under the box are not restored. Containers are emptied before their
 * block is replaced, so nothing spills; then every dropped item, item frame and creature under the surface goes, and
 * players under the surface are moved up onto it.</li>
 * <li><b>Write.</b> Every populate window over the box runs the complex writer, exactly as chunk populate does, with
 * the ledger's {@link Decision} (kept, never re-decided).</li>
 * </ol>
 * Server thread only. The chunks the run loaded are queued for unloading at the end.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Rebuilding a complex)
final class ComplexRebuild implements LocateGeneration.Run {

    /**
     * How far above a column's natural surface the clear looks for the complex's surface pieces: a radar hut's floor,
     * walls, roof and mast stand up to 11 blocks over the recorded ground.
     */
    static final int SURFACE_REACH = 16;
    /** Silo concrete above the natural surface is a shaft ring or cap only this close to it. */
    static final int RING_REACH = 3;
    /** A progress line every this many chunks or windows. */
    static final int PROGRESS_EVERY = 32;

    private final WorldServer world;
    private final ICommandSender sender;
    private final StructurePlan plan;
    private final Decision decision;
    private final ComplexWriter writer;
    private final int x0, z0, x1, z1;
    private final List<int[]> chunks = new ArrayList<int[]>();
    private final List<int[]> windows = new ArrayList<int[]>();
    private final List<int[]> loadedByUs = new ArrayList<int[]>();
    /** The natural surface of each column of the box, filled by the clear. */
    private final int[] surface;
    private boolean writing;
    private int next;
    private long blocksCleared;
    private int windowsWritten;

    ComplexRebuild(WorldServer world, ICommandSender sender, StructurePlan plan, Decision decision,
        ComplexWriter writer) {
        this.world = world;
        this.sender = sender;
        this.plan = plan;
        this.decision = decision;
        this.writer = writer;
        x0 = plan.anchorX();
        z0 = plan.anchorZ();
        x1 = x0 + ComplexLayout.SIZE - 1;
        z1 = z0 + ComplexLayout.SIZE - 1;
        surface = new int[ComplexLayout.SIZE * ComplexLayout.SIZE];
        java.util.Arrays.fill(surface, -1);
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) chunks.add(new int[] { cx, cz });
        }
        for (int wx = Math.floorDiv(x0 - PopulateWindow.OFFSET, 16); wx
            <= Math.floorDiv(x1 - PopulateWindow.OFFSET, 16); wx++) {
            for (int wz = Math.floorDiv(z0 - PopulateWindow.OFFSET, 16); wz
                <= Math.floorDiv(z1 - PopulateWindow.OFFSET, 16); wz++) windows.add(new int[] { wx, wz });
        }
    }

    /** @return the chunks the clear visits */
    int chunkCount() {
        return chunks.size();
    }

    /** @return the populate windows the write runs */
    int windowCount() {
        return windows.size();
    }

    @Override
    public WorldServer world() {
        return world;
    }

    @Override
    public void step() {
        if (!writing) {
            if (next < chunks.size()) {
                clearChunk(chunks.get(next++));
                if (next % PROGRESS_EVERY == 0 && next < chunks.size()) {
                    say("gregguns.locate.rebuild.clearing", next, chunks.size());
                }
                return;
            }
            sweep();
            say("gregguns.locate.rebuild.cleared", chunks.size(), blocksCleared);
            writing = true;
            next = 0;
            return;
        }
        if (next < windows.size()) {
            writeWindow(windows.get(next++));
            if (next % PROGRESS_EVERY == 0 && next < windows.size()) {
                say("gregguns.locate.rebuild.writing", next, windows.size());
            }
            return;
        }
        finish();
    }

    // --- clear --------------------------------------------------------------------------------------------------

    private Chunk load(int cx, int cz) {
        ChunkProviderServer cps = world.theChunkProviderServer;
        if (!cps.chunkExists(cx, cz)) loadedByUs.add(new int[] { cx, cz });
        return cps.loadChunk(cx, cz);
    }

    private void clearChunk(int[] ch) {
        // A record from the first write would set the old complex back over the cleared ground (D348).
        ComplexKeeper.game()
            .forget(world, ch[0], ch[1]);
        clearColumns(ch);
        // At once, before an item, a creature or a player stuck in the new rock is pushed up out of it.
        sweep((ch[0] << 4), (ch[1] << 4), (ch[0] << 4) + 15, (ch[1] << 4) + 15);
    }

    private void clearColumns(int[] ch) {
        load(ch[0], ch[1]);
        // The dimension's own generator makes the chunk's terrain afresh (no populate): the natural blocks to restore.
        Chunk fresh = world.theChunkProviderServer.currentChunkProvider.provideChunk(ch[0], ch[1]);
        int bottom = ComplexPlacer.MIN_FLOOR;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = (ch[0] << 4) + lx, z = (ch[1] << 4) + lz;
                if (x < x0 || x > x1 || z < z0 || z > z1) continue;
                int top = 255;
                while (top > 0 && fresh.getBlock(lx, top, lz) == Blocks.air) top--;
                surface[(x - x0) * ComplexLayout.SIZE + (z - z0)] = top;
                for (int y = bottom; y <= top; y++) {
                    put(x, y, z, fresh.getBlock(lx, y, lz), fresh.getBlockMetadata(lx, y, lz));
                }
                int reach = Math.min(255, Math.max(top, decision.groundY()) + SURFACE_REACH);
                for (int y = top + 1; y <= reach; y++) {
                    if (surfacePiece(x, y, z, top)) put(x, y, z, Blocks.air, 0);
                }
            }
        }
    }

    /**
     * @return whether the block over the natural surface is one of the complex's surface pieces: a radar hut's casing
     *         (walls, roof, the radar's base), a GT machine on a radar casing (the radar controller, its maintenance
     *         hatch), silo concrete just over the ground (a shaft ring or cap, or an escape riser's wall, D354), or the
     *         trapdoor on top of an escape riser, one block over the water
     */
    private boolean surfacePiece(int x, int y, int z, int top) {
        Block b = world.getBlock(x, y, z);
        if (b == Blocks.air) return false;
        int meta = world.getBlockMetadata(x, y, z);
        if (b == Blocks.trapdoor) return y == top + 1;
        if (b == GregGunsCasings.INSTANCE) {
            return meta == GregGunsCasings.WORKS_CASING || meta == GregGunsCasings.GANTRY_CASING
                || meta == GregGunsCasings.RADAR_CASING;
        }
        if (b == gregtech.api.GregTechAPI.sBlockConcretes) return y <= top + RING_REACH;
        if (b == gregtech.api.GregTechAPI.sBlockMachines) {
            for (int[] d : new int[][] { { 0, -1, 0 }, { 0, 1, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 },
                { 0, 0, -1 } }) {
                if (world.getBlock(x + d[0], y + d[1], z + d[2]) == GregGunsCasings.INSTANCE
                    && world.getBlockMetadata(x + d[0], y + d[1], z + d[2]) == GregGunsCasings.RADAR_CASING) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Sets a block unless it is already there; a container is emptied first, so nothing spills. */
    private void put(int x, int y, int z, Block block, int meta) {
        Block was = world.getBlock(x, y, z);
        if (was == block && world.getBlockMetadata(x, y, z) == meta) return;
        if (was.hasTileEntity(world.getBlockMetadata(x, y, z))) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof IInventory) {
                IInventory inv = (IInventory) te;
                for (int i = 0; i < inv.getSizeInventory(); i++) inv.setInventorySlotContents(i, null);
            }
        }
        world.setBlock(x, y, z, block, meta, 2);
        blocksCleared++;
    }

    /**
     * Dropped items, item frames and every creature under the natural surface of the box go; players there are moved up
     * onto it. Run after each chunk's clear and once more after the last.
     */
    private void sweep() {
        sweep(x0, z0, x1, z1);
    }

    /** {@link #sweep()} over the box columns from (ax, az) to (bx, bz), inclusive. */
    private void sweep(int ax, int az, int bx, int bz) {
        AxisAlignedBB box = AxisAlignedBB
            .getBoundingBox(Math.max(ax, x0), 0, Math.max(az, z0), Math.min(bx, x1) + 1, 256, Math.min(bz, z1) + 1);
        for (Object o : new ArrayList<Object>(world.getEntitiesWithinAABB(Entity.class, box))) {
            Entity e = (Entity) o;
            int x = (int) Math.floor(e.posX), z = (int) Math.floor(e.posZ);
            if (x < x0 || x > x1 || z < z0 || z > z1) continue;
            int top = surface[(x - x0) * ComplexLayout.SIZE + (z - z0)];
            if (top < 0 || e.posY >= top + 1) continue;
            if (e instanceof EntityPlayerMP) {
                ((EntityPlayerMP) e).setPositionAndUpdate(x + 0.5, top + 1.1, z + 0.5);
            } else if (!(e instanceof EntityPlayer)) {
                e.setDead();
            }
        }
    }

    // --- write --------------------------------------------------------------------------------------------------

    private void writeWindow(int[] w) {
        PopulateWindow window = new PopulateWindow(w[0], w[1]);
        // The four chunks the window lies in, loaded first, so the write itself never pulls one in (GGS-11-08).
        for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) load(w[0] + dx, w[1] + dz);
        // Hands the writer the world (it places the GT machines and reads the dimension); a dimension that no longer
        // takes new complexes still rewrites one its ledger holds.
        writer.enabledIn(world);
        ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
        try (PopulateGuard.Scope ignored = PopulateGuard.enter(w[0], w[1])) {
            writer.write(plan, decision, window, sink, new WindowedHeightProbe(window, new WorldColumnReader(world)));
            windowsWritten++;
        } catch (RuntimeException e) {
            GregGuns.LOG.error(
                "Rebuild of the complex at cell " + plan.cellX() + "," + plan.cellZ() + " failed in " + window,
                e);
        }
    }

    private void finish() {
        ChunkProviderServer cps = world.theChunkProviderServer;
        for (int[] ch : loadedByUs) cps.unloadChunksIfNotNearSpawn(ch[0], ch[1]);
        loadedByUs.clear();
        com.gregguns.icbm.worldgen.ActivityLevel lv = writer instanceof ComplexPlacer
            ? ((ComplexPlacer) writer).levelOf(plan)
            : null;
        String level = lv == null ? "?" : lv.name();
        say(
            "gregguns.locate.rebuild.done",
            plan.cellX(),
            plan.cellZ(),
            windowsWritten,
            windows.size(),
            lv == null ? level : new ChatComponentTranslation(ComplexSigns.activityNameKey(lv)));
        GregGuns.LOG.info(
            "Rebuilt the silo complex at dimension {} cell {},{}: {} blocks cleared, {} of {} windows written, level {}",
            plan.dimension(),
            plan.cellX(),
            plan.cellZ(),
            blocksCleared,
            windowsWritten,
            windows.size(),
            level);
        LocateGeneration.finished(this);
    }

    private void say(String key, Object... args) {
        sender.addChatMessage(new ChatComponentTranslation(key, args));
    }
}
