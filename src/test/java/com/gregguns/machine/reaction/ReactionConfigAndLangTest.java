package com.gregguns.machine.reaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.core.config.MachinesConfig;

class ReactionConfigAndLangTest {

    @Test
    void configDefaultsMatchTheSheet() {
        // GGS-08-25
        assertEquals(100, MachinesConfig.retortPollutionPerSecond);
        assertEquals(1, MachinesConfig.explosionLowVoltageIndex);
        assertEquals(2, MachinesConfig.explosionMediumVoltageIndex);
        assertEquals(1, MachinesConfig.radiationLevelLow);
        assertEquals(3, MachinesConfig.radiationLevelMedium);
        assertEquals(5, MachinesConfig.radiationLevelHigh);
    }

    @Test
    void riskNamesRoundTripAndUnknownIsNone() {
        for (ReactionRisk risk : ReactionRisk.values()) {
            assertEquals(risk, ReactionRisk.byName(risk.name()));
        }
        assertEquals(ReactionRisk.NONE, ReactionRisk.byName("EXPLOSION_HIGH"));
        assertEquals(ReactionRisk.NONE, ReactionRisk.byName(null));
    }

    @Test
    void radiationRisksAreRadiationAndExplosionsAreExplosions() {
        assertTrue(ReactionRisk.RAD_LOW.isRadiation());
        assertTrue(ReactionRisk.RAD_HIGH.isRadiation());
        assertFalse(ReactionRisk.EXPLOSION_LOW.isRadiation());
        assertTrue(ReactionRisk.EXPLOSION_MEDIUM.isExplosion());
        assertFalse(ReactionRisk.BREAK_ITEM.isExplosion());
    }

    @Test
    void everyKeyTheReactionCodeUsesIsInTheLangFile() throws IOException {
        // GGS-08-29, GGS-08-33
        List<String> lines = Files
            .readAllLines(Paths.get("src/main/resources/assets/gregguns/lang/en_US.lang"), StandardCharsets.UTF_8);
        String[] keys = { "gregguns.recipe.reactionchamber", "item.gregguns.part_heat_ray_focus.name",
            "item.gregguns.part_laser_focus.name", "item.gregguns.part_anti_gravity_core.name",
            "tile.gregguns.casing_reaction.name", "gregguns.reaction.gui.status", "gregguns.reaction.nei.intensity",
            "gregguns.reaction.nei.completion", "gregguns.machine.reaction_chamber.tooltip",
            "gregguns.machine.reaction_chamber.tooltip2", "gregguns.machine.reaction_chamber.type",
            "gregguns.retort.tt.type", "gregguns.retort.error.energy_tier",
            "GT5U.gui.text.recipe_result.gregguns_rc_intensity", "GT5U.gui.text.shutdown_reason.gregguns_rc_failed" };
        for (String key : keys) {
            assertTrue(
                lines.stream()
                    .anyMatch(line -> line.startsWith(key + "=")),
                key);
        }
        for (ReactionRisk risk : ReactionRisk.values()) {
            String key = "gregguns.reaction.risk." + risk.name()
                .toLowerCase();
            assertTrue(
                lines.stream()
                    .anyMatch(line -> line.startsWith(key + "=")),
                key);
        }
    }
}
