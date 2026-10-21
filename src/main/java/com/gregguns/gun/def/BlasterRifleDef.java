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
 * The Blaster Rifle: an IV energy rifle with a scope.
 *
 * <p>
 * Spec: sheets-energy-special.md, "Blaster Rifle", section 3: 10 damage falling to 8 between 25 and 35 blocks, 50 ticks
 * at 1.5 blocks per tick, spread 0.025, 20 percent armor penetration, full-auto every 5 ticks, scope x0.5 with spread
 * x0.75 while zoomed, recoil and muzzle flash 5 ticks, AI range 24 / attack time 30 / burst 5 / delay 3.
 *
 * <p>
 * Deviation: the 50-round clip is 50 shots of charge (GGS-02-13, design 0008). The IV baseline is a 100,000,000 EU
 * Lapotronic Orb, so a shot costs 2,000,000 EU. There is no reload, so the reload key does nothing (plan Q5); the
 * {@code guns.blaster_rifle.reload} sound event exists only to satisfy the sound table and reuses the Laser Rifle's
 * clip sound, as Techguns does. The bolt is an {@link ProjectileKind#ENERGY_BLAST}.
 *
 * <p>
 * Recipe (recipes.md, IV table): Laser Barrel, Carbon Receiver, Carbon Gunstock, an IV Emitter, a Lapotronic Orb (the
 * battery, consumed), 2 Elite circuits, 2 tungsten steel plates and 4 screws, plus 288 L of soldering alloy. Deviation:
 * the sheet's
 * glass lens is dropped, because the assembler map has nine input slots, the eight item inputs and the circuit selector
 * fill them, and the solder is a fluid input that never used a slot, so adding the fluid frees none; the emitter stands
 * for the optic. The gate is the tungsten steel plate.
 */
// Spec: docs/gregguns-spec/02-guns/recipes.md (IV table, Blaster Rifle); energy-electric-items.md
public final class BlasterRifleDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "blaster_rifle";

    /** EU per shot: a 100,000,000 EU IV charge over the sheet's 50 rounds. */
    public static final long EU_PER_SHOT = 2_000_000L;

    /** The IV baseline capacity, EU (a Lapotronic Orb). */
    public static final long CAPACITY_EU = 100_000_000L;

    /** The stats. */
    public static final GunStats STATS = GunStats.builder(NAME)
        .damage(24.0)
        .damageDrop(25, 35, 19.0)
        .npcDamage(10.0, 8.0)
        .spread(0.025)
        .zoom(0.5, 0.75)
        .fire(FireMode.AUTO, 5)
        .energy(EU_PER_SHOT, CAPACITY_EU)
        .projectile(1.5, 50)
        .projectileKind(ProjectileKind.ENERGY_BLAST)
        .penetration(0.2)
        .animation(5, 5)
        .ai(24, 30, 5, 3)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.IV)
        .damageType(DamageType.ENERGY)
        .row(
            Row.assembler(
                "GR-80",
                NAME,
                1,
                Tier.IV,
                40,
                // Circuit 5: the pack's Assembler row with circuit 1, a Circuit IV and 32597 would shadow circuit 1.
                // Circuits 2 to 4 are the other IV guns.
                5,
                Input.mod(ComponentItems.BARREL_LASER, 1),
                Input.mod(ComponentItems.RECEIVER_CARBON, 1),
                Input.mod(ComponentItems.STOCK_CARBON, 1),
                Input.itemList("Emitter_IV", 1),
                Input.itemList("Energy_LapotronicOrb", 1),
                Input.gt("circuit", "Elite", 2),
                Input.gt("plate", "TungstenSteel", 2),
                Input.gt("screw", "TungstenSteel", 4))
                .molten("SolderingAlloy", 288))
        .scenarios(registry -> EnergyBatchScenarios.registerBlaster(registry, NAME))
        .build();

    private BlasterRifleDef() {}
}
