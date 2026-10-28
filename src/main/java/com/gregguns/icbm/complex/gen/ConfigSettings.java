package com.gregguns.icbm.complex.gen;

import java.util.Arrays;
import java.util.Set;

import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.config.WorldgenConfig;
import com.gregguns.worldgen.StructureBiomes;
import com.gregguns.worldgen.WorldgenGate;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** {@link ComplexPlacer.Settings} from {@link IcbmConfig}, read at each call so a config edit takes effect. */
public final class ConfigSettings implements ComplexPlacer.Settings {

    /** @return the writer the ICBM module installs: real blocks, real GT machines, marks left to the filler */
    public static ComplexPlacer writer() {
        ComplexPlacer placer = new ComplexPlacer(
            new WorldBlocks(),
            new GtMachineHook(),
            MarkFiller.NONE,
            new ConfigSettings()).withRadarDish(RadarDishHook.PLACEHOLDER_RADAR_DISH);
        // A world that unloads (a save closed in singleplayer) takes the placer's caches and world reference with it.
        MinecraftForge.EVENT_BUS.register(new Releaser(placer));
        return placer;
    }

    @Override
    public boolean dimensionEnabled(String dimensionName) {
        if (!IcbmConfig.siloComplexEnabled) {
            return false;
        }
        for (String n : IcbmConfig.siloComplexDimensions) {
            if (n.equalsIgnoreCase(dimensionName)) {
                return true;
            }
        }
        return false;
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
