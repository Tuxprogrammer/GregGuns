package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.server.AlienBlasterScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Alien Blaster: a LuV full-auto energy pistol. Every 8 ticks it fires a fast bolt (1.5 blocks per tick, 50 ticks)
 * of 16 fire damage with 20 percent armor penetration and no drop-off, which leaves an {@code AlienBlasterTrail} and
 * ends in a tiny explosion that never breaks blocks. A bolt fired by a player sets its target alight and, with block
 * damage on, the face it hits; an NPC's never does.
 *
 * <p>
 * Spec: sheets-energy-special.md, "Alien Blaster", section 3: damage 16 (no drop-off: start and end both 50), 50 ticks
 * at 1.5 blocks per tick, spread 0, penetration 0.2, full-auto every 8 ticks, a 10 round clip, recoil 5 and muzzle
 * flash 10 ticks, AI range 24 / attack time 40, no scope. Sounds {@code guns.alien_blaster.fire} and {@code .reload}.
 *
 * <p>
 * Deviations (docs/gregguns-spec/02-guns/index.md, batch M5 B5): the 10 round clip is 10 shots of charge at
 * 100,000,000 EU each of the 1,000,000,000 EU LuV baseline (a Lapotronic Orb II), and there is no reload (plan Q5), so
 * the {@code reload} sound is registered but never played. Techguns gives this gun no recipe; the sheet makes it LuV.
 * The sheet's Assembly Line chain (research: the De-Atomizer Pistol; solder and lubricant) is not built for guns yet,
 * so the recipe is a plain LuV Assembler row with the same item inputs. The trail shows because the bolt is a
 * registered, tracked entity (PFX-01).
 *
 * <p>
 * Recipe (recipes.md, LuV table): 2 LuV emitters, a LuV sensor, a Carbon Receiver, a Carbon Gunstock, a Lapotronic Orb
 * II (the battery, consumed), 2 Master circuits, 4 HSS-G plates and 8 HSS-G screws. The gate is the HSS-G plate.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (LuV table, Alien Blaster); energy-electric-items.md
public final class AlienBlasterDef {

    /** The registry name, sound prefix and texture name. */
    public static final String NAME = "alien_blaster";

    /** EU per shot: 1,000,000,000 EU over the sheet's 10 rounds. */
    public static final long EU_PER_SHOT = 100_000_000L;

    /** The LuV baseline capacity, EU (a Lapotronic Orb II). */
    public static final long CAPACITY_EU = 1_000_000_000L;

    /** The stats. */
    public static final GunStats STATS = GunStats.builder(NAME)
        .damage(16.0)
        .damageDrop(50, 50, 16.0)
        .spread(0.0)
        .fire(FireMode.AUTO, 8)
        .energy(EU_PER_SHOT, CAPACITY_EU)
        .projectile(1.5, 50)
        .projectileKind(ProjectileKind.ALIEN_BLAST)
        .penetration(0.2)
        .animation(5, 10)
        .ai(24, 40, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.LUV)
        .damageType(DamageType.FIRE)
        .row(
            Row.assembler(
                "GR-95",
                NAME,
                1,
                Tier.LUV,
                60,
                3,
                Input.itemList("Emitter_LuV", 2),
                Input.itemList("Sensor_LuV", 1),
                Input.mod(ComponentItems.RECEIVER_CARBON, 1),
                Input.mod(ComponentItems.STOCK_CARBON, 1),
                Input.itemList("Energy_LapotronicOrb2", 1),
                Input.gt("circuit", "Master", 2),
                Input.gt("plate", "HSSG", 4),
                Input.gt("screw", "HSSG", 8)))
        .scenarios(registry -> AlienBlasterScenarios.registerAll(registry))
        .build();

    private AlienBlasterDef() {}
}
