package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.RevolverScenarios;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;
import com.gregguns.recipe.RevolverRecipes;

/**
 * The Revolver.
 *
 * <p>
 * Spec: sheets-handguns-smgs.md, "Revolver", section 3: damage 8 falling to 6 between 12 and 20 blocks, lifetime 25
 * ticks at 1.5 blocks per tick, spread 0.025, no scope, semi-auto every 6 ticks, 6 rounds loaded straight from one 9mm
 * bullet item (no magazine, no empty container), 45 tick reload, AI range 18 / attack time 90 / burst 6 / delay 20,
 * recoil 6 ticks and muzzle flash 5 ticks.
 *
 * <p>
 * Recipe (recipes.md, LV table): Iron Barrel, Mechanical Parts, Wooden Stock, 2 steel plates, 4 steel screws and a
 * small steel spring; Assembler, LV, 15 s, circuit 2. Flint and Steel is dropped (the hammer is part of the
 * mechanism). The gate is the steel plate.
 */
public final class RevolverDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "revolver";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("revolver")
        .damage(8.0)
        .damageDrop(12, 20, 6.0)
        .spread(0.025)
        .fire(FireMode.SEMI, 6)
        .magazine(6)
        .ammoType(AmmoIds.NINE_MM)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(45)
        .projectile(1.5, 25)
        .penetration(0)
        .animation(6, 5)
        .ai(18, 90, 6, 20)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.LV)
        .damageType(DamageType.PROJECTILE)
        .row(
            Row.assembler(
                "GR-02",
                NAME,
                1,
                Tier.LV,
                15,
                2,
                Input.mod(PartItems.BARREL_IRON, 1),
                Input.mod(PartItems.MECHANICAL_1, 1),
                Input.mod(PartItems.STOCK_WOODEN, 1),
                Input.gt("plate", "Steel", 2),
                Input.gt("screw", "Steel", 4),
                Input.gt("springSmall", "Steel", 1)))
        .recycling(() -> RevolverRecipes.recycling())
        .scenarios(registry -> RevolverScenarios.registerAll(registry))
        .build();

    private RevolverDef() {}
}
