package com.gregguns.icbm.component;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns polled state into signals that fire on a transition only (GGI-01-05): an unchanged state sends nothing. The
 * first sample only seeds the state, so a world that loads with a tower ready does not announce it. No OpenComputers
 * types, server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (GGI-01-05)
public final class SignalTracker {

    private boolean seeded;
    private boolean ready;
    private int pulses;

    /**
     * Compares the tower now with the last sample.
     *
     * @param nowReady  whether the tower can fire
     * @param nowPulses how many pulses it has fired
     * @return the signal names to send: {@code emp_fired} once for a new pulse, {@code emp_ready} when it becomes
     *         ready; empty when nothing changed
     */
    public List<String> sample(boolean nowReady, int nowPulses) {
        List<String> out = new ArrayList<>(2);
        if (seeded) {
            if (nowPulses > pulses) {
                out.add("emp_fired");
            }
            if (nowReady && !ready) {
                out.add("emp_ready");
            }
        }
        seeded = true;
        ready = nowReady;
        pulses = nowPulses;
        return out;
    }
}
