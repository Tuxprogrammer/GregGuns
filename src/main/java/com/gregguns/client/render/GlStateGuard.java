package com.gregguns.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Scoped save and restore of OpenGL state for one render path, used with try-with-resources so the matrix and
 * attribute stacks are balanced on every exit, early returns and exceptions included:
 *
 * <pre>
 * try (GlStateGuard g = GlStateGuard.matrixAndAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT)) {
 *     // enable, disable, tint, draw
 * }
 * </pre>
 *
 * Attribute bits name exactly the state the path touches: {@code GL_ENABLE_BIT} (cull, blend, lighting, rescale
 * normal, alpha test), {@code GL_COLOR_BUFFER_BIT} (blend function, alpha function), {@code GL_DEPTH_BUFFER_BIT}
 * (depth mask and function), {@code GL_LIGHTING_BIT} (shade model), {@code GL_CURRENT_BIT} (color).
 *
 * <p>
 * Spec: docs/gregguns-spec/01-core (client rendering rules). Shaders such as Angelica's pipeline rely on vanilla state
 * being exactly as the caller left it, so a leak shows up elsewhere, for example as invisible water.
 */
@SideOnly(Side.CLIENT)
public final class GlStateGuard implements AutoCloseable {

    /** The four GL calls the guard makes; a seam so a unit test can record them. */
    public interface Gl {

        void pushMatrix();

        void popMatrix();

        void pushAttrib(int mask);

        void popAttrib();
    }

    private static final class Lwjgl implements Gl {

        @Override
        public void pushMatrix() {
            GL11.glPushMatrix();
        }

        @Override
        public void popMatrix() {
            GL11.glPopMatrix();
        }

        @Override
        public void pushAttrib(int mask) {
            GL11.glPushAttrib(mask);
        }

        @Override
        public void popAttrib() {
            GL11.glPopAttrib();
        }
    }

    private static Gl gl = new Lwjgl();

    private final Gl backend;
    private final boolean matrix;
    private final boolean attrib;
    private boolean closed;

    private GlStateGuard(boolean matrix, int attribMask, boolean attrib) {
        this.backend = gl;
        this.matrix = matrix;
        this.attrib = attrib;
        if (attrib) {
            backend.pushAttrib(attribMask);
        }
        if (matrix) {
            backend.pushMatrix();
        }
    }

    /** Test seam: replaces the GL backend, or restores LWJGL when given null. */
    static void useBackend(Gl replacement) {
        gl = replacement == null ? new Lwjgl() : replacement;
    }

    /** @return a guard that pushes the current matrix and pops it on close */
    public static GlStateGuard matrix() {
        return new GlStateGuard(true, 0, false);
    }

    /**
     * @param mask the {@code GL_*_BIT} groups to save
     * @return a guard that saves those attribute groups and restores them on close
     */
    public static GlStateGuard attrib(int mask) {
        return new GlStateGuard(false, mask, true);
    }

    /**
     * @param mask the {@code GL_*_BIT} groups to save
     * @return a guard for both the matrix and those attribute groups
     */
    public static GlStateGuard matrixAndAttrib(int mask) {
        return new GlStateGuard(true, mask, true);
    }

    /** Rebinds the block atlas, which vanilla assumes is bound after a custom pass that bound something else. */
    public static void rebindBlockAtlas() {
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (matrix) {
            backend.popMatrix();
        }
        if (attrib) {
            backend.popAttrib();
        }
    }
}
