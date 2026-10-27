package com.gregguns.icbm.blast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.WorldEvent;

import com.gregguns.GregGuns;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.explosion.ProjectileExplosion;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.TickBudget;
import com.gregguns.icbm.fx.CloudBroadcast;
import com.gregguns.icbm.fx.CloudKind;
import com.gregguns.icbm.fx.CloudPlan;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Runs every ICBM blast: accepts detonations, shares one {@link TickBudget} between all running blasts, saves blasts in
 * progress with the world and resumes them when it loads. Server side. Register the instance on both the FML and Forge
 * event buses.
 *
 * <p>
 * Avoids: ICBM-02-06 (the budget runs once per tick, at the end phase only), ICBM-02-08 (world unload cancels a copy of
 * the job list, never the list being iterated).
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md
public final class BlastEngine {

    private static final BlastEngine INSTANCE = new BlastEngine();

    private final List<BlastJob> jobs = new ArrayList<>();
    private final BlastScheduler scheduler = new BlastScheduler();
    private final TickBudget budget = new TickBudget(System::nanoTime);
    private final Map<World, BlastSavedData> data = new WeakHashMap<>();
    private int lastSlices;
    private long maxTickNanos;
    private BlastJob lastStarted;
    private String maxTickStage = "none";
    private boolean recording;
    private final List<Long> tickLog = new ArrayList<>();
    private long slowestSliceNanos;
    private String slowestSliceLabel = "none";

    private BlastEngine() {}

    /** Registers the engine on the FML bus (ticks) and the Forge bus (world load and unload). Call once, at init. */
    public static void register() {
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    /** @return the one engine */
    public static BlastEngine instance() {
        return INSTANCE;
    }

    /**
     * Starts a blast; the entry behind {@code IcbmBlasts.detonate}.
     *
     * @return true when accepted
     */
    public boolean start(World world, double x, double y, double z, ExplosiveDef def, int fill, double bonusRadius,
        EntityLivingBase source) {
        if (world.isRemote || def == null
            || def.tier()
                .number() > IcbmConfig.maxBlastTier
            || jobs.size() >= IcbmConfig.maxActiveBlasts) {
            return false;
        }
        int f = Math.max(1, Math.min(fill, def.maxFill()));
        double radius = Math.min(IcbmConfig.maxBlastRadius, def.radius(f) + Math.max(0.0, bonusRadius));
        Explosion probe = new Explosion(world, null, x, y, z, (float) radius);
        if (ForgeEventFactory.onExplosionStart(world, probe)) {
            return false;
        }
        BlastJobState st = new BlastJobState();
        st.explosiveId = def.id();
        st.x = x;
        st.y = y;
        st.z = z;
        st.fill = f;
        st.radius = radius;
        if (source != null) {
            st.sourceUuid = source.getUniqueID()
                .toString();
            st.sourceName = source instanceof EntityPlayer ? "player:" + source.getCommandSenderName() : "";
        }
        // D52: only the per-tier icbm.terrainDamageT<n> switch, never explosions.blockDamage (hand-held weapons).
        st.breaksTerrain = TerrainRule.breaks(
            def.shape(),
            def.tier()
                .number(),
            source instanceof EntityPlayer,
            world.getGameRules()
                .getGameRuleBooleanValue("mobGriefing"));
        lastStarted = new BlastJob(world, st, def, budget);
        jobs.add(lastStarted);
        // Only annihilating explosives get the long-range flash; every other blast uses the normal effects.
        ProjectileExplosion.blastEffects(world, x, y, z, (float) radius, def.annihilates());
        // A large conventional blast gets a smoke plume of its own, sized by its radius (the flash above is for
        // annihilating explosives only). The gas warheads announce their own clouds (Effects.Gas).
        int whole = (int) Math.ceil(radius);
        if (CloudPlan
            .wantsPlume(whole, def.annihilates(), def.shape() != com.gregguns.icbm.explosive.BlastShape.NONE)) {
            int life = CloudPlan.duration(CloudKind.PLUME, whole, 0);
            CloudBroadcast.send(world, CloudKind.PLUME, x, y, z, whole, life, life);
        }
        touch(world);
        return true;
    }

    /**
     * Ends the running blasts of one explosive near a point, as a canceled blast (no aftermath). Antimatter uses it to
     * annihilate red matter (D133). Safe to call from an effect while the engine iterates: a canceled job only turns
     * done and is dropped at the end of the tick.
     *
     * @param world       the world
     * @param explosiveId the explosive to end
     * @param x           center X
     * @param y           center Y
     * @param z           center Z
     * @param reach       blocks
     * @return how many blasts were ended
     */
    public int cancelNear(World world, String explosiveId, double x, double y, double z, double reach) {
        int n = 0;
        for (BlastJob j : new ArrayList<>(jobs)) {
            if (j.world() != world || j.isDone()
                || !j.def()
                    .id()
                    .equals(explosiveId)) {
                continue;
            }
            BlastJobState s = j.state();
            double dx = s.x - x;
            double dy = s.y - y;
            double dz = s.z - z;
            if (dx * dx + dy * dy + dz * dz <= reach * reach) {
                j.cancel();
                n++;
            }
        }
        return n;
    }

    /** @return a copy of the effect state of the blast most recently accepted (scenarios), empty before the first */
    public NBTTagCompound lastEffectState() {
        return lastStarted == null ? new NBTTagCompound() : (NBTTagCompound) lastStarted.state().effectState.copy();
    }

    /** @return blasts running now */
    public int activeBlasts() {
        return jobs.size();
    }

    /**
     * @param explosiveId an explosive id
     * @return blasts of that explosive running now (scenarios)
     */
    public int activeBlasts(String explosiveId) {
        int n = 0;
        for (BlastJob j : jobs) {
            if (!j.isDone() && j.def()
                .id()
                .equals(explosiveId)) {
                n++;
            }
        }
        return n;
    }

    /**
     * @return true when the blast most recently accepted by {@link #start} has finished (or was canceled), whatever
     *         other blasts are running; true when none was ever started. Scenarios wait on this, not on the
     *         engine-wide counters.
     */
    public boolean lastBlastDone() {
        return lastStarted == null || lastStarted.isDone();
    }

    /** @return the stage name of the blast most recently accepted, for failure messages ("none" before the first) */
    public String lastBlastStage() {
        return lastStarted == null ? "none" : lastStarted.stageName();
    }

    /** @return blocks still waiting to break across every blast */
    public int pendingBlocks() {
        int n = 0;
        for (BlastJob j : jobs) {
            n += j.pendingBlocks();
        }
        return n;
    }

    /** @return blasts whose crater is not decided yet (still copying or ray casting), so none of it is pending */
    public int undecidedBlasts() {
        int n = 0;
        for (BlastJob j : jobs) {
            if (j.craterUndecided()) {
                n++;
            }
        }
        return n;
    }

    /** @return blasts in the decide stage now: their cast crater is being posted to listeners in slices */
    public int decidingBlasts() {
        int n = 0;
        for (BlastJob j : jobs) {
            if (j.deciding()) {
                n++;
            }
        }
        return n;
    }

    /** @return slices the shared scheduler ran in the last tick (tests and scenarios) */
    public int lastSlices() {
        return lastSlices;
    }

    /** @return the longest time one tick spent on the scheduler since {@link #resetStats()}, nanoseconds */
    public long maxTickNanos() {
        return maxTickNanos;
    }

    /** @return the stage the first running blast was in after the tick behind {@link #maxTickNanos()} (diagnostics) */
    public String maxTickStage() {
        return maxTickStage;
    }

    /**
     * Test hook: while on, records the scheduler time of every tick and the slowest single slice, to tell a slice that
     * overruns (an engine problem) from a pause the slices cannot control.
     */
    public void recordTicks(boolean on) {
        tickLog.clear();
        slowestSliceNanos = 0L;
        slowestSliceLabel = "none";
        scheduler.observe(on ? (label, nanos) -> {
            if (nanos > slowestSliceNanos) {
                slowestSliceNanos = nanos;
                slowestSliceLabel = label;
            }
        } : null);
        recording = on;
    }

    /** @return the scheduler time of each tick since {@link #recordTicks}, nanoseconds, in tick order */
    public long[] tickNanos() {
        long[] a = new long[tickLog.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = tickLog.get(i);
        }
        return a;
    }

    /** @return the slowest single slice since {@link #recordTicks}: its stage and time */
    public String slowestSlice() {
        return slowestSliceLabel + " " + slowestSliceNanos / 1_000_000.0 + " ms";
    }

    /** Test hook: zeroes {@link #maxTickNanos()}. */
    public void resetStats() {
        maxTickNanos = 0L;
        maxTickStage = "none";
    }

    /** Test hook: cancels and forgets every blast. */
    public void clear() {
        for (BlastJob j : new ArrayList<>(jobs)) {
            j.cancel();
        }
        jobs.clear();
    }

    /** Runs the effects and one budgeted round of terrain slices at the end of each server tick. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || jobs.isEmpty()) {
            return;
        }
        Iterator<BlastJob> it = jobs.iterator();
        while (it.hasNext()) {
            BlastJob job = it.next();
            if (DimensionManager.getWorld(job.world().provider.dimensionId) != job.world()) {
                job.cancel();
                it.remove();
                continue;
            }
            try {
                job.tickEffect();
            } catch (RuntimeException e) {
                GregGuns.LOG.error(
                    "ICBM effect {} failed; dropping the blast",
                    job.def()
                        .id(),
                    e);
                job.cancel();
            }
        }
        long began = System.nanoTime();
        budget.open(IcbmConfig.blastBudgetMs * 1_000_000L);
        lastSlices = scheduler.runTick(jobs, budget);
        long spent = System.nanoTime() - began;
        if (recording && lastSlices > 0) {
            tickLog.add(spent);
        }
        if (spent > maxTickNanos) {
            maxTickNanos = spent;
            maxTickStage = jobs.isEmpty() ? "none"
                : jobs.get(0)
                    .stageName();
        }
        List<World> touched = new ArrayList<>();
        for (BlastJob j : jobs) {
            if (!touched.contains(j.world())) {
                touched.add(j.world());
            }
        }
        jobs.removeIf(BlastJob::isDone);
        for (World w : touched) {
            touch(w);
        }
    }

    /** Resumes the world's saved blasts. */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.world.isRemote && event.world instanceof WorldServer) {
            resume(event.world);
        }
    }

    /** Saves and cancels the world's blasts before it goes (a copy is iterated, never the live list). */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.world;
        if (world.isRemote) {
            return;
        }
        boolean any = false;
        for (BlastJob j : new ArrayList<>(jobs)) {
            if (j.world() == world) {
                any = true;
            }
        }
        if (any) {
            try {
                touch(world);
                world.perWorldStorage.saveAllData();
            } catch (RuntimeException e) {
                GregGuns.LOG.error("Could not save ICBM blasts of dimension {}", world.provider.dimensionId, e);
            }
        }
        for (BlastJob j : new ArrayList<>(jobs)) {
            if (j.world() == world) {
                j.cancel();
                jobs.remove(j);
            }
        }
        data.remove(world);
    }

    // ---- save and load ----

    /**
     * @param world a server world
     * @return the NBT list of that world's blasts in progress
     */
    NBTTagList save(World world) {
        NBTTagList list = new NBTTagList();
        for (BlastJob j : jobs) {
            if (j.world() == world && !j.isDone()) {
                list.appendTag(
                    j.state()
                        .write(new NBTTagCompound()));
            }
        }
        return list;
    }

    /**
     * Rebuilds the world's saved blasts and adds them to the running list.
     *
     * @param world a server world
     * @return how many were resumed
     */
    public int resume(World world) {
        BlastSavedData record = recordFor(world);
        NBTTagList saved = record.takePending();
        return resumeFrom(world, saved);
    }

    /**
     * @param world a server world
     * @param saved blasts as written by {@link #save}
     * @return how many were resumed; entries with an unknown explosive are dropped with a warning
     */
    public int resumeFrom(World world, NBTTagList saved) {
        int n = 0;
        for (int i = 0; i < saved.tagCount(); i++) {
            BlastJobState st = BlastJobState.read(saved.getCompoundTagAt(i));
            ExplosiveDef def = Explosives.get(st.explosiveId);
            if (def == null) {
                GregGuns.LOG.warn("Dropping a saved ICBM blast of unknown explosive '{}'", st.explosiveId);
                continue;
            }
            lastStarted = new BlastJob(world, st, def, budget);
            jobs.add(lastStarted);
            n++;
        }
        if (n > 0) {
            touch(world);
        }
        return n;
    }

    /**
     * Test hook: writes the world's blasts the way a world save does and rebuilds them the way a world load does,
     * without touching the disk.
     *
     * @param world a server world
     * @return how many blasts came back
     */
    public int saveAndReload(World world) {
        NBTTagList saved = save(world);
        for (BlastJob j : new ArrayList<>(jobs)) {
            if (j.world() == world) {
                j.cancel();
                jobs.remove(j);
            }
        }
        // Through a real record: write then read, as the storage would.
        BlastSavedData record = new BlastSavedData(BlastSavedData.NAME);
        NBTTagCompound disk = new NBTTagCompound();
        disk.setTag("blasts", saved);
        record.readFromNBT(disk);
        return resumeFrom(world, record.takePending());
    }

    private BlastSavedData recordFor(World world) {
        BlastSavedData record = data.get(world);
        if (record == null) {
            record = (BlastSavedData) world.perWorldStorage.loadData(BlastSavedData.class, BlastSavedData.NAME);
            if (record == null) {
                record = new BlastSavedData(BlastSavedData.NAME);
                world.perWorldStorage.setData(BlastSavedData.NAME, record);
            }
            record.attach(world);
            data.put(world, record);
        }
        return record;
    }

    private void touch(World world) {
        if (world.perWorldStorage != null) {
            recordFor(world).touch();
        }
    }
}
