package com.gregguns.icbm.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.detect.ContactSource;

/** The {@code gg_radar} and {@code gg_air_defense} calls over fake tiles (GGI-01-02, 04, 05, 06). */
class RadarAndBatteryLogicTest {

    private static final class Missile implements ContactSource {

        final int id;
        final double impactX;
        final double impactZ;

        Missile(int id, double impactX, double impactZ) {
            this.id = id;
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
            return 2;
        }

        @Override
        public double x() {
            return 100;
        }

        @Override
        public double y() {
            return 150;
        }

        @Override
        public double z() {
            return 0;
        }

        @Override
        public double vx() {
            return -2;
        }

        @Override
        public double vy() {
            return 0;
        }

        @Override
        public double vz() {
            return 0;
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
            return Math.sqrt(dx * dx + dz * dz) <= r;
        }
    }

    private static final class FakeRadar implements RadarView {

        boolean online = true;
        int radius = 64;
        final List<Missile> seen = new ArrayList<>();

        @Override
        public boolean online() {
            return online;
        }

        @Override
        public int range() {
            return online ? 500 : 0;
        }

        @Override
        public int protectedRadius() {
            return radius;
        }

        @Override
        public int setProtectedRadius(int blocks) {
            radius = Math.max(16, Math.min(range(), blocks));
            return radius;
        }

        @Override
        public double centerX() {
            return 0;
        }

        @Override
        public double centerZ() {
            return 0;
        }

        @Override
        public List<? extends ContactSource> contacts() {
            return online ? seen : new ArrayList<Missile>();
        }
    }

    @Test
    void contactsCarryTheListedFieldsAndIncomingFollowsTheImpactPoint() {
        FakeRadar r = new FakeRadar();
        r.seen.add(new Missile(7, 10, 10));
        r.seen.add(new Missile(8, 400, 0));
        RadarComponentLogic c = new RadarComponentLogic(r);
        assertEquals(
            2,
            c.getContacts()
                .size());
        java.util.Map<String, Object> first = c.getContacts()
            .get(0);
        assertEquals(
            Arrays.asList("id", "x", "y", "z", "tier", "incoming", "impactX", "impactZ", "eta"),
            new ArrayList<>(first.keySet()));
        assertEquals(true, first.get("incoming"));
        assertEquals(
            false,
            c.getContacts()
                .get(1)
                .get("incoming"),
            "a miss is not incoming (D14)");
        assertEquals(
            1,
            c.getIncoming()
                .size());
        assertEquals(
            7,
            c.getIncoming()
                .get(0)
                .get("id"));
    }

    @Test
    void anOfflineRadarSeesNothingAndTheRadiusIsClampedLikeTheGui() {
        FakeRadar r = new FakeRadar();
        r.seen.add(new Missile(1, 0, 0));
        RadarComponentLogic c = new RadarComponentLogic(r);
        r.online = false;
        assertTrue(
            c.getContacts()
                .isEmpty());
        assertEquals(
            false,
            c.getInfo()
                .get("online"));
        r.online = true;
        assertEquals(500, c.setProtectedRadius(99999));
        assertEquals(16, c.setProtectedRadius(-5));
    }

    @Test
    void radarSignalsFireOnTransitionsOnly() {
        ContactTracker t = new ContactTracker();
        List<Missile> seen = new ArrayList<>();
        assertTrue(
            t.sample(seen, 0, 0, 64)
                .isEmpty());
        seen.add(new Missile(3, 5, 5));
        assertEquals(
            2,
            t.sample(seen, 0, 0, 64)
                .size(),
            "radar_contact and radar_incoming");
        assertTrue(
            t.sample(seen, 0, 0, 64)
                .isEmpty(),
            "an unchanged picture sends nothing");
        seen.clear();
        List<ContactTracker.Signal> lost = t.sample(seen, 0, 0, 64);
        assertEquals(1, lost.size());
        assertEquals(
            "radar_lost",
            lost.get(0)
                .name());
        assertEquals(
            3,
            lost.get(0)
                .id());
    }

    /** A battery with the refusals its own engagement has. */
    private static final class FakeBattery implements AirDefenseView {

        boolean linked = true;
        int sams = 2;
        boolean auto = true;
        int fired;
        int hits;
        final List<Integer> known = Arrays.asList(5);

        @Override
        public boolean linked() {
            return linked;
        }

        @Override
        public int samCount() {
            return sams;
        }

        @Override
        public int capacity() {
            return 4;
        }

        @Override
        public boolean auto() {
            return auto;
        }

        @Override
        public void setAuto(boolean on) {
            auto = on;
        }

        @Override
        public Reply fireAt(int id) {
            if (!linked) {
                return Reply.refused("no_link");
            }
            if (!known.contains(id)) {
                return Reply.refused("no_contact");
            }
            if (sams == 0) {
                return Reply.refused("no_sams");
            }
            sams--;
            fired++;
            return Reply.ok();
        }

        @Override
        public int fired() {
            return fired;
        }

        @Override
        public int hits() {
            return hits;
        }
    }

    @Test
    void fireAtGoesThroughTheBatterysChecks() {
        FakeBattery b = new FakeBattery();
        AirDefenseComponentLogic c = new AirDefenseComponentLogic(b);
        assertEquals(
            "no_contact",
            c.fireAt(9)
                .reason());
        assertTrue(
            c.fireAt(5)
                .isOk());
        assertTrue(
            c.fireAt(5)
                .isOk());
        Reply dry = c.fireAt(5);
        assertFalse(dry.isOk());
        assertEquals("no_sams", dry.reason());
        assertEquals(2, b.fired);
        b.linked = false;
        assertEquals(
            "no_link",
            c.fireAt(5)
                .reason());
        assertEquals(0, c.getSamCount());
        assertEquals(false, c.setAuto(false));
        assertEquals(
            4,
            c.getInfo()
                .get("capacity"));
    }

    @Test
    void samSignalsComeOncePerEvent() {
        AirDefenseSignals s = new AirDefenseSignals();
        assertTrue(
            s.sample(3, 1)
                .isEmpty(),
            "the first sample only seeds");
        assertTrue(
            s.sample(3, 1)
                .isEmpty());
        assertEquals(Arrays.asList("sam_fired", "sam_fired", "sam_hit"), s.sample(5, 2));
        assertTrue(
            s.sample(5, 2)
                .isEmpty());
    }
}
