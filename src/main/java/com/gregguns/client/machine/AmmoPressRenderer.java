package com.gregguns.client.machine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.gregguns.GregGuns;
import com.gregguns.core.config.ClientConfig;
import com.gregguns.machine.MTEAmmoPress;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Draws the Ammo Press's animated front on top of the block, choosing the strip frame from recipe progress.
 *
 * <p>
 * The active front overlay is a 16 x 128 strip of 8 frames. A vanilla animated sprite advances on a timer, so the block
 * itself shows the static front and this renderer draws one frame of the raw strip (bound as a plain texture, outside
 * the block atlas, so the {@code .mcmeta} does not apply) on the front face, plus the glow strip at full brightness.
 * Client only; reached from {@code MTEAmmoPress#renderTESR}, which GT calls on the client alone.
 */
// Spec: docs/gregguns-spec/08-machines-production/ammo-press.md (Animation, layers 1 and 2; GGS-08-55, GGS-08-56)
public final class AmmoPressRenderer {

    private static final String DIR = "textures/blocks/machines/ammo_press/";
    private static final ResourceLocation FRONT = new ResourceLocation(
        GregGuns.MODID,
        DIR + "OVERLAY_FRONT_ACTIVE.png");
    private static final ResourceLocation GLOW = new ResourceLocation(
        GregGuns.MODID,
        DIR + "OVERLAY_FRONT_ACTIVE_GLOW.png");
    /** Frames in both strips (128 px tall, 16 px per frame). */
    private static final int FRAMES = 8;
    /** Distance in front of the face, to avoid z-fighting with the block's own front overlay. */
    private static final double LIFT = 0.002;

    private AmmoPressRenderer() {}

    /**
     * Renders the front frame for one press.
     *
     * @param press   the press being drawn
     * @param x       render offset of the block, from the tile-entity renderer
     * @param y       render offset of the block
     * @param z       render offset of the block
     * @param partial partial tick
     */
    public static void render(MTEAmmoPress press, double x, double y, double z, float partial) {
        if (!ClientConfig.ammoPressAnimation) {
            return;
        }
        IGregTechTileEntity tile = press.getBaseMetaTileEntity();
        Minecraft mc = Minecraft.getMinecraft();
        if (tile == null || mc.theWorld == null) {
            return;
        }
        double now = mc.theWorld.getTotalWorldTime() + partial;
        int frame = press.progressTracker()
            .frame(now, tile.isActive(), FRAMES);
        ForgeDirection facing = tile.getFrontFacing();
        if (frame < 0 || facing == ForgeDirection.UP || facing == ForgeDirection.DOWN) {
            return;
        }
        World world = tile.getWorld();
        double v0 = frame / (double) FRAMES;
        double v1 = (frame + 1) / (double) FRAMES;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0F, -1.0F);

        // Base layer, lit by the light in front of the face.
        int light = world.getLightBrightnessForSkyBlocks(
            tile.getXCoord() + facing.offsetX,
            tile.getYCoord(),
            tile.getZCoord() + facing.offsetZ,
            0);
        quad(FRONT, facing, v0, v1, light);
        // Glow layer: full brightness regardless of the world light.
        quad(GLOW, facing, v0, v1, 15 << 20 | 15 << 4);

        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private static void quad(ResourceLocation texture, ForgeDirection facing, double v0, double v1, int light) {
        Minecraft.getMinecraft().renderEngine.bindTexture(texture);
        // Block-local corners for the four horizontal faces, with u running left to right as seen from outside.
        double lo = -LIFT;
        double hi = 1 + LIFT;
        double xl;
        double zl;
        double xr;
        double zr;
        switch (facing) {
            case NORTH:
                xl = 1;
                zl = lo;
                xr = 0;
                zr = lo;
                break;
            case SOUTH:
                xl = 0;
                zl = hi;
                xr = 1;
                zr = hi;
                break;
            case WEST:
                xl = lo;
                zl = 0;
                xr = lo;
                zr = 1;
                break;
            default: // EAST
                xl = hi;
                zl = 1;
                xr = hi;
                zr = 0;
                break;
        }
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setBrightness(light);
        t.addVertexWithUV(xl, 0, zl, 0, v1);
        t.addVertexWithUV(xr, 0, zr, 1, v1);
        t.addVertexWithUV(xr, 1, zr, 1, v0);
        t.addVertexWithUV(xl, 1, zl, 0, v0);
        t.draw();
    }
}
