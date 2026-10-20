package com.gregguns.armor;

import com.gregguns.armor.flight.FlightItems;
import com.gregguns.armor.slots.EquipmentItems;
import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers every item of the armor module in the order {@link ArmorItemOrder} derives (tier by tier; each set's four
 * pieces together, then the tier's accessories and parts), so the item ids, NEI's default sort and the creative tab
 * all read in build order. Common side, preInit.
 */
// Spec: docs/gregguns-spec/05-armor/index.md (registration order)
public final class ArmorItemRegistry {

    private ArmorItemRegistry() {}

    /**
     * Registers the armor pieces, equipment, flight devices and parts, in order. Call once from
     * {@code ArmorModule.preInit}.
     *
     * @param registrar the armor module's registrar
     */
    public static void registerAll(ModuleRegistrar registrar) {
        EquipmentItems.prepare();
        for (ArmorItemOrder.Slot slot : ArmorItemOrder.build()) {
            switch (slot.kind()) {
                case ARMOR_PIECE:
                    ArmorItems.registerPiece(registrar, slot.set(), slot.armorSlot());
                    break;
                case EQUIPMENT:
                    EquipmentItems.registerOne(registrar, slot.name());
                    break;
                case FLIGHT:
                    FlightItems.registerOne(registrar, slot.name());
                    break;
                case PART:
                    ArmorParts.registerOne(registrar, slot.name());
                    break;
                default:
                    throw new IllegalStateException("unhandled item kind " + slot.kind());
            }
        }
    }
}
