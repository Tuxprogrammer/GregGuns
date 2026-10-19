package com.gregguns.explosion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Finishes the block breaks of large blasts, one batch per server tick (nuke scale). A blast that reaches
 * more than {@link ExplosionRules#BLOCKS_PER_TICK} blocks breaks that many at once and hands the rest here, nearest
 * first, so the fireball grows outward and no single tick does thousands of block updates. Server side.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/m5-exit-gate.md
public final class BlastTicker {

    private static final class Job {

        final World world;
        final Explosion vanilla;
        final EntityPlayer player;
        final float size;
        final BlastSlicer slicer;

        Job(World world, Explosion vanilla, EntityPlayer player, float size, BlastSlicer slicer) {
            this.world = world;
            this.vanilla = vanilla;
            this.player = player;
            this.size = size;
            this.slicer = slicer;
        }
    }

    /** The one instance; registered on the FML bus by {@code GunsModule}. Holds queue state, not gameplay config. */
    private static final BlastTicker INSTANCE = new BlastTicker();

    private final List<Job> jobs = new ArrayList<>();

    private BlastTicker() {}

    /** @return the shared ticker */
    public static BlastTicker instance() {
        return INSTANCE;
    }

    /**
     * Queues the blocks that did not fit in this tick.
     *
     * @param world   the world
     * @param vanilla the explosion, for drop rules and {@code onBlockExploded}
     * @param player  who the breaks are on behalf of, or {@code null} for an NPC
     * @param size    the vanilla power, drives drop chance
     * @param slicer  the blocks still to break
     */
    void enqueue(World world, Explosion vanilla, EntityPlayer player, float size, BlastSlicer slicer) {
        jobs.add(new Job(world, vanilla, player, size, slicer));
    }

    /** @return blocks still waiting across every queued blast (for tests and scenarios) */
    public int pendingBlocks() {
        int n = 0;
        for (Job j : jobs) {
            n += j.slicer.remaining();
        }
        return n;
    }

    /** Breaks one batch per queued blast at the end of each server tick. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || jobs.isEmpty()) {
            return;
        }
        Iterator<Job> it = jobs.iterator();
        while (it.hasNext()) {
            Job job = it.next();
            if (DimensionManager.getWorld(job.world.provider.dimensionId) != job.world) {
                it.remove(); // the world unloaded: drop the rest rather than keep it alive
                continue;
            }
            try {
                for (int[] p : job.slicer.next()) {
                    ProjectileExplosion.breakOne(job.world, job.vanilla, job.player, job.size, p[0], p[1], p[2]);
                }
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Time-sliced explosion break failed; dropping the rest of the blast", e);
                it.remove();
                continue;
            }
            if (job.slicer.done()) {
                it.remove();
            }
        }
    }

    /** Test hook: forget everything queued. */
    public void clear() {
        jobs.clear();
    }
}
