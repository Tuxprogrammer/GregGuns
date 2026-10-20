package com.gregguns.utility.charge;

/**
 * The settings of one charge and the button rules that change them. Pure logic, so the rules are unit tested without a
 * world. The buttons are numbered as in the baseline: 0 arm, 1/2 radius +/-, 3/4 length +/-, 5/6 fuse +/-. GregGuns
 * adds
 * 7, disarm (the only button that works while armed).
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (Parameters, Arming and fuse),
// docs/techguns-spec/BUGS.md (MUTL-33: the TNT radius buttons are no-ops, so they are hidden and ignored)
public final class ChargeParams {

    /** Button id: arm the charge. */
    public static final int BTN_ARM = 0;
    /** Button id: radius +1. */
    public static final int BTN_RADIUS_UP = 1;
    /** Button id: radius -1. */
    public static final int BTN_RADIUS_DOWN = 2;
    /** Button id: length +1. */
    public static final int BTN_LENGTH_UP = 3;
    /** Button id: length -1. */
    public static final int BTN_LENGTH_DOWN = 4;
    /** Button id: fuse +20 ticks. */
    public static final int BTN_FUSE_UP = 5;
    /** Button id: fuse -20 ticks. */
    public static final int BTN_FUSE_DOWN = 6;
    /** Button id: disarm an armed charge and restore its set fuse (GregGuns addition). */
    public static final int BTN_DISARM = 7;
    /** Number of button ids, for range checks. */
    public static final int BUTTON_COUNT = 8;

    private final ChargeType type;
    private int radius;
    private int length;
    private int fuse;
    private int fuseSet;
    private boolean armed;

    /**
     * @param type the charge type; its defaults are used
     */
    public ChargeParams(ChargeType type) {
        this.type = type;
        this.radius = type.radiusDefault();
        this.length = type.lengthDefault();
        this.fuse = ChargeType.FUSE_DEFAULT;
        this.fuseSet = ChargeType.FUSE_DEFAULT;
    }

    /** @return the type */
    public ChargeType type() {
        return type;
    }

    /** @return cross-section size, 3 to 7 */
    public int radius() {
        return radius;
    }

    /** @return tunnel depth in blocks */
    public int length() {
        return length;
    }

    /** @return ticks left on the fuse (the set fuse while not armed) */
    public int fuse() {
        return fuse;
    }

    /** @return the fuse the owner set, which a disarm restores; equals {@link #fuse()} while not armed */
    public int fuseSet() {
        return fuseSet;
    }

    /** @return true once armed; settings are then frozen */
    public boolean armed() {
        return armed;
    }

    /**
     * Restores saved values, clamping each into its range, so a hand-edited save cannot give a charge an out-of-range
     * size.
     *
     * @param savedRadius saved radius
     * @param savedLength saved length
     * @param savedFuse   saved fuse ticks
     * @param savedArmed  saved armed flag
     */
    public void restore(int savedRadius, int savedLength, int savedFuse, boolean savedArmed) {
        radius = clamp(savedRadius, type.radiusMin(), type.radiusMax());
        length = clamp(savedLength, type.lengthMin(), type.lengthMax());
        fuse = clamp(savedFuse, 0, ChargeType.FUSE_MAX);
        fuseSet = savedArmed ? ChargeType.FUSE_DEFAULT : fuse;
        armed = savedArmed;
    }

    /**
     * Restores saved values including the set fuse of an armed charge.
     *
     * @param savedRadius  saved radius
     * @param savedLength  saved length
     * @param savedFuse    saved remaining fuse ticks
     * @param savedFuseSet saved set fuse ticks; clamped into the fuse range
     * @param savedArmed   saved armed flag
     */
    public void restore(int savedRadius, int savedLength, int savedFuse, int savedFuseSet, boolean savedArmed) {
        restore(savedRadius, savedLength, savedFuse, savedArmed);
        fuseSet = clamp(savedFuseSet, ChargeType.FUSE_MIN, ChargeType.FUSE_MAX);
    }

    /**
     * Applies one GUI button. Once armed only {@link #BTN_DISARM} does anything; unknown ids and the hidden TNT radius
     * buttons change nothing. Disarm does nothing on a charge that is not armed.
     *
     * @param button a button id
     * @return true when something changed (so the tile should sync)
     */
    public boolean press(int button) {
        if (armed) {
            if (button == BTN_DISARM) {
                armed = false;
                fuse = fuseSet;
                return true;
            }
            return false;
        }
        switch (button) {
            case BTN_ARM:
                armed = true;
                return true;
            case BTN_RADIUS_UP:
                return setRadius(radius + 1);
            case BTN_RADIUS_DOWN:
                return setRadius(radius - 1);
            case BTN_LENGTH_UP:
                return setLength(length + 1);
            case BTN_LENGTH_DOWN:
                return setLength(length - 1);
            case BTN_FUSE_UP:
                return setFuse(fuse + ChargeType.FUSE_STEP);
            case BTN_FUSE_DOWN:
                return setFuse(fuse - ChargeType.FUSE_STEP);
            default:
                return false;
        }
    }

    /**
     * Counts one tick of the fuse down. Does nothing unless armed.
     *
     * @return what the tile should do this tick
     */
    public ChargeFuse.Tick tickFuse() {
        if (!armed) {
            return ChargeFuse.Tick.NONE;
        }
        ChargeFuse.Result r = ChargeFuse.advance(fuse);
        fuse = r.remaining;
        return r.tick;
    }

    private boolean setRadius(int value) {
        int v = clamp(value, type.radiusMin(), type.radiusMax());
        boolean changed = v != radius;
        radius = v;
        return changed;
    }

    private boolean setLength(int value) {
        int v = clamp(value, type.lengthMin(), type.lengthMax());
        boolean changed = v != length;
        length = v;
        return changed;
    }

    private boolean setFuse(int value) {
        int v = clamp(value, ChargeType.FUSE_MIN, ChargeType.FUSE_MAX);
        boolean changed = v != fuse;
        fuse = v;
        fuseSet = v;
        return changed;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
