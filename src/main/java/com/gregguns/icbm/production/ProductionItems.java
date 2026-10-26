package com.gregguns.icbm.production;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the Missile Chips and Missile Schematics (one item per tier) and holds them for the recipes. Common side,
 * preInit.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (Chip and schematic)
public final class ProductionItems {

    /** PLACEHOLDER art until the guides track delivers licensed icons: the NTM fuze icons stand in. */
    private static final String PLACEHOLDER_ICON = "gregguns:thirdparty/ntm/";

    private static final ItemMissileChip[] CHIPS = new ItemMissileChip[MissileBuildSpec.values().length];
    private static final ItemMissileSchematic[] SCHEMATICS = new ItemMissileSchematic[MissileBuildSpec.values().length];

    private ProductionItems() {}

    /**
     * Registers both items of each tier, whatever the module flag says (content always registers).
     *
     * @param registrar the ICBM module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        IcbmRecipeMaps.init();
        for (MissileBuildSpec spec : MissileBuildSpec.values()) {
            ItemMissileChip chip = registrar.registerItem(ItemMissileChip.name(spec), new ItemMissileChip(spec));
            chip.setTextureName(PLACEHOLDER_ICON + (spec.number() == 1 ? "fuze_assembly_mk1" : "fuze_assembly_mk2"));
            CHIPS[spec.number() - 1] = chip;
            ItemMissileSchematic sch = registrar
                .registerItem(ItemMissileSchematic.name(spec), new ItemMissileSchematic(spec));
            sch.setTextureName(PLACEHOLDER_ICON + (spec.number() == 1 ? "fuze_assembly_mk1" : "fuze_assembly_mk2"));
            SCHEMATICS[spec.number() - 1] = sch;
        }
    }

    /**
     * @param spec a body tier
     * @return that tier's chip item; null before registration
     */
    public static Item chipItem(MissileBuildSpec spec) {
        return CHIPS[spec.number() - 1];
    }

    /**
     * @param spec a body tier
     * @return that tier's schematic item; null before registration
     */
    public static Item schematicItem(MissileBuildSpec spec) {
        return SCHEMATICS[spec.number() - 1];
    }

    /**
     * @param spec a body tier
     * @return one chip of that tier
     */
    public static ItemStack chip(MissileBuildSpec spec) {
        return new ItemStack(chipItem(spec));
    }

    /**
     * @param spec a body tier
     * @return one schematic of that tier
     */
    public static ItemStack schematic(MissileBuildSpec spec) {
        return new ItemStack(schematicItem(spec));
    }
}
