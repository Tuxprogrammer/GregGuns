package com.gregguns.network.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SenderRulesTest {

    private final FakeSender sender = new FakeSender();

    private SenderRules rules() {
        return new SenderRules(sender);
    }

    @Test
    void acceptsTargetInSameDimension() {
        sender.dimension = -1;
        assertNull(
            rules().sameDimension(-1)
                .firstViolation());
    }

    @Test
    void rejectsTargetInAnotherDimension() {
        sender.dimension = 0;
        assertEquals(
            Rejection.WRONG_DIMENSION,
            rules().sameDimension(1)
                .firstViolation()
                .rejection());
    }

    @Test
    void acceptsBlockInsideReach() {
        assertNull(
            rules().withinReachOfBlock(3, 0, 3)
                .firstViolation());
    }

    @Test
    void rejectsBlockBeyondDefaultReach() {
        assertEquals(
            Rejection.OUT_OF_REACH,
            rules().withinReachOfBlock(9, 0, 0)
                .firstViolation()
                .rejection());
    }

    @Test
    void reachIsMeasuredInThreeDimensions() {
        assertEquals(
            Rejection.OUT_OF_REACH,
            rules().withinReachOfBlock(0, 9, 0)
                .firstViolation()
                .rejection());
    }

    @Test
    void reachBoundaryIsInclusive() {
        assertNull(
            rules().withinReachOf(8, 0, 0, 8.0)
                .firstViolation());
        assertEquals(
            Rejection.OUT_OF_REACH,
            rules().withinReachOf(8.01, 0, 0, 8.0)
                .firstViolation()
                .rejection());
    }

    @Test
    void customReachIsHonoured() {
        assertNull(
            rules().withinReachOf(20, 0, 0, 24.0)
                .firstViolation());
    }

    @Test
    void nanCoordinatesAreOutOfReach() {
        assertEquals(
            Rejection.OUT_OF_REACH,
            rules().withinReachOf(Double.NaN, 0, 0, 8.0)
                .firstViolation()
                .rejection());
    }

    @Test
    void nonPositiveReachIsAProgrammingError() {
        assertThrows(IllegalArgumentException.class, () -> rules().withinReachOf(0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> rules().withinReachOf(0, 0, 0, Double.NaN));
    }

    @Test
    void deadSenderIsRejected() {
        sender.alive = false;
        assertEquals(
            Rejection.SENDER_INACTIVE,
            rules().senderMayAct()
                .firstViolation()
                .rejection());
    }

    @Test
    void spectatorLikeSenderIsRejected() {
        sender.spectatorLike = true;
        assertEquals(
            Rejection.SENDER_SPECTATOR,
            rules().senderMayAct()
                .firstViolation()
                .rejection());
    }

    @Test
    void livingSenderMayAct() {
        assertNull(
            rules().senderMayAct()
                .firstViolation());
    }

    @Test
    void permissionHookRejectsAndAccepts() {
        assertEquals(
            Rejection.UNAUTHORISED,
            rules().authorised("owner", s -> false)
                .firstViolation()
                .rejection());
        assertNull(
            rules().authorised("owner", s -> true)
                .firstViolation());
    }
}
