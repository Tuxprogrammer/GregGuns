package com.gregguns.worldgen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

/**
 * Test stub standing in for a world during a populate call. Records every write and every chunk request so a test
 * can assert GGS-11-08 (all writes inside the window) and GGS-11-09 (no chunk other than the populating one is
 * requested). Reused by the G12 cascade harness.
 */
public final class RecordingWorld implements WindowedSink.Target {

    /** One recorded write. {@code kind} is "block", "tile" or "entity". */
    public static final class Write {

        public final String kind;
        public final int x;
        public final int y;
        public final int z;

        Write(String kind, int x, int y, int z) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** One recorded chunk request: {@code method} is loadChunk, provideChunk or getChunkFromChunkCoords. */
    public static final class ChunkRequest {

        public final String method;
        public final int chunkX;
        public final int chunkZ;

        ChunkRequest(String method, int chunkX, int chunkZ) {
            this.method = method;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }

    private final PopulateWindow window;
    public final List<Write> writes = new ArrayList<>();
    public final List<ChunkRequest> chunkRequests = new ArrayList<>();

    /** The world is being populated at chunk {@code (cx, cz)}. */
    public RecordingWorld(int cx, int cz) {
        this.window = new PopulateWindow(cx, cz);
    }

    public PopulateWindow window() {
        return window;
    }

    @Override
    public void setBlock(int x, int y, int z, Block block, int meta) {
        writes.add(new Write("block", x, y, z));
    }

    @Override
    public void setTile(int x, int y, int z, TileEntity tile) {
        writes.add(new Write("tile", x, y, z));
    }

    @Override
    public void spawn(Entity entity, double x, double y, double z) {
        writes.add(new Write("entity", (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
    }

    public void loadChunk(int cx, int cz) {
        chunkRequests.add(new ChunkRequest("loadChunk", cx, cz));
    }

    public void provideChunk(int cx, int cz) {
        chunkRequests.add(new ChunkRequest("provideChunk", cx, cz));
    }

    public void getChunkFromChunkCoords(int cx, int cz) {
        chunkRequests.add(new ChunkRequest("getChunkFromChunkCoords", cx, cz));
    }

    /** Writes whose column lies outside the populate window. Empty when GGS-11-08 holds. */
    public List<Write> writesOutsideWindow() {
        List<Write> out = new ArrayList<>();
        for (Write w : writes) {
            if (!window.contains(w.x, w.z)) {
                out.add(w);
            }
        }
        return out;
    }

    /** Chunk requests for any chunk other than the populating one. Empty when GGS-11-09 holds. */
    public List<ChunkRequest> foreignChunkRequests() {
        List<ChunkRequest> out = new ArrayList<>();
        for (ChunkRequest r : chunkRequests) {
            if (r.chunkX != window.chunkX() || r.chunkZ != window.chunkZ()) {
                out.add(r);
            }
        }
        return out;
    }
}
