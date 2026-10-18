package com.gregguns.client.sound;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.gregguns.gun.sound.GunSoundEvents;
import com.gregguns.gun.sound.GunSoundMath;
import com.gregguns.gun.sound.ReloadSoundGate;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The shooter's own, locally predicted gun sounds. The client plays them the moment the player acts so they do not wait
 * for the server; the server then broadcasts to everyone <em>except</em> this player
 * ({@code com.gregguns.gun.sound.GunSoundPlayer}), so nothing plays twice. Reach this from common code only through
 * {@code GunsClientModule#sounds()} behind a proxy call.
 *
 * <p>
 * Fire and dry-fire are one-shot sounds at the shooter's muzzle. A reload sound follows the player and is cut off by
 * {@link #stopReload()}, or by the {@code stillValid} check failing (reload cancelled, gun switched away). A second
 * reload sound within {@link ReloadSoundGate#WINDOW_TICKS} ticks of the previous one is suppressed. Client only; main
 * thread.
 */
// Spec: docs/gregguns-spec/03-gun-visuals-audio/gun-sound-pipeline.md;
// docs/techguns-spec/03-gun-visuals-audio/sounds.md
@SideOnly(Side.CLIENT)
public final class ClientGunSounds {

    private final ReloadSoundGate reloadGate = new ReloadSoundGate();
    private ReloadSound activeReload;

    /**
     * Plays the shooter's own shot at the muzzle.
     *
     * @param shooter the local player
     * @param event   an unprefixed event name such as {@code GunSoundEvents.PISTOL_9MM_FIRE}
     */
    public void playFire(EntityPlayer shooter, String event) {
        float yaw = shooter.getRotationYawHead();
        playOneShot(
            GunSoundEvents.qualified(event),
            GunSoundMath.FIRE_VOLUME,
            shooter.posX + GunSoundMath.forwardX(yaw, 1.0),
            shooter.posY + shooter.height * 0.5,
            shooter.posZ + GunSoundMath.forwardZ(yaw, 1.0));
    }

    /**
     * Plays the empty-magazine click (the vanilla fallback) at the shooter.
     *
     * @param shooter the local player
     */
    public void playDryFire(EntityPlayer shooter) {
        playOneShot(
            GunSoundEvents.VANILLA_DRY_FIRE,
            GunSoundMath.DRY_FIRE_VOLUME,
            shooter.posX,
            shooter.posY + shooter.height * 0.5,
            shooter.posZ);
    }

    /**
     * Starts the shooter's reload sound, replacing any reload sound still playing.
     *
     * @param shooter    the local player
     * @param event      an unprefixed reload event name
     * @param stillValid asked every sound tick; return false once the reload is cancelled or the gun is switched away
     * @return true if it started, false if suppressed by the double-play window
     */
    public boolean startReload(EntityPlayer shooter, String event, BooleanSupplier stillValid) {
        if (!reloadGate.tryStart(shooter.ticksExisted)) {
            return false;
        }
        cutOff();
        activeReload = new ReloadSound(
            new ResourceLocation(GunSoundEvents.qualified(event)),
            shooter,
            GunSoundMath.HANDLING_VOLUME,
            stillValid);
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(activeReload);
        return true;
    }

    /** Cuts off the reload sound, if one is playing. Call when a reload is cancelled or the gun is deselected. */
    public void stopReload() {
        cutOff();
        reloadGate.reset();
    }

    private void cutOff() {
        if (activeReload != null) {
            activeReload.stop();
            activeReload = null;
        }
    }

    private static void playOneShot(String qualifiedName, float volume, double x, double y, double z) {
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(
                new PositionedSoundRecord(
                    new ResourceLocation(qualifiedName),
                    volume,
                    1.0f,
                    (float) x,
                    (float) y,
                    (float) z));
    }
}
