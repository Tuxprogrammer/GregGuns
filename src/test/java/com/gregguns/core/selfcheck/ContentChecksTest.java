package com.gregguns.core.selfcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class ContentChecksTest {

    /** In-memory resources. */
    static final class FakeResources implements ResourceSource {

        private final Map<String, String> files = new HashMap<>();

        FakeResources with(String path, String text) {
            files.put(path, text);
            return this;
        }

        @Override
        public boolean exists(String path) {
            return files.containsKey(path);
        }

        @Override
        public String readText(String path) throws IOException {
            String text = files.get(path);
            if (text == null) {
                throw new IOException("missing " + path);
            }
            return text;
        }
    }

    private static FakeResources goodResources() {
        return new FakeResources().with(ContentChecks.LANG_PATH, "# header\nitem.gregguns.pistol.name=Pistol\n")
            .with("assets/gregguns/textures/items/pistol.png", "png");
    }

    private static ContentDescriptor item(Tier tier, String lang, String icon, boolean hidden, boolean tab) {
        return new ContentDescriptor("gregguns:pistol", false, lang, icon, tier, hidden, tab);
    }

    @Test
    void completeItemPasses() {
        List<ContentDescriptor> items = Collections
            .singletonList(item(Tier.LV, "item.gregguns.pistol.name", "gregguns:pistol", false, true));
        assertTrue(
            ContentChecks.check(items, goodResources())
                .isEmpty());
    }

    @Test
    void hiddenItemWithoutTabPasses() {
        List<ContentDescriptor> items = Collections
            .singletonList(item(Tier.LV, "item.gregguns.pistol.name", "gregguns:pistol", true, false));
        assertTrue(
            ContentChecks.check(items, goodResources())
                .isEmpty());
    }

    @Test
    void missingTierLangTextureAndTabAreAllReportedTogether() {
        List<ContentDescriptor> items = Collections
            .singletonList(item(null, "item.gregguns.nope.name", "gregguns:nope", false, false));
        List<String> violations = ContentChecks.check(items, goodResources());
        assertEquals(4, violations.size());
        assertTrue(
            violations.get(0)
                .contains("no tier"));
        assertTrue(
            violations.get(1)
                .contains("lang key"));
        assertTrue(
            violations.get(2)
                .contains("textures/items/nope.png"));
        assertTrue(
            violations.get(3)
                .contains("creative tab"));
    }

    @Test
    void missingIconNameIsReported() {
        List<ContentDescriptor> items = Collections
            .singletonList(item(Tier.LV, "item.gregguns.pistol.name", null, false, true));
        assertTrue(
            ContentChecks.check(items, goodResources())
                .get(0)
                .contains("no icon name"));
    }

    @Test
    void blocksUseTheBlocksTextureFolder() {
        ContentDescriptor block = new ContentDescriptor(
            "gregguns:crate",
            true,
            "tile.gregguns.crate.name",
            "gregguns:crate",
            Tier.LV,
            false,
            true);
        FakeResources resources = new FakeResources().with(ContentChecks.LANG_PATH, "tile.gregguns.crate.name=Crate")
            .with("assets/gregguns/textures/blocks/crate.png", "png");
        assertTrue(
            ContentChecks.check(Collections.singletonList(block), resources)
                .isEmpty());
        assertEquals("assets/gregguns/textures/blocks/crate.png", ContentChecks.texturePath("gregguns:crate", true));
    }

    @Test
    void iconWithoutDomainResolvesToMinecraft() {
        assertEquals("assets/minecraft/textures/items/x.png", ContentChecks.texturePath("x", false));
    }

    @Test
    void missingLangFileIsReportedOnceAndSkipsKeyChecks() {
        List<ContentDescriptor> items = Arrays
            .asList(item(Tier.LV, "item.gregguns.pistol.name", "gregguns:pistol", false, true));
        FakeResources resources = new FakeResources().with("assets/gregguns/textures/items/pistol.png", "png");
        List<String> violations = ContentChecks.check(items, resources);
        assertEquals(1, violations.size());
        assertTrue(
            violations.get(0)
                .contains("cannot read"));
    }

    @Test
    void langParserIgnoresCommentsBlankLinesAndHandlesCrlf() {
        assertEquals(
            new HashSet<>(Arrays.asList("a.b", "c")),
            ContentChecks.parseLangKeys("# comment\r\na.b=One\r\n\r\n c = Two=2\r\nnoequals\r\n"));
    }
}
