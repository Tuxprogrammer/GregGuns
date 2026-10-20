package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ShotPattern;
import com.gregguns.gun.server.EvEnergyScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Sonic Rifle: an EV energy rifle that fires a ring volley of 16 sonic projectiles per pull, one hit per entity,
 * with knockback twice a bullet's, one shot per click.
 *
 * <p>
 * Spec: sheets-energy-special.md, "Sonic Rifle", section 3: damage 25 falling to 5 between 5 and 15 blocks, 20 ticks
 * at 1.5 blocks per tick, armor penetration 25%, semi-auto every 12 ticks, no scope, AI range 18 / attack time 40 / no
 * burst, recoil 5 ticks and muzzle flash 5 ticks. The volley is a centre projectile plus rings of 5 and 10 at 0.075
 * and 0.15 rad, all sharing one hit list ({@code SonicProjectile}); the spread stats are ignored. Techguns loaded 8
 * rounds from an Energy Cell; GregGuns drops the cell: the gun is its own battery (design 0003) and a full charge
 * holds 8 shots.
 *
 * <p>
 * Recipe (recipes.md, EV table, cfg 6): Sonic Emitter, Carbon Receiver, Carbon Gunstock, an EV battery hull (full), an
 * EV (Data) circuit, 2 titanium plates and 4 titanium screws; Assembler, EV, 30 s.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Sonic Rifle); per-gun-behavior.md (Sonic Rifle)
// Deviation: the AI attack time variance (13 ticks) is dropped, as for the other guns.
public final class SonicRifleDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "sonic_rifle";

    /** Shots in a full charge (the Techguns clip). */
    public static final int SHOTS = 8;

    /** EU capacity of a full charge: the EV battery baseline (energy-electric-items.md), plan Q9. */
    public static final long CAPACITY_EU = EvEnergy.CAPACITY_EU;

    /** EU spent by one pull: the capacity divided by the Techguns clip. */
    public static final long EU_PER_SHOT = CAPACITY_EU / SHOTS;

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("sonic_rifle")
        .damage(25.0)
        .damageDrop(5, 15, 5.0)
        .spread(0.0)
        .fire(FireMode.SEMI, 12)
        .energy(EU_PER_SHOT, CAPACITY_EU)
        .projectile(1.5, 20)
        .projectileKind(ProjectileKind.SONIC)
        .pattern(ShotPattern.ringVolley(2, 5, 0.075))
        .penetration(0.25)
        .knockback(2.0)
        .animation(5, 5)
        .ai(18, 40, 0, 0)
        .build();

    /** The definition. */
    // The tooltip shows the real type, PHYSICAL (Techguns' tg_sonic source); GUN-20's wrong "Projectile" is not
    // repeated.
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.EV)
        .damageType(DamageType.PHYSICAL)
        .row(
            Row.assembler(
                "GR-72",
                NAME,
                1,
                Tier.EV,
                30,
                6,
                Input.mod(ComponentItems.SONIC_EMITTER, 1),
                Input.mod(ComponentItems.RECEIVER_CARBON, 1),
                Input.mod(ComponentItems.STOCK_CARBON, 1),
                Input.itemList("BatteryHull_EV_Full", 1),
                Input.gt("circuit", "Data", 1),
                Input.gt("plate", "Titanium", 2),
                Input.gt("screw", "Titanium", 4))
                .molten("SolderingAlloy", 144))
        .scenarios(registry -> EvEnergyScenarios.register(registry, NAME))
        .build();

    private SonicRifleDef() {}
}
