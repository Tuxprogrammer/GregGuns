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

    /** Reaction Chamber, EV single block. */
    public static final int REACTION_CHAMBER_EV = 20106;
    /** Reaction Chamber, IV single block. */
    public static final int REACTION_CHAMBER_IV = 20107;
    /** Retort, the Reaction Chamber multiblock. */
    public static final int RETORT = 20108;
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

    private GregGunsMTEIDs() {}

    /**
     * @return every ID defined above, in ascending order, for the uniqueness and range checks
     */
    public static int[] all() {
        return new int[] { AMMO_PRESS_STEAM, AMMO_PRESS_LV, AMMO_PRESS_MV, AMMO_PRESS_HV, AMMO_PRESS_EV, AMMO_WORKS,
            REACTION_CHAMBER_EV, REACTION_CHAMBER_IV, RETORT, REPAIR_BENCH_LV, REPAIR_BENCH_MV, REPAIR_BENCH_HV,
            REPAIR_BENCH_EV, REPAIR_BENCH_IV, ARMORY_GANTRY, CAMO_BENCH, TURRET_MV, TURRET_HV, TURRET_EV, TURRET_IV,
            RAMPART, GUN_MOUNT_HATCH_MV, GUN_MOUNT_HATCH_HV, GUN_MOUNT_HATCH_EV, GUN_MOUNT_HATCH_IV };
    }
}
