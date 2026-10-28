package com.gregguns.armor.slots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

class ElectricGearReadOnlyTest {

    @Test
    void readingTheChargeNeverCreatesATag() {
        ItemStack stack = new ItemStack((net.minecraft.item.Item) null);
        assertEquals(0.0, ElectricGear.chargeReadOnly(stack));
        assertFalse(stack.hasTagCompound(), "the durability bar must not write NBT");
        assertEquals(0.0, ElectricGear.chargeReadOnly(null));
    }

    @Test
    void readsIc2sChargeKey() {
        ItemStack stack = new ItemStack((net.minecraft.item.Item) null);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setDouble("charge", 12345.0);
        stack.setTagCompound(tag);
        assertEquals(12345.0, ElectricGear.chargeReadOnly(stack));
    }
}
