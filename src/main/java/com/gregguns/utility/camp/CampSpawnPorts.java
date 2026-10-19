package com.gregguns.utility.camp;

/**
 * Holds the active {@link CampSpawnPort}. A registry in the sense of the project rules: M7 installs its port once at
 * startup, and the dev scenarios swap in a counting stub and put the old one back. Server side.
 */
// Spec: M6 plan decision 14
public final class CampSpawnPorts {

    private static CampSpawnPort current = CampSpawnPort.NONE;

    private CampSpawnPorts() {}

    /** @return the active port, never null */
    public static synchronized CampSpawnPort get() {
        return current;
    }

    /**
     * @param port the new port; null restores {@link CampSpawnPort#NONE}
     * @return the port it replaced, so a caller can restore it
     */
    public static synchronized CampSpawnPort install(CampSpawnPort port) {
        CampSpawnPort old = current;
        current = port == null ? CampSpawnPort.NONE : port;
        return old;
    }
}
