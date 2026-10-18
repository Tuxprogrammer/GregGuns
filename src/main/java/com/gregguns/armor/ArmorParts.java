package com.gregguns.armor;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.gregguns.api.Tier;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.item.part.ItemPart;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The cloth component every T1 set is crafted and repaired with: Heavy Cloth, {@code part_heavy_cloth}, and its
 * hand recipe (RC-21). Registered by the armor module because the armor sets are its only consumers.
 *
 * <p>
 * Common side. Spec: docs/gregguns-spec/06-items/recipes.md (RC-21).
 */
public final class ArmorParts {

    /** Registry name of the Gas Mask Filter (RC-25). */
    public static final String GAS_MASK_FILTER = "part_gas_mask_filter";
    /** Registry name of the Compressed Air Tank. */
    public static final String TANK_AIR = "tank_air";

    private static ItemPart heavyCloth;
    private static ItemPart gasMaskFilter;
    private static ItemPart tankAir;

    private ArmorParts() {}

    /**
     * Registers the parts. Call from {@code ArmorModule.preInit}.
     *
     * @param registrar the armor module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        heavyCloth = registrar.registerItem(ArmorCatalog.HEAVY_CLOTH, new ItemPart(Tier.STEAM));
        // Deviation: area 06 owns both items. The filter is a plain part here (RC-25); the Compressed Air Tank is a
        // plain
        // part too, without the 1,000 mB IFluidContainerItem behavior of GGS-06-40, because the Oxygen Tanks recipe
        // and the refill need an item named tank_air today. When area 06 adds them, delete these two lines and keep
        // the registry names.
        gasMaskFilter = registrar.registerItem(GAS_MASK_FILTER, new ItemPart(Tier.MV));
        tankAir = registrar.registerItem(TANK_AIR, new ItemPart(Tier.HV));
    }

    /** @return Heavy Cloth; null before registration or with the armor module off */
    public static ItemPart heavyCloth() {
        return heavyCloth;
    }

    /** @return the Gas Mask Filter; null before registration or with the armor module off */
    public static ItemPart gasMaskFilter() {
        return gasMaskFilter;
    }

    /** @return the Compressed Air Tank; null before registration or with the armor module off */
    public static ItemPart tankAir() {
        return tankAir;
    }

    /** Adds RC-21: four wool blocks around a leather make three Heavy Cloth. Call from {@code ArmorModule.init}. */
    public static void addRecipes() {
        // Source: Forge 1.7.10 ShapedOreRecipe; "blockWool" is the ore name of every wool color (RC-21, unchanged from
        // Techguns).
        GameRegistry.addRecipe(
            new ShapedOreRecipe(
                new ItemStack(heavyCloth, 3),
                " W ",
                "WLW",
                " W ",
                'W',
                "blockWool",
                'L',
                new ItemStack(Items.leather)));
    }
}
