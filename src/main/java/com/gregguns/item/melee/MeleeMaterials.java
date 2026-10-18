package com.gregguns.item.melee;

import net.minecraft.item.Item;
import net.minecraftforge.common.util.EnumHelper;

/**
 * The steel tool material of the melee tools: harvest level 2, 1000 uses, efficiency 7.5, damage bonus 2.5,
 * enchantability 12. Created once, on first use during registration. Common side.
 */
// Spec: docs/techguns-spec/06-items/special-items.md (TG_STEEL)
// Source: Forge adds a tool material by reflection on the enum:
// https://github.com/MinecraftForge/MinecraftForge/blob/1.7.10/src/main/java/net/minecraftforge/common/util/EnumHelper.java
final class MeleeMaterials {

    private static Item.ToolMaterial steel;

    private MeleeMaterials() {}

    /** @return the shared steel material */
    static synchronized Item.ToolMaterial steel() {
        if (steel == null) {
            steel = EnumHelper.addToolMaterial(
                "GREGGUNS_STEEL",
                MeleeMath.STEEL_HARVEST_LEVEL,
                MeleeMath.STEEL_DURABILITY,
                MeleeMath.STEEL_EFFICIENCY,
                MeleeMath.STEEL_DAMAGE_BONUS,
                MeleeMath.STEEL_ENCHANTABILITY);
        }
        return steel;
    }
}
