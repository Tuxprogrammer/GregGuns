package com.gregguns.icbm.recipe;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.GregGuns;
import com.gregguns.compat.BartWorksCulture;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gtPlusPlus.core.fluids.GTPPFluids;

/**
 * Turns the {@link Ing} data of the ICBM recipes into real stacks and fluids, after GT's postload when every pack input
 * exists. A missing input answers {@code null} and the caller skips the recipe with an error, except an optional
 * ingredient of an absent mod, which the caller leaves out. Common side.
 */
// Source: GTModHandler.getIC2Item("dynamite" | "industrialTnt", n) is how GT itself reaches the IC2 explosives
// (https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1090-L1094);
// GTPPFluids.Kerosene is the GT++ fluid
// (https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gtPlusPlus/core/fluids/GTPPFluids.java#L328-L329).
// Decompiled: HarvestCraft's registry name {@code mustardseedsItem} (the plantable "Mustard Seeds") comes from the
// jar's
// lang file and textures, because the mod's source repository could not be reached; the lookup is by registry name and
// the recipe omits the seeds when the lookup fails.
public final class IcbmIngredients {

    private IcbmIngredients() {}

    /**
     * @param in an item ingredient
     * @return the stack, or {@code null} when it does not resolve
     */
    public static ItemStack stack(Ing in) {
        int n = in.count();
        switch (in.src()) {
            case GT: {
                int colon = in.id()
                    .indexOf(':');
                String prefix = in.id()
                    .substring(0, colon);
                String material = in.id()
                    .substring(colon + 1);
                OrePrefixes op = OrePrefixes.getPrefix(prefix);
                Materials m = Materials.get(material);
                return op == null || m == null ? null : GTOreDictUnificator.get(op, m, n);
            }
            case TNT:
                return new ItemStack(Blocks.tnt, n);
            case DYNAMITE:
                return GTModHandler.getIC2Item("dynamite", n);
            case INDUSTRIAL_TNT:
                return GTModHandler.getIC2Item("industrialTnt", n);
            case GT_ITEM:
                try {
                    return ItemList.valueOf(in.id())
                        .get(n);
                } catch (IllegalArgumentException e) {
                    GregGuns.LOG.error("No GT ItemList constant {}", in.id());
                    return null;
                }
            case PART: {
                Item item = IcbmParts.get(IcbmPart.valueOf(in.id()));
                return item == null ? null : new ItemStack(item, n);
            }
            case BIO_DISH: {
                ItemStack dish = BartWorksCulture.rottenFleshPetriDish();
                if (dish != null) {
                    dish.stackSize = n;
                }
                return dish;
            }
            case OPTIONAL_MOD_ITEM: {
                int colon = in.id()
                    .indexOf(':');
                String modId = in.id()
                    .substring(0, colon);
                if (!Loader.isModLoaded(modId)) {
                    return null;
                }
                Item item = GameRegistry.findItem(
                    modId,
                    in.id()
                        .substring(colon + 1));
                return item == null ? null : new ItemStack(item, n);
            }
            default:
                return null;
        }
    }

    /**
     * @param in a fluid ingredient
     * @return the fluid stack, or {@code null} when it does not resolve
     */
    public static FluidStack fluid(Ing in) {
        switch (in.src()) {
            case FLUID:
            case GAS: {
                Materials m = Materials.get(in.id());
                if (m == null) {
                    return null;
                }
                // Some GT fluids exist only as a gas or only as a liquid (Sulfuric Gas has a cell and a gas but no
                // liquid form), so each kind falls back to the other.
                FluidStack first = in.src() == Ing.Src.GAS ? m.getGas(in.count()) : m.getFluid(in.count());
                return first != null ? first : in.src() == Ing.Src.GAS ? m.getFluid(in.count()) : m.getGas(in.count());
            }
            case KEROSENE:
                return GTPPFluids.Kerosene == null ? null : new FluidStack(GTPPFluids.Kerosene, in.count());
            default:
                return null;
        }
    }

    /** @return a short name for log lines */
    public static String describe(Ing in) {
        return in.src() + " " + in.id();
    }
}
