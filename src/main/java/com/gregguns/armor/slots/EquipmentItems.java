package com.gregguns.armor.slots;

import net.minecraft.item.Item;

import org.jetbrains.annotations.Nullable;

import com.gregguns.armor.oxygen.FallbackOxygenMask;
import com.gregguns.armor.oxygen.FallbackOxygenTanks;
import com.gregguns.armor.oxygen.gc.GcOxygen;
import com.gregguns.compat.ModIds;
import com.gregguns.core.registry.ModuleRegistrar;

import cpw.mods.fml.common.Loader;

/**
 * Registers the special-slot equipment and holds it for recipes and scenarios: Night Vision Goggles, Ammo Pouch, Gas
 * Mask, Tactical Mask, Oxygen Mask and Oxygen Tanks. The two oxygen items are Galacticraft subclasses when
 * GalacticraftCore is loaded and plain fallback items otherwise, under the same registry names (GGS-05-22). Common
 * side, preInit.
 *
 * <p>
 * Usage: {@code new ItemStack(EquipmentItems.nightVisionGoggles())}.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md, oxygen-gear.md, hazard-protection.md
public final class EquipmentItems {

    /** Registry name of the Oxygen Mask. */
    public static final String OXYGEN_MASK = "oxygen_mask";
    /** Registry name of the Oxygen Tanks. */
    public static final String OXYGEN_TANKS = "oxygen_tanks";

    // Set once in register(): registries of this module's items.
    private static ItemNightVisionGoggles goggles;
    private static ItemAmmoPouch pouch;
    private static ItemGasMask gasMask;
    private static ItemTacticalMask tacticalMask;
    private static Item oxygenMask;
    private static Item oxygenTanks;

    private EquipmentItems() {}

    /**
     * Registers every item in declaration order. The armor module registers through {@code ArmorItemRegistry}
     * instead, which picks the order.
     *
     * @param registrar the armor module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        prepare();
        registerOne(registrar, ItemNightVisionGoggles.NAME);
        registerOne(registrar, ItemAmmoPouch.NAME);
        registerOne(registrar, ItemGasMask.NAME);
        registerOne(registrar, ItemTacticalMask.NAME);
        registerOne(registrar, OXYGEN_MASK);
        registerOne(registrar, OXYGEN_TANKS);
    }

    /** Baubles setup that must run once before any equipment item is registered. */
    public static void prepare() {
        BaublesCompat.warnIfAbsent();
        BaublesCompat.requestSlots();
    }

    /**
     * Registers one equipment item by registry name. The oxygen items are Galacticraft subclasses when
     * GalacticraftCore is loaded (GGS-05-23, only linked on that branch) and fallback items otherwise.
     *
     * @param registrar the armor module's registrar
     * @param name      one of the equipment registry names
     */
    public static void registerOne(ModuleRegistrar registrar, String name) {
        boolean gc = Loader.isModLoaded(ModIds.GALACTICRAFT_CORE);
        switch (name) {
            case ItemNightVisionGoggles.NAME:
                goggles = registrar.registerItem(name, new ItemNightVisionGoggles());
                break;
            case ItemAmmoPouch.NAME:
                pouch = registrar.registerItem(name, new ItemAmmoPouch());
                break;
            case ItemGasMask.NAME:
                gasMask = registrar.registerItem(name, new ItemGasMask());
                break;
            case ItemTacticalMask.NAME:
                tacticalMask = registrar.registerItem(name, new ItemTacticalMask());
                break;
            case OXYGEN_MASK:
                oxygenMask = gc ? GcOxygen.registerMask(registrar, name)
                    : registrar.registerItem(name, new FallbackOxygenMask());
                break;
            case OXYGEN_TANKS:
                oxygenTanks = gc ? GcOxygen.registerTanks(registrar, name)
                    : registrar.registerItem(name, new FallbackOxygenTanks());
                break;
            default:
                throw new IllegalArgumentException("not an equipment item: " + name);
        }
    }

    /** @return the goggles; null before registration */
    public static @Nullable ItemNightVisionGoggles nightVisionGoggles() {
        return goggles;
    }

    /** @return the Ammo Pouch; null before registration */
    public static @Nullable ItemAmmoPouch ammoPouch() {
        return pouch;
    }

    /** @return the Gas Mask; null before registration */
    public static @Nullable ItemGasMask gasMask() {
        return gasMask;
    }

    /** @return the Tactical Mask; null before registration */
    public static @Nullable ItemTacticalMask tacticalMask() {
        return tacticalMask;
    }

    /** @return the Oxygen Mask (a Galacticraft subclass or the fallback); null before registration */
    public static @Nullable Item oxygenMask() {
        return oxygenMask;
    }

    /** @return the Oxygen Tanks (a Galacticraft subclass or the fallback); null before registration */
    public static @Nullable Item oxygenTanks() {
        return oxygenTanks;
    }
}
