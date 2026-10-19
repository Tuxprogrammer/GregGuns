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
 * The SCAR, a heavy-hitting HV assault rifle whose clip (20) is smaller than the magazine it loads from (30 rounds in
 * the M4).
 *
 * <p>
 * Spec: sheets-rifles-shotguns.md, "SCAR", section 3: damage 12 falling to 10 between 30 and 40 blocks, lifetime 50
 * ticks at 1.5 blocks per tick, spread 0.015, armor penetration 0.25, full-auto every 4 ticks, a clip of 20 rounds, 45
 * tick reload, a toggle scope with FOV x0.65 and spread x0.5 while zoomed, AI range 24 / attack time 30 / burst 5 /
 * delay 2, recoil 5 ticks and muzzle flash 5 ticks. Two skins: the base and Black.
 *
 * <p>
 * Deviation: the forced-reload refund is whole loose bullets per {@code floor(20 / 3) = 6} rounds where Techguns
 * divides by 6.67; the integer divisor never refunds more than the magazine's three bullets.
 *
 * <p>
 * Recipe (recipes.md, HV table, GR-31): Hardened Steel Barrel, Hardened Receiver, Plastic Gun Stock, a diamond lens, 2
 * stainless steel plates, 4 stainless steel screws and 2 small stainless steel springs; Assembler, HV, 30 s, circuit
 * 2.
 */
public final class ScarDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "scar";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("scar")
        .damage(12.0)
        .damageDrop(30, 40, 10.0)
        .spread(0.015)
        .zoom(0.65, 0.5)
        .fire(FireMode.AUTO, 4)
        .magazine(20)
        .ammoType(AmmoIds.ASSAULT_RIFLE_MAGAZINE)
        .looseAmmoType(AmmoIds.RIFLE)
        .emptyContainerType(AmmoIds.ASSAULT_RIFLE_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(45)
        .projectile(1.5, 50)
        .penetration(0.25)
        .animation(5, 5)
        .ai(24, 30, 5, 2)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.HV)
        .damageType(DamageType.PROJECTILE)
        .camos(2)
        .row(
            Row.assembler(
                "GR-31",
                NAME,
                1,
                Tier.HV,
                30,
                2,
                Input.mod(PartItems.BARREL_HARDENED, 1),
                Input.mod(ComponentItems.RECEIVER_HARDENED, 1),
                Input.mod(ComponentItems.STOCK_PLASTIC, 1),
                Input.gt("lens", "Diamond", 1),
                Input.gt("plate", "StainlessSteel", 2),
                Input.gt("screw", "StainlessSteel", 4),
                Input.gt("springSmall", "StainlessSteel", 2)))
        .scenarios(registry -> MvGunScenarios.register(registry, NAME))
        .build();

    private ScarDef() {}
}
