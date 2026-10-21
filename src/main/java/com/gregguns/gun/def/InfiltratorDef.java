package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.MvGunScenarios;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Infiltrator, a silenced M4 (Techguns {@code m4_uq}), made by upgrading an M4.
 *
 * <p>
 * Spec: sheets-rifles-shotguns.md, "The Infiltrator", section 3: damage 8 falling to 6 between 25 and 35 blocks,
 * lifetime 35 ticks at 1.5 blocks per tick, spread 0.01, armor penetration 0.15, full-auto every 3 ticks, 30 rounds
 * from one Assault Rifle Magazine, 45 tick reload, a toggle scope with FOV x0.5 and spread x0.5 while zoomed, AI range
 * 24 / attack time 30 / burst 3 / delay 3. One skin. It plays the quieter {@code silencedm4} shot sounds
 * ({@code guns.infiltrator.fire}) and its tooltip says it is silenced; Techguns reads the flag nowhere else.
 *
 * <p>
 * Deviation (design record 0001, GGS-02-40): the upgrade is an Assembler row (GR-33, HV, 30 s, circuit 4) that consumes
 * one M4 matched at any damage and ignoring NBT, plus 2 stainless steel plates, 2 rubber plates, a long stainless steel
 * rod and 2 glass lenses (the plates and rod are the suppressor). GT recipes cannot copy NBT, so the loaded rounds and
 * the camo of the M4 are lost and the Infiltrator comes out full with the default skin.
 */
public final class InfiltratorDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "infiltrator";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("infiltrator")
        .damage(11.0)
        .damageDrop(25, 35, 8.5)
        .npcDamage(8.0, 6.0)
        .spread(0.01)
        .zoom(0.5, 0.5)
        .fire(FireMode.AUTO, 3)
        .magazine(30)
        .ammoType(AmmoIds.ASSAULT_RIFLE_MAGAZINE)
        .looseAmmoType(AmmoIds.RIFLE)
        .emptyContainerType(AmmoIds.ASSAULT_RIFLE_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(45)
        .projectile(1.5, 35)
        .penetration(0.15)
        .silenced()
        .animation(5, 5)
        .ai(24, 30, 3, 3)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.HV)
        .damageType(DamageType.PROJECTILE)
        .camos(1)
        .row(
            Row.upgrade(
                "GR-33",
                NAME,
                M4Def.NAME,
                Tier.HV,
                30,
                4,
                Input.gt("plate", "StainlessSteel", 2),
                Input.gt("plate", "Rubber", 2),
                Input.gt("stickLong", "StainlessSteel", 1),
                Input.gt("lens", "Glass", 2)))
        .scenarios(registry -> {
            MvGunScenarios.register(registry, NAME);
            MvGunScenarios.registerUpgrade(registry, NAME);
        })
        .build();

    private InfiltratorDef() {}
}
