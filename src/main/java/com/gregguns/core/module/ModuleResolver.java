package com.gregguns.core.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Decides which registered modules run. Pure logic: no Minecraft or config types, so it is unit-tested directly.
 */
// Spec: docs/ROADMAP.md (M0 scope: module toggles); CONTRIBUTING.md (Modules: disabling removes content cleanly)
public final class ModuleResolver {

    private ModuleResolver() {}

    /**
     * Resolves the set of modules to run.
     *
     * <p>
     * A module runs when it is registered, its flag is on, and every module it requires also runs. The result is
     * ordered by {@link ModuleId} declaration order, whatever order the modules were registered in, so dispatch is
     * deterministic.
     *
     * @param registered the modules that have a module class
     * @param flag       the config switch for a module id
     * @return the resolution; never {@code null}
     * @throws IllegalArgumentException if {@code registered} lists an id twice
     */
    public static ModuleResolution resolve(Collection<ModuleId> registered, Predicate<ModuleId> flag) {
        return resolve(registered, flag, mod -> true);
    }

    /**
     * Resolves the set of modules to run, also requiring each module's {@linkplain ModuleId#requiredMods() mods}.
     *
     * <p>
     * A module runs when it is registered, its flag is on, every mod it needs is loaded, and every module it requires
     * also runs. A missing mod is reported before a missing module dependency.
     *
     * @param registered the modules that have a module class
     * @param flag       the config switch for a module id
     * @param modLoaded  whether an FML mod id is loaded
     * @return the resolution; never {@code null}
     * @throws IllegalArgumentException if {@code registered} lists an id twice
     */
    public static ModuleResolution resolve(Collection<ModuleId> registered, Predicate<ModuleId> flag,
        Predicate<String> modLoaded) {
        EnumSet<ModuleId> present = EnumSet.noneOf(ModuleId.class);
        for (ModuleId id : registered) {
            if (!present.add(id)) {
                throw new IllegalArgumentException("Module registered twice: " + id);
            }
        }
        List<ModuleId> enabled = new ArrayList<>();
        EnumSet<ModuleId> running = EnumSet.noneOf(ModuleId.class);
        Map<ModuleId, ModuleResolution.SkipReason> skipped = new EnumMap<>(ModuleId.class);
        // EnumSet iterates in declaration order, and every module is declared after its requirements.
        for (ModuleId id : present) {
            if (!flag.test(id)) {
                skipped.put(id, ModuleResolution.SkipReason.DISABLED_BY_CONFIG);
            } else if (!ModPresence.missingMods(id, modLoaded)
                .isEmpty()) {
                    skipped.put(id, ModuleResolution.SkipReason.MISSING_MOD);
                } else if (!running.containsAll(Arrays.asList(id.requires()))) {
                    skipped.put(id, ModuleResolution.SkipReason.MISSING_DEPENDENCY);
                } else {
                    running.add(id);
                    enabled.add(id);
                }
        }
        return new ModuleResolution(enabled, skipped);
    }
}
