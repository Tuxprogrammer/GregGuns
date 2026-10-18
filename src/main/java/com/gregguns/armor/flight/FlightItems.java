package com.gregguns.armor.flight;

import org.jetbrains.annotations.Nullable;

import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the three flight back devices and holds them for recipes and scenarios: the Glider, the Jump Pack and the
 * Anti Gravity Device. There is no Jetpack (design 0009). Common side, preInit.
 *
 * <p>
 * Usage: {@code new ItemStack(FlightItems.glider())}.
 */
// Spec: docs/gregguns-spec/05-armor/powered-flight.md; docs/design/0009-gtnh-integration-defaults.md (decision 1)
public final class FlightItems {

    // Set once in register(): registries of this module's items.
    private static ItemGlider glider;
    private static ItemJumpPack jumpPack;
    private static ItemAntiGravityDevice antiGravityDevice;

    private FlightItems() {}

    /**
     * Registers every item. Call once from {@code ArmorModule.preInit}.
     *
     * @param registrar the armor module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        glider = registrar.registerItem(ItemGlider.NAME, new ItemGlider());
        jumpPack = registrar.registerItem(ItemJumpPack.NAME, new ItemJumpPack());
        antiGravityDevice = registrar.registerItem(ItemAntiGravityDevice.NAME, new ItemAntiGravityDevice());
    }

    /** @return the Glider; null with the armor module off */
    public static @Nullable ItemGlider glider() {
        return glider;
    }

    /** @return the Jump Pack; null with the armor module off */
    public static @Nullable ItemJumpPack jumpPack() {
        return jumpPack;
    }

    /** @return the Anti Gravity Device; null with the armor module off */
    public static @Nullable ItemAntiGravityDevice antiGravityDevice() {
        return antiGravityDevice;
    }
}
