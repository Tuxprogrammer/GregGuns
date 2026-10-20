package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.logic.ShotPattern;
import com.gregguns.gun.server.EvBallisticScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Pulse Rifle: a 12-round EV rifle whose every pull is a 3-round burst at 1.5 times the usual bullet speed, with a
 * scope.
 *
 * <p>
 * Spec: sheets-rifles-shotguns.md, "Pulse Rifle", section 3: damage 10 falling to 8 between 25 and 35 blocks, lifetime
 * 50 ticks at 2.25 blocks per tick, spread 0.024, armor penetration 25%, full-auto every 7 ticks, one burst line of 1 +
 * 2 projectiles per round, 12 rounds from an Advanced Magazine (worth 3 Advanced Bullets, 4 rounds each), 45 tick
 * reload, scope FOV x0.35 and spread x0.5 while zoomed, AI range 24 / attack time 30 / no burst, recoil 8 ticks and
 * muzzle flash 5 ticks. Three skins: the base, Gray and Dark Green.
 *
 * <p>
 * Recipe (recipes.md, EV table): Carbon Barrel, Carbon Receiver, Carbon Gunstock, a diamond lens, an EV (Data) circuit,
 * 2 titanium plates and 4 titanium screws; Assembler, EV, 35 s, circuit 3.
 */
// Deviation: recipes.md adds 144 L of soldering alloy to the Pulse Rifle's recipe. ProductionTable rows carry no fluid
// input yet (the M2 guns have none), so the row omits it; the gate material and the circuit are unchanged.
public final class PulseRifleDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "pulse_rifle";

    /** The stats. */
    // Deviation: the AI attack time variance (10 ticks) is dropped, as for the other guns.
    public static final GunStats STATS = GunStats.builder("pulse_rifle")
        .damage(10.0)
        .damageDrop(25, 35, 8.0)
        .spread(0.024)
        .zoom(0.35, 0.5)
        .fire(FireMode.AUTO, 7)
        .magazine(12)
        .ammoType(AmmoIds.ADVANCED_MAGAZINE)
        .looseAmmoType(AmmoIds.ADVANCED_BULLET)
        .emptyContainerType(AmmoIds.ADVANCED_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(45)
        .projectile(2.25, 50)
        .penetration(0.25)
        .pattern(ShotPattern.burstLine(2))
        .animation(8, 5)
        .ai(24, 30, 0, 1)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.EV)
        .damageType(DamageType.PROJECTILE)
        .camos(3)
        .row(
            Row.assembler(
                "GR-61",
                NAME,
                1,
                Tier.EV,
                35,
                3,
                Input.mod(ComponentItems.BARREL_CARBON, 1),
                Input.mod(ComponentItems.RECEIVER_CARBON, 1),
                Input.mod(ComponentItems.STOCK_CARBON, 1),
                Input.gt("lens", "Diamond", 1),
                Input.gt("circuit", "Data", 1),
                Input.gt("plate", "Titanium", 2),
                Input.gt("screw", "Titanium", 4)))
        .scenarios(registry -> EvBallisticScenarios.register(registry, NAME))
        .build();

    private PulseRifleDef() {}
}
