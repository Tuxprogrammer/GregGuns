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
 * The Grenade Launcher: HV, six 40 mm grenades per reload.
 *
 * <p>
 * Spec: sheets-launchers-tools-grenades.md, "Grenade Launcher", section 3: damage 30, speed 1.0, a fuse of 100 ticks,
 * gravity 0.01, blast radius 1.5, spread 0.05, penetration 0, semi-auto every 5 ticks, 6 rounds loaded shell by shell
 * in a 100 tick reload, recoil 5 and muzzle flash 5 ticks, AI range 24 / attack time 40 / burst 3 / delay 20.
 *
 * <p>
 * Deviation (GUN-04): Techguns hard-coded the fuse, spread and penetration and skipped the damage bonus; here those
 * figures are the gun's stats and are honored like any gun's.
 *
 * <p>
 * Recipe (recipes.md, HV table): Hardened Steel Barrel, Steel Receiver, Plastic Gun Stock, 2 stainless steel plates, 4
 * stainless steel screws and 2 small stainless steel springs; Assembler, HV, 30 s, circuit 7. The gate is the stainless
 * steel plate. Made empty.
 */
public final class GrenadeLauncherDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "grenade_launcher";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("grenade_launcher")
        .damage(36.0)
        .npcDamage(30.0)
        .spread(0.05)
        .fire(FireMode.SEMI, 5)
        .magazine(6)
        .ammoType(AmmoIds.GRENADE_40MM)
        .reloadMode(ReloadMode.INCREMENTAL, 6)
        .reloadTicks(100)
        .projectile(1.0, 100)
        .projectileKind(ProjectileKind.GRENADE)
        .blast(1.5, 0.01)
        .penetration(0)
        .animation(5, 5)
        .ai(24, 40, 3, 20)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.HV)
        .damageType(DamageType.EXPLOSION)
        .row(
            Row.assembler(
                "GR-21",
                NAME,
                1,
                Tier.HV,
                30,
                7,
                Input.mod(PartItems.BARREL_HARDENED, 1),
                Input.mod(ComponentItems.RECEIVER_STEEL, 1),
                Input.mod(ComponentItems.STOCK_PLASTIC, 1),
                Input.gt("plate", "StainlessSteel", 2),
                Input.gt("screw", "StainlessSteel", 4),
                Input.gt("springSmall", "StainlessSteel", 2)))
        .scenarios(registry -> LauncherScenarios.registerGrenade(registry))
        .build();

    private GrenadeLauncherDef() {}
}
