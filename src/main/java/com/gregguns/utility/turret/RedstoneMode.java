package com.gregguns.utility.turret;

/**
 * When a Turret may work, relative to a redstone signal on its block. Pure logic, both sides. The ids are saved in the
 * world and sent in packets, so they never change.
 */
// Spec: docs/techguns-spec/09-machines-utility/turret.md (redstone mode of the powered base)
public enum RedstoneMode {

    /** The signal is ignored; the Turret always works. */
    IGNORE(0),
    /** The Turret works only while the block is powered. */
    WITH_SIGNAL(1),
    /** The Turret works only while the block is not powered. */
    WITHOUT_SIGNAL(2);

    private final int id;

    RedstoneMode(int id) {
        this.id = id;
    }

    /** @return the saved id, 0 to 2 */
    public int id() {
        return id;
    }

    /**
     * @param id a saved or received id
     * @return the mode; an unknown id means {@link #IGNORE}
     */
    public static RedstoneMode fromId(int id) {
        for (RedstoneMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return IGNORE;
    }

    /** @return the next mode in the button cycle */
    public RedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /**
     * @param powered whether the block has a redstone signal
     * @return true when the Turret may work with this signal
     */
    public boolean allows(boolean powered) {
        switch (this) {
            case WITH_SIGNAL:
                return powered;
            case WITHOUT_SIGNAL:
                return !powered;
            default:
                return true;
        }
    }
}
