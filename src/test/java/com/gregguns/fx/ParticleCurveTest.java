package com.gregguns.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class ParticleCurveTest {

    private static List<CurveKey> alphas(float... pairs) {
        ParticleSystemDef.Builder b = ParticleSystemDef.builder("t");
        for (int i = 0; i < pairs.length; i += 2) {
            b.alphaKey(pairs[i], pairs[i + 1]);
        }
        return b.build()
            .alphaKeys();
    }

    @Test
    void noKeysMeansOpaqueWhite() {
        assertEquals(1F, ParticleCurve.alpha(Collections.<CurveKey>emptyList(), 0.5F));
        assertEquals(0xFFFFFF, ParticleCurve.rgb(Collections.<CurveKey>emptyList(), 0.5F));
    }

    @Test
    void reachesEachKeysValueAtItsTime() {
        // Techguns swaps the segment weights, so its curve shows the later key's value at the start of a segment.
        List<CurveKey> k = alphas(0F, 0F, 1F, 0.5F, 0F, 1F);
        assertEquals(0F, ParticleCurve.alpha(k, 0F), 1.0E-6);
        assertEquals(0.5F, ParticleCurve.alpha(k, 0.25F), 1.0E-6);
        assertEquals(1F, ParticleCurve.alpha(k, 0.5F), 1.0E-6);
        assertEquals(0.5F, ParticleCurve.alpha(k, 0.75F), 1.0E-6);
        assertEquals(0F, ParticleCurve.alpha(k, 1F), 1.0E-6);
    }

    @Test
    void keysThatDoNotCoverTheWholeLifetimeClampInsteadOfProducingNaN() {
        // Techguns: first key after time 0, last before 1, or a single key gives 0/0 = NaN (black or undefined).
        List<CurveKey> k = alphas(0.3F, 0.2F, 0.8F, 0.6F);
        assertEquals(0.3F, ParticleCurve.alpha(k, 0F), 1.0E-6);
        assertEquals(0.8F, ParticleCurve.alpha(k, 1F), 1.0E-6);
        assertFalse(Float.isNaN(ParticleCurve.alpha(alphas(0.4F, 0.5F), 0.9F)));
        assertEquals(0.4F, ParticleCurve.alpha(alphas(0.4F, 0.5F), 0.9F), 1.0E-6);
    }

    @Test
    void zeroAlphaFromTheFileIsInvisibleNotOpaque() {
        // Techguns shows "0@0, 0@1" (TeslaFlare) fully opaque because it stores 1 - alpha with swapped weights.
        assertEquals(0F, ParticleCurve.alpha(alphas(0F, 0F, 0F, 1F), 0.5F), 1.0E-6);
    }

    @Test
    void keysAreSortedByTimeEvenIfWrittenOutOfOrder() {
        List<CurveKey> k = alphas(1F, 1F, 0F, 0F);
        assertEquals(0F, ParticleCurve.alpha(k, 0F), 1.0E-6);
        assertEquals(1F, ParticleCurve.alpha(k, 1F), 1.0E-6);
    }

    @Test
    void colorInterpolatesInHsbBetweenKeys() {
        List<CurveKey> k = ParticleSystemDef.builder("t")
            .colorKey(255, 0, 0, 0F)
            .colorKey(255, 255, 0, 1F)
            .build()
            .colorKeys();
        assertEquals(0xFF0000, ParticleCurve.rgb(k, 0F));
        assertEquals(0xFFFF00, ParticleCurve.rgb(k, 1F));
        int mid = ParticleCurve.rgb(k, 0.5F);
        assertEquals(0xFF, (mid >> 16) & 0xFF);
        assertEquals(0x80, (mid >> 8) & 0xFF, 2);
    }
}
