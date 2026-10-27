package com.gregguns.client.icbm;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import com.gregguns.icbm.warhead.ItemBlockWarhead;
import com.gregguns.icbm.warhead.WarheadBlocks;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadStacks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws a warhead item whose explosive has its own model (the nuclear warhead) in the inventory, the hand and on the
 * ground. The block handler cannot do it: {@code renderInventoryBlock} gets no stack. Any other warhead item is left
 * to the block handler (this renderer declines it). Client only.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Model and rendering)
@SideOnly(Side.CLIENT)
final class WarheadItemRenderer implements IItemRenderer {

    private final WarheadCasing casing;

    private WarheadItemRenderer(WarheadCasing casing) {
        this.casing = casing;
    }

    /** Registers the renderer on the casing items that have an explosive-specific model. Call from init. */
    static void register() {
        for (WarheadCasing c : WarheadCasing.values()) {
            Item item = Item.getItemFromBlock(WarheadBlocks.of(c));
            if (item instanceof ItemBlockWarhead && hasSpecificModel(c)) {
                MinecraftForgeClient.registerItemRenderer(item, new WarheadItemRenderer(c));
            }
        }
    }

    private static boolean hasSpecificModel(WarheadCasing c) {
        return !WarheadModels.modelName(c, "nuclear")
            .equals(c.blockName())
            || !WarheadModels.modelName(c, "emp")
                .equals(c.blockName());
    }

    /** @return the model name for this stack: the nuclear model for a nuclear stack, else the casing's own */
    private String modelFor(ItemStack stack) {
        return WarheadModels.modelName(
            casing,
            WarheadStacks.read(casing, stack)
                .explosive());
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        if (item == null || type == ItemRenderType.FIRST_PERSON_MAP) {
            return false;
        }
        return !modelFor(item).equals(casing.blockName());
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        // The same block-style transforms the block handler gets: spin on the ground, tilt in the inventory.
        return helper == ItemRendererHelper.ENTITY_ROTATION || helper == ItemRendererHelper.ENTITY_BOBBING
            || helper == ItemRendererHelper.EQUIPPED_BLOCK
            || helper == ItemRendererHelper.INVENTORY_BLOCK;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        WarheadBlockRenderer.drawItemModel(modelFor(item));
    }
}
