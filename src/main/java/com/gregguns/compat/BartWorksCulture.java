package com.gregguns.compat;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

import com.gregguns.GregGuns;

import bartworks.API.enums.BioCultureEnum;
import bartworks.common.loaders.BioCultureLoader;
import bartworks.util.BioCulture;
import cpw.mods.fml.common.Loader;

/**
 * The only class that touches BartWorks (inside GT5-Unofficial): the E. coli culture, its fluid and its petri dish.
 * Every method checks that BartWorks is loaded and answers {@code null} on any failure, so callers degrade softly. Call
 * it only after init, when BartWorks has created its culture fluids. Common side.
 */
public final class BartWorksCulture {

    private static final String MODID = "bartworks";

    private BartWorksCulture() {}

    private static BioCulture eColi() {
        if (!Loader.isModLoaded(MODID)) {
            return null;
        }
        try {
            // Source: the field is set in BartWorks' preInit from BioCultureEnum.EscherichiaKoli ("Escherichia koli").
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/loaders/BioCultureLoader.java#L25
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/loaders/BioCultureLoader.java#L34
            return BioCultureLoader.eColi;
        } catch (Throwable t) {
            GregGuns.LOG.warn("BartWorks E. coli culture is unavailable: {}", t.toString());
            return null;
        }
    }

    /**
     * @return E. coli's culture fluid, or {@code null} when BartWorks is missing, or the culture is not breedable or
     *         unset
     */
    public static Fluid eColiFluid() {
        BioCulture culture = eColi();
        if (culture == null) {
            return null;
        }
        try {
            // Source: isBreedable / getFluidNotSet / getFluid (getFluid throws while the fluid is unset).
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/util/BioCulture.java#L135-L151
            // The fluid is created for each breedable culture in FluidLoader (called from init):
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/loaders/FluidLoader.java#L82-L100
            if (!culture.isBreedable() || culture.getFluidNotSet()) {
                return null;
            }
            return culture.getFluid();
        } catch (Throwable t) {
            GregGuns.LOG.warn("BartWorks E. coli culture fluid is unavailable: {}", t.toString());
            return null;
        }
    }

    /** @return the culture fluid's Forge name, or {@code null} when unresolved */
    public static String eColiFluidName() {
        Fluid fluid = eColiFluid();
        return fluid == null ? null : fluid.getName();
    }

    /** @return one E. coli petri dish, or {@code null} when unresolved */
    public static ItemStack eColiPetriDish() {
        BioCulture culture = eColi();
        if (culture == null) {
            return null;
        }
        try {
            // Source:
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/API/enums/BioCultureEnum.java#L116-L118
            return BioCultureEnum.getPetriDish(culture);
        } catch (Throwable t) {
            GregGuns.LOG.warn("BartWorks E. coli petri dish is unavailable: {}", t.toString());
            return null;
        }
    }
}
