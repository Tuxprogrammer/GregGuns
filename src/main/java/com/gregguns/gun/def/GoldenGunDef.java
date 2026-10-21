package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.GoldenGunScenarios;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Golden Gun: a GregGuns original (no Techguns counterpart). The 9mm Pistol's model in polished gold, one Golden
 * Round per shot, and a hit on a living entity kills it outright (a boss loses half its maximum health): the same
 * decision as the Nuke Launcher's kill zone, shared in {@code DamageSources#annihilateLiving}, still gated by the
 * damage factors.
 *
 * <p>
 * Stats: semi-auto, a one-round chamber, a 60 tick reload, spread 0.01, the pistol's ballistic bullet (1.5 blocks per
 * tick for 30 ticks). The 1000 damage is nominal: the kill rule replaces it. The gun is exempt from the damage curve
 * and
 * tier floor tests ({@code DamageCurveTest}).
 *
 * <p>
 * Recipe (docs/gregguns-spec/02-guns/recipes.md, GR-104): an Assembler upgrade at LuV, 30 s. It consumes one 9mm
 * Pistol (any NBT), 8 gold plates, 2 LuV circuits and a HSS-G plate, the LuV gate. Sounds are the pistol's.
 */
// Spec: docs/gregguns-spec/02-guns/index.md (Golden Gun); docs/design/0016-damage-balance-pass.md
public final class GoldenGunDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "golden_gun";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder(NAME)
        .damage(1000.0)
        .annihilating()
        .spread(0.01)
        .fire(FireMode.SEMI, 4)
        .magazine(1)
        .ammoType(AmmoIds.GOLDEN_ROUND)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(60)
        .projectile(1.5, 30)
        .penetration(0)
        .animation(3, 5)
        .ai(24, 30, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.LUV)
        .damageType(DamageType.PROJECTILE)
        .row(
            Row.upgrade(
                "GR-104",
                NAME,
                PistolDef.NAME,
                Tier.LUV,
                30,
                7,
                Input.gt("plate", "Gold", 8),
                Input.gt("circuit", "Master", 2),
                Input.gt("plate", "HSSG", 1)))
        .scenarios(registry -> GoldenGunScenarios.registerAll(registry))
        .build();

    private GoldenGunDef() {}
}
