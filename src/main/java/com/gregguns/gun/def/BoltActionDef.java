package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.BoltActionScenarios;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.BoltActionRecipes;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Bolt Action Rifle, the first scoped gun.
 *
 * <p>
 * Spec: sheets-rifles-shotguns.md, "Bolt Action Rifle", section 3: damage 16 falling to 10 between 20 and 30 blocks,
 * lifetime 40 ticks at 2.25 blocks per tick (the base 1.5 times 1.5), spread 0.05, armor penetration 0.15, semi-auto
 * every 25 ticks (the rechamber), 6 rounds loaded straight from one rifle bullet item (no magazine, no empty
 * container), 50 tick reload, scope FOV x0.35 and spread x0.125 while zoomed, AI range 36 / attack time 60 / no
 * burst, recoil 15 ticks and muzzle flash 5 ticks. Three skins: the base, camo1 (Olive) and camo2 (Tactical Black).
 *
 * <p>
 * Recipe (recipes.md, LV table): Hardened Steel Barrel, Iron Receiver, Wooden Stock, a glass lens (the scope), 2 steel
 * plates, 4 steel screws and a small steel spring; Assembler, LV, 20 s, circuit 4. The gate is the steel plate.
 */
public final class BoltActionDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "bolt_action_rifle";

    /** The stats. */
    // Deviation: Techguns' AI attack time has a variance of 20 ticks; GunStats has no variance field, so the NPC uses
    // the plain 60.
    public static final GunStats STATS = GunStats.builder("bolt_action_rifle")
        .damage(16.0)
        .damageDrop(20, 30, 10.0)
        .spread(0.05)
        .zoom(0.35, 0.125)
        .fire(FireMode.SEMI, 25)
        .magazine(6)
        .ammoType(AmmoIds.RIFLE)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(50)
        .projectile(2.25, 40)
        .penetration(0.15)
        .animation(15, 5)
        .ai(36, 60, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.LV)
        .damageType(DamageType.PROJECTILE)
        .rechambers()
        .camos(3)
        .row(
            Row.assembler(
                "GR-04",
                NAME,
                1,
                Tier.LV,
                20,
                4,
                Input.mod(PartItems.BARREL_HARDENED, 1),
                Input.mod(PartItems.RECEIVER_IRON, 1),
                Input.mod(PartItems.STOCK_WOODEN, 1),
                Input.gt("lens", "Glass", 1),
                Input.gt("plate", "Steel", 2),
                Input.gt("screw", "Steel", 4),
                Input.gt("springSmall", "Steel", 1)))
        .recycling(() -> BoltActionRecipes.recycling())
        .scenarios(registry -> BoltActionScenarios.registerAll(registry))
        .build();

    private BoltActionDef() {}
}
