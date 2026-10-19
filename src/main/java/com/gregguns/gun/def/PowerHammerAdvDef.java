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
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Power Hammer (Advanced): EV, the Power Hammer's behavior with melee 7, pickaxe and shovel level 3 and dig speed
 * 15, on the second skin. An upgrade row: one Power Hammer in any state (loaded, worn, camouflaged), 3 diamonds, an EV
 * electric piston and 2 titanium plates; Assembler, EV, 30 s, circuit 8.
 *
 * <p>
 * Spec: sheets-launchers-tools-grenades.md, "Power Hammer (Advanced)". Deviation (design record 0001, GGS-02-40): the
 * original recipe copied the hammer's NBT; a GT recipe cannot, so the row matches the base hammer ignoring its NBT and
 * the result is a fresh, full hammer. The original's drop-off difference is gone: the shockwave has none.
 */
public final class PowerHammerAdvDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "power_hammer_adv";

    /** The melee, mining and drain data. */
    public static final ToolStats TOOL = ToolStats.builder()
        .toolClass("pickaxe", 3)
        .toolClass("shovel", 3)
        .digSpeed(15.0)
        .melee(7.0, 2.0, 0.25)
        .costs(1000, 1000)
        .build();

    /** The stats: the Power Hammer's. */
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
        .tier(Tier.EV)
        .damageType(DamageType.PHYSICAL)
        .tool(TOOL)
        .charges()
        .row(
            Row.upgrade(
                "GR-92",
                NAME,
                PowerHammerDef.NAME,
                Tier.EV,
                30,
                8,
                Input.gt("gem", "Diamond", 3),
                Input.itemList("Electric_Piston_EV", 1),
                Input.gt("plate", "Titanium", 2)))
        .scenarios(registry -> PowerHammerScenarios.registerAdv(registry))
        .build();

    private PowerHammerAdvDef() {}
}
