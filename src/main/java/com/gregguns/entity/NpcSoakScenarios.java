package com.gregguns.entity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import com.gregguns.GregGuns;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.entity.npc.SoakStats;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The NPC soak (M7 exit box 4): 100 mixed NPCs run for 2400 ticks beside a fake player while the server's tick time is
 * sampled. Dev environment only. Server side. Opt-in: it takes about two minutes of game time, so
 * {@code gregguns test all} skips it; run it with {@code gregguns test npc.soak}.
 *
 * <p>
 * Natural spawning stays as configured (the module being on is what the box asks for), and the fake player is in the
 * world's player list, so vanilla spawn cycles run around it too. Anything spawned that way is swept at the end.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/m7-exit-gate.md (box 4), docs/ROADMAP.md (M7 exit)
final class NpcSoakScenarios {

    /** Ticks discarded at the start while the fake player and the arena settle. */
    static final int WARMUP_TICKS = 40;
    /** Ticks sampled with no GregGuns NPCs, the baseline. */
    static final int BASELINE_TICKS = 200;
    /** Ticks sampled with the NPCs running (the 2400 ticks of PLAN-M7). */
    static final int LOAD_TICKS = 2400;
    /** NPCs spawned for the load phase. */
    static final int NPC_COUNT = 100;
    /** Ticks between entity-count log lines. */
    static final int LOG_INTERVAL = 200;
    /** Extra ticks the run may take beyond the phases before the harness fails it as timed out. */
    private static final int TIMEOUT_SLACK = 400;

    /** Absolute cap on the load phase's mean tick time: 50 ms is 20 TPS. */
    static final double BUDGET_MEAN_MS = 50.0;
    /**
     * Largest allowed rise of the mean tick time over the baseline, in ms. An absolute margin, not a ratio: an idle
     * baseline is a millisecond or two, where a percentage would flag noise. Override with
     * {@code -Dgregguns.soak.regressionMs=<ms>}.
     */
    static final double MAX_REGRESSION_MS = 10.0;
    /** System property that overrides {@link #MAX_REGRESSION_MS}. */
    static final String REGRESSION_PROPERTY = "gregguns.soak.regressionMs";

    /** Spawn ring around the player, in blocks (inside the default 32 block arena). */
    private static final double RING_MIN = 6.0;
    private static final double RING_MAX = 24.0;
    /** Half-size in blocks of the box swept for strays at the end. */
    private static final double SWEEP_RADIUS = 64.0;
    /** Length of the {@code MinecraftServer.tickTimeArray} ring buffer. */
    private static final int TICK_ARRAY = 100;

    /**
     * The mix: ordinary mobs, ranged shooters included. Bosses (persistent kinds, the Cyberdemon), the turret and the
     * dev test NPC are left out. Mobs not registered in this run are skipped.
     */
    private static final NpcMob[] MIX = { NpcMob.ZOMBIE_SOLDIER, NpcMob.ZOMBIE_FARMER, NpcMob.ZOMBIE_MINER,
        NpcMob.ZOMBIE_PIGMAN_SOLDIER, NpcMob.SKELETON_SOLDIER, NpcMob.ARMY_SOLDIER, NpcMob.COMMANDO, NpcMob.BANDIT,
        NpcMob.ALIEN_BUG, NpcMob.SUPER_MUTANT_BASIC, NpcMob.SUPER_MUTANT_HEAVY, NpcMob.SUPER_MUTANT_ELITE };

    private NpcSoakScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new SoakScenario());
    }

    /** Cancels the drops of every NPC so the soak leaves no item entities behind. */
    public static final class DropCanceller {

        private boolean active = true;
        private final long deadline;
        private final World world;

        DropCanceller(World world, long deadline) {
            this.world = world;
            this.deadline = deadline;
        }

        @SubscribeEvent
        public void onDrops(LivingDropsEvent event) {
            if (event.entityLiving instanceof NpcEntity) {
                event.setCanceled(true);
            }
            if (world.getTotalWorldTime() > deadline) {
                stop(); // the run timed out without finishing: do not stay registered
            }
        }

        void stop() {
            if (active) {
                active = false;
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }
    }

    /** The mutable state of one soak run, owned by its poller. */
    private static final class Run {

        final ScenarioContext ctx;
        final MinecraftServer server;
        final List<NpcMob> mix;
        final double margin;
        final FakePlayer player;
        final List<Entity> preexisting;
        final DropCanceller drops;
        final double[] baseline = new double[BASELINE_TICKS];
        final double[] load = new double[LOAD_TICKS];
        final List<Entity> spawned = new ArrayList<>();
        int baselineCount;
        int loadCount;
        int lastCounter;
        int peak;

        Run(ScenarioContext ctx, MinecraftServer server, List<NpcMob> mix, double margin, FakePlayer player,
            List<Entity> preexisting, DropCanceller drops) {
            this.ctx = ctx;
            this.server = server;
            this.mix = mix;
            this.margin = margin;
            this.player = player;
            this.preexisting = preexisting;
            this.drops = drops;
            this.lastCounter = server.getTickCounter();
        }

        int alive() {
            int n = 0;
            for (Entity e : spawned) {
                if (!e.isDead) {
                    n++;
                }
            }
            return n;
        }
    }

    private static final class SoakScenario implements Scenario {

        @Override
        public String name() {
            return "npc.soak";
        }

        @Override
        public String description() {
            return "Opt-in (not in 'test all'): 100 mixed NPCs for 2400 ticks; mean tick time under 50 ms and within"
                + " the margin of the no-NPC baseline.";
        }

        @Override
        public boolean includedInAll() {
            return false;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null || !(world instanceof WorldServer)) {
                return ScenarioResult.fail(name(), "needs a running server world");
            }
            double margin = Double
                .parseDouble(System.getProperty(REGRESSION_PROPERTY, Double.toString(MAX_REGRESSION_MS)));
            List<NpcMob> mix = new ArrayList<>();
            for (NpcMob mob : MIX) {
                if (NpcRegistry.isRegistered(mob)) {
                    mix.add(mob);
                }
            }
            if (mix.isEmpty()) {
                return ScenarioResult.fail(name(), "no NPC mob is registered; is modules.npcs on?");
            }

            FakePlayer player = new FakePlayer(
                (WorldServer) world,
                new GameProfile(UUID.nameUUIDFromBytes("npc_soak".getBytes(StandardCharsets.UTF_8)), "npc_soak"));
            player.capabilities.disableDamage = true;
            // The entity tracker crashes sending a connectionless fake player's metadata.
            new NetHandlerPlayServer(server, new NetworkManager(false), player);
            player.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            ctx.spawn(player);

            int total = WARMUP_TICKS + BASELINE_TICKS + LOAD_TICKS;
            DropCanceller drops = new DropCanceller(world, world.getTotalWorldTime() + total + TIMEOUT_SLACK);
            MinecraftForge.EVENT_BUS.register(drops);
            Run run = new Run(ctx, server, mix, margin, player, npcsAround(ctx), drops);

            return ScenarioResult.pending(name(), total + TIMEOUT_SLACK, ticks -> {
                try {
                    return step(run, ticks);
                } catch (RuntimeException | AssertionError e) {
                    cleanUp(run);
                    throw e;
                }
            });
        }

        /** One server tick of the soak; returns the final result on the last tick, otherwise null. */
        private ScenarioResult step(Run run, int ticks) {
            // MinecraftServer.tickTimeArray[n % 100] holds tick n, written at the end of that tick; the newest
            // finished tick is therefore the one before the counter. Each is read once.
            int counter = run.server.getTickCounter();
            boolean fresh = counter != run.lastCounter;
            run.lastCounter = counter;
            double lastMs = run.server.tickTimeArray[Math.floorMod(counter - 1, TICK_ARRAY)] / 1.0e6;

            int loadStart = WARMUP_TICKS + BASELINE_TICKS;
            if (ticks <= WARMUP_TICKS) {
                return null;
            }
            if (ticks <= loadStart) {
                if (fresh && run.baselineCount < BASELINE_TICKS) {
                    run.baseline[run.baselineCount++] = lastMs;
                }
                if (ticks == loadStart) {
                    spawnNpcs(run);
                    GregGuns.LOG.info(
                        "npc.soak: baseline {}; spawned {} NPCs",
                        SoakStats.of(trim(run.baseline, run.baselineCount)),
                        run.spawned.size());
                }
                return null;
            }

            if (fresh && run.loadCount < LOAD_TICKS) {
                run.load[run.loadCount++] = lastMs;
            }
            int alive = run.alive();
            run.peak = Math.max(run.peak, alive);
            int into = ticks - loadStart;
            if (into % LOG_INTERVAL == 0) {
                GregGuns.LOG.info(
                    "npc.soak: tick {}/{}: {} of {} NPCs alive, last tick {} ms",
                    into,
                    LOAD_TICKS,
                    alive,
                    run.spawned.size(),
                    String.format("%.2f", lastMs));
            }
            if (into < LOAD_TICKS) {
                return null;
            }

            // Last load tick: judge, then clean up.
            int swept = cleanUp(run);
            SoakStats base = SoakStats.of(trim(run.baseline, run.baselineCount));
            SoakStats loaded = SoakStats.of(trim(run.load, run.loadCount));
            String npcs = run.spawned.size() + " spawned, peak "
                + run.peak
                + " alive, "
                + alive
                + " alive at the end, "
                + swept
                + " natural strays swept";
            GregGuns.LOG.info("npc.soak: baseline {}", base);
            GregGuns.LOG.info("npc.soak: load {}; NPCs: {}", loaded, npcs);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.note("baseline: " + base);
            out.note("load: " + loaded);
            out.note("NPCs: " + npcs);
            String fail = SoakStats.verdict(base, loaded, BUDGET_MEAN_MS, run.margin);
            out.check(fail == null, fail == null ? "" : fail);
            return out.build();
        }

        private static double[] trim(double[] a, int n) {
            double[] out = new double[n];
            System.arraycopy(a, 0, out, 0, n);
            return out;
        }

        private void spawnNpcs(Run run) {
            Random rnd = run.ctx.random();
            World world = run.ctx.world();
            for (int i = 0; i < NPC_COUNT; i++) {
                NpcMob mob = run.mix.get(i % run.mix.size());
                Entity e = EntityList.createEntityByName(mob.fullName(), world);
                if (!(e instanceof EntityLiving)) {
                    continue;
                }
                double angle = rnd.nextDouble() * Math.PI * 2;
                double dist = RING_MIN + rnd.nextDouble() * (RING_MAX - RING_MIN);
                e.setLocationAndAngles(
                    run.ctx.originX() + 0.5 + Math.cos(angle) * dist,
                    run.ctx.originY(),
                    run.ctx.originZ() + 0.5 + Math.sin(angle) * dist,
                    rnd.nextFloat() * 360.0F,
                    0.0F);
                ((EntityLiving) e).onSpawnWithEgg(null);
                run.ctx.spawn(e);
                run.spawned.add(e);
            }
        }

        private static List<Entity> npcsAround(ScenarioContext ctx) {
            List<Entity> out = new ArrayList<>();
            for (Object o : ctx.world()
                .getEntitiesWithinAABB(NpcEntity.class, sweepBox(ctx))) {
                out.add((Entity) o);
            }
            return out;
        }

        private static AxisAlignedBB sweepBox(ScenarioContext ctx) {
            return AxisAlignedBB.getBoundingBox(
                ctx.originX() - SWEEP_RADIUS,
                0,
                ctx.originZ() - SWEEP_RADIUS,
                ctx.originX() + SWEEP_RADIUS,
                256,
                ctx.originZ() + SWEEP_RADIUS);
        }

        /**
         * Removes everything the soak made with {@code World.removeEntity} (not just {@code setDead}): the NPCs, any
         * NPC that spawned naturally nearby meanwhile, stray experience orbs and the fake player.
         *
         * @return how many natural stray NPCs were swept
         */
        private int cleanUp(Run run) {
            World world = run.ctx.world();
            run.drops.stop();
            for (Entity e : run.spawned) {
                world.removeEntity(e);
                run.ctx.despawn(e);
            }
            int swept = 0;
            for (Object o : world.getEntitiesWithinAABB(NpcEntity.class, sweepBox(run.ctx))) {
                if (!run.preexisting.contains(o)) {
                    world.removeEntity((Entity) o);
                    swept++;
                }
            }
            for (Object o : world.getEntitiesWithinAABB(EntityXPOrb.class, sweepBox(run.ctx))) {
                world.removeEntity((Entity) o);
            }
            world.removeEntity(run.player);
            run.ctx.despawn(run.player);
            return swept;
        }
    }
}
