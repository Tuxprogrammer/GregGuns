package com.gregguns.icbm.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.Predicate;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;

/** The front a generated machine gets, and the audit that notices one left facing down. */
class MachineFacingTest {

    private static final Predicate<ForgeDirection> ANY = d -> d != ForgeDirection.UNKNOWN;
    private static final Predicate<ForgeDirection> HORIZONTAL = d -> d != ForgeDirection.UNKNOWN
        && d != ForgeDirection.UP
        && d != ForgeDirection.DOWN;

    @Test
    void theDoorSideIsPreferred() {
        assertEquals(ForgeDirection.NORTH, MachineFacing.choose(0, HORIZONTAL));
        assertEquals(ForgeDirection.EAST, MachineFacing.choose(1, HORIZONTAL));
        assertEquals(ForgeDirection.SOUTH, MachineFacing.choose(2, HORIZONTAL));
        assertEquals(ForgeDirection.WEST, MachineFacing.choose(3, HORIZONTAL));
        assertEquals(ForgeDirection.WEST, MachineFacing.choose(7, HORIZONTAL));
    }

    @Test
    void aBlockedSideFallsToTheNextHorizontalOne() {
        Predicate<ForgeDirection> notSouth = d -> HORIZONTAL.test(d) && d != ForgeDirection.SOUTH;
        assertEquals(ForgeDirection.WEST, MachineFacing.choose(2, notSouth));
    }

    @Test
    void aVerticalOnlyMachineGetsAVerticalSide() {
        Predicate<ForgeDirection> vertical = d -> d == ForgeDirection.UP || d == ForgeDirection.DOWN;
        assertEquals(ForgeDirection.UP, MachineFacing.choose(1, vertical));
        assertNull(MachineFacing.choose(1, d -> false));
    }

    @Test
    void neverDownWhenAHorizontalSideWorks() {
        assertNotNull(MachineFacing.choose(0, ANY));
        assertEquals(ForgeDirection.NORTH, MachineFacing.choose(0, ANY));
    }

    @Test
    void theAuditFlagsDownOnlyWhereAHorizontalSideIsValid() {
        assertNotNull(MachineFacingAudit.problem(ForgeDirection.DOWN, null, HORIZONTAL));
        assertNull(MachineFacingAudit.problem(ForgeDirection.EAST, null, HORIZONTAL));
        assertNull(MachineFacingAudit.problem(ForgeDirection.UP, null, d -> d == ForgeDirection.UP));
        // A basic machine with its main facing unset is reported too.
        assertNotNull(MachineFacingAudit.problem(ForgeDirection.NORTH, ForgeDirection.UNKNOWN, HORIZONTAL));
        assertNull(MachineFacingAudit.problem(ForgeDirection.NORTH, ForgeDirection.SOUTH, HORIZONTAL));
    }
}
