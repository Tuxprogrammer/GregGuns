package com.gregguns.gun.logic;

/**
 * The persistent state of one gun stack: rounds loaded and the selected camo. Plain data; the server's copy is
 * authoritative and the client's is a mirror that is overwritten by synced NBT.
 *
 * <p>
 * Reload progress and the fire cooldown are deliberately <em>not</em> part of this: they are transient and live in
 * {@link GunController}. Nothing is lost if a player logs out mid-reload, because a reload only moves ammo when it
 * completes.
 *
 * <p>
 * Spec: gun-lifecycle.md section 2 (keys {@code ammo}, {@code camo}).
 */
public final class GunState {

    private int loaded;
    private int camo;

    /** Creates a state with the given rounds and camo 0. The caller is responsible for range; see {@link #full}. */
    public GunState(int loaded) {
        this.loaded = Math.max(0, loaded);
    }

    /** A full gun with camo 0 (what the creative menu and tag-less loot stacks produce). */
    public static GunState full(GunStats stats) {
        return new GunState(stats.magazineSize());
    }

    /**
     * The state a crafting output with item damage {@code damage} stands for: damage is "rounds missing". Damage 0 is
     * a full gun, damage equal to the magazine size is empty.
     */
    public static GunState fromCraftingDamage(GunStats stats, int damage) {
        return new GunState(stats.magazineSize() - Math.max(0, damage));
    }

    /** Rounds currently loaded. */
    public int loaded() {
        return loaded;
    }

    /** True if no round is loaded. */
    public boolean isEmpty() {
        return loaded <= 0;
    }

    /** True if the gun holds a full magazine. */
    public boolean isFull(GunStats stats) {
        return loaded >= stats.magazineSize();
    }

    /** True if at least one shot's worth of rounds is loaded. */
    public boolean canFire(GunStats stats) {
        return loaded >= stats.ammoPerShot();
    }

    /** Sets the rounds, clamped to {@code [0, magazineSize]}. */
    public void setLoaded(GunStats stats, int rounds) {
        loaded = Math.max(0, Math.min(stats.magazineSize(), rounds));
    }

    /** Removes up to {@code rounds} rounds (never below zero) and returns how many were removed. */
    public int use(int rounds) {
        int used = Math.min(loaded, Math.max(0, rounds));
        loaded -= used;
        return used;
    }

    /** Selected camo index; 0 is the default texture. */
    public int camo() {
        return camo;
    }

    /** Sets the camo index (negative values become 0). */
    public void setCamo(int camo) {
        this.camo = Math.max(0, camo);
    }

    /**
     * Cycles the camo forward or backward, wrapping in {@code [0, camoCount)}. Returns the new index; with one or
     * zero variants it returns 0. Unlike Techguns this never touches the rounds (GUN-01).
     */
    public int cycleCamo(int camoCount, boolean back) {
        if (camoCount <= 1) {
            camo = 0;
        } else {
            camo = Math.floorMod(camo + (back ? -1 : 1), camoCount);
        }
        return camo;
    }

    /** Durability bar fill, {@code 1 - loaded / magazineSize}: an empty gun shows an empty bar's worth of damage. */
    public double barFraction(GunStats stats) {
        return 1.0 - (double) loaded / stats.magazineSize();
    }
}
