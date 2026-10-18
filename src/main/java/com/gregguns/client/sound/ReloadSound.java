package com.gregguns.client.sound;

import java.util.function.BooleanSupplier;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A reload sound that follows the player who is reloading and ends when it is told to stop, when the owner's
 * {@code stillValid} check fails (reload canceled, gun switched away), or when the player dies or leaves the world.
 * Client only.
 */
// Spec: docs/techguns-spec/01-core/client-effects-audio.md (TGSound: moving sounds follow their entity)
// Deviation: Techguns' moving sounds never stop on their own (no auto-removal). A reload that is canceled or
// swapped away now cuts its sound off.
@SideOnly(Side.CLIENT)
final class ReloadSound extends MovingSound {

    private final EntityPlayer owner;
    private final BooleanSupplier stillValid;

    ReloadSound(ResourceLocation event, EntityPlayer owner, float volume, BooleanSupplier stillValid) {
        super(event);
        this.owner = owner;
        this.stillValid = stillValid;
        this.volume = volume;
        this.repeat = false;
        moveToOwner();
    }

    /** Ends the sound at the next sound-engine tick. */
    void stop() {
        this.donePlaying = true;
    }

    @Override
    public void update() {
        if (owner.isDead || !stillValid.getAsBoolean()) {
            stop();
            return;
        }
        moveToOwner();
    }

    private void moveToOwner() {
        this.xPosF = (float) owner.posX;
        this.yPosF = (float) (owner.posY + owner.height * 0.5);
        this.zPosF = (float) owner.posZ;
    }
}
