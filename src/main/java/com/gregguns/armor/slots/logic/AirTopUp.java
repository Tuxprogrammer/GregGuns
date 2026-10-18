package com.gregguns.armor.slots.logic;

/**
 * The air rule of the fallback Oxygen Tanks (used only where Galacticraft is absent, where the vanilla air meter is the
 * only breathing system): while the player's air is at or under the threshold and some worn item gives oxygen gear,
 * add air and spend one charge point. Plain Java, both sides.
 */
// Spec: docs/techguns-spec/05-armor/special-equipment.md section 4.1; docs/gregguns-spec/05-armor/oxygen-gear.md
// (table: without Galacticraft the items only restore the vanilla air meter, like Techguns)
public final class AirTopUp {

    /** Air at or under which the tanks act (vanilla's maximum is 300). */
    public static final int THRESHOLD = 280;
    /** Air added per use. */
    public static final int AMOUNT = 20;
    /** Vanilla's full air meter. */
    public static final int FULL = 300;

    private AirTopUp() {}

    /**
     * @param air        the player's current air
     * @param damage     the tanks' stack damage
     * @param maxDamage  the tanks' capacity
     * @param oxygenGear whether a worn item gives oxygen gear
     * @return true when the tanks add air this tick
     */
    public static boolean applies(int air, int damage, int maxDamage, boolean oxygenGear) {
        return oxygenGear && air <= THRESHOLD && damage < maxDamage;
    }

    /**
     * @param air the current air
     * @return the air after one top-up, never above vanilla's full meter
     */
    public static int topUp(int air) {
        return Math.min(FULL, air + AMOUNT);
    }
}
