package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.CombatShotgunScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Combat Shotgun, a pump shotgun.
 *
 * <p>
 * Spec: sheets-rifles-shotguns.md, "Combat Shotgun", section 3: 4 damage per pellet falling to 1.5 between 2 and 5
 * blocks, lifetime 15 ticks at 1.5 blocks per tick, spread 0.01, 7 extra pellets at spread 0.15 (8 projectiles in
 * all, one round per shot), 15% armor penetration, full-auto every 14 ticks, 8 rounds, 50 tick reload, AI range 12 /
 * attack time 30 / no burst, recoil 12 ticks and muzzle flash 5 ticks. It loads shell by shell: up to 8 shells per
 * reload, one round each, and the rounds already in the gun stay (so a forced reload tops up). The pump works after
 * every shot, which plays the rechamber sound.
 *
 * <p>
 * Recipe (recipes.md, MV table): Hardened Steel Barrel, Steel Receiver, Plastic Gun Stock, 2 steel sticks (the pump
 * tube), 2 aluminium plates, 4 aluminium screws and 2 small aluminium springs; Assembler, MV, 25 s, circuit 3. The gate
 * is the aluminium plate.
 */
public final class CombatShotgunDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "combat_shotgun";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("combat_shotgun")
        .damage(4.0)
        .damageDrop(2, 5, 1.5)
        .spread(0.01)
        .pellets(7, 0.15, false)
        .fire(FireMode.AUTO, 14)
        .magazine(8)
        .ammoType(AmmoIds.SHOTGUN_SHELL)
        .reloadMode(ReloadMode.INCREMENTAL, 8)
        .reloadTicks(50)
        .projectile(1.5, 15)
        .penetration(0.15)
        .animation(12, 5)
        .ai(12, 30, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.MV)
        .damageType(DamageType.PROJECTILE)
        .rechambers()
        .row(
            Row.assembler(
                "GR-14",
                NAME,
                1,
                Tier.MV,
                25,
                3,
                Input.mod(PartItems.BARREL_HARDENED, 1),
                Input.mod(ComponentItems.RECEIVER_STEEL, 1),
                Input.mod(ComponentItems.STOCK_PLASTIC, 1),
                Input.gt("stick", "Steel", 2),
                Input.gt("plate", "Aluminium", 2),
                Input.gt("screw", "Aluminium", 4),
                Input.gt("springSmall", "Aluminium", 2)))
        .scenarios(registry -> CombatShotgunScenarios.registerAll(registry))
        .build();

    private CombatShotgunDef() {}
}
