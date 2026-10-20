package com.gregguns.client.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.gregguns.GregGuns;
import com.gregguns.client.render.GlStateGuard;
import com.gregguns.utility.storage.BlockGregChest;
import com.gregguns.utility.storage.TileGregChest;

/**
 * Draws the single chests with the vanilla chest model and each chest's own texture sheet: yaw from the metadata and
 * the
 * lid angle with the vanilla cubic ease-out. Client only. The item form is a placeholder.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 1 (World and item rendering)
public final class ChestRenderer extends TileEntitySpecialRenderer {

    /** Texture of the Reinforced Chest. */
    public static final ResourceLocation REINFORCED = new ResourceLocation(
        GregGuns.MODID,
        "textures/models/machines/reinforced_chest.png");
    /** Texture of the Weapon Chest. */
    public static final ResourceLocation WEAPON = new ResourceLocation(
        GregGuns.MODID,
        "textures/models/machines/weapon_chest.png");

    private final ModelChest model = new ModelChest();

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partial) {
        TileGregChest chest = (TileGregChest) tile;
        boolean weapon = chest.getBlockType() instanceof BlockGregChest
            && ((BlockGregChest) chest.getBlockType()).isWeaponChest();
        int meta = chest.getWorldObj() == null ? 3 : chest.getBlockMetadata();
        float yaw = meta == 2 ? 180.0F : meta == 4 ? 90.0F : meta == 5 ? -90.0F : 0.0F;
        bindTexture(weapon ? WEAPON : REINFORCED);
        try (GlStateGuard g = GlStateGuard.matrixAndAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT)) {
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glTranslatef((float) x, (float) y + 1.0F, (float) z + 1.0F);
            GL11.glScalef(1.0F, -1.0F, -1.0F);
            GL11.glTranslatef(0.5F, 0.5F, 0.5F);
            GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
            float lid = chest.prevLidAngle + (chest.lidAngle - chest.prevLidAngle) * partial;
            lid = 1.0F - lid;
            lid = 1.0F - lid * lid * lid;
            model.chestLid.rotateAngleX = -(lid * (float) Math.PI / 2.0F);
            model.renderAll();
        }
        GlStateGuard.rebindBlockAtlas();
    }

    /** Item form: the same model, closed, on one texture. Placeholder transforms; visual check owed. */
    public static final class ItemForm implements IItemRenderer {

        private final ModelChest model = new ModelChest();
        private final ResourceLocation texture;

        /** @param texture the chest's texture sheet */
        public ItemForm(ResourceLocation texture) {
            this.texture = texture;
        }

        @Override
        public boolean handleRenderType(ItemStack item, ItemRenderType type) {
            return true;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
            return true;
        }

        @Override
        public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
            Minecraft.getMinecraft().renderEngine.bindTexture(texture);
            try (GlStateGuard g = GlStateGuard.matrixAndAttrib(GL11.GL_CURRENT_BIT)) {
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                switch (type) {
                    case INVENTORY:
                        GL11.glTranslatef(0.0F, -0.1F, 0.0F);
                        GL11.glScalef(1.0F, -1.0F, -1.0F);
                        GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
                        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
                        break;
                    case EQUIPPED:
                    case EQUIPPED_FIRST_PERSON:
                        GL11.glTranslatef(0.5F, 0.6F, 0.5F);
                        GL11.glScalef(1.0F, -1.0F, -1.0F);
                        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
                        break;
                    default:
                        GL11.glScalef(1.0F, -1.0F, -1.0F);
                        GL11.glTranslatef(0.0F, -1.0F, 0.0F);
                        break;
                }
                model.renderAll();
            }
            GlStateGuard.rebindBlockAtlas();
        }
    }
}
