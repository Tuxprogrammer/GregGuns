package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.MvGunScenarios;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The AK47 (Techguns' "Kalaschnikow").
 *
 * <p>
 * Spec: sheets-rifles-shotguns.md, "Kalaschnikow (AK47)", section 3: damage 9 falling to 5 between 20 and 30 blocks,
 * lifetime 35 ticks at 1.5 blocks per tick, spread 0.03, armor penetration 0.15, no scope, full-auto every 3 ticks, 30
 * rounds from one Assault Rifle Magazine (a forced reload refunds 1 bullet per 10 rounds), 45 tick reload, AI range
 * 24 / attack time 30 / burst 3 / delay 3, recoil 5 ticks and muzzle flash 5 ticks. Two skins: the base and Blue.
 *
 * <p>
 * Recipe (recipes.md, MV table): Hardened Steel Barrel, Iron Receiver, Wooden Stock, 2 wooden sticks (the foregrip), 2
 * aluminium plates, 4 aluminium screws and 2 small aluminium springs; Assembler, MV, 25 s, circuit 5. The gate is the
 * aluminium plate.
 */
public final class Ak47Def {

    /** The registry name and sound prefix. */
    public static final String NAME = "ak47";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("ak47")
        .damage(10.0)
        .damageDrop(20, 30, 5.5)
        .npcDamage(9.0, 5.0)
        .spread(0.03)
        .fire(FireMode.AUTO, 3)
        .magazine(30)
        .ammoType(AmmoIds.ASSAULT_RIFLE_MAGAZINE)
        .looseAmmoType(AmmoIds.RIFLE)
        .emptyContainerType(AmmoIds.ASSAULT_RIFLE_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(45)
        .projectile(1.5, 35)
        .penetration(0.15)
        .animation(5, 5)
        .ai(24, 30, 3, 3)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.MV)
        .damageType(DamageType.PROJECTILE)
        .camos(2)
        .row(
            Row.assembler(
                "GR-13",
                NAME,
                1,
                Tier.MV,
                25,
                5,
                Input.mod(PartItems.BARREL_HARDENED, 1),
                Input.mod(PartItems.RECEIVER_IRON, 1),
                Input.mod(PartItems.STOCK_WOODEN, 1),
                Input.gt("stick", "Wood", 2),
                Input.gt("plate", "Aluminium", 2),
                Input.gt("screw", "Aluminium", 4),
                Input.gt("springSmall", "Aluminium", 2)))
        .scenarios(registry -> MvGunScenarios.register(registry, NAME))
        .build();

    private Ak47Def() {}
}
