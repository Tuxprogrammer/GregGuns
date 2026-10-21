package com.gregguns.item.part;

import java.util.LinkedHashMap;
import java.util.Map;

import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the component items of {@link PartDefinitions#ALL} and holds them for recipes and the gun items. The list
 * of parts is the definitions' (one entry per part); the name constants and accessors below are conveniences for the
 * parts that existing code names, and a new part needs none of them: look it up with {@link #get(String)}.
 *
 * <p>
 * Usage: {@code new ItemStack(PartItems.barrelHardened(), 1)} or {@code new ItemStack(PartItems.get("part_x"), 1)}.
 * Spec: docs/gregguns-spec/06-items/recipes.md. Common side, preInit.
 */
public final class PartItems {

    /** Registry name of the Hardened Steel Barrel. */
    public static final String BARREL_HARDENED = "part_barrel_hardened";
    /** Registry name of Mechanical Parts T2. */
    public static final String MECHANICAL_2 = "part_mechanical_2";

    /** Registry name of the Stone Barrel. */
    public static final String BARREL_STONE = "part_barrel_stone";
    /** Registry name of the Iron Barrel. */
    public static final String BARREL_IRON = "part_barrel_iron";
    /** Registry name of Mechanical Parts (T1). */
    public static final String MECHANICAL_1 = "part_mechanical_1";
    /** Registry name of the Wooden Stock. */
    public static final String STOCK_WOODEN = "part_stock_wooden";
    /** Registry name of the Iron Receiver. */
    public static final String RECEIVER_IRON = "part_receiver_iron";

    private static final Map<String, ItemPart> ITEMS = new LinkedHashMap<>();

    private PartItems() {}

    /**
     * Registers every part in {@link PartDefinitions#ALL}. Call once from {@code GunsModule.preInit}.
     *
     * @param registrar the guns module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        for (PartDefinition part : PartDefinitions.ALL) {
            registerOne(registrar, part.name());
        }
    }

    /**
     * Registers one part by name, so the caller picks the registration order (see {@code GunsItemOrder}).
     *
     * @param registrar the guns module's registrar
     * @param name      a part's registry name from {@link PartDefinitions#ALL}
     */
    public static void registerOne(ModuleRegistrar registrar, String name) {
        ITEMS.put(
            name,
            registrar.registerItem(
                name,
                new ItemPart(
                    PartDefinitions.byName(name)
                        .tier())));
    }

    /**
     * @param name a part's registry name without the mod id
     * @return the registered part; {@code null} before registration, or for an unknown
     *         name
     */
    public static ItemPart get(String name) {
        return ITEMS.get(name);
    }

    /** @return the Stone Barrel; {@code null} before registration */
    public static ItemPart barrelStone() {
        return get(BARREL_STONE);
    }

    /** @return the Iron Barrel; {@code null} before registration */
    public static ItemPart barrelIron() {
        return get(BARREL_IRON);
    }

    /** @return Mechanical Parts (T1); {@code null} before registration */
    public static ItemPart mechanical1() {
        return get(MECHANICAL_1);
    }

    /** @return the Wooden Stock; {@code null} before registration */
    public static ItemPart stockWooden() {
        return get(STOCK_WOODEN);
    }

    /** @return the Hardened Steel Barrel; {@code null} before registration */
    public static ItemPart barrelHardened() {
        return get(BARREL_HARDENED);
    }

    /** @return the Iron Receiver (RC-13); {@code null} before registration */
    public static ItemPart receiverIron() {
        return get(RECEIVER_IRON);
    }

    /** @return Mechanical Parts T2; {@code null} before registration */
    public static ItemPart mechanical2() {
        return get(MECHANICAL_2);
    }
}
