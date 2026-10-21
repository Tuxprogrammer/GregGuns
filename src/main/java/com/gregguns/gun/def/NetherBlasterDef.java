package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.NetherBlasterScenarios;
import com.gregguns.item.ammo.def.NetherChargeAmmo;

/**
 * The Nether Blaster: LuV, a slow burning bolt fed by Nether Charges. One charge reloads the gun to
 * {@link NetherChargeAmmo#SHOTS_PER_CHARGE} shots (the clip is that constant, so the figure lives in one place) and
 * there is no empty item: a spent gun returns nothing.
 *
 * <p>
 * Spec: sheets-energy-special.md, "Nether Blaster", section 3: damage 14 falling to 8 between 15 and 30 blocks, bolt
 * speed 0.85 for 50 ticks with no gravity, no spread, armor penetration 0.15, full-auto every 8 ticks, 10 rounds from
 * one Nether Charge in a 35 tick reload, recoil 5 and muzzle flash 10 ticks, no zoom, AI range 24 / attack time 40 /
 * no burst. Sounds: the Techguns shot and the Tesla Rifle's reload (PLAN-M5 section 2).
 *
 * <p>
 * Deviations: the bolt burns what it hits and may place fire, under the flame's rules (see
 * {@code NetherBlasterProjectile}, GUN-05); a forced reload hands back the loaded rounds as loose rounds, not nothing
 * (GUN-13).
 *
 * <p>
 * Recipe (recipes.md, LuV table): Assembly Line, LuV, 45 s. Four Cybernetic Parts, a Hardened Steel Barrel, a Pump
 * Mechanism, a Carbon Receiver, an LuV Electric Piston, 2 LuV circuits, 4 HSS-G plates and 8 HSS-G screws, 576 mB
 * Soldering Alloy and 250 mB Lubricant. Research: a Blaster Rifle. Made empty: the Nether Charge is ammo.
 */
public final class NetherBlasterDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "nether_blaster";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("nether_blaster")
        .damage(40.0)
        .damageDrop(15, 30, 23.0)
        .npcDamage(14.0, 8.0)
        .spread(0.0)
        .fire(FireMode.AUTO, 8)
        .magazine(NetherChargeAmmo.SHOTS_PER_CHARGE)
        .ammoType(AmmoIds.NETHER_CHARGE)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(35)
        .projectile(0.85, 50)
        .projectileKind(ProjectileKind.NETHER)
        .blast(0.0, 0.0)
        .penetration(0.15)
        .animation(5, 10)
        .ai(24, 40, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.LUV)
        .damageType(DamageType.FIRE)
        .scenarios(registry -> NetherBlasterScenarios.registerAll(registry))
        .build();

    private NetherBlasterDef() {}
}
