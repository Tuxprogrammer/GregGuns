package com.gregguns.item.grenade;

import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.projectile.FlashbangProjectile;
import com.gregguns.projectile.FragGrenadeProjectile;
import com.gregguns.projectile.GrenadeProjectile;

/**
 * Registers the grenade items (Stielgranate, Frag Grenade, Flashbang) and their entities, and holds the items for
 * recipes and scenarios. Common side,
 * preInit.
 *
 * <p>
 * Usage: {@code new ItemStack(GrenadeItems.stielgranate(), 16)}.
 */
public final class GrenadeItems {

    /** Registry name of the Stielgranate. */
    public static final String STIELGRANATE = "stielgranate";

    /** Registry name of the Frag Grenade. */
    public static final String FRAG_GRENADE = "frag_grenade";
    /** Registry name of the Flashbang. */
    public static final String FLASHBANG = "flashbang";

    private static ItemStielgranate stielgranate;
    private static ItemFragGrenade fragGrenade;
    private static ItemFlashbang flashbang;

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
        fragGrenade = registrar.registerItem(FRAG_GRENADE, new ItemFragGrenade());
        registrar.registerEntity(
            "frag_grenade_projectile",
            FragGrenadeProjectile.class,
            FragGrenadeProjectile.ENTITY_ID,
            GrenadeProjectile.TRACKING_RANGE,
            GrenadeProjectile.UPDATE_FREQUENCY,
            GrenadeProjectile.SENDS_VELOCITY);
        flashbang = registrar.registerItem(FLASHBANG, new ItemFlashbang());
        registrar.registerEntity(
            "flashbang_projectile",
            FlashbangProjectile.class,
            FlashbangProjectile.ENTITY_ID,
            GrenadeProjectile.TRACKING_RANGE,
            GrenadeProjectile.UPDATE_FREQUENCY,
            GrenadeProjectile.SENDS_VELOCITY);
    }

    /** @return the Stielgranate; {@code null} before registration or when the guns module is off */
    public static ItemStielgranate stielgranate() {
        return stielgranate;
    }

    /** @return the Frag Grenade; {@code null} before registration or when the guns module is off */
    public static ItemFragGrenade fragGrenade() {
        return fragGrenade;
    }

    /** @return the Flashbang; {@code null} before registration or when the guns module is off */
    public static ItemFlashbang flashbang() {
        return flashbang;
    }
}
