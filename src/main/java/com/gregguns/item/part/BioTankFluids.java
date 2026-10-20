package com.gregguns.item.part;

import net.minecraftforge.fluids.FluidStack;

import com.gregguns.GregGuns;
import com.gregguns.compat.BartWorksCulture;
import com.gregguns.core.config.BioGunConfig;

import gregtech.api.enums.Materials;

/**
 * Which fluids the Bio Tank takes: the E. coli culture fluid that BartWorks' Bacterial Vat makes, resolved from
 * BartWorks' culture registry (never by a hard-coded fluid name), and GT Biomass when {@code bioGun.acceptBiomass} is
 * on
 * or when the culture fluid cannot be resolved (BartWorks missing). The decision is the pure {@link #accepts}; the
 * lookups are cached on first use, which is after every mod's init, so BartWorks' fluids exist. Common side.
 */
// Spec: docs/gregguns-spec/06-items/fluids.md (Bio Tank), docs/research/pack-integration-audit.md (Option A)
public final class BioTankFluids {

    private static boolean resolved;
    private static String cultureFluid;
    private static String biomassFluid;

    private BioTankFluids() {}

    /**
     * The pure decision.
     *
     * @param fluidName     the fluid offered
     * @param cultureFluid  the culture fluid's name, or {@code null} when unresolved
     * @param biomassFluid  GT Biomass's fluid name, or {@code null} when GT has none
     * @param acceptBiomass the {@code bioGun.acceptBiomass} switch
     * @return whether the tank takes it
     */
    public static boolean accepts(String fluidName, String cultureFluid, String biomassFluid, boolean acceptBiomass) {
        if (fluidName == null) {
            return false;
        }
        if (fluidName.equals(cultureFluid)) {
            return true;
        }
        return (acceptBiomass || cultureFluid == null) && fluidName.equals(biomassFluid);
    }

    /**
     * @param fluidName the fluid offered to a Bio Tank
     * @return whether the tank takes it under the current config and pack
     */
    public static synchronized boolean accepts(String fluidName) {
        resolve();
        return accepts(fluidName, cultureFluid, biomassFluid, BioGunConfig.acceptBiomass);
    }

    /** @return the resolved culture fluid's name, or {@code null} when BartWorks' culture is unavailable */
    public static synchronized String cultureFluidName() {
        resolve();
        return cultureFluid;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        cultureFluid = BartWorksCulture.eColiFluidName();
        FluidStack biomass = Materials.Biomass.getFluid(1);
        biomassFluid = biomass == null || biomass.getFluid() == null ? null
            : biomass.getFluid()
                .getName();
        if (cultureFluid == null) {
            GregGuns.LOG.warn(
                "Bio Tank: no E. coli culture fluid (BartWorks missing or the culture unset); falling back to Biomass");
        } else {
            GregGuns.LOG.info("Bio Tank takes {} (E. coli culture fluid)", cultureFluid);
        }
    }
}
