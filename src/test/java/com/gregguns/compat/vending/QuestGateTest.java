package com.gregguns.compat.vending;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class QuestGateTest {

    @Test
    void theGatesAreThePacksTierQuests() {
        assertEquals(44, QuestGate.packQuest(Tier.STEAM));
        assertEquals(88, QuestGate.packQuest(Tier.LV));
        assertEquals(1148, QuestGate.packQuest(Tier.MV));
        assertEquals(160, QuestGate.packQuest(Tier.HV));
        assertEquals(176, QuestGate.packQuest(Tier.EV));
        assertEquals(213, QuestGate.packQuest(Tier.IV));
        assertEquals(1493, QuestGate.packQuest(Tier.LUV));
        assertEquals(2610, QuestGate.packQuest(Tier.ZPM));
    }

    @Test
    void aPackQuestIdHasAZeroHighHalf() {
        // BetterQuesting keeps the pack's legacy numeric quest IDs as questIDHigh 0, questIDLow <number>.
        assertEquals(new UUID(0L, 88L), QuestGate.questId(Tier.LV));
        assertEquals(new UUID(0L, 2610L), QuestGate.questId(Tier.ZPM));
    }

    @Test
    void everyTierHasItsOwnGate() {
        Set<Integer> quests = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for (Tier tier : Tier.values()) {
            quests.add(QuestGate.packQuest(tier));
            keys.add(QuestGate.gateKey(tier));
        }
        assertEquals(Tier.values().length, quests.size(), "two tiers share a gate quest");
        assertEquals(Tier.values().length, keys.size(), "two tiers share a gate key");
        assertEquals("pack.luv", QuestGate.gateKey(Tier.LUV));
    }

    /**
     * The quest line's tier-skip triggers already rely on these pack quests. Each gate from LV up is the first
     * prerequisite of the matching {@code gg.skip.<tier>} trigger, so the shop opens at the same point the GregGuns tab
     * lets a player skip ahead.
     */
    @Test
    void eachGateIsThatTiersSkipTriggerPrerequisite() throws IOException {
        String text = new String(
            Files.readAllBytes(Paths.get("tools/questbook/quests/skip.json")),
            StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("\"key\"\\s*:\\s*\"gg\\.skip\\.([a-z]+)\"[^{}]*?\"pre\"\\s*:\\s*\\[\\s*(\\d+)")
            .matcher(text);
        int checked = 0;
        while (m.find()) {
            Tier tier = Tier.valueOf(
                m.group(1)
                    .toUpperCase(Locale.ROOT));
            assertEquals(
                Integer.parseInt(m.group(2)),
                QuestGate.packQuest(tier),
                "gate of " + tier + " vs the first prerequisite of gg.skip." + m.group(1));
            checked++;
        }
        assertEquals(Tier.values().length - 1, checked, "one skip trigger per tier from LV to ZPM");
    }

    /** The questbook spec lists every pack quest GregGuns depends on; the shop's gates are among them. */
    @Test
    void everyGateIsAReferencedPackQuest() throws IOException {
        String index = new String(
            Files.readAllBytes(Paths.get("docs/gregguns-spec/12-questbook/index.md")),
            StandardCharsets.UTF_8);
        for (Tier tier : Tier.values()) {
            int id = QuestGate.packQuest(tier);
            assertTrue(
                Pattern.compile("(?m)^\\| " + id + " \\|")
                    .matcher(index)
                    .find(),
                "pack quest " + id + " (" + tier + ") is not in the Referenced pack quests table");
        }
    }
}
