package com.gregguns.utility.turret.entity;

import net.minecraft.item.ItemStack;

/**
 * What the turret renderer reads from any turret-shaped entity: the player turret and the hostile camp turret. Both
 * sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/model-notes/npc_turret.md (same geometry as the head)
public interface TurretDisplay {

    /** @return the plate's armor value that picks the texture sheet */
    int displayArmorValue();

    /** @return the gun the head shows; null for none */
    ItemStack displayGun();
}
