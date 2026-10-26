package com.gregguns.icbm.production;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.core.registry.ModuleRegistrar;

/** Registers the Missile Chip and the Missile Schematic and holds them for the recipes. Common side, preInit. */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (Chip and schematic)
public final class ProductionItems {

    private static ItemMissileChip chip;
    private static ItemMissileSchematic schematic;

    private ProductionItems() {}

    /**
     * Registers both items, whatever the module flag says (content always registers).
     *
     * @param registrar the ICBM module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        IcbmRecipeMaps.init();
        chip = registrar.registerItem(ItemMissileChip.NAME, new ItemMissileChip());
        schematic = registrar.registerItem(ItemMissileSchematic.NAME, new ItemMissileSchematic());
    }

    /** @return the chip item; null before registration */
    public static Item chipItem() {
        return chip;
    }

    /** @return the schematic item; null before registration */
    public static Item schematicItem() {
        return schematic;
    }

    /**
     * @param spec a body tier
     * @return one chip of that tier
     */
    public static ItemStack chip(MissileBuildSpec spec) {
        return new ItemStack(chip, 1, spec.number() - 1);
    }

    /**
     * @param spec a body tier
     * @return one schematic of that tier
     */
    public static ItemStack schematic(MissileBuildSpec spec) {
        return new ItemStack(schematic, 1, spec.number() - 1);
    }
}
