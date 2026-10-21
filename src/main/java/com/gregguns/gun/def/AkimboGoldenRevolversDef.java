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
 * The Akimbo Golden Revolvers: two Golden Revolvers in one item.
 *
 * <p>
 * Spec: sheets-handguns-smgs.md, "Akimbo Golden Revolvers", section 3: damage 16 falling to 6 between 15 and 30
 * blocks, lifetime 25 ticks at 1.5 blocks per tick, spread 0.035, semi-auto every 6 ticks, alternating hands, 12 rounds
 * loaded straight from two 9mm bullet items (no magazine, no empty container), 70 tick reload, AI range 24 / attack
 * time 60 / burst 6 / delay 15, recoil 8 ticks and muzzle flash 5 ticks.
 *
 * <p>
 * Deviation: none in the numbers. The pair does 16 damage against the single gun's 14, which is a Techguns quirk the
 * catalog lists as is (open question 9 of the M2 plan, "keep as listed"); a test pins it so it is a decision, not a
 * typo. Recipe: crafting table only (GGS-02-07), two Golden Revolvers shapeless, and one pair splits back into two; the
 * item has the tier of its inputs (IV). It reuses the Golden Revolver's model twice and the Akimbo Revolvers' reload
 * sound.
 */
public final class AkimboGoldenRevolversDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "akimbo_golden_revolvers";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("akimbo_golden_revolvers")
        .damage(28.0)
        .damageDrop(15, 30, 10.5)
        .npcDamage(16.0, 6.0)
        .spread(0.035)
        .fire(FireMode.SEMI, 6)
        .magazine(12)
        .ammoType(AmmoIds.NINE_MM)
        .reloadMode(ReloadMode.MAGAZINE, 2)
        .reloadTicks(70)
        .projectile(1.5, 25)
        .penetration(0)
        .animation(8, 5)
        .ai(24, 60, 6, 15)
        .akimbo(false)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.IV)
        .damageType(DamageType.PROJECTILE)
        .akimboOf(GoldenRevolverDef.NAME)
        .recipes(() -> AkimboRecipes.register(GoldenRevolverDef.NAME, NAME))
        .scenarios(registry -> AkimboScenarios.register(registry, NAME))
        .build();

    private AkimboGoldenRevolversDef() {}
}
