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
     * Registers every item in declaration order. The armor module registers through {@code ArmorItemRegistry}
     * instead, which picks the order.
     *
     * @param registrar the armor module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        registerOne(registrar, ItemGlider.NAME);
        registerOne(registrar, ItemJumpPack.NAME);
        registerOne(registrar, ItemAntiGravityDevice.NAME);
    }

    /**
     * Registers one flight device by registry name.
     *
     * @param registrar the armor module's registrar
     * @param name      one of the flight device registry names
     */
    public static void registerOne(ModuleRegistrar registrar, String name) {
        switch (name) {
            case ItemGlider.NAME:
                glider = registrar.registerItem(name, new ItemGlider());
                break;
            case ItemJumpPack.NAME:
                jumpPack = registrar.registerItem(name, new ItemJumpPack());
                break;
            case ItemAntiGravityDevice.NAME:
                antiGravityDevice = registrar.registerItem(name, new ItemAntiGravityDevice());
                break;
            default:
                throw new IllegalArgumentException("not a flight item: " + name);
        }
    }

    /** @return the Glider; null before registration */
    public static @Nullable ItemGlider glider() {
        return glider;
    }

    /** @return the Jump Pack; null before registration */
    public static @Nullable ItemJumpPack jumpPack() {
        return jumpPack;
    }

    /** @return the Anti Gravity Device; null before registration */
    public static @Nullable ItemAntiGravityDevice antiGravityDevice() {
        return antiGravityDevice;
    }
}
