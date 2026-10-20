package com.gregguns.gun.sound;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

/**
 * Server-side helper that plays a gun sound positionally to the players near a shooter.
 *
 * <h2>Who hears what</h2>
 * The shooting client predicts its own fire, reload and dry-fire sounds the moment the player acts (see
 * {@code com.gregguns.client.sound.ClientGunSounds}), so it does not wait for the server. The server stays
 * authoritative for everyone else. Every method takes the {@code predictedBy} player, who is excluded from the
 * broadcast so the shooter does not hear the shot twice. Pass {@code null} when nobody predicted the sound (an NPC
 * shooter, a turret): everyone nearby then hears it.
 *
 * <h2>How vanilla 1.7.10 does it</h2>
 * {@code World.playSoundAtEntity(entity, name, volume, pitch)} sends the sound to <em>every</em> player in range,
 * the entity's own player included; with a client-side prediction that doubles the shot.
 * {@code World.playSoundToNearExcept(player, name, volume, pitch)} excludes {@code player}, but it plays the sound at
 * that player's position, so it cannot place a muzzle offset. {@code World.playSoundEffect(x, y, z, ...)} broadcasts
 * from a point with nobody excluded. All of them end in {@code ServerConfigurationManager.sendToAllNear[Except]} with
 * an
 * {@code S29PacketSoundEffect}, reaching players within {@code 16 * max(1, volume)} blocks
 * ({@link GunSoundMath#audibleRange(float)}). A fire sound at volume 4 therefore reaches 64 blocks, and the packet
 * range agrees with the client's linear attenuation range. This class calls {@code sendToAllNearExcept} directly so it
 * can both place the sound and exclude the shooter. It does nothing on a client world.
 *
 * <p>
 * Server only. Call from the main thread.
 */
// Spec: docs/gregguns-spec/03-gun-visuals-audio/gun-sound-pipeline.md; CONTRIBUTING.md (server authority: the client
// predicts only sounds and animations).
// Deviation: Techguns sends its own packet to everyone within 100 blocks and skips the shooter on the client.
// GregGuns reuses the vanilla sound packet and excludes the shooter on the server. The range becomes the vanilla 16 *
// volume blocks (64 for fire), which is what Techguns' volume 4.0 actually carried anyway.
public final class GunSoundPlayer {

    /** How far ahead of the shooter's head yaw a shot sounds from, blocks. */
    public static final double MUZZLE_DISTANCE = 1.0;

    private GunSoundPlayer() {}

    /**
     * Plays a shot from just in front of the shooter, at half its height.
     *
     * @param shooter     the entity that fired (a player or an NPC)
     * @param event       an unprefixed event name such as {@link GunSoundEvents#PISTOL_9MM_FIRE}
     * @param predictedBy the player whose client already played it locally, or {@code null}
     */
    // Deviation: Techguns offsets by (cos, sin) of the head yaw, which points to the shooter's left (CORE-21). This
    // uses the forward vector.
    public static void playFire(Entity shooter, String event, EntityPlayer predictedBy) {
        if (event == null) {
            return; // a gun with no per-shot sound (the continuous beam plays its own)
        }
        float yaw = shooter.getRotationYawHead();
        play(
            shooter.worldObj,
            shooter.posX + GunSoundMath.forwardX(yaw, MUZZLE_DISTANCE),
            shooter.posY + shooter.height * 0.5,
            shooter.posZ + GunSoundMath.forwardZ(yaw, MUZZLE_DISTANCE),
            event,
            GunSoundMath.FIRE_VOLUME,
            1.0f,
            predictedBy);
    }

    /**
     * Plays a reload sound at the shooter. Other players hear it where the reload started; only the shooter's own
     * predicted sound follows them.
     *
     * @param shooter     the reloading entity
     * @param event       an unprefixed reload event name
     * @param predictedBy the player whose client already played it locally, or {@code null}
     */
    // Deviation: Techguns' reload follows the shooter on every client. The vanilla sound packet cannot move a sound,
    // and a 1 to 5 s reload is audible in place; revisit if playtests disagree.
    public static void playReload(Entity shooter, String event, EntityPlayer predictedBy) {
        play(
            shooter.worldObj,
            shooter.posX,
            shooter.posY + shooter.height * 0.5,
            shooter.posZ,
            event,
            GunSoundMath.HANDLING_VOLUME,
            1.0f,
            predictedBy);
    }

    /**
     * Plays the empty-magazine click (the vanilla fallback, {@link GunSoundEvents#VANILLA_DRY_FIRE}).
     *
     * @param shooter     the entity that pulled the trigger
     * @param predictedBy the player whose client already played it locally, or {@code null}
     */
    public static void playDryFire(Entity shooter, EntityPlayer predictedBy) {
        send(
            shooter.worldObj,
            shooter.posX,
            shooter.posY + shooter.height * 0.5,
            shooter.posZ,
            GunSoundEvents.VANILLA_DRY_FIRE,
            GunSoundMath.DRY_FIRE_VOLUME,
            1.0f,
            predictedBy);
    }

    /**
     * Plays a bullet impact at a point. Nobody predicts impacts, so everyone in range hears it.
     *
     * @param world the world
     * @param x     impact X
     * @param y     impact Y
     * @param z     impact Z
     */
    public static void playImpact(World world, double x, double y, double z) {
        play(world, x, y, z, GunSoundEvents.BULLET_IMPACT, GunSoundMath.IMPACT_VOLUME, 1.0f, null);
    }

    /**
     * Plays any registered GregGuns event at a point to the players in range.
     *
     * @param world       the world; nothing happens on a client world
     * @param x           X
     * @param y           Y
     * @param z           Z
     * @param event       an unprefixed event name registered in {@code sounds.json}
     * @param volume      1.0 is normal; above 1 extends the range ({@link GunSoundMath#audibleRange(float)})
     * @param pitch       1.0 is normal
     * @param predictedBy the player to exclude, or {@code null}
     */
    public static void play(World world, double x, double y, double z, String event, float volume, float pitch,
        EntityPlayer predictedBy) {
        send(world, x, y, z, GunSoundEvents.qualified(event), volume, pitch, predictedBy);
    }

    /** Sends an already qualified sound name (vanilla or ours) to the players in range. */
    private static void send(World world, double x, double y, double z, String qualifiedName, float volume, float pitch,
        EntityPlayer predictedBy) {
        if (world == null || world.isRemote) {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return;
        }
        // Source: 1.7.10 WorldManager.playSoundToNearExcept does exactly this call.
        server.getConfigurationManager()
            .sendToAllNearExcept(
                predictedBy,
                x,
                y,
                z,
                GunSoundMath.audibleRange(volume),
                world.provider.dimensionId,
                new S29PacketSoundEffect(qualifiedName, x, y, z, volume, pitch));
    }
}
