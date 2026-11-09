package com.gregguns.item.part;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure layout and numbers of the Bandages and Medkit tooltips (D41): the tier line, one summary line, then either the
 * details and the AddedBy credit (Shift held) or the Shift hint. Takes already localized strings, so it has no
 * Minecraft types. Both sides.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-61, RC-62)
public final class ConsumableTooltipFormat {

    private static final String[] ROMAN = { "I", "II", "III", "IV", "V" };

    private ConsumableTooltipFormat() {}

    /**
     * @param tierLine the tier line, formatted
     * @param summary  one line of what the item does
     * @param details  the lines shown behind Shift (heal, Regeneration, use time)
     * @param hint     the "press Shift" hint
     * @param addedBy  the AddedBy credit, shown behind Shift
     * @param shift    whether Shift is held
     * @return the tooltip lines
     */
    public static List<String> lines(String tierLine, String summary, List<String> details, String hint,
        String addedBy, boolean shift) {
        List<String> out = new ArrayList<>();
        out.add(tierLine);
        out.add(summary);
        if (shift) {
            out.addAll(details);
            out.add(addedBy);
        } else {
            out.add(hint);
        }
        return out;
    }

    /**
     * @param healthPoints health points (half-hearts)
     * @return the hearts, without a trailing ".0" ("1", "3", "1.5")
     */
    public static String hearts(float healthPoints) {
        return trim(healthPoints / 2.0F);
    }

    /**
     * @param healthPoints health points (half-hearts)
     * @return the lang key of the heal line: one heart reads in the singular
     */
    public static String healKey(float healthPoints) {
        return healthPoints == 2.0F ? "gregguns.consumable.tooltip.heal_one" : "gregguns.consumable.tooltip.heal_many";
    }

    /**
     * @param ticks game ticks
     * @return the seconds, without a trailing ".0" ("3", "2.5", "15")
     */
    public static String seconds(int ticks) {
        return trim(ticks / 20.0F);
    }

    /**
     * @param level an effect level, 1 or more
     * @return its Roman numeral up to V, else the plain number
     */
    public static String roman(int level) {
        return level >= 1 && level <= ROMAN.length ? ROMAN[level - 1] : Integer.toString(level);
    }

    private static String trim(float value) {
        return value == Math.rint(value) ? Integer.toString(Math.round(value))
            : String.format(Locale.ROOT, "%.1f", value);
    }
}
