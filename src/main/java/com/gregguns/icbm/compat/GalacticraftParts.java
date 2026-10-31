package com.gregguns.icbm.compat;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The Galacticraft and GTNH rocket parts a missile body and a warhead casing are built from (D69, D328, D343): the
 * engines, fins, nose cones and hull plates of the pack's rockets. They are found by registry name and damage, as the
 * pack's own recipes do, so no Galacticraft class is loaded and the lookup is safe whatever mods are present. Called
 * only from the recipe and bench code of the enabled ICBM module. Common side.
 *
 * <p>
 * A part is named {@code modid:registryName@damage}, which is also the key the NASA Workbench slots match on.
 */
// Source: the registry names and damage values, from the items' own registrations at the pack's tags:
// Galacticraft 3.4.34-GTNH, GalacticraftCore (item.heavyPlating, item.engine, item.rocketFins, item.noseCone):
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/items/GCItems.java#L100-L103
// GalacticraftMars item.heavyNoseCone:
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/planets/asteroids/items/AsteroidsItems.java#L54
// GalacticraftMars item.itemBasicAsteroids (damage 0 reinforcedPlateT3, 1 engineT2, 2 rocketFinsT2):
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/planets/asteroids/items/ItemBasicAsteroids.java#L22
// GalacticraftMars item.null (ItemBasicMars, damage 3 reinforcedPlateT2, the Heavy Duty Plate Tier 2):
// https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/planets/mars/items/ItemBasicMars.java#L22
// NewHorizonsCoreMod 2.9.84 (dreamcraft: HeavyDutyNoseConeTier3/4, HeavyDutyPlateTier4 to 7):
// https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/item/NHItemList.java#L238-L253
// Which rocket takes which part: the GalaxySpace NASA Workbench pages in the recipe dump of the GTNH 2.9.0-RC-2 pack
// (`/gregguns dumprecipes`, D328).
public final class GalacticraftParts {

    /** The Tier 1 rocket engine; the Tier 2 rocket takes two of them. */
    public static final String ENGINE_T1 = "GalacticraftCore:item.engine@0";
    /**
     * The heavy rocket engine (Galacticraft: Planets, "engineT2"), the engine of the pack's Tier 3 and Tier 4 rockets.
     * The T2 and T3 bodies take it. GTNH makes it from reinforced plates whose heavy duty ingot needs an IV Assembler.
     */
    public static final String ENGINE_HEAVY = "GalacticraftMars:item.itemBasicAsteroids@1";
    /** The Tier 1 rocket fins; the Tier 2 rocket takes them too. */
    public static final String FINS_T1 = "GalacticraftCore:item.rocketFins@0";
    /** The heavy rocket fins ("rocketFinsT2"), the fins of the Tier 3 and Tier 4 rockets; the T2 and T3 bodies. */
    public static final String FINS_HEAVY = "GalacticraftMars:item.itemBasicAsteroids@2";

    /**
     * The nose cones and hull plates of each tier's rocket, which a warhead casing of that tier takes (D328): the
     * warhead is the missile's nose, so it carries the rocket's nose cone and its hull. The tier pairs follow the
     * rockets of the pack's NASA Workbench pages: the Tier 1 rocket (HV) for T1, Tier 2 (EV) for T2, Tier 4 (LuV) for
     * T3, Tier 6 (UV) for T4 and Tier 7 (UHV) for T5. The Tier 1 rocket takes item.noseCone and 8 item.heavyPlating;
     * Tier 2 item.noseCone and 10 item.null@3; Tier 4 item.heavyNoseCone and 12 HeavyDutyPlateTier4; Tier 6
     * HeavyDutyNoseConeTier3 and 12 HeavyDutyPlateTier6; Tier 7 HeavyDutyNoseConeTier4 and 12 HeavyDutyPlateTier7.
     */
    public static final String NOSE_T1 = "GalacticraftCore:item.noseCone@0";
    /** The Tier 1 rocket's hull plate, Galacticraft's Heavy Duty Plate. */
    public static final String HULL_T1 = "GalacticraftCore:item.heavyPlating@0";
    /**
     * The Tier 2 rocket's nose cone: the Tier 1 rocket's, which the T1 and T2 casings take (D343: the heavy nose cone
     * needs IV, past the T2 casing's EV floor).
     */
    public static final String NOSE_T2 = NOSE_T1;
    /** The heavy nose cone of the Tier 3 and Tier 4 rockets (IV), which the T3 casing takes. */
    public static final String NOSE_HEAVY = "GalacticraftMars:item.heavyNoseCone@0";
    /** The Tier 2 rocket's hull plate, Heavy Duty Plate Tier 2. */
    public static final String HULL_T2 = "GalacticraftMars:item.null@3";
    /** The Tier 4 rocket's hull plate (NewHorizonsCoreMod, LuV): the T3 casing's hull and the T3 body's plates. */
    public static final String HULL_T3 = "dreamcraft:HeavyDutyPlateTier4@0";
    /** The Tier 5 and Tier 6 rockets' nose cone (NewHorizonsCoreMod). */
    public static final String NOSE_T4 = "dreamcraft:HeavyDutyNoseConeTier3@0";
    /** The Tier 6 rocket's hull plate (NewHorizonsCoreMod). */
    public static final String HULL_T4 = "dreamcraft:HeavyDutyPlateTier6@0";
    /** The Tier 7 and Tier 8 rockets' nose cone (NewHorizonsCoreMod). */
    public static final String NOSE_T5 = "dreamcraft:HeavyDutyNoseConeTier4@0";
    /** The Tier 7 rocket's hull plate (NewHorizonsCoreMod). */
    public static final String HULL_T5 = "dreamcraft:HeavyDutyPlateTier7@0";
    /**
     * Galacticraft's reinforced plate (the Tier 3 rocket's hull), the dev stand-in for the NewHorizonsCoreMod hulls.
     */
    public static final String HULL_REINFORCED = "GalacticraftMars:item.itemBasicAsteroids@0";

    /** The unformed Galacticraft launch pad block (eight of the nine blocks of a pad; meta 0 is the rocket pad). */
    public static final String PAD = "GalacticraftCore:tile.landingPad";
    /** The 8 outer blocks of a formed pad: Galacticraft's multi-block dummy, meta 2 for a launch pad. */
    public static final String PAD_MULTI = "GalacticraftCore:tile.dummyblock";
    /** The formed pad's center block, which holds the pad's tile entity (meta 0 is the rocket pad). */
    public static final String PAD_FULL = "GalacticraftCore:tile.landingPadFull";

    private GalacticraftParts() {}

    private static boolean warned;

    /**
     * The heavy duty plates and nose cones of the Tier 4 to Tier 7 rockets belong to NewHorizonsCoreMod, which every
     * GTNH pack has but the GregGuns dev environment does not load. Without it the T3 body's plates and the casings'
     * hulls are Galacticraft's reinforced plate and the nose cones its heavy nose cone, so the recipes, the bench and
     * the scenarios still exist in development. In a pack this returns the id unchanged.
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
        if (present) {
            return id;
        }
        String fallback = id.equals(NOSE_T4) || id.equals(NOSE_T5) ? NOSE_HEAVY
            : id.startsWith("dreamcraft:HeavyDutyPlateTier") ? HULL_REINFORCED : id;
        if (!fallback.equals(id) && !warned) {
            warned = true;
            com.gregguns.GregGuns.LOG.warn(
                "NewHorizonsCoreMod (dreamcraft) is not loaded: building the T3 body plates and heavy casings from Galacticraft parts");
        }
        return fallback;
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
