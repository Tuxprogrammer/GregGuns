package com.gregguns.armor.slots.logic;

/**
 * The compact, space-free text that describes what is in one Baubles slot, used by the two-client slot sync scenarios
 * to compare the server's view with a client's. For example {@code ammo_pouch;x1;in=0:ammo_9mm_bullet*7}. Plain Java,
 * both sides, dev scenarios only.
 */
public final class SlotPayload {

    /** The payload of a slot with nothing in it. */
    public static final String EMPTY = "empty";

    private SlotPayload() {}

    /**
     * @param item   the item's short registry name
     * @param size   the stack size
     * @param extras more fields (for example {@code nv=on}), appended in order
     * @return {@code item;xSIZE;extra;...}
     */
    public static String build(String item, int size, String... extras) {
        StringBuilder out = new StringBuilder(item).append(";x")
            .append(size);
        for (String extra : extras) {
            out.append(';')
                .append(extra);
        }
        return out.toString();
    }

    /**
     * @param names  the item name in each inventory slot, null for an empty slot
     * @param counts the stack size in each slot
     * @return {@code in=slot:name*count,...}, or {@code in=-} when every slot is empty
     */
    public static String contents(String[] names, int[] counts) {
        StringBuilder out = new StringBuilder("in=");
        boolean any = false;
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) {
                continue;
            }
            if (any) {
                out.append(',');
            }
            out.append(i)
                .append(':')
                .append(names[i])
                .append('*')
                .append(counts[i]);
            any = true;
        }
        return any ? out.toString() : "in=-";
    }

    /**
     * @param payload a payload built by {@link #build} with a {@link #contents} field
     * @param item    an item's short registry name
     * @return the total of that item over the pouch slots, 0 when absent
     */
    public static int countOf(String payload, String item) {
        int total = 0;
        for (String field : payload.split(";")) {
            if (!field.startsWith("in=") || field.equals("in=-")) {
                continue;
            }
            for (String entry : field.substring(3)
                .split(",")) {
                int colon = entry.indexOf(':');
                int star = entry.lastIndexOf('*');
                if (colon > 0 && star > colon
                    && entry.substring(colon + 1, star)
                        .equals(item)) {
                    total += Integer.parseInt(entry.substring(star + 1));
                }
            }
        }
        return total;
    }

    /**
     * @param payload a payload
     * @param field   a whole field such as {@code nv=on}
     * @return whether the payload has exactly that field
     */
    public static boolean hasField(String payload, String field) {
        for (String f : payload.split(";")) {
            if (f.equals(field)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param payload a payload
     * @return the item name, the part before the first {@code ;}
     */
    public static String item(String payload) {
        int semi = payload.indexOf(';');
        return semi < 0 ? payload : payload.substring(0, semi);
    }
}
