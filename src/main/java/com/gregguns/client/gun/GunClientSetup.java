package com.gregguns.client.gun;

import net.minecraftforge.common.MinecraftForge;

import com.gregguns.gun.GunClientHooks;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Wires the client half of the gun framework: the prediction mirror, the input handler, the sound-and-zoom
 * implementation of {@code GunClientEvents}, the camera zoom and the ammo HUD. Called once from
 * {@code GunsClientModule.init}. Client only.
 */
@SideOnly(Side.CLIENT)
public final class GunClientSetup {

    private GunClientSetup() {}

    /** Creates and registers the client gun handlers. */
    public static void init() {
        ClientGunMirror mirror = new ClientGunMirror();
        ZoomView zoom = new ZoomView();
        GunClientHooks.install(new ClientGunEvents(zoom));
        GunClientHooks.installSync(mirror);
        FMLCommonHandler.instance()
            .bus()
            .register(new GunInputHandler(mirror));
        MinecraftForge.EVENT_BUS.register(zoom);
        MinecraftForge.EVENT_BUS.register(new AmmoHud(mirror));
    }
}
