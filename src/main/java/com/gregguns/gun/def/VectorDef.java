package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.MvGunScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Vector, the fastest-firing SMG: 25 rounds from an SMG Magazine at one round every 2 ticks.
 *
 * <p>
 * Spec: sheets-handguns-smgs.md, "Vector", section 3: damage 6 falling to 4 between 17 and 25 blocks, lifetime 30
 * ticks at 1.5 blocks per tick, spread 0.05, armor penetration 0.15, full-auto every 2 ticks, 40 tick reload, a light
 * scope with FOV x0.75 and spread x0.35 while zoomed, AI range 18 / attack time 35 / burst 3 / delay 2, recoil 2 ticks
 * and muzzle flash 5 ticks. A forced reload refunds 1 loose 9mm bullet per 12 rounds (the magazine is worth 2). Two
 * skins: the base and Black.
 *
 * <p>
 * Recipe (recipes.md, HV table, GR-32): Hardened Steel Barrel, Hardened Receiver, Plastic Gun Stock, a glass lens, 2
 * stainless steel plates, 4 stainless steel screws and 2 small stainless steel springs; Assembler, HV, 30 s, circuit
 * 3.
 */
public final class VectorDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "vector";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("vector")
        .damage(8.0)
        .damageDrop(17, 25, 5.5)
        .npcDamage(6.0, 4.0)
        .spread(0.05)
        .zoom(0.75, 0.35)
        .fire(FireMode.AUTO, 2)
        .magazine(25)
        .ammoType(AmmoIds.SMG_MAGAZINE)
        .looseAmmoType(AmmoIds.NINE_MM)
        .emptyContainerType(AmmoIds.SMG_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(40)
        .projectile(1.5, 30)
        .penetration(0.15)
        .animation(2, 5)
        .ai(18, 35, 3, 2)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.HV)
        .damageType(DamageType.PROJECTILE)
        .camos(2)
        .row(
            Row.assembler(
                "GR-32",
                NAME,
                1,
                Tier.HV,
                30,
                3,
                Input.mod(PartItems.BARREL_HARDENED, 1),
                Input.mod(ComponentItems.RECEIVER_HARDENED, 1),
                Input.mod(ComponentItems.STOCK_PLASTIC, 1),
                Input.gt("lens", "Glass", 1),
                Input.gt("plate", "StainlessSteel", 2),
                Input.gt("screw", "StainlessSteel", 4),
                Input.gt("springSmall", "StainlessSteel", 2)))
        .scenarios(registry -> MvGunScenarios.register(registry, NAME))
        .build();

    private VectorDef() {}
}
