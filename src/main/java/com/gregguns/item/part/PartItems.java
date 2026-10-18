package com.gregguns.item.part;

import com.gregguns.api.Tier;
import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the component items and holds them for recipes and the gun items.
 *
 * <p>
 * Registered so far: {@code part_barrel_hardened} (RC-05) and {@code part_mechanical_2} (RC-11), both LV, plus the
 * Revolver's {@code part_barrel_iron} (RC-04, LV), {@code part_mechanical_1} (RC-03) and {@code part_stock_wooden}
 * (RC-02), both ULV, and the Hand Cannon's {@code part_barrel_stone} (RC-01, ULV). The rest of the component index
 * arrives with the guns that need it. Names match the texture files.
 *
 * <p>
 * Usage: {@code new ItemStack(PartItems.barrelHardened(), 1)}. Spec: docs/gregguns-spec/06-items/recipes.md. Common
 * side, preInit.
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

    private static ItemPart barrelStone;
    /** Registry name of the Iron Receiver. */
    public static final String RECEIVER_IRON = "part_receiver_iron";

    private static ItemPart receiverIron;
    private static ItemPart barrelIron;
    private static ItemPart mechanical1;
    private static ItemPart stockWooden;
    private static ItemPart barrelHardened;
    private static ItemPart mechanical2;

    private PartItems() {}

    /**
     * Registers every part item. Call once from {@code GunsModule.preInit}.
     *
     * @param registrar the guns module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        barrelStone = registrar.registerItem(BARREL_STONE, new ItemPart(Tier.STEAM));
        barrelIron = registrar.registerItem(BARREL_IRON, new ItemPart(Tier.LV));
        mechanical1 = registrar.registerItem(MECHANICAL_1, new ItemPart(Tier.STEAM));
        stockWooden = registrar.registerItem(STOCK_WOODEN, new ItemPart(Tier.STEAM));
        barrelHardened = registrar.registerItem(BARREL_HARDENED, new ItemPart(Tier.LV));
        mechanical2 = registrar.registerItem(MECHANICAL_2, new ItemPart(Tier.LV));
        receiverIron = registrar.registerItem(RECEIVER_IRON, new ItemPart(Tier.LV));
    }

    /** @return the Stone Barrel; {@code null} before registration or when the guns module is off */
    public static ItemPart barrelStone() {
        return barrelStone;
    }

    /** @return the Iron Barrel; {@code null} before registration or when the guns module is off */
    public static ItemPart barrelIron() {
        return barrelIron;
    }

    /** @return Mechanical Parts (T1); {@code null} before registration or when the guns module is off */
    public static ItemPart mechanical1() {
        return mechanical1;
    }

    /** @return the Wooden Stock; {@code null} before registration or when the guns module is off */
    public static ItemPart stockWooden() {
        return stockWooden;
    }

    /** @return the Hardened Steel Barrel; {@code null} before registration or when the guns module is off */
    public static ItemPart barrelHardened() {
        return barrelHardened;
    }

    /** @return the Iron Receiver (RC-13); {@code null} before registration or when the guns module is off */
    public static ItemPart receiverIron() {
        return receiverIron;
    }

    /** @return Mechanical Parts T2; {@code null} before registration or when the guns module is off */
    public static ItemPart mechanical2() {
        return mechanical2;
    }
}
