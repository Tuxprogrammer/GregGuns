package com.gregguns.client.icbm;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import com.gregguns.icbm.warhead.InertWarheads;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws the inert warhead (D282) as the model of the live warhead it becomes, dimmed so an inert casing reads apart
 * from
 * a live one at a glance: the casing's own model, or the payload's model where it has one (nuclear, EMP, ender and the
 * other T3 skins). The flat icon is only the fallback for the map view. Client only.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The inert warhead)
@SideOnly(Side.CLIENT)
final class InertWarheadItemRenderer implements IItemRenderer {

    /** The brightness of an inert model against a live one. */
    private static final float INERT_TINT = 0.6F;

    private InertWarheadItemRenderer() {}

    /** Registers the renderer on the five inert warhead items. Call from init. */
    static void register() {
        for (WarheadCasing casing : WarheadCasing.values()) {
            if (InertWarheads.item(casing) != null) {
                MinecraftForgeClient.registerItemRenderer(InertWarheads.item(casing), new InertWarheadItemRenderer());
            }
        }
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return item != null && type != ItemRenderType.FIRST_PERSON_MAP;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        // The same block-style transforms as the live warhead item: spin on the ground, tilt in the inventory.
        return helper == ItemRendererHelper.ENTITY_ROTATION || helper == ItemRendererHelper.ENTITY_BOBBING
            || helper == ItemRendererHelper.EQUIPPED_BLOCK
            || helper == ItemRendererHelper.INVENTORY_BLOCK;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        WarheadCasing casing = InertWarheads.casing(item);
        String payload = InertWarheads.payload(item);
        String model = WarheadModels.modelName(casing, payload == null ? WarheadData.DEFAULT_EXPLOSIVE : payload);
        ObjDraw.tint = INERT_TINT;
        try {
            WarheadBlockRenderer.drawItemModel(model);
        } finally {
            ObjDraw.tint = 1.0F;
        }
    }
}
