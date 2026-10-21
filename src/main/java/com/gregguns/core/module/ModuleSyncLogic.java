package com.gregguns.core.module;

import java.util.List;

/**
 * Pure logic of the login sync: what the server sends and how the client applies it. Kept apart from the packet so it
 * is
 * unit-tested without Minecraft.
 */
// Spec: docs/gregguns-spec/01-core/config-gtnhlib.md (module switches, GGS-01-03)
public final class ModuleSyncLogic {

    private ModuleSyncLogic() {}

    /** @return the mask the server sends: the modules active on its own config, dependencies resolved */
    public static int serverMask() {
        return ModuleState.localMask();
    }

    /**
     * Applies a received mask.
     *
     * @param serverMask the server's mask
     * @param localMask  this client's own mask
     * @return the modules the two sides disagree on, as {@link ModuleMismatch#differences} lines (informational only:
     *         the server's values now apply)
     */
    public static List<String> apply(int serverMask, int localMask) {
        ModuleState.applyServerMask(serverMask);
        return ModuleMismatch.differences(serverMask, localMask);
    }
}
