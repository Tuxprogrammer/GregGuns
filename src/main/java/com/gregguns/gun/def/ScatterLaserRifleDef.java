package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ShotPattern;
import com.gregguns.gun.server.EnergyBatchScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Scatter Laser Rifle: an IV energy rifle whose every pull fires five laser beams in a fan.
 *
 * <p>
 * Spec: sheets-energy-special.md, "Scatter Laser Rifle [WIP]", section 3: 6 damage per beam with no drop-off, 7.5
 * blocks per tick for 5 ticks (37.5 blocks of reach, an instant hitscan here as for the Laser Rifle), main spread 0.1,
 * 5 beams at fan spread 0.15, full-auto every 7 ticks, scope x0.75 with spread x0.75 while zoomed, AI range 18.
 *
 * <p>
 * Deviation: Techguns has no recipe, no loot and no user for it (creative tab only); GregGuns gives it the recipes.md
 * IV row (design record 0001, plan Q3). The 40-round clip is 40 shots of charge at 2,500,000 EU each of the
 * 100,000,000 EU IV baseline; no reload (plan Q5). The fan ignores the main spread: all five beams roll
 * {@code 0.15 x 40} degrees ({@link ShotPattern#fan}), where Techguns rolled one beam at 0.1 and four at 0.15.
 *
 * <p>
 * Recipe: 2 Laser Barrels, Carbon Receiver, Carbon Gunstock, 2 IV Emitters, a Lapotronic Orb (consumed), 2 Elite
 * circuits, 3 tungsten steel plates and 6 screws. The gate is the tungsten steel plate.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (IV table, Scatter Laser Rifle); techguns-spec projectiles.md 3.13
public final class ScatterLaserRifleDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "scatter_laser_rifle";

    /** Beams per shot. */
    public static final int BEAMS = 5;

    /** EU per shot: 100,000,000 EU over 40 rounds. */
    public static final long EU_PER_SHOT = 2_500_000L;

    /** The IV baseline capacity, EU (a Lapotronic Orb). */
    public static final long CAPACITY_EU = 100_000_000L;

    /** The stats. */
    public static final GunStats STATS = GunStats.builder(NAME)
        .damage(6.0)
        .spread(0.1)
        .zoom(0.75, 0.75)
        .fire(FireMode.AUTO, 7)
        .energy(EU_PER_SHOT, CAPACITY_EU)
        .projectile(7.5, 5)
        .projectileKind(ProjectileKind.LASER)
        .pattern(ShotPattern.fan(BEAMS, 0.15))
        .animation(5, 5)
        .ai(18, 30, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.IV)
        .damageType(DamageType.ENERGY)
        .row(
            Row.assembler(
                "GR-82",
                NAME,
                1,
                Tier.IV,
                45,
                3,
                Input.mod(ComponentItems.BARREL_LASER, 2),
                Input.mod(ComponentItems.RECEIVER_CARBON, 1),
                Input.mod(ComponentItems.STOCK_CARBON, 1),
                Input.itemList("Emitter_IV", 2),
                Input.itemList("Energy_LapotronicOrb", 1),
                Input.gt("circuit", "Elite", 2),
                Input.gt("plate", "TungstenSteel", 3),
                Input.gt("screw", "TungstenSteel", 6)))
        .scenarios(registry -> EnergyBatchScenarios.registerScatter(registry, NAME))
        .build();

    private ScatterLaserRifleDef() {}
}
