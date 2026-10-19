package com.gregguns.utility.turret;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Finds the Turret entity a base is linked to. The link is the entity's UUID, matched against live candidates that
 * already passed an {@code instanceof} check; a missing, stale or wrong-type link just means "unlinked" and the base
 * respawns. Nothing here casts an entity id (MUTL-25: Techguns cast the entity found by id to the turret class and
 * threw when the id belonged to another entity). Pure logic.
 */
// Spec: docs/techguns-spec/BUGS.md (MUTL-25, NPC-12), docs/gregguns-spec/09-machines-utility/turret.md
public final class TurretLink {

    private TurretLink() {}

    /**
     * @param id      the saved entity UUID; null means unlinked
     * @param nearby  candidates already filtered to the turret class (may be null or contain nulls)
     * @param uuidOf  reads a candidate's UUID
     * @param isAlive true for a candidate that is not dead
     * @param <T>     the candidate type
     * @return the live candidate with that UUID, or null
     */
    public static <T> T find(UUID id, Iterable<? extends T> nearby, Function<? super T, UUID> uuidOf,
        Predicate<? super T> isAlive) {
        if (id == null || nearby == null) {
            return null;
        }
        for (T candidate : nearby) {
            if (candidate != null && isAlive.test(candidate) && id.equals(uuidOf.apply(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * The death notification (NPC-12): a turret that lost its base tells no one and throws nothing.
     *
     * @param host the turret's host; may be null
     * @return true when a host was told
     */
    public static boolean notifyDeath(TurretHost host) {
        if (host == null) {
            return false;
        }
        host.onTurretDeath();
        return true;
    }
}
