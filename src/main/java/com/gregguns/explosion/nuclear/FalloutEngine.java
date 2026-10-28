package com.gregguns.explosion.nuclear;

import java.util.Iterator;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.gregguns.GregGuns;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.explosion.BlockBreakGuard;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Runs fallout every server tick (D21). Three hard bounds keep it safe on any server: a job scans at most
 * {@code falloutColumnsPerTick} columns and at most {@code falloutBudgetMs} milliseconds a tick across all worlds; a
 * job
 * converts at most {@code falloutMaxBlocks} blocks in total; decay reverts at most {@code falloutRevertPerTick} blocks
 * a
 * tick per world. Nothing spreads and nothing random-ticks. Only loaded chunks are touched, and every conversion asks
 * the protection mods first ({@link BlockBreakGuard}). Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md
public final class FalloutEngine {

    private static final FalloutEngine INSTANCE = new FalloutEngine();
    private static boolean registered;

    private long maxTickNanos;
    private long convertedTotal;
    private int tickCounter;

    private FalloutEngine() {}

    /** @return the one engine */
    public static FalloutEngine instance() {
        return INSTANCE;
    }

    /** Registers the tick handler once; safe to call from several modules. */
    public static synchronized void register() {
        if (!registered) {
            registered = true;
            FMLCommonHandler.instance()
                .bus()
                .register(INSTANCE);
        }
    }

    /**
     * @param world  a server world
     * @param create whether to create the record when there is none
     * @return the world's fallout record, or null when there is none and {@code create} is false
     */
    static FalloutSavedData data(World world, boolean create) {
        FalloutSavedData d = (FalloutSavedData) world.perWorldStorage
            .loadData(FalloutSavedData.class, FalloutSavedData.NAME);
        if (d == null && create) {
            d = new FalloutSavedData(FalloutSavedData.NAME);
            world.perWorldStorage.setData(FalloutSavedData.NAME, d);
        }
        return d;
    }

    /** @return the longest time one tick spent on fallout since {@link #resetStats()}, nanoseconds */
    public long maxTickNanos() {
        return maxTickNanos;
    }

    /** @return blocks converted since {@link #resetStats()} */
    public long convertedTotal() {
        return convertedTotal;
    }

    /** Test hook: zeroes the statistics. */
    public void resetStats() {
        maxTickNanos = 0L;
        convertedTotal = 0L;
    }

    /** @param world a world @return true when it has fallout jobs still converting */
    public boolean busy(World world) {
        FalloutSavedData d = data(world, false);
        return d != null && !d.jobs.isEmpty();
    }

    /** @param world a world @return blocks waiting to decay in it */
    public int irradiatedBlocks(World world) {
        FalloutSavedData d = data(world, false);
        int n = 0;
        if (d != null) {
            for (FalloutSavedData.Job j : d.jobs) {
                n += j.blocks();
            }
            for (FalloutSavedData.Decay dec : d.decays) {
                n += dec.blocks.length / 3 - dec.cursor + dec.kept;
            }
        }
        return n;
    }

    /** One tick of fallout at the end of the server tick. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.worldServers == null) {
            return;
        }
        long start = System.nanoTime();
        long limit = start + ExplosionsConfig.falloutBudgetMs * 1_000_000L;
        int columns = ExplosionsConfig.falloutColumnsPerTick;
        boolean worked = false;
        boolean exposure = ++tickCounter % RadiationExposure.PERIOD_TICKS == 0;
        for (WorldServer world : server.worldServers) {
            if (world == null) {
                continue;
            }
            FalloutSavedData d = data(world, false);
            if (d == null || d.jobs.isEmpty() && d.decays.isEmpty()) {
                continue;
            }
            try {
                columns = runJobs(world, d, columns, limit);
                runDecay(world, d);
                worked = true;
                if (exposure) {
                    RadiationExposure.check(world);
                }
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Fallout failed in dimension {}; dropping its jobs", world.provider.dimensionId, e);
                d.jobs.clear();
                d.touch();
            }
        }
        if (worked) {
            maxTickNanos = Math.max(maxTickNanos, System.nanoTime() - start);
        }
    }

    private int runJobs(WorldServer world, FalloutSavedData d, int columns, long limit) {
        Iterator<FalloutSavedData.Job> it = d.jobs.iterator();
        while (it.hasNext() && columns > 0) {
            FalloutSavedData.Job job = it.next();
            if (job.wait > 0) {
                job.wait--;
                continue;
            }
            int width = 2 * job.radius + 1;
            long total = (long) width * width;
            long r2 = (long) job.radius * job.radius;
            while (columns > 0 && job.cursor < total && job.blocks() < job.budget) {
                int dx = job.cursor % width - job.radius;
                int dz = job.cursor / width - job.radius;
                job.cursor++;
                if ((long) dx * dx + (long) dz * dz > r2) {
                    continue; // outside the disc: free, not a column
                }
                columns--;
                convertColumn(world, job, job.x + dx, job.z + dz, Math.sqrt((double) dx * dx + (double) dz * dz));
                if ((columns & 7) == 0 && System.nanoTime() > limit) {
                    columns = 0;
                }
            }
            d.touch();
            if (job.cursor >= total || job.blocks() >= job.budget) {
                finishJob(world, d, job);
                it.remove();
            }
        }
        return columns;
    }

    private void convertColumn(WorldServer world, FalloutSavedData.Job job, int x, int z, double distance) {
        if (FalloutMath.roll(job.seed, x, z, 0) >= FalloutMath.chanceAt(job.peak, distance, job.radius)) {
            return;
        }
        // Never load a chunk for fallout.
        if (!world.blockExists(x, 64, z)) {
            return;
        }
        int y = Math.min(255, world.getHeightValue(x, z) - 1);
        for (int steps = 0; steps < 8 && y > 0; steps++, y--) {
            Block block = world.getBlock(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);
            if (FalloutBlocks.variantFor(block, meta) >= 0) {
                break;
            }
            if (block.getMaterial()
                .isLiquid()
                || block.getMaterial()
                    .isSolid() && block.isOpaqueCube()) {
                return; // water, or something solid that is not surface earth: leave this column alone
            }
        }
        int surfaceVariant = -1;
        for (int depth = 0; depth < 3 && y > 0 && job.blocks() < job.budget; depth++, y--) {
            if (depth > 0 && FalloutMath.roll(job.seed, x, z, depth) >= 0.5) {
                break;
            }
            Block block = world.getBlock(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);
            int variant = FalloutBlocks.variantFor(block, meta);
            if (variant < 0) {
                break; // caves and other material end the column: surface only
            }
            if (depth == 0) {
                surfaceVariant = variant;
            } else if (variant != surfaceVariant) {
                break; // at most two of the same material below the surface block (D84): grass over dirt, not stone
            }
            if (!BlockBreakGuard.mayBreak(world, x, y, z, block, meta, FakePlayerFactory.getMinecraft(world))) {
                return;
            }
            if (placeFallout(world, job, x, y, z, depth, variant, distance)) {
                job.add(x, y, z);
                convertedTotal++;
            }
        }
    }

    /**
     * Turns one block of the surface into fallout: goo (rare, top block, near ground zero, capped per blast and per
     * config), trinitite (sand in the inner fireball), or irradiated earth, hot in the inner fireball of a ZPM or
     * better blast. All rolls are deterministic from the job's seed.
     *
     * @return true when a block was placed
     */
    private boolean placeFallout(WorldServer world, FalloutSavedData.Job job, int x, int y, int z, int depth,
        int variant, double distance) {
        double inner = FalloutMath.INNER_SHARE * job.radius;
        // Hot earth and trinitite thin out smoothly from ground zero to the rim of the inner fireball (D155).
        double hotChance = FalloutMath.hotChance(distance, inner, job.grade);
        boolean hot = FalloutMath.roll(job.seed, x, z, 4 + depth) < hotChance;
        if (depth == 0 && distance <= inner * FalloutMath.GOO_REACH
            && job.goo < ExplosionsConfig.falloutMaxGoo
            && FalloutMath.roll(job.seed, x, z, 3) < FalloutMath.gooChance(distance, inner)) {
            if (world.setBlock(x, y, z, FalloutBlocks.goo(), job.grade >= 1 ? 1 : 0, 2)) {
                job.goo++;
                FalloutSavedData data = data(world, true);
                data.addGoo(x, y, z);
                return true;
            }
            return false;
        }
        if (variant == FalloutBlocks.SAND && FalloutMath.roll(job.seed, x, z, 7 + depth) < hotChance) {
            return world.setBlock(x, y, z, FalloutBlocks.trinitite(), 0, 2);
        }
        return world.setBlock(x, y, z, FalloutBlocks.irradiated(), variant | (hot ? FalloutBlocks.HOT_BIT : 0), 2);
    }

    /** Called when a goo block is broken or replaced: the ledger that holds back decay forgets it. */
    static void gooRemoved(World world, int x, int y, int z) {
        FalloutSavedData d = data(world, false);
        if (d != null) {
            d.removeGoo(x, y, z);
        }
    }

    /**
     * Digs up every goo block the ledger knows in a world, as a player with a shovel would (the blocks vanish without
     * drops), and forgets any entry whose block is already gone. Used by tests to leave a world clean.
     *
     * @param world a world
     * @return how many goo blocks were removed
     */
    public int clearGoo(World world) {
        FalloutSavedData d = data(world, false);
        if (d == null || world.isRemote) {
            return 0;
        }
        int n = 0;
        for (int[] p : d.gooPositions()) {
            if (world.getBlock(p[0], p[1], p[2]) == FalloutBlocks.goo()) {
                world.setBlock(p[0], p[1], p[2], net.minecraft.init.Blocks.air, 0, 2);
                n++;
            }
            d.removeGoo(p[0], p[1], p[2]);
        }
        return n;
    }

    /** @param world a world @return goo blocks the ledger holds in it */
    public int gooBlocks(World world) {
        FalloutSavedData d = data(world, false);
        return d == null ? 0 : d.gooCount();
    }

    private void finishJob(World world, FalloutSavedData d, FalloutSavedData.Job job) {
        if (job.count == 0) {
            return;
        }
        FalloutSavedData.Decay decay = new FalloutSavedData.Decay();
        decay.due = world.getTotalWorldTime() + ExplosionsConfig.falloutDecayMinutes * 1200L;
        decay.deadline = decay.due + FalloutSavedData.Decay.MAX_AGE_TICKS;
        decay.blocks = java.util.Arrays.copyOf(job.converted, job.count);
        d.decays.add(decay);
        d.touch();
    }

    private void runDecay(WorldServer world, FalloutSavedData d) {
        if (d.decays.isEmpty()) {
            return;
        }
        long now = world.getTotalWorldTime();
        FalloutSavedData.Decay decay = null;
        for (FalloutSavedData.Decay candidate : d.decays) {
            if (now >= candidate.due) {
                decay = candidate;
                break;
            }
        }
        if (decay == null) {
            return;
        }
        int total = decay.blocks.length / 3;
        int end = Math.min(total, decay.cursor + ExplosionsConfig.falloutRevertPerTick);
        for (; decay.cursor < end; decay.cursor++) {
            int x = decay.blocks[decay.cursor * 3];
            int y = decay.blocks[decay.cursor * 3 + 1];
            int z = decay.blocks[decay.cursor * 3 + 2];
            if (!world.blockExists(x, y, z)) {
                // Its chunk is unloaded: keep the entry for the next pass (compacted in place; kept <= cursor).
                int at = decay.kept++ * 3;
                decay.blocks[at] = x;
                decay.blocks[at + 1] = y;
                decay.blocks[at + 2] = z;
            } else if (d.gooInChunkOf(x, z)) {
                // Goo still stands in this chunk: the land does not recover until it is cleaned up (D21).
                int at = decay.kept++ * 3;
                decay.blocks[at] = x;
                decay.blocks[at + 1] = y;
                decay.blocks[at + 2] = z;
                // Goo has no age cap: the record waits as long as the goo stands. Extended right where the entry is
                // kept, not at the end of the pass (a pass spans many ticks, and the last slice may hold no goo).
                decay.deadline = Math.max(decay.deadline, now + FalloutSavedData.Decay.MAX_AGE_TICKS);
            } else {
                Block there = world.getBlock(x, y, z);
                if (there == FalloutBlocks.irradiated()) {
                    world.setBlock(
                        x,
                        y,
                        z,
                        FalloutBlocks.original(FalloutBlocks.variantOf(world.getBlockMetadata(x, y, z))),
                        0,
                        2);
                } else if (there == FalloutBlocks.trinitite()) {
                    world.setBlock(x, y, z, FalloutBlocks.original(FalloutBlocks.SAND), 0, 2);
                }
            }
        }
        d.touch();
        if (decay.cursor >= total) {
            if (decay.kept > 0 && now < decay.deadline) {
                // Retry the unloaded entries later; the age cap (deadline) drops what never loads.
                decay.blocks = java.util.Arrays.copyOf(decay.blocks, decay.kept * 3);
                decay.cursor = 0;
                decay.kept = 0;
                decay.due = now + RETRY_TICKS;
            } else {
                d.decays.remove(decay);
            }
        }
    }

    /** World ticks before a decay pass retries the blocks whose chunks were unloaded (10 seconds). */
    static final long RETRY_TICKS = 200L;

    /** Test hook: every decay record of the world is due now, so its blocks start reverting. */
    public static void expireAll(World world) {
        FalloutSavedData d = data(world, false);
        if (d != null) {
            for (FalloutSavedData.Decay decay : d.decays) {
                decay.due = 0L;
            }
            d.touch();
        }
    }

    /**
     * Test hook: records one irradiated block as a finished blast's decay entry that is not due yet.
     *
     * @param world       a server world
     * @param x           block X
     * @param y           block Y
     * @param z           block Z
     * @param ageCapTicks how long unloaded entries are retried before they are given up on
     */
    public static void trackForTest(World world, int x, int y, int z, long ageCapTicks) {
        FalloutSavedData d = data(world, true);
        FalloutSavedData.Decay decay = new FalloutSavedData.Decay();
        decay.due = world.getTotalWorldTime() + 10_000_000L;
        decay.deadline = world.getTotalWorldTime() + ageCapTicks;
        decay.blocks = new int[] { x, y, z };
        d.decays.add(decay);
        d.touch();
    }

    /** Test hook: forget a world's record. */
    public static void clear(World world) {
        FalloutSavedData d = data(world, false);
        if (d != null) {
            d.jobs.clear();
            d.decays.clear();
            d.touch();
        }
    }

    /** @param player a player @return true when creative */
    static boolean exempt(EntityPlayer player) {
        return player.capabilities.isCreativeMode || !(player instanceof EntityPlayerMP);
    }
}
