package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.def.AkimboPistolsDef;
import com.gregguns.gun.def.AkimboRevolversDef;
import com.gregguns.gun.def.PistolDef;
import com.gregguns.gun.def.RevolverDef;

/**
 * The akimbo combine and split NBT arithmetic: sums and splits rounds, keeps each gun's camo and unrelated keys,
 * round-trips, and does not reproduce GUN-26 (the second gun's camo and tag lost on combine).
 */
// Spec: docs/techguns-spec/02-guns/recipes.md (RecipeAkimboGun, RecipeAkimboSplit); docs/techguns-spec/02-guns/index.md
// GUN-26
class AkimboNbtTest {

    private static final GunStats SINGLE = PistolDef.STATS;
    private static final GunStats PAIR = AkimboPistolsDef.STATS;

    private static NBTTagCompound gun(int ammo, int camo) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setShort(GunNbt.KEY_AMMO, (short) ammo);
        tag.setByte(GunNbt.KEY_CAMO, (byte) camo);
        return tag;
    }

    @Test
    void combineSumsTheRoundsAndTakesTheFirstGunsCamo() {
        NBTTagCompound pair = AkimboNbt.combine(gun(18, 1), gun(7, 2), SINGLE, PAIR);
        GunState state = GunNbt.read(pair, PAIR);
        assertEquals(25, state.loaded());
        assertEquals(1, state.camo());
    }

    @Test
    void theSecondGunsWholeTagRidesAlong() {
        NBTTagCompound second = gun(7, 2);
        second.setInteger("marker", 99);
        NBTTagCompound pair = AkimboNbt.combine(gun(18, 1), second, SINGLE, PAIR);
        NBTTagCompound kept = pair.getCompoundTag(AkimboNbt.KEY_SECOND);
        assertEquals(2, kept.getByte(GunNbt.KEY_CAMO));
        assertEquals(99, kept.getInteger("marker"));
    }

    @Test
    void splitConservesTheRoundsGivingTheOddOneToTheSecondGun() {
        NBTTagCompound pair = AkimboNbt.combine(gun(18, 0), gun(7, 0), SINGLE, PAIR);
        NBTTagCompound[] out = AkimboNbt.split(pair, SINGLE, PAIR);
        assertEquals(
            12,
            GunNbt.read(out[0], SINGLE)
                .loaded());
        assertEquals(
            13,
            GunNbt.read(out[1], SINGLE)
                .loaded());
        assertEquals(
            25,
            GunNbt.read(out[0], SINGLE)
                .loaded()
                + GunNbt.read(out[1], SINGLE)
                    .loaded());
    }

    @Test
    void doesNotReproduceGun26_roundTripKeepsEachGunsCamoAndExtraKeys() {
        NBTTagCompound a = gun(10, 1);
        a.setString("owner", "first");
        NBTTagCompound b = gun(10, 2);
        b.setString("owner", "second");
        NBTTagCompound[] out = AkimboNbt.split(AkimboNbt.combine(a, b, SINGLE, PAIR), SINGLE, PAIR);
        assertEquals(1, out[0].getByte(GunNbt.KEY_CAMO));
        assertEquals("first", out[0].getString("owner"));
        assertEquals(2, out[1].getByte(GunNbt.KEY_CAMO));
        assertEquals("second", out[1].getString("owner"));
        assertEquals(
            10,
            GunNbt.read(out[0], SINGLE)
                .loaded());
        assertEquals(
            10,
            GunNbt.read(out[1], SINGLE)
                .loaded());
        assertFalse(out[0].hasKey(AkimboNbt.KEY_SECOND), "the split gun must not carry the pair's second tag");
        assertFalse(out[1].hasKey(AkimboNbt.KEY_SECOND));
    }

    @Test
    void aRoundTripOfEqualAmmoRestoresEachGunsRounds() {
        for (int ammo : new int[] { 0, 1, 17, 18 }) {
            NBTTagCompound[] out = AkimboNbt
                .split(AkimboNbt.combine(gun(ammo, 0), gun(ammo, 0), SINGLE, PAIR), SINGLE, PAIR);
            assertEquals(
                ammo,
                GunNbt.read(out[0], SINGLE)
                    .loaded(),
                "first gun with " + ammo);
            assertEquals(
                ammo,
                GunNbt.read(out[1], SINGLE)
                    .loaded(),
                "second gun with " + ammo);
        }
    }

    @Test
    void firingBetweenCombineAndSplitSplitsWhatIsLeft() {
        NBTTagCompound pair = AkimboNbt.combine(gun(18, 0), gun(18, 0), SINGLE, PAIR);
        GunState state = GunNbt.read(pair, PAIR);
        state.use(5);
        GunNbt.write(pair, state);
        NBTTagCompound[] out = AkimboNbt.split(pair, SINGLE, PAIR);
        assertEquals(
            15,
            GunNbt.read(out[0], SINGLE)
                .loaded());
        assertEquals(
            16,
            GunNbt.read(out[1], SINGLE)
                .loaded());
    }

    @Test
    void aSplitCamoCycledOnThePairShowsOnTheFirstGunOnly() {
        NBTTagCompound pair = AkimboNbt.combine(gun(18, 0), gun(18, 2), SINGLE, PAIR);
        pair.setByte(GunNbt.KEY_CAMO, (byte) 1);
        NBTTagCompound[] out = AkimboNbt.split(pair, SINGLE, PAIR);
        assertEquals(1, out[0].getByte(GunNbt.KEY_CAMO));
        assertEquals(2, out[1].getByte(GunNbt.KEY_CAMO));
    }

    @Test
    void tagLessGunsReadAsFullGuns() {
        NBTTagCompound pair = AkimboNbt.combine(null, null, SINGLE, PAIR);
        assertEquals(
            36,
            GunNbt.read(pair, PAIR)
                .loaded());
        NBTTagCompound[] out = AkimboNbt.split(null, SINGLE, PAIR);
        assertEquals(
            18,
            GunNbt.read(out[0], SINGLE)
                .loaded());
        assertEquals(
            18,
            GunNbt.read(out[1], SINGLE)
                .loaded());
    }

    @Test
    void aPairWithoutASecondTagSplitsIntoTwoCopiesOfItsOwnCamo() {
        NBTTagCompound[] out = AkimboNbt.split(gun(35, 1), SINGLE, PAIR);
        assertEquals(
            17,
            GunNbt.read(out[0], SINGLE)
                .loaded());
        assertEquals(
            18,
            GunNbt.read(out[1], SINGLE)
                .loaded());
        assertEquals(1, out[0].getByte(GunNbt.KEY_CAMO));
        assertEquals(1, out[1].getByte(GunNbt.KEY_CAMO));
    }

    @Test
    void theInputTagsAreNeverMutated() {
        NBTTagCompound a = gun(18, 1);
        NBTTagCompound b = gun(7, 2);
        NBTTagCompound aBefore = (NBTTagCompound) a.copy();
        NBTTagCompound bBefore = (NBTTagCompound) b.copy();
        NBTTagCompound pair = AkimboNbt.combine(a, b, SINGLE, PAIR);
        assertEquals(aBefore, a);
        assertEquals(bBefore, b);
        NBTTagCompound pairBefore = (NBTTagCompound) pair.copy();
        AkimboNbt.split(pair, SINGLE, PAIR);
        assertEquals(pairBefore, pair);
    }

    @Test
    void revolversCombineTwelveFromSixAndSix() {
        GunStats single = RevolverDef.STATS;
        GunStats pair = AkimboRevolversDef.STATS;
        NBTTagCompound out = AkimboNbt.combine(gun(6, 0), gun(4, 0), single, pair);
        assertEquals(
            10,
            GunNbt.read(out, pair)
                .loaded());
        NBTTagCompound[] back = AkimboNbt.split(out, single, pair);
        assertEquals(
            5,
            GunNbt.read(back[0], single)
                .loaded());
        assertEquals(
            5,
            GunNbt.read(back[1], single)
                .loaded());
        assertTrue(
            GunNbt.read(back[0], single)
                .loaded() <= single.magazineSize());
    }
}
