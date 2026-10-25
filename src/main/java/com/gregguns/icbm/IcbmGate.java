package com.gregguns.icbm;

/**
 * The decision whether the ICBM module runs, as pure logic with no Minecraft types so it is unit-tested directly.
 *
 * <p>
 * The module needs its {@code modules.icbm} flag on and Galacticraft loaded (D9). With the flag on and Galacticraft
 * absent it turns itself off, which the module reports with one log line (see {@link IcbmAvailability}); the core mod
 * is unaffected.
 */
// Spec: docs/gregguns-icbm-spec/DECISIONS.md (D9); docs/ROADMAP-ICBM.md (I0: "turns itself off, with one log line")
public final class IcbmGate {

    /** The outcome of the gate. */
    public enum Verdict {
        /** Flag on and Galacticraft present: the module runs. */
        ACTIVE,
        /** The flag is off. The normal, silent way to disable the module. */
        OFF_BY_CONFIG,
        /** The flag is on but Galacticraft is absent: the module turns itself off and says so once. */
        OFF_NO_GALACTICRAFT
    }

    private IcbmGate() {}

    /**
     * @param flagOn            the raw {@code modules.icbm} config value
     * @param galacticraftReady whether Galacticraft's core mod is loaded
     * @return what happens to the module
     */
    public static Verdict decide(boolean flagOn, boolean galacticraftReady) {
        if (!flagOn) {
            return Verdict.OFF_BY_CONFIG;
        }
        return galacticraftReady ? Verdict.ACTIVE : Verdict.OFF_NO_GALACTICRAFT;
    }

    /**
     * @param verdict a verdict from {@link #decide}
     * @return the one log line for a self-disable, or {@code null} when nothing needs saying
     */
    public static String logLine(Verdict verdict) {
        if (verdict == Verdict.OFF_NO_GALACTICRAFT) {
            return "Module icbm is on in the config but Galacticraft is not loaded; the ICBM module turns itself off "
                + "(content stays registered, no recipes or behavior). Install Galacticraft to use it.";
        }
        return null;
    }
}
