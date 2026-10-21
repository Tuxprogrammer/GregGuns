package com.gregguns.client.armor;

import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * Client half of the {@code modules.armor} module. The armor sets use the vanilla biped armor layers (see
 * {@code ItemGregGunsArmor.getArmorTexture}); the
 * client work is the step-height handler, the flight motion and the worn models of the accessories
 * ({@code com.gregguns.client.accessory}). Client only, reached through {@code ClientProxy}.
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
            .register(new SwimBoostClient());
        FMLCommonHandler.instance()
            .bus()
            .register(new com.gregguns.client.flight.FlightClient());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .register(new com.gregguns.client.accessory.AccessoryRenderEvents());
    }
}
