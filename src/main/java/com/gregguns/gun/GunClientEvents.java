package com.gregguns.gun;

import net.minecraft.entity.EntityLivingBase;

import com.gregguns.gun.logic.GunStats;

/**
 * What the client does when a gun fires, reloads or zooms, for any entity that can be seen: the local player (from
 * local prediction) and every other shooter (from the server's {@code PacketGunEvent}). This is the seam between the
 * gun item and the rendering framework.
 *
 * <h2>Who calls it</h2>
 * <ul>
 * <li><b>Local player:</b> the client mirror ({@code com.gregguns.client.gun.ClientGunMirror}) calls it the moment the
 * player acts, before the server answers. The server's own broadcast excludes the shooter, so each event reaches this
 * interface once. If the server rejects a predicted reload, {@link #onReloadCanceled} follows.</li>
 * <li><b>Other entities in range:</b> the client packet handler calls it when the server reports that entity X fired
 * or reloaded. Their sounds already arrive as ordinary sound packets, so implementations only animate for them.</li>
 * </ul>
 *
 * <h2>Rules for implementers</h2>
 * Client thread only. Calls are made for the entity's currently held gun, but the entity may stop holding it a tick
 * later, so read the held item again before drawing. Do not touch server state, and do not assume the call order
 * between entities. The default holder, {@link GunClientHooks}, installs a no-op.
 *
 * <h2>Installing</h2>
 * {@code GunsClientModule} installs the sound-playing implementation. A renderer that wants these events wraps it:
 *
 * <pre>
 * GunClientEvents sounds = GunClientHooks.events();
 * GunClientHooks.install(new GunClientEvents() {
 *     public void onShotFired(EntityLivingBase e, GunStats s, int hand) { sounds.onShotFired(e, s, hand); anim(e); }
 *     ...
 * });
 * </pre>
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md sections 5, 7 and 12 (the client-side recoil, reload animation
// and zoom); docs/techguns-spec/01-core/networking.md (GunFiredMessage, ReloadStartedMessage).
public interface GunClientEvents {

    /**
     * A shot was fired: start the recoil and muzzle-flash animation.
     *
     * @param entity     the shooter
     * @param stats      the gun that fired
     * @param attackType hand for alternating akimbo guns, 0 right and 1 left; always 0 for single guns
     */
    void onShotFired(EntityLivingBase entity, GunStats stats, int attackType);

    /**
     * The trigger was pulled on an empty gun with nothing to reload with. Local player only.
     *
     * @param entity the player
     * @param stats  the gun
     */
    void onDryFire(EntityLivingBase entity, GunStats stats);

    /**
     * A reload began: start the reload animation, which lasts {@code ticks} client ticks.
     *
     * @param entity the reloading entity
     * @param stats  the gun being reloaded
     * @param ticks  reload duration in ticks (20 per second)
     */
    void onReloadStarted(EntityLivingBase entity, GunStats stats, int ticks);

    /**
     * A reload ended early (weapon switched, fired a round, server refused it): stop the reload animation.
     *
     * @param entity the entity that was reloading
     */
    void onReloadCanceled(EntityLivingBase entity);

    /**
     * The local player's zoom changed. The server decides zoom; this is its answer. Unscoped guns never zoom.
     *
     * @param entity        the local player
     * @param zoomed        true while zoomed
     * @param fovMultiplier the factor to apply to the field of view, 1 when not zoomed
     */
    void setZoom(EntityLivingBase entity, boolean zoomed, double fovMultiplier);
}
