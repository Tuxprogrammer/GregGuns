package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.server.NuclearDeathRayScenarios;

/**
 * The Nuclear Death Ray: a ZPM energy gun that holds a continuous radiation beam. While the trigger is held it fires
 * every tick; each tick's {@code BeamProjectile} refreshes the shooter's beam, traces 48 blocks and burns every entity
 * on the line once with RADIATION damage (6 down to 1 between 20 and 40 blocks, 35 percent armor penetration). The beam
 * plays its own start and loop sounds, so the gun does not set {@code startsLoop} and its per-shot fire sound is
 * silent.
 *
 * <p>
 * Spec: sheets-energy-special.md, "Nuclear Death Ray", section 3: damage 6 falling to 1 from 20 to 40 blocks, spread
 * 0, penetration 0.35, recoil 5 and muzzle flash 5 ticks, AI range 24 / attack time 40 / burst 5 every 5 ticks.
 *
 * <p>
 * Deviations (docs/gregguns-spec/02-guns/index.md, batch M5 B6): Techguns feeds the gun a Nuclear Power Cell (100
 * rounds, one per 5 ticks) and returns a Depleted cell. GregGuns runs it on EU like every energy gun (plan Q2,
 * accepted): a full ZPM charge (10,000,000,000 EU, the GT {@code Energy_Module} baseline) is those 100 rounds at the
 * old rate, 500 ticks of beam, so one tick of beam costs 20,000,000 EU, charged as one shot per tick. There is no
 * reload (plan Q5). The Nuclear Power Cell stays a ZPM component and a radiation hazard item; it is not ammo. The
 * recipe is an Assembly Line spec ({@code NdrLineTable}, GR-103), not an Assembler row, because its inputs do not fit
 * the Assembler.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Nuclear Death Ray);
// docs/gregguns-spec/02-guns/recipes.md (LuV and ZPM table); energy-electric-items.md
public final class NuclearDeathRayDef {

    /** The registry name, sound prefix and texture name. */
    public static final String NAME = "nuclear_death_ray";

    /** Beam ticks a full charge lasts: Techguns' 100 rounds at one per 5 ticks. */
    public static final int FULL_CHARGE_TICKS = 500;

    /** The ZPM baseline capacity, EU (a GT Energy Module). */
    public static final long CAPACITY_EU = 10_000_000_000L;

    /** EU per tick of beam: the capacity over {@link #FULL_CHARGE_TICKS}. */
    public static final long EU_PER_SHOT = CAPACITY_EU / FULL_CHARGE_TICKS;

    /** The stats. */
    public static final GunStats STATS = GunStats.builder(NAME)
        .damage(6.0)
        .damageDrop(20.0, 40.0, 1.0)
        .spread(0.0)
        .fire(FireMode.AUTO, 1)
        .energy(EU_PER_SHOT, CAPACITY_EU)
        .projectile(7.5, 5)
        .projectileKind(ProjectileKind.BEAM)
        .penetration(0.35)
        .animation(5, 5)
        .ai(24, 40, 5, 5)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.ZPM)
        .damageType(DamageType.RADIATION)
        .scenarios(registry -> NuclearDeathRayScenarios.registerAll(registry))
        .build();

    private NuclearDeathRayDef() {}
}
