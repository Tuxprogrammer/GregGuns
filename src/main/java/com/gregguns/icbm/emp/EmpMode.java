package com.gregguns.icbm.emp;

/** What a pulse reaches (golden-dome.md: {@code all}, {@code missiles}, {@code machines}). No Minecraft types. */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/emp-tower.md (Modes)
public enum EmpMode {

    /** Machines and missiles. */
    ALL("all", true, true),
    /** Missiles in flight only; machines are left alone. */
    MISSILES("missiles", false, true),
    /** GT and IC2 devices only; missiles are left alone. */
    MACHINES("machines", true, false);

    private final String id;
    private final boolean machines;
    private final boolean missiles;

    EmpMode(String id, boolean machines, boolean missiles) {
        this.id = id;
        this.machines = machines;
        this.missiles = missiles;
    }

    /** @return the name a computer uses */
    public String id() {
        return id;
    }

    /** @return true when machines are drained */
    public boolean hitsMachines() {
        return machines;
    }

    /** @return true when missiles in flight are rolled */
    public boolean hitsMissiles() {
        return missiles;
    }

    /** @return the next mode in the cycle, for the GUI button */
    public EmpMode next() {
        EmpMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /**
     * @param id a mode name, any case
     * @return the mode, or {@code null} when the name is not one
     */
    public static EmpMode byId(String id) {
        if (id == null) {
            return null;
        }
        for (EmpMode m : values()) {
            if (m.id.equalsIgnoreCase(id.trim())) {
                return m;
            }
        }
        return null;
    }
}
