package com.gregguns.icbm.compat;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The Galacticraft rocket parts a missile body is built from (D69): the Tier 1 and Tier 2 engines and fins. They are
 * found by registry name and damage, as the pack's own recipes do, so no Galacticraft class is loaded and the lookup is
 * safe whatever mods are present. Called only from the recipe and bench code of the enabled ICBM module. Common side.
 *
 * <p>
 * A part is named {@code modid:registryName@damage}, which is also the key the NASA Workbench slots match on.
 */
// Source: the registry names and damage values the pack itself uses for these parts, in NewHorizonsCoreMod's
// https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/gthandler/recipes/AssemblerRecipes.java#L6440-L6560
// (item.engine 0 = Tier 1 engine, item.rocketFins 0 = Tier 1 fins, item.itemBasicAsteroids 1 and 2 = Tier 2 engine and
// fins); the names there match Galacticraft's own item tables at
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/planets/asteroids/items/ItemBasicAsteroids.java#L22-L23
public final class GalacticraftParts {

    /** The Tier 1 rocket engine. */
    public static final String ENGINE_T1 = "GalacticraftCore:item.engine@0";
    /** The Tier 2 rocket engine (Galacticraft: Planets, "engineT2"). */
    public static final String ENGINE_T2 = "GalacticraftMars:item.itemBasicAsteroids@1";
    /** The Tier 1 rocket fins. */
    public static final String FINS_T1 = "GalacticraftCore:item.rocketFins@0";
    /** The Tier 2 rocket fins (Galacticraft: Planets, "rocketFinsT2"). */
    public static final String FINS_T2 = "GalacticraftMars:item.itemBasicAsteroids@2";

    private GalacticraftParts() {}

    /**
     * @param id    a part id, {@code modid:registryName@damage}
     * @param count stack size
     * @return the stack, or null when the mod or item is not there
     */
    public static ItemStack find(String id, int count) {
        int colon = id.indexOf(':');
        int at = id.lastIndexOf('@');
        if (colon < 0 || at < colon) {
            return null;
        }
        Item item = GameRegistry.findItem(id.substring(0, colon), id.substring(colon + 1, at));
        return item == null ? null : new ItemStack(item, count, Integer.parseInt(id.substring(at + 1)));
    }

    /**
     * @param stack any stack, may be null
     * @return its part id, or null when the item is not a registered item
     */
    public static String keyOf(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        return id == null ? null : id.modId + ":" + id.name + "@" + stack.getItemDamage();
    }
}
