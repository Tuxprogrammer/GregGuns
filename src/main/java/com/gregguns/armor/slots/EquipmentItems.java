package com.gregguns.armor.slots;

import net.minecraft.item.Item;

import org.jetbrains.annotations.Nullable;

import com.gregguns.armor.oxygen.FallbackOxygenMask;
import com.gregguns.armor.oxygen.FallbackOxygenTanks;
import com.gregguns.armor.oxygen.gc.OxygenMaskGC;
import com.gregguns.armor.oxygen.gc.OxygenTanksGC;
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
    /** Galacticraft's mod id. */
    public static final String GC_MODID = "GalacticraftCore";

    // Set once in register(): registries of this module's items.
    private static ItemNightVisionGoggles goggles;
    private static ItemAmmoPouch pouch;
    private static ItemGasMask gasMask;
    private static ItemTacticalMask tacticalMask;
    private static Item oxygenMask;
    private static Item oxygenTanks;

    private EquipmentItems() {}

    /**
     * Registers every item. Call once from {@code ArmorModule.preInit}.
     *
     * @param registrar the armor module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        BaublesCompat.warnIfAbsent();
        goggles = registrar.registerItem(ItemNightVisionGoggles.NAME, new ItemNightVisionGoggles());
        pouch = registrar.registerItem(ItemAmmoPouch.NAME, new ItemAmmoPouch());
        gasMask = registrar.registerItem(ItemGasMask.NAME, new ItemGasMask());
        tacticalMask = registrar.registerItem(ItemTacticalMask.NAME, new ItemTacticalMask());
        if (Loader.isModLoaded(GC_MODID)) {
            // The GC subclasses are only linked on this branch (GGS-05-23).
            oxygenMask = registrar.registerItem(OXYGEN_MASK, new OxygenMaskGC());
            oxygenTanks = registrar.registerItem(OXYGEN_TANKS, new OxygenTanksGC());
        } else {
            oxygenMask = registrar.registerItem(OXYGEN_MASK, new FallbackOxygenMask());
            oxygenTanks = registrar.registerItem(OXYGEN_TANKS, new FallbackOxygenTanks());
        }
    }

    /** @return the goggles; null with the armor module off */
    public static @Nullable ItemNightVisionGoggles nightVisionGoggles() {
        return goggles;
    }

    /** @return the Ammo Pouch; null with the armor module off */
    public static @Nullable ItemAmmoPouch ammoPouch() {
        return pouch;
    }

    /** @return the Gas Mask; null with the armor module off */
    public static @Nullable ItemGasMask gasMask() {
        return gasMask;
    }

    /** @return the Tactical Mask; null with the armor module off */
    public static @Nullable ItemTacticalMask tacticalMask() {
        return tacticalMask;
    }

    /** @return the Oxygen Mask (a Galacticraft subclass or the fallback); null with the armor module off */
    public static @Nullable Item oxygenMask() {
        return oxygenMask;
    }

    /** @return the Oxygen Tanks (a Galacticraft subclass or the fallback); null with the armor module off */
    public static @Nullable Item oxygenTanks() {
        return oxygenTanks;
    }
}
