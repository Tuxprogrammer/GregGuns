package com.gregguns.icbm.complex.gen;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.worldgen.StructureBiomes;

/** {@link ComplexPlacer.Settings} from {@link IcbmConfig}, read at each call so a config edit takes effect. */
public final class ConfigSettings implements ComplexPlacer.Settings {

    /** @return the writer the ICBM module installs: real blocks, real GT machines, marks left to the filler */
    public static ComplexPlacer writer() {
        return new ComplexPlacer(new WorldBlocks(), new GtMachineHook(), MarkFiller.NONE, new ConfigSettings())
            .withRadarDish(RadarDishHook.PLACEHOLDER_RADAR_DISH);
    }

    @Override
    public boolean dimensionEnabled(String dimensionName) {
        return IcbmConfig.siloComplexEnabled
            && new HashSet<String>(Arrays.asList(IcbmConfig.siloComplexDimensions)).contains(dimensionName);
    }

    @Override
    public int chancePercent() {
        return IcbmConfig.siloComplexChancePercent;
    }

    @Override
    public Set<Type> overworldBiomes() {
        return StructureBiomes.parseTypes(Arrays.asList(IcbmConfig.siloComplexOverworldBiomes));
    }
}
