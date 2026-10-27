package com.gregguns.icbm.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gregguns.icbm.detect.ContactSource;

/**
 * Diffs what a radar sees from one sample to the next into signals (GGI-01-05): {@code radar_contact} for a new
 * missile, {@code radar_incoming} when a missile first counts as incoming, {@code radar_lost} when one is gone. Each
 * signal carries the contact id. An unchanged picture sends nothing, and the first sample only seeds. No
 * OpenComputers types, server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_radar signals)
public final class ContactTracker {

    /** One signal to send. */
    public static final class Signal {

        private final String name;
        private final int id;

        Signal(String name, int id) {
            this.name = name;
            this.id = id;
        }

        /** @return the signal name */
        public String name() {
            return name;
        }

        /** @return the contact id it is about */
        public int id() {
            return id;
        }
    }

    private boolean seeded;
    private final Map<Integer, Boolean> known = new HashMap<>();

    /**
     * @param contacts what the radar sees now
     * @param centerX  the protected center X
     * @param centerZ  the protected center Z
     * @param radius   the protected radius
     * @return the signals for what changed since the last sample
     */
    public List<Signal> sample(List<? extends ContactSource> contacts, double centerX, double centerZ, double radius) {
        List<Signal> out = new ArrayList<>();
        Map<Integer, Boolean> now = new HashMap<>();
        Set<Integer> seen = new HashSet<>();
        for (ContactSource c : contacts) {
            boolean incoming = c.incoming(centerX, centerZ, radius);
            seen.add(c.id());
            now.put(c.id(), incoming);
            if (!seeded) {
                continue;
            }
            Boolean before = known.get(c.id());
            if (before == null) {
                out.add(new Signal("radar_contact", c.id()));
            }
            if (incoming && (before == null || !before)) {
                out.add(new Signal("radar_incoming", c.id()));
            }
        }
        if (seeded) {
            for (Integer id : known.keySet()) {
                if (!seen.contains(id)) {
                    out.add(new Signal("radar_lost", id));
                }
            }
        }
        known.clear();
        known.putAll(now);
        seeded = true;
        return out;
    }
}
