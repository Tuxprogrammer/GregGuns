package com.gregguns.gun;

import net.minecraft.entity.EntityLivingBase;

import com.gregguns.armor.Armor;
import com.gregguns.armor.logic.ArmorBonus;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.gun.logic.ShotContext;

/**
 * Where a shot's armor accuracy bonus comes from: the shooter's worn GregGuns armor, read through
 * {@code Armor.bonus(entity, ArmorBonus.GUN_ACCURACY)}. The bonus is a fraction ({@code 0.1} makes the spread 10
 * percent tighter, see {@code SpreadModel}); pieces and full sets add up and the result is clamped to {@code [0, 1]}.
 *
 * <p>
 * It reads 0 when {@code modules.armor} is off. The armor classes are touched only inside the nested {@link ArmorRead}
 * class, after that check, so a pack with the armor module disabled never loads them from the gun code.
 *
 * <p>
 * Both sides read the same worn equipment, so the server (which rolls the spread) and any client-side mirror of a shot
 * agree. Today only the server rolls spread: the client prediction mirror predicts fire rate, rounds and reloads and
 * spawns no projectiles, so it has no spread to scale. A client effect that does roll its own offsets (a muzzle
 * effect) builds its context with {@link #shotContext}.
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 5 (armor accuracy bonus);
// docs/techguns-spec/05-armor/set-bonuses.md (GunAccuracy)
public final class GunAccuracy {

    private GunAccuracy() {}

    /**
     * @param armorEnabled whether {@code modules.armor} is on
     * @param raw          the summed {@code GUN_ACCURACY} bonus of the shooter's armor
     * @return the bonus a shot uses: 0 when armor is off or the value is not a finite number, else clamped to
     *         {@code [0, 1]}
     */
    public static double effectiveBonus(boolean armorEnabled, double raw) {
        if (!armorEnabled || Double.isNaN(raw) || Double.isInfinite(raw)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, raw));
    }

    /**
     * @param shooter any living entity, may be null
     * @return the armor accuracy bonus of the shooter's worn armor, 0 when the armor module is off
     */
    public static double armorBonus(EntityLivingBase shooter) {
        boolean armorEnabled = shooter != null && ModuleState.isActive(ModuleId.ARMOR);
        return effectiveBonus(armorEnabled, armorEnabled ? ArmorRead.accuracy(shooter) : 0.0);
    }

    /**
     * @param shooter a player
     * @param zoomed  the server's zoom state of the shooter
     * @return the shot context of a player's shot, with the armor accuracy bonus
     */
    public static ShotContext shotContext(EntityLivingBase shooter, boolean zoomed) {
        return ShotContext.player(zoomed, armorBonus(shooter));
    }

    /** Isolates the armor classes so they load only when the armor module is on. */
    private static final class ArmorRead {

        private ArmorRead() {}

        static double accuracy(EntityLivingBase shooter) {
            return Armor.bonus(shooter, ArmorBonus.GUN_ACCURACY);
        }
    }
}
