package com.gregguns.utility;

import net.minecraft.item.Item;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

/**
 * A turret armor plate: slot 19 of the Turret and the input of Bastion Plating. It carries only its armor value; the
 * Turret and Rampart read it from the stack. Both sides.
 */
// Spec: docs/gregguns-spec/09-machines-utility/recipes.md (Turret armor plates),
// docs/techguns-spec/09-machines-utility/
// turret.md (Armor)
public final class ItemTurretArmor extends Item implements Tiered {

    private final TurretPlate plate;

    /**
     * @param plate the plate this item is
     */
    public ItemTurretArmor(TurretPlate plate) {
        this.plate = plate;
        setMaxStackSize(16);
    }

    /** @return the plate data */
    public TurretPlate plate() {
        return plate;
    }

    /** @return the armor value, 5, 10, 15 or 20 */
    public int armorValue() {
        return plate.armorValue();
    }

    @Override
    public Tier getTier() {
        return plate.tier();
    }
}
