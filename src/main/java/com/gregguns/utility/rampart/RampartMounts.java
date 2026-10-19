package com.gregguns.utility.rampart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Gun Mount Hatches a Rampart controller currently owns. A structure check builds a new list and {@link #commit}s
 * it: hatches that left the list are released (their turret dies, GGS-08-80), and hatches that stayed or joined are
 * bound to the controller without disturbing a turret that is already standing. Server side.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Rampart: Mounted entity, GGS-08-80)
public final class RampartMounts {

    private final List<MTEGunMountHatch> bound = new ArrayList<>();

    /**
     * Replaces the owned hatches.
     *
     * @param next       the hatches the last structure check found; empty when the structure is not formed
     * @param controller the controller to bind them to
     */
    public void commit(List<MTEGunMountHatch> next, MTERampart controller) {
        for (MTEGunMountHatch old : bound) {
            if (!next.contains(old)) {
                old.release();
            }
        }
        bound.clear();
        for (MTEGunMountHatch h : next) {
            h.bind(controller);
            bound.add(h);
        }
    }

    /** Releases every hatch, which kills every mount's turret (the controller was broken or removed). */
    public void releaseAll() {
        commit(Collections.<MTEGunMountHatch>emptyList(), null);
    }

    /** @return the hatches, in bay order as the check found them; read-only */
    public List<MTEGunMountHatch> hatches() {
        return Collections.unmodifiableList(bound);
    }

    /** @return how many mounts the formed structure has; 0 when unformed */
    public int size() {
        return bound.size();
    }
}
