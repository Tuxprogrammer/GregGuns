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
 * The Tesla Rifle: an EV energy rifle whose bolt hits at once and jumps to up to three more creatures within 5 blocks
 * of the last, 12 lightning damage each, 2.5 times a second, with a light scope.
 *
 * <p>
 * Spec: sheets-energy-special.md, "Tesla Rifle", section 3: damage 12 with no drop-off, speed 5 for 10 ticks (50
 * blocks of reach), spread 0, full-auto every 8 ticks, scope FOV x0.75 and spread x1.0 while zoomed, AI range 24 /
 * attack time 30 / no burst, recoil 5 ticks and muzzle flash 10 ticks. Techguns loaded 25 rounds from an Energy Cell;
 * GregGuns drops the cell: the gun is its own battery (design 0003) and a full charge holds 25 shots. The chain (3
 * jumps, range 5, no damage drop per jump) is {@code TeslaProjectile}; the FX is {@code TeslaFlare} at every target.
 *
 * <p>
 * Recipe (recipes.md, EV table, cfg 5): Carbon Receiver, Carbon Gunstock, an EV emitter (the Techguns Coil), an EV
 * battery hull (full), an EV (Data) circuit, 2 titanium plates and 4 titanium screws; Assembler, EV, 30 s.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Tesla Rifle); docs/gregguns-spec/02-guns/recipes.md (EV)
// Deviation: the shot is an instant hitscan with the chain resolved in the same tick (I4), not a 10 tick bolt.
// Deviation: the AI attack time variance (10 ticks) is dropped, as for the other guns.
public final class TeslaRifleDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "tesla_rifle";

    /** Shots in a full charge (the Techguns clip). */
    public static final int SHOTS = 25;

    /** EU capacity of a full charge: the EV battery baseline (energy-electric-items.md), plan Q9. */
    public static final long CAPACITY_EU = EvEnergy.CAPACITY_EU;

    /** EU spent by one pull: the capacity divided by the Techguns clip. */
    public static final long EU_PER_SHOT = CAPACITY_EU / SHOTS;

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("tesla_rifle")
        .damage(12.0)
        .spread(0.0)
        .zoom(0.75, 1.0)
        .fire(FireMode.AUTO, 8)
        .energy(EU_PER_SHOT, CAPACITY_EU)
        .projectile(5.0, 10)
        .projectileKind(ProjectileKind.TESLA)
        .penetration(0)
        .animation(5, 10)
        .ai(24, 30, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.EV)
        .damageType(DamageType.LIGHTNING)
        .row(
            Row.assembler(
                "GR-71",
                NAME,
                1,
                Tier.EV,
                30,
                5,
                Input.mod(ComponentItems.RECEIVER_CARBON, 1),
                Input.mod(ComponentItems.STOCK_CARBON, 1),
                Input.itemList("Emitter_EV", 1),
                Input.itemList("BatteryHull_EV_Full", 1),
                Input.gt("circuit", "Data", 1),
                Input.gt("plate", "Titanium", 2),
                Input.gt("screw", "Titanium", 4))
                .molten("SolderingAlloy", 144))
        .scenarios(registry -> EvEnergyScenarios.register(registry, NAME))
        .build();

    private TeslaRifleDef() {}
}
