package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.logic.ToolStats;
import com.gregguns.gun.server.ChainsawScenarios;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Chainsaw: HV, a tool gun on a Fuel Tank of 300 rounds. Left click is melee 12 (2 empty) and mining as an axe of
 * level 3 at speed 14, one round per hit or block. Right click cuts: an invisible projectile of 10 damage with 25
 * percent armor penetration and no knockback, 3 blocks of reach, repeating every 3 ticks at one round per cut; the
 * first cut after a pause plays the start sound.
 *
 * <p>
 * Spec: sheets-launchers-tools-grenades.md, "Chainsaw": reload 45 ticks, recoil 5, AI range 3 / attack time 10.
 * Deviation: no gore (the death-effect milestone). Recipe (recipes.md, HV table): 4 Mechanical Parts, an Iron
 * Receiver, GT's steel chainsaw head, an HV electric piston, a polyethylene plate, 2 stainless steel plates and 4
 * stainless steel screws; Assembler, HV, 35 s, circuit 10. Made unloaded: the fuel tank is ammo.
 */
public final class ChainsawDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "chainsaw";

    /** The melee, mining and drain data: one round per hit and per block. */
    public static final ToolStats TOOL = ToolStats.builder()
        .toolClass("axe", 3)
        .digSpeed(14.0)
        .melee(12.0, 2.0, 0.25)
        .costs(1000, 1000)
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
        .projectileKind(ProjectileKind.INVISIBLE)
        .penetration(0.25)
        .knockback(0.0)
        .animation(5, 5)
        .ai(3, 10, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.HV)
        .damageType(DamageType.PHYSICAL)
        .tool(TOOL)
        .startsLoop()
        .row(
            Row.assembler(
                "GR-90",
                NAME,
                1,
                Tier.HV,
                35,
                10,
                Input.mod(PartItems.MECHANICAL_1, 4),
                Input.mod(PartItems.RECEIVER_IRON, 1),
                Input.gt("toolHeadChainsaw", "Steel", 1),
                Input.itemList("Electric_Piston_HV", 1),
                Input.gt("plate", "Polyethylene", 1),
                Input.gt("plate", "StainlessSteel", 2),
                Input.gt("screw", "StainlessSteel", 4)))
        .scenarios(registry -> ChainsawScenarios.registerAll(registry))
        .build();

    private ChainsawDef() {}
}
