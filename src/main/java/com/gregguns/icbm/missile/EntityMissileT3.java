package com.gregguns.icbm.missile;

import net.minecraft.world.World;

/**
 * The T3 (LuV to ZPM) missile. A class of its own, because Galacticraft's {@code RocketFuels} keys its fuel lists by
 * entity class (D19). It stands in the silo only (D70). Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md
public final class EntityMissileT3 extends EntityMissile {

    /** @param world the world */
    public EntityMissileT3(World world) {
        super(world);
    }

    @Override
    public MissileTier tier() {
        return MissileTier.T3;
    }
}
