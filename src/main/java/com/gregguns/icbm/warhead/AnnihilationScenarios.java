package com.gregguns.icbm.warhead;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.explosion.nuclear.FalloutBlocks;
import com.gregguns.icbm.blast.BlastEngine;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;

/**
 * The I5 annihilation scenarios (D130 to D133): antimatter annihilates its kill zone, digs its crater and leaves no
 * fallout; antimatter ends red matter in reach; red matter eats, kills, pulls, idles and ends within the tick budget
 * and
 * survives a save and reload in the middle of its life.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/antimatter.md, red-matter.md
final class AnnihilationScenarios {

    /** The red matter test body: fill 2 of 128 is a ball of about 8 blocks (32 x (2/128)^(1/3) = 8.0). */
    private static final int RED_FILL = 2;
    /** The stone cube around the body, half-edge. */
    private static final int CUBE = 10;

    private AnnihilationScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new AntimatterAnnihilate());
        registry.register(new AntimatterEndsRedMatter());
        registry.register(new RedMatterLifecycle());
        registry.register(new RedMatterSaveReload());
    }

    /** @return the ball's largest radius at the test fill, as the effect computes it */
    private static int redRadius() {
        return com.gregguns.icbm.warhead.effects.RedMatterModel.maxRadius(
            Explosives.get("redmatter")
                .radius(RED_FILL));
    }

    private static boolean off() {
        return Explosives.get("antimatter") == null || Explosives.get("redmatter") == null;
    }

    /** A stone cube of half-edge {@link #CUBE} centered {@code CUBE} above the origin, with a 5x5x5 air pocket. */
    private static void buildCube(ScenarioContext ctx) {
        for (int dx = -CUBE; dx <= CUBE; dx++) {
            for (int dy = 0; dy <= 2 * CUBE; dy++) {
                for (int dz = -CUBE; dz <= CUBE; dz++) {
                    boolean pocket = Math.abs(dx) <= 2 && Math.abs(dy - CUBE) <= 2 && Math.abs(dz) <= 2;
                    ctx.setBlock(dx, dy, dz, pocket ? Blocks.air : Blocks.stone, 0);
                }
            }
        }
    }

    private static int stoneLeft(ScenarioContext ctx, int radius) {
        World w = ctx.world();
        int n = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= radius * radius
                        && w.getBlock(ctx.originX() + dx, ctx.originY() + CUBE + dy, ctx.originZ() + dz)
                            == Blocks.stone) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    // ---- antimatter ----

    private static final class AntimatterAnnihilate implements Scenario {

        @Override
        public String name() {
            return "icbm.antimatter.annihilate";
        }

        @Override
        public String description() {
            return "An antimatter warhead (fill 1, radius 16, kill zone 9) over a stone slab: a Wither boss, a mob with"
                + " 100,000 health and a cow in the kill zone all die, the crater is dug, and no irradiated block"
                + " appears (antimatter leaves no fallout, D21).";
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
            final ExplosiveDef def = Explosives.get("antimatter");
            final int ox = ctx.originX();
            final int oy = ctx.originY();
            final int oz = ctx.originZ();
            for (int dx = -24; dx <= 24; dx++) {
                for (int dz = -24; dz <= 24; dz++) {
                    for (int dy = -6; dy <= -1; dy++) {
                        ctx.setBlock(dx, dy, dz, Blocks.stone, 0);
                    }
                }
            }
            EntityWither wither = ctx.spawnAt(new EntityWither(w), ox + 6.5, oy, oz + 0.5, 0, 0);
            wither.func_82215_s(0);
            EntityZombie tank = ctx.spawnAt(new EntityZombie(w), ox - 4.5, oy, oz + 0.5, 0, 0);
            tank.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.maxHealth)
                .setBaseValue(100000.0);
            tank.setHealth(100000.0F);
            tank.hurtResistantTime = 100;
            EntityCow cow = ctx.spawnAt(new EntityCow(w), ox + 0.5, oy, oz + 3.5, 0, 0);
            final EntityLivingBase[] victims = { wither, tank, cow };
            final boolean savedT4 = IcbmConfig.terrainDamageT4;
            final int savedTier = IcbmConfig.maxBlastTier;
            IcbmConfig.terrainDamageT4 = true;
            IcbmConfig.maxBlastTier = 5;
            BlastEngine.instance()
                .clear();
            if (!IcbmBlasts.detonate(w, ox + 0.5, oy + 0.5, oz + 0.5, def, 1, null)) {
                IcbmConfig.terrainDamageT4 = savedT4;
                return ScenarioResult.fail(name(), "the engine did not accept the blast");
            }
            return ScenarioResult.pending(name(), 20 * 60, ticks -> {
                if (!BlastEngine.instance()
                    .lastBlastDone() && ticks < 20 * 55) {
                    return null;
                }
                IcbmConfig.terrainDamageT4 = savedT4;
                IcbmConfig.maxBlastTier = savedTier;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(
                    BlastEngine.instance()
                        .lastBlastDone(),
                    "the blast finished");
                for (EntityLivingBase e : victims) {
                    out.check(
                        e.isDead || e.getHealth() <= 0.0F,
                        e.getClass()
                            .getSimpleName() + " died in the kill zone");
                }
                int dug = 0;
                int irradiated = 0;
                for (int dx = -24; dx <= 24; dx++) {
                    for (int dz = -24; dz <= 24; dz++) {
                        for (int dy = -6; dy <= 4; dy++) {
                            net.minecraft.block.Block b = w.getBlock(ox + dx, oy + dy, oz + dz);
                            if (b == FalloutBlocks.irradiated()) {
                                irradiated++;
                            }
                            if (dy < 0 && dx * dx + dz * dz <= 36 && b != Blocks.stone) {
                                dug++;
                            }
                        }
                    }
                }
                out.check(dug > 100, "the crater was dug under the center, blocks removed: " + dug);
                out.check(irradiated == 0, "antimatter leaves no fallout, irradiated blocks: " + irradiated);
                return out.build();
            });
        }
    }

    private static final class AntimatterEndsRedMatter implements Scenario {

        @Override
        public String name() {
            return "icbm.antimatter.ends_red_matter";
        }

        @Override
        public String description() {
            return "A red matter body is running; an antimatter blast 60 blocks away (terrain off) ends it at once,"
                + " long before its lifetime, and itself finishes.";
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
            final int ox = ctx.originX();
            final int oy = ctx.originY();
            final int oz = ctx.originZ();
            final boolean savedT4 = IcbmConfig.terrainDamageT4;
            IcbmConfig.terrainDamageT4 = false; // the body idles from the start; only its life is under test
            BlastEngine engine = BlastEngine.instance();
            engine.clear();
            if (!IcbmBlasts.detonate(w, ox + 0.5, oy + 5.5, oz + 0.5, Explosives.get("redmatter"), RED_FILL, null)) {
                IcbmConfig.terrainDamageT4 = savedT4;
                return ScenarioResult.fail(name(), "the engine did not accept the red matter");
            }
            final boolean[] fired = { false };
            return ScenarioResult.pending(name(), 600, ticks -> {
                if (ticks == 10 && !fired[0]) {
                    fired[0] = true;
                    if (engine.activeBlasts() != 1) {
                        IcbmConfig.terrainDamageT4 = savedT4;
                        return ScenarioResult
                            .fail(name(), "expected the red matter to be running, blasts: " + engine.activeBlasts());
                    }
                    IcbmBlasts.detonate(w, ox + 60.5, oy + 5.5, oz + 0.5, Explosives.get("antimatter"), 1, null);
                    return null;
                }
                if (!fired[0] || (engine.activeBlasts() > 0 && ticks < 500)) {
                    return null;
                }
                IcbmConfig.terrainDamageT4 = savedT4;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(engine.activeBlasts() == 0, "both blasts are over, running: " + engine.activeBlasts());
                out.check(ticks < 400, "ended well before the 1,200-tick lifetime, at tick " + ticks);
                return out.build();
            });
        }
    }

    // ---- red matter ----

    private static final class RedMatterLifecycle implements Scenario {

        @Override
        public String name() {
            return "icbm.redmatter.lifecycle";
        }

        @Override
        public String description() {
            return "A red matter body (fill 2, radius 8) inside a stone cube: it eats the stone out to its radius, kills"
                + " an item and a cow in its ball, pulls an item from outside toward it, goes idle at full size and"
                + " ends on its own before its lifetime, with no tick over the blast budget plus slack.";
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
            buildCube(ctx);
            final World w = ctx.world();
            final int ox = ctx.originX();
            final int oy = ctx.originY();
            final int oz = ctx.originZ();
            final int cy = oy + CUBE;
            EntityCow cow = ctx.spawnAt(new EntityCow(w), ox + 0.5, cy - 2, oz + 1.5, 0, 0);
            EntityItem inside = ctx.spawn(new EntityItem(w, ox + 1.5, cy - 1.5, oz + 0.5, new ItemStack(Items.stick)));
            final double startDistance = 12.5;
            EntityItem outside = ctx
                .spawn(new EntityItem(w, ox + startDistance, cy + 0.5, oz + 0.5, new ItemStack(Items.apple)));
            outside.motionX = 0.0;
            final boolean savedT4 = IcbmConfig.terrainDamageT4;
            final int savedBudget = IcbmConfig.blastBudgetMs;
            final int savedLife = IcbmConfig.redMatterLifetimeSeconds;
            IcbmConfig.terrainDamageT4 = true;
            IcbmConfig.blastBudgetMs = 6;
            IcbmConfig.redMatterLifetimeSeconds = 30;
            BlastEngine engine = BlastEngine.instance();
            engine.clear();
            engine.resetStats();
            if (!IcbmBlasts.detonate(w, ox + 0.5, cy + 0.5, oz + 0.5, Explosives.get("redmatter"), RED_FILL, null)) {
                IcbmConfig.terrainDamageT4 = savedT4;
                IcbmConfig.blastBudgetMs = savedBudget;
                IcbmConfig.redMatterLifetimeSeconds = savedLife;
                return ScenarioResult.fail(name(), "the engine did not accept the red matter");
            }
            final double[] closest = { startDistance };
            final int[] idleAt = { -1 };
            final int[] maxRadius = { 0 };
            final long[] worstNanos = { 0L };
            return ScenarioResult.pending(name(), 20 * 40, ticks -> {
                if (!outside.isDead) {
                    closest[0] = Math.min(closest[0], Math.abs(outside.posX - (ox + 0.5)));
                }
                NBTTagCompound s = engine.lastEffectState();
                maxRadius[0] = Math.max(maxRadius[0], s.getInteger("rmRadius"));
                if (idleAt[0] < 0 && s.getInteger("rmMode") == 1) {
                    idleAt[0] = ticks;
                }
                worstNanos[0] = Math.max(worstNanos[0], engine.maxTickNanos());
                if (!engine.lastBlastDone() && ticks < 20 * 38) {
                    return null;
                }
                IcbmConfig.terrainDamageT4 = savedT4;
                IcbmConfig.blastBudgetMs = savedBudget;
                IcbmConfig.redMatterLifetimeSeconds = savedLife;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                int ball = stoneLeft(ctx, redRadius() - 1);
                out.check(engine.lastBlastDone(), "the body ended on its own");
                out.check(ticks < 20 * 30, "ended before its 600-tick lifetime ran out, tick " + ticks);
                out.check(idleAt[0] > 0, "the body went idle at full size");
                out.check(maxRadius[0] == redRadius(), "grew to its radius " + redRadius() + ", was " + maxRadius[0]);
                out.check(ball == 0, "no stone left inside radius " + (redRadius() - 1) + ", left: " + ball);
                out.check(stoneLeft(ctx, CUBE) > 1000, "stone outside the ball is untouched");
                out.check(cow.isDead || cow.getHealth() <= 0.0F, "the cow in the ball was annihilated");
                out.check(inside.isDead, "the item in the ball was removed");
                out.check(
                    outside.isDead || closest[0] < startDistance - 0.5,
                    "the item outside was pulled in, closest " + closest[0]);
                long budgetNanos = 6L * 1_000_000L;
                out.check(
                    worstNanos[0] <= budgetNanos + 25_000_000L,
                    "no tick spent more than the budget plus slack on blasts: " + worstNanos[0] / 1_000_000.0 + " ms");
                return out.build();
            });
        }
    }

    private static final class RedMatterSaveReload implements Scenario {

        @Override
        public String name() {
            return "icbm.redmatter.save_reload";
        }

        @Override
        public String description() {
            return "A red matter body is saved and reloaded in the middle of its life (growing and eating): it comes"
                + " back at the same radius and cursor, not at full size and not restarted, and still finishes the"
                + " cube and ends (ICBM-02-80).";
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
            buildCube(ctx);
            final World w = ctx.world();
            final int ox = ctx.originX();
            final int cy = ctx.originY() + CUBE;
            final int oz = ctx.originZ();
            final boolean savedT4 = IcbmConfig.terrainDamageT4;
            final int savedBudget = IcbmConfig.blastBudgetMs;
            final int savedLife = IcbmConfig.redMatterLifetimeSeconds;
            IcbmConfig.terrainDamageT4 = true;
            IcbmConfig.blastBudgetMs = 2; // slow enough that the body is still growing when we save it
            IcbmConfig.redMatterLifetimeSeconds = 30;
            BlastEngine engine = BlastEngine.instance();
            engine.clear();
            if (!IcbmBlasts.detonate(w, ox + 0.5, cy + 0.5, oz + 0.5, Explosives.get("redmatter"), RED_FILL, null)) {
                IcbmConfig.terrainDamageT4 = savedT4;
                IcbmConfig.blastBudgetMs = savedBudget;
                IcbmConfig.redMatterLifetimeSeconds = savedLife;
                return ScenarioResult.fail(name(), "the engine did not accept the red matter");
            }
            final NBTTagCompound[] saved = { null };
            final int[] reloaded = { -1 };
            final NBTTagCompound[] after = { null };
            return ScenarioResult.pending(name(), 20 * 40, ticks -> {
                NBTTagCompound s = engine.lastEffectState();
                if (saved[0] == null) {
                    boolean growing = s.getInteger("rmRadius") >= 4 && s.getInteger("rmMode") == 0;
                    if (!growing && ticks < 200) {
                        return null;
                    }
                    saved[0] = s;
                    reloaded[0] = engine.saveAndReload(w);
                    after[0] = engine.lastEffectState();
                    return null;
                }
                if (!engine.lastBlastDone() && ticks < 20 * 38) {
                    return null;
                }
                IcbmConfig.terrainDamageT4 = savedT4;
                IcbmConfig.blastBudgetMs = savedBudget;
                IcbmConfig.redMatterLifetimeSeconds = savedLife;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(reloaded[0] == 1, "one blast came back from the save, got " + reloaded[0]);
                out.check(
                    saved[0].getInteger("rmRadius") >= 4,
                    "it was saved mid-growth, radius " + saved[0].getInteger("rmRadius"));
                out.check(
                    after[0].getInteger("rmRadius") == saved[0].getInteger("rmRadius")
                        && after[0].getInteger("rmCursor") == saved[0].getInteger("rmCursor")
                        && after[0].getInteger("rmInner") == saved[0].getInteger("rmInner"),
                    "radius, shell and cursor came back as saved: " + saved[0] + " vs " + after[0]);
                out.check(
                    after[0].getInteger("rmRadius") < redRadius() || after[0].getInteger("rmMode") == 0,
                    "it did not come back at full size");
                out.check(engine.lastBlastDone(), "the reloaded body finished and ended");
                out.check(stoneLeft(ctx, redRadius() - 1) == 0, "the reloaded body finished eating its ball");
                return out.build();
            });
        }
    }
}
