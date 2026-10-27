package com.gregguns.icbm.blast;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.ForgeEventFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gregguns.GregGuns;
import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.explosion.Annihilation;
import com.gregguns.explosion.ExplosionRules;
import com.gregguns.explosion.ProjectileExplosion;
import com.gregguns.icbm.explosive.BlastContext;
import com.gregguns.icbm.explosive.BlastShape;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.TickBudget;
import com.mojang.authlib.GameProfile;

/**
 * One running ICBM blast. Its stages are: copy the resistance cube on the server thread (a slab per slice), ray-cast it
 * on a worker thread, post {@code ExplosionEvent.Detonate} and hurt entities on the server thread, then break the
 * crater in slices under the shared tick budget through {@link ProjectileExplosion#breakBlock}, the one block-breaking
 * path of the mod. The world is only ever read or changed on the server thread; the worker sees a snapshot.
 *
 * <p>
 * Avoids: ICBM-02-09 (a cancel flag the worker polls), ICBM-02-11 (the type's finish hook runs when the crater is
 * done, not when it is queued), ICBM-02-59 (the Detonate event carries the real block and entity lists before anything
 * is hurt or broken, so listeners can veto), ICBM-02-95 (unloaded chunks are never loaded, only skipped), ICBM-02-96
 * (every break goes through the protection checks).
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md
final class BlastJob implements BlastTask {

    private static final Logger AUDIT = LogManager.getLogger(ExplosionRules.LOGGER_NAME);
    /** Blocks one slice breaks. */
    static final int BREAKS_PER_SLICE = 48;
    /** Cast cells one decide slice posts to listeners and filters (I3: a nuclear crater is about a million). */
    static final int DECIDE_PER_SLICE = 1024;

    private final World world;
    private final BlastJobState st;
    private final ExplosiveDef def;
    private final TickBudget budget;
    private final BlastContext ctx;

    private BlastGrid grid;
    /** Bytes held in {@link GridMemory}; zero while queued for memory or after the cast. */
    private long reserved;
    private int nextSlab;
    private volatile boolean castDone;
    private volatile boolean cancelled;
    private volatile int[] castResult;
    private volatile Throwable castFailure;
    private Explosion vanilla;
    private EntityPlayer breaker;
    private boolean breakerResolved;
    private int broken;
    private boolean finished;

    BlastJob(World world, BlastJobState state, ExplosiveDef def, TickBudget budget) {
        this.world = world;
        this.st = state;
        this.def = def;
        this.budget = budget;
        this.ctx = new BlastContext(
            world,
            st.x,
            st.y,
            st.z,
            def,
            st.fill,
            st.radius,
            resolveSource(),
            st.effectState,
            budget,
            st.breaksTerrain);
    }

    World world() {
        return world;
    }

    BlastJobState state() {
        return st;
    }

    ExplosiveDef def() {
        return def;
    }

    /** @return blocks still to break */
    int pendingBlocks() {
        return st.stage == BlastJobState.Stage.APPLY ? st.remainingEdits() : 0;
    }

    /** @return true while the crater is not yet decided (snapshot or cast stage) */
    boolean craterUndecided() {
        return st.stage == BlastJobState.Stage.SNAPSHOT || st.stage == BlastJobState.Stage.CAST
            || st.stage == BlastJobState.Stage.DECIDE;
    }

    /** @return true while the crater is being posted to listeners and filtered in slices (the decide stage) */
    boolean deciding() {
        return st.stage == BlastJobState.Stage.DECIDE;
    }

    /** @return blocks broken so far (this run) */
    int brokenBlocks() {
        return broken;
    }

    @Override
    public String label() {
        return stageName();
    }

    String stageName() {
        return st.stage.name() + (st.stage == BlastJobState.Stage.APPLY ? " (" + st.remainingEdits() + " left)" : "");
    }

    boolean isDone() {
        return st.stage == BlastJobState.Stage.DONE;
    }

    void cancel() {
        cancelled = true;
        ctx.markCancelled();
        finish();
    }

    // ---- effect ticking (server thread, once per tick) ----

    void tickEffect() {
        if (isDone()) {
            return;
        }
        if (!st.effectStarted) {
            st.effectStarted = true;
            def.effect()
                .onDetonate(ctx);
        }
        if (st.effectActive) {
            st.effectActive = def.effect()
                .onTick(ctx, st.age);
        }
        st.age++;
        if (st.stage == BlastJobState.Stage.EFFECT && !st.effectActive) {
            finish();
        }
    }

    // ---- terrain work (scheduler slices) ----

    @Override
    public boolean wantsWork() {
        switch (st.stage) {
            case SNAPSHOT:
                return ensureGrid();
            case APPLY:
                return true;
            case CAST:
                return castDone;
            case DECIDE:
                return ensureDecideMemory();
            default:
                return false;
        }
    }

    @Override
    public boolean step() {
        try {
            switch (st.stage) {
                case SNAPSHOT:
                    snapshotSlice();
                    break;
                case CAST:
                    afterCast();
                    break;
                case DECIDE:
                    decideSlice();
                    break;
                case APPLY:
                    applySlice();
                    break;
                default:
                    break;
            }
        } catch (RuntimeException e) {
            GregGuns.LOG.error("ICBM blast {} failed; dropping it", def.id(), e);
            cancel();
        }
        return isDone();
    }

    private void snapshotSlice() {
        boolean cast = st.breaksTerrain && def.shape() != BlastShape.NONE;
        if (!cast) {
            beginDecide(new int[0]);
            return;
        }
        if (!ensureGrid()) {
            return;
        }
        fillSlab(nextSlab);
        nextSlab++;
        if (nextSlab > grid.radius()) {
            st.stage = BlastJobState.Stage.CAST;
            submitCast();
        }
    }

    /**
     * Reserves and allocates the cube. Returns false while the shared memory cap keeps the blast queued, so the
     * scheduler skips it until other blasts finish their casts.
     */
    private boolean ensureGrid() {
        if (grid != null) {
            return true;
        }
        if (!st.breaksTerrain || def.shape() == BlastShape.NONE) {
            return true;
        }
        int radius = Math.max(1, Math.min((int) Math.ceil(st.radius), 255));
        long bytes = GridMemory.bytesFor(radius);
        if (!GridMemory.shared()
            .tryReserve(bytes, IcbmConfig.maxGridMemoryMb * 1024L * 1024L)) {
            return false;
        }
        reserved = bytes;
        grid = new BlastGrid(radius);
        nextSlab = -radius;
        return true;
    }

    private void releaseGrid() {
        grid = null;
        if (reserved > 0) {
            GridMemory.shared()
                .release(reserved);
            reserved = 0L;
        }
    }

    private void fillSlab(int ox) {
        int cx = (int) Math.floor(st.x);
        int cy = (int) Math.floor(st.y);
        int cz = (int) Math.floor(st.z);
        int radius = grid.radius();
        long r2 = (long) radius * radius;
        int wx = cx + ox;
        for (int oz = -radius; oz <= radius; oz++) {
            int wz = cz + oz;
            boolean loaded = world.blockExists(wx, 64, wz);
            for (int oy = -radius; oy <= radius; oy++) {
                if ((long) ox * ox + (long) oy * oy + (long) oz * oz > r2) {
                    continue;
                }
                int wy = cy + oy;
                if (!loaded || wy < 0 || wy > 255) {
                    grid.set(ox, oy, oz, BlastGrid.UNLOADED);
                    continue;
                }
                Block block = world.getBlock(wx, wy, wz);
                if (block.getMaterial() == net.minecraft.block.material.Material.air) {
                    continue;
                }
                if (block.getBlockHardness(world, wx, wy, wz) < 0.0F) {
                    grid.set(ox, oy, oz, BlastGrid.UNBREAKABLE);
                } else if (block.getMaterial()
                    .isLiquid()) {
                        grid.set(ox, oy, oz, BlastGrid.encode((float) BlastRayCaster.LIQUID_COST));
                    } else {
                        // The full-signature resistance, so GregTech's override is used (GGS-04-13).
                        float res = block.getExplosionResistance(null, world, wx, wy, wz, st.x, st.y, st.z);
                        grid.set(ox, oy, oz, BlastGrid.encode(res));
                    }
            }
        }
    }

    private void submitCast() {
        final BlastGrid g = grid;
        final int radius = (int) Math.ceil(st.radius);
        final double fx = st.x - Math.floor(st.x);
        final double fy = st.y - Math.floor(st.y);
        final double fz = st.z - Math.floor(st.z);
        final BlastShape shape = def.shape();
        // D59: the default energy is calibrated to stone for both casters, so the radius is the crater radius in stone.
        final double energy = def.rayEnergy() > 0.0 ? def.rayEnergy()
            : shape == BlastShape.RAY_GRID ? BlastRayCaster.gridEnergy(st.radius)
                : BlastRayCaster.defaultEnergy(st.radius);
        final long seed = world.rand.nextLong();
        BlastWorkers.submit(() -> {
            try {
                Random rng = new Random(seed);
                int[] out = shape == BlastShape.RAY_GRID
                    ? BlastRayCaster.castGrid(g, radius, fx, fy, fz, energy, rng, () -> cancelled)
                    : BlastRayCaster.cast(g, radius, fx, fy, fz, energy, rng, () -> cancelled);
                castResult = out;
            } catch (Throwable t) {
                castFailure = t;
            } finally {
                castDone = true;
            }
        });
    }

    private void afterCast() {
        // The cube is dead weight now; the cast result takes its place under the same memory cap (ensureDecideMemory).
        releaseGrid();
        if (castFailure != null) {
            GregGuns.LOG.error("ICBM blast {} ray cast failed", def.id(), castFailure);
            finish();
            return;
        }
        beginDecide(castResult == null ? new int[0] : castResult);
        castResult = null;
    }

    /**
     * Holds the cast result (4 bytes a cell, at most the 2.1 bytes a cube cell cost, so within what the cube had
     * reserved) in {@link GridMemory} while the crater is decided. Returns false while the cap keeps it queued; a lone
     * blast always passes, as for the cube.
     */
    private boolean ensureDecideMemory() {
        if (reserved > 0 || st.edits.length == 0) {
            return true;
        }
        long bytes = 4L * st.edits.length;
        if (!GridMemory.shared()
            .tryReserve(bytes, IcbmConfig.maxGridMemoryMb * 1024L * 1024L)) {
            return false;
        }
        reserved = bytes;
        return true;
    }

    /** Hands the cast result to the decide stage; nothing is posted yet. */
    private void beginDecide(int[] offsets) {
        st.edits = offsets;
        st.cursor = 0;
        st.kept = 0;
        st.stage = BlastJobState.Stage.DECIDE;
    }

    /**
     * One slice of the decide stage (server thread): posts {@code ExplosionEvent.Detonate} for the next
     * {@link #DECIDE_PER_SLICE} cast cells, so claim listeners may veto cells, and compacts the survivors to the front
     * of {@code edits} in place. The first slice also carries the victim list and hurts them, once ({@code damaged}).
     * Later slices carry an empty victim list. Both cursors are saved, so a reload resumes where it stopped.
     */
    @SuppressWarnings("unchecked")
    private void decideSlice() {
        EntityLivingBase source = ctx.source();
        int[] offsets = st.edits;
        int cx = (int) Math.floor(st.x);
        int cy = (int) Math.floor(st.y);
        int cz = (int) Math.floor(st.z);
        int end = Math.min(offsets.length, st.cursor + DECIDE_PER_SLICE);
        Explosion vanilla = new Explosion(world, null, st.x, st.y, st.z, (float) st.radius);
        vanilla.isSmoking = st.breaksTerrain;
        for (int i = st.cursor; i < end; i++) {
            int p = offsets[i];
            vanilla.affectedBlockPositions
                .add(new ChunkPosition(cx + BlastPacking.x(p), cy + BlastPacking.y(p), cz + BlastPacking.z(p)));
        }
        List<Entity> victims = java.util.Collections.emptyList();
        if (!st.damaged) {
            double reach = st.radius + 1.0;
            victims = ExplosionRules.victims(
                world.getEntitiesWithinAABBExcludingEntity(
                    null,
                    AxisAlignedBB.getBoundingBox(
                        st.x - reach,
                        st.y - reach,
                        st.z - reach,
                        st.x + reach,
                        st.y + reach,
                        st.z + reach)),
                (Entity) source,
                ExplosionsConfig.selfDamage);
        }
        ForgeEventFactory.onExplosionDetonate(world, vanilla, victims, st.radius * 2.0);
        if (!st.damaged) {
            st.damaged = true;
            hurt(victims, source);
        }
        // Keep the listeners' survivors, in the original nearest-first order, packed at the front.
        Set<ChunkPosition> keptCells = new HashSet<>(vanilla.affectedBlockPositions);
        int write = st.kept;
        for (int i = st.cursor; i < end; i++) {
            int p = offsets[i];
            if (keptCells
                .contains(new ChunkPosition(cx + BlastPacking.x(p), cy + BlastPacking.y(p), cz + BlastPacking.z(p)))) {
                offsets[write++] = p;
            }
        }
        st.kept = write;
        st.cursor = end;
        if (st.cursor < offsets.length) {
            return;
        }
        st.edits = java.util.Arrays.copyOf(offsets, st.kept);
        st.cursor = 0;
        st.kept = 0;
        if (reserved > 0L) {
            GridMemory.shared()
                .release(reserved);
            reserved = 0L;
        }
        st.stage = st.edits.length > 0 ? BlastJobState.Stage.APPLY : BlastJobState.Stage.EFFECT;
        if (st.edits.length == 0) {
            effectOnlyDone();
        }
    }

    private void effectOnlyDone() {
        if (!st.effectActive) {
            finish();
        }
    }

    private void hurt(List<Entity> victims, EntityLivingBase source) {
        double center = def.damage(st.fill);
        for (Entity victim : victims) {
            if (victim.isDead) {
                continue;
            }
            double dx = victim.posX - st.x;
            double dy = victim.posY - st.y;
            double dz = victim.posZ - st.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (def.annihilates() && victim instanceof EntityLivingBase
                && Annihilation.inKillZone(distance, st.radius)) {
                DamageSources.annihilateLiving((EntityLivingBase) victim, source, def.annihilatesBosses());
                continue;
            }
            double amount = DamageRamp.at(center, st.radius, distance);
            if (amount > 0.0) {
                DamageSources.strike(victim, DamageType.EXPLOSION, source, (float) amount, 0.0);
            }
            if (!def.ownsMotion()) {
                push(victim, dx, victim.posY + victim.height / 2.0 - st.y, dz);
            }
        }
    }

    /** Shared knockback for every conventional blast: away from the center, scaled by {@link Knockback}. */
    private void push(Entity victim, double dx, double dy, double dz) {
        if (victim.isDead) {
            return;
        }
        double[] v = Knockback.velocity(st.radius, dx, dy, dz);
        if (v[0] == 0.0 && v[1] == 0.0 && v[2] == 0.0) {
            return;
        }
        double keep = 1.0;
        if (victim instanceof EntityLivingBase) {
            keep -= ((EntityLivingBase) victim).getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
                .getAttributeValue();
        }
        if (keep <= 0.0) {
            return;
        }
        victim.motionX += v[0] * keep;
        victim.motionY += v[1] * keep;
        victim.motionZ += v[2] * keep;
        victim.velocityChanged = true;
        if (victim instanceof EntityPlayerMP) {
            ((EntityPlayerMP) victim).playerNetServerHandler.sendPacket(
                ProjectileExplosion.pushPacket(
                    st.x,
                    st.y,
                    st.z,
                    (float) st.radius,
                    Vec3.createVectorHelper(v[0] * keep, v[1] * keep, v[2] * keep)));
        }
    }

    private void applySlice() {
        if (vanilla == null) {
            vanilla = new Explosion(world, null, st.x, st.y, st.z, (float) st.radius);
            vanilla.isSmoking = true;
        }
        if (!breakerResolved) {
            breakerResolved = true;
            breaker = resolveBreaker();
        }
        int cx = (int) Math.floor(st.x);
        int cy = (int) Math.floor(st.y);
        int cz = (int) Math.floor(st.z);
        int end = Math.min(st.edits.length, st.cursor + BREAKS_PER_SLICE);
        for (; st.cursor < end; st.cursor++) {
            int p = st.edits[st.cursor];
            int x = cx + BlastPacking.x(p);
            int y = cy + BlastPacking.y(p);
            int z = cz + BlastPacking.z(p);
            // Never load a chunk to break a block in it (avoids ICBM-02-95).
            if (y < 0 || y > 255 || !world.blockExists(x, y, z)) {
                continue;
            }
            if (ProjectileExplosion
                .breakBlock(world, vanilla, breaker, (float) st.radius, x, y, z, IcbmConfig.blastDrops)) {
                broken++;
            }
        }
        if (st.cursor >= st.edits.length) {
            st.edits = new int[0];
            st.cursor = 0;
            st.stage = BlastJobState.Stage.EFFECT;
            if (!st.effectActive) {
                finish();
            }
        }
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        cancelled = true;
        st.stage = BlastJobState.Stage.DONE;
        releaseGrid();
        try {
            def.effect()
                .onFinish(ctx);
        } catch (RuntimeException e) {
            GregGuns.LOG.error("ICBM effect {} onFinish failed", def.id(), e);
        }
        if (broken > 0) {
            EntityLivingBase source = ctx.source();
            AUDIT.info(
                ExplosionRules.logLine(
                    world.provider.dimensionId,
                    st.x,
                    st.y,
                    st.z,
                    st.radius,
                    source == null ? null : source.getCommandSenderName(),
                    "icbm:" + def.id(),
                    broken));
        }
    }

    // ---- who did it ----

    private EntityLivingBase resolveSource() {
        if (st.sourceUuid.isEmpty()) {
            return null;
        }
        UUID id;
        try {
            id = UUID.fromString(st.sourceUuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
        EntityPlayer player = world.func_152378_a(id);
        if (player != null) {
            return player;
        }
        for (Object o : world.loadedEntityList) {
            if (o instanceof EntityLivingBase && ((Entity) o).getUniqueID()
                .equals(id)) {
                return (EntityLivingBase) o;
            }
        }
        return null;
    }

    /** The player breaks are checked against: the source, or a fake player of an offline owner; null for an NPC. */
    private EntityPlayer resolveBreaker() {
        if (st.sourceUuid.isEmpty() || st.sourceName.isEmpty()) {
            return ctx.source() instanceof EntityPlayer ? (EntityPlayer) ctx.source() : null;
        }
        EntityLivingBase live = resolveSource();
        if (live instanceof EntityPlayer) {
            return (EntityPlayer) live;
        }
        if (live != null) {
            return null;
        }
        if (world instanceof net.minecraft.world.WorldServer && st.sourceName.startsWith("player:")) {
            return FakePlayerFactory.get(
                (net.minecraft.world.WorldServer) world,
                new GameProfile(UUID.fromString(st.sourceUuid), st.sourceName.substring("player:".length())));
        }
        return null;
    }
}
