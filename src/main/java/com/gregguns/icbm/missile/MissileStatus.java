package com.gregguns.icbm.missile;

/**
 * A snapshot of the missile on a pad, for the launch-control GUI. Immutable. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/stack-and-pad.md (The pad API)
public final class MissileStatus {

    /** The pad holds nothing. */
    public static final MissileStatus NONE = new MissileStatus(false, null, 0, 0, 0, null, 0);

    private final boolean armed;
    private final MissileTier tier;
    private final int fuelMb;
    private final int capacityMb;
    private final int guidance;
    private final String explosive;
    private final int fill;

    /**
     * @param armed      whether the stack is armed (a missile entity stands on the pad)
     * @param tier       the body tier; null when nothing is there
     * @param fuelMb     fuel in the tank
     * @param capacityMb the tank size
     * @param guidance   guidance tiers above the body's tier (D24)
     * @param explosive  the warhead's explosive id
     * @param fill       the warhead fill
     */
    public MissileStatus(boolean armed, MissileTier tier, int fuelMb, int capacityMb, int guidance, String explosive,
        int fill) {
        this.armed = armed;
        this.tier = tier;
        this.fuelMb = fuelMb;
        this.capacityMb = capacityMb;
        this.guidance = guidance;
        this.explosive = explosive;
        this.fill = fill;
    }

    /** @return true when a missile entity (an armed stack) stands on the pad */
    public boolean present() {
        return tier != null;
    }

    /** @return same as {@link #present()}: a missile entity exists only once armed */
    public boolean armed() {
        return armed;
    }

    /** @return true when the tank is full */
    public boolean fueled() {
        return tier != null && capacityMb > 0 && fuelMb >= capacityMb;
    }

    /** @return the tier; null when {@link #present()} is false */
    public MissileTier tier() {
        return tier;
    }

    /** @return mB in the tank */
    public int fuelMb() {
        return fuelMb;
    }

    /** @return mB the tank holds */
    public int capacityMb() {
        return capacityMb;
    }

    /** @return the guidance tiers above the body's tier */
    public int guidance() {
        return guidance;
    }

    /** @return the warhead's explosive id; null when nothing is there */
    public String explosive() {
        return explosive;
    }

    /** @return the warhead fill */
    public int fill() {
        return fill;
    }
}
