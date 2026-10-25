package com.gregguns.icbm;

import com.gregguns.core.config.ModulesConfig;

import cpw.mods.fml.common.Loader;

/**
 * Runtime probe for the ICBM module's optional mods. Asks Forge's {@link Loader} by mod id and never loads a
 * Galacticraft or OpenComputers class: every reference to those mods lives in classes that are only touched behind
 * these checks (or behind {@code @Optional}), so the module and the core mod run without them.
 */
// Spec: docs/gregguns-icbm-spec/DECISIONS.md (D9, D37)
// Source: FML 1.7.10 cpw.mods.fml.common.Loader#isModLoaded(String): true when a mod with that id is in the mod list.
public final class IcbmAvailability {

    /** Galacticraft's core mod id in the GTNH pack (GalacticraftCore, GalacticraftMars; MOD-INDEX.md). */
    public static final String GALACTICRAFT_CORE = "GalacticraftCore";
    /** OpenComputers' mod id. */
    public static final String OPEN_COMPUTERS = "OpenComputers";

    private static volatile Boolean galacticraft;

    private IcbmAvailability() {}

    /** @return whether Galacticraft's core mod is loaded; probed once and cached */
    public static boolean galacticraftPresent() {
        Boolean cached = galacticraft;
        if (cached == null) {
            cached = Loader.isModLoaded(GALACTICRAFT_CORE);
            galacticraft = cached;
        }
        return cached;
    }

    /** @return whether OpenComputers is loaded (the optional scripting hook, D37) */
    public static boolean openComputersPresent() {
        return Loader.isModLoaded(OPEN_COMPUTERS);
    }

    /** @return the gate's verdict for the current config and mod list */
    public static IcbmGate.Verdict verdict() {
        return IcbmGate.decide(ModulesConfig.icbm, galacticraftPresent());
    }
}
