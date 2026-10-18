package com.gregguns.core.selfcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class UnificationChecksTest {

    private static final List<String> PREFIXES = Arrays.asList("plate", "plateDouble", "dust", "circuit", "ring");

    private static UnificationDescriptor item(String name, String... ores) {
        return new UnificationDescriptor("gregguns:" + name, Arrays.asList(ores), null, null);
    }

    @Test
    void aGregGunsPlateSteelIsAViolation() {
        List<String> out = UnificationChecks.check(Collections.singletonList(item("part_x", "plateSteel")), PREFIXES);
        assertEquals(1, out.size());
        assertTrue(
            out.get(0)
                .contains("plateSteel"));
        assertTrue(
            out.get(0)
                .contains("GT prefix plate"));
    }

    @Test
    void theBarePrefixAndTheLongestPrefixCollide() {
        assertEquals("plate", UnificationChecks.collidingPrefix("plate", PREFIXES));
        assertEquals("plateDouble", UnificationChecks.collidingPrefix("plateDoubleBronze", PREFIXES));
        assertEquals("circuit", UnificationChecks.collidingPrefix("circuitBasic", PREFIXES));
    }

    @Test
    void namespacedNamesAndLowercaseContinuationsAreFine() {
        assertNull(UnificationChecks.collidingPrefix("partGunReceiver", PREFIXES));
        assertNull(UnificationChecks.collidingPrefix("platen", PREFIXES));
        assertNull(UnificationChecks.collidingPrefix("gregguns:plateSteel", PREFIXES));
        assertTrue(
            UnificationChecks.check(Collections.singletonList(item("part_x", "partGunReceiver")), PREFIXES)
                .isEmpty());
    }

    @Test
    void anItemGtUnifiesAsAMaterialPartIsAViolation() {
        UnificationDescriptor unified = new UnificationDescriptor(
            "gregguns:part_x",
            Collections.<String>emptyList(),
            "plate",
            "Steel");
        List<String> out = UnificationChecks.check(Collections.singletonList(unified), PREFIXES);
        assertEquals(1, out.size());
        assertTrue(
            out.get(0)
                .contains("plate.Steel"));
    }

    @Test
    void anItemWithNoOreNamesPasses() {
        assertTrue(
            UnificationChecks.check(Collections.singletonList(item("part_x")), PREFIXES)
                .isEmpty());
    }

    @Test
    void aComponentWithoutARecipeFailsUnlessAllowlistedOrDeferred() {
        Map<String, String> allow = new HashMap<>();
        allow.put("gregguns:cell_depleted", "made by use");
        List<String> out = ComponentChecks.check(
            Arrays.asList("gregguns:a", "gregguns:b", "gregguns:cell_depleted", "gregguns:laser"),
            Arrays.asList("gregguns:a"),
            allow,
            Arrays.asList("gregguns:laser"));
        assertEquals(1, out.size());
        assertTrue(
            out.get(0)
                .contains("gregguns:b"));
    }

    @Test
    void aStaleOrReasonlessAllowlistEntryFails() {
        Map<String, String> allow = new HashMap<>();
        allow.put("gregguns:a", "loot only");
        allow.put("gregguns:b", " ");
        List<String> out = ComponentChecks.check(
            Arrays.asList("gregguns:a", "gregguns:b"),
            Arrays.asList("gregguns:a"),
            allow,
            Collections.<String>emptyList());
        assertEquals(2, out.size());
    }

    @Test
    void selfCheckRunReportsBothRules() {
        ComponentInput input = new ComponentInput(
            Collections.singletonList(item("part_x", "plateSteel")),
            PREFIXES,
            Arrays.asList("gregguns:part_x"),
            Collections.<String, String>emptyMap(),
            Collections.<String>emptyList());
        ContentDescriptor content = new ContentDescriptor(
            "gregguns:part_x",
            false,
            "item.gregguns.part_x.name",
            "gregguns:part_x",
            Tier.LV,
            false,
            true);
        ContentChecksTest.FakeResources resources = new ContentChecksTest.FakeResources()
            .with(ContentChecks.LANG_PATH, "item.gregguns.part_x.name=X\n")
            .with("assets/gregguns/textures/items/part_x.png", "png");
        SelfCheck.Report report = SelfCheck.run(
            Collections.singletonList(content),
            Collections.<RecipeDescriptor>emptyList(),
            resources,
            Collections.<String>emptyList(),
            input);
        assertEquals(
            2,
            report.violations()
                .size());
    }
}
