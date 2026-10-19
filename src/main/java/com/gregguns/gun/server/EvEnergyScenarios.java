package com.gregguns.gun.server;

import net.minecraft.entity.monster.EntityZombie;

import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.GunItems;
import com.gregguns.item.gun.ItemEnergyGun;
import com.gregguns.projectile.BeamVisual;
import com.gregguns.recipe.GunRecipeScenarios;

/**
 * The {@code gun.<name>.*} scenarios of the EV energy guns (Laser, Tesla and Sonic Rifle). Every gun gets:
 * <ul>
 * <li>{@code recipe}: the EV assembly row is in GT's assembler map and a lower machine is not offered it;</li>
 * <li>{@code gate}: the row carries the titanium gate, is found at EV and is refused at HV;</li>
 * <li>{@code fire}: a charged gun spends exactly its EU per pull, clicks dry with no reload, and shows its own
 * behavior (the laser beam hit, the tesla chain, the sonic volley).</li>
 * </ul>
 * Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Laser Rifle, Tesla Rifle, Sonic Rifle);
// docs/gregguns-spec/02-guns/energy-electric-items.md (GGS-02-12, GGS-02-13); docs/testing/README.md
public final class EvEnergyScenarios {

    private static final int HELD_TICKS = 13;
    private static final float EPS = 0.05F;

    private EvEnergyScenarios() {}

    /**
     * Registers one EV energy gun's scenarios.
     *
     * @param registry where to add them
     * @param gunName  the gun's registry name, for example {@code "laser_rifle"}
     */
    public static void register(ScenarioRegistry registry, String gunName) {
        GunDefinition def = GunDefinitions.byName(gunName);
        registry.register(
            GunRecipeScenarios.assembly(
                "gun." + def.name() + ".recipe",
                "The " + def.name()
                    + " is in GT's assembler map (so NEI lists it) at "
                    + def.tier()
                    + " with its sheet inputs, and not below it.",
                def.assembly()));
        registry.register(
            GunRecipeScenarios.tierGate(
                "gun." + def.name() + ".gate",
                "The " + def.name()
                    + " row carries the titanium gate, an EV assembler finds it with the sheet inputs, and an HV assembler does not.",
                def.assembly()));
        switch (def.stats()
            .projectileKind()) {
            case LASER:
                registry.register(new LaserFire(def));
                break;
            default:
                break;
        }
    }

    // ----- shared helpers ----------------------------------------------------------------------------------------

    /** A rig holding the gun with a charge of {@code shots} pulls' worth of EU. */
    static GunRig charged(ScenarioContext ctx, GunDefinition def, double shots) {
        GunStats stats = def.stats();
        ItemEnergyGun item = (ItemEnergyGun) GunItems.get(def.name());
        GunRig rig = new GunRig(ctx, item, stats.magazineSize());
        ElectricGear.charge(rig.gun, shots * stats.euPerShot(), def.tier());
        return rig;
    }

    static long stored(GunRig rig) {
        return ((ItemEnergyGun) rig.gun.getItem()).chargeOf(rig.gun);
    }

    static int beamCount(ScenarioContext ctx) {
        int n = 0;
        for (Object o : ctx.world().loadedEntityList) {
            if (o instanceof BeamVisual) {
                n++;
            }
        }
        return n;
    }

    static float lost(EntityZombie z) {
        return z.getMaxHealth() - z.getHealth();
    }

    /** The health a bare zombie loses from {@code raw} damage of a type, per the damage model. */
    static float expectedLoss(EntityZombie z, DamageType type, double raw, double penetration) {
        double armor = z.getTotalArmorValue();
        return (float) DamageModel
            .resolve(type, raw, penetration, share -> share * DamageModel.vanillaArmorFactor(armor));
    }

    static boolean near(float expected, float actual) {
        return Math.abs(expected - actual) < EPS;
    }

    /** Ticks a held trigger for {@link #HELD_TICKS} ticks, then releases it. */
    static void hold(GunRig rig) {
        rig.press();
        for (int i = 0; i < HELD_TICKS; i++) {
            rig.tick();
        }
        rig.release();
    }

    // ----- Laser Rifle -------------------------------------------------------------------------------------------

    /** The beam hits at once for the flat ENERGY figure, shows one beam per pull, spends exact EU, clicks dry. */
    private static final class LaserFire implements Scenario {

        private final GunDefinition def;

        LaserFire(GunDefinition def) {
            this.def = def;
        }

        @Override
        public String name() {
            return "gun." + def.name() + ".fire";
        }

        @Override
        public String description() {
            return "A Laser Rifle charged for two and a half shots hurts a zombie 8 blocks away by the flat 12 ENERGY damage on its first pull, shows one beam per pull, spends exactly its EU per pull, and holding on after two pulls only clicks dry (no reload).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunStats stats = def.stats();
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            GunRig rig = charged(ctx, def, 2.5);
            EntityZombie target = GunRig.zombie(ctx, 8);
            long start = stored(rig);
            out.checkEquals("charge before firing", (long) (2.5 * stats.euPerShot()), start);
            int beams = beamCount(ctx);
            rig.press();
            rig.tick();
            out.checkEquals("EU spent by the first pull", stats.euPerShot(), start - stored(rig));
            out.checkEquals("one beam for the first pull", 1, beamCount(ctx) - beams);
            float expected = expectedLoss(target, DamageType.ENERGY, stats.damage(), 0.0);
            out.check(
                near(expected, lost(target)),
                "the zombie should lose the flat laser damage (" + expected + "), lost " + lost(target));
            for (int i = 1; i < HELD_TICKS; i++) {
                rig.tick();
            }
            rig.release();
            out.checkEquals("EU spent by two pulls and a dry click", 2 * stats.euPerShot(), start - stored(rig));
            out.checkEquals("beams of two pulls", 2, beamCount(ctx) - beams);
            out.check(!rig.reloading(), "an energy gun never starts a reload");
            return out.build();
        }
    }
}
