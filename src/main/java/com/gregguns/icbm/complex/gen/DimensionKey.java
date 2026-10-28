package com.gregguns.icbm.complex.gen;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import micdoodle8.mods.galacticraft.api.prefab.world.gen.WorldProviderSpace;

/**
 * The key a dimension goes by in the D38 planet table and in {@code icbm.siloComplexDimensions}. A Galacticraft
 * planet's {@code getDimensionName()} is its localized name ("Io", "Ganymede", "Barnarda C"), which differs by
 * language and from the table, so a space dimension is keyed by its celestial body's own unlocalized name ("iojupiter",
 * "ganymed", "barnarda2"), the way GalacticGreg keys them. Any other dimension keeps its provider name.
 */
// Source: GalacticGreg's lookup of the same key,
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/galacticgreg/api/enums/DimensionDef.java#L236-L250
// and Galacticraft's WorldProviderSpace#getDimensionName (the localized name),
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/api/prefab/world/gen/WorldProviderSpace.java#L80-L82
public final class DimensionKey {

    private DimensionKey() {}

    /** @return the table key of a world's dimension */
    public static String of(World world) {
        return of(world.provider);
    }

    /** @return the table key of a provider */
    public static String of(WorldProvider provider) {
        if (Loader.isModLoaded("GalacticraftCore")) {
            String body = bodyName(provider);
            if (body != null) {
                return body;
            }
        }
        return provider.getDimensionName();
    }

    @Optional.Method(modid = "GalacticraftCore")
    private static String bodyName(WorldProvider provider) {
        if (provider instanceof WorldProviderSpace) {
            return ((WorldProviderSpace) provider).getCelestialBody()
                .getName();
        }
        return null;
    }
}
