package com.gregguns.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The outcome of {@link ModuleResolver#resolve}: which modules run, in what order, and why each other one does not.
 * Immutable and free of Minecraft types.
 */
public final class ModuleResolution {

    /** Why a registered module does not run. */
    public enum SkipReason {
        /** Its {@code modules.<name>} flag is off. The normal way to disable content. */
        DISABLED_BY_CONFIG,
        /** Its flag is on, but a module it requires is off or not present. */
        MISSING_DEPENDENCY
    }

    private final List<ModuleId> enabled;
    private final Map<ModuleId, SkipReason> skipped;

    ModuleResolution(List<ModuleId> enabled, Map<ModuleId, SkipReason> skipped) {
        this.enabled = Collections.unmodifiableList(new ArrayList<>(enabled));
        this.skipped = Collections.unmodifiableMap(new EnumMap<>(skipped));
    }

    /** @return the modules to run, in dispatch order (dependencies first) */
    public List<ModuleId> enabled() {
        return enabled;
    }

    /** @return every registered module that will not run, with the reason */
    public Map<ModuleId, SkipReason> skipped() {
        return skipped;
    }

    /**
     * @param id the module to test
     * @return whether the module runs
     */
    public boolean isEnabled(ModuleId id) {
        return enabled.contains(id);
    }
}
