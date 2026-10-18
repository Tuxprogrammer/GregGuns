package com.gregguns.client.render;

import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The per-draw values a {@link GunModel} may use: rounds left, reload and fire progress, and which view is drawing.
 * One instance is reused by a renderer for every draw (the render thread is single threaded). Client only.
 *
 * <p>
 * Spec: rendering-framework.md section 4, step 6 (the extended render call) and section 10.
 */
@SideOnly(Side.CLIENT)
public final class ModelState {

    /** {@link #reloadProgress} outside a reload: a sentinel above 1. */
    public static final float NOT_RELOADING = 10.0f;

    /** Rounds loaded, 100 when unknown. */
    public int ammoLeft = 100;
    /** Reload progress 0 to 1, or {@link #NOT_RELOADING}. */
    public float reloadProgress = NOT_RELOADING;
    /** Recoil progress while firing, else 0. */
    public float fireProgress;
    /** The view being drawn. */
    public ItemRenderType renderType = ItemRenderType.ENTITY;
}
