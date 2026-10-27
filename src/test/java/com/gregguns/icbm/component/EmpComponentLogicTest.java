package com.gregguns.icbm.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.emp.EmpMode;
import com.gregguns.icbm.emp.EmpRules;

/** The {@code gg_emp_tower} calls over a fake tower (GGI-01-02, 04, 05). */
class EmpComponentLogicTest {

    /** A tower that follows the same shared gate and clamp as the real one. */
    private static final class FakeTower implements EmpTowerView {

        int radius = 60;
        EmpMode mode = EmpMode.ALL;
        int cooldown;
        long stored = 1_000_000L;
        int pulses;
        boolean moduleOn = true;

        @Override
        public int radius() {
            return radius;
        }

        @Override
        public void setRadius(int blocks) {
            radius = EmpRules.clampRadius(blocks, 100);
        }

        @Override
        public EmpMode mode() {
            return mode;
        }

        @Override
        public void setMode(EmpMode m) {
            mode = m;
        }

        @Override
        public int cooldownLeft() {
            return cooldown;
        }

        @Override
        public long cost() {
            return EmpRules.euCost(radius);
        }

        @Override
        public long stored() {
            return stored;
        }

        @Override
        public int pulses() {
            return pulses;
        }

        @Override
        public EmpRules.Gate gate() {
            return EmpRules.gate(moduleOn, cooldown > 0, stored, cost());
        }

        @Override
        public EmpRules.Gate fire() {
            EmpRules.Gate g = gate();
            if (g.isGood()) {
                stored -= cost();
                cooldown = EmpRules.COOLDOWN_TICKS;
                pulses++;
            }
            return g;
        }
    }

    @Test
    void fireGoesThroughTheSameGateAsTheButton() {
        FakeTower t = new FakeTower();
        EmpComponentLogic c = new EmpComponentLogic(t);
        assertTrue(c.isReady());
        assertTrue(
            c.fire()
                .isOk());
        assertEquals(1, t.pulses);
        Reply again = c.fire();
        assertFalse(again.isOk());
        assertEquals("cooling", again.reason());
        assertEquals(1, t.pulses, "a refused call fires nothing");
    }

    @Test
    void everyRefusalHasItsOwnReasonAndFiresNothing() {
        FakeTower t = new FakeTower();
        t.stored = 0;
        EmpComponentLogic c = new EmpComponentLogic(t);
        assertEquals(
            "no_power",
            c.fire()
                .reason());
        t.stored = 10_000_000L;
        t.moduleOn = false;
        assertEquals(
            "module_off",
            c.fire()
                .reason());
        assertEquals(0, t.pulses);
        assertEquals(10_000_000L, t.stored, "a refusal costs nothing");
    }

    @Test
    void doesNotReproduceICBM0511_theComputerCannotSetAnOutOfRangeRadius() {
        FakeTower t = new FakeTower();
        EmpComponentLogic c = new EmpComponentLogic(t);
        assertEquals(EmpRules.MIN_RADIUS, c.setRadius(-100));
        assertEquals(100, c.setRadius(9999));
        assertEquals(37, c.setRadius(37));
        assertEquals(37, c.getRadius());
    }

    @Test
    void modesParseAndBadNamesAreRefused() {
        FakeTower t = new FakeTower();
        EmpComponentLogic c = new EmpComponentLogic(t);
        assertEquals("all", c.getMode());
        assertTrue(
            c.setMode("Missiles")
                .isOk());
        assertEquals("missiles", c.getMode());
        Reply bad = c.setMode("everything");
        assertFalse(bad.isOk());
        assertEquals("bad_mode", bad.reason());
        assertEquals("missiles", c.getMode(), "a bad name changes nothing");
    }

    @Test
    void cooldownIsReported() {
        FakeTower t = new FakeTower();
        t.cooldown = 120;
        assertEquals(120, new EmpComponentLogic(t).getCooldown());
        assertFalse(new EmpComponentLogic(t).isReady());
    }

    @Test
    void infoCarriesTheConfigDerivedConstants() {
        java.util.Map<String, Object> info = new EmpComponentLogic(new FakeTower()).info();
        assertEquals(EmpRules.MIN_RADIUS, info.get("minRadius"));
        assertEquals(EmpRules.COOLDOWN_TICKS, info.get("cooldownTicks"));
        assertTrue(info.containsKey("maxRadius") && info.containsKey("maxDevices") && info.containsKey("euPerArea"));
    }

    @Test
    void signalsFireOnTransitionsOnly() {
        SignalTracker s = new SignalTracker();
        assertTrue(
            s.sample(true, 0)
                .isEmpty(),
            "the first sample only seeds");
        assertTrue(
            s.sample(true, 0)
                .isEmpty(),
            "an unchanged state sends nothing");
        assertEquals(java.util.Arrays.asList("emp_fired"), s.sample(false, 1));
        assertTrue(
            s.sample(false, 1)
                .isEmpty());
        assertEquals(java.util.Arrays.asList("emp_ready"), s.sample(true, 1));
        assertTrue(
            s.sample(true, 1)
                .isEmpty());
        assertEquals(java.util.Arrays.asList("emp_fired"), s.sample(true, 2));
        s.sample(false, 2);
        assertEquals(java.util.Arrays.asList("emp_fired", "emp_ready"), s.sample(true, 3));
    }
}
