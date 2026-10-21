package com.gregguns.gun.def;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageType;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.server.EvBallisticScenarios;
import com.gregguns.item.part.ComponentItems;
import com.gregguns.item.part.PartItems;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;

/**
 * The Minigun: a 200-round EV rotary gun that fires one round every tick (20 per second) once its barrels are at full
 * spin: 15 ticks of holding the trigger from rest, falling back over 30 ticks after release (see
 * {@link com.gregguns.gun.logic.SpinGate}), with a spin-up sound before the first shot.
 *
 * <p>
 * Spec: sheets-rifles-shotguns.md, "Minigun", section 3: damage 5 falling to 3 between 20 and 40 blocks, lifetime 50
 * ticks at 1.5 blocks per tick, spread 0.025, armor penetration 15%, full-auto every tick, 200 rounds from a Minigun
 * Ammo Drum (worth 16 Rifle Bullets), 100 tick reload, no scope, AI range 24 / attack time 40 / burst 10 / delay 1,
 * recoil 5 ticks and muzzle flash 5 ticks with {@code checkRecoil}. A forced reload refunds Rifle Bullets (12 whole
 * bullets per full drum, the sheet's 12.5 floored). One skin.
 *
 * <p>
 * Recipe (recipes.md, EV table): 6 Hardened Steel Barrels, a Hardened Receiver, an EV electric motor, 3 titanium
 * plates and 6 titanium screws; Assembler, EV, 40 s, circuit 4. The gate is the titanium plate.
 */
// Deviation: Techguns fires at once; the spin-up fire delay (and spin-down) is GregGuns' own. The spin-up start sound
// is Techguns' commented-out feature, built here (plan decision Q10 keeps the 20
// rounds per second). The client's checkRecoil flag keeps the recoil window and the rotor turn from restarting every
// tick.
public final class MinigunDef {

    /** The registry name and sound prefix. */
    public static final String NAME = "minigun";

    /** Ticks from rest to full spin with the trigger held; no shot leaves before it (0.75 s). */
    public static final int SPIN_UP_TICKS = 15;
    /** Ticks from full spin to rest with the trigger released (1.5 s). */
    public static final int SPIN_DOWN_TICKS = 30;

    /** The stats. */
    // Deviation: the AI attack time variance (13 ticks) is dropped, as for the other guns.
    public static final GunStats STATS = GunStats.builder("minigun")
        .damage(7.0)
        .damageDrop(20, 40, 4.0)
        .npcDamage(5.0, 3.0)
        .spread(0.025)
        .fire(FireMode.AUTO, 1)
        .magazine(200)
        .ammoType(AmmoIds.MINIGUN_DRUM)
        .looseAmmoType(AmmoIds.RIFLE)
        .emptyContainerType(AmmoIds.MINIGUN_DRUM_EMPTY)
        .reloadMode(ReloadMode.MAGAZINE, 1)
        .reloadTicks(100)
        .projectile(1.5, 50)
        .penetration(0.15)
        .animation(5, 5)
        .spinUp(SPIN_UP_TICKS, SPIN_DOWN_TICKS)
        .ai(24, 40, 10, 1)
        .build();

    /** The definition. */
    public static final GunDefinition DEFINITION = GunDefinition.of(NAME, STATS)
        .tier(Tier.EV)
        .damageType(DamageType.PROJECTILE)
        .startsLoop()
        .row(
            Row.assembler(
                "GR-62",
                NAME,
                1,
                Tier.EV,
                40,
                4,
                Input.mod(PartItems.BARREL_HARDENED, 6),
                Input.mod(ComponentItems.RECEIVER_HARDENED, 1),
                Input.itemList("Electric_Motor_EV", 1),
                Input.gt("plate", "Titanium", 3),
                Input.gt("screw", "Titanium", 6)))
        .scenarios(registry -> EvBallisticScenarios.register(registry, NAME))
        .build();

    private MinigunDef() {}
}
