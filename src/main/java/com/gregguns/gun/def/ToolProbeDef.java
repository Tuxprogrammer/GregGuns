package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.logic.ToolStats;

/**
 * A test-only tool gun for the I11 scenarios and the batch that adds the real ones: a Chainsaw-shaped gun (axe level
 * 3, melee 12 or 2 empty, 300 rounds from a Fuel Tank) whose block cost is half a round, so two blocks cost one round
 * and the remainder is exercised. It is <em>not</em> in {@link GunDefinitions#ALL}: it has no recipe, no sounds and no
 * model, and is registered only in the dev environment, hidden from the creative tab.
 */
public final class ToolProbeDef {

    /** The registry name. */
    public static final String NAME = "probe_tool_gun";

    /** The tool data. */
    public static final ToolStats TOOL = ToolStats.builder()
        .toolClass("axe", 3)
        .digSpeed(14.0)
        .melee(12.0, 2.0, 0.25)
        .costs(1000, 500)
        .build();

    /** The stats. */
    public static final GunStats STATS = GunStats.builder(NAME)
        .damage(10.0)
        .fire(FireMode.AUTO, 3)
        .magazine(300)
        .ammoType(AmmoIds.FUEL_TANK)
        .emptyContainerType(AmmoIds.FUEL_TANK)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(45)
        .projectile(1.5, 2)
        .penetration(0.25)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.HV)
        .damageType(DamageType.PHYSICAL)
        .tool(TOOL)
        .build();

    private ToolProbeDef() {}
}
