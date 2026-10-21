package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.ChargeMode;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.BioGunScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Bio Gun: EV, a fluid-tank charge gun. One full Bio Tank loads 30 rounds. The left button fires one round at
 * level 1 every 6 ticks; the right button charges while held (30 ticks to full) and fires on release at level 1 to 3,
 * spending one round per level (never more than are loaded, GUN-02). Every glob poisons (Poison IV for 100 ticks) and
 * leaves a bio blob on the block it hits when safe mode allows (PFX-28).
 *
 * <p>
 * Spec: sheets-energy-special.md, "Bio Gun", section 3: poison damage 10 falling to 8 between 8 and 15 blocks, lifetime
 * 35 ticks at 1.0 blocks per tick, gravity 0.01, spread 0.015, armor penetration 0.15, no scope, 30 rounds from one
 * Bio Tank in a 45 tick reload, recoil 5 and muzzle flash 5 ticks, AI range 18 / attack time 30 / no burst. One skin.
 * Sounds {@code guns.bio_gun.fire}, {@code .reload} and the glob's {@code .impact}.
 *
 * <p>
 * Deviations (docs/gregguns-spec/02-guns/index.md, batch M5 B3a): the Bio Tank is filled with BartWorks' E. coli
 * culture
 * fluid by the Fluid Canner (a GregGuns Bacterial Vat recipe makes it from Biomass; Biomass itself only behind
 * {@code bioGun.acceptBiomass} or without BartWorks) and a full tank is always 30 rounds (GGS-02-32 scaling deferred,
 * as for the Flamethrower); the gun has no
 * charge sound, because the original has none, so the definition does not call {@code charges()}; blob placement and
 * growth obey safe mode and {@code BlockEvent.PlaceEvent} (PFX-28); the charge fixes GUN-02, GUN-10 and GUN-31.
 *
 * <p>
 * Recipe (recipes.md, EV table): a Pump Mechanism, a Steel Receiver, a Plastic Gun Stock, a small titanium pipe, 2
 * titanium plates and 4 titanium screws; Assembler, EV, 30 s, circuit 7. The gate is the titanium plate. Made empty:
 * the tank is ammo.
 */
public final class BioGunDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "bio_gun";

    /** The stats. */
    public static final GunStats STATS = GunStats.builder("bio_gun")
        .damage(16.0)
        .damageDrop(8, 15, 13.0)
        .npcDamage(10.0, 8.0)
        .spread(0.015)
        .fire(FireMode.AUTO, 6)
        .magazine(30)
        .ammoType(AmmoIds.BIO_TANK)
        .emptyContainerType(AmmoIds.BIO_TANK)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(45)
        .projectile(1.0, 35)
        .projectileKind(ProjectileKind.BIO)
        .blast(0.0, 0.01)
        .penetration(0.15)
        .charge(30, ChargeMode.LEVELS, 0)
        .animation(5, 5)
        .ai(18, 30, 0, 0)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.EV)
        .damageType(DamageType.POISON)
        .row(
            Row.assembler(
                "GR-65",
                NAME,
                1,
                Tier.EV,
                30,
                7,
                Input.mod(ComponentItems.PUMP_MECHANISM, 1),
                Input.mod(ComponentItems.RECEIVER_STEEL, 1),
                Input.mod(ComponentItems.STOCK_PLASTIC, 1),
                Input.gt("pipeSmall", "Titanium", 1),
                Input.gt("plate", "Titanium", 2),
                Input.gt("screw", "Titanium", 4)))
        .scenarios(registry -> BioGunScenarios.registerAll(registry))
        .build();

    private BioGunDef() {}
}
