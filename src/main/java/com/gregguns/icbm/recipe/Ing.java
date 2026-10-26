package com.gregguns.icbm.recipe;

import com.gregguns.api.Tier;

/**
 * One ingredient of an ICBM Assembler recipe, as plain data: where it comes from, how many, the earliest tier a
 * player can make it, and a rough cost in plate equivalents for the D34 check. The recipe classes resolve the source
 * to a real stack or fluid; the unit tests read the data. No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Recipes)
public final class Ing {

    /** Where an ingredient comes from. */
    public enum Src {
        /** A GT ore-dict part: {@code id} is {@code prefix:Material}, such as {@code plate:Titanium}. */
        GT,
        /** The vanilla TNT block. */
        TNT,
        /** IC2 dynamite, through GT's IC2 lookup. */
        DYNAMITE,
        /** IC2 Industrial TNT, through GT's IC2 lookup. */
        INDUSTRIAL_TNT,
        /** A named {@code gregtech.api.enums.ItemList} entry, such as {@code Field_Generator_HV}. */
        GT_ITEM,
        /** A GregGuns ICBM part: {@code id} is the {@link IcbmPart} name. */
        PART,
        /** The BartWorks Escherichia petri dish (the rotten-flesh culture). */
        BIO_DISH,
        /** A required item of another mod, {@code modid:name@damage}: a Galacticraft rocket part (D69). */
        MOD_ITEM,
        /** An item of another mod, {@code modid:name}; left out of the recipe when the mod is absent. */
        OPTIONAL_MOD_ITEM,
        /** A GT fluid that is a liquid: {@code id} is the material name. */
        FLUID,
        /** A GT fluid that is a gas: {@code id} is the material name. */
        GAS,
        /** GT++ Kerosene. */
        KEROSENE
    }

    private final Src src;
    private final String id;
    private final int count;
    private final Tier tier;
    private final double cost;

    private Ing(Src src, String id, int count, Tier tier, double cost) {
        this.src = src;
        this.id = id;
        this.count = count;
        this.tier = tier;
        this.cost = cost;
    }

    /**
     * @param prefix   a GT ore prefix name, such as {@code plate}
     * @param material a GT material name, such as {@code Titanium}
     * @param count    how many items
     * @param tier     the earliest tier a player makes it
     * @param unitCost cost of one item, plate equivalents
     * @return the ingredient
     */
    public static Ing gt(String prefix, String material, int count, Tier tier, double unitCost) {
        return new Ing(Src.GT, prefix + ":" + material, count, tier, unitCost * count);
    }

    /**
     * @param count how many TNT blocks
     * @return the ingredient (a toluene and acid chain, MV)
     */
    public static Ing tnt(int count) {
        return new Ing(Src.TNT, "tnt", count, Tier.MV, 0.8 * count);
    }

    /**
     * @param count how many sticks of dynamite
     * @return the ingredient (the nitration chain, LV)
     */
    public static Ing dynamite(int count) {
        return new Ing(Src.DYNAMITE, "dynamite", count, Tier.LV, 1.0 * count);
    }

    /**
     * @param count how many Industrial TNT blocks
     * @return the ingredient (toluene plus nitration mixture, HV)
     */
    public static Ing industrialTnt(int count) {
        return new Ing(Src.INDUSTRIAL_TNT, "industrialTnt", count, Tier.HV, 1.2 * count);
    }

    /**
     * @param name     an {@code ItemList} constant
     * @param count    how many
     * @param tier     the tier of the item
     * @param unitCost cost of one, plate equivalents
     * @return the ingredient
     */
    public static Ing gtItem(String name, int count, Tier tier, double unitCost) {
        return new Ing(Src.GT_ITEM, name, count, tier, unitCost * count);
    }

    /**
     * @param part  an ICBM part
     * @param count how many
     * @return the ingredient, at the part's tier and cost
     */
    public static Ing part(IcbmPart part, int count) {
        return new Ing(Src.PART, part.name(), count, part.tier(), part.unitCost() * count);
    }

    /**
     * @param count how many petri dishes
     * @return the ingredient (the Bio Lab, HV)
     */
    public static Ing bioDish(int count) {
        return new Ing(Src.BIO_DISH, "escherichia", count, Tier.HV, 3.0 * count);
    }

    /**
     * A Galacticraft rocket part the recipe always takes (D69); it fails to resolve when the mod is absent, which
     * skips the recipe.
     *
     * @param id       {@code modid:name@damage}, see {@code GalacticraftParts}
     * @param count    how many
     * @param tier     the earliest tier a player makes it
     * @param unitCost cost of one, plate equivalents
     * @return the ingredient
     */
    public static Ing modItem(String id, int count, Tier tier, double unitCost) {
        return new Ing(Src.MOD_ITEM, id, count, tier, unitCost * count);
    }

    /**
     * An item of another mod that the recipe takes when the mod is there. It costs nothing in the D34 count and has
     * no tier, because it is optional.
     *
     * @param id    {@code modid:name}
     * @param count how many
     * @return the ingredient
     */
    public static Ing optionalMod(String id, int count) {
        return new Ing(Src.OPTIONAL_MOD_ITEM, id, count, Tier.STEAM, 0.0);
    }

    /**
     * @param material a GT material name
     * @param liters   amount in liters
     * @param tier     the earliest tier a player makes it
     * @return a liquid ingredient (1 plate equivalent per 2,000 L)
     */
    public static Ing fluid(String material, int liters, Tier tier) {
        return new Ing(Src.FLUID, material, liters, tier, liters / 2000.0);
    }

    /**
     * @param material a GT material name
     * @param liters   amount in liters
     * @param tier     the earliest tier a player makes it
     * @return a gas ingredient (1 plate equivalent per 2,000 L)
     */
    public static Ing gas(String material, int liters, Tier tier) {
        return new Ing(Src.GAS, material, liters, tier, liters / 2000.0);
    }

    /**
     * @param liters amount in liters
     * @return GT++ Kerosene (Distillery, MV)
     */
    public static Ing kerosene(int liters) {
        return new Ing(Src.KEROSENE, "kerosene", liters, Tier.MV, liters / 2000.0);
    }

    /** @return the source kind */
    public Src src() {
        return src;
    }

    /** @return the source id; see {@link Src} */
    public String id() {
        return id;
    }

    /** @return the count, or the liters for a fluid */
    public int count() {
        return count;
    }

    /** @return the earliest tier a player can make it */
    public Tier tier() {
        return tier;
    }

    /** @return the cost of the whole stack, plate equivalents (D34) */
    public double cost() {
        return cost;
    }

    /** @return true for a fluid ingredient */
    public boolean isFluid() {
        return src == Src.FLUID || src == Src.GAS || src == Src.KEROSENE;
    }

    /** @return true when the recipe leaves this ingredient out if its mod is absent */
    public boolean isOptional() {
        return src == Src.OPTIONAL_MOD_ITEM;
    }
}
