package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.GunScenarios;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.PistolRecipes;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The 9mm Pistol.
 *
 * <p>
 * Spec: sheets-handguns-smgs.md, "9mm Pistol", section 3: damage 8 falling to 5 between 18 and 25 blocks (the later
 * builder call wins, GUN-16), lifetime 30 ticks at 1.5 blocks per tick, spread 0.025, no scope, semi-auto every 4
 * ticks, 18 rounds, 35 tick reload, AI range 24 / attack time 30 / burst 3 / delay 10, recoil 3 ticks and muzzle
 * flash 5 ticks.
 *
 * <p>
 * Recipe (docs/gregguns-spec/02-guns/recipes.md, LV table): Hardened Steel Barrel, Mechanical Parts T2, 2 steel plates,
 * 2 rubber plates (the grip stands in for Techguns' plastic, an MV material), 4 steel screws and a small steel spring;
 * Assembler, LV, 15 s, circuit 1. The gate is the steel plate.
 */
public final class PistolDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "pistol_9mm";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("pistol")
        .damage(8.0)
        .damageDrop(18, 25, 5.0)
        .spread(0.025)
        .fire(FireMode.SEMI, 4)
        .magazine(18)
        .ammoType(AmmoIds.NINE_MM_PISTOL_MAGAZINE)
        .looseAmmoType(AmmoIds.NINE_MM)
        .emptyContainerType(AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(35)
        .projectile(1.5, 30)
        .penetration(0)
        .animation(3, 5)
        .ai(24, 30, 3, 10)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.LV)
        .damageType(DamageType.PROJECTILE)
        .row(
            Row.assembler(
                "GR-01",
                NAME,
                1,
                Tier.LV,
                15,
                1,
                Input.mod(PartItems.BARREL_HARDENED, 1),
                Input.mod(PartItems.MECHANICAL_2, 1),
                Input.gt("plate", "Steel", 2),
                Input.gt("plate", "Rubber", 2),
                Input.gt("screw", "Steel", 4),
                Input.gt("springSmall", "Steel", 1)))
        .recycling(() -> PistolRecipes.recycling())
        .scenarios(registry -> GunScenarios.registerAll(registry))
        .build();

    private PistolDef() {}
}
