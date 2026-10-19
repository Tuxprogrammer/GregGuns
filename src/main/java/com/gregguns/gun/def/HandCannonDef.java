package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.HandCannonScenarios;
import com.gregguns.recipe.HandCannonRecipes;

/**
 * The Hand Cannon.
 *
 * <p>
 * Spec: sheets-handguns-smgs.md, "Hand Cannon", section 3: damage 8 falling to 5 between 10 and 25 blocks, speed 1.0
 * blocks per tick for 25 ticks (gravity 0.015 comes from the SimpleProjectile), spread 0.035, no scope, full-auto
 * every 12 ticks, one round loaded straight from one stone bullet (no magazine, no empty container), 30 tick reload,
 * AI range 12 / attack time 60 / burst 0 / delay 0, recoil 5 ticks and muzzle flash 5 ticks.
 *
 * <p>
 * Recipe: crafting table only (no machine exists at ULV), see {@link HandCannonRecipes#registerGun()}; no assembly row.
 */
public final class HandCannonDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "handcannon";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("handcannon")
        .damage(8.0)
        .damageDrop(10, 25, 5.0)
        .spread(0.035)
        .fire(FireMode.AUTO, 12)
        .magazine(1)
        .ammoType(AmmoIds.STONE)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(30)
        .projectile(1.0, 25)
        .projectileKind(ProjectileKind.GRAVITY_BULLET)
        .penetration(0)
        .animation(5, 5)
        .ai(12, 60, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.STEAM)
        .damageType(DamageType.PROJECTILE)
        .recipes(() -> HandCannonRecipes.registerGun())
        .scenarios(registry -> HandCannonScenarios.registerAll(registry))
        .build();

    private HandCannonDef() {}
}
