package com.gregguns.client.render;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Entry point of the gun rendering framework: registers the tick handler (animation clock) and the Forge handlers
 * (zoom FOV, scope overlay, arm pose). Gun renderers themselves are registered per item through
 * {@link GunRenderers#register}. Called once from {@code GunsClientModule.init}. Client only.
 */
@SideOnly(Side.CLIENT)
public final class GunRenderClient {

    private GunRenderClient() {}

    /** Registers the framework's event handlers. */
    public static void init(GunAnimations animations) {
        FMLCommonHandler.instance()
            .bus()
            .register(new GunClientTickHandler(animations));
        new GunClientForgeEvents(animations).register();
    }
}
