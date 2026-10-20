package com.gregguns.projectile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The server's table of live {@link MovingBeam}s, one per shooter. A beam exists only while its shooter keeps
 * refreshing it; expired ones are dropped on the next refresh, so the table stays as small as the number of shooters
 * firing now. Server thread only, no Minecraft types.
 */
// Spec: docs/techguns-spec/02-guns/per-gun-behavior.md (Nuclear Death Ray: BEAM_LIFETIME = 10)
public final class MovingBeams {

    private static final Map<UUID, MovingBeam> LIVE = new HashMap<>();

    private MovingBeams() {}

    /**
     * Refreshes the shooter's beam, creating it when none is alive.
     *
     * @param shooter the shooter's unique id
     * @param now     the world tick
     * @return what this tick should do
     */
    public static MovingBeam.Step refresh(UUID shooter, long now) {
        for (Iterator<MovingBeam> it = LIVE.values()
            .iterator(); it.hasNext();) {
            if (!it.next()
                .isActive(now)) {
                it.remove();
            }
        }
        return LIVE.computeIfAbsent(shooter, id -> new MovingBeam())
            .refresh(now);
    }

    /** @return how many beams are held, for tests and the dev command */
    public static int size() {
        return LIVE.size();
    }

    /** Forgets every beam. For tests and server shutdown. */
    public static void clear() {
        LIVE.clear();
    }
}
