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
 * The Mac10.
 *
 * <p>
 * Spec: sheets-handguns-smgs.md, "Mac10", section 3: damage 5 falling to 3 between 15 and 24 blocks, lifetime 25
 * ticks at 1.5 blocks per tick, spread 0.05, no armor penetration, no scope, full-auto every 2 ticks, 32 rounds from
 * one SMG Magazine (a forced reload refunds 1 bullet per 16 rounds), 40 tick reload, AI range 18 / attack time 35 /
 * burst 3 / delay 2, recoil 2 ticks and muzzle flash 5 ticks. One skin.
 *
 * <p>
 * Recipe (recipes.md, MV table): Hardened Steel Barrel, Steel Receiver, a polyethylene plate, 2 aluminium plates, 4
 * aluminium screws and 2 small aluminium springs; Assembler, MV, 20 s, circuit 2. The gate is the aluminium plate.
 */
public final class Mac10Def {

    /** The registry name and sound prefix. */
    public static final String NAME = "mac10";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("mac10")
        .damage(6.0)
        .damageDrop(15, 24, 3.5)
        .npcDamage(5.0, 3.0)
        .spread(0.05)
        .fire(FireMode.AUTO, 2)
        .magazine(32)
        .ammoType(AmmoIds.SMG_MAGAZINE)
        .looseAmmoType(AmmoIds.NINE_MM)
        .emptyContainerType(AmmoIds.SMG_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(40)
        .projectile(1.5, 25)
        .penetration(0)
        .animation(2, 5)
        .ai(18, 35, 3, 2)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.MV)
        .damageType(DamageType.PROJECTILE)
        .camos(1)
        .row(
            Row.assembler(
                "GR-11",
                NAME,
                1,
                Tier.MV,
                20,
                2,
                Input.mod(PartItems.BARREL_HARDENED, 1),
                Input.mod(ComponentItems.RECEIVER_STEEL, 1),
                Input.gt("plate", "Plastic", 1),
                Input.gt("plate", "Aluminium", 2),
                Input.gt("screw", "Aluminium", 4),
                Input.gt("springSmall", "Aluminium", 2)))
        .scenarios(registry -> MvGunScenarios.register(registry, NAME))
        .build();

    private Mac10Def() {}
}
