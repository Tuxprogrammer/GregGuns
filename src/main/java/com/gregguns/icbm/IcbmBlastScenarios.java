package com.gregguns.icbm;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ExplosionEvent;

import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.explosion.PackExplosionProbe;
import com.gregguns.icbm.blast.BlastEngine;
import com.gregguns.icbm.explosive.BlastContext;
import com.gregguns.icbm.explosive.BlastEffect;
import com.gregguns.icbm.explosive.BlastShape;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.ExplosiveTier;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Scenarios of the blast engine (I1): every registered explosive detonates through the shared pipeline and respects
 * protection and {@code explosions.blockDamage}; several large blasts at once stay within the tick budget; a blast
 * survives a save and a reload. Dev environment only, server side. They register hidden test explosives that the
 * registry's {@code all()} does not list.
 */
// Spec: docs/ROADMAP-ICBM.md (I1 exit gate); docs/gregguns-icbm-spec/02-explosives/blast-engine.md
final class IcbmBlastScenarios {

    private static final int HALF = 14;
    private static final int CENTER_DY = HALF + 1;

    private static ExplosiveDef sphere;
    private static ExplosiveDef grid;
    private static ExplosiveDef none;
    private static ExplosiveDef big;

    /** Counts what the NONE-shape test effect did. */
    static final int[] EFFECT_CALLS = new int[3];

    private IcbmBlastScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        if (Explosives.get("scenariosphere") == null) {
            sphere = ExplosiveDef.builder("scenariosphere", ExplosiveTier.T1)
                .shape(BlastShape.RAY_SPHERE)
                .baseRadius(8)
                .hidden()
                .build();
            grid = ExplosiveDef.builder("scenariogrid", ExplosiveTier.T2)
                .shape(BlastShape.RAY_GRID)
                .baseRadius(8)
                .hidden()
                .build();
            none = ExplosiveDef.builder("scenarionone", ExplosiveTier.T1)
                .shape(BlastShape.NONE)
                .baseRadius(6)
                .effect(new CountingEffect())
                .hidden()
                .build();
            big = ExplosiveDef.builder("scenariobig", ExplosiveTier.T2)
                .shape(BlastShape.RAY_SPHERE)
                .baseRadius(18)
                .hidden()
                .build();
            Explosives.register(sphere);
            Explosives.register(grid);
            Explosives.register(none);
            Explosives.register(big);
        }
        registry.register(PackExplosionProbe.skipIfProtected(new Pipeline()));
        registry.register(PackExplosionProbe.skipIfProtected(new Budget()));
        registry.register(PackExplosionProbe.skipIfProtected(new SaveReload()));
    }

    /** Counts the hook calls, so a scenario can see the effect ran on the shared pipeline. */
    private static final class CountingEffect implements BlastEffect {

        @Override
        public void onDetonate(BlastContext ctx) {
            EFFECT_CALLS[0]++;
        }

        @Override
        public boolean onTick(BlastContext ctx, int age) {
            EFFECT_CALLS[1]++;
            return age < 3;
        }

        @Override
        public void onFinish(BlastContext ctx) {
            EFFECT_CALLS[2]++;
        }
    }

    // ---- helpers ----

    private static void fillStone(World w, int cx, int cy, int cz, int half, int hy) {
        for (int x = cx - half; x <= cx + half; x++) {
            for (int y = cy - hy; y <= cy + hy; y++) {
                for (int z = cz - half; z <= cz + half; z++) {
                    w.setBlock(x, y, z, Blocks.stone, 0, 2);
                }
            }
        }
    }

    private static int countStone(World w, int cx, int cy, int cz, int half, int hy) {
        int n = 0;
        for (int x = cx - half; x <= cx + half; x++) {
            for (int y = cy - hy; y <= cy + hy; y++) {
                for (int z = cz - half; z <= cz + half; z++) {
                    if (w.getBlock(x, y, z) == Blocks.stone) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    private static EntityZombie source(ScenarioContext ctx) {
        EntityZombie z = new EntityZombie(ctx.world());
        z.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY() + 1, ctx.originZ() + HALF + 4.5, 0, 0);
        return ctx.spawn(z);
    }

    /** Clears every stone block of the test cube so the area ends as it began. */
    private static void clearCube(ScenarioContext ctx, int cx, int cy, int cz, int half, int hy) {
        for (int x = cx - half; x <= cx + half; x++) {
            for (int y = cy - hy; y <= cy + hy; y++) {
                for (int z = cz - half; z <= cz + half; z++) {
                    ctx.world()
                        .setBlock(x, y, z, Blocks.air, 0, 2);
                }
            }
        }
    }

    /** Vetoes every block of every blast, as a claim mod does. */
    public static final class Veto {

        int seen;

        @SubscribeEvent
        public void onDetonate(ExplosionEvent.Detonate event) {
            seen++;
            event.getAffectedBlocks()
                .clear();
        }
    }

    // ---- 1. every explosive through the shared pipeline ----

    private static final class Pipeline implements Scenario {

        @Override
        public String name() {
            return "icbm.blast.pipeline";
        }

        @Override
        public String description() {
            return "Every registered explosive detonates through the shared pipeline: it breaks blocks when"
                + " explosions.blockDamage and its tier allow, breaks none when blockDamage is off or a claim listener"
                + " vetoes ExplosionEvent.Detonate, and a NONE-shape type runs its effect hooks without terrain damage.";
        }

        @Override
        public int areaRadius() {
            return 24;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final List<ExplosiveDef> defs = new ArrayList<>(Explosives.allIncludingHidden());
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World w = ctx.world();
            final int cx = ctx.originX();
            final int cy = ctx.originY() + CENTER_DY;
            final int cz = ctx.originZ();
            final boolean savedBlockDamage = ExplosionsConfig.blockDamage;
            final EntityZombie src = source(ctx);
            final Veto veto = new Veto();
            BlastEngine.instance()
                .clear();
            // mode 0: allowed; mode 1: blockDamage off; mode 2: a listener vetoes
            final int[] cursor = { 0, 0 };
            final int[] before = { 0 };
            final boolean[] started = { false };
            final int[] detonatedAt = { 0 };
            return ScenarioResult.pending(name(), 40 * 20 * Math.max(1, defs.size()), ticks -> {
                try {
                    if (cursor[0] >= defs.size()) {
                        return finish(ctx, out, savedBlockDamage, veto, cx, cy, cz);
                    }
                    ExplosiveDef def = defs.get(cursor[0]);
                    int mode = cursor[1];
                    if (!started[0]) {
                        fillStone(w, cx, cy, cz, HALF, HALF);
                        before[0] = countStone(w, cx, cy, cz, HALF, HALF);
                        ExplosionsConfig.blockDamage = mode != 1;
                        if (mode == 2) {
                            MinecraftForge.EVENT_BUS.register(veto);
                        }
                        out.check(
                            IcbmBlasts.detonate(w, cx + 0.5, cy + 0.5, cz + 0.5, def, 1, src),
                            def.id() + " mode " + mode + ": the blast should be accepted");
                        started[0] = true;
                        detonatedAt[0] = ticks;
                        return null;
                    }
                    if (BlastEngine.instance()
                        .activeBlasts() > 0 && ticks - detonatedAt[0] < 400) {
                        return null;
                    }
                    if (mode == 2) {
                        MinecraftForge.EVENT_BUS.unregister(veto);
                    }
                    int broken = before[0] - countStone(w, cx, cy, cz, HALF, HALF);
                    boolean terrain = def.shape() != BlastShape.NONE && def.tier()
                        .number() <= IcbmConfig.maxBlastTier
                        && IcbmConfig.terrainDamage(
                            def.tier()
                                .number());
                    if (mode == 0 && terrain) {
                        out.check(broken > 20, def.id() + " should break blocks with block damage on, broke " + broken);
                        out.check(w.getBlock(cx, cy, cz) == Blocks.air, def.id() + " should crater the center block");
                    } else {
                        out.check(broken == 0, def.id() + " mode " + mode + " should break nothing, broke " + broken);
                    }
                    out.check(
                        BlastEngine.instance()
                            .activeBlasts() == 0,
                        def.id() + " should have finished");
                    started[0] = false;
                    cursor[1]++;
                    if (cursor[1] > 2) {
                        cursor[1] = 0;
                        cursor[0]++;
                    }
                    return null;
                } catch (RuntimeException e) {
                    ExplosionsConfig.blockDamage = savedBlockDamage;
                    MinecraftForge.EVENT_BUS.unregister(veto);
                    return ScenarioResult.fail(name(), "threw " + e);
                }
            });
        }

        private ScenarioResult finish(ScenarioContext ctx, ScenarioResult.Builder out, boolean savedBlockDamage,
            Veto veto, int cx, int cy, int cz) {
            ExplosionsConfig.blockDamage = savedBlockDamage;
            out.check(veto.seen > 0, "the Detonate event should have been posted to listeners");
            out.check(EFFECT_CALLS[0] >= 3, "the effect's onDetonate should run per blast, ran " + EFFECT_CALLS[0]);
            out.check(EFFECT_CALLS[2] >= 3, "the effect's onFinish should run per blast, ran " + EFFECT_CALLS[2]);
            clearCube(ctx, cx, cy, cz, HALF, HALF);
            return out.build();
        }
    }

    // ---- 2. several large blasts, one budget ----

    private static final class Budget implements Scenario {

        @Override
        public String name() {
            return "icbm.blast.budget";
        }

        @Override
        public String description() {
            return "Three radius-18 blasts at once share one tick budget: no tick spends much more than"
                + " icbm.blastBudgetMs on them, and all three finish and crater the stone.";
        }

        @Override
        public int areaRadius() {
            return 40;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World w = ctx.world();
            final int cx = ctx.originX();
            final int cy = ctx.originY() + 20;
            final int cz = ctx.originZ();
            final int half = 34;
            final int hy = 19;
            final boolean savedBlockDamage = ExplosionsConfig.blockDamage;
            final int savedBudget = IcbmConfig.blastBudgetMs;
            BlastEngine.instance()
                .clear();
            BlastEngine.instance()
                .resetStats();
            ExplosionsConfig.blockDamage = true;
            IcbmConfig.blastBudgetMs = 4;
            fillStone(w, cx, cy, cz, half, hy);
            final int before = countStone(w, cx, cy, cz, half, hy);
            final EntityZombie src = source(ctx);
            int accepted = 0;
            for (int i = -1; i <= 1; i++) {
                if (IcbmBlasts.detonate(w, cx + i * 14 + 0.5, cy + 0.5, cz + 0.5, big, 1, src)) {
                    accepted++;
                }
            }
            out.checkEquals("blasts accepted", 3, accepted);
            final int[] sawConcurrent = { 0 };
            return ScenarioResult.pending(name(), 20 * 60, ticks -> {
                sawConcurrent[0] = Math.max(
                    sawConcurrent[0],
                    BlastEngine.instance()
                        .activeBlasts());
                if (BlastEngine.instance()
                    .activeBlasts() > 0) {
                    return null;
                }
                ExplosionsConfig.blockDamage = savedBlockDamage;
                IcbmConfig.blastBudgetMs = savedBudget;
                long budgetNanos = 4_000_000L;
                long slack = 30_000_000L;
                long max = BlastEngine.instance()
                    .maxTickNanos();
                out.check(sawConcurrent[0] == 3, "all three should have run together, saw " + sawConcurrent[0]);
                out.check(
                    max <= budgetNanos + slack,
                    "the worst tick spent " + max / 1_000_000L + " ms on blasts against a 4 ms budget");
                int broken = before - countStone(w, cx, cy, cz, half, hy);
                out.check(broken > 3000, "three radius-18 blasts should crater the stone, broke " + broken);
                out.check(ticks >= 2, "a budget of 4 ms spreads the work over several ticks, took " + ticks);
                clearCube(ctx, cx, cy, cz, half, hy);
                return out.build();
            });
        }
    }

    // ---- 3. save and reload ----

    private static final class SaveReload implements Scenario {

        @Override
        public String name() {
            return "icbm.blast.save_reload";
        }

        @Override
        public String description() {
            return "A radius-18 blast saved and reloaded while it is still breaking blocks resumes where it stopped:"
                + " the blocks left when it was saved are exactly the blocks broken after the reload.";
        }

        @Override
        public int areaRadius() {
            return 24;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World w = ctx.world();
            final int cx = ctx.originX();
            final int cy = ctx.originY() + 21;
            final int cz = ctx.originZ();
            final int half = 22;
            final int hy = 20;
            final boolean savedBlockDamage = ExplosionsConfig.blockDamage;
            final int savedBudget = IcbmConfig.blastBudgetMs;
            BlastEngine.instance()
                .clear();
            ExplosionsConfig.blockDamage = true;
            IcbmConfig.blastBudgetMs = 1;
            fillStone(w, cx, cy, cz, half, hy);
            final int initial = countStone(w, cx, cy, cz, half, hy);
            final EntityZombie src = source(ctx);
            out.check(IcbmBlasts.detonate(w, cx + 0.5, cy + 0.5, cz + 0.5, big, 1, src), "accepted");
            final boolean[] reloaded = { false };
            final int[] expected = { 0 };
            return ScenarioResult.pending(name(), 20 * 90, ticks -> {
                BlastEngine engine = BlastEngine.instance();
                if (!reloaded[0]) {
                    int pending = engine.pendingBlocks();
                    int brokenNow = initial - countStone(w, cx, cy, cz, half, hy);
                    if (pending > 0 && brokenNow > 0) {
                        expected[0] = brokenNow + pending;
                        out.checkEquals("blasts resumed", 1, engine.saveAndReload(w));
                        out.checkEquals("pending after the reload", pending, engine.pendingBlocks());
                        reloaded[0] = true;
                    }
                    return null;
                }
                if (engine.activeBlasts() > 0) {
                    return null;
                }
                ExplosionsConfig.blockDamage = savedBlockDamage;
                IcbmConfig.blastBudgetMs = savedBudget;
                int broken = initial - countStone(w, cx, cy, cz, half, hy);
                out.checkEquals("blocks broken in total", expected[0], broken);
                clearCube(ctx, cx, cy, cz, half, hy);
                return out.build();
            });
        }
    }
}
