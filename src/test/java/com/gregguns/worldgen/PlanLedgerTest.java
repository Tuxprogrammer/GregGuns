package com.gregguns.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/** The first-populate-decides rule (PLAN-M8 Q3) and the flag record. */
class PlanLedgerTest {

    @Test
    void firstCallerDecidesAndLaterCallersObeyWithoutRunningTheirDecider() {
        PlanLedger ledger = new PlanLedger();
        String key = PlanKey.of("test", SizeClass.MEDIUM, 2, 5);
        AtomicInteger runs = new AtomicInteger();

        Decision first = ledger.decideOnce(key, () -> {
            runs.incrementAndGet();
            return Decision.placed(64);
        });
        Decision second = ledger.decideOnce(key, () -> {
            runs.incrementAndGet();
            return Decision.rejected("water");
        });

        assertEquals(1, runs.get());
        assertSame(first, second);
        assertTrue(second.isPlaced());
        assertEquals(64, second.groundY());
    }

    @Test
    void aRejectionIsRecordedToo() {
        PlanLedger ledger = new PlanLedger();
        String key = PlanKey.of("test", SizeClass.BIG, 0, 1);
        ledger.decideOnce(key, () -> Decision.rejected("biome"));
        Decision again = ledger.decideOnce(key, () -> Decision.placed(70));
        assertFalse(again.isPlaced());
        assertEquals("biome", again.reason());
        assertEquals(Decision.NO_Y, again.groundY());
    }

    @Test
    void differentKeysAreIndependent() {
        PlanLedger ledger = new PlanLedger();
        ledger.decideOnce(PlanKey.of("test", SizeClass.SMALL, 1, 1), () -> Decision.placed(60));
        ledger.decideOnce(PlanKey.of("test", SizeClass.MEDIUM, 1, 1), () -> Decision.placed(61));
        ledger.decideOnce(PlanKey.of("test", SizeClass.SMALL, 1, 2), () -> Decision.placed(62));
        assertEquals(3, ledger.size());
        assertEquals(
            61,
            ledger.get(PlanKey.of("test", SizeClass.MEDIUM, 1, 1))
                .groundY());
    }

    @Test
    void aThrowingDeciderRecordsNothingSoTheNextWindowCanTry() {
        PlanLedger ledger = new PlanLedger();
        String key = PlanKey.of("test", SizeClass.SMALL, 0, 0);
        assertThrows(
            IllegalArgumentException.class,
            () -> ledger.decideOnce(key, () -> { throw new IllegalArgumentException("boom"); }));
        assertNull(ledger.get(key));
        assertEquals(0, ledger.revision());
        assertEquals(
            70,
            ledger.decideOnce(key, () -> Decision.placed(70))
                .groundY());
    }

    @Test
    void aNullDecisionIsRefused() {
        PlanLedger ledger = new PlanLedger();
        assertThrows(IllegalStateException.class, () -> ledger.decideOnce("k", () -> null));
        assertEquals(0, ledger.size());
    }

    @Test
    void revisionMovesOnlyWhenSomethingChanges() {
        PlanLedger ledger = new PlanLedger();
        ledger.decideOnce("k", () -> Decision.placed(64));
        int after = ledger.revision();
        ledger.decideOnce("k", () -> Decision.placed(99));
        assertEquals(after, ledger.revision());
        assertTrue(ledger.addFlag("k", new FlagPos(1, 2, 3)));
        assertEquals(after + 1, ledger.revision());
        assertFalse(ledger.addFlag("k", new FlagPos(1, 2, 3)));
        assertEquals(after + 1, ledger.revision());
    }

    @Test
    void flagsAttachOnlyToPlacedPlans() {
        PlanLedger ledger = new PlanLedger();
        assertFalse(ledger.addFlag("none", new FlagPos(0, 0, 0)));
        ledger.decideOnce("rej", () -> Decision.rejected("water"));
        assertFalse(ledger.addFlag("rej", new FlagPos(0, 0, 0)));
        assertTrue(
            ledger.get("rej")
                .flags()
                .isEmpty());
        ledger.decideOnce("ok", () -> Decision.placed(66));
        ledger.addFlag("ok", new FlagPos(5, 67, 6));
        ledger.addFlag("ok", new FlagPos(7, 67, 8));
        assertEquals(
            2,
            ledger.get("ok")
                .flags()
                .size());
    }

    @Test
    void blankRejectionReasonBecomesUnknown() {
        assertEquals(
            "unknown",
            Decision.rejected("  ")
                .reason());
        assertEquals(
            "unknown",
            Decision.rejected(null)
                .reason());
    }

    @Test
    void planKeyFollowsClassAndCell() {
        assertEquals("test:MEDIUM:-3:4", PlanKey.of("test", SizeClass.MEDIUM, -3, 4));
        StructurePlan plan = new StructurePlan("x", SizeClass.BIG, 0, 7, -8, 0, 0, 0, 0, new Footprint(1, 1, 1), 1L);
        assertEquals("x:BIG:7:-8", PlanKey.of(plan));
        org.junit.jupiter.api.Assertions
            .assertNotEquals(PlanKey.of("a", SizeClass.BIG, 1, 1), PlanKey.of("b", SizeClass.BIG, 1, 1));
    }
}
