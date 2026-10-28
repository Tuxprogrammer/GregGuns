package com.gregguns.icbm.complex.gen;

import java.util.Arrays;
import java.util.Set;

import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import com.gregguns.GregGuns;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.config.WorldgenConfig;
import com.gregguns.icbm.complex.PlanetTable;
import com.gregguns.worldgen.StructureBiomes;
import com.gregguns.worldgen.WorldgenGate;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** {@link ComplexPlacer.Settings} from {@link IcbmConfig}, read at each call so a config edit takes effect. */
public final class ConfigSettings implements ComplexPlacer.Settings {

    /** @return the writer the ICBM module installs: real blocks, real GT machines, the small rooms' furniture */
    public static ComplexPlacer writer() {
        ComplexPlacer placer = new ComplexPlacer(
            new WorldBlocks(),
            new GtMachineHook(),
            new FurnitureFiller(),
            new ConfigSettings()).withStyles(PaletteStyles.FACTORY);
        // A world that unloads (a save closed in singleplayer) takes the placer's caches and world reference with it.
        MinecraftForge.EVENT_BUS.register(new Releaser(placer));
        return placer;
    }

    /** Config names already logged as missing from the planet table, so each is logged once. */
    private final Set<String> loggedUnknown = java.util.Collections
        .newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());

    @Override
    public boolean dimensionEnabled(String dimensionName) {
        if (!IcbmConfig.siloComplexEnabled) {
            return false;
        }
        Gate g = gate(IcbmConfig.siloComplexDimensions, dimensionName);
        if (g == Gate.NOT_IN_TABLE && loggedUnknown.add(dimensionName.toLowerCase(java.util.Locale.ROOT))) {
            GregGuns.LOG.info(
                "Silo complexes: '{}' is in icbm.siloComplexDimensions but not in the planet table (D38); it gets no"
                    + " complexes",
                dimensionName);
        }
        return g == Gate.ENABLED;
    }

    /** What the dimension list says about one dimension. */
    enum Gate {
        /** Listed and in the planet table: complexes generate. */
        ENABLED,
        /** Not listed. */
        NOT_LISTED,
        /**
         * Listed but missing from the planet table (T Ceti E in a config written before it left the table): skipped,
         * never generated at a fallback ceiling.
         */
        NOT_IN_TABLE
    }

    /**
     * @param listed        the config's dimension names
     * @param dimensionName a {@link DimensionKey}
     * @return whether the dimension generates complexes, and why not
     */
    static Gate gate(String[] listed, String dimensionName) {
        for (String n : listed) {
            if (n.equalsIgnoreCase(dimensionName)) {
                return PlanetTable.tierOf(dimensionName) == null ? Gate.NOT_IN_TABLE : Gate.ENABLED;
            }
        }
        return Gate.NOT_LISTED;
    }

    @Override
    public int chancePercent() {
        return IcbmConfig.siloComplexChancePercent;
    }

    @Override
    public boolean worldTypeAllowed(String worldType) {
        return WorldgenGate.worldTypeAllowed(WorldgenConfig.structureWorldTypes, worldType);
    }

    @Override
    public int[] ross128Weights() {
        return IcbmConfig.siloComplexRoss128Weights;
    }

    @Override
    public Set<Type> overworldBiomes() {
        return StructureBiomes.parseTypes(Arrays.asList(IcbmConfig.siloComplexOverworldBiomes));
    }

    /** Public, because Forge's generated event handlers call it from outside the package. */
    public static final class Releaser {

        private final ComplexPlacer placer;

        Releaser(ComplexPlacer placer) {
            this.placer = placer;
        }

        @SubscribeEvent
        public void onUnload(WorldEvent.Unload event) {
            if (!event.world.isRemote) placer.releaseWorld();
        }
    }
}
