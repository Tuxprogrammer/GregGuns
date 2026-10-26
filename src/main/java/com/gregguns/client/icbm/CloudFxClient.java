package com.gregguns.client.icbm;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntitySmokeFX;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import com.gregguns.icbm.fx.ActiveClouds;
import com.gregguns.icbm.fx.CloudFxHooks;
import com.gregguns.icbm.fx.CloudKind;
import com.gregguns.icbm.fx.CloudPlan;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Animates the blast clouds the server announces ({@code PacketBlastCloud}): each client tick, every running cloud
 * spawns a bounded handful of vanilla smoke particles, tinted per gas kind, scattered through the cloud's sphere (or
 * up a rising column for the plume). The particles are added to the effect renderer directly, because the world's own
 * {@code spawnParticle} drops anything farther than 16 blocks from the camera, which would hide a large cloud seen from
 * outside. Counts come from {@link CloudPlan}, are scaled down by the player's particle setting, and a cloud far from
 * the camera spawns nothing. Client only.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md
@SideOnly(Side.CLIENT)
public final class CloudFxClient {

    /** Clouds farther than their radius plus this from the camera spawn nothing, blocks. */
    private static final double CULL = 128.0;
    private static final int PARTICLE_AGE_MIN = 30;
    private static final int PARTICLE_AGE_SPAN = 20;
    private static final Random RND = new Random();

    private final ActiveClouds clouds = new ActiveClouds();
    private final double[] offset = new double[3];
    private int lastDimension = Integer.MIN_VALUE;

    private CloudFxClient() {}

    /** Installs the sink and the tick handler. */
    static void install() {
        CloudFxClient client = new CloudFxClient();
        // The packet handler already runs on the client main thread.
        CloudFxHooks
            .install((kind, x, y, z, radius, life, left) -> client.clouds.add(kind, x, y, z, radius, life, left));
        FMLCommonHandler.instance()
            .bus()
            .register(client);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null || mc.renderViewEntity == null) {
            clouds.clear();
            lastDimension = Integer.MIN_VALUE;
            return;
        }
        if (world.provider.dimensionId != lastDimension) {
            clouds.clear();
            lastDimension = world.provider.dimensionId;
        }
        if (mc.isGamePaused()) {
            return;
        }
        double scale = mc.gameSettings.particleSetting == 0 ? 1.0 : mc.gameSettings.particleSetting == 1 ? 0.5 : 0.25;
        Entity eye = mc.renderViewEntity;
        for (ActiveClouds.Cloud c : clouds.clouds()) {
            double dx = c.x - eye.posX;
            double dy = c.y - eye.posY;
            double dz = c.z - eye.posZ;
            double reach = CULL + c.radius;
            if (dx * dx + dy * dy + dz * dz > reach * reach) {
                continue;
            }
            int n = (int) Math.ceil(CloudPlan.perTick(c.kind, c.radius) * scale);
            for (int i = 0; i < n; i++) {
                spawn(mc, world, c);
            }
        }
        clouds.tick();
    }

    private void spawn(Minecraft mc, World world, ActiveClouds.Cloud c) {
        CloudPlan.sample(c.kind, c.radius, c.age, c.life, RND, offset);
        double x = c.x + offset[0];
        double y = c.y + offset[1];
        double z = c.z + offset[2];
        boolean plume = c.kind == CloudKind.PLUME;
        float size = plume ? 9.0F : 6.0F;
        double rise = plume ? 0.08 + RND.nextDouble() * 0.06 : 0.0;
        EntityFX fx = new EntitySmokeFX(
            world,
            x,
            y,
            z,
            (RND.nextDouble() - 0.5) * 0.02,
            rise + (RND.nextDouble() - 0.5) * 0.01,
            (RND.nextDouble() - 0.5) * 0.02,
            size) {

            {
                // A shorter, fixed life keeps the live particle count bounded (vanilla caps one layer at 4,000).
                particleMaxAge = PARTICLE_AGE_MIN + RND.nextInt(PARTICLE_AGE_SPAN) + (plume ? 30 : 0);
            }

            /** Full bright: the tint is the cloud's color, not the (often dark) light at the particle. */
            @Override
            public int getBrightnessForRender(float partialTicks) {
                return 0xF000F0;
            }
        };
        float shade = 0.85F + RND.nextFloat() * 0.3F;
        fx.setRBGColorF(
            Math.min(1.0F, c.kind.red() * shade),
            Math.min(1.0F, c.kind.green() * shade),
            Math.min(1.0F, c.kind.blue() * shade));
        mc.effectRenderer.addEffect(fx);
    }
}
