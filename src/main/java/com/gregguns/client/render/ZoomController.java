package com.gregguns.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.client.anim.ZoomAnimator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The local player's zoom: an eased FOV multiplier and whether the scope overlay is showing. The zoom belongs to the
 * gun that was held when it was set, and ends as soon as anything else is held (deviation from Techguns, which keeps
 * it between guns of equal factor; matches the gun-logic {@code ZoomState}). Client only.
 *
 * <p>
 * The multiplier is applied through Forge's {@code FOVUpdateEvent}, so it multiplies with Angelica's own zoom key
 * (GGS-03-05). Spec: rendering-framework.md section 9.1; angelica-item-rendering.md "zoom key stacks with scope zoom".
 */
@SideOnly(Side.CLIENT)
public final class ZoomController {

    private final ZoomAnimator animator = new ZoomAnimator();
    private Item owner;
    private boolean overlay;

    /**
     * Sets the zoom for the item the local player holds now.
     *
     * @param fovMultiplier below 1 zooms in; 1 or more clears the zoom
     * @param scopeOverlay  true to draw the scope overlay
     */
    void setZoom(float fovMultiplier, boolean scopeOverlay) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ItemStack held = player == null ? null : player.getHeldItem();
        if (held == null || fovMultiplier >= 1.0f) {
            reset();
            return;
        }
        owner = held.getItem();
        overlay = scopeOverlay;
        animator.setTarget(fovMultiplier);
    }

    /** Eases back to no zoom and hides the overlay. */
    void reset() {
        owner = null;
        overlay = false;
        animator.reset();
    }

    /** Once per client tick: ends the zoom if the held item changed, then advances the easing. */
    void tick(EntityPlayer player) {
        ItemStack held = player == null ? null : player.getHeldItem();
        if (owner != null && (held == null || held.getItem() != owner)) {
            reset();
        }
        animator.tick();
    }

    /** @return the FOV multiplier for this frame, 1.0 when not zoomed */
    double fovMultiplier(double partialTick) {
        return animator.value(partialTick);
    }

    /** @return true if the scope overlay should replace the gun model for this item */
    boolean isScoped(Item item) {
        return overlay && owner == item && animator.isZoomed();
    }

    /** @return the item the zoom belongs to, or null */
    Item owner() {
        return owner;
    }
}
