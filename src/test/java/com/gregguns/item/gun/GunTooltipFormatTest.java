package com.gregguns.item.gun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.logic.GunCatalogue;
import com.gregguns.gun.logic.GunStats;

class GunTooltipFormatTest {

    @Test
    void pistolNumbersMatchTheSpecSheet() {
        GunStats pistol = GunCatalogue.PISTOL;
        assertEquals("8.0-5.0", GunTooltipFormat.damage(pistol));
        assertEquals("18.0-25.0", GunTooltipFormat.dropOffRange(pistol));
        assertEquals("45", GunTooltipFormat.reach(pistol));
        assertEquals("1.75", GunTooltipFormat.reloadSeconds(pistol));
        assertEquals("0.025", GunTooltipFormat.spread(pistol));
        assertEquals("0", GunTooltipFormat.penetrationPercent(pistol));
    }

    @Test
    void doesNotReproduceGUN19_damageWithoutDropOffShowsOneNumber() {
        GunStats flat = GunStats.builder("flat")
            .damage(6.0)
            .ammoType("x")
            .build();
        assertEquals("6.0", GunTooltipFormat.damage(flat));
    }

    @Test
    void doesNotReproduceGUN18_rangeIsInBlocksNotTicks() {
        // The pistol lives 30 ticks; the range line must show the 18-25 block drop-off and the 45 block reach.
        String range = GunTooltipFormat.dropOffRange(GunCatalogue.PISTOL) + "/"
            + GunTooltipFormat.reach(GunCatalogue.PISTOL);
        assertEquals("18.0-25.0/45", range);
    }

    @Test
    void spreadTrimsTrailingZeros() {
        GunStats half = GunStats.builder("g")
            .spread(0.5)
            .ammoType("x")
            .build();
        assertEquals("0.5", GunTooltipFormat.spread(half));
        GunStats zero = GunStats.builder("z")
            .spread(0.0)
            .ammoType("x")
            .build();
        assertEquals("0", GunTooltipFormat.spread(zero));
    }
}
