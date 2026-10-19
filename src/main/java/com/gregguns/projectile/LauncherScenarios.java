package com.gregguns.projectile;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.explosion.GrenadeScenarios;
import com.gregguns.gun.def.GrenadeLauncherDef;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.def.RocketLauncherDef;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.server.GunRig;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.GunRecipeScenarios;

/**
 * The {@code gun.rocket_launcher.*} and {@code gun.grenade_launcher.*} in-game scenarios: both launchers fired through
 * the same server handler as every gun, and what their projectiles do: the rocket's direct hit and splash, the config
 * and protection switches on its blocks and on the shooter, the 40 mm grenade's bounce and fuse, and its direct hit.
 * Dev environment only. Server side.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.4, 3.7); docs/gregguns-spec/02-guns/index.md
// (launchers, batch B5); docs/testing/README.md
public final class LauncherScenarios {

    private static final float EPS = 0.3F;
    private static final int CORRIDOR = 14;

    private LauncherScenarios() {}

    /**
     * Registers the Rocket Launcher's scenarios.
     *
     * @param registry where to add them
     */
    public static void registerRocket(ScenarioRegistry registry) {
        GunDefinition def = GunDefinitions.byName(RocketLauncherDef.NAME);
        registry.register(new Reload(def, 1));
        registry.register(
            GunRecipeScenarios.assembly(
                "gun.rocket_launcher.recipe",
                "The rocket launcher is in GT's assembler map at HV with its sheet inputs, and not below HV.",
                def.assembly()));
        registry.register(new RocketDirect());
        registry.register(new RocketWall(RocketWall.Kind.SPLASH));
        registry.register(new RocketWall(RocketWall.Kind.BLOCKS));
        registry.register(new RocketWall(RocketWall.Kind.SELF));
        registry.register(new RocketWall(RocketWall.Kind.NOSELF));
        registry.register(new RocketWall(RocketWall.Kind.CANCELLED));
    }

    /**
     * Registers the Grenade Launcher's scenarios.
     *
     * @param registry where to add them
     */
    public static void registerGrenade(ScenarioRegistry registry) {
        GunDefinition def = GunDefinitions.byName(GrenadeLauncherDef.NAME);
        registry.register(new Reload(def, 6));
        registry.register(
            GunRecipeScenarios.assembly(
                "gun.grenade_launcher.recipe",
                "The grenade launcher is in GT's assembler map at HV with its sheet inputs, and not below HV.",
                def.assembly()));
        registry.register(new GrenadeBounce());
        registry.register(new GrenadeDirect(false));
        registry.register(new GrenadeDirect(true));
    }

    // ----- helpers -----------------------------------------------------------------------------------------------

    /** A zombie that cannot move and has 200 health, so a 50 damage hit never kills it. */
    private static EntityZombie sturdy(ScenarioContext ctx, double dx, double dz) {
        EntityZombie z = GunRig.zombie(ctx, 0);
        z.setPosition(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5 + dz);
        z.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(200.0);
        z.setHealth(200.0F);
        z.addPotionEffect(new net.minecraft.potion.PotionEffect(net.minecraft.potion.Potion.moveSlowdown.id, 2000, 10));
        return z;
    }

    private static void sturdy(FakePlayer shooter) {
        shooter.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(200.0);
        shooter.setHealth(200.0F);
    }

    private static float lost(EntityLivingBase e) {
        return e.getMaxHealth() - e.getHealth();
    }

    /** Health a target loses to {@code raw} damage of explosion type. */
    private static float lossFor(EntityLivingBase target, double raw) {
        double armor = target.getTotalArmorValue();
        return (float) DamageModel
            .resolve(DamageType.EXPLOSION, raw, 0.0, share -> share * DamageModel.vanillaArmorFactor(armor));
    }

    /** A wall of dirt (weak enough for a launcher blast to break) three wide and three high at {@code dx}. */
    private static void dirtWall(ScenarioContext ctx, int dx) {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dy = 0; dy <= 2; dy++) {
                ctx.setBlock(dx, dy, dz, Blocks.dirt, 0);
            }
        }
    }

    private static int airInWall(ScenarioContext ctx, int dx) {
        int air = 0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dy = 0; dy <= 2; dy++) {
                Block b = ctx.world()
                    .getBlock(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
                if (b == Blocks.air) {
                    air++;
                }
            }
        }
        return air;
    }

    /** Finds the one projectile of a class that appears after construction. */
    private static final class Tracker<T extends GenericProjectile> {

        private final Class<T> type;
        private final Set<Object> before = Collections.newSetFromMap(new IdentityHashMap<>());
        private T found;

        Tracker(ScenarioContext ctx, Class<T> type) {
            this.type = type;
            before.addAll(ctx.world().loadedEntityList);
        }

        T poll(ScenarioContext ctx) {
            if (found == null) {
                for (Object o : ctx.world().loadedEntityList) {
                    if (type.isInstance(o) && !before.contains(o)) {
                        found = type.cast(o);
                        break;
                    }
                }
            }
            return found;
        }
    }

    /** Fires the rig's gun once: press, release, and the shot happens on the next rig tick. */
    private static void pull(GunRig rig) {
        rig.press();
        rig.release();
    }

    private static void restore(boolean blocks, boolean self) {
        ExplosionsConfig.blockDamage = blocks;
        ExplosionsConfig.selfDamage = self;
    }

    // ----- reload ------------------------------------------------------------------------------------------------

    /** An empty launcher reloads from loose rounds and returns nothing (GUN-13 does not apply). */
    private static final class Reload implements Scenario {

        private final GunDefinition def;
        private final int rounds;

        Reload(GunDefinition def, int rounds) {
            this.def = def;
            this.rounds = rounds;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".reload";
        }

        @Override
        public String description() {
            return "An empty " + def.name()
                + " reloads after "
                + def.stats()
                    .reloadTicks()
                + " ticks from "
                + rounds
                + " loose round(s): the clip fills, the rounds are consumed and nothing comes back.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            GunRig rig = new GunRig(ctx, GunItems.get(def.name()), 0);
            rig.give(stats.ammoType(), rounds);
            int reloadTicks = stats.reloadTicks();
            pull(rig);
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
                out.checkEquals("rounds at the end of the reload", stats.magazineSize(), loadedAt[reloadTicks]);
                out.checkEquals("loose rounds left", 0, rig.count(stats.ammoType()));
                out.checkEquals("projectiles launched", 0, rig.bullets());
                return out.build();
            });
        }
    }

    // ----- rocket ------------------------------------------------------------------------------------------------

    /** A rocket that hits a zombie: the direct hit lands once, the zombie behind it takes the splash ramp. */
    private static final class RocketDirect implements Scenario {

        @Override
        public String name() {
            return "gun.rocket_launcher.direct";
        }

        @Override
        public String description() {
            return "A rocket fired into a zombie 4 blocks away deals the gun's 50 damage to it once (the direct hit, left out of the splash), a zombie 1 block behind takes the splash ramp, and the floor is intact with explosions.blockDamage off.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            boolean savedBlocks = ExplosionsConfig.blockDamage;
            boolean savedSelf = ExplosionsConfig.selfDamage;
            restore(false, true);
            GunRig rig = new GunRig(ctx, GunItems.get(RocketLauncherDef.NAME), 1, CORRIDOR);
            EntityZombie front = sturdy(ctx, 4.0, 0.0);
            EntityZombie behind = sturdy(ctx, 5.2, 0.0);
            Tracker<RocketProjectile> rocket = new Tracker<>(ctx, RocketProjectile.class);
            pull(rig);
            return ScenarioResult.pending(name(), 60, ticks -> {
                rig.tick();
                RocketProjectile r = rocket.poll(ctx);
                if (r == null || !r.isDead) {
                    if (ticks >= 55) {
                        restore(savedBlocks, savedSelf);
                        return ScenarioResult.fail(name(), "the rocket never exploded");
                    }
                    return null;
                }
                restore(savedBlocks, savedSelf);
                double dx = behind.posX - r.posX;
                double dy = behind.posY - r.posY;
                double dz = behind.posZ - r.posZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                float splash = lossFor(behind, LauncherMath.splashDamage(50.0, 1.75, distance));
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(
                    Math.abs(lost(front) - lossFor(front, 50.0)) < EPS,
                    "the zombie hit should lose the full 50 once, " + lossFor(front, 50.0)
                        + " but lost "
                        + lost(front));
                out.check(
                    Math.abs(lost(behind) - splash) < EPS && splash > 0.0F && splash < lossFor(behind, 50.0),
                    "the zombie behind (distance " + distance
                        + ") should lose the splash "
                        + splash
                        + " but lost "
                        + lost(behind));
                out.checkEquals(
                    "floor intact",
                    Blocks.stone,
                    ctx.world()
                        .getBlock(ctx.originX() + 4, ctx.originY() - 1, ctx.originZ()));
                out.checkEquals("rounds left", 0, rig.loaded());
                return out.build();
            });
        }
    }

    /** A rocket into a dirt wall, in one of four switch settings. */
    private static final class RocketWall implements Scenario {

        enum Kind {

            SPLASH("splash",
                "A rocket into a dirt wall 5 blocks away with block damage off leaves the wall whole, hurts a zombie beside the impact by exactly the splash ramp at its distance from the blast, and does not touch the shooter."),
            BLOCKS("blocks",
                "With explosions.blockDamage on, the same rocket breaks the dirt at the impact, the zombie still takes the same splash, and the blocks the Forge BreakEvent allows are the ones removed."),
            SELF("self", "A rocket into a dirt wall 2 blocks ahead hurts the shooter standing in the blast (PFX-25)."),
            NOSELF("noself", "The same wall 2 blocks ahead with explosions.selfDamage off leaves the shooter unhurt."),
            CANCELLED("cancelled",
                "A cancelled ExplosionEvent.Start (a protection claim) stops the rocket's blast entirely: the zombie beside the impact loses nothing and the wall stays whole.");

            final String suffix;
            final String description;

            Kind(String suffix, String description) {
                this.suffix = suffix;
                this.description = description;
            }
        }

        private final Kind kind;

        RocketWall(Kind kind) {
            this.kind = kind;
        }

        @Override
        public String name() {
            return "gun.rocket_launcher.wall." + kind.suffix;
        }

        @Override
        public String description() {
            return kind.description;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            boolean savedBlocks = ExplosionsConfig.blockDamage;
            boolean savedSelf = ExplosionsConfig.selfDamage;
            int wallDx = kind == Kind.SELF || kind == Kind.NOSELF ? 2 : 5;
            restore(kind == Kind.BLOCKS, kind != Kind.NOSELF);
            GunRig rig = new GunRig(ctx, GunItems.get(RocketLauncherDef.NAME), 1, CORRIDOR);
            sturdy(rig.shooter);
            dirtWall(ctx, wallDx);
            // Beside the impact, off the rocket's line: one block to the side, in front of the wall.
            EntityZombie witness = sturdy(ctx, wallDx - 0.8, 1.0);
            Tracker<RocketProjectile> rocket = new Tracker<>(ctx, RocketProjectile.class);
            GrenadeScenarios.CancelStart cancel = kind == Kind.CANCELLED ? new GrenadeScenarios.CancelStart() : null;
            if (cancel != null) {
                MinecraftForge.EVENT_BUS.register(cancel);
            }
            float healthBefore = rig.shooter.getHealth();
            pull(rig);
            Runnable done = () -> {
                restore(savedBlocks, savedSelf);
                if (cancel != null) {
                    MinecraftForge.EVENT_BUS.unregister(cancel);
                }
            };
            return ScenarioResult.pending(name(), 60, ticks -> {
                rig.tick();
                RocketProjectile r = rocket.poll(ctx);
                if (r == null || !r.isDead) {
                    if (ticks >= 55) {
                        done.run();
                        return ScenarioResult.fail(name(), "the rocket never exploded");
                    }
                    return null;
                }
                done.run();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                double dx = witness.posX - r.posX;
                double dy = witness.posY - r.posY;
                double dz = witness.posZ - r.posZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                float splash = lossFor(witness, LauncherMath.splashDamage(50.0, 1.75, distance));
                int air = airInWall(ctx, wallDx);
                switch (kind) {
                    case SPLASH:
                        out.checkEquals("wall blocks broken", 0, air);
                        out.check(
                            Math.abs(lost(witness) - splash) < EPS && splash > 0.0F,
                            "the witness (distance " + distance
                                + ") should lose "
                                + splash
                                + " but lost "
                                + lost(witness));
                        out.check(
                            rig.shooter.getHealth() == healthBefore,
                            "the shooter 5 blocks away should be untouched");
                        break;
                    case BLOCKS:
                        out.check(air > 0, "block damage on: the rocket should break dirt at the impact");
                        out.check(
                            Math.abs(lost(witness) - splash) < EPS,
                            "block damage must not change entity damage, expected " + splash
                                + " lost "
                                + lost(witness));
                        break;
                    case SELF:
                        out.check(
                            rig.shooter.getHealth() < healthBefore,
                            "the shooter in the blast should be hurt (PFX-25), health " + rig.shooter.getHealth());
                        break;
                    case NOSELF:
                        out.check(
                            rig.shooter.getHealth() == healthBefore,
                            "with selfDamage off the shooter should be unhurt, health " + rig.shooter.getHealth());
                        break;
                    default:
                        out.checkEquals("wall blocks broken", 0, air);
                        out.check(
                            lost(witness) == 0.0F,
                            "a cancelled ExplosionEvent.Start must stop all damage, lost " + lost(witness));
                        break;
                }
                return out.build();
            });
        }
    }

    // ----- grenade -----------------------------------------------------------------------------------------------

    /** A 40 mm grenade into a wall: it bounces, loses 20 ticks of fuse and lives on; it ends before its lifetime. */
    private static final class GrenadeBounce implements Scenario {

        @Override
        public String name() {
            return "gun.grenade_launcher.bounce";
        }

        @Override
        public String description() {
            return "A 40 mm grenade fired at a stone wall 6 blocks away bounces instead of exploding (bounces left 2 to 1), the bounce shortens its 100 tick fuse by 20, and it explodes on its own before the fuse's end.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            boolean savedBlocks = ExplosionsConfig.blockDamage;
            boolean savedSelf = ExplosionsConfig.selfDamage;
            restore(false, true);
            GunRig rig = new GunRig(ctx, GunItems.get(GrenadeLauncherDef.NAME), 6, CORRIDOR);
            sturdy(rig.shooter);
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    ctx.setBlock(6, dy, dz, Blocks.stone, 0);
                }
            }
            Tracker<GrenadeLauncherProjectile> grenade = new Tracker<>(ctx, GrenadeLauncherProjectile.class);
            pull(rig);
            int[] seenAt = { -1 };
            int[] fuseThen = { 0 };
            boolean[] aliveAtBounce = { false };
            return ScenarioResult.pending(name(), 140, ticks -> {
                rig.tick();
                GrenadeLauncherProjectile g = grenade.poll(ctx);
                if (g != null && seenAt[0] < 0 && g.getBouncesLeft() == 1) {
                    seenAt[0] = ticks;
                    fuseThen[0] = g.getFuseLeft();
                    aliveAtBounce[0] = !g.isDead;
                }
                if (g == null || !g.isDead) {
                    if (ticks >= 135) {
                        restore(savedBlocks, savedSelf);
                        return ScenarioResult.fail(name(), "the grenade never exploded");
                    }
                    return null;
                }
                restore(savedBlocks, savedSelf);
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(seenAt[0] > 0, "the grenade should have bounced off the wall (bounces left 1)");
                out.check(aliveAtBounce[0], "the grenade should be live at the bounce");
                // Fuse = 100 minus ticks flown minus 20 for the bounce; the poll count is within a few ticks of the
                // grenade's own.
                int expected = 100 - 20 - seenAt[0];
                out.check(
                    Math.abs(fuseThen[0] - expected) <= 4,
                    "the fuse after the bounce should be about " + expected
                        + " (100 - 20 - "
                        + seenAt[0]
                        + " ticks), it was "
                        + fuseThen[0]);
                out.check(ticks < 125, "the grenade should explode before its lifetime ends, at " + ticks);
                return out.build();
            });
        }
    }

    /** A 40 mm grenade into a zombie: the blast alone damages it, by the ramp at its distance from the blast. */
    private static final class GrenadeDirect implements Scenario {

        private final boolean blocks;

        GrenadeDirect(boolean blocks) {
            this.blocks = blocks;
        }

        @Override
        public String name() {
            return blocks ? "gun.grenade_launcher.blocks" : "gun.grenade_launcher.direct";
        }

        @Override
        public String description() {
            return blocks
                ? "With explosions.blockDamage on, a 40 mm grenade that hits a zombie in front of a dirt wall breaks some of the wall."
                : "A 40 mm grenade fired into a zombie 4 blocks away explodes on contact and the zombie loses exactly the 30 damage ramp at its distance from the blast (no separate direct hit); the dirt wall behind it stays whole with explosions.blockDamage off.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            boolean savedBlocks = ExplosionsConfig.blockDamage;
            boolean savedSelf = ExplosionsConfig.selfDamage;
            restore(blocks, true);
            GunRig rig = new GunRig(ctx, GunItems.get(GrenadeLauncherDef.NAME), 6, CORRIDOR);
            sturdy(rig.shooter);
            dirtWall(ctx, 5);
            EntityZombie target = sturdy(ctx, 4.0, 0.0);
            Tracker<GrenadeLauncherProjectile> grenade = new Tracker<>(ctx, GrenadeLauncherProjectile.class);
            pull(rig);
            return ScenarioResult.pending(name(), 60, ticks -> {
                rig.tick();
                GrenadeLauncherProjectile g = grenade.poll(ctx);
                if (g == null || !g.isDead) {
                    if (ticks >= 55) {
                        restore(savedBlocks, savedSelf);
                        return ScenarioResult.fail(name(), "the grenade never exploded");
                    }
                    return null;
                }
                restore(savedBlocks, savedSelf);
                double dx = target.posX - g.posX;
                double dy = target.posY - g.posY;
                double dz = target.posZ - g.posZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                float expected = lossFor(target, LauncherMath.splashDamage(30.0, 1.5, distance));
                int air = airInWall(ctx, 5);
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                if (blocks) {
                    out.check(air > 0, "block damage on: the blast should break some of the dirt wall");
                } else {
                    out.check(
                        Math.abs(lost(target) - expected) < EPS && expected > 0.0F,
                        "the zombie (distance " + distance + ") should lose " + expected + " but lost " + lost(target));
                    out.checkEquals("wall blocks broken", 0, air);
                }
                out.checkEquals("rounds left", 5, rig.loaded());
                return out.build();
            });
        }
    }
}
