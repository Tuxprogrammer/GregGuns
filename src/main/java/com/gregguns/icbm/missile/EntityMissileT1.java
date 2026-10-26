package com.gregguns.icbm.missile;

import net.minecraft.world.World;

/**
 * The T1 (HV) missile. A class of its own, because Galacticraft's {@code RocketFuels} keys its fuel lists by entity
 * class (D19). Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-entity.md
public final class EntityMissileT1 extends EntityMissile {

    /** @param world the world */
    public EntityMissileT1(World world) {
        super(world);
    }

    @Override
    public MissileTier tier() {
        return MissileTier.T1;
    }
}
