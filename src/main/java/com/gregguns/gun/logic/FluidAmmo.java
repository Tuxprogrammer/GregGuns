package com.gregguns.gun.logic;

/**
 * The rules of fluid-tank ammo (the Flamethrower's Fuel Tank), as pure arithmetic. A tank is a 1,000 mB Forge fluid
 * container that GT's Fluid Canner fills whole, so a gun treats a <em>full</em> tank as one magazine of rounds and
 * hands back an empty tank, the same item with no fluid, when the reload swaps it in.
 *
 * <p>
 * Consumption is counted in rounds while the gun fires: a tank is spent whole at reload and the gun then counts down
 * its own rounds, so {@link #mbPerRound} is the figure that ties a round to the fuel in a tank (the spec's "1,000 mB
 * over 100 rounds is 10 mB per shot") for tooltips and balance, not something the shot path subtracts. No Minecraft
 * types; both sides.
 */
// Spec: docs/gregguns-spec/02-guns/fluid-ammo.md (per-shot consumption table, GGS-02-29 to 32)
// Deviation: the spec scales the rounds per tank by the fluid's fuel value (GGS-02-32). Not done in M2: a full tank is
// a
// fixed 100 rounds whatever fuel it holds, and which fuels a tank accepts is fixed on the tank item. Recorded in
// docs/gregguns-spec/02-guns/index.md; the scaling needs the loaded amount to travel through the reload, which the
// shared reload machine has no channel for.
public final class FluidAmmo {

    private FluidAmmo() {}

    /**
     * Whether a tank holds enough fuel to be a magazine. The Canner only fills a tank whole and only empties it whole,
     * so a tank is either full or empty in play; a partly filled one (another mod's pipe, a creative stack) is not
     * ammo, so one cannot be topped up to a full reload.
     *
     * @param amountMb   the fluid in the tank, mB
     * @param capacityMb the tank's capacity, mB
     * @return true if the tank is full and so counts as one magazine
     */
    public static boolean isFullTank(int amountMb, int capacityMb) {
        return capacityMb > 0 && amountMb >= capacityMb;
    }

    /**
     * @param capacityMb    a tank's capacity, mB
     * @param roundsPerTank rounds one full tank loads
     * @return the millilitres of fuel behind one round, as a real number (the capacity over the rounds)
     */
    public static double mbPerRound(int capacityMb, int roundsPerTank) {
        if (roundsPerTank <= 0) {
            throw new IllegalArgumentException("roundsPerTank must be positive: " + roundsPerTank);
        }
        return (double) capacityMb / roundsPerTank;
    }

    /**
     * The millilitres of fuel a gun's remaining rounds stand for.
     *
     * @param loadedRounds  rounds in the gun
     * @param capacityMb    a tank's capacity, mB
     * @param roundsPerTank rounds one full tank loads
     * @return the fuel left in the gun, mB, rounded down
     */
    public static int fuelLeftMb(int loadedRounds, int capacityMb, int roundsPerTank) {
        return (int) Math.floor(Math.max(0, loadedRounds) * mbPerRound(capacityMb, roundsPerTank));
    }
}
