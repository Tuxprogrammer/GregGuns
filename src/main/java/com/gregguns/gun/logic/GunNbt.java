package com.gregguns.gun.logic;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/**
 * Codec between {@link GunState} and a stack's {@code NBTTagCompound}. This is the only class in the package that
 * touches Minecraft types.
 *
 * <p>
 * Layout (kept identical to the Techguns spec so imported thinking carries over): {@code ammo} (short, rounds
 * loaded) and {@code camo} (byte, camo index). Readers tolerate a missing tag, missing keys, a different numeric type
 * and out-of-range values from old data (CORE-28); the writer always writes both keys so a partial tag can never
 * appear (GUN-01).
 *
 * <p>
 * Usage: the item reads with {@link #read} on both sides, mutates the {@link GunState}, and on the server writes it
 * back with {@link #write}. The client never writes ammo it computed itself: it takes the server's synced tag.
 */
public final class GunNbt {

    /** Key of the loaded rounds (short). */
    public static final String KEY_AMMO = "ammo";
    /** Key of the camo index (byte). */
    public static final String KEY_CAMO = "camo";

    private GunNbt() {}

    /**
     * Reads the state from a stack tag.
     *
     * @param tag   the stack's tag; null or empty means "a fresh stack" and yields a full gun with camo 0
     * @param stats the gun's stats, used for the magazine size
     * @return a state whose rounds are clamped to {@code [0, magazineSize]}
     */
    public static GunState read(NBTTagCompound tag, GunStats stats) {
        if (tag == null || !tag.hasKey(KEY_AMMO, Constants.NBT.TAG_ANY_NUMERIC)) {
            GunState fresh = GunState.full(stats);
            if (tag != null && tag.hasKey(KEY_CAMO, Constants.NBT.TAG_ANY_NUMERIC)) {
                fresh.setCamo(tag.getByte(KEY_CAMO));
            }
            return fresh;
        }
        GunState state = new GunState(0);
        state.setLoaded(stats, tag.getShort(KEY_AMMO));
        if (tag.hasKey(KEY_CAMO, Constants.NBT.TAG_ANY_NUMERIC)) {
            state.setCamo(tag.getByte(KEY_CAMO));
        }
        return state;
    }

    /** Writes both keys into {@code tag}, replacing existing values and leaving unrelated keys alone. */
    public static void write(NBTTagCompound tag, GunState state) {
        tag.setShort(KEY_AMMO, (short) state.loaded());
        tag.setByte(KEY_CAMO, (byte) state.camo());
    }

    /**
     * Cycles the camo on a stack tag without disturbing the rounds. A tag-less gun keeps the full magazine it would
     * be read as (GUN-01).
     *
     * @return the new camo index
     */
    public static int switchCamo(NBTTagCompound tag, GunStats stats, int camoCount, boolean back) {
        GunState state = read(tag, stats);
        int camo = state.cycleCamo(camoCount, back);
        write(tag, state);
        return camo;
    }
}
