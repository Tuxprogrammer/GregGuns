package com.gregguns.client.armor;

import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * Client half of the {@code modules.armor} module. Armor 3D models are out of scope for M3 wave A: the sets use the
 * vanilla biped armor layers (see {@code ItemGregGunsArmor.getArmorTexture}), so the only client work is the
 * step-height
 * handler. Client only, reached through {@code ClientProxy}.
 */
// Spec: docs/ROADMAP.md (M3)
public final class ArmorClientModule implements ClientModule {

    @Override
    public ModuleId id() {
        return ModuleId.ARMOR;
    }

    @Override
    public void init() {
        FMLCommonHandler.instance()
            .bus()
            .register(new StepHeightClient());
        FMLCommonHandler.instance()
            .bus()
            .register(new com.gregguns.client.flight.FlightClient());
    }
}
