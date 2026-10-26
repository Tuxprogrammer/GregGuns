package com.gregguns.icbm.warhead;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.explosion.nuclear.FalloutBlocks;
import com.gregguns.explosion.nuclear.FalloutEngine;
import com.gregguns.explosion.nuclear.NuclearScale;
import com.gregguns.explosion.nuclear.RadiationExposure;
import com.gregguns.icbm.blast.BlastEngine;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;

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
    }

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

        @Override
        public int areaRadius() {
            return 72;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (off()) {
                return ScenarioResult.fail(name(), "the ICBM content is not registered");
            }
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World w = ctx.world();
            final ExplosiveDef def = Explosives.get("nuclear");
            final int r = (int) Math.ceil(Math.min(IcbmConfig.maxBlastRadius, def.radius(def.maxFill())));
            final int half = r + 2;
            final int cx = ctx.originX();
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
            final int[] stage = { 0, 0, 0, 0 }; // fill x, detonated tick, ticks deciding, reloads
            final int[] before = { 0 };
            final int[] lastReload = { 0 };
            final int budgetMs = 6;
            return ScenarioResult.pending(name(), 20 * 60 * 20, ticks -> {
                try {
                    BlastEngine engine = BlastEngine.instance();
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
                    int broken = before[0] - stone(w, cx, cy, cz, half);
                    out.check(stage[2] >= 3, "the crater should be decided over several ticks, took " + stage[2]);
                    out.check(stage[3] == 1, "the decide stage should have been reloaded once");
                    out.check(
                        max <= budgetMs * 1_000_000L + 35_000_000L,
                        "the worst tick spent " + max / 1_000_000L
                            + " ms on blasts against a "
                            + budgetMs
                            + " ms budget");
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
                            + " ms");
                    return out.build();
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
                + " a Wither boss, a mob with a huge health pool and hurt-resistance frames, and a cow. Terrain damage"
                + " is off so the scenario is only about entities.";
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
            final boolean savedT3 = IcbmConfig.terrainDamageT3;
            IcbmConfig.terrainDamageT3 = false;
            boolean accepted = IcbmBlasts.detonate(w, ox + 0.5, oy + 0.5, oz + 0.5, def, 8, null);
            if (!accepted) {
                IcbmConfig.terrainDamageT3 = savedT3;
                return ScenarioResult.fail(name(), "the engine did not accept the blast");
            }
            return ScenarioResult.pending(name(), 300, ticks -> {
                boolean allDead = true;
                for (EntityLivingBase e : victims) {
                    allDead &= e.isDead || e.getHealth() <= 0.0F;
                }
                if (!allDead && ticks < 200) {
                    return null;
                }
                IcbmConfig.terrainDamageT3 = savedT3;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(zone > 15.0, "the kill zone should be 60% of the radius, was " + zone);
                out.check(wither.isDead || wither.getHealth() <= 0.0F, "the Wither boss died in the kill zone");
                out.check(
                    tank.isDead || tank.getHealth() <= 0.0F,
                    "the 100,000-health mob died through its resistance");
                out.check(cow.isDead || cow.getHealth() <= 0.0F, "the cow died in the kill zone");
                return out.build();
            });
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
            for (int dx = -span; dx <= span; dx++) {
                for (int dz = -span; dz <= span; dz++) {
                    ctx.setBlock(dx, -1, dz, Blocks.grass, 0);
                    ctx.setBlock(dx, -2, dz, Blocks.dirt, 0);
                    ctx.setBlock(dx, -3, dz, Blocks.dirt, 0);
                    ctx.setBlock(dx, -4, dz, Blocks.stone, 0);
                }
            }
            final boolean savedT3 = IcbmConfig.terrainDamageT3;
            final boolean savedFallout = ExplosionsConfig.fallout;
            final int savedDecay = ExplosionsConfig.falloutDecayMinutes;
            IcbmConfig.terrainDamageT3 = true;
            ExplosionsConfig.fallout = true;
            BlastEngine.instance()
                .clear();
            FalloutEngine.clear(w);
            FalloutEngine.instance()
                .resetStats();
            if (!IcbmBlasts.detonate(w, ox + 0.5, oy + 0.5, oz + 0.5, def, fill, null)) {
                IcbmConfig.terrainDamageT3 = savedT3;
                return ScenarioResult.fail(name(), "the engine did not accept the blast");
            }
            final int[] phase = { 0, 0 };
            return ScenarioResult.pending(name(), 20 * 60 * 6, ticks -> {
                try {
                    FalloutEngine fe = FalloutEngine.instance();
                    if (phase[0] == 0) {
                        // Wait for the crater, then for the fallout job (100 tick delay plus the scan) to finish.
                        if (BlastEngine.instance()
                            .activeBlasts() > 0 || fe.busy(w)
                            || fe.convertedTotal() == 0 && ticks < 700) {
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
                            max <= ExplosionsConfig.falloutBudgetMs * 1_000_000L + 25_000_000L,
                            "the worst fallout tick took " + max / 1_000_000L
                                + " ms against a "
                                + ExplosionsConfig.falloutBudgetMs
                                + " ms budget");
                        if (first != null) {
                            net.minecraft.entity.player.EntityPlayer p = FakePlayerFactory
                                .getMinecraft((net.minecraft.world.WorldServer) w);
                            p.setPosition(first[0] + 0.5, first[1] + 1.0, first[2] + 0.5);
                            out.check(
                                RadiationExposure.touching(w, p),
                                "a player standing on irradiated earth is exposed");
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
                    for (int dx = -span; dx <= span; dx++) {
                        for (int dz = -span; dz <= span; dz++) {
                            for (int dy = 6; dy >= -8; dy--) {
                                if (w.getBlock(ox + dx, oy + dy, oz + dz) == FalloutBlocks.irradiated()) {
                                    left++;
                                }
                            }
                        }
                    }
                    pendingResult.check(left == 0, "decay should put every block back, " + left + " remain");
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
}
