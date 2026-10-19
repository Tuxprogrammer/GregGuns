package com.gregguns.projectile;

import net.minecraft.world.World;

import com.gregguns.combat.DamageType;

/**
 * A fast energy bolt (the De-Atomizer's blue blast): a straight {@link GenericProjectile} that deals ENERGY damage.
 * Impact effects belong to the FX core; the area explosion is commented out in Techguns and is not built.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.10 EnergyBlastProjectile)
public class EnergyBlastProjectile extends GenericProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its energy blast. */
    public static final int ENTITY_ID = 26;

    /**
     * @param world the world
     */
    public EnergyBlastProjectile(World world) {
        super(world);
    }

    @Override
    protected DamageType getDamageType() {
        return DamageType.ENERGY;
    }
}
