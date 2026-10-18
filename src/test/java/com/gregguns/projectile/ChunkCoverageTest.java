package com.gregguns.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ChunkCoverageTest {

    @Test
    void chunkOfFloorsNegativeCoordinates() {
        assertEquals(0, ChunkCoverage.chunkOf(0.0));
        assertEquals(0, ChunkCoverage.chunkOf(15.99));
        assertEquals(1, ChunkCoverage.chunkOf(16.0));
        assertEquals(-1, ChunkCoverage.chunkOf(-0.01));
        assertEquals(-1, ChunkCoverage.chunkOf(-16.0));
        assertEquals(-2, ChunkCoverage.chunkOf(-16.01));
    }

    @Test
    void segmentInsideOneLoadedChunkPasses() {
        assertTrue(ChunkCoverage.allLoaded(1, 1, 5, 5, (cx, cz) -> cx == 0 && cz == 0));
    }

    @Test
    void doesNotReproduceWGEN27_anUnloadedChunkOnThePathIsNeverAskedToLoad() {
        // The test only answers "is it loaded"; nothing here can load a chunk. A segment that crosses into an
        // unloaded chunk must be refused so the entity can end instead of reading blocks there.
        assertFalse(ChunkCoverage.allLoaded(14, 5, 18, 5, (cx, cz) -> cx == 0));
        // A diagonal clipping the corner chunk counts too.
        assertFalse(ChunkCoverage.allLoaded(15, 15, 17, 17, (cx, cz) -> cx == cz));
    }

    @Test
    void everyChunkInTheBoundingRectangleIsChecked() {
        List<String> asked = new ArrayList<>();
        ChunkCoverage.allLoaded(15, 15, 17, 17, (cx, cz) -> {
            asked.add(cx + "," + cz);
            return true;
        });
        assertEquals(4, asked.size());
        assertTrue(asked.contains("0,1") && asked.contains("1,0"));
    }

    @Test
    void negativeSegmentsAreCoveredToo() {
        assertFalse(ChunkCoverage.allLoaded(-1, 0, 1, 0, (cx, cz) -> cx >= 0));
        assertTrue(ChunkCoverage.allLoaded(-1, 0, 1, 0, (cx, cz) -> true));
    }
}
