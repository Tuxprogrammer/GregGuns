package com.gregguns.icbm.warhead;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;

/**
 * Reads and writes the warhead NBT {@code {explosive, fill}} on stacks and tiles, applying the clamping rules of
 * {@link WarheadData}. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The warhead item)
public final class WarheadStacks {

    /** NBT key: the explosive id (string). */
    public static final String K_EXPLOSIVE = "explosive";
    /** NBT key: the charges loaded (int). */
    public static final String K_FILL = "fill";

    private WarheadStacks() {}

    /**
     * @param casing the casing
     * @param def    an explosive, or null for the default
     * @return the fill cap of this explosive in this casing: the casing cap, or the explosive's own if lower
     */
    public static int cap(WarheadCasing casing, ExplosiveDef def) {
        return def == null ? casing.fillCap() : Math.min(casing.fillCap(), def.maxFill());
    }

    /**
     * @param casing the casing
     * @param tag    a compound holding the keys; may be null
     * @return the safe explosive and fill
     */
    public static WarheadData read(WarheadCasing casing, NBTTagCompound tag) {
        String id = tag == null ? null : tag.getString(K_EXPLOSIVE);
        ExplosiveDef def = id == null || id.isEmpty() ? null : Explosives.get(id);
        // An explosive of another tier cannot sit in this casing; it reads as the default.
        if (def != null && def.tier() != casing.tier()) {
            def = null;
        }
        if (def == null) {
            def = Explosives.get(WarheadData.DEFAULT_EXPLOSIVE);
            if (def == null || def.tier() != casing.tier()) {
                def = firstOfTier(casing);
            }
        }
        int fill = tag != null && tag.hasKey(K_FILL) ? tag.getInteger(K_FILL) : casing.fillCap();
        String resolved = def == null ? WarheadData.DEFAULT_EXPLOSIVE : def.id();
        return WarheadData.of(resolved, fill, cap(casing, def), true);
    }

    private static ExplosiveDef firstOfTier(WarheadCasing casing) {
        for (ExplosiveDef d : Explosives.all()) {
            if (d.tier() == casing.tier()) {
                return d;
            }
        }
        return null;
    }

    /**
     * @param casing the casing
     * @param stack  a warhead stack
     * @return its safe data
     */
    public static WarheadData read(WarheadCasing casing, ItemStack stack) {
        return read(casing, stack == null ? null : stack.getTagCompound());
    }

    /**
     * @param tag  the compound to write into
     * @param data the values
     */
    public static void write(NBTTagCompound tag, WarheadData data) {
        tag.setString(K_EXPLOSIVE, data.explosive());
        tag.setInteger(K_FILL, data.fill());
    }

    /**
     * @param item   the warhead item
     * @param casing its casing
     * @param id     the explosive id
     * @param fill   the charges (clamped)
     * @return a new stack holding that warhead
     */
    public static ItemStack make(net.minecraft.item.Item item, WarheadCasing casing, String id, int fill) {
        ItemStack stack = new ItemStack(item);
        NBTTagCompound tag = new NBTTagCompound();
        ExplosiveDef def = Explosives.get(id);
        write(tag, WarheadData.of(id, fill, cap(casing, def), def != null));
        stack.setTagCompound(tag);
        return stack;
    }
}
