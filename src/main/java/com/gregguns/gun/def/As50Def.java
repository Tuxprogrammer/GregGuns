package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.HeavyGunScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The AS50 Sniper Rifle: a scoped, semi-auto anti-materiel rifle.
 *
 * <p>
 * Spec: sheets-rifles-shotguns.md, "AS50 Sniper Rifle", section 3: damage 32 falling to 24 between 30 and 40 blocks,
 * lifetime 50 ticks at 2.25 blocks per tick, spread 0.0625, armor penetration 25%, semi-auto every 10 ticks, 10 rounds
 * from one AS50 Magazine (a forced reload refunds one High Caliber Round per 5 rounds), 80 tick reload, scope FOV
 * x0.35 and spread x0.125 while zoomed, AI range 36 / attack time 30 / no burst, recoil 10 ticks and muzzle flash 5
 * ticks. One skin.
 *
 * <p>
 * Recipe (recipes.md, HV table, GR-40): 2 Hardened Steel Barrels, a Hardened Receiver, a Plastic Gun Stock, a diamond
 * lens (the scope), 3 stainless steel plates, 6 screws and 2 small springs; Assembler, HV, 35 s, circuit 5. The gate is
 * the stainless steel plate.
 */
public final class As50Def {

    /** The registry name and sound prefix. */
    public static final String NAME = "as50";

    /** The stats. */
    // Deviation: Techguns' AI attack time has a variance of 10 ticks; GunStats has no variance field, so the NPC uses
    // the plain 30.
    public static final GunStats STATS = GunStats.builder("as50")
        .damage(32.0)
        .damageDrop(30, 40, 24.0)
        .spread(0.0625)
        .zoom(0.35, 0.125)
        .fire(FireMode.SEMI, 10)
        .magazine(10)
        .ammoType(AmmoIds.AS50_MAGAZINE)
        .looseAmmoType(AmmoIds.HIGH_CALIBER_ROUND)
        .emptyContainerType(AmmoIds.AS50_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(80)
        .projectile(2.25, 50)
        .penetration(0.25)
        .animation(10, 5)
        .ai(36, 30, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.HV)
        .damageType(DamageType.PROJECTILE)
        .row(
            Row.assembler(
                "GR-40",
                NAME,
                1,
                Tier.HV,
                35,
                5,
                Input.mod(PartItems.BARREL_HARDENED, 2),
                Input.mod(ComponentItems.RECEIVER_HARDENED, 1),
                Input.mod(ComponentItems.STOCK_PLASTIC, 1),
                Input.gt("lens", "Diamond", 1),
                Input.gt("plate", "StainlessSteel", 3),
                Input.gt("screw", "StainlessSteel", 6),
                Input.gt("springSmall", "StainlessSteel", 2)))
        .scenarios(registry -> HeavyGunScenarios.register(registry, NAME))
        .build();

    private As50Def() {}
}
