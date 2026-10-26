package com.gregguns.explosion.nuclear;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSavedData;

/**
 * The per-dimension save record of fallout: the jobs still converting and the decay records still counting down
 * (D21: "decay is a per-chunk countdown kept as a record", here per blast). Plain data plus NBT; {@link FalloutEngine}
 * owns the logic. A job saved mid-scan resumes at its cursor.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md (save and load)
public final class FalloutSavedData extends WorldSavedData {

    /** The map-storage name. */
    public static final String NAME = "gregguns_fallout";

    /** One blast's conversion in progress. */
    public static final class Job {

        /** Ground zero. */
        public int x, y, z;
        /** Fallout radius, blocks. */
        public int radius;
        /** Chance at ground zero. */
        public double peak;
        /** Hard cap on converted blocks. */
        public int budget;
        /** Deterministic roll seed. */
        public long seed;
        /** Ticks to wait before starting (the crater finishes first). */
        public int wait;
        /** Index of the next column of the (2r+1)^2 square. */
        public int cursor;
        /** Converted block positions so far, x y z triples. */
        public int[] converted = new int[0];
        /** How many ints of {@link #converted} are in use. */
        public int count;

        /** @return blocks converted so far */
        public int blocks() {
            return count / 3;
        }

        void add(int bx, int by, int bz) {
            if (count + 3 > converted.length) {
                converted = java.util.Arrays.copyOf(converted, Math.max(48, converted.length * 2));
            }
            converted[count++] = bx;
            converted[count++] = by;
            converted[count++] = bz;
        }

        NBTTagCompound write() {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("x", x);
            t.setInteger("y", y);
            t.setInteger("z", z);
            t.setInteger("radius", radius);
            t.setDouble("peak", peak);
            t.setInteger("budget", budget);
            t.setLong("seed", seed);
            t.setInteger("wait", wait);
            t.setInteger("cursor", cursor);
            t.setIntArray("converted", java.util.Arrays.copyOf(converted, count));
            return t;
        }

        static Job read(NBTTagCompound t) {
            Job j = new Job();
            j.x = t.getInteger("x");
            j.y = t.getInteger("y");
            j.z = t.getInteger("z");
            j.radius = Math.max(1, t.getInteger("radius"));
            j.peak = t.getDouble("peak");
            j.budget = t.getInteger("budget");
            j.seed = t.getLong("seed");
            j.wait = t.getInteger("wait");
            j.cursor = Math.max(0, t.getInteger("cursor"));
            j.converted = t.getIntArray("converted");
            j.count = j.converted.length - j.converted.length % 3;
            return j;
        }
    }

    /** A finished blast's irradiated blocks waiting to decay. */
    public static final class Decay {

        /** World time at which reverting starts. */
        public long due;
        /** Block positions as x y z triples. */
        public int[] blocks = new int[0];
        /** Index (in triples) of the next block to revert. */
        public int cursor;

        NBTTagCompound write() {
            NBTTagCompound t = new NBTTagCompound();
            t.setLong("due", due);
            t.setIntArray("blocks", blocks);
            t.setInteger("cursor", cursor);
            return t;
        }

        static Decay read(NBTTagCompound t) {
            Decay d = new Decay();
            d.due = t.getLong("due");
            d.blocks = t.getIntArray("blocks");
            d.cursor = Math.max(0, Math.min(t.getInteger("cursor"), d.blocks.length / 3));
            return d;
        }
    }

    /** Jobs in progress, oldest first. */
    public final List<Job> jobs = new ArrayList<>();
    /** Decay records, oldest first. */
    public final List<Decay> decays = new ArrayList<>();

    /** @param name the storage name; Forge's map storage calls this by reflection */
    public FalloutSavedData(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        jobs.clear();
        decays.clear();
        NBTTagList j = tag.getTagList("jobs", 10);
        for (int i = 0; i < j.tagCount(); i++) {
            jobs.add(Job.read(j.getCompoundTagAt(i)));
        }
        NBTTagList d = tag.getTagList("decays", 10);
        for (int i = 0; i < d.tagCount(); i++) {
            decays.add(Decay.read(d.getCompoundTagAt(i)));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList j = new NBTTagList();
        for (Job job : jobs) {
            j.appendTag(job.write());
        }
        tag.setTag("jobs", j);
        NBTTagList d = new NBTTagList();
        for (Decay decay : decays) {
            d.appendTag(decay.write());
        }
        tag.setTag("decays", d);
    }

    /** Marks the record changed so the next world save writes it. */
    public void touch() {
        markDirty();
    }
}
