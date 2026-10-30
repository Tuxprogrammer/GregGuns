package com.gregguns.client.sound;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A rotor wind-up or wind-down that follows the holder and ends when told to stop, when the holder dies or is
 * removed, or when it has played through (it never repeats, so a leaked one ends by itself). Client only.
 */
// Spec: docs/gregguns-spec/02-guns/index.md (Minigun, spin sounds)
@SideOnly(Side.CLIENT)
final class RotorSound extends MovingSound {

    private final EntityLivingBase owner;

    RotorSound(ResourceLocation event, EntityLivingBase owner, float volume) {
        super(event);
        this.owner = owner;
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
        if (owner.isDead || owner.worldObj == null) {
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
