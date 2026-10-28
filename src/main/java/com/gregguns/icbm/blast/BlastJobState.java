package com.gregguns.icbm.blast;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Everything about a running blast that must survive a save: what it is, where, how far along, and the crater still to
 * break. Plain data plus NBT; the world-touching {@link BlastJob} owns one. A blast saved before its crater is decided
 * restarts from the snapshot on load (nothing was lost: no block had changed); one saved while breaking resumes at its
 * cursor.
 *
 * <p>
 * Avoids: ICBM-02-20 and ICBM-02-76 (counters are saved as full ints, never bytes), ICBM-02-46 (stage and progress are
 * saved, so a reload resumes instead of restarting), ICBM-02-80 (the radius is saved as saved, not recomputed).
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-engine.md (save and load)
public final class BlastJobState {

    /** Stages of a blast, in order. */
    public enum Stage {
        /** Copying the resistance cube on the server thread. */
        SNAPSHOT,
        /** The worker thread is ray-casting. */
        CAST,
        /** The cast crater is posted to listeners and filtered in batches under the budget (I3). */
        DECIDE,
        /** Terrain breaks in slices under the shared budget. */
        APPLY,
        /** Terrain done; the type's effect may still tick. */
        EFFECT,
        /** Nothing left to do. */
        DONE
    }

    /** Registry id. */
    public String explosiveId = "";
    /** Center X. */
    public double x;
    /** Center Y. */
    public double y;
    /** Center Z. */
    public double z;
    /** Warhead fill. */
    public int fill = 1;
    /** Final radius, blocks. */
    public double radius;
    /** Source UUID text, or empty. */
    public String sourceUuid = "";
    /** Source name for a fake player when offline, or empty. */
    public String sourceName = "";
    /** Ticks since detonation. */
    public int age;
    /** Current stage. */
    public Stage stage = Stage.SNAPSHOT;
    /** Whether the entity damage pass already ran. */
    public boolean damaged;
    /** Whether the effect's {@code onDetonate} already ran. */
    public boolean effectStarted;
    /** Whether the effect still wants ticks. */
    public boolean effectActive = true;
    /** Whether terrain breaks at all (decided at detonation). */
    public boolean breaksTerrain;
    /** Cap on the center damage (D154, launcher warheads), 0 for none. */
    public double damageCap;
    /** Packed offsets still to break, nearest first. */
    public int[] edits = new int[0];
    /** Index of the next edit to apply. */
    public int cursor;
    /**
     * While {@link Stage#DECIDE}: how many of {@link #edits} (the front of the array) survived the listeners so far.
     */
    public int kept;
    /** The effect's own state. */
    public NBTTagCompound effectState = new NBTTagCompound();

    /** @return edits not yet applied */
    public int remainingEdits() {
        return Math.max(0, edits.length - cursor);
    }

    /**
     * @param tag the compound to fill
     * @return the same compound
     */
    public NBTTagCompound write(NBTTagCompound tag) {
        tag.setString("explosive", explosiveId);
        tag.setDouble("x", x);
        tag.setDouble("y", y);
        tag.setDouble("z", z);
        tag.setInteger("fill", fill);
        tag.setDouble("radius", radius);
        tag.setString("sourceUuid", sourceUuid);
        tag.setString("sourceName", sourceName);
        tag.setInteger("age", age);
        // Before the crater is decided nothing changed in the world, so the safe thing to save is "start over". A blast
        // saved mid-decide keeps its cast result and cursors instead: re-casting a nuclear crater costs real time.
        Stage saved = stage == Stage.CAST ? Stage.SNAPSHOT : stage;
        tag.setInteger("stage", saved.ordinal());
        tag.setBoolean("damaged", damaged);
        tag.setBoolean("effectStarted", effectStarted);
        tag.setBoolean("effectActive", effectActive);
        tag.setBoolean("breaksTerrain", breaksTerrain);
        tag.setDouble("damageCap", damageCap);
        boolean keepEdits = saved == Stage.APPLY || saved == Stage.DECIDE;
        tag.setIntArray("edits", keepEdits ? edits : new int[0]);
        tag.setInteger("cursor", keepEdits ? cursor : 0);
        tag.setInteger("kept", saved == Stage.DECIDE ? kept : 0);
        tag.setTag("effectState", effectState.copy());
        return tag;
    }

    /**
     * @param tag a compound written by {@link #write}
     * @return the state it holds; unknown stage numbers fall back to the snapshot
     */
    public static BlastJobState read(NBTTagCompound tag) {
        BlastJobState s = new BlastJobState();
        s.explosiveId = tag.getString("explosive");
        s.x = tag.getDouble("x");
        s.y = tag.getDouble("y");
        s.z = tag.getDouble("z");
        s.fill = Math.max(1, tag.getInteger("fill"));
        s.radius = tag.getDouble("radius");
        s.sourceUuid = tag.getString("sourceUuid");
        s.sourceName = tag.getString("sourceName");
        s.age = tag.getInteger("age");
        int st = tag.getInteger("stage");
        s.stage = st >= 0 && st < Stage.values().length ? Stage.values()[st] : Stage.SNAPSHOT;
        s.damaged = tag.getBoolean("damaged");
        s.effectStarted = tag.getBoolean("effectStarted");
        s.effectActive = tag.getBoolean("effectActive");
        s.breaksTerrain = tag.getBoolean("breaksTerrain");
        s.damageCap = Math.max(0.0, tag.getDouble("damageCap"));
        s.edits = tag.getIntArray("edits");
        s.cursor = Math.max(0, Math.min(tag.getInteger("cursor"), s.edits.length));
        s.kept = Math.max(0, Math.min(tag.getInteger("kept"), s.cursor));
        s.effectState = tag.hasKey("effectState", 10) ? tag.getCompoundTag("effectState") : new NBTTagCompound();
        return s;
    }
}
