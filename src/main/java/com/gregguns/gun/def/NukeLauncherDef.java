package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.projectile.NukeLauncherScenarios;

/**
 * The Nuke Launcher: ZPM, one Tactical Nuke Rocket per reload.
 *
 * <p>
 * Spec: sheets-launchers-tools-grenades.md, "Nuke Launcher", section 3: damage 250 with no drop-off, speed 1.0 for 100
 * ticks, gravity 0.01, blast radius 10, spread 0.05, full-auto every 10 ticks, one round per reload (40 ticks), recoil
 * 10 and muzzle flash 5 ticks, AI range 24 / attack time 80 / no burst. The explosion knocks back ten times as hard as
 * a bullet. It is the Rocket Launcher's shot with a bigger blast: the same {@code RocketProjectile}, whose blast at
 * this size is a nuke (time-sliced block breaks, the long-range FX packet and the {@code nukeexplosion} sound).
 *
 * <p>
 * Recipe: not an Assembler row (the sheet's inputs do not fit the Assembler's six slots); it is built on the Assembly
 * Line, see {@code GunLineTable} (GR-70). The gun is made unloaded: the nuke rocket is ammo.
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Nuke Launcher);
// docs/gregguns-spec/02-guns/recipes.md (LuV and ZPM table)
public final class NukeLauncherDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "nuke_launcher";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("nuke_launcher")
        .damage(250.0)
        .spread(0.05)
        .fire(FireMode.AUTO, 10)
        .magazine(1)
        .ammoType(AmmoIds.ROCKET_NUKE)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(40)
        .projectile(1.0, 100)
        .projectileKind(ProjectileKind.ROCKET)
        .blast(10.0, 0.01)
        .knockback(10.0)
        .penetration(0)
        .animation(10, 5)
        .ai(24, 80, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.ZPM)
        .damageType(DamageType.EXPLOSION)
        .scenarios(registry -> NukeLauncherScenarios.registerAll(registry))
        .build();

    private NukeLauncherDef() {}
}
