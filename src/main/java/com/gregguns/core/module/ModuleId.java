package com.gregguns.core.module;

/**
 * Every feature module GregGuns can register, in the order the roadmap delivers them.
 *
 * <p>
 * The constant's {@link #configKey()} is the key under {@code modules} in {@code config/gregguns.cfg}, and must match a
 * field of {@link com.gregguns.core.config.ModulesConfig} (a unit test enforces this). Declaration order is the
 * dispatch order, so a module always appears after every module it {@linkplain #requires() requires}.
 */
// Spec: docs/gregguns-spec/01-core/config-gtnhlib.md
// Spec: docs/ROADMAP.md (M0 scope: "per-module registration ... with module toggles")
public enum ModuleId {

    /** Combat core and ballistic arsenal: damage system, guns, projectiles, ammo (M1 and M2). */
    GUNS("guns"),
    /** Armor, set bonuses and special slots (M3). Entry gate: M1 done. */
    ARMOR("armor", GUNS),
    /** Production chain and machines (M4). Entry gate: M1 done. */
    MACHINES("machines", GUNS),
    /** Energy and exotic weapons (M5). Entry gate: M2 and M4 done. */
    ENERGY_WEAPONS("energyWeapons", GUNS, MACHINES),
    /** Utility and defense machines (M6). Entry gate: M4 done. */
    UTILITY("utility", MACHINES),
    /** NPCs and spawning (M7). Entry gate: M2 done. */
    NPCS("npcs", GUNS),
    /** Worldgen and structures (M8). Entry gate: M7 done. */
    WORLDGEN("worldgen", NPCS),
    /**
     * Missiles, warheads and the ICBM program (ICBM roadmap I0 to I7). Off by default until the ICBM beta gate. Also
     * needs Galacticraft at runtime (D9): without it the module turns itself off, see
     * {@link com.gregguns.icbm.IcbmGate}.
     */
    ICBM("icbm", GUNS, MACHINES);

    private final String configKey;
    private final ModuleId[] requires;

    ModuleId(String configKey, ModuleId... requires) {
        this.configKey = configKey;
        this.requires = requires;
    }

    /** @return the name of this module's flag under {@code modules} in the config file */
    public String configKey() {
        return configKey;
    }

    /** @return the modules that must be enabled for this one to run; a fresh copy, safe to modify */
    public ModuleId[] requires() {
        return requires.clone();
    }
}
