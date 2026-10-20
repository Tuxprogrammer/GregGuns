package com.gregguns.worldgen;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The pure record of every plan decision in one dimension. "The first populate of any overlapping window decides and
 * records; later windows obey the record" (PLAN-M8 Q3) is {@link #decideOnce}: the supplier, which measures terrain and
 * biome in the caller's own window, runs only when no decision exists.
 *
 * <p>
 * Not thread-safe: world generation runs on the server thread. No Minecraft types, so it is unit tested directly.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (GGS-11-10); PLAN-M8 Q3
public final class PlanLedger {

    private final Map<String, Decision> decisions = new LinkedHashMap<String, Decision>();
    private int revision;

    /**
     * Returns the recorded decision for the plan, or runs {@code decider} once, records its result and returns it. If
     * the supplier throws, nothing is recorded and the next window may try again.
     *
     * @param planKey see {@link PlanKey}
     * @param decider measures and decides; never run when a decision exists
     */
    public Decision decideOnce(String planKey, Supplier<Decision> decider) {
        Decision known = decisions.get(planKey);
        if (known != null) {
            return known;
        }
        Decision made = decider.get();
        if (made == null) {
            throw new IllegalStateException("decider returned null for " + planKey);
        }
        decisions.put(planKey, made);
        revision++;
        return made;
    }

    /** The recorded decision, or null if no window has decided this plan yet. */
    public Decision get(String planKey) {
        return decisions.get(planKey);
    }

    /**
     * Records a camp flag position on a placed plan. Ignored (false) when the plan is undecided or rejected, or the
     * position is already recorded.
     */
    public boolean addFlag(String planKey, FlagPos pos) {
        Decision d = decisions.get(planKey);
        if (d == null) {
            return false;
        }
        Decision next = d.withFlag(pos);
        if (next == d) {
            return false;
        }
        decisions.put(planKey, next);
        revision++;
        return true;
    }

    /** Restores a decision read from disk, replacing any current one. */
    void restore(String planKey, Decision decision) {
        decisions.put(planKey, decision);
    }

    public int size() {
        return decisions.size();
    }

    /** Counts changes since construction; the saved-data wrapper compares it to know when to mark itself dirty. */
    public int revision() {
        return revision;
    }

    /** A read-only view in insertion order. */
    public Map<String, Decision> all() {
        return Collections.unmodifiableMap(decisions);
    }
}
