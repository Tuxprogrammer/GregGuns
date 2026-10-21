package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.logic.ShotPattern;
import com.gregguns.gun.server.EvBallisticScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The PDW: a 20-round EV submachine gun whose every pull is a 2-round burst.
 *
 * <p>
 * Spec: sheets-rifles-shotguns.md, "PDW", section 3: damage 6 falling to 3 between 16 and 25 blocks, lifetime 25
 * ticks at 1.5 blocks per tick, spread 0.03, armor penetration 25%, full-auto every 2 ticks, one burst line of 1 + 1
 * projectiles per round, 20 rounds from an Advanced Magazine (worth 3 Advanced Bullets), 40 tick reload, no scope, AI
 * range 18 / attack time 30 / burst 4 / delay 2, recoil 5 ticks and muzzle flash 5 ticks with {@code checkRecoil}. A
 * forced reload refunds Advanced Bullets at the gun's own rate (about 6.67 rounds each, floored to 6). Three skins:
 * the base, Silver and Dark Green.
 *
 * <p>
 * Recipe (recipes.md, EV table): Carbon Barrel, Carbon Receiver, Carbon Gunstock, 2 titanium plates, 4 titanium screws
 * and 2 small titanium springs; Assembler, EV, 30 s, circuit 2. The gate is the titanium plate.
 */
public final class PdwDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "pdw";

    /** The stats. */
    // Deviation: the AI attack time variance (10 ticks) is dropped, as for the other guns.
    public static final GunStats STATS = GunStats.builder("pdw")
        .damage(9.0)
        .damageDrop(16, 25, 4.5)
        .npcDamage(6.0, 3.0)
        .spread(0.03)
        .fire(FireMode.AUTO, 2)
        .magazine(20)
        .ammoType(AmmoIds.ADVANCED_MAGAZINE)
        .looseAmmoType(AmmoIds.ADVANCED_BULLET)
        .emptyContainerType(AmmoIds.ADVANCED_MAGAZINE_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(40)
        .projectile(1.5, 25)
        .penetration(0.25)
        .pattern(ShotPattern.burstLine(1))
        .animation(5, 5)
        .ai(18, 30, 4, 2)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.EV)
        .damageType(DamageType.PROJECTILE)
        .camos(3)
        .row(
            Row.assembler(
                "GR-60",
                NAME,
                1,
                Tier.EV,
                30,
                2,
                Input.mod(ComponentItems.BARREL_CARBON, 1),
                Input.mod(ComponentItems.RECEIVER_CARBON, 1),
                Input.mod(ComponentItems.STOCK_CARBON, 1),
                Input.gt("plate", "Titanium", 2),
                Input.gt("screw", "Titanium", 4),
                Input.gt("springSmall", "Titanium", 2)))
        .scenarios(registry -> EvBallisticScenarios.register(registry, NAME))
        .build();

    private PdwDef() {}
}
