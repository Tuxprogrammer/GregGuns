package com.gregguns.icbm.missile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import com.gregguns.GregGuns;

/**
 * The chunk ticket of one flying missile (D25): a single entity ticket that forces at most two chunks, the one the
 * missile is in and the next on its heading, and drops the one behind it. Released on impact and on timeout; a ticket
 * the
 * world loads from disk is adopted by its live in-flight missile and released otherwise. Chunks are compared by value
 * (ICBM-03-32). Server side.
 *
 * <p>
 * Source: Forge 1.7.10-10.13.4.1614 {@code net.minecraftforge.common.ForgeChunkManager}: {@code requestTicket} returns
 * null past the mod's ticket limit (default 200), {@code forceChunk} pins and {@code unforceChunk} unpins one chunk of
 * a ticket, and an entity ticket is bound with {@code bindEntity}.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/chunk-loading.md
public final class MissileTickets {

    private Ticket ticket;
    private boolean warned;

    /**
     * Brings the held chunks to the wanted ones: forces new, unforces stale, and loads the new ones now.
     *
     * @param owner  the missile entity
     * @param wanted from {@link ChunkWindow#wanted}, at most two
     * @return true while the missile holds a ticket (false if Forge refused one)
     */
    public boolean update(Entity owner, int[][] wanted) {
        World world = owner.worldObj;
        if (ticket == null) {
            ticket = ForgeChunkManager.requestTicket(GregGuns.instance, world, ForgeChunkManager.Type.ENTITY);
            if (ticket == null) {
                if (!warned) {
                    warned = true;
                    GregGuns.LOG.warn("No chunk ticket left for a missile at {}, {}", owner.posX, owner.posZ);
                }
                return false;
            }
            ticket.bindEntity(owner);
        }
        List<ChunkCoordIntPair> held = new ArrayList<>(ticket.getChunkList());
        for (ChunkCoordIntPair c : held) {
            boolean keep = false;
            for (int[] w : wanted) {
                keep |= c.chunkXPos == w[0] && c.chunkZPos == w[1];
            }
            if (!keep) {
                ForgeChunkManager.unforceChunk(ticket, c);
            }
        }
        for (int[] w : wanted) {
            ChunkCoordIntPair pair = new ChunkCoordIntPair(w[0], w[1]);
            if (!ticket.getChunkList()
                .contains(pair)) {
                ForgeChunkManager.forceChunk(ticket, pair);
            }
            // forceChunk only pins. loadChunk loads (or generates) the chunk now so the missile can see it;
            // getChunkFromChunkCoords is not enough, because it can hand back an empty chunk without loading.
            if (world instanceof WorldServer) {
                ((WorldServer) world).theChunkProviderServer.loadChunk(w[0], w[1]);
            }
        }
        return true;
    }

    /** @return the chunks held now; 0 without a ticket. Never more than {@link ChunkWindow#MAX_CHUNKS} */
    public int heldChunks() {
        return ticket == null ? 0
            : ticket.getChunkList()
                .size();
    }

    /** @return 1 while a ticket is held, else 0 */
    public int tickets() {
        return ticket == null ? 0 : 1;
    }

    /**
     * Takes over a ticket Forge re-bound to this missile when the world loaded (D25: a live flight survives a restart).
     *
     * @param loaded the ticket from the loading callback
     */
    void adopt(Ticket loaded) {
        if (ticket != null && ticket != loaded) {
            ForgeChunkManager.releaseTicket(ticket);
        }
        ticket = loaded;
    }

    /** Unforces every chunk and returns the ticket. Idempotent. */
    public void release() {
        if (ticket != null) {
            ForgeChunkManager.releaseTicket(ticket);
            ticket = null;
        }
    }

    /** Registers the loading callback: tickets loaded from disk are released, a missile asks for its own again. */
    public static void registerCallback() {
        ForgeChunkManager.setForcedChunkLoadingCallback(GregGuns.instance, MissileTickets::onTicketsLoaded);
    }

    /**
     * The world loaded these tickets from disk. Forge has already re-bound each entity ticket and loaded its chunk, so
     * a live missile in flight adopts its ticket (its next tick trims it to the wanted window); every other ticket,
     * including one whose entity is gone, is released, so none outlives the entity that wanted it (D25).
     *
     * @param tickets the loaded tickets
     * @param world   the world
     */
    static void onTicketsLoaded(List<Ticket> tickets, World world) {
        for (Ticket t : tickets) {
            Entity e = t.getEntity();
            if (e instanceof EntityMissile && !e.isDead && ((EntityMissile) e).phase() != FlightPhase.PAD) {
                ((EntityMissile) e).tickets()
                    .adopt(t);
            } else {
                ForgeChunkManager.releaseTicket(t);
            }
        }
    }

    /** @return the Forge ticket, or null; for the scenarios */
    Ticket ticket() {
        return ticket;
    }
}
