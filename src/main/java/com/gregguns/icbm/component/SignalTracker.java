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

    private final String firedName;
    private final String readyName;
    private boolean seeded;
    private boolean ready;
    private int pulses;

    /** A tracker for the EMP tower: {@code emp_fired} and {@code emp_ready}. */
    public SignalTracker() {
        this("emp_fired", "emp_ready");
    }

    /**
     * @param firedName the signal for a new shot
     * @param readyName the signal for becoming ready
     */
    public SignalTracker(String firedName, String readyName) {
        this.firedName = firedName;
        this.readyName = readyName;
    }

    /**
     * Compares the tower now with the last sample.
     *
     * @param nowReady  whether the tower can fire
     * @param nowPulses how many pulses it has fired
     * @return the signal names to send: the fired name once for a new pulse, the ready name when it becomes
     *         ready; empty when nothing changed
     */
    public List<String> sample(boolean nowReady, int nowPulses) {
        List<String> out = new ArrayList<>(2);
        if (seeded) {
            if (nowPulses > pulses) {
                out.add(firedName);
            }
            if (nowReady && !ready) {
                out.add(readyName);
            }
        }
        seeded = true;
        ready = nowReady;
        pulses = nowPulses;
        return out;
    }
}
