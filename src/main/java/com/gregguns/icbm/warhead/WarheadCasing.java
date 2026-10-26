package com.gregguns.icbm.warhead;

import com.gregguns.icbm.explosive.ExplosiveTier;

/**
 * The warhead casings, one per casing tier. A casing fixes the tier, the fill cap (D23), the size of the placed
 * warhead in blocks and the width of its model, which the collision box follows (D31). I1 ships T1 and T2; the heavier
 * casings arrive with
 * their milestones. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The warhead item)
public enum WarheadCasing {

    /** HV casing, 1x1x1, up to 5 charges. */
    T1("warhead_t1", ExplosiveTier.T1, 5, 1, 0.5),
    /** EV to IV casing, 1x1x1, up to 20 charges. */
    T2("warhead_t2", ExplosiveTier.T2, 20, 1, 0.4);

    private final String blockName;
    private final ExplosiveTier tier;
    private final int fillCap;
    private final int heightBlocks;
    private final double boxWidth;

    WarheadCasing(String blockName, ExplosiveTier tier, int fillCap, int heightBlocks, double boxWidth) {
        this.blockName = blockName;
        this.tier = tier;
        this.fillCap = fillCap;
        this.heightBlocks = heightBlocks;
        this.boxWidth = boxWidth;
    }

    /** @return the bare registry name of the block and item */
    public String blockName() {
        return blockName;
    }

    /** @return the casing tier; a warhead carries an explosive of exactly this tier */
    public ExplosiveTier tier() {
        return tier;
    }

    /** @return the most charges the casing holds (D23) */
    public int fillCap() {
        return fillCap;
    }

    /** @return how many blocks tall the placed warhead is; above 1 it uses filler blocks */
    public int heightBlocks() {
        return heightBlocks;
    }

    /** @return the width of the model, in blocks; the collision and selection column is this plus a margin */
    public double boxWidth() {
        return boxWidth;
    }

    /**
     * @param tier an explosive tier
     * @return the casing for that tier, or {@code null} when no casing ships yet
     */
    public static WarheadCasing forTier(ExplosiveTier tier) {
        for (WarheadCasing c : values()) {
            if (c.tier == tier) {
                return c;
            }
        }
        return null;
    }
}
