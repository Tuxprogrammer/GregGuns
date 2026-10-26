package com.gregguns.icbm.control;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Reads and writes a launch target on a GT data stick (D13). The stick keeps the target under one compound tag, with
 * the
 * dimension it was saved in, so a target from another dimension is refused instead of fired at the wrong place. An
 * empty or damaged tag reads as no target, never as (0, 0) (ICBM-04-06). Both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (Data stick)
public final class TargetStick {

    /** The tag the target lives under. */
    public static final String TAG = "gregguns_icbm_target";
    private static final String K_X = "x";
    private static final String K_Z = "z";
    private static final String K_DIM = "dim";

    /** What a stick action came to; one lang key per value, {@code gregguns.icbm.control.stick.<name lowercased>}. */
    public enum Outcome {

        /** The target was copied from the stick. */
        LOADED,
        /** The target was copied to the stick. */
        SAVED,
        /** No data stick is in the slot. */
        NO_STICK,
        /** The stick holds no target. */
        EMPTY_STICK,
        /** The stick's target is from another dimension. */
        WRONG_DIMENSION,
        /** The block has no target to save. */
        NO_TARGET;

        /** @return the lang key of the message */
        public String langKey() {
            return "gregguns.icbm.control.stick." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private TargetStick() {}

    /**
     * Writes a target to a stick's tag.
     *
     * @param stickTag the stick's tag; created by the caller if the stack had none
     * @param target   a set target
     * @param dim      the dimension id
     */
    public static void write(NBTTagCompound stickTag, LaunchTarget target, int dim) {
        NBTTagCompound t = new NBTTagCompound();
        t.setInteger(K_X, target.x());
        t.setInteger(K_Z, target.z());
        t.setInteger(K_DIM, dim);
        stickTag.setTag(TAG, t);
    }

    /**
     * @param stickTag a stick's tag, may be null
     * @return the saved target, or {@link LaunchTarget#NONE} when there is none or it is malformed
     */
    public static LaunchTarget read(NBTTagCompound stickTag) {
        if (stickTag == null || !stickTag.hasKey(TAG, 10)) {
            return LaunchTarget.NONE;
        }
        NBTTagCompound t = stickTag.getCompoundTag(TAG);
        if (!t.hasKey(K_X, 3) || !t.hasKey(K_Z, 3)) {
            return LaunchTarget.NONE;
        }
        return LaunchTarget.of(t.getInteger(K_X), t.getInteger(K_Z));
    }

    /**
     * @param stickTag a stick's tag, may be null
     * @param fallback the dimension to report when the tag has none
     * @return the dimension the target was saved in
     */
    public static int dimension(NBTTagCompound stickTag, int fallback) {
        if (stickTag == null || !stickTag.hasKey(TAG, 10)) {
            return fallback;
        }
        NBTTagCompound t = stickTag.getCompoundTag(TAG);
        return t.hasKey(K_DIM, 3) ? t.getInteger(K_DIM) : fallback;
    }
}
