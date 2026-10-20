package com.gregguns.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DevEnvironmentTest {

    private static boolean withProperty(String value) {
        String old = System.getProperty(DevEnvironment.TEST_HOOKS_PROPERTY);
        try {
            if (value == null) {
                System.clearProperty(DevEnvironment.TEST_HOOKS_PROPERTY);
            } else {
                System.setProperty(DevEnvironment.TEST_HOOKS_PROPERTY, value);
            }
            return DevEnvironment.testHooksEnabled();
        } finally {
            if (old == null) {
                System.clearProperty(DevEnvironment.TEST_HOOKS_PROPERTY);
            } else {
                System.setProperty(DevEnvironment.TEST_HOOKS_PROPERTY, old);
            }
        }
    }

    @Test
    void propertyNameIsStable() {
        assertEquals("gregguns.testHooks", DevEnvironment.TEST_HOOKS_PROPERTY);
    }

    @Test
    void offByDefaultOutsideTheWorkspace() {
        assertFalse(DevEnvironment.isActive(), "unit tests run without a launch blackboard");
        assertFalse(withProperty(null));
    }

    @Test
    void onlyTheExactTrueValueOptsIn() {
        assertTrue(withProperty("true"));
        assertFalse(withProperty("false"));
        assertFalse(withProperty("yes"));
        assertFalse(withProperty(""));
    }
}
