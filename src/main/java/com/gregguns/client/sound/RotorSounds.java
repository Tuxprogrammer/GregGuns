package com.gregguns.client.sound;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import com.gregguns.client.anim.RotorSoundCue;
import com.gregguns.gun.sound.GunSoundEvents;
import com.gregguns.gun.sound.GunSoundMath;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The wind-up and wind-down sounds of spinning rotors (the Minigun), one {@link RotorSoundCue} per shooter, driven by
 * the rotor speed that the client animation state already tracks: the local player's rotor follows the trigger, other
 * shooters' rotors follow their shots. The sounds follow the shooter, and every one is cut off when the rotor is
 * stopped dead, the gun is switched away, the shooter dies, or the world unloads ({@link #stopAll()}); a sound that
 * lost its owner ends by itself. Client only; main thread. No packets: it plays on the client that sees the rotor
 * spin, so a dedicated server never touches it.
 */
// Spec: docs/gregguns-spec/02-guns/index.md (PDW, Pulse Rifle and Minigun: Minigun, "Spin sounds"); D346
@SideOnly(Side.CLIENT)
public final class RotorSounds {

    private static final class Entry {

        final RotorSoundCue cue = new RotorSoundCue();
        RotorSound playing;
        boolean seen;
    }

    private final Map<Integer, Entry> entries = new HashMap<>();

    /**
     * Feeds one shooter's rotor speed for this tick.
     *
     * @param holder   the shooter
     * @param gunName  the held gun's content name, the sound event prefix ({@code guns.<name>.spin_up})
     * @param fraction the rotor speed over its full speed, 0 to 1
     */
    public void update(EntityLivingBase holder, String gunName, double fraction) {
        Entry e = entries.get(holder.getEntityId());
        if (e == null) {
            if (fraction <= 0.0) {
                return;
            }
            e = new Entry();
            entries.put(holder.getEntityId(), e);
        }
        e.seen = true;
        switch (e.cue.next(fraction)) {
            case SPIN_UP:
                play(e, holder, GunSoundEvents.gun(gunName, GunSoundEvents.SPIN_UP), 1.0f);
                break;
            case SPIN_DOWN:
                // A wind-down from part speed is quieter, in proportion.
                play(e, holder, GunSoundEvents.gun(gunName, GunSoundEvents.SPIN_DOWN), (float) (0.4 + 0.6 * fraction));
                break;
            case STOP:
                stop(e);
                break;
            default:
                break;
        }
    }

    /** Call once after every shooter was fed: shooters that were not fed this tick lose their sounds. */
    public void endTick() {
        Iterator<Entry> it = entries.values()
            .iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (!e.seen) {
                stop(e);
                it.remove();
            } else {
                e.seen = false;
            }
        }
    }

    /** Cuts every rotor sound and forgets every shooter (world unload, disconnect). */
    public void stopAll() {
        for (Entry e : entries.values()) {
            stop(e);
        }
        entries.clear();
    }

    private static void play(Entry e, EntityLivingBase holder, String event, float volume) {
        stop(e);
        e.playing = new RotorSound(
            new ResourceLocation(GunSoundEvents.qualified(event)),
            holder,
            volume * GunSoundMath.HANDLING_VOLUME);
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(e.playing);
    }

    private static void stop(Entry e) {
        if (e.playing != null) {
            e.playing.stop();
            e.playing = null;
        }
    }
}
