package com.gregguns.worldgen;

/**
 * Where the world's own surface structures (vanilla and Realistic World Gen villages, vanilla temples) will stand,
 * answered from their start rule rather than from blocks already written, so the answer is the same whichever chunk
 * loads first (D302). The silo complex sinks deeper under a box one of them meets. Nothing here names a Minecraft
 * structure class, so the populate pipeline stays testable; {@link VanillaStarts} is the world adapter.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Structure starts); D302
public interface StructureStarts {

    /** What will stand on an area: a short reason tag and a detail string for the log. */
    final class Blocker {

        /** {@code village} or {@code temple}. */
        public final String reason;
        /** The start chunk and the structure's box, for the log line. */
        public final String detail;
        /** The start chunk, or 0 when not known. */
        public final int chunkX, chunkZ;

        public Blocker(String reason, String detail) {
            this(reason, detail, 0, 0);
        }

        public Blocker(String reason, String detail, int chunkX, int chunkZ) {
            this.reason = reason;
            this.detail = detail == null ? "" : detail;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public String toString() {
            return reason + (detail.isEmpty() ? "" : " (" + detail + ")");
        }
    }

    /**
     * Never loads or generates a chunk.
     *
     * @return the first structure whose box (with its margin) meets the block area, inclusive bounds; or null
     */
    Blocker blocking(int minX, int minZ, int maxX, int maxZ);

    /** No structure anywhere: scenarios, unit tests and providers without vanilla structures. */
    StructureStarts NONE = (minX, minZ, maxX, maxZ) -> null;
}
