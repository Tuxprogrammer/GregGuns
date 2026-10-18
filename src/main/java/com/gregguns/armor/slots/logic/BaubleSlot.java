package com.gregguns.armor.slots.logic;

/**
 * The Baubles Expanded slot types GregGuns equipment uses. GregGuns requests no new slot types (design 0009 decision
 * 2): the pack already assigns one {@code head}, one {@code wings} and one {@code quiver} slot. Plain Java, both sides.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (table; GGS-05-02, GGS-05-03); docs/design/0009 decision 2
public enum BaubleSlot {

    /** Night Vision Goggles (and the fallback Oxygen Mask). */
    HEAD("head"),
    /** The back-device slot: one device at a time, so flight gear cannot stack. */
    WINGS("wings"),
    /** The Ammo Pouch. */
    QUIVER("quiver");

    private final String typeName;

    BaubleSlot(String typeName) {
        this.typeName = typeName;
    }

    /**
     * @return the slot type name exactly as {@code BaubleExpandedSlots} spells it (its {@code headType},
     *         {@code wingsType} and {@code quiverType} constants)
     */
    public String typeName() {
        return typeName;
    }

    /** @return a new one-element array with {@link #typeName()}, the shape {@code getBaubleTypes} wants */
    public String[] types() {
        return new String[] { typeName };
    }
}
