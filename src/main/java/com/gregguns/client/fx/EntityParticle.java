package com.gregguns.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import com.gregguns.fx.ParticleState;
import com.gregguns.fx.QuadMath;
import com.gregguns.fx.RenderMode;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * One FX particle. The physics and look live in the pure {@link ParticleState}; this class is the thin vanilla shell:
 * it positions itself from the state each tick and draws exactly one quad through the supplied tessellator
 * (GGS-04-06). It does not collide and does not light by the world, except in {@code ALPHA_SHADED}.
 *
 * <p>
 * Side: client only. Created by {@link FxClient} on the client thread.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (3.3, 3.4);
// docs/gregguns-spec/04-projectiles-fx/particle-rendering.md
@SideOnly(Side.CLIENT)
final class EntityParticle extends EntityFX {

    private static final int FULL_BRIGHT = 0xF000F0;

    private final ParticleState state;
    private final ResourceLocation texture;
    /** The client tick count of this particle's last update, so {@link FxClient} can spot one vanilla dropped. */
    int lastUpdateTick;

    EntityParticle(World world, ParticleState state, ResourceLocation texture, int createdTick) {
        super(world, state.x(), state.y(), state.z(), 0, 0, 0);
        this.state = state;
        this.texture = texture;
        this.lastUpdateTick = createdTick;
        this.noClip = true;
        this.particleMaxAge = Integer.MAX_VALUE;
        this.setSize(0.01F, 0.01F);
    }

    @Override
    public void onUpdate() {
        lastUpdateTick = FxClient.tickCount();
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (!state.tick()) {
            setDead();
            return;
        }
        setPosition(state.x(), state.y(), state.z());
    }

    /**
     * Vanilla's layer 2 is the items sheet; this draws with its own texture and puts the layer's texture back, as the
     * spec's description of Techguns does.
     */
    @Override
    public int getFXLayer() {
        return 2;
    }

    @Override
    public void renderParticle(Tessellator t, float partial, float rx, float rxz, float rz, float ryz, float rxy) {
        ParticleDrawer.draw(t, this, state, texture, partial, rx, rxz, rz, ryz, rxy);
    }

    /** Draws a particle. Split out so the GL state handling reads in one place. */
    private static final class ParticleDrawer {

        static void draw(Tessellator t, EntityParticle p, ParticleState s, ResourceLocation texture, float partial,
            float rx, float rxz, float rz, float ryz, float rxy) {
            float size = s.size(partial);
            if (size <= 0F) {
                return;
            }
            int frame = s.frame(partial);
            float[] uv = QuadMath.uv(
                frame,
                s.def()
                    .columns(),
                s.def()
                    .rows(),
                s.rotation());
            double cx = s.x(partial) - interpPosX;
            double cy = s.y(partial) - interpPosY;
            double cz = s.z(partial) - interpPosZ;
            if (QuadMath.nearCull(cx, cy, cz, 0.1F * size)) {
                return;
            }
            double[] corners = QuadMath.corners(cx, cy, cz, 0.1F * size, rx, rxz, rz, ryz, rxy);
            RenderMode mode = s.def()
                .renderMode();
            int brightness = mode == RenderMode.ALPHA_SHADED ? p.getBrightnessForRender(partial) : FULL_BRIGHT;

            // Angelica 2.2.28 (shaders on) captures this quad instead of drawing it: it samples the bound texture,
            // blend function and depth mask at the first addVertex (so the state set below is the state the instanced
            // draw uses), and ignores alphaFunc, which comes from the layer's own state. So depth mask must be off
            // before the quad is emitted, and our alpha test would not help there.
            // Source:
            // https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/particles/ParticleCaptureTessellator.java
            // Source:
            // https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/rendering/particles/ParticleRenderState.java
            // The Techguns pattern the spec describes: set this particle's state, write the quad, draw it, put the
            // layer's state back and reopen the batch. Angelica's capture tessellator instances the quad; plain
            // vanilla draws it (and any batch already open) right here.
            Minecraft.getMinecraft().renderEngine.bindTexture(texture);
            boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            setState(mode);
            ParticleQuad.emit(t, corners, uv, s.rgb(partial), s.alpha(partial), brightness);
            t.draw();
            restoreState(depthTest, depthMask);
            Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationItemsTexture);
            t.startDrawingQuads();
        }

        private static void setState(RenderMode mode) {
            switch (mode) {
                case ADDITIVE:
                    GL11.glDepthMask(false);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                    break;
                case NO_Z_TEST:
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                    GL11.glDepthMask(false);
                    break;
                case SOLID:
                    GL11.glDisable(GL11.GL_BLEND);
                    break;
                case ALPHA:
                case ALPHA_SHADED:
                default:
                    // Blended quads must not write depth, or their transparent texels hide the water drawn later.
                    GL11.glDepthMask(false);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    break;
            }
        }

        /**
         * Puts back what vanilla's particle pass expects (alpha blend on) and the depth test and mask exactly as they
         * were before {@link #setState}: NO_Z_TEST switches both off, and a depth mask left off hides what draws after
         * (water under shaders).
         */
        private static void restoreState(boolean depthTest, boolean depthMask) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            if (depthTest) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            } else {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
            GL11.glDepthMask(depthMask);
        }
    }
}
