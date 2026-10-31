package com.gregguns.client.icbm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.ResourceLocation;

import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.FlightPhase;
import com.gregguns.icbm.missile.MissileEngineMath;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The missile's engine: Galacticraft's own rocket loop ({@code galacticraftcore:shuttle.shuttle}), played by event
 * name from Galacticraft's registered sounds (we ship none of its audio). It follows the missile, rises in pitch
 * through the countdown, runs at full pitch in flight, and ends when the missile is removed (impact, interception,
 * despawn, chunk unload), when an EMP fries its guidance (no more thrust, D342) or when the world changes. Client only.
 */
// Source:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/client/sounds/SoundUpdaterRocket.java
// (a repeating MovingSound on "shuttle.shuttle" that follows the rocket, pitch 0 rising to 1 during the ignited
// countdown, ended through donePlaying when the rocket is dead)
// Source:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/resources/assets/galacticraftcore/sounds.json
// Deviation: Galacticraft's loop is non-attenuating and fades with altitude because the player rides the rocket. A
// missile is heard from the ground, so this one fades with distance instead.
@SideOnly(Side.CLIENT)
public final class MissileEngineSound extends MovingSound {

    private final EntityMissile missile;

    private MissileEngineSound(EntityMissile missile) {
        super(new ResourceLocation(MissileEngineMath.EVENT));
        this.missile = missile;
        this.field_147666_i = ISound.AttenuationType.LINEAR;
        this.volume = MissileEngineMath.VOLUME;
        this.field_147663_c = 0.0F;
        this.repeat = true;
        this.field_147665_h = 0;
        follow();
    }

    /**
     * Starts the loop for a missile, if Galacticraft's sound is registered. A missing sound plays nothing and raises
     * nothing.
     *
     * @return the sound, or null when none could be started
     */
    public static MissileEngineSound start(EntityMissile missile) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getSoundHandler() == null
                || mc.getSoundHandler()
                    .getSound(new ResourceLocation(MissileEngineMath.EVENT)) == null) {
                return null;
            }
            MissileEngineSound sound = new MissileEngineSound(missile);
            mc.getSoundHandler()
                .playSound(sound);
            return sound;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Ends the loop at the next sound-engine tick. */
    public void stop() {
        this.donePlaying = true;
    }

    /** @return true once the loop has ended */
    public boolean finished() {
        return donePlaying;
    }

    @Override
    public void update() {
        // An entity that left the client world (killed, or its chunk unloaded) may not read as dead; look it up.
        if (missile.isDead || missile.worldObj == null
            || missile.worldObj.getEntityByID(missile.getEntityId()) != missile) {
            stop();
            return;
        }
        FlightPhase ph = missile.phase();
        if (!MissileEngineMath.sounding(ph, missile.guidanceFried())) {
            stop();
            return;
        }
        this.field_147663_c = ph == FlightPhase.WARMUP
            ? MissileEngineMath.warmupPitch(missile.warmupTick(), missile.warmupTotal())
            : 1.0F;
        follow();
    }

    private void follow() {
        this.xPosF = (float) missile.posX;
        this.yPosF = (float) missile.posY;
        this.zPosF = (float) missile.posZ;
    }
}
