package com.gregguns.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.client.anim.AdsBlend;
import com.gregguns.core.config.ClientConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The local player's aim down sights (ADS): an eased blend between the normal hold and the sights lined up on the
 * screen center, plus the rules that go with it (FOV, crosshair, sprint). Client only, and thin: the transition curve
 * is {@link AdsBlend}, the offset math is {@code AdsOffset}, and the aiming state itself is the server's.
 *
 * <p>
 * The server decides whether the player is aiming ({@code ZoomState}) and tells the client with the zoom event; this
 * class only shows it. The blend is pulled back to the normal hold, with no change to the server's state, while a GUI
 * is open, a third-person camera is on, the player is dead or the client option is off; it returns by itself when the
 * cause goes away. It ends outright on a weapon swap, a reload and a world unload.
 *
 * <p>
 * It draws nothing and sets no GL state: the renderer reads {@link #blendFor} and translates inside its own matrix
 * push, so nothing can leak to a shader pack's passes.
 *
 * <p>
 * Spec: docs/gregguns-spec/03-gun-visuals-audio/ads.md.
 */
@SideOnly(Side.CLIENT)
public final class AdsController {

    /** The blend above which the sights count as lined up (hides the crosshair). */
    private static final double LINED_UP = 0.5;

    private final AdsBlend blend = new AdsBlend();
    private Item owner;
    private boolean narrowsFov;
    private boolean wantsAim;

    /**
     * The server's answer: the local player is (or is no longer) aiming with the gun they hold.
     *
     * @param item       the held gun, or null
     * @param aiming     true while the server says the player is aiming
     * @param narrowsFov true if ADS narrows the FOV itself (a gun with no scope zoom of its own)
     */
    public void setAiming(Item item, boolean aiming, boolean narrowsFov) {
        if (!aiming || item == null) {
            wantsAim = false;
            return;
        }
        owner = item;
        wantsAim = true;
        this.narrowsFov = narrowsFov;
    }

    /** Ends the aim at once with no animation (weapon swap, reload, death, world unload). */
    public void reset() {
        owner = null;
        wantsAim = false;
        narrowsFov = false;
        blend.reset();
    }

    /** Once per client tick, after the player's own tick. */
    void tick(Minecraft mc) {
        EntityPlayer player = mc.thePlayer;
        ItemStack held = player == null ? null : player.getHeldItem();
        if (owner != null && (held == null || held.getItem() != owner || !player.isEntityAlive())) {
            reset();
        }
        boolean show = owner != null && wantsAim
            && ClientConfig.adsEnabled
            && mc.currentScreen == null
            && mc.gameSettings.thirdPersonView == 0;
        blend.setAiming(show);
        blend.tick(ClientConfig.adsTransitionTicks);
        if (owner != null && !wantsAim && blend.isIdle()) {
            reset();
        }
    }

    /**
     * @param item        the item being drawn in first person
     * @param partialTick the frame's partial tick
     * @return the blend for that item: 0 in the normal hold, 1 with the sights on the screen center
     */
    public double blendFor(Item item, double partialTick) {
        return owner != null && owner == item ? blend.value(partialTick) : 0.0;
    }

    /**
     * @param partialTick the frame's partial tick
     * @return the FOV multiplier ADS adds, 1 when it adds none (not aiming, or a gun whose scope zoom owns the FOV)
     */
    public double fovMultiplier(double partialTick) {
        if (owner == null || !narrowsFov) {
            return 1.0;
        }
        double fov = ClientConfig.adsFov;
        return 1.0 + (fov - 1.0) * blend.value(partialTick);
    }

    /** @return true while the sights are lined up and the option hides the crosshair */
    public boolean hidesCrosshair() {
        return ClientConfig.adsHideCrosshair && owner != null && blend.value(1.0) >= LINED_UP;
    }

    /**
     * Keeps the player walking while they aim: clears the sprint flag and the sprint key before the player's own
     * update reads them. Called at the start of the local player's tick. The key reads as released until pressed
     * again, as in vanilla when you let go of it.
     */
    void suppressSprint(Minecraft mc, EntityPlayer player) {
        if (ClientConfig.adsStopsSprint && owner != null && blend.isAiming()) {
            player.setSprinting(false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        }
    }

    /** Public entry for the input handler (another package). */
    public void onPlayerTickStart(Minecraft mc, EntityPlayer player) {
        suppressSprint(mc, player);
    }
}
