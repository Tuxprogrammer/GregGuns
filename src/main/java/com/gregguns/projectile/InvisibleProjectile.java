package com.gregguns.projectile;

import net.minecraft.world.World;

import com.gregguns.combat.DamageType;

/**
 * The Chainsaw's cut: an ordinary straight bullet that is never drawn. Its reach (speed 1.5 for 2 ticks, about 3
 * blocks), damage and armor penetration come from the gun's stats; the client renderer draws nothing.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.12 InvisibleProjectile)
// Deviation: Techguns tagged the hit GORE; GregGuns has no gore death effect yet (the death-effect milestone owns it),
// so the cut is a plain PHYSICAL hit.
public class InvisibleProjectile extends GenericProjectile {

    /** Mod-local entity ID; the same number Techguns uses for its invisible projectile. */
    public static final int ENTITY_ID = 21;

    /**
     * @param world the world
     */
    public InvisibleProjectile(World world) {
        super(world);
    }

    @Override
    protected DamageType getDamageType() {
        return DamageType.PHYSICAL;
    }
}
