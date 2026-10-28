package com.gregguns.core.testing;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

import com.gregguns.GregGuns;

/**
 * Keeps a scenario's area loaded with Forge chunk tickets, so projectiles (which end at an unloaded chunk) and
 * multi-tick scenarios behave. <b>Dev environment only:</b> nothing in gameplay code calls this, and
 * {@link #registerCallback()} is reached only when {@code DevEnvironment.testHooksEnabled()}. Server side.
 *
 * <p>
 * Source: Forge 1.7.10-10.13.4.1614 {@code net.minecraftforge.common.ForgeChunkManager}.
 * {@code setForcedChunkLoadingCallback(mod, callback)} must precede {@code requestTicket(mod, world, Type.NORMAL)}
 * (which throws otherwise, and returns {@code null} past the mod's ticket limit, default 200). A ticket holds at most
 * {@code getMaxChunkListDepth()} chunks (default 25) and unforces the oldest beyond that, so a larger area uses
 * several tickets. {@code forceChunk} pins a chunk and {@code releaseTicket} unpins everything the ticket holds.
 * NORMAL tickets are saved with the world and handed to the callback on the next load; ours releases them all, since a
 * scenario never outlives its server.
 */
public final class ScenarioChunks {

    private ScenarioChunks() {}

    /** Registers the loading callback for the mod. Call once, during init, in the dev environment only. */
    public static void registerCallback() {
        ForgeChunkManager.setForcedChunkLoadingCallback(GregGuns.instance, (tickets, world) -> {
            for (ForgeChunkManager.Ticket t : tickets) {
                ForgeChunkManager.releaseTicket(t);
            }
        });
    }

    /** What {@link #hold} returns; release it when the scenario is over. */
    public static final class Hold {

        private final List<ForgeChunkManager.Ticket> tickets;

        private Hold(List<ForgeChunkManager.Ticket> tickets) {
            this.tickets = tickets;
        }

        /** Unforces every chunk and returns the tickets. Idempotent. */
        public void release() {
            for (ForgeChunkManager.Ticket t : tickets) {
                ForgeChunkManager.releaseTicket(t);
            }
            tickets.clear();
        }

        /** @return the number of tickets still held */
        public int ticketCount() {
            return tickets.size();
        }
    }

    /**
     * Loads and pins the chunks covering {@code origin +- radius}.
     *
     * @param world   the world
     * @param originX block X
     * @param originZ block Z
     * @param radius  half-size in blocks
     * @return the hold; if tickets ran out the chunks are still loaded now, and a warning is logged
     */
    public static Hold hold(World world, int originX, int originZ, int radius) {
        List<int[]> chunks = ScenarioArea.chunksCovering(originX, originZ, radius);
        List<ForgeChunkManager.Ticket> tickets = new ArrayList<>();
        int depth = 0;
        ForgeChunkManager.Ticket ticket = null;
        for (int[] c : chunks) {
            if (ticket == null || depth >= Math.max(1, ticket.getMaxChunkListDepth())) {
                ticket = ForgeChunkManager.requestTicket(GregGuns.instance, world, ForgeChunkManager.Type.NORMAL);
                if (ticket == null) {
                    GregGuns.LOG.warn("No chunk ticket left for the scenario area at {}, {}", originX, originZ);
                    break;
                }
                tickets.add(ticket);
                depth = 0;
            }
            ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(c[0], c[1]));
            depth++;
        }
        for (int[] c : chunks) {
            // Forcing keeps a chunk from unloading; loadChunk loads (or generates) it now.
            world.getChunkProvider()
                .loadChunk(c[0], c[1]);
        }
        return new Hold(tickets);
    }

    /**
     * Counts the chunk tickets the mod can still get by requesting until Forge refuses, then returns them all. A suite
     * that leaks tickets runs the mod dry (default limit 200), after which a missile's launch or a hold silently
     * pins nothing; this makes that visible. Dev environment only.
     *
     * @param world the world
     * @return how many more tickets {@code requestTicket} would hand out now
     */
    public static int freeTickets(World world) {
        List<ForgeChunkManager.Ticket> probe = new ArrayList<>();
        try {
            ForgeChunkManager.Ticket t;
            while ((t = ForgeChunkManager.requestTicket(GregGuns.instance, world, ForgeChunkManager.Type.NORMAL))
                != null) {
                probe.add(t);
            }
        } finally {
            for (ForgeChunkManager.Ticket t : probe) {
                ForgeChunkManager.releaseTicket(t);
            }
        }
        return probe.size();
    }
}
