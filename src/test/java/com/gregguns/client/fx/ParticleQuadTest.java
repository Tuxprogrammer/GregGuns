package com.gregguns.client.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.Tessellator;

import org.junit.jupiter.api.Test;

import com.gregguns.fx.QuadMath;

/** GGS-04-06: one quad, UV before every vertex, one color, no normal and no disableColor. */
class ParticleQuadTest {

    /** Records the calls Angelica's capture tessellator cares about. */
    private static final class Recorder extends Tessellator {

        final List<String> calls = new ArrayList<>();

        Recorder() {
            super();
        }

        @Override
        public void setTextureUV(double u, double v) {
            calls.add("uv");
        }

        @Override
        public void addVertex(double x, double y, double z) {
            calls.add("vertex");
        }

        @Override
        public void setColorRGBA(int r, int g, int b, int a) {
            calls.add("color " + r + "," + g + "," + b + "," + a);
        }

        @Override
        public void setBrightness(int value) {
            calls.add("brightness");
        }

        @Override
        public void setNormal(float x, float y, float z) {
            calls.add("normal");
        }

        @Override
        public void disableColor() {
            calls.add("disableColor");
        }
    }

    @Test
    void emitsFourVerticesEachPrecededByItsUvAndOneColor() {
        Recorder t = new Recorder();
        ParticleQuad
            .emit(t, QuadMath.corners(0, 0, 0, 0.5F, 1, 0, 0, 0, 1), QuadMath.uv(0, 1, 1, 0), 0x336699, 0.5F, 0xF000F0);
        assertEquals(
            java.util.Arrays.asList(
                "brightness",
                "color 51,102,153,128",
                "uv",
                "vertex",
                "uv",
                "vertex",
                "uv",
                "vertex",
                "uv",
                "vertex"),
            t.calls);
    }

    @Test
    void neverCallsSetNormalOrDisableColor() {
        Recorder t = new Recorder();
        ParticleQuad.emit(t, new double[12], new float[8], 0xFFFFFF, 1F, 0xF000F0);
        assertEquals(
            0,
            t.calls.stream()
                .filter(c -> c.equals("normal") || c.equals("disableColor"))
                .count());
    }
}
