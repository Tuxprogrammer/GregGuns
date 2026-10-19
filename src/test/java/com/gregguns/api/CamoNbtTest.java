package com.gregguns.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

class CamoNbtTest {

    @Test
    void nullStackReadsAsDefaultAndWriteIsIgnored() {
        assertEquals(0, CamoNbt.get(null, "camo", 4));
        CamoNbt.set(null, "camo", 4, 2);
    }

    @Test
    void stackWithoutTagReadsAsDefault() {
        assertEquals(0, CamoNbt.get(new ItemStack((net.minecraft.item.Item) null), "camo", 4));
    }

    @Test
    void roundTripsAndClampsOutOfRange() {
        ItemStack stack = new ItemStack((net.minecraft.item.Item) null);
        CamoNbt.set(stack, "camo", 4, 3);
        assertEquals(3, CamoNbt.get(stack, "camo", 4));
        CamoNbt.set(stack, "camo", 4, 9);
        assertEquals(0, CamoNbt.get(stack, "camo", 4));
    }
}
