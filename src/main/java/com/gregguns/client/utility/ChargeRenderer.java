package com.gregguns.client.utility;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import com.gregguns.client.render.GlStateGuard;
import com.gregguns.utility.charge.ChargeType;
import com.gregguns.utility.charge.TileExplosiveCharge;

/**
 * Placeholder renderers of the explosive charges: a tile renderer for the placed block and an item renderer for the
 * inventory, hand and dropped forms. Both draw {@code ChargeModel} through {@link ChargeBoxRenderer}. Client only.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (World rendering)
public final class ChargeRenderer extends TileEntitySpecialRenderer implements IItemRenderer {

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileExplosiveCharge)) {
            return;
        }
        TileExplosiveCharge charge = (TileExplosiveCharge) tile;
        try (GlStateGuard g = GlStateGuard.matrixAndAttrib(GL11.GL_CURRENT_BIT)) {
            GL11.glTranslated(x, y, z);
            ChargeBoxRenderer.draw(
                charge.params()
                    .type(),
                charge.orientation(),
                charge.params()
                    .armed());
        }
        GlStateGuard.rebindBlockAtlas();
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    /**
     * Asks Forge for the vanilla block pose in every type: the inventory slot's isometric cube, the in-hand block grip
     * (first and third person) and the dropped block's bobbing and spin. Each puts the origin at the middle of a
     * block-sized cube, which {@link ChargeItemPose} centers the model on. {@code BLOCK_3D} is what the hand
     * renderers read to pick the block grip (smaller, tilted) over the flat-item grip.
     */
    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        switch (helper) {
            case BLOCK_3D:
            case ENTITY_BOBBING:
            case ENTITY_ROTATION:
            case EQUIPPED_BLOCK:
            case INVENTORY_BLOCK:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        ChargeType charge = ChargeType.fromMeta(item.getItemDamage());
        boolean inHand = type == ItemRenderType.EQUIPPED || type == ItemRenderType.EQUIPPED_FIRST_PERSON;
        float[] move = ChargeItemPose.translation(charge, inHand);
        try (GlStateGuard g = GlStateGuard.matrixAndAttrib(GL11.GL_CURRENT_BIT)) {
            GL11.glTranslatef(move[0], move[1], move[2]);
            // Wall pose: upright, plate toward the viewer (+z), the way the model notes show the item.
            ChargeBoxRenderer.draw(charge, ChargeItemPose.ITEM_ORIENTATION, false);
        }
        GlStateGuard.rebindBlockAtlas();
    }
}
