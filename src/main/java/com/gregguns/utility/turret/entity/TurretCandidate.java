package com.gregguns.utility.turret.entity;

import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;

import com.gregguns.utility.turret.TurretHost;
import com.gregguns.utility.turret.TurretTargetSelector;

/**
 * Adapts a living entity to the pure {@link TurretTargetSelector.Candidate}. Every cast is behind an
 * {@code instanceof}. Server side.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (TurretTargetSelector)
final class TurretCandidate implements TurretTargetSelector.Candidate {

    private final EntityLivingBase entity;

    TurretCandidate(EntityLivingBase entity) {
        this.entity = entity;
    }

    /**
     * @param host   the shooting turret's host
     * @param self   the shooting turret
     * @param entity a possible target
     * @return true when the host's rules choose the entity
     */
    static boolean shouldShoot(TurretHost host, TurretEntity self, EntityLivingBase entity) {
        if (entity == null || entity == self || host == null) {
            return false;
        }
        return TurretTargetSelector.isTarget(host.targetSettings(), new TurretCandidate(entity));
    }

    @Override
    public boolean isHostileMob() {
        return entity instanceof IMob;
    }

    @Override
    public boolean isTurret() {
        return entity instanceof TurretEntity;
    }

    @Override
    public UUID turretOwner() {
        if (entity instanceof TurretEntity) {
            TurretHost other = ((TurretEntity) entity).host();
            return other == null ? null : other.ownerId();
        }
        return null;
    }

    @Override
    public boolean isShootablePlayer() {
        return entity instanceof EntityPlayer && !((EntityPlayer) entity).capabilities.disableDamage;
    }

    @Override
    public UUID playerId() {
        return entity instanceof EntityPlayer ? entity.getUniqueID() : null;
    }

    @Override
    public boolean isPassiveAnimal() {
        return entity instanceof IAnimals && !(entity instanceof IMob) && !(entity instanceof TurretEntity);
    }

    @Override
    public boolean isTamedOrOwned() {
        if (entity instanceof EntityTameable && ((EntityTameable) entity).isTamed()) {
            return true;
        }
        if (entity instanceof EntityHorse && ((EntityHorse) entity).isTame()) {
            return true;
        }
        if (entity instanceof IEntityOwnable) {
            String owner = ((IEntityOwnable) entity).func_152113_b();
            return owner != null && !owner.isEmpty();
        }
        return false;
    }
}
