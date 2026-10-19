package com.gregguns.explosion;

import net.minecraft.block.Block;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

/** In-world checks for nuke-scale explosions (M5 I9). */
// Spec: docs/gregguns-spec/04-projectiles-fx/m5-exit-gate.md
public final class BlastScenarios {

    private static final int HALF = 12;
    private static final int CENTRE_Y = 6;

    private BlastScenarios() {}

    /**
     * @param registry where to register
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Sliced());
    }

    private static int fillAndCount(ScenarioContext ctx, Block block, boolean fill) {
        int count = 0;
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                for (int dy = -1; dy <= CENTRE_Y * 2 + 1; dy++) {
                    if (fill) {
                        ctx.setBlock(dx, dy, dz, block, 0);
                    } else if (ctx.world()
                        .getBlock(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz) == block) {
                            count++;
                        }
                }
            }
        }
        return count;
    }

    /** A blast far larger than the per-tick cap breaks some blocks now and the rest over later ticks. */
    private static final class Sliced implements Scenario {

        @Override
        public String name() {
            return "explosion.nuke.sliced";
        }

        @Override
        public String description() {
            return "A size 16 blast in a dirt block reaches more than the per-tick cap: at most the cap breaks in the first tick, BlastTicker drains the rest over later ticks, and every reached block ends up broken.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            BlastTicker.instance()
                .clear();
            fillAndCount(ctx, Blocks.dirt, true);
            int before = fillAndCount(ctx, Blocks.dirt, false);
            EntityZombie owner = new EntityZombie(ctx.world());
            owner.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY() + CENTRE_Y, ctx.originZ() + 20.5, 0, 0);
            ctx.spawn(owner);
            ProjectileExplosion.Result result = new ProjectileExplosion(
                ctx.world(),
                null,
                owner,
                "nuke_launcher",
                ctx.originX() + 0.5,
                ctx.originY() + CENTRE_Y + 0.5,
                ctx.originZ() + 0.5,
                16.0F,
                250.0F,
                4.0F,
                10.0F).explode(true);
            ctx.despawn(owner);
            int afterFirst = fillAndCount(ctx, Blocks.dirt, false);
            out.check(result.happened(), "the blast should happen");
            out.check(
                result.blocksReached() > ExplosionRules.BLOCKS_PER_TICK,
                "the blast should reach more than the cap, reached " + result.blocksReached());
            int pending = BlastTicker.instance()
                .pendingBlocks();
            out.check(pending > 0, "blocks beyond the cap should be queued, pending " + pending);
            out.check(
                before - afterFirst <= ExplosionRules.BLOCKS_PER_TICK,
                "the first tick may break at most the cap, broke " + (before - afterFirst));
            if (!out.build()
                .passed()) {
                BlastTicker.instance()
                    .clear();
                return out.build();
            }
            return ScenarioResult.pending(name(), 40, ticks -> {
                if (BlastTicker.instance()
                    .pendingBlocks() > 0) {
                    return null;
                }
                ScenarioResult.Builder done = ScenarioResult.builder(name());
                int after = fillAndCount(ctx, Blocks.dirt, false);
                done.check(ticks >= 1, "the rest should take at least one more tick");
                done.check(
                    before - after >= result.blocksReached() - pending - 5,
                    "the queued blocks should end up broken, before " + before + " after " + after);
                fillAndCount(ctx, Blocks.air, true);
                return done.build();
            });
        }
    }
}
