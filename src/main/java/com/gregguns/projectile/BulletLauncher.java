package com.gregguns.projectile;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ShotOffset;

/**
 * The installed {@link ProjectileLauncher}: spawns one projectile per shot direction on the server, of the entity
 * class the gun's {@link ProjectileKind} names. Server only; a client world is ignored, because the server alone
 * decides hits.
 *
 * <p>
 * Each kind has a factory in a small table. {@link ProjectileKind#BULLET} and {@link ProjectileKind#GRAVITY_BULLET}
 * are built in. A later milestone that adds a kind (a rocket, a laser) calls {@link #register} during its preInit with
 * a constructor reference, for example {@code BulletLauncher.register(ProjectileKind.ROCKET, RocketProjectile::new)};
 * its entity extends {@link GenericProjectile} and is registered through the module registrar. A gun whose kind has no
 * factory fires nothing and logs an error once.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (2.2)
public final class BulletLauncher implements ProjectileLauncher {

    private static final Map<ProjectileKind, Function<World, GenericProjectile>> FACTORIES = new EnumMap<>(
        ProjectileKind.class);
    private static final Set<ProjectileKind> REPORTED = EnumSet.noneOf(ProjectileKind.class);

    static {
        FACTORIES.put(ProjectileKind.BULLET, GenericProjectile::new);
        FACTORIES.put(ProjectileKind.GRAVITY_BULLET, SimpleProjectile::new);
    }

    /**
     * Registers the entity factory of a projectile kind, replacing any earlier one. Call from a module's preInit.
     *
     * @param kind    the kind
     * @param factory builds an empty projectile of the kind in a world
     */
    public static void register(ProjectileKind kind, Function<World, GenericProjectile> factory) {
        if (kind == null || factory == null) {
            throw new IllegalArgumentException("kind and factory are required");
        }
        FACTORIES.put(kind, factory);
    }

    /**
     * Removes the factory of a kind, so its shots are dropped again. Meant for scenarios that need a kind without a
     * factory; the caller restores the returned factory with {@link #register} in a finally block.
     *
     * @param kind the kind
     * @return the removed factory, or null if the kind had none
     */
    public static Function<World, GenericProjectile> unregister(ProjectileKind kind) {
        return FACTORIES.remove(kind);
    }

    /**
     * @param kind a kind
     * @return true if shots of this kind can be spawned
     */
    public static boolean isRegistered(ProjectileKind kind) {
        return FACTORIES.containsKey(kind);
    }

    @Override
    public void launch(World world, EntityLivingBase shooter, GunStats stats, ShotOffset offset, float damageScale) {
        if (world.isRemote) {
            return;
        }
        Function<World, GenericProjectile> factory = FACTORIES.get(stats.projectileKind());
        if (factory == null) {
            if (REPORTED.add(stats.projectileKind())) {
                GregGuns.LOG.error(
                    "Gun {} fires projectile kind {} but no factory is registered; its shots are dropped",
                    stats.id(),
                    stats.projectileKind());
            }
            return;
        }
        GenericProjectile bullet = factory.apply(world);
        bullet.fire(shooter, stats, offset, damageScale);
        world.spawnEntityInWorld(bullet);
    }
}
