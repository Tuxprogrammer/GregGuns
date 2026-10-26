package com.gregguns.machine;

import com.gregguns.core.module.ModuleId;

/**
 * Which module owns each GregGuns MTE id. Pure data: the machines are always registered, and this tells the visibility
 * and gating code which module's flag applies to a machine stack or tile.
 */
// Spec: docs/gregguns-spec/08-machines-production/machine-roster.md
public final class MteModules {

    private MteModules() {}

    /**
     * @param mteId a GT MetaTileEntity id
     * @return the owning module, or {@code null} when the id is not a GregGuns machine
     */
    public static ModuleId moduleOf(int mteId) {
        if (mteId >= GregGunsMTEIDs.AMMO_PRESS_STEAM && mteId <= GregGunsMTEIDs.AMMO_WORKS) {
            return ModuleId.GUNS;
        }
        if (mteId >= GregGunsMTEIDs.REPAIR_BENCH_LV && mteId <= GregGunsMTEIDs.CAMO_BENCH) {
            return ModuleId.MACHINES;
        }
        if (mteId >= GregGunsMTEIDs.TURRET_MV && mteId <= GregGunsMTEIDs.GUN_MOUNT_HATCH_IV) {
            return ModuleId.UTILITY;
        }
        if (mteId >= GregGunsMTEIDs.ICBM_FIRST && mteId <= GregGunsMTEIDs.ICBM_LAST) {
            return ModuleId.ICBM;
        }
        return null;
    }

    /**
     * @param casingMeta a {@code GregGunsCasings} metadata value
     * @return the module whose machines use that casing
     */
    public static ModuleId casingModule(int casingMeta) {
        if (casingMeta >= com.gregguns.block.GregGunsCasings.BASTION_IRON) {
            return ModuleId.UTILITY;
        }
        if (casingMeta >= com.gregguns.block.GregGunsCasings.GANTRY_CASING) {
            return ModuleId.MACHINES;
        }
        return ModuleId.GUNS;
    }
}
