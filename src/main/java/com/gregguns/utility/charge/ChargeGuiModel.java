package com.gregguns.utility.charge;

/**
 * What the charge settings screen shows and which of its controls are live, as pure functions and one small state
 * holder. The screen reads a {@link State} that the server fills through sync values (and the client copies), so every
 * label, bar and enabled flag is decided here and unit tested without a world or a GUI. The server never trusts this:
 * {@link TileExplosiveCharge#pressBy} re-checks the owner on every press.
 */
// Spec: docs/gregguns-spec/09-machines-utility/explosive-charges.md (GGS-09-40 to GGS-09-44)
public final class ChargeGuiModel {

    /** Compass names of the six blast directions, indexed by orientation; the lang key suffix of each. */
    private static final String[] DIRECTION_NAMES = { "up", "down", "south", "north", "east", "west" };

    private ChargeGuiModel() {}

    /** The values the screen displays; plain fields so a sync value can write them from the network thread. */
    public static final class State {

        /** True once armed. */
        public volatile boolean armed;
        /** Ticks left on the fuse (the set fuse while not armed). */
        public volatile int fuse = ChargeType.FUSE_DEFAULT;
        /** The fuse the owner set; a disarm restores it. */
        public volatile int fuseSet = ChargeType.FUSE_DEFAULT;
        /** Tunnel depth in blocks. */
        public volatile int length;
        /** Tunnel cross-section size. */
        public volatile int radius;
        /** True when the viewing player is the owner or on the owner's team. */
        public volatile boolean canEdit;

        /**
         * @param params the charge settings to copy
         * @param edit   whether the viewer may change them
         */
        public void load(ChargeParams params, boolean edit) {
            armed = params.armed();
            fuse = params.fuse();
            fuseSet = params.fuseSet();
            length = params.length();
            radius = params.radius();
            canEdit = edit;
        }
    }

    /** Why the controls are or are not usable; the screen turns this into the line under the owner. */
    public enum Access {
        /** The viewer may change the settings and the charge is not armed. */
        EDITABLE,
        /** The viewer may press disarm, but the settings are frozen while armed. */
        FROZEN_ARMED,
        /** The viewer is not the owner or a team member: read only. */
        VIEW_ONLY
    }

    /**
     * @param canEdit whether the viewer is the owner or team
     * @param armed   whether the charge is armed
     * @return the access level
     */
    public static Access access(boolean canEdit, boolean armed) {
        if (!canEdit) {
            return Access.VIEW_ONLY;
        }
        return armed ? Access.FROZEN_ARMED : Access.EDITABLE;
    }

    /**
     * Whether the screen shows a button as pressable. This mirrors {@link ChargeParams#press} plus the owner rule; the
     * server enforces the same rules again.
     *
     * @param s      the displayed state
     * @param type   the charge type
     * @param button a {@code ChargeParams.BTN_*} id
     * @return true when the button is live
     */
    public static boolean enabled(State s, ChargeType type, int button) {
        if (!s.canEdit) {
            return false;
        }
        if (s.armed) {
            return button == ChargeParams.BTN_DISARM;
        }
        switch (button) {
            case ChargeParams.BTN_ARM:
                return true;
            case ChargeParams.BTN_RADIUS_UP:
                return type.radiusAdjustable() && s.radius < type.radiusMax();
            case ChargeParams.BTN_RADIUS_DOWN:
                return type.radiusAdjustable() && s.radius > type.radiusMin();
            case ChargeParams.BTN_LENGTH_UP:
                return s.length < type.lengthMax();
            case ChargeParams.BTN_LENGTH_DOWN:
                return s.length > type.lengthMin();
            case ChargeParams.BTN_FUSE_UP:
                return s.fuse < ChargeType.FUSE_MAX;
            case ChargeParams.BTN_FUSE_DOWN:
                return s.fuse > ChargeType.FUSE_MIN;
            default:
                return false;
        }
    }

    /**
     * @param ticks fuse ticks (20 per second)
     * @return seconds with one decimal, rounded up so the display never reads 0.0 before the blast; "0.0" at zero
     */
    public static String seconds(int ticks) {
        int tenths = (Math.max(0, ticks) + 1) / 2;
        return (tenths / 10) + "." + (tenths % 10);
    }

    /**
     * @param fuse    ticks left
     * @param fuseSet the set fuse
     * @return how full the fuse bar is, 0 to 1: the share of the set fuse still burning; full while not armed
     */
    public static float fuseFraction(int fuse, int fuseSet) {
        if (fuseSet <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (float) fuse / fuseSet));
    }

    /**
     * @param orientation the placed face, 0 to 5 (any int is normalized)
     * @return the lang key suffix of the world direction the tunnel runs: up, down, south, north, east or west
     */
    public static String directionName(int orientation) {
        return DIRECTION_NAMES[ChargeGeometry.normalize(orientation)];
    }

    /**
     * @param value a stepper value
     * @param min   smallest
     * @param max   largest
     * @return how far along the range the value is, 0 to 1; 1 when the range is a single value
     */
    public static float rangeFraction(int value, int min, int max) {
        if (max <= min) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (float) (value - min) / (max - min)));
    }
}
