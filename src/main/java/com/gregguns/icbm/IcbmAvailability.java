package com.gregguns.icbm;

import com.gregguns.compat.ModIds;
import com.gregguns.core.module.ModPresence;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;

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

    /**
     * The real cause of an inactive module, for the reasons scenarios give when they skip.
     *
     * @return why the ICBM module is off, or "the ICBM module is on" when it is not
     */
    public static String offReason() {
        if (ModuleState.isActive(ModuleId.ICBM)) {
            return "the ICBM module is on";
        }
        return galacticraftPresent() ? "the ICBM module is off (modules.icbm is false in the config)"
            : "the ICBM module is off (Galacticraft is not installed)";
    }
}
