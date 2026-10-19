package com.gregguns.client.utility;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

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
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        ChargeBoxRenderer.draw(
            charge.params()
                .type(),
            charge.orientation(),
            charge.params()
                .armed());
        GL11.glPopMatrix();
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return helper == ItemRendererHelper.ENTITY_BOBBING || helper == ItemRendererHelper.ENTITY_ROTATION;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        ChargeType charge = ChargeType.fromMeta(item.getItemDamage());
        GL11.glPushMatrix();
        switch (type) {
            case INVENTORY:
                GL11.glScalef(16, 16, 16);
                GL11.glTranslatef(0.0F, 0.2F, 0.0F);
                GL11.glRotatef(30, 1, 0, 0);
                GL11.glRotatef(45, 0, 1, 0);
                GL11.glTranslatef(-0.5F, -0.2F, -0.5F);
                break;
            case ENTITY:
                GL11.glTranslatef(-0.5F, -0.25F, -0.5F);
                break;
            default:
                GL11.glTranslatef(0.2F, 0.3F, 0.2F);
                break;
        }
        // Wall pose: upright, plate toward the viewer (+z), the way the model notes show the item.
        ChargeBoxRenderer.draw(charge, 3, false);
        GL11.glPopMatrix();
    }
}
