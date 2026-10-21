package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.server.EnergyBatchScenarios;
import com.gregguns.recipe.AkimboRecipes;

/**
 * The Akimbo De-Atomizer Pistols: two De-Atomizer Pistols in one item that fire alternately.
 *
 * <p>
 * Spec: sheets-energy-special.md, "Akimbo De-Atomizer Pistols", section 3: the single gun's damage and range, spread
 * 0.04, semi-auto every 5 ticks, 40 rounds, alternating hands (one round per pull, the hand by the parity of the rounds
 * left).
 *
 * <p>
 * Recipe (GGS-02-07): crafting table only, two De-Atomizer Pistols shapeless; one akimbo gun splits back into two. No
 * machine row, so the tier is the single gun's (IV). The stored charge is the sum of both guns', and a split halves it
 * (the second gun takes the odd EU), see {@code AkimboRecipes}.
 *
 * <p>
 * Deviation: the charge is 40 shots of 5,000,000 EU, twice the single gun's capacity; no reload (plan Q5).
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (Akimbo combine and split); sheets-energy-special.md (Akimbo De-Atomizer)
public final class AkimboDeatomizerPistolsDef {

    /** The registry name. */
    public static final String NAME = "akimbo_deatomizer_pistols";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder(NAME)
        .damage(32.0)
        .damageDrop(20, 30, 16.0)
        .npcDamage(16.0, 8.0)
        .spread(0.04)
        .fire(FireMode.SEMI, 5)
        .energy(DeatomizerPistolDef.EU_PER_SHOT, 2 * DeatomizerPistolDef.CAPACITY_EU)
        .projectile(1.5, 50)
        .projectileKind(ProjectileKind.ENERGY_BLAST)
        .penetration(0.2)
        .animation(5, 5)
        .ai(24, 60, 0, 0)
        .akimbo(false)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.IV)
        .damageType(DamageType.ENERGY)
        .akimboOf(DeatomizerPistolDef.NAME)
        .recipes(() -> AkimboRecipes.register(DeatomizerPistolDef.NAME, NAME))
        .scenarios(registry -> EnergyBatchScenarios.registerAkimbo(registry, NAME))
        .build();

    private AkimboDeatomizerPistolsDef() {}
}
