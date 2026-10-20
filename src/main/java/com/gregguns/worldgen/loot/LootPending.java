package com.gregguns.worldgen.loot;

import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.Nullable;

/**
 * An unfilled chest's loot order: which category, how rich, and the per-chest seed. Immutable. Stored in the chest's
 * NBT until the first read of its inventory turns it into items (GGS-11-20, WGEN-25).
 *
 * <p>
 * The seed comes from {@link #seed}: world seed, plan cell and chest index only, so a chest holds the same items
 * whichever chest is opened first, and whichever accessor (player, hopper, GT extraction, block break) touches it
 * first. Pure; no world access.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (GGS-11-20); fixes WGEN-25 (unseeded chest contents)
public final class LootPending {

    /** NBT key of the compound inside the chest tag. */
    public static final String KEY = "GGLootPending";
    private static final String K_CATEGORY = "Category";
    private static final String K_RICHNESS = "Richness";
    private static final String K_SEED = "Seed";

    private final String category;
    private final int richness;
    private final long seed;

    /**
     * @param category a {@code gregguns:*} category name
     * @param richness 0..2 (clamped)
     * @param seed     the per-chest seed from {@link #seed}
     */
    public LootPending(String category, int richness, long seed) {
        this.category = category;
        this.richness = LootDraw.clamp(richness);
        this.seed = seed;
    }

    /** @return the category name */
    public String category() {
        return category;
    }

    /** @return richness 0..2 */
    public int richness() {
        return richness;
    }

    /** @return the per-chest seed */
    public long seed() {
        return seed;
    }

    /** @return a fresh Random for this chest; every call restarts the same sequence */
    public Random random() {
        return new Random(seed);
    }

    /**
     * Derives a chest's seed. Order-independent by construction: it depends on nothing but its arguments.
     *
     * @param worldSeed  the world seed
     * @param cellX      the plan's grid cell X
     * @param cellZ      the plan's grid cell Z
     * @param chestIndex the chest's index inside the structure (stable per layout)
     * @return a well-mixed seed
     */
    public static long seed(long worldSeed, int cellX, int cellZ, int chestIndex) {
        long h = mix(worldSeed ^ 0x9E3779B97F4A7C15L);
        h = mix(h ^ cellX * 0xC2B2AE3D27D4EB4FL);
        h = mix(h ^ cellZ * 0x165667B19E3779F9L);
        return mix(h ^ chestIndex * 0x27D4EB2F165667C5L);
    }

    /** splitmix64 finalizer. */
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** @param tag the chest tag to write into; the pending order goes under {@link #KEY} */
    public void writeTo(NBTTagCompound tag) {
        NBTTagCompound t = new NBTTagCompound();
        t.setString(K_CATEGORY, category);
        t.setByte(K_RICHNESS, (byte) richness);
        t.setLong(K_SEED, seed);
        tag.setTag(KEY, t);
    }

    /**
     * @param tag a chest tag
     * @return the pending order, or null when the chest has none (or its category is blank)
     */
    public static @Nullable LootPending readFrom(NBTTagCompound tag) {
        if (!tag.hasKey(KEY, 10)) {
            return null;
        }
        NBTTagCompound t = tag.getCompoundTag(KEY);
        String category = t.getString(K_CATEGORY);
        if (category.isEmpty()) {
            return null;
        }
        return new LootPending(category, t.getByte(K_RICHNESS), t.getLong(K_SEED));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LootPending)) {
            return false;
        }
        LootPending p = (LootPending) o;
        return richness == p.richness && seed == p.seed && category.equals(p.category);
    }

    @Override
    public int hashCode() {
        return category.hashCode() * 31 + richness + Long.hashCode(seed) * 17;
    }
}
