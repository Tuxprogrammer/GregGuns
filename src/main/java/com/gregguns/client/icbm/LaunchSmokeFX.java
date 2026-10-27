package com.gregguns.client.icbm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.world.World;

import com.gregguns.icbm.missile.LaunchTimer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * One puff of launch smoke (D88): gray and billowing, born at the engine nozzle, rolling outward along the floor and
 * swelling as it ages. The countdown thickens it: more puffs, faster spread, bigger puffs as ignition nears. Drawn
 * from the vanilla particle sheet's smoke frames; the motion and growth are our own. Client only.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-sequence.md (Smoke)
@SideOnly(Side.CLIENT)
public final class LaunchSmokeFX extends EntityFX {

    private final float startScale;
    private final float endScale;

    private LaunchSmokeFX(World world, double x, double y, double z, double vx, double vy, double vz, float start,
        float end, int life) {
        super(world, x, y, z, vx, vy, vz);
        this.motionX = vx;
        this.motionY = vy;
        this.motionZ = vz;
        this.noClip = false;
        float gray = 0.55F + rand.nextFloat() * 0.3F;
        this.particleRed = gray;
        this.particleGreen = gray;
        this.particleBlue = gray;
        this.particleAlpha = 0.85F;
        this.startScale = start;
        this.endScale = end;
        this.particleScale = start;
        this.particleMaxAge = life;
    }

    /**
     * Spawns this tick's smoke for a missile standing on its pad.
     *
     * @param world     the client world
     * @param x         nozzle X
     * @param y         nozzle Y (the missile's feet)
     * @param z         nozzle Z
     * @param intensity {@link LaunchTimer#smokeIntensity}: 0 for none, 1 at ignition
     * @param flame     true in the last moments, when the engines show fire
     */
    public static void emit(World world, double x, double y, double z, double intensity, boolean flame) {
        int puffs = LaunchTimer.puffsPerTick(intensity);
        double spread = LaunchTimer.spreadSpeed(intensity);
        Minecraft mc = Minecraft.getMinecraft();
        for (int i = 0; i < puffs; i++) {
            double angle = world.rand.nextDouble() * Math.PI * 2.0;
            double speed = spread * (0.5 + world.rand.nextDouble() * 0.6);
            float start = 1.2F + (float) intensity * 1.0F + world.rand.nextFloat() * 0.5F;
            float end = start * (2.2F + (float) intensity * 1.6F);
            int life = 50 + world.rand.nextInt(30) + (int) (intensity * 30.0);
            mc.effectRenderer.addEffect(
                new LaunchSmokeFX(
                    world,
                    x + (world.rand.nextDouble() - 0.5) * 0.4,
                    y + 0.15,
                    z + (world.rand.nextDouble() - 0.5) * 0.4,
                    Math.cos(angle) * speed,
                    0.02 + world.rand.nextDouble() * 0.04 + 0.10 * intensity,
                    Math.sin(angle) * speed,
                    start,
                    end,
                    life));
        }
        if (flame) {
            for (int i = 0; i < 3; i++) {
                world.spawnParticle(
                    "flame",
                    x + (world.rand.nextDouble() - 0.5) * 0.3,
                    y + 0.1,
                    z + (world.rand.nextDouble() - 0.5) * 0.3,
                    0.0,
                    -0.02,
                    0.0);
            }
        }
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) {
            setDead();
            return;
        }
        float t = (float) particleAge / particleMaxAge;
        particleTextureIndexX = 7 - (int) (t * 8.0F);
        particleScale = startScale + (endScale - startScale) * (float) Math.sqrt(t);
        particleAlpha = 0.85F * (1.0F - t * t);
        moveEntity(motionX, motionY, motionZ);
        // Slows as it rolls out, and starts to rise as it cools: a billow along the floor, then up.
        motionX *= 0.95;
        motionZ *= 0.95;
        motionY = motionY * 0.985 + 0.004;
    }
}
