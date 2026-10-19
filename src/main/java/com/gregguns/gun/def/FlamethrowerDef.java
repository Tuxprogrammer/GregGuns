package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.FlamethrowerScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Flamethrower: HV, a fluid-tank gun. One full Fuel Tank loads 100 rounds, a reload swaps a full tank in and hands
 * an empty one back, and the tank is filled by GT's Fluid Canner (RC-56, no GregGuns recipe: the tank is a Forge fluid
 * container, which the Canner fills on the fly).
 *
 * <p>
 * Spec: sheets-energy-special.md, "Flamethrower", section 3: FIRE damage 5 falling to 2 between 4 and 16 blocks,
 * lifetime 16 ticks at 1.0 blocks per tick, gravity 0.01, spread 0.05, no scope, full-auto every 2 ticks, 100 rounds
 * from one Fuel Tank in a 45 tick reload, recoil 5 and muzzle flash 5 ticks, AI range 12 / attack time 20 / burst 5 /
 * delay 5. One skin. The first shot after ten idle ticks plays {@code guns.flamethrower.start}, the shots after it
 * {@code guns.flamethrower.fire}.
 *
 * <p>
 * Deviations (docs/gregguns-spec/02-guns/index.md, batch B6): the forced-reload refund is none, because the tank is
 * spent whole and the fuel behind the remaining rounds goes with it (GUN-13, as the spec's "no refund"); a full tank is
 * always 100 rounds whatever fuel it holds (GGS-02-32 scaling is deferred); only player-fired flames ignite entities or
 * blocks and block fire needs {@code explosions.blockDamage} (GUN-05).
 *
 * <p>
 * Recipe (recipes.md, HV table): a Pump Mechanism, an Iron Receiver, a Plastic Gun Stock, 4 Kanthal fine wires (the
 * igniter coil), a small stainless steel pipe, 2 stainless steel plates and 4 stainless steel screws; Assembler, HV,
 * 30 s, circuit 9. The gate is the stainless steel plate. Made empty: the tank is ammo.
 */
public final class FlamethrowerDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "flamethrower";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("flamethrower")
        .damage(5.0)
        .damageDrop(4, 16, 2.0)
        .spread(0.05)
        .fire(FireMode.AUTO, 2)
        .magazine(100)
        .ammoType(AmmoIds.FUEL_TANK)
        .emptyContainerType(AmmoIds.FUEL_TANK)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(45)
        .projectile(1.0, 16)
        .projectileKind(ProjectileKind.FLAME)
        .blast(0.0, 0.01)
        .penetration(0)
        .animation(5, 5)
        .ai(12, 20, 5, 5)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.HV)
        .damageType(DamageType.FIRE)
        .startsLoop()
        .row(
            Row.assembler(
                "GR-50",
                NAME,
                1,
                Tier.HV,
                30,
                9,
                Input.mod(ComponentItems.PUMP_MECHANISM, 1),
                Input.mod(PartItems.RECEIVER_IRON, 1),
                Input.mod(ComponentItems.STOCK_PLASTIC, 1),
                Input.gt("wireFine", "Kanthal", 4),
                Input.gt("pipeSmall", "StainlessSteel", 1),
                Input.gt("plate", "StainlessSteel", 2),
                Input.gt("screw", "StainlessSteel", 4)))
        .scenarios(registry -> FlamethrowerScenarios.registerAll(registry))
        .build();

    private FlamethrowerDef() {}
}
