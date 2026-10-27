package com.gregguns.icbm.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.detect.ContactSource;

/** The {@code getContacts()} records and the radar signals (GGI-01-05, 06). */
class ContactRecordsTest {

    private static final class Fake implements ContactSource {

        final int id;
        final double x;
        final double z;
        final double vx;
        final double vz;
        final double impactX;
        final double impactZ;

        Fake(int id, double x, double z, double vx, double vz, double impactX, double impactZ) {
            this.id = id;
            this.x = x;
            this.z = z;
            this.vx = vx;
            this.vz = vz;
            this.impactX = impactX;
            this.impactZ = impactZ;
        }

        @Override
        public int id() {
            return id;
        }

        @Override
        public int dimension() {
            return 0;
        }

        @Override
        public double speed() {
            return Math.sqrt(vx * vx + vz * vz);
        }

        @Override
        public double x() {
            return x;
        }

        @Override
        public double y() {
            return 90;
        }

        @Override
        public double z() {
            return z;
        }

        @Override
        public double vx() {
            return vx;
        }

        @Override
        public double vy() {
            return 0;
        }

        @Override
        public double vz() {
            return vz;
        }

        @Override
        public int tier() {
            return 2;
        }

        @Override
        public double impactX() {
            return impactX;
        }

        @Override
        public double impactZ() {
            return impactZ;
        }

        @Override
        public boolean incoming(double cx, double cz, double r) {
            double dx = impactX - cx;
            double dz = impactZ - cz;
            return dx * dx + dz * dz <= r * r;
        }
    }

    @Test
    void aRecordHasEveryListedField() {
        Map<String, Object> r = ContactRecords.record(new Fake(7, 0, 0, 2, 0, 100, 0), 100, 0, 50);
        assertEquals(
            Arrays.asList("id", "x", "y", "z", "tier", "incoming", "impactX", "impactZ", "eta"),
            Arrays.asList(
                r.keySet()
                    .toArray()));
        assertEquals(7, r.get("id"));
        assertEquals(2, r.get("tier"));
        assertEquals(50, r.get("eta"), "100 blocks at 2 blocks per tick");
    }

    @Test
    void incomingIsDecidedByThePredictedImpactPointNotTheCurrentPosition() {
        // Right on top of the base but landing far away: not incoming. Far away but landing on the base: incoming.
        Fake overhead = new Fake(1, 0, 0, 1, 0, 900, 0);
        Fake distant = new Fake(2, 800, 0, 1, 0, 10, 0);
        List<Map<String, Object>> all = ContactRecords.records(Arrays.asList(overhead, distant), 0, 0, 50, false);
        assertEquals(
            false,
            all.get(0)
                .get("incoming"));
        assertEquals(
            true,
            all.get(1)
                .get("incoming"));
        List<Map<String, Object>> inc = ContactRecords.records(Arrays.asList(overhead, distant), 0, 0, 50, true);
        assertEquals(1, inc.size());
        assertEquals(
            2,
            inc.get(0)
                .get("id"));
    }

    @Test
    void aMissileNotMovingSidewaysHasNoEta() {
        assertEquals(-1, ContactRecords.eta(new Fake(1, 0, 0, 0, 0, 5, 5)));
    }

    @Test
    void radarSignalsFireOnTransitionsOnly() {
        ContactTracker t = new ContactTracker();
        Fake far = new Fake(1, 500, 0, 1, 0, 900, 0);
        assertTrue(
            t.sample(Arrays.asList(far), 0, 0, 50)
                .isEmpty(),
            "the first sample only seeds");
        assertTrue(
            t.sample(Arrays.asList(far), 0, 0, 50)
                .isEmpty(),
            "an unchanged picture sends nothing");
        Fake second = new Fake(2, 600, 0, 1, 0, 10, 0);
        List<ContactTracker.Signal> s = t.sample(Arrays.asList(far, second), 0, 0, 50);
        assertEquals(2, s.size());
        assertEquals(
            "radar_contact",
            s.get(0)
                .name());
        assertEquals(
            "radar_incoming",
            s.get(1)
                .name());
        assertEquals(
            2,
            s.get(1)
                .id());
        assertTrue(
            t.sample(Arrays.asList(far, second), 0, 0, 50)
                .isEmpty(),
            "still incoming: no repeat");
        List<ContactTracker.Signal> lost = t.sample(Arrays.asList(far), 0, 0, 50);
        assertEquals(1, lost.size());
        assertEquals(
            "radar_lost",
            lost.get(0)
                .name());
        assertEquals(
            2,
            lost.get(0)
                .id());
    }

    @Test
    void aContactThatTurnsIncomingAnnouncesItOnce() {
        ContactTracker t = new ContactTracker();
        t.sample(Arrays.asList(new Fake(1, 500, 0, 1, 0, 900, 0)), 0, 0, 50);
        List<ContactTracker.Signal> s = t.sample(Arrays.asList(new Fake(1, 450, 0, 1, 0, 20, 0)), 0, 0, 50);
        assertEquals(1, s.size());
        assertEquals(
            "radar_incoming",
            s.get(0)
                .name());
    }
}
