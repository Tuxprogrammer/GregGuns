package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

class GunNbtTest {

    private static final GunStats PISTOL = GunCatalogue.PISTOL;

    @Test
    void roundTripKeepsAmmoAndCamo() {
        GunState s = new GunState(11);
        s.setCamo(2);
        NBTTagCompound tag = new NBTTagCompound();
        GunNbt.write(tag, s);
        GunState back = GunNbt.read(tag, PISTOL);
        assertEquals(11, back.loaded());
        assertEquals(2, back.camo());
    }

    @Test
    void layoutUsesTheDocumentedKeysAndTypes() {
        NBTTagCompound tag = new NBTTagCompound();
        GunNbt.write(tag, new GunState(9));
        assertTrue(tag.hasKey("ammo", 2), "ammo is a short");
        assertTrue(tag.hasKey("camo", 1), "camo is a byte");
    }

    @Test
    void aMissingTagReadsAsAFullGun() {
        assertEquals(
            18,
            GunNbt.read(null, PISTOL)
                .loaded());
        assertEquals(
            18,
            GunNbt.read(new NBTTagCompound(), PISTOL)
                .loaded());
    }

    @Test
    void missingKeysAreTolerated() {
        NBTTagCompound onlyAmmo = new NBTTagCompound();
        onlyAmmo.setShort("ammo", (short) 4);
        GunState s = GunNbt.read(onlyAmmo, PISTOL);
        assertEquals(4, s.loaded());
        assertEquals(0, s.camo());

        NBTTagCompound onlyCamo = new NBTTagCompound();
        onlyCamo.setByte("camo", (byte) 1);
        GunState c = GunNbt.read(onlyCamo, PISTOL);
        assertEquals(18, c.loaded());
        assertEquals(1, c.camo());
    }

    @Test
    void oldOrOddDataIsClampedAndTypeTolerant() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("ammo", 500);
        assertEquals(
            18,
            GunNbt.read(tag, PISTOL)
                .loaded());
        tag.setInteger("ammo", -3);
        assertEquals(
            0,
            GunNbt.read(tag, PISTOL)
                .loaded());
        tag.setByte("ammo", (byte) 6);
        assertEquals(
            6,
            GunNbt.read(tag, PISTOL)
                .loaded());
        tag.setString("ammo", "junk");
        assertEquals(
            18,
            GunNbt.read(tag, PISTOL)
                .loaded());
    }

    @Test
    void writeLeavesUnrelatedKeysAlone() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("other", "kept");
        GunNbt.write(tag, new GunState(1));
        assertEquals("kept", tag.getString("other"));
    }

    @Test
    void doesNotReproduceGUN01_camoSwitchOnATaglessGunKeepsItsRounds() {
        NBTTagCompound tag = new NBTTagCompound();
        int camo = GunNbt.switchCamo(tag, PISTOL, 3, false);
        assertEquals(1, camo);
        GunState s = GunNbt.read(tag, PISTOL);
        assertEquals(18, s.loaded());
        assertEquals(1, s.camo());
    }

    @Test
    void camoCyclesAndWraps() {
        GunState s = new GunState(1);
        assertEquals(1, s.cycleCamo(3, false));
        assertEquals(2, s.cycleCamo(3, false));
        assertEquals(0, s.cycleCamo(3, false));
        assertEquals(2, s.cycleCamo(3, true));
        assertEquals(0, s.cycleCamo(1, false));
    }

    @Test
    void craftingDamageMeansRoundsMissing() {
        assertEquals(
            18,
            GunState.fromCraftingDamage(PISTOL, 0)
                .loaded());
        assertEquals(
            0,
            GunState.fromCraftingDamage(PISTOL, 18)
                .loaded());
        assertEquals(
            0,
            GunState.fromCraftingDamage(PISTOL, 99)
                .loaded());
        assertEquals(
            1.0,
            GunState.fromCraftingDamage(PISTOL, 18)
                .barFraction(PISTOL),
            1e-12);
        assertEquals(
            0.0,
            GunState.full(PISTOL)
                .barFraction(PISTOL),
            1e-12);
    }

    @Test
    void stateNeverGoesOutOfRange() {
        GunState s = new GunState(5);
        assertEquals(5, s.use(10));
        assertEquals(0, s.loaded());
        s.setLoaded(PISTOL, 99);
        assertEquals(18, s.loaded());
        s.setLoaded(PISTOL, -4);
        assertEquals(0, s.loaded());
    }
}
