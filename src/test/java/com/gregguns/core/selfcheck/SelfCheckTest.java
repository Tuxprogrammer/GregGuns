package com.gregguns.core.selfcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class SelfCheckTest {

    private static ContentChecksTest.FakeResources resources() {
        return new ContentChecksTest.FakeResources().with(ContentChecks.LANG_PATH, "item.gregguns.a.name=A\n")
            .with("assets/gregguns/textures/items/a.png", "png");
    }

    @Test
    void cleanRunHasNoViolationsAndASummary() {
        ContentDescriptor a = new ContentDescriptor(
            "gregguns:a",
            false,
            "item.gregguns.a.name",
            "gregguns:a",
            Tier.LV,
            false,
            true);
        SelfCheck.Report report = SelfCheck.run(
            Collections.singletonList(a),
            Collections.<RecipeDescriptor>emptyList(),
            resources(),
            Collections.<String>emptyList());
        assertTrue(
            report.violations()
                .isEmpty());
        assertTrue(
            report.summary()
                .contains("1 items/blocks, 0 recipes"));
        report.throwIfFailed();
    }

    @Test
    void emptyRunPassesTrivially() {
        SelfCheck.Report report = SelfCheck.run(
            Collections.<ContentDescriptor>emptyList(),
            Collections.<RecipeDescriptor>emptyList(),
            resources(),
            Collections.<String>emptyList());
        assertTrue(
            report.violations()
                .isEmpty());
    }

    @Test
    void failureListsEveryViolationAcrossAllChecks() {
        ContentDescriptor bad = new ContentDescriptor(
            "gregguns:bad",
            false,
            "item.gregguns.bad.name",
            "gregguns:bad",
            null,
            false,
            false);
        RecipeDescriptor recipe = new RecipeDescriptor(
            "r1",
            480,
            false,
            Arrays.asList(StackDescriptor.item("null", false, false, null)),
            Arrays.asList(StackDescriptor.item("gregguns:a", true, true, Tier.LV)));
        ContentChecksTest.FakeResources r = resources()
            .with(SoundChecks.SOUNDS_JSON_PATH, "{\"e\":{\"sounds\":[\"missing\"]}}");
        SelfCheck.Report report = SelfCheck
            .run(Collections.singletonList(bad), Collections.singletonList(recipe), r, Arrays.asList("glue problem"));

        // glue (1) + content (4: tier, lang, texture, tab) + recipe (2: null input, machine) + sound (1)
        assertEquals(
            8,
            report.violations()
                .size());
        SelfCheckFailedException e = assertThrows(SelfCheckFailedException.class, report::throwIfFailed);
        assertTrue(
            e.getMessage()
                .contains("8 violation(s)"));
        for (String violation : report.violations()) {
            assertTrue(
                e.getMessage()
                    .contains(violation));
        }
    }
}
