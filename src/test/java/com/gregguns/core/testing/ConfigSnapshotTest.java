package com.gregguns.core.testing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.gregguns.core.config.GregGunsConfig;
import com.gregguns.core.config.IcbmConfig;

class ConfigSnapshotTest {

    /** A stand-in config class: a primitive, a string, an array and a constant. */
    static final class Sample {

        static final int CONSTANT = 7;
        static boolean flag = true;
        static int count = 3;
        static String name = "a";
        static int[] weights = { 1, 2, 3 };
        static String[] names = { "x", "y" };
    }

    @AfterEach
    void reset() {
        Sample.flag = true;
        Sample.count = 3;
        Sample.name = "a";
        Sample.weights = new int[] { 1, 2, 3 };
        Sample.names = new String[] { "x", "y" };
    }

    private static ConfigSnapshot ofSample() {
        return ConfigSnapshot.of(Collections.<Class<?>>singletonList(Sample.class));
    }

    @Test
    void restoreUndoesEveryKindOfChange() {
        ConfigSnapshot snap = ofSample();
        Sample.flag = false;
        Sample.count = 99;
        Sample.name = null;
        Sample.weights = new int[] { 9 };
        Sample.names[0] = "edited in place";
        List<String> changed = snap.restore();
        assertTrue(Sample.flag);
        assertEquals(3, Sample.count);
        assertEquals("a", Sample.name);
        assertArrayEquals(new int[] { 1, 2, 3 }, Sample.weights);
        assertArrayEquals(new String[] { "x", "y" }, Sample.names);
        assertEquals(5, changed.size(), "every changed field is reported: " + changed);
    }

    @Test
    void restoringUntouchedConfigReportsNothingAndConstantsAreIgnored() {
        ConfigSnapshot snap = ofSample();
        assertEquals(5, snap.size(), "the five mutable fields, not the constant");
        assertTrue(
            snap.restore()
                .isEmpty());
    }

    @Test
    void anInPlaceEditOfALiveArrayDoesNotAlterTheSnapshot() {
        ConfigSnapshot snap = ofSample();
        Sample.weights[1] = 50;
        snap.restore();
        // the restored array is a fresh copy, so editing it again and restoring once more still returns the original
        Sample.weights[2] = 60;
        snap.restore();
        assertArrayEquals(new int[] { 1, 2, 3 }, Sample.weights);
    }

    @Test
    void theRealSnapshotCoversTheConfigClassesAndRestoresAnIcbmChange() {
        ConfigSnapshot snap = ConfigSnapshot.ofAll();
        assertTrue(snap.size() > 100, "all " + GregGunsConfig.CLASSES.size() + " config classes: " + snap.size());
        int chance = IcbmConfig.siloComplexChancePercent;
        String[] dims = IcbmConfig.siloComplexDimensions.clone();
        IcbmConfig.siloComplexChancePercent = chance + 1;
        IcbmConfig.siloComplexDimensions = new String[] { "leaked" };
        List<String> changed = snap.restore();
        assertEquals(chance, IcbmConfig.siloComplexChancePercent);
        assertTrue(Arrays.equals(dims, IcbmConfig.siloComplexDimensions));
        assertTrue(changed.contains("IcbmConfig.siloComplexChancePercent"), changed.toString());
    }
}
