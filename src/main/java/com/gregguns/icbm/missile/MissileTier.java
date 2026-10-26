package com.gregguns.icbm.missile;

import com.gregguns.icbm.explosive.ExplosiveTier;

/**
 * The missile body tiers that launch from the Galacticraft pad (T1 and T2). A tier fixes the fuel the tank holds (D20),
 * the range band, the flight speed, the base guidance scatter (D24), the model height in blocks (D31) and which fuels
 * the pad and loader accept (D19). No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-bodies.md, fuel.md
public enum MissileTier {

    /** HV body: RP-1 or better, 2,000 mB, up to 1,000 blocks. */
    T1("missile_body_t1", ExplosiveTier.T1, 2000, 20, 1000, 0.8, 12.0, 3, 20.0F, "nitrofuel", "rocketfuelmixb",
        "rocketfuelmixd", "rocketfuelmixc", "rocketfuelmixa"),
    /** EV to IV body: Dense Hydrazine or better, 4,000 mB, up to 3,000 blocks. */
    T2("missile_body_t2", ExplosiveTier.T2, 4000, 20, 3000, 1.2, 8.0, 4, 40.0F, "rocketfuelmixd", "rocketfuelmixc",
        "rocketfuelmixa");

    private final String blockName;
    private final ExplosiveTier warheadTier;
    private final int fuelMb;
    private final int minRange;
    private final int maxRange;
    private final double speed;
    private final double baseScatter;
    private final int heightBlocks;
    private final float health;
    private final String[] fuels;

    MissileTier(String blockName, ExplosiveTier warheadTier, int fuelMb, int minRange, int maxRange, double speed,
        double baseScatter, int heightBlocks, float health, String... fuels) {
        this.blockName = blockName;
        this.warheadTier = warheadTier;
        this.fuelMb = fuelMb;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.speed = speed;
        this.baseScatter = baseScatter;
        this.heightBlocks = heightBlocks;
        this.health = health;
        this.fuels = fuels;
    }

    /** @return 1 or 2 */
    public int number() {
        return ordinal() + 1;
    }

    /** @return the bare registry name of the body block and item */
    public String blockName() {
        return blockName;
    }

    /** @return the heaviest warhead tier this body carries; lighter warheads fit too */
    public ExplosiveTier warheadTier() {
        return warheadTier;
    }

    /** @return the base tank, in mB, before the config factor (D20) */
    public int baseFuelMb() {
        return fuelMb;
    }

    /**
     * @return the shortest launch range, blocks; a closer target is refused so the fuel bonus cannot be farmed (D20)
     */
    public int minRange() {
        return minRange;
    }

    /** @return the longest launch range, blocks, before the config factor */
    public int baseMaxRange() {
        return maxRange;
    }

    /** @return horizontal flight speed, blocks per tick */
    public double speed() {
        return speed;
    }

    /** @return the half-width of the scatter at zero distance and no guidance bonus, blocks (D24) */
    public double baseScatter() {
        return baseScatter;
    }

    /** @return the body's height in blocks, including the base block; the warhead sits on top of this */
    public int heightBlocks() {
        return heightBlocks;
    }

    /** @return hit points of the flying missile; the same at every point of the flight (ICBM-03-01) */
    public float health() {
        return health;
    }

    /** @return the names of the fluids this tier's tank accepts (D19, "this fuel or better") */
    public String[] fuels() {
        return fuels.clone();
    }

    /**
     * @param warhead a warhead tier
     * @return true when the warhead fits this body
     */
    public boolean carries(ExplosiveTier warhead) {
        return warhead.number() <= warheadTier.number();
    }

    /**
     * @param number 1 or 2
     * @return the tier, or null
     */
    public static MissileTier byNumber(int number) {
        for (MissileTier t : values()) {
            if (t.number() == number) {
                return t;
            }
        }
        return null;
    }
}
