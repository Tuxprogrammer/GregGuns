package com.gregguns.item.part;

import com.gregguns.api.Tier;
import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the component items and holds them for recipes and the gun items.
 *
 * <p>
 * Registered for the M1 pistol slice: {@code part_barrel_hardened} (RC-05) and {@code part_mechanical_2} (RC-11), both
 * LV. The rest of the component index arrives with the guns that need it. Names match the texture files.
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

    private static ItemPart barrelHardened;
    private static ItemPart mechanical2;

    private PartItems() {}

    /**
     * Registers every part item. Call once from {@code GunsModule.preInit}.
     *
     * @param registrar the guns module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        barrelHardened = registrar.registerItem(BARREL_HARDENED, new ItemPart(Tier.LV));
        mechanical2 = registrar.registerItem(MECHANICAL_2, new ItemPart(Tier.LV));
    }

    /** @return the Hardened Steel Barrel; {@code null} before registration or when the guns module is off */
    public static ItemPart barrelHardened() {
        return barrelHardened;
    }

    /** @return Mechanical Parts T2; {@code null} before registration or when the guns module is off */
    public static ItemPart mechanical2() {
        return mechanical2;
    }
}
