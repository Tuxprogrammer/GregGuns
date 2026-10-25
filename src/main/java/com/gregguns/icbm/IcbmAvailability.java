package com.gregguns.icbm;

import com.gregguns.compat.ModIds;
import com.gregguns.core.module.ModPresence;

/**
 * Runtime probe for the ICBM module's optional mods. Never loads a Galacticraft or OpenComputers class: every reference
 * to those mods lives in classes that are only touched behind these checks (or behind {@code @Optional}), so the module
 * and the core mod run without them. Whether Galacticraft is required is decided by the module system (see
 * {@link com.gregguns.core.module.ModuleId#requiredMods()}), not here.
 */
// Spec: docs/gregguns-icbm-spec/DECISIONS.md (D9, D13)
public final class IcbmAvailability {

    private IcbmAvailability() {}

    /** @return whether Galacticraft's core mod is loaded; probed once and cached */
    public static boolean galacticraftPresent() {
        return ModPresence.isLoaded(ModIds.GALACTICRAFT_CORE);
    }

    /** @return whether OpenComputers is loaded (the optional scripting hook, D13); probed once and cached */
    public static boolean openComputersPresent() {
        return ModPresence.isLoaded(ModIds.OPEN_COMPUTERS);
    }
}
