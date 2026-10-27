package com.gregguns.icbm.defense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.detect.Contact;

class RadarRulesTest {

    @Test
    void rangeGrowsWithTierAndIsNoneBelowHv() {
        assertEquals(0, RadarRules.range(2));
        assertEquals(128, RadarRules.range(3));
        assertEquals(256, RadarRules.range(4));
        int last = 0;
        for (int t = 3; t <= 12; t++) {
            assertTrue(RadarRules.range(t) >= last, "tier " + t);
            last = RadarRules.range(t);
        }
        assertEquals(1536, RadarRules.range(12));
    }

    @Test
    void radiusIsClampedToTheFloorAndTheRange() {
        assertEquals(RadarRules.MIN_RADIUS, RadarRules.clampRadius(-5, 128));
        assertEquals(128, RadarRules.clampRadius(9999, 128));
        assertEquals(64, RadarRules.clampRadius(64, 128));
    }

    @Test
    void redstoneIsTheIncomingCountUpToFifteen() {
        assertEquals(0, RadarRules.redstone(0));
        assertEquals(3, RadarRules.redstone(3));
        assertEquals(15, RadarRules.redstone(40));
    }

    @Test
    void stateFollowsPowerContactsAndIncoming() {
        assertEquals(RadarRules.State.OFF, RadarRules.state(false, 5, 5));
        assertEquals(RadarRules.State.CLEAR, RadarRules.state(true, 0, 0));
        assertEquals(RadarRules.State.TRACKING, RadarRules.state(true, 2, 0));
        assertEquals(RadarRules.State.THREAT, RadarRules.state(true, 2, 1));
    }

    @Test
    void powerDrawIsAQuarterAmp() {
        assertEquals(128, RadarRules.euPerTick(512));
    }

    @Test
    void theSweepSplitsDetectedFromIncomingNearestFirst() {
        List<Contact> in = new ArrayList<Contact>();
        in.add(new Contact(1, 0, 90, 80, 0, -1, 0, 0, 1, 500, 500));
        in.add(new Contact(2, 0, 40, 80, 0, -1, 0, 0, 1, 10, 0));
        in.add(new Contact(3, 0, 60, 80, 0, -1, 0, 0, 2, -10, 0));
        Sweep s = Sweep.of(in, 0, 0, 64);
        assertEquals(
            3,
            s.detected()
                .size());
        assertEquals(
            2,
            s.detected()
                .get(0)
                .id());
        assertEquals(
            2,
            s.incoming()
                .size());
        assertEquals(
            2,
            s.incoming()
                .get(0)
                .id());
        assertEquals(
            3,
            s.incoming()
                .get(1)
                .id());
    }

    @Test
    void theTowerPieceIsThreeBySevenBySix() {
        assertEquals(RadarTowerShape.DEPTH, RadarTowerShape.PIECES[0].length);
        for (String[] aisle : RadarTowerShape.PIECES[0]) {
            assertEquals(RadarTowerShape.HEIGHT, aisle.length);
            for (String row : aisle) {
                assertEquals(RadarTowerShape.WIDTH, row.length());
            }
        }
        assertEquals(1, RadarTowerShape.count('~'));
        assertEquals(
            '~',
            RadarTowerShape.PIECES[0][0][RadarTowerShape.CONTROLLER_ROW].charAt(RadarTowerShape.CONTROLLER_COLUMN));
        assertTrue(RadarTowerShape.casingPositions() >= RadarTowerShape.CASING_MINIMUM);
    }
}
