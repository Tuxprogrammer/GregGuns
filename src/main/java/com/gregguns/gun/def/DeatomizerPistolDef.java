package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.server.EnergyBatchScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The De-Atomizer Pistol: an IV semi-auto energy pistol that fires a blue blast.
 *
 * <p>
 * Spec: sheets-energy-special.md, "De-Atomizer Pistol", section 3: 16 damage falling to 8 between 20 and 30 blocks, 50
 * ticks at 1.5 blocks per tick, spread 0.035, 20 percent armor penetration, semi-auto with a minimum of 8 ticks
 * between shots, no scope, recoil and muzzle flash 5 ticks, AI range 24 / attack time 60.
 *
 * <p>
 * The blast has impact FX only. Techguns commented out its area explosion, so there is none here either
 * (projectiles.md 3.10).
 *
 * <p>
 * Deviation: the 20-round clip is 20 shots of charge at 5,000,000 EU each of the 100,000,000 EU IV baseline; there is
 * no reload (plan Q5). The {@code reload} sound event reuses the Techguns clip sound and is never played.
 *
 * <p>
 * Recipe (recipes.md, IV table): Laser Barrel, Sonic Emitter, Mechanical Parts T3, a Lapotronic Orb (the battery,
 * consumed), an Elite circuit, 2 titanium plates, 2 tungsten steel plates and 4 tungsten steel screws. The gate is the
 * tungsten steel plate.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (IV table, De-Atomizer Pistol); energy-electric-items.md
public final class DeatomizerPistolDef {

    /** The registry name, sound prefix and texture name. */
    public static final String NAME = "deatomizer_pistol";

    /** EU per shot: 100,000,000 EU over the sheet's 20 rounds. */
    public static final long EU_PER_SHOT = 5_000_000L;

    /** The IV baseline capacity, EU (a Lapotronic Orb). */
    public static final long CAPACITY_EU = 100_000_000L;

    /** The stats. */
    public static final GunStats STATS = GunStats.builder(NAME)
        .damage(16.0)
        .damageDrop(20, 30, 8.0)
        .spread(0.035)
        .fire(FireMode.SEMI, 8)
        .energy(EU_PER_SHOT, CAPACITY_EU)
        .projectile(1.5, 50)
        .projectileKind(ProjectileKind.ENERGY_BLAST)
        .penetration(0.2)
        .animation(5, 5)
        .ai(24, 60, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.IV)
        .damageType(DamageType.ENERGY)
        .row(
            Row.assembler(
                "GR-81",
                NAME,
                1,
                Tier.IV,
                40,
                2,
                Input.mod(ComponentItems.BARREL_LASER, 1),
                Input.mod(ComponentItems.SONIC_EMITTER, 1),
                Input.mod(ComponentItems.MECHANICAL_3, 1),
                Input.itemList("Energy_LapotronicOrb", 1),
                Input.gt("circuit", "Elite", 1),
                Input.gt("plate", "Titanium", 2),
                Input.gt("plate", "TungstenSteel", 2),
                Input.gt("screw", "TungstenSteel", 4))
                .molten("SolderingAlloy", 288))
        .scenarios(registry -> EnergyBatchScenarios.registerDeatomizer(registry, NAME))
        .build();

    private DeatomizerPistolDef() {}
}
