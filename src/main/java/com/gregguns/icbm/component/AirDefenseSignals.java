package com.gregguns.icbm.component;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the battery's shot counters into {@code sam_fired} and {@code sam_hit} signals (GGI-01-05): one per new shot or
 * hit, nothing while the counters stand still. The first sample only seeds, so a battery that loads with old counts
 * announces nothing. No OpenComputers types, server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_air_defense signals)
public final class AirDefenseSignals {

    private boolean seeded;
    private int fired;
    private int hits;

    /**
     * @param nowFired SAMs launched so far
     * @param nowHits  SAMs that hit so far
     * @return the signal names to send, one per new event
     */
    public List<String> sample(int nowFired, int nowHits) {
        List<String> out = new ArrayList<>(2);
        if (seeded) {
            for (int i = fired; i < nowFired; i++) {
                out.add("sam_fired");
            }
            for (int i = hits; i < nowHits; i++) {
                out.add("sam_hit");
            }
        }
        seeded = true;
        fired = nowFired;
        hits = nowHits;
        return out;
    }
}
