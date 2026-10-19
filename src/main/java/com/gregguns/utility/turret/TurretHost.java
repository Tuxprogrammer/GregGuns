package com.gregguns.utility.turret;

import java.util.UUID;

import net.minecraft.item.ItemStack;

import com.gregguns.utility.security.FriendProviders;
import com.gregguns.utility.security.SecurityLevel;
import com.gregguns.utility.turret.entity.TurretEntity;

/**
 * What a {@link TurretEntity} needs from the thing that holds it: the Turret block today, a Rampart gun mount later.
 * The entity never reaches into a tile entity; it asks this interface, so a new host needs no entity change. Server
 * side: every method runs on the server thread.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md; docs/techguns-spec/09-machines-utility/turret.md (Linked
// entity)
public interface TurretHost {

    /** @return the owner's UUID; null for an ownerless host */
    UUID ownerId();

    /** @return who the turret leaves alone; never null */
    SecurityLevel security();

    /** @return the Target Animals toggle */
    boolean targetsAnimals();

    /**
     * @return the gun to fire, or null when none is mounted or its tier is above the host's; the turret then holds
     *         fire
     */
    ItemStack mountedGun();

    /**
     * Uses one shot of the mounted gun: reloads from the ammo slots when the gun is empty and writes the new round
     * count into the gun.
     *
     * @return true when a round was available and the shot is allowed
     */
    boolean consumeAmmo();

    /** @return vanilla armor points of the turret (each is 4 percent), from the armor plate; 0 for none */
    int armorValue();

    /** @return false once the block that holds the host is gone, so the turret lets go of it */
    boolean isHostValid();

    /** @return true while the turret may act: powered and allowed by the redstone mode */
    boolean isTurretActive();

    /**
     * A turret with no host asks to be taken over, for example after a chunk reload.
     *
     * @param turret the asking entity
     * @return true when this host now owns the link; false when it is linked to another live turret
     */
    boolean adopt(TurretEntity turret);

    /** The linked turret died (health 0). Must not throw. */
    void onTurretDeath();

    /** @return the settings the target rules read, using the pack's friend provider */
    default TurretTargetSelector.Settings targetSettings() {
        return new TurretTargetSelector.Settings(security(), ownerId(), targetsAnimals(), FriendProviders.current());
    }
}
