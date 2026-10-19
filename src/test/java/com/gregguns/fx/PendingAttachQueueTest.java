package com.gregguns.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class PendingAttachQueueTest {

    @Test
    void doesNotReproducePFX01_aPacketThatBeatsTheEntityIsAttachedWhenTheEntityArrives() {
        // Techguns drops the request with "ent was null" and the AlienBlasterTrail never shows.
        PendingAttachQueue q = new PendingAttachQueue();
        AtomicBoolean present = new AtomicBoolean(false);
        List<String> attached = new ArrayList<>();
        q.offer("AlienBlasterTrail", 77, PendingAttachQueue.DEFAULT_WAIT_TICKS);
        q.tick(id -> present.get(), (name, id) -> attached.add(name + "#" + id));
        q.tick(id -> present.get(), (name, id) -> attached.add(name + "#" + id));
        assertEquals(0, attached.size());
        assertEquals(1, q.size());
        present.set(true);
        q.tick(id -> present.get(), (name, id) -> attached.add(name + "#" + id));
        assertEquals(java.util.Collections.singletonList("AlienBlasterTrail#77"), attached);
        assertEquals(0, q.size());
    }

    @Test
    void aRequestForAnEntityThatNeverComesExpires() {
        PendingAttachQueue q = new PendingAttachQueue();
        q.offer("X", 1, 3);
        for (int i = 0; i < 3; i++) {
            q.tick(id -> false, (name, id) -> {});
        }
        assertEquals(0, q.size());
        assertEquals(1, q.expired());
    }

    @Test
    void theQueueIsBoundedAndDropsTheOldest() {
        PendingAttachQueue q = new PendingAttachQueue();
        for (int i = 0; i < PendingAttachQueue.MAX_PENDING + 5; i++) {
            q.offer("X", i, 40);
        }
        assertEquals(PendingAttachQueue.MAX_PENDING, q.size());
        assertEquals(5, q.expired());
    }

    @Test
    void clearForgetsEverything() {
        PendingAttachQueue q = new PendingAttachQueue();
        q.offer("X", 1, 40);
        q.clear();
        assertEquals(0, q.size());
    }
}
