package com.gregguns.projectile;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GrimReaperDef;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.server.GunRig;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.GunRecipeScenarios;

/**
 * The {@code gun.grim_reaper.*} in-game scenarios: the four-rocket reload, one rocket per trigger pull at the six tick
 * interval, the rocket's radius and its block damage, and the LuV upgrade recipe. Dev environment only. Server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Grim Reaper); docs/testing/README.md
public final class GrimReaperScenarios {

    private static final int CORRIDOR = 14;
    private static final float EPS = 0.3F;

    private GrimReaperScenarios() {}

    /**
     * Registers the Grim Reaper's scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        GunDefinition def = GunDefinitions.byName(GrimReaperDef.NAME);
        registry.register(new Reload());
        registry.register(new Volley());
        registry.register(new Blast(false));
        registry.register(new Blast(true));
        registry.register(
            GunRecipeScenarios.assembly(
                "gun.grim_reaper.recipe",
                "The Grim Reaper (GR-97) is in GT's assembler map at LuV, 45 s, with its Rocket Launcher, three barrels, carbon receiver, LuV piston and HSSG plates, and not below LuV.",
                def.assembly()));
        registry.register(
            GunRecipeScenarios.upgrade(
                "gun.grim_reaper.upgrade",
                "The GR-97 upgrade is found with a used, camouflaged Rocket Launcher (any NBT) in slot 0 at LuV and outputs a fresh Grim Reaper.",
                def.assembly()));
        registry.register(
            GunRecipeScenarios.tierGate(
                "gun.grim_reaper.tiergate",
                "The Grim Reaper row carries the LuV gate material, GT finds it at LuV voltage and not one tier below.",
                def.assembly()));
    }

    private static EntityZombie sturdy(ScenarioContext ctx, double dx) {
        EntityZombie z = GunRig.zombie(ctx, 0);
        z.setPosition(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5);
        z.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(400.0);
        z.setHealth(400.0F);
        z.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 2000, 10));
        return z;
    }

    private static float lost(EntityLivingBase e) {
        return e.getMaxHealth() - e.getHealth();
    }

    private static float lossFor(EntityLivingBase target, double raw) {
        double armor = target.getTotalArmorValue();
        return (float) DamageModel
            .resolve(DamageType.EXPLOSION, raw, 0.0, share -> share * DamageModel.vanillaArmorFactor(armor));
    }

    /** The rockets that appear after construction. */
    private static final class Rockets {

        private final Set<Object> before = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Set<RocketProjectile> found = Collections.newSetFromMap(new IdentityHashMap<>());
        private RocketProjectile first;

        Rockets(ScenarioContext ctx) {
            before.addAll(ctx.world().loadedEntityList);
        }

        void poll(ScenarioContext ctx) {
            for (Object o : ctx.world().loadedEntityList) {
                if (o instanceof RocketProjectile && !before.contains(o)
                    && found.add((RocketProjectile) o)
                    && first == null) {
                    first = (RocketProjectile) o;
                }
            }
        }

        int count() {
            return found.size();
        }

        boolean allDead() {
            for (RocketProjectile r : found) {
                if (!r.isDead) {
                    return false;
                }
            }
            return !found.isEmpty();
        }
    }

    /** An empty Grim Reaper reloads four loose rockets in 40 ticks. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.grim_reaper.reload";
        }

        @Override
        public String description() {
            return "An empty Grim Reaper reloads after 40 ticks from 4 loose rockets: all four tubes fill, the rockets are consumed and nothing comes back.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = GrimReaperDef.STATS;
            GunRig rig = new GunRig(ctx, GunItems.get(GrimReaperDef.NAME), 0);
            rig.give(stats.ammoType(), 4);
            int reloadTicks = stats.reloadTicks();
            rig.press();
            rig.release();
            rig.tick();
            boolean started = rig.reloading();
            int[] loadedAt = new int[reloadTicks + 3];
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < loadedAt.length) {
                    loadedAt[ticks] = rig.loaded();
                }
                if (ticks < reloadTicks + 2) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the dry trigger should have started a reload");
                out.checkEquals("rounds one tick before the end", 0, loadedAt[reloadTicks - 1]);
                out.checkEquals("rounds at the end of the reload", 4, loadedAt[reloadTicks]);
                out.checkEquals("loose rockets left", 0, rig.count(stats.ammoType()));
                out.checkEquals("projectiles launched", 0, rig.bullets());
                return out.build();
            });
        }
    }

    /** One rocket per pull, then one every six ticks while the trigger is held. */
    private static final class Volley implements Scenario {

        @Override
        public String name() {
            return "gun.grim_reaper.volley";
        }

        @Override
        public String description() {
            return "A loaded Grim Reaper launches exactly one rocket per trigger pull (3 rounds left); held for 14 more ticks it launches two more, six ticks apart, and a zombie in the line loses the 50 explosion damage at least once.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            boolean savedBlocks = ExplosionsConfig.blockDamage;
            ExplosionsConfig.blockDamage = false;
            GunRig rig = new GunRig(ctx, GunItems.get(GrimReaperDef.NAME), 4, CORRIDOR);
            EntityZombie target = sturdy(ctx, 6.0);
            Rockets rockets = new Rockets(ctx);
            rig.press();
            rig.release();
            int[] phase = { 0 };
            int[] pullShots = { -1 };
            int[] pullBullets = { -1 };
            int[] pullLoaded = { -1 };
            return ScenarioResult.pending(name(), 90, ticks -> {
                rig.tick();
                rockets.poll(ctx);
                if (phase[0] == 0) {
                    pullShots[0] = rig.shots();
                    pullBullets[0] = rockets.count();
                    pullLoaded[0] = rig.loaded();
                    phase[0] = 1;
                    // Hold the trigger from here.
                    rig.press();
                    return null;
                }
                if (phase[0] == 1) {
                    if (ticks < 14) {
                        return null;
                    }
                    rig.release();
                    phase[0] = 2;
                }
                if (!rockets.allDead() && ticks < 80) {
                    return null;
                }
                ExplosionsConfig.blockDamage = savedBlocks;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("rockets after one pull", 1, pullBullets[0]);
                out.checkEquals("rounds spent by one pull", 1, pullShots[0]);
                out.checkEquals("rounds left after one pull", 3, pullLoaded[0]);
                out.checkEquals("rockets after holding the trigger", 3, rockets.count());
                out.checkEquals("rounds left in the end", 1, rig.loaded());
                out.check(
                    lost(target) >= lossFor(target, 50.0) - EPS,
                    "the zombie should have taken at least one full 50 hit, lost " + lost(target));
                return out.build();
            });
        }
    }

    /** One rocket, with the block damage switch off or on. */
    private static final class Blast implements Scenario {

        private final boolean blocks;

        Blast(boolean blocks) {
            this.blocks = blocks;
        }

        @Override
        public String name() {
            return blocks ? "gun.grim_reaper.blocks" : "gun.grim_reaper.direct";
        }

        @Override
        public String description() {
            return blocks
                ? "With explosions.blockDamage on, a Grim Reaper rocket into a dirt wall 5 blocks away breaks dirt at the impact."
                : "A Grim Reaper rocket into a zombie 4 blocks away carries the 1.75 block radius, deals the gun's 50 once, and leaves the floor intact with block damage off.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            boolean savedBlocks = ExplosionsConfig.blockDamage;
            ExplosionsConfig.blockDamage = blocks;
            GunRig rig = new GunRig(ctx, GunItems.get(GrimReaperDef.NAME), 4, CORRIDOR);
            if (blocks) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        ctx.setBlock(5, dy, dz, Blocks.dirt, 0);
                    }
                }
            }
            EntityZombie front = blocks ? null : sturdy(ctx, 4.0);
            Rockets rockets = new Rockets(ctx);
            rig.press();
            rig.release();
            return ScenarioResult.pending(name(), 60, ticks -> {
                rig.tick();
                rockets.poll(ctx);
                if (rockets.first == null || !rockets.first.isDead) {
                    if (ticks >= 55) {
                        ExplosionsConfig.blockDamage = savedBlocks;
                        return ScenarioResult.fail(name(), "the rocket never exploded");
                    }
                    return null;
                }
                ExplosionsConfig.blockDamage = savedBlocks;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(
                    Math.abs(rockets.first.getBlastRadius() - 1.75F) < 1e-4,
                    "blast radius " + rockets.first.getBlastRadius());
                if (blocks) {
                    int air = 0;
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dy = 0; dy <= 2; dy++) {
                            if (ctx.world()
                                .getBlock(ctx.originX() + 5, ctx.originY() + dy, ctx.originZ() + dz) == Blocks.air) {
                                air++;
                            }
                        }
                    }
                    out.check(air > 0, "block damage on: the rocket should break dirt at the impact");
                } else {
                    out.check(
                        Math.abs(lost(front) - lossFor(front, 50.0)) < EPS,
                        "the zombie hit should lose the full 50 once, " + lossFor(front, 50.0)
                            + " but lost "
                            + lost(front));
                    out.checkEquals(
                        "floor intact",
                        Blocks.stone,
                        ctx.world()
                            .getBlock(ctx.originX() + 4, ctx.originY() - 1, ctx.originZ()));
                }
                return out.build();
            });
        }
    }
}
