package com.gregguns.client.icbm;

import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;
import com.gregguns.icbm.missile.EntityMissileT1;
import com.gregguns.icbm.missile.EntityMissileT2;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client half of {@code modules.icbm}: the warhead block renderer and the blast cloud animation. Its content is always
 * registered, so this runs
 * whatever the module flag says. Client only.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md
@SideOnly(Side.CLIENT)
public final class IcbmClientModule implements ClientModule {

    @Override
    public ModuleId id() {
        return ModuleId.ICBM;
    }

    @Override
    public void preInit() {
        WarheadBlockRenderer.register();
        CloudFxClient.install();
        MissileBodyRenderer.register();
        RenderingRegistry.registerEntityRenderingHandler(EntityMissileT1.class, new RenderMissile());
        RenderingRegistry.registerEntityRenderingHandler(EntityMissileT2.class, new RenderMissile());
    }

    @Override
    public void init() {
        MissileBodyItemRenderer.register();
        WarheadItemRenderer.register();
    }
}
