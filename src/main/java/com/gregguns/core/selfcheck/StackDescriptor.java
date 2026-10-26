package com.gregguns.core.selfcheck;

import com.gregguns.api.Tier;

/**
 * Minecraft-free description of one recipe slot (an input or an output) for the dev self-check.
 *
 * <p>
 * The glue code reads real stacks, ore-dict lists and fluid stacks and fills this in; the checks then run over plain
 * data so unit tests can cover every failure kind with fake descriptors. Both sides.
 */
public final class StackDescriptor {

    /** What kind of thing the slot holds. */
    public enum Kind {
        /** A concrete item stack. */
        ITEM,
        /** An ore-dictionary entry (a list of acceptable stacks). */
        ORE_DICT,
        /** A fluid stack. */
        FLUID,
        /** A deliberately empty slot, which is a {@code null} entry in a shaped grid. */
        EMPTY_SLOT
    }

    private final String label;
    private final Kind kind;
    private final boolean present;
    private final boolean gregGuns;
    private final Tier tier;
    private final boolean upgrade;

    private StackDescriptor(String label, Kind kind, boolean present, boolean gregGuns, Tier tier, boolean upgrade) {
        this.label = label;
        this.kind = kind;
        this.present = present;
        this.gregGuns = gregGuns;
        this.tier = tier;
        this.upgrade = upgrade;
    }

    /**
     * An item slot.
     *
     * @param label    human-readable name for messages (registry name when known)
     * @param present  {@code false} when the stack or its item is {@code null}, or its size is not positive
     * @param gregGuns {@code true} when the item belongs to the {@code gregguns} namespace
     * @param tier     the item's declared tier, or {@code null} when it is not a GregGuns item or declares none
     * @return the descriptor
     */
    public static StackDescriptor item(String label, boolean present, boolean gregGuns, Tier tier) {
        return new StackDescriptor(label, Kind.ITEM, present, gregGuns, tier, false);
    }

    /**
     * An ore-dictionary slot.
     *
     * @param label   human-readable name for messages
     * @param present {@code false} when the entry matches no item
     * @return the descriptor
     */
    public static StackDescriptor oreDict(String label, boolean present) {
        return new StackDescriptor(label, Kind.ORE_DICT, present, false, null, false);
    }

    /**
     * A fluid slot.
     *
     * @param label   human-readable name for messages
     * @param present {@code false} when the fluid stack or its fluid is {@code null}
     * @return the descriptor
     */
    public static StackDescriptor fluid(String label, boolean present) {
        return new StackDescriptor(label, Kind.FLUID, present, false, null, false);
    }

    /**
     * @return a descriptor for a {@code null} entry in a shaped crafting grid
     */
    public static StackDescriptor emptySlot() {
        return new StackDescriptor("empty slot", Kind.EMPTY_SLOT, false, false, null, false);
    }

    /**
     * @return the label used in violation messages
     */
    public String label() {
        return label;
    }

    /**
     * @return the slot kind
     */
    public Kind kind() {
        return kind;
    }

    /**
     * @return whether the slot resolved to something real
     */
    public boolean isPresent() {
        return present;
    }

    /**
     * @return whether this is a GregGuns item
     */
    public boolean isGregGuns() {
        return gregGuns;
    }

    /**
     * @return a copy of this descriptor marked as a tier upgrade: as a recipe input it may be of a higher tier than the
     *         output (a guidance computer better than its body, D24)
     */
    public StackDescriptor asUpgrade() {
        return new StackDescriptor(label, kind, present, gregGuns, tier, true);
    }

    /**
     * @return whether this input is allowed to be of a higher tier than the recipe's output
     */
    public boolean isUpgrade() {
        return upgrade;
    }

    /**
     * @return the item's tier, or {@code null} when unknown or not applicable
     */
    public Tier tier() {
        return tier;
    }
}
