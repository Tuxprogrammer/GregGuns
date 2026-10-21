package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.server.EvEnergyScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Laser Rifle: an EV energy rifle whose every pull is an instant beam of 12 flat damage, four times a second,
 * with a light scope.
 *
 * <p>
 * Spec: sheets-energy-special.md, "Laser Rifle", section 3: damage 12 with no drop-off, 5 ticks at 7.5 blocks per
 * tick (37.5 blocks of reach), spread 0, full-auto every 5 ticks, scope FOV x0.75 and spread x0.75 while zoomed, AI
 * range 24 / attack time 30 / no burst, recoil 5 ticks and muzzle flash 5 ticks. Techguns loaded 45 rounds from an
 * Energy Cell; GregGuns drops the cell: the gun is its own battery (design 0003) and a full charge holds 45 shots.
 *
 * <p>
 * Recipe (recipes.md, EV table, cfg 1): Laser Barrel, Hardened Receiver, Plastic Gun Stock, Mechanical Parts T3, an
 * EV battery hull (full), an EV (Data) circuit, 2 titanium plates and 4 titanium screws; Assembler, EV, 30 s.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Laser Rifle); docs/gregguns-spec/02-guns/recipes.md (EV)
// Deviation: the shot is an instant hitscan (plan M5 Q1), not Techguns' 7.5 block per tick bolt; the reach is the same.
// Deviation: the AI attack time variance (10 ticks) is dropped, as for the other guns.
public final class LaserRifleDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "laser_rifle";

    /** Shots in a full charge (the Techguns clip). */
    public static final int SHOTS = 45;

    /** EU capacity of a full charge: the EV battery baseline (energy-electric-items.md), plan Q9. */
    public static final long CAPACITY_EU = EvEnergy.CAPACITY_EU;

    /** EU spent by one pull: the capacity divided by the Techguns clip, so a full charge is {@value #SHOTS} shots. */
    public static final long EU_PER_SHOT = CAPACITY_EU / SHOTS;

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("laser_rifle")
        .damage(18.0)
        .npcDamage(12.0)
        .spread(0.0)
        .zoom(0.75, 0.75)
        .fire(FireMode.AUTO, 5)
        .energy(EU_PER_SHOT, CAPACITY_EU)
        .projectile(7.5, 5)
        .projectileKind(ProjectileKind.LASER)
        .penetration(0)
        .animation(5, 5)
        .ai(24, 30, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.EV)
        .damageType(DamageType.ENERGY)
        .row(
            Row.assembler(
                "GR-70",
                NAME,
                1,
                Tier.EV,
                30,
                1,
                Input.mod(ComponentItems.BARREL_LASER, 1),
                Input.mod(ComponentItems.RECEIVER_HARDENED, 1),
                Input.mod(ComponentItems.STOCK_PLASTIC, 1),
                Input.mod(ComponentItems.MECHANICAL_3, 1),
                Input.itemList("BatteryHull_EV_Full", 1),
                Input.gt("circuit", "Data", 1),
                Input.gt("plate", "Titanium", 2),
                Input.gt("screw", "Titanium", 4))
                .molten("SolderingAlloy", 144))
        .scenarios(registry -> EvEnergyScenarios.register(registry, NAME))
        .build();

    private LaserRifleDef() {}
}
