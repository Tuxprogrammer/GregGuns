package com.gregguns.core.module;

import java.util.ArrayList;
import java.util.List;

/**
 * Compares the module switches a server runs with the ones a client runs. Pure logic, no Minecraft types.
 *
 * <p>
 * The switches are encoded as a bit mask, bit {@code i} for {@code ModuleId.values()[i]}. A client with different
 * switches cannot register the server's content (MTE ids in particular); {@code MtePlaceholders} keeps it from
 * crashing, and this class tells the player what differs.
 */
public final class ModuleMismatch {

    private ModuleMismatch() {}

    /**
     * @param enabled the modules that run
     * @return the bit mask of them
     */
    public static int mask(Iterable<ModuleId> enabled) {
        int mask = 0;
        for (ModuleId id : enabled) {
            mask |= 1 << id.ordinal();
        }
        return mask;
    }

    /** @return the largest mask a valid packet can carry */
    public static int maxMask() {
        return (1 << ModuleId.values().length) - 1;
    }

    /**
     * @param server the server's mask
     * @param client the client's mask
     * @return one line per module the two sides disagree on, empty when they agree
     */
    public static List<String> differences(int server, int client) {
        List<String> out = new ArrayList<>();
        for (ModuleId id : ModuleId.values()) {
            boolean s = (server & (1 << id.ordinal())) != 0;
            boolean c = (client & (1 << id.ordinal())) != 0;
            if (s != c) {
                out.add("modules." + id.configKey() + ": server " + onOff(s) + ", client " + onOff(c));
            }
        }
        return out;
    }

    private static String onOff(boolean value) {
        return value ? "on" : "off";
    }
}
