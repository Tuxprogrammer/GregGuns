package com.gregguns.armor.logic;

import java.util.Locale;

/**
 * A numeric effect worn armor can give. Pieces and full sets map bonuses to amounts (see {@link BonusMap}); the amounts
 * of all worn pieces add up. The armor module applies the first group itself; the rest are read through
 * {@code Armor.bonus(...)} by the system that owns them (guns, oxygen gear, powered armor). Plain Java, both sides.
 */
// Spec: docs/techguns-spec/05-armor/set-bonuses.md section 1 (TGArmorBonus)
public enum ArmorBonus {

    /** Movement speed, as a fraction added to 1 (0.5 is +50%); doubled while sprinting. Applied as an attribute. */
    SPEED,
    /** Added to vertical motion on jump. */
    JUMP,
    /** Fraction of the fall distance removed (0.2 means 80% of the distance counts). */
    FALL_REDUCTION,
    /** Blocks of fall distance that do not count. */
    FREE_FALL_HEIGHT,
    /** Block break speed, as a fraction added to 1. */
    BREAK_SPEED,
    /** Extra break speed fraction while in water or lava. */
    BREAK_SPEED_WATER,
    /** Extra maximum health points (1.0 is half a heart). Applied as an attribute. */
    EXTRA_HEALTH,
    /** Knockback resistance, added. Applied as an attribute. */
    KNOCKBACK_RESISTANCE,
    /** Above 0 raises step height to 1.0 without touching other mods' step height. See {@link StepHeightPolicy}. */
    STEP_ASSIST,
    /** Above 0 grants Night Vision without touching a potion's. See {@link NightVisionPolicy}. */
    NIGHT_VISION,
    /** Spread reduction fraction. Read by the gun code through {@code Armor.bonus}. */
    GUN_ACCURACY,
    /** Above 0 marks oxygen gear; read by the oxygen tanks (slots branch). */
    OXYGEN_GEAR,
    /** Above 0 means fire immunity: cancels fire and lava hits and puts the wearer out (powered armor). */
    COOLING_SYSTEM,
    /**
     * Extra speed fraction while in water or lava, added to {@link #SPEED} (HEV suit). Applied as part of the speed
     * attribute.
     */
    SPEED_WATER;

    /** @return the lower-case id, for example {@code "fall_reduction"} */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the tooltip label key, {@code gregguns.armor.bonus.<id>} */
    public String langKey() {
        return "gregguns.armor.bonus." + id();
    }
}
