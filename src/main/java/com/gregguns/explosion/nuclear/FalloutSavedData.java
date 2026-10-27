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
        /** 0 for a launcher-scale blast, 1 for a ZPM or better warhead (hot rubble, hot goo). */
        public int grade;
        /** Goo blocks placed so far; capped by the config. */
        public int goo;
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
            t.setInteger("grade", grade);
            t.setInteger("goo", goo);
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
            j.grade = t.getInteger("grade");
            j.goo = t.getInteger("goo");
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
        /** Entries (triples) put back at the front of {@link #blocks} because their chunk was unloaded. */
        public int kept;
        /** World time after which unloaded entries are given up on (the age cap). */
        public long deadline;
        /** Age cap after the first due time: seven in-game days. */
        public static final long MAX_AGE_TICKS = 7L * 24000L;

        NBTTagCompound write() {
            NBTTagCompound t = new NBTTagCompound();
            t.setLong("due", due);
            t.setIntArray("blocks", blocks);
            t.setInteger("cursor", cursor);
            t.setInteger("kept", kept);
            t.setLong("deadline", deadline);
            return t;
        }

        static Decay read(NBTTagCompound t) {
            Decay d = new Decay();
            d.due = t.getLong("due");
            d.blocks = t.getIntArray("blocks");
            d.cursor = Math.max(0, Math.min(t.getInteger("cursor"), d.blocks.length / 3));
            d.kept = Math.max(0, Math.min(t.getInteger("kept"), d.cursor));
            d.deadline = t.hasKey("deadline") ? t.getLong("deadline") : d.due + MAX_AGE_TICKS;
            return d;
        }
    }

    /** Jobs in progress, oldest first. */
    public final List<Job> jobs = new ArrayList<>();
    /** Decay records, oldest first. */
    public final List<Decay> decays = new ArrayList<>();

    /** Positions of fallout goo, packed (x, z, y); the ledger that holds back decay. */
    private final java.util.Set<Long> gooBlocks = new java.util.LinkedHashSet<>();
    /** Goo blocks per chunk, packed (chunkX, chunkZ); derived from {@link #gooBlocks}. */
    private final java.util.Map<Long, Integer> gooChunks = new java.util.HashMap<>();

    static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    static long chunkKey(int x, int z) {
        return ((long) (x >> 4) << 32) | ((z >> 4) & 0xFFFFFFFFL);
    }

    /** Records a goo block. */
    public void addGoo(int x, int y, int z) {
        if (gooBlocks.add(pack(x, y, z))) {
            gooChunks.merge(chunkKey(x, z), 1, Integer::sum);
            touch();
        }
    }

    /** Forgets a goo block; true when it was in the ledger. */
    public boolean removeGoo(int x, int y, int z) {
        if (!gooBlocks.remove(pack(x, y, z))) {
            return false;
        }
        long key = chunkKey(x, z);
        int left = gooChunks.getOrDefault(key, 1) - 1;
        if (left <= 0) {
            gooChunks.remove(key);
        } else {
            gooChunks.put(key, left);
        }
        touch();
        return true;
    }

    /** @return true when goo still stands in the chunk that holds the block column */
    public boolean gooInChunkOf(int x, int z) {
        return gooChunks.containsKey(chunkKey(x, z));
    }

    /** @return how many goo blocks the ledger holds */
    public int gooCount() {
        return gooBlocks.size();
    }

    /** @param name the storage name; Forge's map storage calls this by reflection */
    public FalloutSavedData(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        jobs.clear();
        decays.clear();
        gooBlocks.clear();
        gooChunks.clear();
        int[] goo = tag.getIntArray("goo");
        for (int i = 0; i + 2 < goo.length; i += 3) {
            addGoo(goo[i], goo[i + 1], goo[i + 2]);
        }
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
        int[] goo = new int[gooBlocks.size() * 3];
        int i = 0;
        for (long k : gooBlocks) {
            goo[i++] = (int) (k >> 38) << 6 >> 6; // sign-extend the 26-bit x
            goo[i++] = (int) (k & 0xFFF);
            goo[i++] = (int) (k >> 12) << 6 >> 6;
        }
        tag.setIntArray("goo", goo);
    }

    /** Marks the record changed so the next world save writes it. */
    public void touch() {
        markDirty();
    }
}
