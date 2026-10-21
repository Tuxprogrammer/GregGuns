package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.MvGunScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The M4 Assault Rifle, the first scoped gun that is not a bolt action.
 *
 * <p>
 * Spec: sheets-rifles-shotguns.md, "M4 Assault Rifle", section 3: damage 8 falling to 6 between 25 and 35 blocks,
 * lifetime 35 ticks at 1.5 blocks per tick, spread 0.015, armor penetration 0.15, full-auto every 3 ticks, 30 rounds
 * from one Assault Rifle Magazine (worth 3 loose rifle bullets: a forced reload refunds 1 per 10 rounds), 45 tick
 * reload, a toggle scope with FOV x0.75 and spread x0.75 while zoomed, AI range 24 / attack time 30 / burst 3 /
 * delay 3, recoil 5 ticks and muzzle flash 5 ticks. Five skins: the base, Swamp, Desert, Ice and Hazard.
 *
 * <p>
 * Deviation: the M4's scope draws no overlay (only the FOV change and the spread bonus); its future model profile
 * sets no {@code ScopeSpec}, so the shared zoom path runs unchanged for a gun without an overlay.
 *
 * <p>
 * Recipe (recipes.md, MV table): Hardened Steel Barrel, Steel Receiver, Plastic Gun Stock, a glass lens, 2 aluminium
 * plates, 4 aluminium screws and 2 small aluminium springs; Assembler, MV, 25 s, circuit 4. The gate is the aluminium
 * plate.
 */
public final class M4Def {

    /** The registry name and sound prefix. */
    public static final String NAME = "m4";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("m4")
        .damage(9.0)
        .damageDrop(25, 35, 7.0)
        .npcDamage(8.0, 6.0)
        .spread(0.015)
        .zoom(0.75, 0.75)
        .fire(FireMode.AUTO, 3)
        .magazine(30)
        .ammoType(AmmoIds.ASSAULT_RIFLE_MAGAZINE)
        .looseAmmoType(AmmoIds.RIFLE)
        .emptyContainerType(AmmoIds.ASSAULT_RIFLE_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(45)
        .projectile(1.5, 35)
        .penetration(0.15)
        .animation(5, 5)
        .ai(24, 30, 3, 3)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.MV)
        .damageType(DamageType.PROJECTILE)
        .camos(5)
        .row(
            Row.assembler(
                "GR-12",
                NAME,
                1,
                Tier.MV,
                25,
                4,
                Input.mod(PartItems.BARREL_HARDENED, 1),
                Input.mod(ComponentItems.RECEIVER_STEEL, 1),
                Input.mod(ComponentItems.STOCK_PLASTIC, 1),
                Input.gt("lens", "Glass", 1),
                Input.gt("plate", "Aluminium", 2),
                Input.gt("screw", "Aluminium", 4),
                Input.gt("springSmall", "Aluminium", 2)))
        .scenarios(registry -> MvGunScenarios.register(registry, NAME))
        .build();

    private M4Def() {}
}
