package com.gregguns.armor.oxygen.gc;

import net.minecraft.item.Item;

import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the Galacticraft oxygen items. A separate class so that the JVM verifier of the caller never has to load
 * {@link OxygenMaskGC} or {@link OxygenTanksGC} (and so Galacticraft) unless this method really runs (GGS-05-23).
 * Common side, preInit, only when GalacticraftCore is loaded.
 */
// Spec: docs/gregguns-spec/05-armor/oxygen-gear.md (GGS-05-22, GGS-05-23)
public final class GcOxygen {

    private GcOxygen() {}

    /**
     * @param registrar the armor module's registrar
     * @param maskName  registry name of the mask
     * @return the registered Galacticraft oxygen mask
     */
    public static Item registerMask(ModuleRegistrar registrar, String maskName) {
        return registrar.registerItem(maskName, new OxygenMaskGC());
    }

    /**
     * @param registrar the armor module's registrar
     * @param tanksName registry name of the tanks
     * @return the registered Galacticraft oxygen tanks
     */
    public static Item registerTanks(ModuleRegistrar registrar, String tanksName) {
        return registrar.registerItem(tanksName, new OxygenTanksGC());
    }
}
