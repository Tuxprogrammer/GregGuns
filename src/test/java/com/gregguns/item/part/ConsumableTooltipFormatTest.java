package com.gregguns.item.part;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ConsumableTooltipFormatTest {

    @Test
    void withoutShiftTheTooltipIsTierSummaryAndHint() {
        assertEquals(
            Arrays.asList("Tier: MV", "Hold right-click", "Press [SHIFT]"),
            ConsumableTooltipFormat
                .lines("Tier: MV", "Hold right-click", Arrays.asList("a", "b"), "Press [SHIFT]", "Added by", false));
    }

    @Test
    void withShiftTheDetailsAndTheCreditReplaceTheHint() {
        assertEquals(
            Arrays.asList("Tier: MV", "Hold right-click", "a", "b", "Added by"),
            ConsumableTooltipFormat
                .lines("Tier: MV", "Hold right-click", Arrays.asList("a", "b"), "Press [SHIFT]", "Added by", true));
    }

    @Test
    void theNumbersReadAsHeartsSecondsAndRomanLevels() {
        assertEquals("1", ConsumableTooltipFormat.hearts(ConsumableSpec.BANDAGES.healHp()));
        assertEquals("3", ConsumableTooltipFormat.hearts(ConsumableSpec.MEDKIT.healHp()));
        assertEquals("1.5", ConsumableTooltipFormat.hearts(3.0F));
        assertEquals("3", ConsumableTooltipFormat.seconds(ConsumableSpec.BANDAGES.useTicks()));
        assertEquals("2.5", ConsumableTooltipFormat.seconds(ConsumableSpec.MEDKIT.useTicks()));
        assertEquals("20", ConsumableTooltipFormat.seconds(ConsumableSpec.MEDKIT.regenTicks()));
        assertEquals("I", ConsumableTooltipFormat.roman(ConsumableSpec.BANDAGES.regenAmplifier() + 1));
        assertEquals("II", ConsumableTooltipFormat.roman(ConsumableSpec.MEDKIT.regenAmplifier() + 1));
        assertEquals("7", ConsumableTooltipFormat.roman(7));
        assertEquals("gregguns.consumable.tooltip.heal_one", ConsumableTooltipFormat.healKey(2.0F));
        assertEquals("gregguns.consumable.tooltip.heal_many", ConsumableTooltipFormat.healKey(6.0F));
    }
}
