package com.gregguns.icbm.warhead;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.potion.Potion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.gregguns.combat.DamageFactors;
import com.gregguns.combat.DamageSources;
import com.gregguns.core.config.DamageFactorsConfig;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.explosion.nuclear.Fallout;
import com.gregguns.explosion.nuclear.FalloutBlocks;
import com.gregguns.explosion.nuclear.FalloutEngine;
import com.gregguns.explosion.nuclear.ItemIrradiatedRubble;
import com.gregguns.explosion.nuclear.NuclearScale;
import com.gregguns.explosion.nuclear.RadiationExposure;
import com.gregguns.icbm.blast.BlastEngine;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;

import gregtech.api.enchants.EnchantmentHazmat;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;

/**
 * The I3 nuclear scenarios: the nuclear-radius crater is decided incrementally within the tick budget (and survives a
 * save and reload mid-decide), nothing living survives the kill zone and fallout stays within its caps and decays.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md, blast-engine.md (Incremental crater)
final class NuclearScenarios {

    private NuclearScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new CraterNuclear());
        registry.register(new KillZone());
        registry.register(new FalloutCaps());
        registry.register(new RubbleInert());
        registry.register(new DecayUnloaded());
    }

    /**
     * Makes a FakePlayer behave like a connected survival player for effects and damage: a stub network handler (potion
     * effects and death send packets, and a FakePlayer has none; an unconnected NetworkManager just queues them), and
     * the 60-tick respawn protection EntityPlayerMP counts down in its own tick, which a fake never gets.
     */
    static void connect(EntityPlayerMP p) {
        if (p.playerNetServerHandler == null) {
            new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
        }
        try {
            for (java.lang.reflect.Field f : EntityPlayerMP.class.getDeclaredFields()) {
                if (f.getType() == int.class && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    if (f.getInt(p) == 60 && f.getName()
                        .equals("field_147101_bU")) {
                        f.setInt(p, 0);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not clear the respawn protection", e);
        }
    }

    /** No tick may spend more than this on blasts, however loaded the box: a slice that ignores the clock would. */
    private static final long HARD_CAP_NANOS = 50_000_000L;

    private static boolean off() {
        return WarheadBlocks.of(WarheadCasing.T1) == null || Explosives.get("nuclear") == null;
    }

    /** A nuclear-radius crater: decided in many slices, within the budget, across a reload, and dug at full size. */
    private static final class CraterNuclear implements Scenario {

        @Override
        public String name() {
            return "icbm.blast.crater_nuclear";
        }

        @Override
        public String description() {
            return "A full nuclear warhead (radius 64) over a stone slab: the crater is posted to listeners in slices"
                + " over several ticks (never one pass), no tick spends more than the budget on blasts, a save and"
                + " reload in the middle of the decide stage loses nothing, and the crater reaches the radius.";
        }

        /** Only the harness's bookkeeping slot; the cube is built at {@link #FAR_X} from it (see below). */
        @Override
        public int areaRadius() {
            return 8;
        }

        /**
         * The cube and crater reach 66 blocks from the center, past the harness's largest area (48) and its 80-block
         * slot spacing, so building them in the slot spilled into the next scenario's area (crater_size, the next
         * slot, started straight into a million-block crater). Slots sit at 0..400 blocks east of the base origin, so
         * 600 blocks west of this slot lies at least 133 blocks clear of every slot's area.
         */
        private static final int FAR_X = -600;

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (off()) {
                return ScenarioResult.fail(name(), "the ICBM content is not registered");
            }
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World w = ctx.world();
            final ChunkProviderServer cps = ((WorldServer) w).theChunkProviderServer;
            final ExplosiveDef def = Explosives.get("nuclear");
            final int r = (int) Math.ceil(Math.min(IcbmConfig.maxBlastRadius, def.radius(def.maxFill())));
            final int half = r + 2;
            final int cx = ctx.originX() + FAR_X;
            final int cy = 140;
            final int cz = ctx.originZ();
            final boolean savedBlockDamage = ExplosionsConfig.blockDamage;
            final boolean savedT3 = IcbmConfig.terrainDamageT3;
            final int savedBudget = IcbmConfig.blastBudgetMs;
            final boolean savedFallout = ExplosionsConfig.fallout;
            ExplosionsConfig.blockDamage = false; // D52: ignored
            ExplosionsConfig.fallout = false;
            IcbmConfig.terrainDamageT3 = true;
            IcbmConfig.blastBudgetMs = 6;
            BlastEngine.instance()
                .clear();
            final int[] stage = { 0, 0, 0, 0, 0 }; // fill x, detonated tick, ticks deciding, reloads, clear x
            final ScenarioResult[] verdict = { null };
            final int[] before = { 0 };
            final int[] lastReload = { 0 };
            final int budgetMs = 6;
            return ScenarioResult.pending(name(), 20 * 60 * 20, ticks -> {
                try {
                    BlastEngine engine = BlastEngine.instance();
                    if (verdict[0] != null) {
                        // Leave the cube as found: air again, a few planes a tick, before the next scenario starts.
                        for (int n = 0; n < 8 && stage[4] <= 2 * half; n++, stage[4]++) {
                            int x = cx - half + stage[4];
                            for (int z = cz - half; z <= cz + half; z++) {
                                for (int y = cy - half; y < cy + half; y++) {
                                    if (w.getBlock(x, y, z) != Blocks.air) {
                                        w.setBlock(x, y, z, Blocks.air, 0, 2);
                                    }
                                }
                            }
                        }
                        if (stage[4] > 2 * half) {
                            for (int x = (cx - half) >> 4; x <= (cx + half) >> 4; x++) {
                                for (int z = (cz - half) >> 4; z <= (cz + half) >> 4; z++) {
                                    cps.unloadChunksIfNotNearSpawn(x, z);
                                }
                            }
                            return verdict[0];
                        }
                        return null;
                    }
                    if (stage[0] == 0) {
                        // Far from every slot the chunks are not loaded, and setBlock into an unloaded chunk does
                        // nothing: load the 9 x 9 chunks that hold the cube first.
                        for (int x = (cx - half) >> 4; x <= (cx + half) >> 4; x++) {
                            for (int z = (cz - half) >> 4; z <= (cz + half) >> 4; z++) {
                                cps.loadChunk(x, z);
                            }
                        }
                    }
                    if (stage[0] <= 2 * half) {
                        // Fill one X plane per step: stone below the center, air from it up, in 100k-block steps.
                        for (int n = 0; n < 8 && stage[0] <= 2 * half; n++, stage[0]++) {
                            int x = cx - half + stage[0];
                            for (int z = cz - half; z <= cz + half; z++) {
                                for (int y = cy - half; y < cy; y++) {
                                    w.setBlock(x, y, z, Blocks.stone, 0, 2);
                                }
                            }
                        }
                        return null;
                    }
                    if (stage[1] == 0) {
                        before[0] = stone(w, cx, cy, cz, half);
                        engine.resetStats();
                        engine.recordTicks(true);
                        out.check(
                            IcbmBlasts.detonate(w, cx + 0.5, cy + 0.5, cz + 0.5, def, def.maxFill(), null),
                            "the nuclear blast should be accepted");
                        stage[1] = ticks;
                        return null;
                    }
                    if (engine.decidingBlasts() > 0) {
                        stage[2]++;
                        if (stage[3] == 0 && stage[2] >= 3) {
                            out.checkEquals("blasts resumed after a reload mid-decide", 1, engine.saveAndReload(w));
                            stage[3] = 1;
                            lastReload[0] = ticks;
                        }
                    }
                    int age = ticks - stage[1];
                    if (engine.activeBlasts() > 0 && age < 20 * 60 * 15) {
                        return null;
                    }
                    ExplosionsConfig.blockDamage = savedBlockDamage;
                    ExplosionsConfig.fallout = savedFallout;
                    IcbmConfig.terrainDamageT3 = savedT3;
                    IcbmConfig.blastBudgetMs = savedBudget;
                    long max = engine.maxTickNanos();
                    long[] perTick = engine.tickNanos();
                    String slowest = engine.slowestSlice();
                    engine.recordTicks(false);
                    long limit = budgetMs * 1_000_000L + 5_000_000L;
                    int over = 0;
                    for (long t : perTick) {
                        if (t > limit) {
                            over++;
                        }
                    }
                    long[] sorted = perTick.clone();
                    java.util.Arrays.sort(sorted);
                    long p50 = sorted.length == 0 ? 0L : sorted[sorted.length / 2];
                    long p99 = sorted.length == 0 ? 0L
                        : sorted[(int) Math.min(sorted.length - 1, sorted.length * 99L / 100L)];
                    int broken = before[0] - stone(w, cx, cy, cz, half);
                    out.check(stage[2] >= 3, "the crater should be decided over several ticks, took " + stage[2]);
                    out.check(stage[3] == 1, "the decide stage should have been reloaded once");
                    // BlastEngine.maxTickNanos is the scheduler's own time inside the tick (not the whole server tick),
                    // so it is the blast's own share. One tick can still run long on a shared box (a garbage collection
                    // or another process taking the core), which no slice can prevent, so the bar is the distribution:
                    // 99% of the ticks that ran blast work (one tick at least) stay within the budget plus one slice's overshoot, and no
                    // tick passes a hard cap that would mean a slice that does not watch the clock.
                    out.check(
                        over <= Math.max(1, perTick.length / 100),
                        over + " of "
                            + perTick.length
                            + " blast ticks passed "
                            + limit / 1_000_000L
                            + " ms (p50 "
                            + p50 / 1_000_000L
                            + " ms, p99 "
                            + p99 / 1_000_000L
                            + " ms, slowest slice "
                            + slowest
                            + "), at most 1% (and one tick) allowed against a "
                            + budgetMs
                            + " ms budget");
                    out.check(
                        max <= HARD_CAP_NANOS,
                        "the worst tick spent " + max / 1_000_000L
                            + " ms on blasts (stage after it: "
                            + engine.maxTickStage()
                            + "; slowest slice "
                            + slowest
                            + "), past the "
                            + HARD_CAP_NANOS / 1_000_000L
                            + " ms hard cap");
                    double hemisphere = 2.0 / 3.0 * Math.PI * r * r * r;
                    out.check(
                        broken >= 0.45 * hemisphere,
                        "a radius-" + r
                            + " crater should break most of the hemisphere ("
                            + (long) (0.45 * hemisphere)
                            + "), broke "
                            + broken);
                    int depth = 0;
                    while (depth < half && w.getBlock(cx, cy - 1 - depth, cz) == Blocks.air) {
                        depth++;
                    }
                    out.check(depth >= r - 4, "the crater should be about " + r + " deep, was " + depth);
                    out.note(
                        "nuclear r" + r
                            + ": broken "
                            + broken
                            + ", ticks deciding "
                            + stage[2]
                            + ", worst tick "
                            + max / 1_000_000L
                            + " ms ("
                            + engine.maxTickStage()
                            + "), "
                            + perTick.length
                            + " blast ticks, p50 "
                            + p50 / 1_000_000L
                            + " ms, p99 "
                            + p99 / 1_000_000L
                            + " ms, "
                            + over
                            + " over "
                            + limit / 1_000_000L
                            + " ms, slowest slice "
                            + slowest);
                    verdict[0] = out.build();
                    return null;
                } catch (RuntimeException e) {
                    ExplosionsConfig.blockDamage = savedBlockDamage;
                    ExplosionsConfig.fallout = savedFallout;
                    IcbmConfig.terrainDamageT3 = savedT3;
                    IcbmConfig.blastBudgetMs = savedBudget;
                    return ScenarioResult.fail(name(), "threw " + e);
                }
            });
        }

        private static int stone(World w, int cx, int cy, int cz, int half) {
            int n = 0;
            for (int x = cx - half; x <= cx + half; x++) {
                for (int z = cz - half; z <= cz + half; z++) {
                    for (int y = cy - half; y < cy; y++) {
                        if (w.getBlock(x, y, z) == Blocks.stone) {
                            n++;
                        }
                    }
                }
            }
            return n;
        }
    }

    /** Annihilation: a boss, an armored-looking mob with hurt-resistance frames and a cow all die in the zone. */
    private static final class KillZone implements Scenario {

        @Override
        public String name() {
            return "icbm.nuclear.kill_zone";
        }

        @Override
        public String description() {
            return "A nuclear warhead (fill 8, radius 32, kill zone 19) kills everything living inside the kill zone:"
                + " a Wither boss, a mob with a huge health pool and hurt-resistance frames, and a cow; a survival"
                + " player dies exactly when the server's damage settings let an NPC hurt players, and a creative"
                + " player is left alone. Terrain damage is off so the scenario is only about entities.";
        }

        @Override
        public int areaRadius() {
            return 40;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (off()) {
                return ScenarioResult.fail(name(), "the ICBM content is not registered");
            }
            ctx.clearArea(30, 40);
            final World w = ctx.world();
            final ExplosiveDef def = Explosives.get("nuclear");
            final int ox = ctx.originX();
            final int oy = ctx.originY();
            final int oz = ctx.originZ();
            for (int dx = -30; dx <= 30; dx++) {
                for (int dz = -30; dz <= 30; dz++) {
                    ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
                }
            }
            double zone = NuclearScale.killRadius(def.radius(8));
            EntityWither wither = ctx.spawnAt(new EntityWither(w), ox + 10.5, oy, oz + 0.5, 0, 0);
            wither.func_82215_s(0); // no spawn-time invulnerability: the boss is only protected by its health pool
            EntityZombie tank = ctx.spawnAt(new EntityZombie(w), ox - 8.5, oy, oz + 0.5, 0, 0);
            tank.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.maxHealth)
                .setBaseValue(100000.0);
            tank.setHealth(100000.0F);
            tank.hurtResistantTime = 100;
            tank.addPotionEffect(
                new net.minecraft.potion.PotionEffect(net.minecraft.potion.Potion.resistance.id, 600, 4));
            EntityCow cow = ctx.spawnAt(new EntityCow(w), ox + 0.5, oy, oz + 12.5, 0, 0);
            final EntityLivingBase[] victims = { wither, tank, cow };
            // Two fake players in the zone, each in its own chunk entity list only (a FakePlayer has no network
            // handler,
            // so it must not enter the world's tracker): a survival one and a creative one. The blast finds them like
            // any entity and applies the server's damage settings through DamageSources.annihilateLiving.
            final EntityPlayerMP survival = zonePlayer(w, "survival", ox + 4.5, oy, oz - 4.5, false);
            final EntityPlayerMP creative = zonePlayer(w, "creative", ox - 4.5, oy, oz - 4.5, true);
            final MinecraftServer server = MinecraftServer.getServer();
            final boolean playerDies = DamageFactors.damageFactor(
                DamageSources.settingsFromConfig(),
                DamageFactors.Attacker.NPC,
                true,
                server != null && server.isPVPEnabled()) > 0.0;
            final boolean savedT3 = IcbmConfig.terrainDamageT3;
            IcbmConfig.terrainDamageT3 = false;
            boolean accepted = IcbmBlasts.detonate(w, ox + 0.5, oy + 0.5, oz + 0.5, def, 8, null);
            if (!accepted) {
                IcbmConfig.terrainDamageT3 = savedT3;
                releasePlayer(w, survival);
                releasePlayer(w, creative);
                return ScenarioResult.fail(name(), "the engine did not accept the blast");
            }
            return ScenarioResult.pending(name(), 300, ticks -> {
                boolean allDead = true;
                for (EntityLivingBase e : victims) {
                    allDead &= e.isDead || e.getHealth() <= 0.0F;
                }
                if (!(allDead && (!playerDies || survival.getHealth() <= 0.0F)) && ticks < 200) {
                    return null;
                }
                IcbmConfig.terrainDamageT3 = savedT3;
                final boolean survivalDead = survival.getHealth() <= 0.0F;
                final boolean creativeDead = creative.getHealth() <= 0.0F;
                releasePlayer(w, survival);
                releasePlayer(w, creative);
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(zone > 15.0, "the kill zone should be 60% of the radius, was " + zone);
                out.check(wither.isDead || wither.getHealth() <= 0.0F, "the Wither boss died in the kill zone");
                out.check(
                    tank.isDead || tank.getHealth() <= 0.0F,
                    "the 100,000-health mob died through its resistance");
                out.check(cow.isDead || cow.getHealth() <= 0.0F, "the cow died in the kill zone");
                out.check(
                    survivalDead == playerDies,
                    "a survival player in the kill zone should " + (playerDies ? "die" : "survive")
                        + " under the server's damage settings (NPC factor "
                        + DamageFactorsConfig.damageFactorNPC
                        + "), "
                        + (survivalDead ? "died" : "survived"));
                out.check(!creativeDead, "a creative player in the kill zone is not killed (vanilla creative rule)");
                return out.build();
            });
        }

        /** A fake player stood in the chunk's entity list at a spot, at full health, survival or creative. */
        /**
         * A server player that never joined. Its death is recorded instead of run, because other mods' death handlers
         * (server utilities, stats) look the player up and throw for one that never logged in, aborting the blast.
         */
        private static final class Victim extends EntityPlayerMP {

            Victim(MinecraftServer server, net.minecraft.world.WorldServer world,
                com.mojang.authlib.GameProfile profile, net.minecraft.server.management.ItemInWorldManager manager) {
                super(server, world, profile, manager);
            }

            @Override
            public void onDeath(net.minecraft.util.DamageSource source) {
                // recorded by health <= 0
            }
        }

        private static EntityPlayerMP zonePlayer(World w, String kind, double x, double y, double z, boolean creative) {
            // Forge's FakePlayer is invulnerable by design, so the victim is a plain server player that never joined.
            net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) w;
            EntityPlayerMP p = new Victim(
                MinecraftServer.getServer(),
                ws,
                new com.mojang.authlib.GameProfile(
                    java.util.UUID.nameUUIDFromBytes(("gregguns:kill_zone:" + kind).getBytes()),
                    "[GG_" + kind + "]"),
                new net.minecraft.server.management.ItemInWorldManager(ws));
            connect(p);
            p.isDead = false;
            p.deathTime = 0;
            p.hurtResistantTime = 0;
            p.capabilities.isCreativeMode = creative;
            p.capabilities.disableDamage = creative;
            p.setHealth(p.getMaxHealth());
            p.setPosition(x, y, z);
            w.getChunkFromBlockCoords((int) Math.floor(x), (int) Math.floor(z))
                .addEntity(p);
            return p;
        }

        private static void releasePlayer(World w, EntityPlayer p) {
            w.getChunkFromBlockCoords((int) Math.floor(p.posX), (int) Math.floor(p.posZ))
                .removeEntity(p);
            p.isDead = false;
            p.deathTime = 0;
            p.capabilities.isCreativeMode = false;
            p.capabilities.disableDamage = false;
            p.setHealth(p.getMaxHealth());
        }
    }

    /** Fallout: converted blocks and tick time within the caps, nothing outside the disc, decay, exposure. */
    private static final class FalloutCaps implements Scenario {

        @Override
        public String name() {
            return "icbm.nuclear.fallout_caps";
        }

        @Override
        public String description() {
            return "A nuclear warhead (fill 8) over a grass plain leaves irradiated surface blocks within the caps: at"
                + " most falloutMaxBlocks, none outside the fallout disc, none below the surface layer, every fallout"
                + " tick within the budget; a player standing on them is exposed; and they decay back to dirt.";
        }

        @Override
        public int areaRadius() {
            return 60;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (off()) {
                return ScenarioResult.fail(name(), "the ICBM content is not registered");
            }
            final World w = ctx.world();
            final ExplosiveDef def = Explosives.get("nuclear");
            final int ox = ctx.originX();
            final int oy = ctx.originY();
            final int oz = ctx.originZ();
            final int fill = 8;
            final double radius = def.radius(fill);
            final int disc = NuclearScale.falloutRadius(radius, ExplosionsConfig.falloutMaxRadius);
            final int span = disc + 4;
            ctx.clearArea(span, 12);
            final Runnable lay = () -> {
                for (int dx = -span; dx <= span; dx++) {
                    for (int dz = -span; dz <= span; dz++) {
                        // Irradiated blocks left by an earlier aborted run (a crater floor reaches 8 below the plain).
                        for (int dy = -12; dy < 0; dy++) {
                            if (w.getBlock(ox + dx, oy + dy, oz + dz) == FalloutBlocks.irradiated()) {
                                w.setBlock(ox + dx, oy + dy, oz + dz, Blocks.air, 0, 2);
                            }
                        }
                        ctx.setBlock(dx, -1, dz, Blocks.grass, 0);
                        ctx.setBlock(dx, -2, dz, Blocks.dirt, 0);
                        ctx.setBlock(dx, -3, dz, Blocks.dirt, 0);
                        ctx.setBlock(dx, -4, dz, Blocks.stone, 0);
                    }
                }
            };
            lay.run();
            final boolean savedT3 = IcbmConfig.terrainDamageT3;
            final boolean savedFallout = ExplosionsConfig.fallout;
            final int savedDecay = ExplosionsConfig.falloutDecayMinutes;
            IcbmConfig.terrainDamageT3 = true;
            ExplosionsConfig.fallout = true;
            BlastEngine.instance()
                .clear();
            FalloutEngine.clear(w);
            // Warm-up: the first fallout tick of a fresh server loads the protection hooks and block classes, a
            // one-off hundred milliseconds that says nothing about the steady cost the 3 ms budget is about. A tiny
            // job runs first; the worst-tick statistic is reset after it.
            if (!Fallout.start(w, ox + 0.5, oy, oz + 0.5, 4.0)) {
                IcbmConfig.terrainDamageT3 = savedT3;
                ExplosionsConfig.fallout = savedFallout;
                return ScenarioResult.fail(name(), "fallout did not start");
            }
            final int[] phase = { -1, 0, 0 };
            return ScenarioResult.pending(name(), 20 * 60 * 6, ticks -> {
                try {
                    FalloutEngine fe = FalloutEngine.instance();
                    if (phase[0] == -1) {
                        if (fe.busy(w) && ticks < 600) {
                            return null;
                        }
                        FalloutEngine.clear(w);
                        lay.run();
                        fe.resetStats();
                        if (!IcbmBlasts.detonate(w, ox + 0.5, oy + 0.5, oz + 0.5, def, fill, null)) {
                            IcbmConfig.terrainDamageT3 = savedT3;
                            ExplosionsConfig.fallout = savedFallout;
                            return ScenarioResult.fail(name(), "the engine did not accept the blast");
                        }
                        phase[0] = 0;
                        phase[2] = ticks;
                        return null;
                    }
                    if (phase[0] == 0) {
                        // Wait for the crater, then for the fallout job (100 tick delay plus the scan) to finish.
                        if (BlastEngine.instance()
                            .activeBlasts() > 0 || fe.busy(w)
                            || fe.convertedTotal() == 0 && ticks - phase[2] < 700) {
                            return null;
                        }
                        phase[0] = 1;
                        phase[1] = ticks;
                        ScenarioResult.Builder out = ScenarioResult.builder(name());
                        int total = 0;
                        int outside = 0;
                        int deep = 0;
                        int[] first = null;
                        for (int dx = -span; dx <= span; dx++) {
                            for (int dz = -span; dz <= span; dz++) {
                                int inColumn = 0;
                                for (int dy = 6; dy >= -8; dy--) {
                                    Block b = w.getBlock(ox + dx, oy + dy, oz + dz);
                                    if (b != FalloutBlocks.irradiated()) {
                                        continue;
                                    }
                                    total++;
                                    if (++inColumn == 4) {
                                        deep++;
                                    }
                                    if (first == null) {
                                        first = new int[] { ox + dx, oy + dy, oz + dz };
                                    }
                                    if (dx * dx + dz * dz > disc * disc) {
                                        outside++;
                                    }
                                    // Surface only: the layer below a converted block is converted or the column's
                                    // third block; never the stone.
                                    // (counted per column below: no column holds more than three)
                                }
                            }
                        }
                        out.check(total > 0, "a nuclear strike should leave irradiated blocks, found " + total);
                        out.check(
                            total <= ExplosionsConfig.falloutMaxBlocks,
                            "converted blocks " + total
                                + " should stay within the cap "
                                + ExplosionsConfig.falloutMaxBlocks);
                        out.check(outside == 0, outside + " irradiated blocks lie outside the fallout disc of " + disc);
                        out.check(deep == 0, deep + " columns converted past the surface layers");
                        long max = fe.maxTickNanos();
                        out.check(
                            // FalloutEngine.maxTickNanos times only its own handler, so it is already the fallout's own
                            // share of the tick; the allowance is the overshoot of one 8-column check interval.
                            max <= ExplosionsConfig.falloutBudgetMs * 1_000_000L + 5_000_000L,
                            "the worst fallout tick took " + max / 1_000_000L
                                + " ms against a "
                                + ExplosionsConfig.falloutBudgetMs
                                + " ms budget");
                        if (first != null) {
                            net.minecraft.entity.player.EntityPlayer p = FakePlayerFactory
                                .getMinecraft((net.minecraft.world.WorldServer) w);
                            connect((EntityPlayerMP) p);
                            p.setPosition(first[0] + 0.5, first[1] + 1.0, first[2] + 0.5);
                            out.check(
                                RadiationExposure.touching(w, p),
                                "a player standing on irradiated earth is exposed");
                            // GT's radioactivity must really land on the bare player and not on one in full hazmat
                            // (the Hazmat enchantment on all four armor pieces, which GT counts as full hazmat).
                            p.clearActivePotions();
                            out.check(
                                RadiationExposure.expose(w, p) && p.isPotionActive(Potion.moveSlowdown),
                                "a bare player on irradiated earth gets GT's radioactivity (slowness)");
                            p.clearActivePotions();
                            for (int slot = 1; slot <= 4; slot++) {
                                ItemStack piece = new ItemStack(Items.iron_helmet);
                                piece.addEnchantment(EnchantmentHazmat.INSTANCE, 1);
                                p.setCurrentItemOrArmor(slot, piece);
                            }
                            boolean suited = RadiationExposure.expose(w, p);
                            boolean slowed = p.isPotionActive(Potion.moveSlowdown);
                            for (int slot = 1; slot <= 4; slot++) {
                                p.setCurrentItemOrArmor(slot, null);
                            }
                            p.clearActivePotions();
                            out.check(
                                !suited && !slowed,
                                "a player in full GT hazmat on irradiated earth is not irradiated");
                        }
                        out.note(
                            "nuclear fill " + fill
                                + ": converted "
                                + total
                                + " (cap "
                                + ExplosionsConfig.falloutMaxBlocks
                                + "), disc "
                                + disc
                                + ", worst fallout tick "
                                + max / 1_000_000L
                                + " ms");
                        FalloutEngine.expireAll(w);
                        pendingResult = out;
                        return null;
                    }
                    if (fe.irradiatedBlocks(w) > 0 && ticks - phase[1] < 20 * 60 * 3) {
                        return null;
                    }
                    ExplosionsConfig.fallout = savedFallout;
                    ExplosionsConfig.falloutDecayMinutes = savedDecay;
                    IcbmConfig.terrainDamageT3 = savedT3;
                    int left = 0;
                    String where = "";
                    for (int dx = -span; dx <= span; dx++) {
                        for (int dz = -span; dz <= span; dz++) {
                            for (int dy = 6; dy >= -8; dy--) {
                                if (w.getBlock(ox + dx, oy + dy, oz + dz) == FalloutBlocks.irradiated()) {
                                    if (left++ == 0) {
                                        where = " (first at " + dx
                                            + ","
                                            + dy
                                            + ","
                                            + dz
                                            + ", record still holds "
                                            + fe.irradiatedBlocks(w)
                                            + ")";
                                    }
                                }
                            }
                        }
                    }
                    pendingResult.check(left == 0, "decay should put every block back, " + left + " remain" + where);
                    return pendingResult.build();
                } catch (RuntimeException e) {
                    ExplosionsConfig.fallout = savedFallout;
                    IcbmConfig.terrainDamageT3 = savedT3;
                    return ScenarioResult.fail(name(), "threw " + e);
                }
            });
        }

        private ScenarioResult.Builder pendingResult;
    }

    /** The rubble is radioactive by itself and GT knows nothing about it, so no recipe turns it into U-235. */
    private static final class RubbleInert implements Scenario {

        @Override
        public String name() {
            return "icbm.nuclear.rubble";
        }

        @Override
        public String description() {
            return "Irradiated Rubble has no GT item data, so no Macerator or Arc Furnace recipe"
                + " takes it (it would be free uranium-235); carried, it irradiates a bare player and not one in full"
                + " hazmat; irradiated earth cannot be silk harvested.";
        }

        @Override
        public int areaRadius() {
            return 4;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (off()) {
                return ScenarioResult.fail(name(), "the ICBM content is not registered");
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemStack rubble = new ItemStack(FalloutBlocks.rubble());
            out.check(GTOreDictUnificator.getItemData(rubble) == null, "the rubble should carry no GT item data");
            out.check(
                GTUtility.getRadioactivityLevel(rubble) == 0,
                "GT should not see the rubble as radioactive (its own onUpdate does that)");
            out.check(
                RecipeMaps.maceratorRecipes.findRecipeQuery()
                    .items(rubble)
                    .voltage(TierEU.RECIPE_UV)
                    .find() == null,
                "no Macerator recipe should take the rubble");
            out.check(
                RecipeMaps.arcFurnaceRecipes.findRecipeQuery()
                    .items(rubble)
                    .voltage(TierEU.RECIPE_UV)
                    .find() == null,
                "no Arc Furnace recipe should take the rubble");
            // (The Recycler turns any item into scrap, so it is no recycling route for rubble.)
            EntityPlayer p = FakePlayerFactory.getMinecraft((WorldServer) ctx.world());
            connect((EntityPlayerMP) p);
            p.clearActivePotions();
            ItemStack stack = new ItemStack(FalloutBlocks.rubble(), 3);
            out.check(
                ItemIrradiatedRubble.irradiate(p, stack) && p.isPotionActive(Potion.moveSlowdown),
                "carrying rubble should irradiate a bare player");
            p.clearActivePotions();
            for (int slot = 1; slot <= 4; slot++) {
                ItemStack piece = new ItemStack(Items.iron_helmet);
                piece.addEnchantment(EnchantmentHazmat.INSTANCE, 1);
                p.setCurrentItemOrArmor(slot, piece);
            }
            boolean suited = ItemIrradiatedRubble.irradiate(p, stack);
            boolean slowed = p.isPotionActive(Potion.moveSlowdown);
            for (int slot = 1; slot <= 4; slot++) {
                p.setCurrentItemOrArmor(slot, null);
            }
            p.clearActivePotions();
            out.check(!suited && !slowed, "carrying rubble in full GT hazmat should not irradiate");
            out.check(
                !FalloutBlocks.irradiated()
                    .canSilkHarvest(ctx.world(), p, ctx.originX(), ctx.originY(), ctx.originZ(), 0),
                "irradiated earth should never be silk harvested");
            return out.build();
        }
    }

    /**
     * Decay skips unloaded chunks and finishes them when they load: one entry in a chunk that unloads, one whose age
     * cap runs out first.
     */
    private static final class DecayUnloaded implements Scenario {

        private static final int Y = 70;
        private String out0 = "";

        @Override
        public String name() {
            return "icbm.nuclear.decay_unloaded";
        }

        @Override
        public String description() {
            return "Two irradiated blocks in two chunks that unload before decay is due: after decay expires they are"
                + " kept (not forgotten, not reverted); once the first chunk loads again its block reverts to dirt,"
                + " and the second, whose age cap ran out while unloaded, is given up on.";
        }

        @Override
        public int areaRadius() {
            return 4;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (off()) {
                return ScenarioResult.fail(name(), "the ICBM content is not registered");
            }
            final WorldServer w = (WorldServer) ctx.world();
            final ChunkProviderServer cps = w.theChunkProviderServer;
            // Far from the spawn chunks (which never unload) and from every scenario slot.
            final int ax = w.getSpawnPoint().posX + 900;
            final int az = w.getSpawnPoint().posZ + 900;
            final int bx = ax + 64;
            final int bz = az;
            FalloutEngine.clear(w);
            for (int[] at : new int[][] { { ax, az }, { bx, bz } }) {
                cps.loadChunk(at[0] >> 4, at[1] >> 4);
                w.setBlock(at[0], Y, at[1], FalloutBlocks.irradiated(), 0, 2);
            }
            out0 = "before unload " + w.getBlock(ax, Y, az)
                .getUnlocalizedName()
                + ", saver "
                + w.getSaveHandler()
                    .getClass()
                    .getSimpleName()
                + ", levelSaving "
                + w.levelSaving;
            FalloutEngine.trackForTest(w, ax, Y, az, 24000L);
            FalloutEngine.trackForTest(w, bx, Y, bz, 1L);
            // Unload both chunks, saved first so the irradiated block is on disk.
            cps.saveChunks(true, null);
            for (int[] at : new int[][] { { ax, az }, { bx, bz } }) {
                cps.unloadChunksIfNotNearSpawn(at[0] >> 4, at[1] >> 4);
            }
            final int[] phase = { 0, 0 };
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            return ScenarioResult.pending(name(), 20 * 60, ticks -> {
                try {
                    FalloutEngine fe = FalloutEngine.instance();
                    switch (phase[0]) {
                        case 0:
                            if (cps.chunkExists(ax >> 4, az >> 4) || cps.chunkExists(bx >> 4, bz >> 4)) {
                                if (ticks > 600) {
                                    FalloutEngine.clear(w);
                                    return ScenarioResult.fail(
                                        name(),
                                        "the chunks did not unload within 600 ticks (levelSaving " + w.levelSaving
                                            + ", forced "
                                            + w.getPersistentChunks()
                                                .containsKey(
                                                    new net.minecraft.world.ChunkCoordIntPair(ax >> 4, az >> 4))
                                            + ", loaded "
                                            + cps.getLoadedChunkCount()
                                            + ", A "
                                            + cps.chunkExists(ax >> 4, az >> 4)
                                            + ", B "
                                            + cps.chunkExists(bx >> 4, bz >> 4)
                                            + ")");
                                }
                                cps.unloadQueuedChunks();
                                // Neighbors populating or a stray request take a chunk off the unload queue: queue
                                // again.
                                cps.unloadChunksIfNotNearSpawn(ax >> 4, az >> 4);
                                cps.unloadChunksIfNotNearSpawn(bx >> 4, bz >> 4);
                                return null;
                            }
                            FalloutEngine.expireAll(w);
                            phase[0] = 1;
                            phase[1] = ticks;
                            return null;
                        case 1:
                            if (ticks - phase[1] < 60) {
                                return null; // decay runs and finds both chunks unloaded; the age cap of B runs out
                            }
                            out.checkEquals(
                                "entries kept while their chunk is unloaded (B's age cap ran out)",
                                1,
                                fe.irradiatedBlocks(w));
                            out.check(!cps.chunkExists(ax >> 4, az >> 4), "decay must not load the chunk to revert it");
                            cps.loadChunk(ax >> 4, az >> 4); // loads the chunk back from disk
                            out.check(
                                w.getBlock(ax, Y, az) == FalloutBlocks.irradiated(),
                                "the irradiated block should be back with its saved chunk, found "
                                    + w.getBlock(ax, Y, az)
                                        .getUnlocalizedName()
                                    + " ("
                                    + out0
                                    + ")");
                            FalloutEngine.expireAll(w);
                            phase[0] = 2;
                            phase[1] = ticks;
                            return null;
                        default:
                            if (w.getBlock(ax, Y, az) == FalloutBlocks.irradiated() && ticks - phase[1] < 400) {
                                return null;
                            }
                            out.check(
                                w.getBlock(ax, Y, az) == Blocks.dirt,
                                "the block should revert to dirt once its chunk loads again, found "
                                    + w.getBlock(ax, Y, az)
                                        .getUnlocalizedName());
                            out.checkEquals("nothing left to decay", 0, fe.irradiatedBlocks(w));
                            w.setBlock(ax, Y, az, Blocks.air, 0, 2);
                            FalloutEngine.clear(w);
                            return out.build();
                    }
                } catch (RuntimeException e) {
                    FalloutEngine.clear(w);
                    return ScenarioResult.fail(name(), "threw " + e);
                }
            });
        }
    }
}
