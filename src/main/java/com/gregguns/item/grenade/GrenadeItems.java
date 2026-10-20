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
        registerOne(registrar, STIELGRANATE);
        registerOne(registrar, FRAG_GRENADE);
        registerOne(registrar, FLASHBANG);
        registerEntities(registrar);
    }

    /**
     * Registers one grenade item by name, so the caller picks the registration order (see {@code GunsItemOrder}).
     *
     * @param registrar the guns module's registrar
     * @param name      {@link #STIELGRANATE}, {@link #FRAG_GRENADE} or {@link #FLASHBANG}
     */
    public static void registerOne(ModuleRegistrar registrar, String name) {
        if (STIELGRANATE.equals(name)) {
            stielgranate = registrar.registerItem(STIELGRANATE, new ItemStielgranate());
        } else if (FRAG_GRENADE.equals(name)) {
            fragGrenade = registrar.registerItem(FRAG_GRENADE, new ItemFragGrenade());
        } else if (FLASHBANG.equals(name)) {
            flashbang = registrar.registerItem(FLASHBANG, new ItemFlashbang());
        } else {
            throw new IllegalArgumentException("not a grenade item: " + name);
        }
    }

    /**
     * Registers the three grenade projectile entities (no item ids involved).
     *
     * @param registrar the guns module's registrar
     */
    public static void registerEntities(ModuleRegistrar registrar) {
        registrar.registerEntity(
            "grenade_projectile",
            GrenadeProjectile.class,
            GrenadeProjectile.ENTITY_ID,
            GrenadeProjectile.TRACKING_RANGE,
            GrenadeProjectile.UPDATE_FREQUENCY,
            GrenadeProjectile.SENDS_VELOCITY);
        registrar.registerEntity(
            "frag_grenade_projectile",
            FragGrenadeProjectile.class,
            FragGrenadeProjectile.ENTITY_ID,
            GrenadeProjectile.TRACKING_RANGE,
            GrenadeProjectile.UPDATE_FREQUENCY,
            GrenadeProjectile.SENDS_VELOCITY);
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
