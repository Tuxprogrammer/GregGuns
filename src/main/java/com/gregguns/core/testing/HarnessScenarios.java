package com.gregguns.core.testing;

import net.minecraft.block.Block;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

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
        registry.register(new SpawnedMobsStay());
    }

    /**
     * A spawned zombie is still the same live entity after the ticks in which pack mods (SpecialMobs, Infernal Mobs)
     * would replace or modify it. In the dev workspace there is nothing to replace it, so this only fails in the pack.
     */
    private static final class SpawnedMobsStay implements Scenario {

        private static final int WAIT_TICKS = 5;

        @Override
        public String name() {
            return "harness.pack_mobs";
        }

        @Override
        public String description() {
            return "A zombie spawned through the context is not replaced by a SpecialMobs variant and gets no Infernal Mobs"
                + " modifier: same entity, alive and in the world a few ticks later.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EntityZombie zombie = new EntityZombie(ctx.world());
            zombie.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0, 0);
            zombie.setCurrentItemOrArmor(4, new ItemStack(Blocks.pumpkin));
            ctx.spawn(zombie);
            return ScenarioResult.pending(name(), WAIT_TICKS + 5, ticks -> {
                if (ticks < WAIT_TICKS) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(!zombie.isDead, "the zombie should still be alive and not replaced by a variant");
                out.check(ctx.world().loadedEntityList.contains(zombie), "the zombie should still be in the world");
                out.checkEquals("class of the zombie's mob", EntityZombie.class, zombie.getClass());
                return out.build();
            });
        }
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
