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
 * The LMG: a 100-round full-auto machine gun with a light scope.
 *
 * <p>
 * Spec: sheets-rifles-shotguns.md, "LMG", section 3: damage 8 falling to 6 between 30 and 40 blocks, lifetime 50 ticks
 * at 1.5 blocks per tick, spread 0.02, armor penetration 15%, full-auto every 2 ticks, 100 rounds from one LMG
 * Magazine (worth 8 Rifle Bullets), 100 tick reload, scope FOV x0.75 and spread x0.75 while zoomed, AI range 24 /
 * attack time 40 / burst 6 / delay 3, recoil 5 ticks and muzzle flash 5 ticks. One skin.
 *
 * <p>
 * Recipe (recipes.md, HV table, GR-41): Hardened Steel Barrel, Hardened Receiver, Plastic Gun Stock, a glass lens, 3
 * stainless steel plates, 6 screws and 3 small springs; Assembler, HV, 35 s, circuit 6. The gate is the stainless steel
 * plate.
 */
public final class LmgDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "lmg";

    /** The stats. */
    // Deviation: the AI attack time variance (13 ticks) is dropped, as for the other guns.
    public static final GunStats STATS = GunStats.builder("lmg")
        .damage(10.0)
        .damageDrop(30, 40, 7.5)
        .npcDamage(8.0, 6.0)
        .spread(0.02)
        .zoom(0.75, 0.75)
        .fire(FireMode.AUTO, 2)
        .magazine(100)
        .ammoType(AmmoIds.LMG_MAGAZINE)
        .looseAmmoType(AmmoIds.RIFLE)
        .emptyContainerType(AmmoIds.LMG_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(100)
        .projectile(1.5, 50)
        .penetration(0.15)
        .animation(5, 5)
        .ai(24, 40, 6, 3)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.HV)
        .damageType(DamageType.PROJECTILE)
        .row(
            Row.assembler(
                "GR-41",
                NAME,
                1,
                Tier.HV,
                35,
                6,
                Input.mod(PartItems.BARREL_HARDENED, 1),
                Input.mod(ComponentItems.RECEIVER_HARDENED, 1),
                Input.mod(ComponentItems.STOCK_PLASTIC, 1),
                Input.gt("lens", "Glass", 1),
                Input.gt("plate", "StainlessSteel", 3),
                Input.gt("screw", "StainlessSteel", 6),
                Input.gt("springSmall", "StainlessSteel", 3)))
        .scenarios(registry -> HeavyGunScenarios.register(registry, NAME))
        .build();

    private LmgDef() {}
}
