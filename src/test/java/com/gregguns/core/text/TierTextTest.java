package com.gregguns.core.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.EnumChatFormatting;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

import gregtech.api.enums.GTValues;

// Spec: docs/gregguns-spec/06-items/ammo.md (tooltips name tiers in GT colors)
class TierTextTest {

    @Test
    void everyTierUsesGtsColorAndName() {
        for (Tier tier : Tier.values()) {
            int i = tier.voltageIndex();
            assertEquals(GTValues.TIER_COLORS[i] + GTValues.VN[i] + EnumChatFormatting.RESET, TierText.colored(tier));
        }
    }

    @Test
    void knownColorsMatchGtnhStandard() {
        assertTrue(
            TierText.colored(Tier.LV)
                .startsWith(EnumChatFormatting.DARK_GREEN.toString()));
        assertTrue(
            TierText.colored(Tier.MV)
                .startsWith(EnumChatFormatting.GOLD.toString()));
        assertTrue(
            TierText.colored(Tier.HV)
                .startsWith(EnumChatFormatting.YELLOW.toString()));
        assertTrue(
            TierText.colored(Tier.ZPM)
                .startsWith(EnumChatFormatting.AQUA.toString()));
    }
}
