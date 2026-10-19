package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.AkimboScenarios;
import com.gregguns.recipe.AkimboRecipes;

/**
 * The Akimbo Revolvers: two Revolvers in one item.
 *
 * <p>
 * Spec: sheets-handguns-smgs.md, "Akimbo Revolvers", section 3: damage 8 falling to 6 between 12 and 20 blocks,
 * lifetime 25 ticks at 1.5 blocks per tick, spread 0.035, semi-auto every 4 ticks, alternating hands, 12 rounds loaded
 * straight from two 9mm bullet items (no magazine, no empty container), 70 tick reload, AI range 18 / attack time 90 /
 * burst 6 / delay 20, recoil 6 ticks and muzzle flash 5 ticks.
 *
 * <p>
 * Recipe: crafting table only (GGS-02-07), two Revolvers shapeless, and one Akimbo Revolvers splits back into two. The
 * item has the tier of its inputs (LV), as the Akimbo Pistols do.
 */
public final class AkimboRevolversDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "akimbo_revolvers";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("akimbo_revolvers")
        .damage(8.0)
        .damageDrop(12, 20, 6.0)
        .spread(0.035)
        .fire(FireMode.SEMI, 4)
        .magazine(12)
        .ammoType(AmmoIds.NINE_MM)
        .reloadMode(ReloadMode.MAGAZINE, 2)
        .reloadTicks(70)
        .projectile(1.5, 25)
        .penetration(0)
        .animation(6, 5)
        .ai(18, 90, 6, 20)
        .akimbo(false)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.LV)
        .damageType(DamageType.PROJECTILE)
        .akimboOf(RevolverDef.NAME)
        .recipes(() -> AkimboRecipes.register(RevolverDef.NAME, NAME))
        .scenarios(registry -> AkimboScenarios.register(registry, NAME))
        .build();

    private AkimboRevolversDef() {}
}
