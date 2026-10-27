package com.gregguns.icbm.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ContactTest {

    private static Contact at(int id, double x, double z, double ix, double iz) {
        return new Contact(id, 0, x, 100, z, 0.5, 0, 0, 1, ix, iz);
    }

    @Test
    void impactInsideTheCircleIsIncomingAndTheEdgeCounts() {
        assertTrue(ContactMath.incoming(30, 40, 0, 0, 50));
        assertFalse(ContactMath.incoming(30, 41, 0, 0, 50));
    }

    @Test
    void aMissileThatPassesByIsNotIncomingWhateverItsHeading() {
        // Flying straight at the radar, but landing 400 blocks past it.
        Contact c = new Contact(1, 0, 100, 80, 0, -0.8, 0, 0, 1, -400, 0);
        assertFalse(c.incoming(0, 0, 100));
        assertTrue(c.incoming(-400, 0, 20));
    }

    @Test
    void aNegativeRadiusProtectsNothing() {
        assertFalse(ContactMath.incoming(0, 0, 0, 0, -1));
    }

    @Test
    void rangeIsACircleNotAChunkAlignedSquare() {
        assertTrue(ContactMath.inRange(70, 70, 0, 0, 100));
        assertFalse(ContactMath.inRange(80, 80, 0, 0, 100));
        assertFalse(ContactMath.inRange(110, 0, 0, 0, 100));
    }

    @Test
    void nearestFirstWithIdBreakingTies() {
        List<Contact> l = new ArrayList<Contact>();
        l.add(at(3, 50, 0, 0, 0));
        l.add(at(2, 10, 0, 0, 0));
        l.add(at(1, 0, 10, 0, 0));
        Contacts.sortNearest(l, 0, 0);
        assertEquals(
            1,
            l.get(0)
                .id());
        assertEquals(
            2,
            l.get(1)
                .id());
        assertEquals(
            3,
            l.get(2)
                .id());
    }

    @Test
    void incomingFilterKeepsOrder() {
        List<Contact> l = new ArrayList<Contact>();
        l.add(at(1, 0, 0, 500, 500));
        l.add(at(2, 0, 0, 5, 5));
        l.add(at(3, 0, 0, -5, 5));
        List<Contact> in = Contacts.incoming(l, 0, 0, 20);
        assertEquals(2, in.size());
        assertEquals(
            2,
            in.get(0)
                .id());
        assertEquals(
            3,
            in.get(1)
                .id());
    }

    @Test
    void speedIsTheVectorLength() {
        assertEquals(5.0, new Contact(1, 0, 0, 0, 0, 3, 4, 0, 2, 0, 0).speed(), 1e-9);
    }
}
