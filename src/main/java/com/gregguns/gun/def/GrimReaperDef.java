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
import com.gregguns.projectile.GrimReaperScenarios;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Grim Reaper: LuV, a four-tube rocket launcher. It fires the Rocket Launcher's rocket
 * ({@link ProjectileKind#ROCKET}) one per trigger pull, six ticks apart, and loads up to four loose rockets per reload.
 *
 * <p>
 * Spec: sheets-launchers-tools-grenades.md, "Grim Reaper", section 3: explosion damage 50 with no drop-off, speed 1.0
 * for 100 ticks, gravity 0.01, blast radius 1.75, spread 0.05, full-auto every 6 ticks, 4 rounds, shell-by-shell
 * reload of up to 4 rockets in 40 ticks, recoil 10 and muzzle flash 5 ticks, AI range 24 / attack time 120 / burst 4 /
 * burst delay 30. Knockback is three times a bullet's, as for the Rocket Launcher.
 *
 * <p>
 * Recipe (GR-97): an upgrade row on one Rocket Launcher (any NBT), three Hardened Steel Barrels (the other three
 * tubes), a Carbon Receiver, an LuV electric piston and 4 HSSG plates; Assembler, LuV, 45 s, circuit 9. The gate is
 * the HSSG plate and the LuV piston. Deviation (GGS-02-40): the sheet consumes four Rocket Launchers and adds an
 * Assembly Line research scan; a gun stacks to 1, so a four-launcher input cannot be met by an Assembler slot, and the
 * Assembly Line path for guns is not built. The barrels stand for the three extra tubes, and the separate LuV
 * circuit is dropped (the piston carries the tier). The 250 L of lubricant is kept as a row fluid.
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Grim Reaper);
// docs/gregguns-spec/02-guns/index.md
// (launchers, batch B5)
public final class GrimReaperDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "grim_reaper";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder(NAME)
        .damage(90.0)
        .npcDamage(50.0)
        .spread(0.05)
        .fire(FireMode.AUTO, 6)
        .magazine(4)
        .ammoType(AmmoIds.ROCKET)
        .reloadMode(ReloadMode.INCREMENTAL, 4)
        .reloadTicks(40)
        .projectile(1.0, 100)
        .projectileKind(ProjectileKind.ROCKET)
        .blast(1.75, 0.01)
        .knockback(3.0)
        .penetration(0)
        .animation(10, 5)
        .ai(24, 120, 4, 30)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.LUV)
        .damageType(DamageType.EXPLOSION)
        .row(
            Row.upgrade(
                "GR-97",
                NAME,
                RocketLauncherDef.NAME,
                Tier.LUV,
                45,
                9,
                Input.mod(PartItems.BARREL_HARDENED, 3),
                Input.mod(ComponentItems.RECEIVER_CARBON, 1),
                Input.itemList("Electric_Piston_LuV", 1),
                Input.gt("plate", "HSSG", 4))
                .fluid("Lubricant", 250))
        .scenarios(registry -> GrimReaperScenarios.registerAll(registry))
        .build();

    private GrimReaperDef() {}
}
