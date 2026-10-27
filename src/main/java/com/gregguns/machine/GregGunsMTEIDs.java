package com.gregguns.machine;

/**
 * The fixed GT MetaTileEntity IDs GregGuns uses. Every ID lives in the reserved block {@link #BLOCK_FIRST} to
 * {@link #BLOCK_LAST}; IDs are saved in the world, so they never change and are never configurable.
 *
 * <p>
 * The ICBM module owns the sub-range {@link #ICBM_FIRST} to {@link #ICBM_LAST}; nothing else may take an ID in it.
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

    /** First ID of the sub-range reserved for the ICBM module (launch control, silo, radars, battery, EMP tower). */
    public static final int ICBM_FIRST = 20140;
    /** Last ID of the sub-range reserved for the ICBM module; the end of the GregGuns block. */
    public static final int ICBM_LAST = 20163;

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

    /*
     * RETIRED, RESERVED FOREVER (design record 0012): 20106 Reaction Chamber EV, 20107 Reaction Chamber IV, 20108
     * Retort. These IDs are never reused and never renumbered. The full Retort is kept on branch
     * archive/retort-multiblock and tag archive/retort-v1. See RETIRED below.
     */

    /** Ammunition Works multiblock (HV controller). Built in M4. */
    public static final int AMMO_WORKS = 20105;

    /** Repair Bench, LV. Built in M4. */
    public static final int REPAIR_BENCH_LV = 20109;
    /** Repair Bench, MV. Built in M4. */
    public static final int REPAIR_BENCH_MV = 20110;
    /** Repair Bench, HV. Built in M4. */
    public static final int REPAIR_BENCH_HV = 20111;
    /** Repair Bench, EV. Built in M4. */
    public static final int REPAIR_BENCH_EV = 20112;
    /** Repair Bench, IV. Built in M4. */
    public static final int REPAIR_BENCH_IV = 20113;
    /** Armory Gantry multiblock (HV controller). Built in M4. */
    public static final int ARMORY_GANTRY = 20114;
    /** Camo Bench (LV). Built in M4. */
    public static final int CAMO_BENCH = 20115;

    /** Turret, MV. Built in M6. */
    public static final int TURRET_MV = 20116;
    /** Turret, HV. Built in M6. */
    public static final int TURRET_HV = 20117;
    /** Turret, EV. Built in M6. */
    public static final int TURRET_EV = 20118;
    /** Turret, IV. Built in M6. */
    public static final int TURRET_IV = 20119;
    /** Rampart multiblock (HV controller). Built in M6. */
    public static final int RAMPART = 20120;
    /** Gun Mount Hatch, MV. Built in M6. */
    public static final int GUN_MOUNT_HATCH_MV = 20121;
    /** Gun Mount Hatch, HV. Built in M6. */
    public static final int GUN_MOUNT_HATCH_HV = 20122;
    /** Gun Mount Hatch, EV. Built in M6. */
    public static final int GUN_MOUNT_HATCH_EV = 20123;
    /** Gun Mount Hatch, IV. Built in M6. */
    public static final int GUN_MOUNT_HATCH_IV = 20124;

    /** Launch Control (HV), the block beside a Galacticraft launch pad. Built in ICBM I2. */
    public static final int LAUNCH_CONTROL = 20140;

    /**
     * Missile Silo controller (HV hull; the Energy Hatch tier sets the missile tier it handles), the T3 launcher. Built
     * in ICBM I3.
     */
    public static final int MISSILE_SILO = 20141;

    /** EMP Tower (EV), the pulse that drains machines and rolls missiles in range. Built in ICBM I4. */
    public static final int EMP_TOWER = 20146;

    /**
     * MTE IDs that were registered by a shipped alpha and are retired. Worlds may still hold tile entities saved under
     * them, so no registration may ever take one of these numbers again. Not a constant per ID on purpose: nothing may
     * refer to a retired machine.
     */
    private static final int[] RETIRED = { 20106, 20107, 20108 };

    private GregGunsMTEIDs() {}

    /**
     * @return the retired, permanently reserved IDs (20106 to 20108: the Reaction Chamber EV and IV and the Retort); a
     *         copy, ascending
     */
    public static int[] retired() {
        return RETIRED.clone();
    }

    /**
     * @return every ID defined above, in ascending order, for the uniqueness and range checks
     */
    public static int[] all() {
        return new int[] { AMMO_PRESS_STEAM, AMMO_PRESS_LV, AMMO_PRESS_MV, AMMO_PRESS_HV, AMMO_PRESS_EV, AMMO_WORKS,
            REPAIR_BENCH_LV, REPAIR_BENCH_MV, REPAIR_BENCH_HV, REPAIR_BENCH_EV, REPAIR_BENCH_IV, ARMORY_GANTRY,
            CAMO_BENCH, TURRET_MV, TURRET_HV, TURRET_EV, TURRET_IV, RAMPART, GUN_MOUNT_HATCH_MV, GUN_MOUNT_HATCH_HV,
            GUN_MOUNT_HATCH_EV, GUN_MOUNT_HATCH_IV, LAUNCH_CONTROL, MISSILE_SILO, EMP_TOWER };
    }

    /**
     * @return the IDs of the ICBM sub-range that are built so far, ascending; every ID of {@link #all()} inside
     *         {@link #ICBM_FIRST} to {@link #ICBM_LAST} is listed here
     */
    public static int[] icbm() {
        return new int[] { LAUNCH_CONTROL, MISSILE_SILO, EMP_TOWER };
    }
}
