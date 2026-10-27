package com.gregguns.client.icbm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;

import com.gregguns.icbm.fx.OrbFxHooks;
import com.gregguns.icbm.fx.OrbKind;
import com.gregguns.icbm.fx.OrbPlan;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the annihilation orbs the server announces ({@code PacketBlastOrb}): the antimatter flash and the red matter
 * body, each one translucent glowing sphere (a red matter body also gets a darker core). One reused GLU quadric, no
 * per-frame allocation, no block scan, at most {@link OrbPlan#MAX_ACTIVE} orbs. Client only.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md (Orbs)
@SideOnly(Side.CLIENT)
public final class OrbFxClient {

    private static final class Orb {

        final OrbKind kind;
        final double x;
        final double y;
        final double z;
        int radius;
        final int whole;
        int age;
        int left;

        Orb(OrbKind kind, double x, double y, double z, int radius, int life) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.left = life;
            this.whole = life;
        }
    }

    private final List<Orb> orbs = new ArrayList<>();
    private final Sphere sphere = new Sphere();
    private int lastDimension = Integer.MIN_VALUE;

    private OrbFxClient() {}

    /** Installs the sink, the tick handler and the world renderer. */
    static void install() {
        OrbFxClient client = new OrbFxClient();
        client.sphere.setDrawStyle(GLU.GLU_FILL);
        client.sphere.setNormals(GLU.GLU_NONE);
        OrbFxHooks.install(client::add);
        FMLCommonHandler.instance()
            .bus()
            .register(client);
        MinecraftForge.EVENT_BUS.register(client);
    }

    private void add(OrbKind kind, double x, double y, double z, int radius, int life) {
        for (Orb o : orbs) {
            if (o.kind == kind && Math.abs(o.x - x) < 1.0 && Math.abs(o.y - y) < 1.0 && Math.abs(o.z - z) < 1.0) {
                o.radius = radius;
                o.left = Math.max(o.left, life);
                return;
            }
        }
        if (orbs.size() >= OrbPlan.MAX_ACTIVE) {
            orbs.remove(0);
        }
        orbs.add(new Orb(kind, x, y, z, radius, life));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            orbs.clear();
            lastDimension = Integer.MIN_VALUE;
            return;
        }
        if (mc.theWorld.provider.dimensionId != lastDimension) {
            orbs.clear();
            lastDimension = mc.theWorld.provider.dimensionId;
        }
        if (mc.isGamePaused()) {
            return;
        }
        for (Iterator<Orb> it = orbs.iterator(); it.hasNext();) {
            Orb o = it.next();
            o.age++;
            if (--o.left <= 0) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (orbs.isEmpty()) {
            return;
        }
        Entity eye = Minecraft.getMinecraft().renderViewEntity;
        if (eye == null) {
            return;
        }
        float pt = event.partialTicks;
        double ex = eye.lastTickPosX + (eye.posX - eye.lastTickPosX) * pt;
        double ey = eye.lastTickPosY + (eye.posY - eye.lastTickPosY) * pt;
        double ez = eye.lastTickPosZ + (eye.posZ - eye.lastTickPosZ) * pt;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        for (Orb o : orbs) {
            double r = OrbPlan.drawRadius(o.kind, o.radius, o.age, Math.max(o.whole, o.age + o.left));
            float a = OrbPlan.alpha(o.kind, o.age, Math.max(o.whole, o.age + o.left));
            if (r < 0.1 || a <= 0.0F) {
                continue;
            }
            GL11.glPushMatrix();
            GL11.glTranslated(o.x - ex, o.y - ey, o.z - ez);
            if (o.kind == OrbKind.RED_MATTER) {
                GL11.glColor4f(0.04F, 0.0F, 0.02F, a);
                sphere.draw((float) (r * 0.9), OrbPlan.SEGMENTS, OrbPlan.SEGMENTS);
            }
            GL11.glColor4f(o.kind.red(), o.kind.green(), o.kind.blue(), a * (o.kind.swells() ? 0.6F : 0.25F));
            sphere.draw((float) r, OrbPlan.SEGMENTS, OrbPlan.SEGMENTS);
            GL11.glPopMatrix();
        }
        GL11.glDepthMask(true);
        GL11.glPopAttrib();
    }
}
