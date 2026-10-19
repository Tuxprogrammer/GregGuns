package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.SawedOffScenarios;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;
import com.gregguns.recipe.SawedOffRecipes;

/**
 * The Sawed Off, a double-barrel shotgun.
 *
 * <p>
 * Spec: sheets-rifles-shotguns.md, "Sawed Off", section 3: 4 damage per pellet falling to 1.5 between 1 and 4 blocks,
 * lifetime 10 ticks at 1.5 blocks per tick, spread 0.01, 7 extra pellets at spread 0.2 (8 in all, one round per
 * shot), full-auto every 4 ticks, 2 rounds, 28 tick reload, AI range 12 / attack time 60 / burst 2 / delay 20,
 * recoil 5 ticks and muzzle flash 5 ticks. It loads shell by shell: up to 2 shells per reload, one round each, and
 * the rounds already in the gun stay (so a forced reload tops up).
 *
 * <p>
 * Recipe (recipes.md, LV table): two Iron Barrels, a Wooden Stock, a steel plate, 2 steel screws and a small steel
 * spring; Assembler, LV, 10 s, circuit 3. Flint and Steel is dropped (the hammers are part of the mechanism). The gate
 * is the steel plate.
 */
public final class SawedOffDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "sawed_off";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("sawed_off")
        .damage(4.0)
        .damageDrop(1, 4, 1.5)
        .spread(0.01)
        .pellets(7, 0.2, false)
        .fire(FireMode.AUTO, 4)
        .magazine(2)
        .ammoType(AmmoIds.SHOTGUN_SHELL)
        .reloadMode(ReloadMode.INCREMENTAL, 2)
        .reloadTicks(28)
        .projectile(1.5, 10)
        .penetration(0)
        .animation(5, 5)
        .ai(12, 60, 2, 20)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.LV)
        .damageType(DamageType.PROJECTILE)
        .row(
            Row.assembler(
                "GR-03",
                NAME,
                1,
                Tier.LV,
                10,
                3,
                Input.mod(PartItems.BARREL_IRON, 2),
                Input.mod(PartItems.STOCK_WOODEN, 1),
                Input.gt("plate", "Steel", 1),
                Input.gt("screw", "Steel", 2),
                Input.gt("springSmall", "Steel", 1)))
        .recycling(() -> SawedOffRecipes.recycling())
        .scenarios(registry -> SawedOffScenarios.registerAll(registry))
        .build();

    private SawedOffDef() {}
}
