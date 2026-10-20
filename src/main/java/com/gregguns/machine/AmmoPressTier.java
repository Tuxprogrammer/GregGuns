package com.gregguns.machine;

import com.gregguns.api.Tier;

/**
 * The five Ammo Press tiers as plain data: MTE ID, D1 tier and what the tier needs to run a recipe. Pure logic, both
 * sides, no Minecraft or GT machine types, so the unit tests can pin it to the roster.
 *
 * <p>
 * Usage: {@code AmmoPressTier.EV.canRun(Tier.HV)} is {@code true}; {@code AmmoPressTier.lowestFor(Tier.MV)} is
 * {@link #MV}.
 */
// Spec: docs/gregguns-spec/08-machines-production/ammo-press.md (Tiers and what each runs),
// machine-roster.md (IDs 20100 to 20104)
public enum AmmoPressTier {

    /** Steam (Bronze) press: runs on steam, and like every GT steam machine every recipe up to LV. */
    STEAM(GregGunsMTEIDs.AMMO_PRESS_STEAM, Tier.STEAM),
    /** LV press. */
    LV(GregGunsMTEIDs.AMMO_PRESS_LV, Tier.LV),
    /** MV press. */
    MV(GregGunsMTEIDs.AMMO_PRESS_MV, Tier.MV),
    /** HV press. */
    HV(GregGunsMTEIDs.AMMO_PRESS_HV, Tier.HV),
    /** EV press. */
    EV(GregGunsMTEIDs.AMMO_PRESS_EV, Tier.EV);

    private final int mteId;
    private final Tier tier;

    AmmoPressTier(int mteId, Tier tier) {
        this.mteId = mteId;
        this.tier = tier;
    }

    /** @return the GT MetaTileEntity ID */
    public int mteId() {
        return mteId;
    }

    /** @return the D1 tier of this press */
    public Tier tier() {
        return tier;
    }

    /** @return {@code true} for the steam-powered press */
    public boolean isSteam() {
        return this == STEAM;
    }

    /**
     * The highest recipe tier this press runs. It is the press's own tier, except for the Steam press: GT's steam
     * machines run recipes up to LV and pay in steam instead of EU.
     *
     * @return the highest recipe tier
     */
    // Source:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachineBronze.java#L222-L225
    // (checkRecipe queries at TierEU.LV) and SteamOverclockDescriber#L25 ("recipe tier is always LV")
    public Tier recipeCeiling() {
        return isSteam() ? Tier.LV : tier;
    }

    /**
     * A machine runs every recipe at or below its own tier ({@code docs/design/0001-tier-map.md}); the Steam press
     * runs up to LV ({@link #recipeCeiling()}).
     *
     * @param recipeTier the D1 tier of a {@code ProductionTable} row
     * @return whether this press can run a recipe of that tier
     */
    public boolean canRun(Tier recipeTier) {
        return recipeTier.ordinal() <= recipeCeiling().ordinal();
    }

    /**
     * @param recipeTier the D1 tier of a recipe
     * @return the lowest press that runs it (the Steam press for ULV and LV rows), or {@code null} above EV (the
     *         Assembly Line makes those items)
     */
    public static AmmoPressTier lowestFor(Tier recipeTier) {
        for (AmmoPressTier press : values()) {
            if (press.canRun(recipeTier)) {
                return press;
            }
        }
        return null;
    }
}
