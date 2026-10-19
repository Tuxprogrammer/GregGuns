package com.gregguns.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FxRegistryTest {

    private static FxRegistry loaded(String text) {
        FxRegistry r = new FxRegistry();
        r.load(text, "t.txt");
        return r;
    }

    @Test
    void lookupsIgnoreCase() {
        FxRegistry r = loaded("FXLIST RocketExplosion {\n Blast\n}\nBlast {\n}\n");
        assertEquals(
            1,
            r.resolve("rocketEXPLOSION")
                .size());
        assertTrue(r.has("BLAST"));
    }

    @Test
    void doesNotReproducePFX02_anUnknownNameResolvesToNothingInsteadOfNull() {
        // Techguns returns null for an undefined name (AntiGravRingGreen) and its callers throw.
        assertTrue(
            new FxRegistry().resolve("AntiGravRingGreen")
                .isEmpty());
    }

    @Test
    void unknownListMembersAreSkippedAndReported() {
        FxRegistry r = loaded("FXLIST L {\n Real\n Ghost\n}\nReal {\n}\n");
        assertEquals(
            1,
            r.resolve("L")
                .size());
        assertEquals(
            1,
            r.danglingMembers()
                .size());
        assertTrue(
            r.danglingMembers()
                .iterator()
                .next()
                .contains("Ghost"));
    }

    @Test
    void aListMayNameAnotherListAndACycleEndsAtTheDepthLimit() {
        FxRegistry r = loaded("FXLIST A {\n B\n}\nFXLIST B {\n S\n A\n}\nS {\n}\n");
        // A -> B -> (S, A -> B -> ...) terminates; S appears once per pass until the depth limit.
        int n = r.resolve("A")
            .size();
        assertTrue(n >= 1 && n <= FxRegistry.MAX_DEPTH, "got " + n);
    }

    @Test
    void aLaterDefinitionReplacesAnEarlierOne() {
        FxRegistry r = loaded("S {\n Columns = 2\n}\n");
        r.load("S {\n Columns = 3\n}\n", "two.txt");
        assertEquals(
            3,
            r.system("s")
                .columns());
    }

    @Test
    void errorsAccumulateAcrossFiles() {
        FxRegistry r = new FxRegistry();
        r.load("A {\n Bogus = 1\n}\n", "one.txt");
        r.load("B {\n Bogus = 1\n}\n", "two.txt");
        assertEquals(
            2,
            r.errors()
                .size());
        assertFalse(r.has("a"));
    }

    @Test
    void theShippedDataFilesParseWithNoErrorsAndNoDanglingMembers() {
        FxRegistry shipped = FxResources.shared();
        assertTrue(
            shipped.errors()
                .isEmpty(),
            shipped.errors()
                .toString());
        assertTrue(
            shipped.danglingMembers()
                .isEmpty(),
            shipped.danglingMembers()
                .toString());
    }
}
