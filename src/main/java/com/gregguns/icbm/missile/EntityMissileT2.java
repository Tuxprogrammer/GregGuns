package com.gregguns.icbm.missile;

import net.minecraft.world.World;

/**
 * The T2 (EV to IV) missile. A class of its own, because Galacticraft's {@code RocketFuels} keys its fuel lists by
 * entity
 * class (D19). Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-entity.md
public final class EntityMissileT2 extends EntityMissile {

    /** @param world the world */
    public EntityMissileT2(World world) {
        super(world);
    }

    @Override
    public MissileTier tier() {
        return MissileTier.T2;
    }
}
