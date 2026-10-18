package com.gregguns.gun;

import net.minecraft.entity.EntityLivingBase;

import com.gregguns.gun.logic.GunStats;

/**
 * Holder for the client-side gun hooks. Both fields default to no-ops, so the common code that calls them is safe on a
 * dedicated server and before the client installs anything. A registry-like holder, set once per side at startup.
 *
 * <p>
 * Usage from common code (a packet handler):
 * {@code GunClientHooks.events().onShotFired(entity, stats, 0);}
 */
public final class GunClientHooks {

    private static final GunClientEvents NO_EVENTS = new GunClientEvents() {

        @Override
        public void onShotFired(EntityLivingBase entity, GunStats stats, int attackType) {}

        @Override
        public void onDryFire(EntityLivingBase entity, GunStats stats) {}

        @Override
        public void onReloadStarted(EntityLivingBase entity, GunStats stats, int ticks) {}

        @Override
        public void onReloadCanceled(EntityLivingBase entity) {}

        @Override
        public void setZoom(EntityLivingBase entity, boolean zoomed, double fovMultiplier) {}
    };

    private static GunClientEvents events = NO_EVENTS;
    private static GunClientSync sync = GunClientSync.NONE;

    private GunClientHooks() {}

    /** @return the installed events sink, never null */
    public static GunClientEvents events() {
        return events;
    }

    /** @return the installed mirror callbacks, never null */
    public static GunClientSync sync() {
        return sync;
    }

    /**
     * Installs the events sink. A renderer wraps the previous one ({@link #events()}) to keep the sounds.
     *
     * @param newEvents the sink; must not be null
     */
    public static void install(GunClientEvents newEvents) {
        if (newEvents == null) {
            throw new IllegalArgumentException("events");
        }
        events = newEvents;
    }

    /**
     * Installs the mirror callbacks. Called once by {@code GunsClientModule}.
     *
     * @param newSync the callbacks; must not be null
     */
    public static void installSync(GunClientSync newSync) {
        if (newSync == null) {
            throw new IllegalArgumentException("sync");
        }
        sync = newSync;
    }
}
