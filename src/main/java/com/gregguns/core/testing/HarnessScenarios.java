package com.gregguns.core.testing;

import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;

/**
 * Built-in scenarios that exercise the harness itself end to end in a real world. They prove the context helpers work,
 * so a failing feature scenario can be trusted to point at the feature. Server side only.
 */
public final class HarnessScenarios {

    private HarnessScenarios() {}

    /**
     * Registers the built-in scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new EntityLifecycle());
        registry.register(new AreaRestore());
    }

    /** Spawns a pig at the origin, checks it and its health, removes it, and checks it is gone. */
    private static final class EntityLifecycle implements Scenario {

        @Override
        public String name() {
            return "harness.entity";
        }

        @Override
        public String description() {
            return "Spawns a pig at the origin, checks its health, removes it and checks it is gone.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            EntityPig pig = new EntityPig(ctx.world());
            pig.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0, 0);
            ctx.spawn(pig);
            out.check(!pig.isDead, "pig should be alive after spawning");
            out.check(ctx.world().loadedEntityList.contains(pig), "pig should be in the world's entity list");
            out.checkEquals("pig health", 10.0F, pig.getHealth());
            ctx.despawn(pig);
            out.check(pig.isDead, "pig should be dead after despawn");
            return out.build();
        }
    }

    /** Clears a small area, places a block, then cleans up mid-run and checks the original block is back. */
    private static final class AreaRestore implements Scenario {

        @Override
        public String name() {
            return "harness.area";
        }

        @Override
        public String description() {
            return "Clears an area, places a block, and checks cleanup restores the original block.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Block original = ctx.world()
                .getBlock(ctx.originX(), ctx.originY(), ctx.originZ());
            ctx.clearArea(1, 2);
            out.check(
                ctx.world()
                    .isAirBlock(ctx.originX(), ctx.originY(), ctx.originZ()),
                "origin should be air after clearArea");
            ctx.setBlock(0, 0, 0, Blocks.stone, 0);
            out.check(
                ctx.world()
                    .getBlock(ctx.originX(), ctx.originY(), ctx.originZ()) == Blocks.stone,
                "origin should be stone after setBlock");
            ctx.cleanup();
            out.check(
                ctx.world()
                    .getBlock(ctx.originX(), ctx.originY(), ctx.originZ()) == original,
                "origin should hold its original block after cleanup");
            return out.build();
        }
    }
}
