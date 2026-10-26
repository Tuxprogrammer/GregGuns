package com.gregguns.client.icbm;

import com.gregguns.core.module.ClientModule;
import com.gregguns.core.module.ModuleId;

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
    }
}
