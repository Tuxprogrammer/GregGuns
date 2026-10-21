package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.item.part.PartItems;
import com.gregguns.projectile.LauncherScenarios;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Rocket Launcher: HV, one rocket per reload.
 *
 * <p>
 * Spec: sheets-launchers-tools-grenades.md, "Rocket Launcher", section 3: explosion damage 50 with no drop-off,
 * speed 1.0 for 100 ticks, gravity 0.01, blast radius 1.75, spread 0.05, full-auto every 10 ticks, one round per
 * reload (40 ticks), recoil 10 and muzzle flash 5 ticks, AI range 24 / attack time 80 / no burst. A rocket's direct hit
 * knocks back three times as hard as a bullet's.
 *
 * <p>
 * Recipe (recipes.md, HV table): two Hardened Steel Barrels, a Steel Receiver, a small stainless steel pipe, 2
 * stainless steel plates and 4 stainless steel screws; Assembler, HV, 30 s, circuit 8. The gate is the stainless steel
 * plate. The gun is made unloaded: the rocket is ammo.
 */
public final class RocketLauncherDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "rocket_launcher";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("rocket_launcher")
        .damage(60.0)
        .npcDamage(50.0)
        .spread(0.05)
        .fire(FireMode.AUTO, 10)
        .magazine(1)
        .ammoType(AmmoIds.ROCKET)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(40)
        .projectile(1.0, 100)
        .projectileKind(ProjectileKind.ROCKET)
        .blast(1.75, 0.01)
        .knockback(3.0)
        .penetration(0)
        .animation(10, 5)
        .ai(24, 80, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.HV)
        .damageType(DamageType.EXPLOSION)
        .row(
            Row.assembler(
                "GR-20",
                NAME,
                1,
                Tier.HV,
                30,
                8,
                Input.mod(PartItems.BARREL_HARDENED, 2),
                Input.mod(ComponentItems.RECEIVER_STEEL, 1),
                Input.gt("pipeSmall", "StainlessSteel", 1),
                Input.gt("plate", "StainlessSteel", 2),
                Input.gt("screw", "StainlessSteel", 4)))
        .scenarios(registry -> LauncherScenarios.registerRocket(registry))
        .build();

    private RocketLauncherDef() {}
}
