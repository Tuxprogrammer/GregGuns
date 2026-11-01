package com.gregguns.core.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.Loader;

/**
 * Runtime probe for the mods a module needs. Asks Forge's {@link Loader} by mod id and caches the answer (the mod list
 * does not change after startup). The probe is injectable ({@code Predicate<String>}), so the logic that uses it is
 * unit-tested without Forge.
 */
// Spec: docs/gregguns-icbm-spec/DECISIONS.md (D9)
// Source: FML 1.7.10 cpw.mods.fml.common.Loader#isModLoaded(String): true when a mod with that id is in the mod list.
public final class ModPresence {

    private static final Map<String, Boolean> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private ModPresence() {}

    /**
     * @param modId an FML mod id
     * @return whether that mod is loaded; probed once per id and cached
     */
    public static boolean isLoaded(String modId) {
        return isLoaded(modId, Loader::isModLoaded);
    }

    /**
     * @param modId an FML mod id
     * @param probe asks the mod list; may throw when there is none
     * @return the probe's answer, cached; false when the probe throws, which is not cached (D344)
     */
    static boolean isLoaded(String modId, Predicate<String> probe) {
        Boolean known = CACHE.get(modId);
        if (known != null) {
            return known;
        }
        boolean loaded;
        try {
            loaded = probe.test(modId);
        } catch (RuntimeException e) {
            // Outside a running Forge (a plain unit test) the Loader has no mod list, so nothing counts as loaded. The
            // answer is not cached and is logged, so a failed probe in a real game neither sticks nor hides as a
            // missing mod.
            if (WARNED.add(modId)) {
                GregGuns.LOG.warn("Could not ask Forge whether {} is loaded ({}); treating it as absent", modId, e);
            }
            return false;
        }
        CACHE.put(modId, loaded);
        return loaded;
    }

    /**
     * @param id        a module
     * @param modLoaded the probe for a mod id
     * @return the mods {@code id} needs that are not loaded, in declaration order; empty when it has everything
     */
    public static List<String> missingMods(ModuleId id, Predicate<String> modLoaded) {
        List<String> missing = new ArrayList<>();
        for (String mod : id.requiredMods()) {
            if (!modLoaded.test(mod)) {
                missing.add(mod);
            }
        }
        return missing;
    }
}
