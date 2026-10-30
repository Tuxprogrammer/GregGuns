package com.gregguns.icbm.silo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.silo.SiloSequence.Shaft;
import com.gregguns.icbm.silo.SiloSequence.Stage;
import com.gregguns.icbm.silo.SiloSequence.Step;

/** The silo's staged assembly: body, then warhead, then fuel, each part in the shaft before its cycle (D286). */
class SiloSequenceTest {

    @Test
    void anEmptyShaftTakesABodyFirst() {
        assertEquals(Step.PLACE_BODY, SiloSequence.next(Stage.NONE, Shaft.EMPTY, true, false));
        // Body and warhead together: the body still goes first.
        assertEquals(Step.PLACE_BODY, SiloSequence.next(Stage.NONE, Shaft.EMPTY, true, true));
    }

    @Test
    void aWarheadWithoutABodyWaitsInTheBus() {
        assertEquals(Step.WAIT_BODY, SiloSequence.next(Stage.NONE, Shaft.EMPTY, false, true));
        assertEquals(Step.NO_BODY, SiloSequence.next(Stage.NONE, Shaft.EMPTY, false, false));
    }

    @Test
    void theBodyRunsItsCycleBeforeAnyWarheadIsTaken() {
        // The body stands; its cycle has not finished: a warhead in the bus waits for it.
        assertEquals(Step.RUN_BODY, SiloSequence.next(Stage.BODY, Shaft.BODY, false, true));
        assertEquals(Stage.BODY_DONE, SiloSequence.afterCycle(Stage.BODY));
    }

    @Test
    void aReadyBodyTakesAFittingWarheadOrWaits() {
        assertEquals(Step.PLACE_WARHEAD, SiloSequence.next(Stage.BODY_DONE, Shaft.BODY, false, true));
        assertEquals(Step.WAIT_WARHEAD, SiloSequence.next(Stage.BODY_DONE, Shaft.BODY, true, false));
    }

    @Test
    void theWarheadCycleEndsTheSequenceAndTheMissileTakesFuel() {
        assertEquals(Step.RUN_WARHEAD, SiloSequence.next(Stage.WARHEAD, Shaft.STACK, false, false));
        assertEquals(Stage.NONE, SiloSequence.afterCycle(Stage.WARHEAD));
        assertEquals(Stage.NONE, SiloSequence.reconcile(Stage.NONE, Shaft.MISSILE));
        assertEquals(Step.PUMP, SiloSequence.next(Stage.NONE, Shaft.MISSILE, true, true));
    }

    @Test
    void aFullRunInOrder() {
        Stage stage = Stage.NONE;
        // Fuel and warhead arrive first, then the body: nothing starts until the body is there.
        assertEquals(Step.WAIT_BODY, SiloSequence.next(stage, Shaft.EMPTY, false, true));
        assertEquals(Step.PLACE_BODY, SiloSequence.next(stage, Shaft.EMPTY, true, true));
        stage = Stage.BODY; // placed, cycle running
        stage = SiloSequence.afterCycle(SiloSequence.reconcile(stage, Shaft.BODY));
        assertEquals(Stage.BODY_DONE, stage);
        assertEquals(Step.PLACE_WARHEAD, SiloSequence.next(stage, Shaft.BODY, false, true));
        stage = Stage.WARHEAD; // seated, cycle running
        stage = SiloSequence.afterCycle(SiloSequence.reconcile(stage, Shaft.STACK));
        assertEquals(Stage.NONE, stage);
        assertEquals(Step.PUMP, SiloSequence.next(stage, Shaft.MISSILE, false, false));
    }

    @Test
    void theWorldWinsOverTheRememberedStage() {
        // The body was broken: the sequence ends whatever stage it was in.
        for (Stage s : Stage.values()) {
            assertEquals(Stage.NONE, SiloSequence.reconcile(s, Shaft.EMPTY));
        }
        // The warhead was taken off the nose: back to waiting for one.
        assertEquals(Stage.BODY_DONE, SiloSequence.reconcile(Stage.WARHEAD, Shaft.BODY));
        // A player armed the stack by hand: the missile is there, the sequence is over.
        assertEquals(Stage.NONE, SiloSequence.reconcile(Stage.WARHEAD, Shaft.MISSILE));
    }

    @Test
    void aBodyFoundInTheShaftIsAdopted() {
        // Stacked by hand, or left by a controller that was broken and rebuilt: it gets its body cycle.
        assertEquals(Stage.BODY, SiloSequence.reconcile(Stage.NONE, Shaft.BODY));
        assertEquals(Stage.BODY, SiloSequence.reconcile(Stage.NONE, Shaft.STACK));
        assertEquals(Step.RUN_BODY, SiloSequence.next(Stage.BODY, Shaft.STACK, false, false));
        // After it, a warhead seated by hand is mated like the silo's own.
        assertEquals(Step.RUN_WARHEAD, SiloSequence.next(Stage.BODY_DONE, Shaft.STACK, false, false));
    }

    @Test
    void aWarheadThatWouldNotArmIsReportedNotMated() {
        assertEquals(Step.BAD_WARHEAD, SiloSequence.next(Stage.BODY_DONE, Shaft.BAD_STACK, false, true));
        assertEquals(Step.BAD_WARHEAD, SiloSequence.next(Stage.WARHEAD, Shaft.BAD_STACK, false, false));
    }

    @Test
    void somethingElseInTheShaftBlocksEveryStep() {
        assertEquals(Stage.NONE, SiloSequence.reconcile(Stage.BODY_DONE, Shaft.OTHER));
        assertEquals(Step.BLOCKED, SiloSequence.next(Stage.NONE, Shaft.OTHER, true, true));
    }

    @Test
    void savedStagesReadBack() {
        for (Stage s : Stage.values()) {
            assertEquals(s, SiloSequence.parse(s.name()));
        }
        assertEquals(Stage.NONE, SiloSequence.parse(""));
        assertEquals(Stage.NONE, SiloSequence.parse(null));
        assertEquals(Stage.NONE, SiloSequence.parse("MATING"));
    }
}
