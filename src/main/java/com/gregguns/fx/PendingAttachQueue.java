package com.gregguns.fx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Holds "attach this FX to that entity" requests whose entity the client has not received yet, and retries them each
 * tick until the entity appears or the wait runs out. Fixes PFX-01: Techguns' packet could arrive before the entity
 * spawn packet and was dropped with a "ent was null" message, so the AlienBlasterTrail never showed. Pure.
 *
 * <p>
 * Client thread only.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (1: PacketSpawnParticleOnEntity); Fixes PFX-01
public final class PendingAttachQueue {

    /** How long a request waits for its entity, ticks (two seconds). */
    public static final int DEFAULT_WAIT_TICKS = 40;
    /** The most requests held at once; the oldest is dropped beyond this. */
    public static final int MAX_PENDING = 256;

    /** Receives a request whose entity now exists. */
    public interface Attach {

        /**
         * @param name     the FX name
         * @param entityId the entity's id
         */
        void attach(String name, int entityId);
    }

    private static final class Entry {

        final String name;
        final int entityId;
        int ticksLeft;

        Entry(String name, int entityId, int ticksLeft) {
            this.name = name;
            this.entityId = entityId;
            this.ticksLeft = ticksLeft;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private int expired;

    /**
     * Queues a request.
     *
     * @param name      the FX name
     * @param entityId  the entity's id
     * @param waitTicks how long to wait for the entity
     */
    public void offer(String name, int entityId, int waitTicks) {
        if (entries.size() >= MAX_PENDING) {
            entries.remove(0);
            expired++;
        }
        entries.add(new Entry(name, entityId, waitTicks));
    }

    /**
     * Retries every request once and drops the ones that ran out of time.
     *
     * @param present true when the client now knows that entity id
     * @param attach  called for each request whose entity is present; the request is then removed
     */
    public void tick(IntPredicate present, Attach attach) {
        Iterator<Entry> it = entries.iterator();
        List<Entry> ready = new ArrayList<>();
        while (it.hasNext()) {
            Entry e = it.next();
            if (present.test(e.entityId)) {
                ready.add(e);
                it.remove();
            } else if (--e.ticksLeft <= 0) {
                it.remove();
                expired++;
            }
        }
        for (Entry e : ready) {
            attach.attach(e.name, e.entityId);
        }
    }

    /** Forgets every request, for example when the world changes. */
    public void clear() {
        entries.clear();
    }

    /** @return requests still waiting */
    public int size() {
        return entries.size();
    }

    /** @return requests dropped because the entity never came or the queue was full */
    public int expired() {
        return expired;
    }
}
