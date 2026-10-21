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
 * The Thompson SMG (Techguns' "Submachine Gun").
 *
 * <p>
 * Spec: sheets-handguns-smgs.md, "Submachine Gun (Thompson)", section 3: damage 5 falling to 3 between 15 and 24
 * blocks, lifetime 25 ticks at 1.5 blocks per tick, spread 0.05, no armor penetration, no scope, full-auto every 3
 * ticks, 20 rounds from one SMG Magazine (worth 2 loose 9mm bullets: a forced reload refunds 1 per 10 rounds), 40
 * tick reload, AI range 18 / attack time 45 / burst 3 / delay 3, recoil 5 ticks and muzzle flash 5 ticks. One skin.
 *
 * <p>
 * Recipe (recipes.md, MV table): Iron Barrel, Iron Receiver, Wooden Stock, 2 aluminium plates, 4 aluminium screws and
 * 2 small aluminium springs; Assembler, MV, 20 s, circuit 1. The gate is the aluminium plate.
 */
public final class ThompsonDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "thompson_smg";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("thompson_smg")
        .damage(6.0)
        .damageDrop(15, 24, 3.5)
        .npcDamage(5.0, 3.0)
        .spread(0.05)
        .fire(FireMode.AUTO, 3)
        .magazine(20)
        .ammoType(AmmoIds.SMG_MAGAZINE)
        .looseAmmoType(AmmoIds.NINE_MM)
        .emptyContainerType(AmmoIds.SMG_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(40)
        .projectile(1.5, 25)
        .penetration(0)
        .animation(5, 5)
        .ai(18, 45, 3, 3)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.MV)
        .damageType(DamageType.PROJECTILE)
        .camos(1)
        .row(
            Row.assembler(
                "GR-10",
                NAME,
                1,
                Tier.MV,
                20,
                1,
                Input.mod(PartItems.BARREL_IRON, 1),
                Input.mod(PartItems.RECEIVER_IRON, 1),
                Input.mod(PartItems.STOCK_WOODEN, 1),
                Input.gt("plate", "Aluminium", 2),
                Input.gt("screw", "Aluminium", 4),
                Input.gt("springSmall", "Aluminium", 2)))
        .scenarios(registry -> MvGunScenarios.register(registry, NAME))
        .build();

    private ThompsonDef() {}
}
