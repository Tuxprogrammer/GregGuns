package com.gregguns.client.projectile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.gregguns.GregGuns;
import com.gregguns.core.DevEnvironment;
import com.gregguns.explosion.FlashHooks;
import com.gregguns.explosion.FlashMath;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The flashbang white-out: a white sheet over the whole screen that stays solid for the first third of the effect and
 * then fades, drawn after the whole HUD. The curve is {@link FlashMath#overlayAlpha}; the server starts it with
 * {@code PacketFlash} through {@link FlashHooks}. Time is read from the world clock so a paused game does not tick it.
 * Client only.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/grenades.md (Flashbang)
@SideOnly(Side.CLIENT)
public final class FlashOverlay implements FlashHooks.Sink {

    private long startTime = -1L;
    private int total;

    @Override
    public void flash(int ticks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return;
        }
        startTime = mc.theWorld.getTotalWorldTime();
        total = ticks;
        if (DevEnvironment.testHooksEnabled()) {
            GregGuns.LOG.info("[gregguns] AUTOTEST flash white-out {} ticks", ticks);
        }
    }

    /** Draws the sheet over everything. */
    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || startTime < 0L) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            startTime = -1L;
            return;
        }
        double elapsed = mc.theWorld.getTotalWorldTime() - startTime + event.partialTicks;
        double alpha = FlashMath.overlayAlpha(elapsed, total);
        if (alpha <= 0.0) {
            startTime = -1L;
            return;
        }
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int a = (int) Math.round(alpha * 255.0);
        Gui.drawRect(0, 0, res.getScaledWidth(), res.getScaledHeight(), a << 24 | 0xFFFFFF);
    }
}
