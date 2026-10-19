package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.GoldenRevolverScenarios;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Golden Revolver (Techguns {@code goldenrevolver}), an upgraded Revolver.
 *
 * <p>
 * Spec: sheets-handguns-smgs.md, "Golden Revolver", section 3: damage 14 falling to 6 between 12 and 30 blocks,
 * lifetime 25 ticks at 1.5 blocks per tick, spread 0.015, no scope, semi-auto every 8 ticks, 6 rounds loaded straight
 * from one 9mm bullet item (no magazine, no empty container), 45 tick reload, recoil 8 ticks and muzzle flash 5 ticks,
 * AI range 24 / attack time 60 / burst 6 / delay 15. One skin. It shares the Revolver's reload sound and plays its own
 * {@code goldenrevolver} shot sounds.
 *
 * <p>
 * Deviation (design record 0001, GGS-02-40, GUN-15): the upgrade is an Assembler row (GR-51, IV, 30 s, circuit 4)
 * that consumes one Revolver matched at any damage and ignoring NBT, plus 8 gold plates (the gold ingots of the
 * original), 2 tungsten steel plates and 4 tungsten steel screws. GT recipes cannot copy NBT, so the loaded rounds and
 * the camo of the Revolver are lost and the Golden Revolver comes out full. The gate is the tungsten steel plate.
 */
public final class GoldenRevolverDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "golden_revolver";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("golden_revolver")
        .damage(14.0)
        .damageDrop(12, 30, 6.0)
        .spread(0.015)
        .fire(FireMode.SEMI, 8)
        .magazine(6)
        .ammoType(AmmoIds.NINE_MM)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(45)
        .projectile(1.5, 25)
        .penetration(0)
        .animation(8, 5)
        .ai(24, 60, 6, 15)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.IV)
        .damageType(DamageType.PROJECTILE)
        .row(
            Row.upgrade(
                "GR-51",
                NAME,
                RevolverDef.NAME,
                Tier.IV,
                30,
                4,
                Input.gt("plate", "Gold", 8),
                Input.gt("plate", "TungstenSteel", 2),
                Input.gt("screw", "TungstenSteel", 4)))
        .scenarios(registry -> GoldenRevolverScenarios.registerAll(registry))
        .build();

    private GoldenRevolverDef() {}
}
