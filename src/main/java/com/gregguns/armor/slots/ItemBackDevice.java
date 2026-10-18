package com.gregguns.armor.slots;

import com.gregguns.armor.slots.logic.BaubleSlot;

/**
 * Base class of a back device: a Baubles {@code wings} item with the {@link BackDevice} hooks. Subclass it, return a
 * tier, and implement {@link BackDevice#onBackTick}; the Baubles plumbing, tooltip and the once-per-tick dispatch
 * ({@link SlotEquipment}) come for free. The glider, jump pack and anti-gravity device of a later branch are built on
 * this.
 *
 * <p>
 * Server and client.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-02: wings slot, one back device at a time)
public abstract class ItemBackDevice extends ItemBauble implements BackDevice {

    @Override
    public final BaubleSlot slot() {
        return BaubleSlot.WINGS;
    }
}
