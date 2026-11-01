package com.gregguns.icbm.complex.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.complex.SurfaceVariant;
import com.gregguns.icbm.complex.gen.SiloMouth.Want;
import com.gregguns.worldgen.HeightProbe;

/** The mouth finish's rules (D319): what each cell of a silo's mouth must be, whatever grew there meanwhile. */
class SiloMouthTest {

    private static final int SX = 100, SZ = -37, PAD = 28, TOP = 70;

    private static SiloMouth mouth(SurfaceVariant v) {
        SiloMouth m = new SiloMouth(SX, SZ, PAD, v);
        for (int x = SX; x < SX + 5; x++) for (int z = SZ; z < SZ + 5; z++) m.record(x, z, TOP);
        return m;
    }

    @Test
    void theBoreIsAirFromJustOverThePadToTheSurfaceInEveryVariant() {
        for (SurfaceVariant v : SurfaceVariant.values()) {
            SiloMouth m = mouth(v);
            for (int x = SX + 1; x <= SX + 3; x++) {
                for (int z = SZ + 1; z <= SZ + 3; z++) {
                    assertEquals(Want.KEEP, m.want(x, PAD, z), "the pad stays");
                    for (int y = PAD + 1; y < TOP; y++) assertEquals(Want.AIR, m.want(x, y, z), v + " at y " + y);
                }
            }
        }
    }

    @Test
    void theCapFollowsTheVariant() {
        SiloMouth open = mouth(SurfaceVariant.OPEN);
        SiloMouth half = mouth(SurfaceVariant.HALF_OPEN);
        SiloMouth closed = mouth(SurfaceVariant.CLOSED);
        int caps = 0, holes = 0;
        for (int x = SX + 1; x <= SX + 3; x++) {
            for (int z = SZ + 1; z <= SZ + 3; z++) {
                assertEquals(Want.AIR, open.want(x, TOP, z), "open: no cap");
                assertEquals(Want.CLEAR_COVER, open.want(x, TOP + 1, z), "open: nothing grows over the bore");
                assertEquals(x <= SX + 2 ? Want.CAP : Want.AIR, half.want(x, TOP, z), "half open: the west half");
                Want c = closed.want(x, TOP, z);
                if (c == Want.CAP) {
                    caps++;
                    assertEquals(Want.CLEAR_COVER, closed.want(x, TOP + 1, z), "nothing grows on a cap");
                } else {
                    holes++;
                    assertEquals(Want.AIR, c);
                    assertEquals(Want.AIR, closed.want(x, 255, z), "a closed mouth's hole is air to build height");
                }
            }
        }
        assertEquals(7, caps, "closed: seven of nine capped (D17)");
        assertEquals(2, holes);
        assertEquals(Want.CAP, closed.want(SX + 2, TOP, SZ + 2), "the center is always capped");
    }

    @Test
    void theRingKeepsItsConcreteAndLosesItsCover() {
        SiloMouth m = mouth(SurfaceVariant.OPEN);
        assertEquals(Want.KEEP, m.want(SX, TOP, SZ));
        assertEquals(Want.KEEP, m.want(SX + 4, PAD + 20, SZ + 2));
        assertEquals(Want.CLEAR_COVER, m.want(SX, TOP + 1, SZ + 3));
        assertEquals(Want.KEEP, m.want(SX - 1, TOP + 1, SZ), "outside the footprint");
    }

    @Test
    void aColumnNotYetWrittenOrUnderWaterIsLeftAlone() {
        SiloMouth m = new SiloMouth(SX, SZ, PAD, SurfaceVariant.OPEN);
        assertFalse(m.complete());
        assertEquals(Want.KEEP, m.want(SX + 2, PAD + 5, SZ + 2));
        for (int x = SX; x < SX + 5; x++) for (int z = SZ; z < SZ + 5; z++) m.record(x, z, TOP);
        assertTrue(m.complete());
        m.record(SX + 2, SZ + 2, HeightProbe.NONE);
        assertTrue(m.complete(), "a lake column is known");
        assertEquals(Want.KEEP, m.want(SX + 2, PAD + 5, SZ + 2));
    }

    @Test
    void everyPopulateThatCanWriteOverTheFootprintIsInTheReach() {
        SiloMouth m = mouth(SurfaceVariant.OPEN);
        for (int x = SX; x < SX + 5; x++) {
            // A populate for chunk c may write c*16 .. c*16+31: chunks (x >> 4) - 1 and x >> 4 reach x.
            assertTrue(m.minChunkX() <= (x >> 4) - 1 - SiloMouth.MARGIN_CHUNKS && m.maxChunkX() >= (x >> 4));
        }
        for (int z = SZ; z < SZ + 5; z++) {
            assertTrue(m.minChunkZ() <= (z >> 4) - 1 && m.maxChunkZ() >= (z >> 4) + SiloMouth.MARGIN_CHUNKS);
        }
    }

    @Test
    void theRecordSurvivesASave() {
        SiloMouth m = mouth(SurfaceVariant.HALF_OPEN);
        m.record(SX + 1, SZ + 3, 66);
        SiloMouth back = SiloMouth.fromNbt(m.toNbt());
        assertNotNull(back);
        assertEquals(SurfaceVariant.HALF_OPEN, back.variant());
        assertEquals(PAD, back.padY());
        assertEquals(66, back.top(SX + 1, SZ + 3));
        assertEquals(TOP, back.top(SX + 4, SZ));
    }
}
