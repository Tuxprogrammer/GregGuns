package com.gregguns.recipe.recycling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

/**
 * The Uranium-235 marker that makes the nuclear components radioactive through GT (GGS-06-23): it names exactly those
 * three, and its amount is too small for any GT recycler to return. The live check is {@code recycling.radioactivity}.
 */
class RadiationMarkerTest {

    /** GT's material unit, {@code GTValues.M} at 5.09.54.205. */
    private static final long M = 3_628_800L;

    @Test
    void marksExactlyTheThreeNuclearComponents() {
        assertEquals(
            new HashSet<>(Arrays.asList("cell_nuclear", "part_nuclear_warhead", "part_rad_emitter")),
            RadiationMarker.NAMES);
        assertTrue(RadiationMarker.marks("cell_nuclear"));
        assertFalse(RadiationMarker.marks("pistol_9mm"));
        assertFalse(RadiationMarker.marks("ammo_rocket_nuke"), "the rocket's data is hand-written uranium already");
    }

    @Test
    void theMarkerIsBelowATinyDustANuggetAndAMillibucket() {
        long amount = RadiationMarker.amount(M);
        assertTrue(amount > 0, "a zero amount is no material: GT would not read it");
        // GTOreDictUnificator.getDust and getIngot give a tiny dust or a nugget from 9 x amount >= M.
        assertTrue(amount * 9 < M, "a recycler would return uranium");
        // GTRecipeRegistrator.registerReverseFluidSmelting: (amount * 144) / M millibuckets of the main material.
        assertEquals(0, amount * 144 / M, "the Fluid Extractor would return molten uranium");
    }

    @Test
    void theMarkerIsSmallAgainstAPlate() {
        // The marked parts are made of whole plates (one M each); the marker must sort after every one of them.
        assertTrue(RadiationMarker.amount(M) < M);
    }

    @Test
    void noMarkedNameIsExcludedFromRecycling() {
        for (String name : RadiationMarker.NAMES) {
            assertEquals(null, RecyclingExclusions.reason(name), name + " would get no data, so no marker");
        }
    }
}
