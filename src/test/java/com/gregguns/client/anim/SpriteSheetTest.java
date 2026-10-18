package com.gregguns.client.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SpriteSheetTest {

    @Test
    void framesFollowProgress() {
        SpriteSheet s = new SpriteSheet(4, 4, 8);
        assertEquals(0, s.frameAt(0.0));
        assertEquals(0, s.frameAt(0.124));
        assertEquals(1, s.frameAt(0.125));
        assertEquals(7, s.frameAt(0.999));
        assertEquals(-1, s.frameAt(1.0));
        assertEquals(-1, s.frameAt(-0.1));
    }

    @Test
    void framesReadLeftToRightThenTopToBottom() {
        SpriteSheet s = new SpriteSheet(4, 4, 8);
        assertEquals(0.0, s.u0(0), 1e-9);
        assertEquals(0.25, s.u1(0), 1e-9);
        assertEquals(0.0, s.v0(3), 1e-9);
        assertEquals(0.75, s.u0(3), 1e-9);
        assertEquals(0.0, s.u0(4), 1e-9);
        assertEquals(0.25, s.v0(4), 1e-9);
        assertEquals(0.5, s.v1(4), 1e-9);
    }

    @Test
    void rejectsBadGrids() {
        assertThrows(IllegalArgumentException.class, () -> new SpriteSheet(2, 2, 5));
        assertThrows(IllegalArgumentException.class, () -> new SpriteSheet(0, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> new SpriteSheet(2, 2, 0));
    }
}
