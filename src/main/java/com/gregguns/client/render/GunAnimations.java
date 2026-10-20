package com.gregguns.client.render;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gregguns.client.anim.GunAnimState;
import com.gregguns.client.anim.MuzzleLight;
import com.gregguns.client.anim.ShotOrigin;
import com.gregguns.client.render.profile.GunTransforms;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The client's gun animation state, one record per shooter keyed by entity id, plus the client clock and the zoom
 * controller. This is the API the gun item's client side calls (through
 * {@code GunsClientModule.animations()}): the same hooks serve the local player's own prediction and a "player X
 * fired" packet for another player or a mob.
 *
 * <ul>
 * <li>{@link #onShotFired}: a shot happened (the local player's own, or a packet for someone else's).</li>
 * <li>{@link #onShotEcho}: the server's echo of a shot; it never restarts the local player's own recoil (GVA-03).</li>
 * <li>{@link #onReloadStarted} and {@link #onReloadCanceled}.</li>
 * <li>{@link #setZoom} and {@link #clearZoom} for the local player's scope.</li>
 * </ul>
 *
 * Records are dropped when idle and on world unload. Client only; main thread only.
 *
 * <p>
 * Spec: rendering-framework.md sections 3 and 9.1; BUGS GVA-03, GVA-19.
 */
@SideOnly(Side.CLIENT)
public final class GunAnimations {

    private static final int FALLBACK_RECOIL_TICKS = 5;
    private static final int FALLBACK_FLASH_TICKS = 5;
    private static final int PRUNE_PERIOD_TICKS = 40;

    private final Map<Integer, GunAnimState> states = new HashMap<>();
    private final ZoomController zoom = new ZoomController();
    private long tick;
    private float partial;

    /** @return the current time as {@code tick + partialTick}, in ticks */
    public double now() {
        return tick + (double) partial;
    }

    /** @return the partial tick of the frame being rendered */
    double partial() {
        return partial;
    }

    /** @return the zoom controller (FOV and scope overlay state) */
    public ZoomController zoom() {
        return zoom;
    }

    /**
     * Records a shot using the held gun's registered recoil and flash lengths.
     *
     * @param shooter    the shooter: the local player (predicted) or anyone else (from a packet)
     * @param attackType 0 normally, or the gun index for alternating akimbo guns
     */
    public void onShotFired(EntityLivingBase shooter, int attackType) {
        shot(shooter, attackType, -1, -1, isLocal(shooter) ? ShotOrigin.PREDICTED : ShotOrigin.REMOTE);
    }

    /** As {@link #onShotFired(EntityLivingBase, int)} with explicit window lengths in ticks. */
    public void onShotFired(EntityLivingBase shooter, int attackType, int recoilTicks, int flashTicks) {
        shot(shooter, attackType, recoilTicks, flashTicks, isLocal(shooter) ? ShotOrigin.PREDICTED : ShotOrigin.REMOTE);
    }

    /**
     * Records the server's echo of a shot. For the local player it is ignored while a predicted shot is recent, so
     * the first-person kick does not restart with latency (GVA-03). For anyone else it is a normal shot.
     */
    public void onShotEcho(EntityLivingBase shooter, int attackType) {
        shot(shooter, attackType, -1, -1, isLocal(shooter) ? ShotOrigin.SERVER_ECHO : ShotOrigin.REMOTE);
    }

    /**
     * Starts a reload animation. For the local player this also clears the zoom (Techguns resets zoom when a reload
     * starts).
     *
     * @param shooter       whoever is reloading
     * @param durationTicks reload length in ticks
     */
    public void onReloadStarted(EntityLivingBase shooter, int durationTicks) {
        if (shooter == null) {
            return;
        }
        GunAnimState s = stateFor(shooter);
        s.reloadStarted(now(), durationTicks, heldTag(shooter));
        s.spinReset(); // the rotor does not move in a reload
        if (isLocal(shooter)) {
            zoom.reset();
        }
    }

    /** Cancels a reload animation (gun switched, dropped, or the reload was interrupted). */
    public void onReloadCanceled(EntityLivingBase shooter) {
        if (shooter == null) {
            return;
        }
        GunAnimState s = states.get(shooter.getEntityId());
        if (s != null) {
            s.reloadCanceled();
        }
    }

    /**
     * Zooms the local player's view while holding the current gun. The zoom ends by itself when another item is
     * selected.
     *
     * @param fovMultiplier 1.0 for none, below 1 to zoom in (for example 0.35); 1.0 or more clears the zoom
     * @param scopeOverlay  true to also draw the scope overlay and hide the gun model
     */
    public void setZoom(float fovMultiplier, boolean scopeOverlay) {
        zoom.setZoom(fovMultiplier, scopeOverlay);
    }

    /** Clears the local player's zoom. */
    public void clearZoom() {
        zoom.reset();
    }

    /**
     * @return the dynamic-light level for a holder who fired recently: 12 for three ticks after a shot, else 0
     */
    public int muzzleLuminance(EntityLivingBase holder) {
        GunAnimState s = holder == null ? null : states.get(holder.getEntityId());
        return s == null ? 0 : MuzzleLight.luminance(s.ticksSinceShot(now()));
    }

    /** @return the record of this shooter, or null if none (nothing to animate) */
    GunAnimState peek(EntityLivingBase shooter) {
        return states.get(shooter.getEntityId());
    }

    /** Called once per client tick (end phase). */
    void clientTick() {
        tick++;
        zoom.tick(Minecraft.getMinecraft().thePlayer);
        tickSpins();
        if (tick % PRUNE_PERIOD_TICKS == 0) {
            double now = now();
            Iterator<GunAnimState> it = states.values()
                .iterator();
            while (it.hasNext()) {
                if (it.next()
                    .isIdle(now)) {
                    it.remove();
                }
            }
        }
    }

    /** The angle of this holder's rotor for the frame being rendered, in turns; 0 at rest and without a record. */
    double rotorTurns(EntityLivingBase holder, int gunTag) {
        GunAnimState s = holder == null ? null : states.get(holder.getEntityId());
        return s == null ? 0.0 : s.rotorTurns(gunTag, partial);
    }

    /** Advances every spinning rotor one tick; a holder who put the gun away (or left) loses the spin at once. */
    private void tickSpins() {
        World world = Minecraft.getMinecraft().theWorld;
        for (Map.Entry<Integer, GunAnimState> e : states.entrySet()) {
            GunAnimState s = e.getValue();
            int tag = s.spinTag();
            if (tag < 0) {
                continue;
            }
            Entity holder = world == null ? null : world.getEntityByID(e.getKey());
            s.spinTick(holder instanceof EntityLivingBase && heldTag((EntityLivingBase) holder) == tag);
        }
    }

    /** Called at the start of every rendered frame with the partial tick. */
    void setPartial(float partialTicks) {
        partial = partialTicks;
    }

    /** Drops every record and the zoom (world unload, disconnect). */
    void clear() {
        states.clear();
        zoom.reset();
    }

    private void shot(EntityLivingBase shooter, int attackType, int recoilTicks, int flashTicks, ShotOrigin origin) {
        if (shooter == null) {
            return;
        }
        GunTransforms t = heldTransforms(shooter);
        int recoil = recoilTicks >= 0 ? recoilTicks : t != null ? t.recoilTicks() : FALLBACK_RECOIL_TICKS;
        int flash = flashTicks >= 0 ? flashTicks : t != null ? t.flashTicks() : FALLBACK_FLASH_TICKS;
        GunAnimState s = stateFor(shooter);
        s.shot(now(), recoil, flash, attackType, origin, heldTag(shooter), t != null && t.checkRecoil());
        if (t != null && t.rotorSpin() != null) {
            s.spinDriven(t.rotorSpin(), heldTag(shooter));
        }
    }

    private GunAnimState stateFor(EntityLivingBase shooter) {
        return states.computeIfAbsent(shooter.getEntityId(), id -> new GunAnimState());
    }

    private static boolean isLocal(EntityLivingBase e) {
        return e == Minecraft.getMinecraft().thePlayer;
    }

    private static GunTransforms heldTransforms(EntityLivingBase e) {
        ItemStack held = e.getHeldItem();
        return held == null ? null : GunRenderers.transformsOf(held.getItem());
    }

    private static int heldTag(EntityLivingBase e) {
        ItemStack held = e.getHeldItem();
        return held == null ? -1 : Item.getIdFromItem(held.getItem());
    }
}
