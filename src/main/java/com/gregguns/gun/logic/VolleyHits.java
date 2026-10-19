package com.gregguns.gun.logic;

import java.util.HashSet;
import java.util.Set;

/**
 * The shared hit list of one volley: every projectile of a ring volley carries the same instance, so an entity struck
 * by several of them is hurt once. Pure; entities are identified by their integer entity id. Server thread only.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.13 SonicShotgunProjectile, shared entitiesHit list)
public final class VolleyHits {

    private final Set<Integer> hit = new HashSet<>();

    /**
     * Records a hit.
     *
     * @param entityId the struck entity's id
     * @return true if this is the volley's first hit on the entity (apply damage), false if already hit
     */
    public boolean firstHit(int entityId) {
        return hit.add(entityId);
    }

    /** @return how many distinct entities the volley has hit */
    public int count() {
        return hit.size();
    }
}
