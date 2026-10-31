package com.gregguns.icbm.warhead;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.blast.BlastEngine;
import com.gregguns.icbm.blast.DamageRamp;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;

/**
 * The I7 balance pass's damage scenarios (0018 §6-7, D34, D328): every damaging warhead, at a full casing, measured
 * on live mobs. A mob with far more health than any warhead deals stands at ground zero and another at half the radius;
 * each one's health loss is the damage the blast really dealt after every handler in the pack. Ground zero must reach
 * the tier's target (T1 1,200, T2 3,000, T3 8,000, T4 16,000) and match the engine's own number, and half the radius
 * must follow the ramp. Nuclear and antimatter must kill a Wither and a mob with 100,000 health in the kill zone. The
 * measured numbers are written as notes, which the balance table in the spec copies. Terrain damage is off, so the
 * runs leave no crater.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-types.md (Balance pass, D328)
final class BalanceScenarios {

    /** Health of the measuring mob: well above any center damage, so it never dies and its loss is the damage. */
    static final double TANK_HEALTH = 100000.0;
    /** How close the measured ramp must follow {@link DamageRamp}, as a share of the expected damage. */
    static final double RAMP_TOLERANCE = 0.05;
    /**
     * Ticks the scenario waits after laying its arena and spawning the mobs before it detonates: the server works off
     * the arena's block writes (seconds on the full pack) and the mobs tick in the world first (D344).
     */
    static final int SETTLE_TICKS = 40;

    private BalanceScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        for (WarheadBlast blast : WarheadBlast.values()) {
            if (blast.damaging() && blast.kind() != WarheadBlast.Kind.NUCLEAR
                && blast.kind() != WarheadBlast.Kind.ANTIMATTER) {
                registry.register(new CenterDamage(blast));
            }
        }
        registry.register(new KillZone(WarheadBlast.NUCLEAR));
        registry.register(new KillZone(WarheadBlast.ANTIMATTER));
    }

    /** A cow with {@link #TANK_HEALTH} health, or null when the pack caps max health below it. */
    private static EntityCow tank(ScenarioContext ctx, double x, double z) {
        World w = ctx.world();
        EntityCow cow = ctx.spawnAt(new EntityCow(w), x, ctx.originY(), z, 0, 0);
        cow.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(TANK_HEALTH);
        cow.setHealth((float) TANK_HEALTH);
        cow.hurtResistantTime = 0;
        return cow.getHealth() < TANK_HEALTH - 1.0 ? null : cow;
    }

    /**
     * @return null when every mob is alive, in the world's entity list and has ticked there; otherwise why not, so a
     *         blast is never measured on a mob the world does not hold (it would read as 0 damage)
     */
    private static String unready(World w, EntityCow... mobs) {
        for (EntityLivingBase m : mobs) {
            if (m.isDead || !w.loadedEntityList.contains(m)) {
                return m.getClass()
                    .getSimpleName() + " is not in the world after "
                    + SETTLE_TICKS
                    + " ticks (dead "
                    + m.isDead
                    + ")";
            }
            if (m.ticksExisted <= 0) {
                return m.getClass()
                    .getSimpleName() + " has not ticked in "
                    + SETTLE_TICKS
                    + " ticks";
            }
        }
        return null;
    }

    /** Puts a waiting mob back on its mark, still, at full health. */
    private static void ready(EntityLivingBase m, double x, double y, double z) {
        m.setPositionAndUpdate(x, y, z);
        m.motionX = 0.0;
        m.motionY = 0.0;
        m.motionZ = 0.0;
        m.setHealth(m.getMaxHealth());
        m.hurtResistantTime = 0;
    }

    private static void floor(ScenarioContext ctx, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
            }
        }
    }

    /** Saves the terrain switches, turns them off, and returns a restorer. */
    private static Runnable terrainOff() {
        final boolean t1 = IcbmConfig.terrainDamageT1;
        final boolean t2 = IcbmConfig.terrainDamageT2;
        final boolean t3 = IcbmConfig.terrainDamageT3;
        final boolean t4 = IcbmConfig.terrainDamageT4;
        final int tier = IcbmConfig.maxBlastTier;
        IcbmConfig.terrainDamageT1 = false;
        IcbmConfig.terrainDamageT2 = false;
        IcbmConfig.terrainDamageT3 = false;
        IcbmConfig.terrainDamageT4 = false;
        IcbmConfig.maxBlastTier = 5;
        return () -> {
            IcbmConfig.terrainDamageT1 = t1;
            IcbmConfig.terrainDamageT2 = t2;
            IcbmConfig.terrainDamageT3 = t3;
            IcbmConfig.terrainDamageT4 = t4;
            IcbmConfig.maxBlastTier = tier;
        };
    }

    /** A full conventional warhead: ground zero meets the tier target, half the radius follows the ramp. */
    private static final class CenterDamage implements Scenario {

        private final WarheadBlast blast;

        CenterDamage(WarheadBlast blast) {
            this.blast = blast;
        }

        @Override
        public String name() {
            return "icbm.balance.damage_" + blast.id();
        }

        @Override
        public String description() {
            return "A full " + blast.id()
                + " warhead deals at least its tier's center damage to a live mob at ground"
                + " zero, and the ramp's damage at half the radius.";
        }

        @Override
        public int areaRadius() {
            return 40;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final ExplosiveDef def = Explosives.get(blast.id());
            if (def == null) {
                return ScenarioResult.fail(name(), "the ICBM content is not registered");
            }
            ctx.clearArea(36, 20);
            floor(ctx, 36);
            final int fill = def.maxFill();
            final double radius = def.radius(fill);
            final double expected = def.damage(fill);
            final double half = Math.floor(radius * 0.5);
            final double expectedHalf = DamageRamp.at(expected, radius, half);
            final EntityCow zero = tank(ctx, ctx.originX() + 0.5, ctx.originZ() + 0.5);
            final EntityCow mid = tank(ctx, ctx.originX() + 0.5 + half, ctx.originZ() + 0.5);
            if (zero == null || mid == null) {
                return ScenarioResult.fail(name(), "could not raise a mob's health to " + (int) TANK_HEALTH);
            }
            final Runnable[] restore = { null };
            final int[] firedAt = { -1 };
            return ScenarioResult.pending(name(), SETTLE_TICKS + 20 * 40, ticks -> {
                if (firedAt[0] < 0) {
                    if (ticks < SETTLE_TICKS) {
                        return null;
                    }
                    String unready = unready(ctx.world(), zero, mid);
                    if (unready != null) {
                        zero.setDead();
                        mid.setDead();
                        return ScenarioResult.fail(name(), unready);
                    }
                    // A cow walks while it waits: put both back on their marks, at full health.
                    ready(zero, ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5);
                    ready(mid, ctx.originX() + 0.5 + half, ctx.originY(), ctx.originZ() + 0.5);
                    restore[0] = terrainOff();
                    BlastEngine.instance()
                        .clear();
                    // The blast center sits at the mobs' feet, as a warhead on the ground or a missile's impact does.
                    if (!IcbmBlasts.detonate(
                        ctx.world(),
                        ctx.originX() + 0.5,
                        ctx.originY() + 0.1,
                        ctx.originZ() + 0.5,
                        def,
                        fill,
                        null)) {
                        restore[0].run();
                        zero.setDead();
                        mid.setDead();
                        return ScenarioResult.fail(name(), "the engine did not accept the blast");
                    }
                    firedAt[0] = ticks;
                    return null;
                }
                if (!BlastEngine.instance()
                    .lastBlastDone() && ticks - firedAt[0] < 20 * 35) {
                    return null;
                }
                restore[0].run();
                double dealt = TANK_HEALTH - zero.getHealth();
                double dealtHalf = TANK_HEALTH - mid.getHealth();
                double target = blast.tier()
                    .minCenterDamage();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.note(
                    String.format(
                        "BALANCE %s fill=%d radius=%.1f center=%.0f (engine %.0f, target %.0f) at %.0f blocks=%.0f"
                            + " (ramp %.0f)",
                        blast.id(),
                        fill,
                        radius,
                        dealt,
                        expected,
                        target,
                        half,
                        dealtHalf,
                        expectedHalf));
                out.check(
                    dealt >= target - 0.5,
                    "ground zero took " + Math.round(dealt) + ", below the tier target " + Math.round(target));
                out.check(
                    Math.abs(dealt - expected) <= expected * 0.01 + 0.5,
                    "ground zero took " + Math.round(dealt) + ", not the engine's " + Math.round(expected));
                out.check(
                    Math.abs(dealtHalf - expectedHalf) <= expectedHalf * RAMP_TOLERANCE + 0.5,
                    "half the radius took " + Math.round(dealtHalf) + ", not the ramp's " + Math.round(expectedHalf));
                zero.setDead();
                mid.setDead();
                return out.build();
            });
        }
    }

    /** Nuclear and antimatter: a Wither and a 100,000-health mob in the kill zone both die (0018 §7). */
    private static final class KillZone implements Scenario {

        private final WarheadBlast blast;

        KillZone(WarheadBlast blast) {
            this.blast = blast;
        }

        @Override
        public String name() {
            return "icbm.balance.kill_zone_" + blast.id();
        }

        @Override
        public String description() {
            return "A " + blast.id()
                + " warhead (fill 1) kills a Wither and a mob with 100,000 health in its kill"
                + " zone.";
        }

        @Override
        public int areaRadius() {
            return 40;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final ExplosiveDef def = Explosives.get(blast.id());
            if (def == null) {
                return ScenarioResult.fail(name(), "the ICBM content is not registered");
            }
            ctx.clearArea(36, 20);
            floor(ctx, 36);
            World w = ctx.world();
            final EntityWither wither = ctx
                .spawnAt(new EntityWither(w), ctx.originX() + 3.5, ctx.originY(), ctx.originZ() + 0.5, 0, 0);
            wither.func_82215_s(0);
            final EntityCow cow = tank(ctx, ctx.originX() - 3.5, ctx.originZ() + 0.5);
            if (cow == null) {
                wither.setDead();
                return ScenarioResult.fail(name(), "could not raise a mob's health to " + (int) TANK_HEALTH);
            }
            final EntityLivingBase[] victims = { wither, cow };
            final Runnable restore = terrainOff();
            BlastEngine.instance()
                .clear();
            com.gregguns.explosion.nuclear.FalloutEngine.clear(w);
            if (!IcbmBlasts.detonate(w, ctx.originX() + 0.5, ctx.originY() + 0.1, ctx.originZ() + 0.5, def, 1, null)) {
                restore.run();
                wither.setDead();
                cow.setDead();
                return ScenarioResult.fail(name(), "the engine did not accept the blast");
            }
            return ScenarioResult.pending(name(), 20 * 60, ticks -> {
                if (!BlastEngine.instance()
                    .lastBlastDone() && ticks < 20 * 55) {
                    return null;
                }
                restore.run();
                com.gregguns.explosion.nuclear.FalloutEngine.clear(ctx.world());
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                for (EntityLivingBase e : victims) {
                    boolean dead = e.isDead || e.getHealth() <= 0.0F;
                    out.check(
                        dead,
                        e.getClass()
                            .getSimpleName() + " survived the kill zone with "
                            + e.getHealth());
                    if (!dead) {
                        e.setDead();
                    }
                }
                out.note("BALANCE " + blast.id() + " kill zone: Wither and the 100,000-health mob dead");
                return out.build();
            });
        }
    }
}
