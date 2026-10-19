package com.gregguns.utility.charge;

import com.gregguns.api.Tier;

/**
 * The two explosive charges: the Explosive Charge (TNT) and the Advanced Explosive Charge (C4). One block, two metas.
 * Holds the fixed parameter ranges, the hardness rule and the sound events. Pure data; no Minecraft types.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (Parameters),
// docs/gregguns-spec/09-machines-utility/explosive-charges.md (GGS-09-14), recipes.md (GGS-09-31)
public enum ChargeType {

    /** The Explosive Charge: fixed 3-wide tunnel, hardness below the TNT cap, MV. */
    TNT(0, "explosive_charge", Tier.MV, 3, 3, 3, 2, 3, 5, "tnt_charge"),
    /** The Advanced Explosive Charge: 3 to 7 wide, hardness up to the advanced cap, HV. */
    ADVANCED(1, "advanced_explosive_charge", Tier.HV, 3, 5, 7, 2, 10, 10, "c4_charge");

    /** Shortest fuse in ticks, shared by both types. */
    public static final int FUSE_MIN = 60;
    /** Default fuse in ticks. */
    public static final int FUSE_DEFAULT = 100;
    /** Longest fuse in ticks. */
    public static final int FUSE_MAX = 200;
    /** GUI step of the fuse in ticks (one second). */
    public static final int FUSE_STEP = 20;

    private final int meta;
    private final String itemName;
    private final Tier tier;
    private final int radiusMin;
    private final int radiusDefault;
    private final int radiusMax;
    private final int lengthMin;
    private final int lengthDefault;
    private final int lengthMax;
    private final String soundPrefix;

    ChargeType(int meta, String itemName, Tier tier, int radiusMin, int radiusDefault, int radiusMax, int lengthMin,
        int lengthDefault, int lengthMax, String soundPrefix) {
        this.meta = meta;
        this.itemName = itemName;
        this.tier = tier;
        this.radiusMin = radiusMin;
        this.radiusDefault = radiusDefault;
        this.radiusMax = radiusMax;
        this.lengthMin = lengthMin;
        this.lengthDefault = lengthDefault;
        this.lengthMax = lengthMax;
        this.soundPrefix = soundPrefix;
    }

    /** @return the block metadata of this type */
    public int meta() {
        return meta;
    }

    /** @return the bare name used for the lang key */
    public String itemName() {
        return itemName;
    }

    /** @return the tier whose Assembler recipe makes this charge (GGS-09-31) */
    public Tier tier() {
        return tier;
    }

    /** @return smallest cross-section size */
    public int radiusMin() {
        return radiusMin;
    }

    /** @return the cross-section size a new charge has */
    public int radiusDefault() {
        return radiusDefault;
    }

    /** @return largest cross-section size */
    public int radiusMax() {
        return radiusMax;
    }

    /** @return false for the TNT charge, whose radius is fixed and whose radius buttons are hidden (MUTL-33) */
    public boolean radiusAdjustable() {
        return radiusMin != radiusMax;
    }

    /** @return shortest tunnel in blocks */
    public int lengthMin() {
        return lengthMin;
    }

    /** @return the tunnel length a new charge has */
    public int lengthDefault() {
        return lengthDefault;
    }

    /** @return longest tunnel in blocks */
    public int lengthMax() {
        return lengthMax;
    }

    /** @return the sound event played when the charge is placed */
    public String plantSound() {
        return "effects." + soundPrefix + "_plant";
    }

    /** @return the sound event played when the charge is armed */
    public String initSound() {
        return "effects." + soundPrefix + "_init";
    }

    /** @return the sound event played once per second while the fuse burns */
    public String tickSound() {
        return "effects." + soundPrefix + "_tick";
    }

    /**
     * Whether a block of this hardness may be bored. Negative hardness (bedrock and other unbreakables) never is. The
     * TNT charge breaks strictly below its cap and the advanced one up to and including its cap, as in the baseline.
     *
     * @param hardness    the block hardness
     * @param tntCap      the TNT hardness cap ({@code UtilityConfig.chargeMaxHardness})
     * @param advancedCap the advanced hardness cap ({@code UtilityConfig.chargeAdvancedMaxHardness})
     * @return true when the block can be broken
     */
    public boolean canBreak(float hardness, float tntCap, float advancedCap) {
        if (hardness < 0.0F || Float.isNaN(hardness)) {
            return false;
        }
        return this == TNT ? hardness < tntCap : hardness <= advancedCap;
    }

    /**
     * @param meta a block metadata
     * @return the type; any value other than 1 is the TNT charge, so a corrupt meta is harmless
     */
    public static ChargeType fromMeta(int meta) {
        return meta == ADVANCED.meta ? ADVANCED : TNT;
    }
}
