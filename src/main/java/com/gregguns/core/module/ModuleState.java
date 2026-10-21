package com.gregguns.core.module;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.gregguns.core.config.ModuleFlags;

/**
 * Which modules are active right now. Module flags never change what GregGuns registers (every block, item, entity,
 * tile entity and machine always exists, see {@code ModuleManager}); they only switch behavior: recipes, spawning,
 * worldgen, machine operation, NEI and creative visibility, and the use of existing items. This class answers "is the
 * behavior on?" for every one of those.
 *
 * <p>
 * The answer comes from one of two places. In single player and on a dedicated server it is the local config:
 * {@code modules.<name>} is on and every module it {@linkplain ModuleId#requires() requires} is active. A client
 * connected to a remote server uses the server's answer instead, received at login in {@link PacketModuleSync} and
 * dropped again on disconnect, so a client with other flags behaves as the server says. Pure logic apart from the
 * config
 * read, so it is unit-tested directly.
 */
// Spec: docs/gregguns-spec/01-core/config-gtnhlib.md (module switches, GGS-01-03)
public final class ModuleState {

    /** Bit mask of the server's active modules while connected to a remote server; {@code -1} when there is none. */
    private static volatile int serverMask = -1;

    private ModuleState() {}

    /**
     * @param id a module
     * @return whether its behavior is on (the server's value while connected to a remote server)
     */
    public static boolean isActive(ModuleId id) {
        int server = serverMask;
        if (server >= 0) {
            return (server & (1 << id.ordinal())) != 0;
        }
        return isActiveLocally(id, ModuleFlags::isEnabled);
    }

    /**
     * @param id   a module
     * @param flag the config switch of a module
     * @return whether the module's flag is on and every module it requires is active
     */
    public static boolean isActiveLocally(ModuleId id, Predicate<ModuleId> flag) {
        if (!flag.test(id)) {
            return false;
        }
        for (ModuleId required : id.requires()) {
            if (!isActiveLocally(required, flag)) {
                return false;
            }
        }
        return true;
    }

    /** @return the bit mask of the modules active on this side's own config (ignores the server's values) */
    public static int localMask() {
        return maskOf(ModuleFlags::isEnabled);
    }

    /**
     * @param flag the config switch of a module
     * @return the bit mask of the modules active under {@code flag}, bit {@code i} for {@code ModuleId.values()[i]}
     */
    public static int maskOf(Predicate<ModuleId> flag) {
        int mask = 0;
        for (ModuleId id : ModuleId.values()) {
            if (isActiveLocally(id, flag)) {
                mask |= 1 << id.ordinal();
            }
        }
        return mask;
    }

    /** @return the bit mask of the modules active for behavior right now */
    public static int currentMask() {
        int server = serverMask;
        return server >= 0 ? server : localMask();
    }

    /** @return the largest mask a valid packet can carry */
    public static int maxMask() {
        return (1 << ModuleId.values().length) - 1;
    }

    /**
     * Switches this side to the server's modules. Called on the client when the login packet arrives.
     *
     * @param mask the server's active-module mask
     * @throws IllegalArgumentException if the mask has bits that name no module
     */
    public static void applyServerMask(int mask) {
        if (mask < 0 || mask > maxMask()) {
            throw new IllegalArgumentException("Not a module mask: " + mask);
        }
        serverMask = mask;
    }

    /** Goes back to the local config. Called on the client when it leaves a server, and by tests. */
    public static void clearServerMask() {
        serverMask = -1;
    }

    /** @return whether the server's modules are in force instead of the local config */
    public static boolean usingServerValues() {
        return serverMask >= 0;
    }

    /**
     * @param mask a module mask
     * @return the modules in it, in {@link ModuleId} declaration order
     */
    public static List<ModuleId> idsOf(int mask) {
        List<ModuleId> out = new ArrayList<>();
        for (ModuleId id : ModuleId.values()) {
            if ((mask & (1 << id.ordinal())) != 0) {
                out.add(id);
            }
        }
        return out;
    }

    /**
     * @param id a module
     * @return the tooltip line for an item of a module that is off
     */
    public static String disabledLine(ModuleId id) {
        return "Disabled by the server's GregGuns config (modules." + id.configKey() + ")";
    }
}
