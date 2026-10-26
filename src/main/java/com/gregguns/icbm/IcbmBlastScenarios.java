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
 * protection and the per-tier terrain switch (D52); several large blasts at once stay within the tick budget; a blast
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
        registry.register(PackExplosionProbe.skipIfProtected(new CraterSize()));
        registry.register(PackExplosionProbe.skipIfProtected(new EffectVeto()));
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

    private static boolean[] saveTerrain() {
        return new boolean[] { IcbmConfig.terrainDamageT1, IcbmConfig.terrainDamageT2, IcbmConfig.terrainDamageT3,
            IcbmConfig.terrainDamageT4 };
    }

    private static void restoreTerrain(boolean[] saved) {
        IcbmConfig.terrainDamageT1 = saved[0];
        IcbmConfig.terrainDamageT2 = saved[1];
        IcbmConfig.terrainDamageT3 = saved[2];
        IcbmConfig.terrainDamageT4 = saved[3];
    }

    private static void setTerrain(int tier, boolean on) {
        boolean[] all = saveTerrain();
        all[Math.max(1, Math.min(4, tier)) - 1] = on;
        restoreTerrain(all);
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
                + " its tier's terrain switch allows (explosions.blockDamage is ignored, D52), breaks none when the switch is"
                + " off or a claim listener vetoes ExplosionEvent.Detonate, and a NONE-shape type runs its effect hooks without terrain damage.";
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
            final boolean[] savedTerrain = saveTerrain();
            final EntityZombie src = source(ctx);
            final Veto veto = new Veto();
            BlastEngine.instance()
                .clear();
            // mode 0: allowed (blockDamage off, to prove it is ignored); mode 1: the tier switch off; mode 2: a
            // listener vetoes
            final int[] cursor = { 0, 0 };
            final int[] before = { 0 };
            final boolean[] started = { false };
            final int[] detonatedAt = { 0 };
            return ScenarioResult.pending(name(), 3 * 900 * Math.max(1, defs.size()), ticks -> {
                try {
                    if (cursor[0] >= defs.size()) {
                        restoreTerrain(savedTerrain);
                        return finish(ctx, out, savedBlockDamage, veto, cx, cy, cz);
                    }
                    ExplosiveDef def = defs.get(cursor[0]);
                    int mode = cursor[1];
                    if (!started[0]) {
                        fillStone(w, cx, cy, cz, HALF, HALF);
                        before[0] = countStone(w, cx, cy, cz, HALF, HALF);
                        ExplosionsConfig.blockDamage = false;
                        setTerrain(
                            def.tier()
                                .number(),
                            mode != 1);
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
                    BlastEngine engine = BlastEngine.instance();
                    int age = ticks - detonatedAt[0];
                    // A type with a long effect (a 30 s gas cloud) keeps ticking after its crater is done; once the
                    // terrain has had 100 ticks and nothing is left to break, the scenario ends the blast itself.
                    boolean terrainSettled = age >= 100 && engine.pendingBlocks() == 0 && engine.undecidedBlasts() == 0;
                    if (engine.activeBlasts() > 0 && !terrainSettled && age < 800) {
                        return null;
                    }
                    int left = engine.pendingBlocks();
                    engine.clear();
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
                        // D59: the radius is the crater radius in stone, so the floor grows with it (a fifth of the
                        // sphere, capped by the test cube).
                        double rr = Math.min(def.radius(1), HALF);
                        long floor = Math.max(4L, Math.round(0.2 * 4.0 / 3.0 * Math.PI * rr * rr * rr));
                        out.check(
                            broken >= floor,
                            def.id() + " should break at least "
                                + floor
                                + " blocks at radius "
                                + def.radius(1)
                                + ", broke "
                                + broken);
                        out.check(w.getBlock(cx, cy, cz) == Blocks.air, def.id() + " should crater the center block");
                    } else if (mode != 0 || def.shape() != BlastShape.NONE) {
                        // A NONE-shape type may change blocks in its own effect (the Breaching tunnel) in mode 0;
                        // modes 1 and 2 hold every type to the tier switch and to protection.
                        out.check(broken == 0, def.id() + " mode " + mode + " should break nothing, broke " + broken);
                    }
                    out.check(left == 0, def.id() + " should have finished its crater, " + left + " left");
                    started[0] = false;
                    cursor[1]++;
                    if (cursor[1] > 2) {
                        cursor[1] = 0;
                        cursor[0]++;
                    }
                    return null;
                } catch (RuntimeException e) {
                    ExplosionsConfig.blockDamage = savedBlockDamage;
                    restoreTerrain(savedTerrain);
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
            ExplosionsConfig.blockDamage = false; // D52: ignored by ICBM blasts
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
            ExplosionsConfig.blockDamage = false; // D52: ignored by ICBM blasts
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

    // ---- 4. crater size in stone (D59) ----

    private static final class CraterSize implements Scenario {

        @Override
        public String name() {
            return "icbm.blast.crater_size";
        }

        @Override
        public String description() {
            return "Every crater type at full fill on a stone slab digs its radius (D59): at least half the stone"
                + " hemisphere, as deep as the radius less two at the center and along the surface, and nothing past"
                + " the radius plus one.";
        }

        @Override
        public int areaRadius() {
            return 48;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World w = ctx.world();
            final int cx = ctx.originX();
            final int cy = ctx.originY() + 26;
            final int cz = ctx.originZ();
            final boolean savedBlockDamage = ExplosionsConfig.blockDamage;
            final boolean[] savedTerrain = saveTerrain();
            restoreTerrain(new boolean[] { true, true, true, true });
            final List<ExplosiveDef> defs = new ArrayList<>();
            for (ExplosiveDef d : Explosives.allIncludingHidden()) {
                if (d.shape() != BlastShape.NONE && d.tier()
                    .number() <= IcbmConfig.maxBlastTier) {
                    defs.add(d);
                }
            }
            BlastEngine.instance()
                .clear();
            final int[] cursor = { 0 };
            final int[] detonatedAt = { -1 };
            final int[] before = { 0 };
            return ScenarioResult.pending(name(), 1500 * Math.max(1, defs.size()), ticks -> {
                try {
                    BlastEngine engine = BlastEngine.instance();
                    if (cursor[0] >= defs.size()) {
                        restoreTerrain(savedTerrain);
                        ExplosionsConfig.blockDamage = savedBlockDamage;
                        clearSlab(w, cx, cy, cz, 24);
                        return out.build();
                    }
                    ExplosiveDef def = defs.get(cursor[0]);
                    int r = (int) Math.ceil(Math.min(IcbmConfig.maxBlastRadius, def.radius(def.maxFill())));
                    int half = r + 3;
                    if (detonatedAt[0] < 0) {
                        // Stone below the center cell, air from the center up.
                        for (int x = cx - half; x <= cx + half; x++) {
                            for (int z = cz - half; z <= cz + half; z++) {
                                for (int y = cy - half; y < cy + half; y++) {
                                    w.setBlock(x, y, z, y < cy ? Blocks.stone : Blocks.air, 0, 2);
                                }
                            }
                        }
                        before[0] = countSlab(w, cx, cy, cz, half);
                        ExplosionsConfig.blockDamage = false;
                        out.check(
                            IcbmBlasts.detonate(w, cx + 0.5, cy + 0.5, cz + 0.5, def, def.maxFill(), null),
                            def.id() + " should be accepted");
                        detonatedAt[0] = ticks;
                        return null;
                    }
                    int age = ticks - detonatedAt[0];
                    if (engine.activeBlasts() > 0 && age < 1400
                        && !(age >= 40 && engine.pendingBlocks() == 0 && engine.undecidedBlasts() == 0)) {
                        return null;
                    }
                    engine.clear();
                    int broken = before[0] - countSlab(w, cx, cy, cz, half);
                    double hemisphere = 2.0 / 3.0 * Math.PI * r * r * r;
                    int centerDepth = 0;
                    while (centerDepth < half && w.getBlock(cx, cy - 1 - centerDepth, cz) == Blocks.air) {
                        centerDepth++;
                    }
                    out.check(
                        broken >= 0.5 * hemisphere,
                        def.id() + " r"
                            + r
                            + " should break at least half the hemisphere ("
                            + (int) (0.5 * hemisphere)
                            + "), broke "
                            + broken);
                    out.check(
                        centerDepth >= r - 2,
                        def.id() + " r" + r + " should dig " + (r - 2) + " deep, dug " + centerDepth);
                    int reach = r - 2;
                    out.check(
                        w.getBlock(cx + reach, cy - 1, cz) == Blocks.air
                            && w.getBlock(cx - reach, cy - 1, cz) == Blocks.air
                            && w.getBlock(cx, cy - 1, cz + reach) == Blocks.air
                            && w.getBlock(cx, cy - 1, cz - reach) == Blocks.air,
                        def.id() + " r" + r + " should reach " + reach + " blocks along the surface on all four sides");
                    out.check(
                        outsideBroken(w, cx, cy, cz, half, r + 1) == 0,
                        def.id() + " r" + r + " should break nothing past the radius plus one");
                    out.note(def.id() + " r" + r + ": blocks broken " + broken + " / depth " + centerDepth);
                    detonatedAt[0] = -1;
                    cursor[0]++;
                    return null;
                } catch (RuntimeException e) {
                    restoreTerrain(savedTerrain);
                    ExplosionsConfig.blockDamage = savedBlockDamage;
                    return ScenarioResult.fail(name(), "threw " + e);
                }
            });
        }

        private static void clearSlab(World w, int cx, int cy, int cz, int half) {
            for (int x = cx - half; x <= cx + half; x++) {
                for (int z = cz - half; z <= cz + half; z++) {
                    for (int y = cy - half; y < cy + half; y++) {
                        w.setBlock(x, y, z, Blocks.air, 0, 2);
                    }
                }
            }
        }

        private static int countSlab(World w, int cx, int cy, int cz, int half) {
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

        /** Stone cells of the slab that are gone although they lie farther than {@code limit} from the blast center. */
        private static int outsideBroken(World w, int cx, int cy, int cz, int half, int limit) {
            int n = 0;
            double l2 = (double) limit * limit;
            for (int x = cx - half; x <= cx + half; x++) {
                for (int z = cz - half; z <= cz + half; z++) {
                    for (int y = cy - half; y < cy; y++) {
                        double dx = x - cx;
                        double dy = y - cy;
                        double dz = z - cz;
                        if (dx * dx + dy * dy + dz * dz > l2 && w.getBlock(x, y, z) != Blocks.stone) {
                            n++;
                        }
                    }
                }
            }
            return n;
        }
    }

    // ---- 5. fire and glass obey claims and mobGriefing (D52) ----

    private static final class EffectVeto implements Scenario {

        @Override
        public String name() {
            return "icbm.blast.effect_veto";
        }

        @Override
        public String description() {
            return "The Incendiary (fire) and Sonic (glass) effects change blocks through ExplosionEvent.Detonate:"
                + " unvetoed they place fire and shatter glass; a listener veto, the tier switch off, or mobGriefing"
                + " off for a mob-caused blast leaves every block alone.";
        }

        @Override
        public int areaRadius() {
            return 24;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final ExplosiveDef fire = Explosives.get("incendiary");
            final ExplosiveDef sonic = Explosives.get("sonic");
            if (fire == null || sonic == null) {
                return ScenarioResult.skip(name(), "the warhead types are not registered");
            }
            final World w = ctx.world();
            final int cx = ctx.originX();
            final int cy = ctx.originY() + 26;
            final int cz = ctx.originZ();
            final int half = 11;
            final boolean[] savedTerrain = saveTerrain();
            final boolean savedGriefing = w.getGameRules()
                .getGameRuleBooleanValue("mobGriefing");
            final EntityZombie src = source(ctx);
            final Veto veto = new Veto();
            BlastEngine.instance()
                .clear();
            // mode 0: free; 1: listener veto; 2: tier switch off; 3: mobGriefing off (a zombie caused it)
            final int[] cursor = { 0, 0 };
            final boolean[] started = { false };
            final int[] at = { 0 };
            return ScenarioResult.pending(name(), 4 * 2 * 200, ticks -> {
                try {
                    if (cursor[0] >= 2) {
                        restoreTerrain(savedTerrain);
                        setGriefing(w, savedGriefing);
                        clear(w, cx, cy, cz, half);
                        return out.build();
                    }
                    ExplosiveDef def = cursor[0] == 0 ? fire : sonic;
                    int mode = cursor[1];
                    if (!started[0]) {
                        // A stone floor at cy-1 with a glass wall ring one block above it.
                        clear(w, cx, cy, cz, half);
                        for (int x = cx - half; x <= cx + half; x++) {
                            for (int z = cz - half; z <= cz + half; z++) {
                                w.setBlock(x, cy - 1, z, Blocks.stone, 0, 2);
                            }
                        }
                        for (int x = cx - 5; x <= cx + 5; x++) {
                            w.setBlock(x, cy + 1, cz + 5, Blocks.glass, 0, 2);
                        }
                        restoreTerrain(new boolean[] { true, true, true, true });
                        setGriefing(w, mode != 3);
                        if (mode == 2) {
                            setTerrain(
                                def.tier()
                                    .number(),
                                false);
                        }
                        if (mode == 1) {
                            MinecraftForge.EVENT_BUS.register(veto);
                        }
                        out.check(
                            IcbmBlasts.detonate(w, cx + 0.5, cy + 0.5, cz + 0.5, def, def.maxFill(), src),
                            def.id() + " mode " + mode + " should be accepted");
                        started[0] = true;
                        at[0] = ticks;
                        return null;
                    }
                    if (ticks - at[0] < 5) {
                        return null;
                    }
                    BlastEngine.instance()
                        .clear();
                    if (mode == 1) {
                        MinecraftForge.EVENT_BUS.unregister(veto);
                    }
                    int changed = cursor[0] == 0 ? count(w, cx, cy, cz, half, Blocks.fire)
                        : 11 - count(w, cx, cy, cz, half, Blocks.glass);
                    String what = cursor[0] == 0 ? "fire blocks placed" : "glass blocks shattered";
                    if (mode == 0) {
                        out.check(changed > 0, def.id() + " should change blocks when free, " + what + ": " + changed);
                    } else {
                        out.check(
                            changed == 0,
                            def.id() + " mode " + mode + " should leave blocks alone, " + what + ": " + changed);
                    }
                    started[0] = false;
                    cursor[1]++;
                    if (cursor[1] > 3) {
                        cursor[1] = 0;
                        cursor[0]++;
                    }
                    return null;
                } catch (RuntimeException e) {
                    restoreTerrain(savedTerrain);
                    setGriefing(w, savedGriefing);
                    MinecraftForge.EVENT_BUS.unregister(veto);
                    return ScenarioResult.fail(name(), "threw " + e);
                }
            });
        }

        private static void setGriefing(World w, boolean on) {
            w.getGameRules()
                .setOrCreateGameRule("mobGriefing", Boolean.toString(on));
        }

        private static void clear(World w, int cx, int cy, int cz, int half) {
            for (int x = cx - half; x <= cx + half; x++) {
                for (int z = cz - half; z <= cz + half; z++) {
                    for (int y = cy - 1; y <= cy + 2; y++) {
                        w.setBlock(x, y, z, Blocks.air, 0, 2);
                    }
                }
            }
        }

        private static int count(World w, int cx, int cy, int cz, int half, net.minecraft.block.Block block) {
            int n = 0;
            for (int x = cx - half; x <= cx + half; x++) {
                for (int z = cz - half; z <= cz + half; z++) {
                    for (int y = cy; y <= cy + 2; y++) {
                        if (w.getBlock(x, y, z) == block) {
                            n++;
                        }
                    }
                }
            }
            return n;
        }
    }
}
