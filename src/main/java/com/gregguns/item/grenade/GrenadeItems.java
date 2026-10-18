package com.gregguns.item.grenade;

import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.projectile.GrenadeProjectile;

/**
 * Registers the grenade items and the grenade entity, and holds the items for recipes and scenarios. Common side,
 * preInit.
 *
 * <p>
 * Usage: {@code new ItemStack(GrenadeItems.stielgranate(), 16)}.
 */
public final class GrenadeItems {

    /** Registry name of the Stielgranate. */
    public static final String STIELGRANATE = "stielgranate";

    private static ItemStielgranate stielgranate;

    private GrenadeItems() {}

    /**
     * Registers the Stielgranate and {@link GrenadeProjectile}. Call once from {@code GunsModule.preInit}.
     *
     * @param registrar the guns module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        stielgranate = registrar.registerItem(STIELGRANATE, new ItemStielgranate());
        registrar.registerEntity(
            "grenade_projectile",
            GrenadeProjectile.class,
            GrenadeProjectile.ENTITY_ID,
            GrenadeProjectile.TRACKING_RANGE,
            GrenadeProjectile.UPDATE_FREQUENCY,
            GrenadeProjectile.SENDS_VELOCITY);
    }

    /** @return the Stielgranate; {@code null} before registration or when the guns module is off */
    public static ItemStielgranate stielgranate() {
        return stielgranate;
    }
}
