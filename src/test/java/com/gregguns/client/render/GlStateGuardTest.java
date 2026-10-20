package com.gregguns.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlStateGuardTest {

    private final List<String> calls = new ArrayList<>();

    @BeforeEach
    void install() {
        GlStateGuard.useBackend(new GlStateGuard.Gl() {

            @Override
            public void pushMatrix() {
                calls.add("pushMatrix");
            }

            @Override
            public void popMatrix() {
                calls.add("popMatrix");
            }

            @Override
            public void pushAttrib(int mask) {
                calls.add("pushAttrib" + mask);
            }

            @Override
            public void popAttrib() {
                calls.add("popAttrib");
            }
        });
    }

    @AfterEach
    void restore() {
        GlStateGuard.useBackend(null);
    }

    @Test
    void balancesOnNormalExit() {
        try (GlStateGuard g = GlStateGuard.matrixAndAttrib(7)) {
            calls.add("draw");
        }
        assertEquals(java.util.Arrays.asList("pushAttrib7", "pushMatrix", "draw", "popMatrix", "popAttrib"), calls);
    }

    @Test
    void balancesWhenTheBodyThrows() {
        assertThrows(IllegalStateException.class, () -> {
            try (GlStateGuard g = GlStateGuard.matrix()) {
                throw new IllegalStateException("boom");
            }
        });
        assertEquals(java.util.Arrays.asList("pushMatrix", "popMatrix"), calls);
    }

    @Test
    void attribOnlyTouchesTheAttribStack() {
        try (GlStateGuard g = GlStateGuard.attrib(3)) {
            calls.add("draw");
        }
        assertEquals(java.util.Arrays.asList("pushAttrib3", "draw", "popAttrib"), calls);
    }

    @Test
    void closingTwiceRestoresOnce() {
        GlStateGuard g = GlStateGuard.matrix();
        g.close();
        g.close();
        assertEquals(java.util.Arrays.asList("pushMatrix", "popMatrix"), calls);
    }
}
