package com.gregguns.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import com.gregguns.client.anim.AnimWindow;
import com.gregguns.client.anim.GunAnimState;
import com.gregguns.client.anim.GunCurves;
import com.gregguns.client.render.profile.GunTransforms;
import com.gregguns.client.render.profile.ScopeSpec;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Forge-bus handlers of the gun rendering framework: the zoom FOV, the scope overlay, the held-gun arm pose and
 * clearing state on world unload. Client only.
 *
 * <p>
 * Scope FOV goes through {@code FOVUpdateEvent}, so it multiplies with Angelica's zoom key, which divides the final
 * FOV afterwards (GGS-03-05). The overlay is drawn on the {@code HELMET} element, which Angelica's HUD cache redraws
 * every frame (GGS-03-04), at a size taken from the screen, never from the FOV.
 *
 * <p>
 * Spec: rendering-framework.md sections 6.3 and 9; angelica-item-rendering.md.
 */
@SideOnly(Side.CLIENT)
public final class GunClientForgeEvents {

    private final GunAnimations animations;

    GunClientForgeEvents(GunAnimations animations) {
        this.animations = animations;
    }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent event) {
        // Clamp only our armor speed modifier's share of the FOV to vanilla's Speed II + sprint (ARM-05).
        net.minecraft.entity.ai.attributes.IAttributeInstance speed = event.entity
            .getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed);
        net.minecraft.entity.ai.attributes.AttributeModifier ours = speed == null ? null
            : speed.getModifier(com.gregguns.armor.ArmorEvents.SPEED_ID);
        if (ours != null) {
            double ratio = speed.getAttributeValue() / event.entity.capabilities.getWalkSpeed();
            event.newfov *= (float) com.gregguns.armor.logic.FovClamp.fovFactor(ratio, 1.0 + ours.getAmount());
        }
        event.newfov *= (float) animations.zoom()
            .fovMultiplier(animations.partial());
        // Aim down sights: a modest extra narrowing for guns with no scope zoom (1 for every other gun).
        event.newfov *= (float) animations.ads()
            .fovMultiplier(animations.partial());
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            // Sights lined up: the gun's own sights are the aim point, so the crosshair goes (cancel only if it is
            // ours to cancel: first person, a gun aiming, option on).
            if (animations.ads()
                .hidesCrosshair() && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
                event.setCanceled(true);
            }
            return;
        }
        if (event.type != RenderGameOverlayEvent.ElementType.HELMET) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.gameSettings.thirdPersonView != 0) {
            return;
        }
        ScopeSpec forced = GunRenderers.debugScope();
        if (forced != null) {
            drawScope(forced, event.resolution, 0.0);
            return;
        }
        ItemStack held = player.getHeldItem();
        if (held == null || !animations.zoom()
            .isScoped(held.getItem())) {
            return;
        }
        GunTransforms t = GunRenderers.transformsOf(held.getItem());
        ScopeSpec scope = t == null ? null : t.scope();
        if (scope == null) {
            return;
        }
        double kick = 0.0;
        GunAnimState anim = animations.peek(player);
        if (anim != null) {
            double p = anim.recoilProgress(animations.now(), Item.getIdFromItem(held.getItem()));
            kick = p == AnimWindow.NONE ? 0.0 : GunCurves.scopeRecoil(p);
        }
        drawScope(scope, event.resolution, kick);
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // The arms of any player holding a gun are raised like a drawn bow. Vanilla clears the flag after the draw.
        if (GunRenderers.isGun(event.entityPlayer.getHeldItem())) {
            RenderPlayer r = event.renderer;
            r.modelBipedMain.aimedBow = true;
            r.modelArmorChestplate.aimedBow = true;
            r.modelArmor.aimedBow = true;
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            animations.clear();
        }
    }

    /**
     * Draws the scope: a square sized by the screen height, centered, shifted by the recoil kick, with black bars
     * filling the rest of the screen.
     */
    private static void drawScope(ScopeSpec scope, ScaledResolution res, double kick) {
        double w = res.getScaledWidth_double();
        double h = res.getScaledHeight_double();
        double dy = -kick * scope.kickUp() * h;
        double dx = kick * scope.kickSide() * h;
        double x0 = (w - h) / 2.0 + dx;
        double y0 = dy;
        double x1 = x0 + h;
        double y1 = y0 + h;
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(ScopeTextures.get(scope.texture()));
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.addVertexWithUV(x0, y1, -90.0, 0.0, 1.0);
            t.addVertexWithUV(x1, y1, -90.0, 1.0, 1.0);
            t.addVertexWithUV(x1, y0, -90.0, 1.0, 0.0);
            t.addVertexWithUV(x0, y0, -90.0, 0.0, 0.0);
            t.draw();
            // Black bars sample a white texel rather than switching textures off: shader gbuffers always sample.
            WhiteTexture.bind();
            t.startDrawingQuads();
            t.setColorOpaque_I(0);
            bar(t, 0.0, 0.0, x0, h); // left
            bar(t, x1, 0.0, w, h); // right
            bar(t, x0, 0.0, x1, y0); // top, when the kick shifts the square down
            bar(t, x0, y1, x1, h); // bottom, when the kick shifts it up
            t.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static void bar(Tessellator t, double left, double top, double right, double bottom) {
        if (right <= left || bottom <= top) {
            return;
        }
        t.addVertex(left, bottom, -90.0);
        t.addVertex(right, bottom, -90.0);
        t.addVertex(right, top, -90.0);
        t.addVertex(left, top, -90.0);
    }

    /** Registers this handler on the Forge event bus. */
    void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }
}
