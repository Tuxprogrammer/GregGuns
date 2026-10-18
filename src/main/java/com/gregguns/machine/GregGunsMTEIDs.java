package com.gregguns.machine;

/**
 * The fixed GT MetaTileEntity IDs GregGuns uses. Every ID lives in the reserved block {@link #BLOCK_FIRST} to
 * {@link #BLOCK_LAST}; IDs are saved in the world, so they never change and are never configurable.
 *
 * <p>
 * Only the machines built so far have a constant (the Ammo Press family, Steam to EV, and the Ammunition Works,
 * 20105). The roster
 * (docs/gregguns-spec/08-machines-production/machine-roster.md) holds the full plan; add a constant here when a machine
 * is built, using the ID the roster gives it. Pure data, both sides.
 */
// Spec: docs/gregguns-spec/08-machines-production/metatileentity-framework.md (MTE ID reservation, GGS-08-01),
// docs/gregguns-spec/08-machines-production/machine-roster.md (GGS-08-48)
public final class GregGunsMTEIDs {

    /** First ID of the reserved GregGuns block. */
    public static final int BLOCK_FIRST = 20100;
    /** Last ID of the reserved GregGuns block. */
    public static final int BLOCK_LAST = 20163;

    /** Ammo Press, Steam (Bronze). Built in M4. */
    public static final int AMMO_PRESS_STEAM = 20100;
    /** Ammo Press, LV. Built in M1 for the pistol slice. */
    public static final int AMMO_PRESS_LV = 20101;
    /** Ammo Press, MV. Built in M4. */
    public static final int AMMO_PRESS_MV = 20102;
    /** Ammo Press, HV. Built in M4. */
    public static final int AMMO_PRESS_HV = 20103;
    /** Ammo Press, EV. Built in M4. */
    public static final int AMMO_PRESS_EV = 20104;

    /** Ammunition Works multiblock (HV controller). Built in M4. */
    public static final int AMMO_WORKS = 20105;

    private GregGunsMTEIDs() {}

    /**
     * @return every ID defined above, in ascending order, for the uniqueness and range checks
     */
    public static int[] all() {
        return new int[] { AMMO_PRESS_STEAM, AMMO_PRESS_LV, AMMO_PRESS_MV, AMMO_PRESS_HV, AMMO_PRESS_EV, AMMO_WORKS };
    }
}
