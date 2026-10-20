package com.gregguns.core.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Bio Gun switches, the {@code bioGun} category of {@code config/gregguns.cfg}. Fields only; no Minecraft types.
 * Synced: the server's value decides what the Bio Tank accepts.
 */
// Spec: docs/gregguns-spec/06-items/fluids.md (Bio Tank), docs/research/pack-integration-audit.md (Option A)
@Config(modid = "gregguns", category = "bioGun")
@Config.Sync
public class BioGunConfig {

    @Config.Comment({ "Also let the Bio Tank take plain GT Biomass, besides the Bacterial Vat's E. coli culture fluid.",
        "Biomass is also accepted whenever BartWorks (the culture fluid's source) is missing. Default: false." })
    public static boolean acceptBiomass = false;
}
