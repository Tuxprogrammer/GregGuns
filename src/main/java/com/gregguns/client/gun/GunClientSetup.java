package com.gregguns.client.gun;

import net.minecraftforge.common.MinecraftForge;

import com.gregguns.client.GunsClientModule;
import com.gregguns.client.render.GunRenderers;
import com.gregguns.client.render.profile.ClientGunDefinition;
import com.gregguns.client.render.profile.ClientGunDefinitions;
import com.gregguns.gun.GunClientHooks;
import com.gregguns.item.gun.GunItems;

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

    /**
     * Creates and registers the client gun handlers.
     *
     * @return the input handler, which the dev autotest drives
     */
    public static GunInputHandler init() {
        ClientGunMirror mirror = new ClientGunMirror();
        GunClientHooks.install(new ClientGunEvents(GunsClientModule.animations()));
        for (ClientGunDefinition definition : ClientGunDefinitions.ALL) {
            GunRenderers.register(definition, GunItems.get(definition.gunName()));
        }
        GunClientHooks.installSync(mirror);
        GunInputHandler input = new GunInputHandler(mirror);
        FMLCommonHandler.instance()
            .bus()
            .register(input);
        MinecraftForge.EVENT_BUS.register(new AmmoHud(mirror));
        return input;
    }
}
