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

    /** GTNH's Heavy Duty Rocket Engine Tier 3 (NewHorizonsCoreMod item), the engine of the Tier 3 rocket. */
    public static final String ENGINE_T3 = "dreamcraft:HeavyDutyRocketEngineTier3@0";
    /** GTNH's Heavy Duty Rocket Fins Tier 3, the fins of the Tier 3 rocket. */
    public static final String FINS_T3 = "dreamcraft:HeavyDutyRocketFinsTier3@0";

    /** The unformed Galacticraft launch pad block (eight of the nine blocks of a pad; meta 0 is the rocket pad). */
    public static final String PAD = "GalacticraftCore:tile.landingPad";
    /** The 8 outer blocks of a formed pad: Galacticraft's multi-block dummy, meta 2 for a launch pad. */
    public static final String PAD_MULTI = "GalacticraftCore:tile.dummyblock";
    /** The formed pad's center block, which holds the pad's tile entity (meta 0 is the rocket pad). */
    public static final String PAD_FULL = "GalacticraftCore:tile.landingPadFull";

    private GalacticraftParts() {}

    /**
     * The Tier 3 parts belong to NewHorizonsCoreMod, which every GTNH pack has but the GregGuns dev environment does
     * not load. Without it the Tier 3 body is built from the Tier 2 parts, so the recipes, the bench and the scenarios
     * still exist in development. In a pack this returns the id unchanged.
     *
     * @param id a part id
     * @return the id to look up in this installation
     */
    public static String resolve(String id) {
        if (!id.startsWith("dreamcraft:")) {
            return id;
        }
        boolean present;
        try {
            present = cpw.mods.fml.common.Loader.isModLoaded("dreamcraft");
        } catch (RuntimeException | LinkageError e) {
            present = true; // outside a running game (unit tests): keep the pack ids
        }
        return present ? id : id.equals(ENGINE_T3) ? ENGINE_T2 : id.equals(FINS_T3) ? FINS_T2 : id;
    }

    /**
     * @param id a block id, {@code modid:registryName}
     * @return the block, or null when the mod or block is not there
     */
    public static net.minecraft.block.Block findBlock(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? null : GameRegistry.findBlock(id.substring(0, colon), id.substring(colon + 1));
    }

    /**
     * @param id    a part id, {@code modid:registryName@damage}
     * @param count stack size
     * @return the stack, or null when the mod or item is not there
     */
    public static ItemStack find(String rawId, int count) {
        String id = resolve(rawId);
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
