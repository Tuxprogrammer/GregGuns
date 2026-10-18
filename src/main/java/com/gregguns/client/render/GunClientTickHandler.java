package com.gregguns.client.render;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Drives the gun animation clock from the FML event bus: one tick per client tick (end phase), and the partial tick at
 * the start of every rendered frame. Client only.
 */
@SideOnly(Side.CLIENT)
public final class GunClientTickHandler {

    private final GunAnimations animations;

    GunClientTickHandler(GunAnimations animations) {
        this.animations = animations;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getMinecraft().theWorld == null) {
            animations.clear();
            return;
        }
        animations.clientTick();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            animations.setPartial(event.renderTickTime);
        }
    }
}
