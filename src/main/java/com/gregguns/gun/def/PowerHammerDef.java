package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.ChargeMode;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.logic.ToolStats;
import com.gregguns.gun.server.PowerHammerScenarios;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Power Hammer: HV, a tool gun on a Compressed Air Tank of 300 rounds. Left click is melee 6 (2 empty) and mining
 * as a pickaxe and shovel of level 2 at speed 12, one round per hit or block. Right click charges for 20 ticks and
 * releases a shockwave that spends 1 to 10 rounds of air ({@code floor(10 x charge)}), hits everything in a 4 x 4 box
 * with 3 blocks of reach for {@code rounds x 2.5} and breaks the blocks in it.
 *
 * <p>
 * Spec: sheets-launchers-tools-grenades.md, "Power Hammer": reload 45 ticks, recoil 12, AI range 3 / attack time 30.
 * Recipe (recipes.md, HV table): 2 Mechanical Parts, an Iron Receiver, an HV electric piston, 2 stainless steel double
 * plates, 2 stainless steel plates and 4 stainless steel screws; Assembler, HV, 35 s, circuit 11. Made unloaded: the
 * air tank is ammo.
 */
public final class PowerHammerDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "power_hammer";

    /** The melee, mining and drain data: one round per hit and per block. */
    public static final ToolStats TOOL = ToolStats.builder()
        .toolClass("pickaxe", 2)
        .toolClass("shovel", 2)
        .digSpeed(12.0)
        .melee(6.0, 2.0, 0.25)
        .costs(1000, 1000)
        .build();

    /** The stats. */
    public static final GunStats STATS = GunStats.builder(NAME)
        .damage(2.5)
        .fire(FireMode.SEMI, 4)
        .magazine(300)
        .ammoType(AmmoIds.AIR_TANK)
        .emptyContainerType(AmmoIds.AIR_TANK)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(45)
        .projectile(1.5, 2)
        .penetration(0)
        .charge(20, ChargeMode.SCALED, 10)
        .animation(12, 5)
        .ai(3, 30, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.HV)
        .damageType(DamageType.PHYSICAL)
        .tool(TOOL)
        .charges()
        .row(
            Row.assembler(
                "GR-91",
                NAME,
                1,
                Tier.HV,
                35,
                11,
                Input.mod(PartItems.MECHANICAL_1, 2),
                Input.mod(PartItems.RECEIVER_IRON, 1),
                Input.itemList("Electric_Piston_HV", 1),
                Input.gt("plateDouble", "StainlessSteel", 2),
                Input.gt("plate", "StainlessSteel", 2),
                Input.gt("screw", "StainlessSteel", 4)))
        .scenarios(registry -> PowerHammerScenarios.registerHammer(registry))
        .build();

    private PowerHammerDef() {}
}
