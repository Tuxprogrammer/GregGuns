package com.gregguns.gun.server;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.ItemStack;

import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.NuclearDeathRayDef;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.GunItems;
import com.gregguns.projectile.MovingBeams;
import com.gregguns.recipe.NdrLineScenarios;

/**
 * The {@code gun.nuclear_death_ray.*} scenarios: holding the trigger fires a beam tick every tick and spends one
 * shot's EU per tick, the beam burns a target by the RADIATION figure each tick, and it stops when the charge runs out.
 * Through the same server handler as the other guns, using {@link GunRig}. The recipe scenarios are
 * {@link NdrLineScenarios}. Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Nuclear Death Ray); docs/techguns-spec/04-projectiles-fx/
// projectiles.md (MovingBeam); docs/gregguns-spec/02-guns/energy-electric-items.md; docs/testing/README.md
public final class NuclearDeathRayScenarios {

    private static final GunStats STATS = NuclearDeathRayDef.STATS;
    private static final int HELD_TICKS = 6;
    private static final int RANGE = 6;
    /** Slack for float rounding and the few radiation side effects a hit may add over the scenario's ticks. */
    private static final double ALLOWANCE = 2.0;

    private NuclearDeathRayScenarios() {}

    /**
     * Registers the Nuclear Death Ray scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fire());
        registry.register(new Drain());
        NdrLineScenarios.registerAll(registry);
    }

    private static GunRig chargedRig(ScenarioContext ctx, int shots) {
        GunRig rig = new GunRig(ctx, GunItems.get(NuclearDeathRayDef.NAME), 0, RANGE + 4);
        ElectricGear.charge(rig.gun, (double) STATS.euPerShot() * shots, NuclearDeathRayDef.DEFINITION.tier());
        return rig;
    }

    private static long stored(ItemStack gun) {
        return (long) Math.floor(ElectricGear.charge(gun));
    }

    private static EntityZombie tankyZombie(ScenarioContext ctx, int dx) {
        EntityZombie z = GunRig.zombie(ctx, dx);
        z.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(400.0);
        z.setHealth(400.0F);
        z.hurtResistantTime = 0;
        return z;
    }

    /**
     * What one tick of beam costs the zombie after the damage model: full damage (inside 20 blocks), 35 percent pen.
     */
    private static float perTick(EntityZombie z) {
        double armor = z.getTotalArmorValue();
        return (float) DamageModel.resolve(
            DamageType.RADIATION,
            STATS.damage(),
            STATS.penetration(),
            share -> share * DamageModel.vanillaArmorFactor(armor));
    }

    /** Holding the trigger spends one shot's EU every tick and burns the target every tick. */
    private static final class Fire implements Scenario {

        @Override
        public String name() {
            return "gun.nuclear_death_ray.fire";
        }

        @Override
        public String description() {
            return "Holding the trigger on a charged Nuclear Death Ray fires a beam tick every tick: each tick spends exactly 20,000,000 EU from the stack, burns a zombie 6 blocks away for the 6 RADIATION damage after the damage model with 35 percent penetration, and keeps one live beam; there is no reload and no round change.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            MovingBeams.clear();
            GunRig rig = chargedRig(ctx, 40);
            EntityZombie z = tankyZombie(ctx, RANGE);
            long start = stored(rig.gun);
            int[] ticked = { 0 };
            rig.press();
            return ScenarioResult.pending(name(), HELD_TICKS + 12, ticks -> {
                if (ticks <= HELD_TICKS) {
                    ticked[0]++;
                }
                if (ticks == HELD_TICKS + 1) {
                    rig.release();
                }
                rig.tick();
                if (ticks < HELD_TICKS + 3) {
                    return null;
                }
                long spent = start - stored(rig.gun);
                int n = (int) (spent / STATS.euPerShot());
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("EU spent is a whole number of shots", 0L, spent % STATS.euPerShot());
                out.check(
                    n >= ticked[0] - 1 && n <= ticked[0] + 1,
                    "one shot per tick held: " + ticked[0] + " ticks held but " + n + " shots");
                float lost = z.getMaxHealth() - z.getHealth();
                // One damage tick per entity per world tick (MovingBeam): the harness may run two shots inside one
                // world tick, and the second deals nothing, so one beam tick fewer than the shots is allowed.
                float expected = n * perTick(z);
                float floor = (n - 1) * perTick(z);
                out.check(
                    lost >= floor - 0.05F * n && lost <= expected + ALLOWANCE,
                    "zombie should lose " + (n - 1)
                        + " to "
                        + n
                        + " x "
                        + perTick(z)
                        + " = "
                        + floor
                        + " to "
                        + expected
                        + " but lost "
                        + lost);
                out.checkEquals("live beams", 1, com.gregguns.projectile.MovingBeams.size());
                out.check(STATS.isEnergy(), "an energy gun");
                out.checkEquals("fire rate (ticks)", 1, STATS.fireRateTicks());
                out.check(!rig.reloading(), "an energy gun never reloads");
                MovingBeams.clear();
                return out.build();
            });
        }
    }

    /** A charge for three ticks of beam gives exactly three ticks of beam, then nothing. */
    private static final class Drain implements Scenario {

        private static final int SHOTS = 3;

        @Override
        public String name() {
            return "gun.nuclear_death_ray.drain";
        }

        @Override
        public String description() {
            return "A Nuclear Death Ray charged for exactly 3 ticks of beam spends all of it over 3 ticks while the trigger is held and burns the zombie for exactly 3 beam ticks; further ticks spend nothing and deal nothing.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            MovingBeams.clear();
            GunRig rig = chargedRig(ctx, SHOTS);
            EntityZombie z = tankyZombie(ctx, RANGE);
            rig.press();
            return ScenarioResult.pending(name(), HELD_TICKS + 12, ticks -> {
                rig.tick();
                if (ticks < HELD_TICKS + 4) {
                    return null;
                }
                rig.release();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("EU left", 0L, stored(rig.gun));
                float lost = z.getMaxHealth() - z.getHealth();
                float expected = SHOTS * perTick(z);
                out.check(
                    lost >= expected - 0.15F && lost <= expected + ALLOWANCE,
                    "zombie should lose 3 x " + perTick(z) + " = " + expected + " but lost " + lost);
                MovingBeams.clear();
                return out.build();
            });
        }
    }
}
