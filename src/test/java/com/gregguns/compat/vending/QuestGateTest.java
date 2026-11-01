package com.gregguns.compat.vending;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class QuestGateTest {

    /** The shipped quest files are named {@code <title>-<url-safe base64 of the quest UUID>.json}. */
    private static final Path QUESTS = Paths.get("src/main/resources/assets/gregguns/questbook/Quests");

    @Test
    void uuid5MatchesPythonsUuid5() {
        // Python: uuid.uuid5(uuid.UUID('6f1f3c52-9d0a-4b6e-8a53-2c7e0f4b91d8'), key)
        assertEquals(UUID.fromString("98947aed-29d1-500c-a3ed-dfad40ff65a3"), QuestGate.questId("gg.lv.pistol"));
        assertEquals(UUID.fromString("85851e39-3edc-5e33-a809-3e4b32292e64"), QuestGate.questId("gg.lv.intro"));
        assertEquals(UUID.fromString("21de8ba9-7b65-5582-8d36-2fcb877359b9"), QuestGate.questId("gg.steam.intro"));
        assertEquals(UUID.fromString("1aaa7078-f215-57a1-9e0d-187ec8c93393"), QuestGate.questId("gg.zpm.intro"));
    }

    @Test
    void introKeysFollowTheChapterNames() {
        assertEquals("gg.steam.intro", QuestGate.introKey(Tier.STEAM));
        assertEquals("gg.luv.intro", QuestGate.introKey(Tier.LUV));
        assertEquals("gg.zpm.intro", QuestGate.introKey(Tier.ZPM));
    }

    @Test
    void theNamespaceIsTheQuestLines() throws IOException {
        Path source = Paths.get("tools/questbook/quests/_line.json");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("\"namespace\"\\s*:\\s*\"([0-9a-f-]+)\"")
            .matcher(text);
        assertTrue(m.find(), "no namespace in " + source);
        assertEquals(QuestGate.NAMESPACE, UUID.fromString(m.group(1)), "QuestGate.NAMESPACE must match " + source);
    }

    @Test
    void everyGateQuestShipsInTheJar() throws IOException {
        for (Tier tier : Tier.values()) {
            UUID id = QuestGate.questId(QuestGate.introKey(tier));
            String suffix = "-" + Base64.getUrlEncoder()
                .encodeToString(
                    ByteBuffer.allocate(16)
                        .putLong(id.getMostSignificantBits())
                        .putLong(id.getLeastSignificantBits())
                        .array())
                + ".json";
            try (Stream<Path> files = Files.walk(QUESTS)) {
                assertTrue(
                    files.anyMatch(
                        p -> p.getFileName()
                            .toString()
                            .endsWith(suffix)),
                    "the gate quest " + QuestGate.introKey(tier) + " (" + id + ") is not in " + QUESTS);
            }
        }
    }
}
