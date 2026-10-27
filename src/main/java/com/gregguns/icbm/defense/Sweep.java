package com.gregguns.icbm.defense;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gregguns.icbm.detect.Contact;
import com.gregguns.icbm.detect.Contacts;

/**
 * One radar sweep: every missile in range, and the ones among them aimed inside the protected radius. Immutable; the
 * lists are nearest first. The sweep is clean-room (D14 replaces ICBM-Classic's chunk query and its distance-trend
 * test), so nothing here is ported. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Sweep)
public final class Sweep {

    /** The sweep of a radar that is off or has not scanned yet. */
    public static final Sweep EMPTY = new Sweep(Collections.<Contact>emptyList(), Collections.<Contact>emptyList());

    private final List<Contact> detected;
    private final List<Contact> incoming;

    private Sweep(List<Contact> detected, List<Contact> incoming) {
        this.detected = detected;
        this.incoming = incoming;
    }

    /**
     * @param inRange the contacts within range (any order)
     * @param cx      the protected center X (the radar)
     * @param cz      the protected center Z
     * @param radius  the protected radius
     * @return the sweep, both lists nearest to the center first
     */
    public static Sweep of(List<Contact> inRange, double cx, double cz, double radius) {
        List<Contact> all = new ArrayList<Contact>(inRange);
        Contacts.sortNearest(all, cx, cz);
        List<Contact> in = Contacts.incoming(all, cx, cz, radius);
        return new Sweep(Collections.unmodifiableList(all), Collections.unmodifiableList(in));
    }

    /** @return every contact in range, nearest first */
    public List<Contact> detected() {
        return detected;
    }

    /** @return the contacts aimed inside the protected radius, nearest first */
    public List<Contact> incoming() {
        return incoming;
    }
}
