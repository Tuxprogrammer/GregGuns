package com.gregguns.gun.logic;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/**
 * The NBT arithmetic of the akimbo combine and split recipes: two single guns become one akimbo gun and back. Pure
 * logic over {@link NBTTagCompound}, so it is unit-tested without a world; the recipe classes and the crafted-event
 * handler only feed it stacks' tags.
 *
 * <h2>Combine</h2>
 * The akimbo tag is a copy of the <b>first</b> gun's tag (so its camo shows and any other per-gun key survives),
 * with {@code ammo} set to the sum of both guns' rounds and the <b>second</b> gun's tag stored whole under
 * {@link #KEY_SECOND}. A tag-less gun reads as a full gun ({@link GunNbt#read}), as in Techguns.
 *
 * <h2>Split</h2>
 * The first gun gets the akimbo tag without {@link #KEY_SECOND}, with {@code ammo = floor(total / 2)}; the second gets
 * the stored tag with the remainder, so an odd round goes to the second gun and the total is conserved. An akimbo gun
 * that never had a second tag (found in a chest, or a pre-change save) splits into two copies of its own camo.
 *
 * <p>
 * Deviation: GUN-26. Techguns kept only the first gun's camo on combine, so a split returned two guns with the first
 * gun's skin; and its split event also ran on the client, which showed a ghost item. Here the second gun's whole tag
 * (camo included) rides along and comes back on split, and the extra gun is given on the server only (see
 * {@code AkimboRecipes}). The total rounds are conserved, but they are re-balanced between the two guns (floor half
 * and remainder), exactly as the baseline does.
 *
 * <p>
 * Spec: docs/techguns-spec/02-guns/recipes.md (RecipeAkimboGun, RecipeAkimboSplit), docs/gregguns-spec/02-guns/
 * recipes.md (Akimbo combine and split), GGS-02-07.
 */
public final class AkimboNbt {

    /** Key of the second gun's tag inside the akimbo gun's tag (compound). */
    public static final String KEY_SECOND = "akimbo_second";

    /**
     * IC2's stack-NBT key of an electric item's stored EU. The code never writes it (GGS-02-13); the only read is the
     * durability bar's read-only reader, and the akimbo recipes clear it from a copied tag so they can set the summed
     * or halved charge through IC2 instead.
     */
    public static final String KEY_CHARGE = "charge";

    private AkimboNbt() {}

    /**
     * The charge of the two guns a split makes: half to the first, the odd EU to the second, so the total is
     * conserved (the same rule as the rounds).
     *
     * @param total the akimbo gun's stored EU, 0 or more
     * @return {first, second}
     */
    public static long[] splitCharge(long total) {
        long first = Math.max(0L, total) / 2;
        return new long[] { first, Math.max(0L, total) - first };
    }

    /**
     * Builds the akimbo gun's tag from two single guns' tags.
     *
     * @param first       the first gun's tag (null reads as a full gun)
     * @param second      the second gun's tag (null reads as a full gun)
     * @param singleStats the single gun's stats (clip size)
     * @param akimboStats the akimbo gun's stats (clip size, twice the single clip)
     * @return a new tag, never null
     */
    public static NBTTagCompound combine(NBTTagCompound first, NBTTagCompound second, GunStats singleStats,
        GunStats akimboStats) {
        GunState a = GunNbt.read(first, singleStats);
        GunState b = GunNbt.read(second, singleStats);
        NBTTagCompound out = first == null ? new NBTTagCompound() : (NBTTagCompound) first.copy();
        out.removeTag(KEY_SECOND);
        out.removeTag(KEY_CHARGE); // an energy akimbo gun is charged with the sum by the recipe, through IC2
        NBTTagCompound secondCopy = second == null ? new NBTTagCompound() : (NBTTagCompound) second.copy();
        // A gun combined twice (an akimbo gun is never an input, but be safe) must not nest a stale second tag.
        secondCopy.removeTag(KEY_SECOND);
        secondCopy.removeTag(KEY_CHARGE);
        GunState sum = new GunState(0);
        sum.setLoaded(akimboStats, a.loaded() + b.loaded());
        sum.setCamo(a.camo());
        GunNbt.write(out, sum);
        GunNbt.write(secondCopy, b);
        out.setTag(KEY_SECOND, secondCopy);
        return out;
    }

    /**
     * Splits an akimbo gun's tag into the two single guns' tags.
     *
     * @param akimbo      the akimbo gun's tag (null reads as a full akimbo gun)
     * @param singleStats the single gun's stats
     * @param akimboStats the akimbo gun's stats
     * @return two new tags: index 0 is the gun the recipe's result carries, index 1 the one given besides
     */
    public static NBTTagCompound[] split(NBTTagCompound akimbo, GunStats singleStats, GunStats akimboStats) {
        GunState total = GunNbt.read(akimbo, akimboStats);
        int firstRounds = total.loaded() / 2;
        int secondRounds = total.loaded() - firstRounds;

        NBTTagCompound firstTag = akimbo == null ? new NBTTagCompound() : (NBTTagCompound) akimbo.copy();
        firstTag.removeTag(KEY_SECOND);
        firstTag.removeTag(KEY_CHARGE); // each gun is charged with its half by the recipe, through IC2
        NBTTagCompound secondTag;
        if (akimbo != null && akimbo.hasKey(KEY_SECOND, Constants.NBT.TAG_COMPOUND)) {
            secondTag = (NBTTagCompound) akimbo.getCompoundTag(KEY_SECOND)
                .copy();
        } else {
            secondTag = (NBTTagCompound) firstTag.copy();
        }
        secondTag.removeTag(KEY_CHARGE);
        GunState first = new GunState(0);
        first.setLoaded(singleStats, firstRounds);
        first.setCamo(total.camo());
        GunNbt.write(firstTag, first);

        GunState second = GunNbt.read(secondTag, singleStats);
        second.setLoaded(singleStats, secondRounds);
        GunNbt.write(secondTag, second);
        return new NBTTagCompound[] { firstTag, secondTag };
    }
}
