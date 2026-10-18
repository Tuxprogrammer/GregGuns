package com.gregguns.explosion;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.gun.sound.GunSoundPlayer;
import com.gregguns.network.GregGunsNetwork;

/**
 * What a flashbang does when it goes off: a bang, a burst of sparks, and for every living thing within
 * {@link FlashMath#RADIUS} with a line of sight to the blast, an effect scaled by {@link FlashMath#intensity}. It never
 * touches a block and never damages anything.
 *
 * <ul>
 * <li><b>Players</b> get Blindness and Nausea for the effect time and a white-out on their screen
 * ({@link PacketFlash}). Looking away from the blast cuts the effect to between 15% and 100% of what facing it gives,
 * so a player who turns their back may take none.</li>
 * <li><b>Mobs</b> get Slowness and Weakness for the effect time and forget their attack target.</li>
 * </ul>
 *
 * The thrower is affected like anyone else, unless {@code explosions.selfDamage} is off (PFX-22 blinded the thrower
 * with no wall or facing check; the checks are the fix, and the config switch matches the explosions').
 *
 * <p>
 * GT hazards: none applies. GregTech's hazards (heat, cold, biological, radiation, electricity, gas, space) are
 * environmental and a flash is none of them, so protection there never reduces a flash; the Night Vision Goggles do not
 * either.
 *
 * <p>
 * Server only.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/grenades.md (Flashbang);
// docs/techguns-spec/04-projectiles-fx/projectiles.md
// (FlashBangProjectile.explode)
public final class FlashBlast {

    private static final float BANG_VOLUME = 4.0F;
    /** The bang's event: {@code guns.flashbang.bang}. */
    public static final String BANG = "guns.flashbang.bang";

    private FlashBlast() {}

    /**
     * Sets the flash off.
     *
     * @param world the world; nothing happens on a client
     * @param owner who threw it, may be {@code null}
     * @param x     centre X
     * @param y     centre Y
     * @param z     centre Z
     * @return how many entities were affected
     */
    @SuppressWarnings("unchecked")
    public static int detonate(World world, EntityLivingBase owner, double x, double y, double z) {
        if (world.isRemote) {
            return 0;
        }
        GunSoundPlayer.play(world, x, y, z, BANG, BANG_VOLUME, 1.0F, null);
        if (world instanceof WorldServer) {
            ((WorldServer) world).func_147487_a("fireworksSpark", x, y, z, 40, 0.15, 0.15, 0.15, 0.25);
        }
        double r = FlashMath.RADIUS;
        List<EntityLivingBase> nearby = world.getEntitiesWithinAABB(
            EntityLivingBase.class,
            AxisAlignedBB.getBoundingBox(x - r, y - r, z - r, x + r, y + r, z + r));
        int affected = 0;
        for (EntityLivingBase e : nearby) {
            if (e.isDead || e == owner && !ExplosionsConfig.selfDamage) {
                continue;
            }
            if (affect(world, e, owner, x, y, z)) {
                affected++;
            }
        }
        return affected;
    }

    /**
     * How strongly the flash reaches one entity: line of sight, falloff and, for a player, where they are looking.
     * Reads the world only, so scenarios can call it for a player that is not in the world.
     *
     * @param world the world
     * @param e     the entity
     * @param x     blast X
     * @param y     blast Y
     * @param z     blast Z
     * @return the intensity, 0 to 1 ({@link FlashMath#intensity})
     */
    public static double intensityAt(World world, EntityLivingBase e, double x, double y, double z) {
        boolean player = e instanceof EntityPlayer;
        double eyeY = e.posY + e.getEyeHeight();
        double visibility = FlashMath.visibility(
            LineOfSight.clear(world, x, y, z, e.posX, eyeY, e.posZ),
            LineOfSight.clear(world, x, y, z, e.posX, e.posY + e.height * 0.5, e.posZ),
            LineOfSight.clear(world, x, y, z, e.posX, e.posY + 0.1, e.posZ));
        if (visibility <= 0.0) {
            return 0.0;
        }
        double dx = x - e.posX;
        double dy = y - eyeY;
        double dz = z - e.posZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double dot = 1.0;
        if (player && distance > 1.0E-6) {
            Vec3 look = e.getLook(1.0F);
            dot = (look.xCoord * dx + look.yCoord * dy + look.zCoord * dz) / distance;
        }
        return FlashMath.intensity(distance, dot, visibility, player);
    }

    /**
     * Applies the flash to one entity: the potions, the mob's lost target, and the white-out for a player.
     *
     * @param world the world
     * @param e     the entity
     * @param owner who threw it, may be {@code null} (unused by the effect, kept for the audit trail)
     * @param x     blast X
     * @param y     blast Y
     * @param z     blast Z
     * @return true when the entity was affected
     */
    public static boolean affect(World world, EntityLivingBase e, EntityLivingBase owner, double x, double y,
        double z) {
        boolean player = e instanceof EntityPlayer;
        double intensity = intensityAt(world, e, x, y, z);
        int ticks = FlashMath.effectTicks(intensity);
        if (ticks <= 0) {
            return false;
        }
        if (player) {
            e.addPotionEffect(new PotionEffect(Potion.blindness.id, ticks, 0));
            e.addPotionEffect(new PotionEffect(Potion.confusion.id, FlashMath.nauseaTicks(ticks), 0));
            if (e instanceof EntityPlayerMP && ((EntityPlayerMP) e).playerNetServerHandler != null) {
                GregGunsNetwork.channel()
                    .sendTo(new PacketFlash(ticks), (EntityPlayerMP) e);
            }
        } else {
            e.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, ticks, FlashMath.mobSlownessLevel(intensity)));
            e.addPotionEffect(new PotionEffect(Potion.weakness.id, ticks, 0));
            confuse(e);
        }
        return true;
    }

    /** The mob forgets who it was after. Its senses bring a target back later, once the slowness has done its work. */
    private static void confuse(EntityLivingBase e) {
        e.setRevengeTarget(null);
        if (e instanceof EntityLiving) {
            ((EntityLiving) e).setAttackTarget(null);
        }
        if (e instanceof net.minecraft.entity.EntityCreature) {
            ((net.minecraft.entity.EntityCreature) e).setTarget((Entity) null);
        }
    }
}
