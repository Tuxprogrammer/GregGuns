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
 * The Akimbo Mac10s: two Mac10s in one item that fire together.
 *
 * <p>
 * Spec: sheets-handguns-smgs.md, "Akimbo Mac10s", section 3: damage 5 falling to 3 between 15 and 24 blocks, lifetime
 * 25 ticks at 1.5 blocks per tick, spread 0.07, full-auto every 2 ticks, <b>double shot</b> (every pull spends 2 rounds
 * and spawns two projectiles, one from each hand), 64 rounds from two SMG Magazines, 68 tick reload, AI range 18 /
 * attack time 45 / burst 3 / delay 3, recoil 2 ticks and muzzle flash 5 ticks. A reload takes two magazines and
 * returns two empty ones; a forced reload refunds the single Mac10's 16 rounds per bullet (GUN-14's factor is not
 * repeated).
 *
 * <p>
 * Recipe (GR-42): crafting table only (GGS-02-07), two Mac10s shapeless, and one Akimbo Mac10s splits back into two.
 * There is no machine row, so the item has the tier of its input gun (MV), not the HV the recipe sheet lists; see the
 * note on design record 0001.
 */
public final class AkimboMac10sDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "akimbo_mac10s";

    /** The stats. */
    // Deviation: the AI attack time variance (15 ticks) is dropped, as for the other guns.
    public static final GunStats STATS = GunStats.builder("akimbo_mac10s")
        .damage(5.0)
        .damageDrop(15, 24, 3.0)
        .spread(0.07)
        .fire(FireMode.AUTO, 2)
        .magazine(64)
        .ammoType(AmmoIds.SMG_MAGAZINE)
        .looseAmmoType(AmmoIds.NINE_MM)
        .emptyContainerType(AmmoIds.SMG_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 2)
        .reloadTicks(68)
        .projectile(1.5, 25)
        .penetration(0)
        .animation(2, 5)
        .ai(18, 45, 3, 3)
        .akimbo(true)
        .ammoPerShot(2)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.MV)
        .damageType(DamageType.PROJECTILE)
        .akimboOf(Mac10Def.NAME)
        .recipes(() -> AkimboRecipes.register(Mac10Def.NAME, NAME))
        .scenarios(registry -> AkimboScenarios.register(registry, NAME))
        .build();

    private AkimboMac10sDef() {}
}
