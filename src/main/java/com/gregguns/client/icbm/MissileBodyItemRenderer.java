package com.gregguns.client.icbm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import com.gregguns.icbm.missile.MissileBlocks;
import com.gregguns.icbm.missile.MissileTier;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws a missile body as an item (inventory slot, NEI, hand, dropped): the full-height model with its own texture
 * file bound directly, so it never depends on the block atlas, scaled to fit the slot and centered. A body is tall
 * and thin, so in a slot it is a narrow full-height shape. Client only.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-bodies.md (Model and rendering)
@SideOnly(Side.CLIENT)
public final class MissileBodyItemRenderer implements IItemRenderer {

    private final MissileTier tier;

    private MissileBodyItemRenderer(MissileTier tier) {
        this.tier = tier;
    }

    /** Registers a renderer for each tier's body item. Call once the blocks exist (the client module's init). */
    public static void register() {
        for (MissileTier t : MissileTier.values()) {
            Item item = Item.getItemFromBlock(MissileBlocks.body(t));
            if (item != null) {
                MinecraftForgeClient.registerItemRenderer(item, new MissileBodyItemRenderer(t));
            }
        }
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type == ItemRenderType.INVENTORY || type == ItemRenderType.EQUIPPED
            || type == ItemRenderType.EQUIPPED_FIRST_PERSON
            || type == ItemRenderType.ENTITY;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        // The standard block-in-slot transform for the inventory; the rest is placed below.
        return type == ItemRenderType.INVENTORY && helper == ItemRendererHelper.INVENTORY_BLOCK;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        float h = tier.heightBlocks();
        GL11.glPushMatrix();
        switch (type) {
            case INVENTORY:
                // Origin at the center of a block-sized cell: fit the whole height into the slot, centered.
                float fit = 1.8F / h;
                GL11.glScalef(fit, fit, fit);
                GL11.glTranslatef(-0.5F, -h / 2.0F, -0.5F);
                break;
            case ENTITY:
                GL11.glScalef(0.3F, 0.3F, 0.3F);
                GL11.glTranslatef(-0.5F, 0.0F, -0.5F);
                break;
            default:
                // In the hand: stood upright, scaled down so it does not cover the screen.
                GL11.glScalef(0.3F, 0.3F, 0.3F);
                GL11.glTranslatef(0.5F, 0.2F, 0.5F);
                break;
        }
        Minecraft.getMinecraft().renderEngine.bindTexture(WarheadModels.bodyTextureFile(tier));
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setBrightness(0xF000F0);
        ObjDraw.drawRaw(WarheadModels.body(tier));
        t.draw();
        GL11.glPopMatrix();
    }
}
