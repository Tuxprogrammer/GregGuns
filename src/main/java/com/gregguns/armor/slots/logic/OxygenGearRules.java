package com.gregguns.armor.slots.logic;

/**
 * Which parts of Galacticraft's oxygen setup a GregGuns helmet may supply through {@code IBreathableArmor}: the
 * <em>helmet</em> (mask) and the <em>gear</em> components only, never a tank (GGS-05-25, GGS-05-26). Tanks must be real
 * Galacticraft tank items, because Galacticraft computes "air empty" from its own tank slots. Plain Java, so the rule
 * is
 * unit-testable without Galacticraft on the classpath.
 */
// Spec: docs/gregguns-spec/05-armor/oxygen-gear.md (GGS-05-25, GGS-05-26)
// Source: Galacticraft 3.4.34-GTNH IBreathableArmor.EnumGearType { HELMET, GEAR, TANK1, TANK2 }:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/api/item/IBreathableArmor.java#L9-L33
public final class OxygenGearRules {

    /** Mirrors Galacticraft's {@code EnumGearType} by name. */
    public enum Component {
        /** The mask. */
        HELMET,
        /** The oxygen gear (the regulator). */
        GEAR,
        /** First tank. */
        TANK1,
        /** Second tank. */
        TANK2
    }

    private OxygenGearRules() {}

    /**
     * @param component a component Galacticraft asks about
     * @return whether a GregGuns helmet claims to handle it
     */
    public static boolean handles(Component component) {
        return component == Component.HELMET || component == Component.GEAR;
    }

    /**
     * @param component a component Galacticraft asks about
     * @param powered   whether the helmet currently has charge
     * @return whether the helmet supplies the component right now
     */
    public static boolean canBreathe(Component component, boolean powered) {
        return handles(component) && powered;
    }

    /**
     * @param galacticraftName the name of a Galacticraft {@code EnumGearType} constant
     * @return the matching component
     * @throws IllegalArgumentException for a name Galacticraft does not use
     */
    public static Component fromName(String galacticraftName) {
        return Component.valueOf(galacticraftName);
    }
}
