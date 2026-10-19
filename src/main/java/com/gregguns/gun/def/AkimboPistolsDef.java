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
 * The Akimbo Pistols: two 9mm Pistols in one item.
 *
 * <p>
 * Spec: sheets-handguns-smgs.md, "Akimbo Pistols", section 3: damage 8 falling to 5 between 18 and 25 blocks, lifetime
 * 30 ticks at 1.5 blocks per tick, spread 0.035, semi-auto every 3 ticks, alternating hands (one round per pull, the
 * hand picked by the parity of the rounds left), 36 rounds from two 9mm Pistol Magazines, 52 tick reload, AI range 24
 * / attack time 30 / burst 3 / delay 10, recoil and muzzle flash 3 ticks. A reload takes two magazines and returns two
 * empty ones; a forced reload refunds the single pistol's 6 rounds per bullet.
 *
 * <p>
 * Recipe: crafting table only (GGS-02-07), two 9mm Pistols shapeless, and one Akimbo Pistols splits back into two.
 * There is no machine row, so the item has the tier of its inputs (LV): see the note on design record 0001.
 */
public final class AkimboPistolsDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "akimbo_pistols";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("akimbo_pistols")
        .damage(8.0)
        .damageDrop(18, 25, 5.0)
        .spread(0.035)
        .fire(FireMode.SEMI, 3)
        .magazine(36)
        .ammoType(AmmoIds.NINE_MM_PISTOL_MAGAZINE)
        .looseAmmoType(AmmoIds.NINE_MM)
        .emptyContainerType(AmmoIds.NINE_MM_PISTOL_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 2)
        .reloadTicks(52)
        .projectile(1.5, 30)
        .penetration(0)
        .animation(3, 3)
        .ai(24, 30, 3, 10)
        .akimbo(false)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.LV)
        .damageType(DamageType.PROJECTILE)
        .akimboOf(PistolDef.NAME)
        .recipes(() -> AkimboRecipes.register(PistolDef.NAME, NAME))
        .scenarios(registry -> AkimboScenarios.register(registry, NAME))
        .build();

    private AkimboPistolsDef() {}
}
