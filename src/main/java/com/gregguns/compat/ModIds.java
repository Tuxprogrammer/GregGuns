package com.gregguns.compat;

/**
 * FML mod ids of the optional mods GregGuns integrates with, defined once. Compile-time constants, so annotations such
 * as {@code @Optional.Method(modid = ...)} can use them too.
 */
public final class ModIds {

    /** Galacticraft's core mod id in the GTNH pack (GalacticraftCore, GalacticraftMars; MOD-INDEX.md). */
    public static final String GALACTICRAFT_CORE = "GalacticraftCore";
    /** OpenComputers' mod id. */
    public static final String OPEN_COMPUTERS = "OpenComputers";

    private ModIds() {}
}
