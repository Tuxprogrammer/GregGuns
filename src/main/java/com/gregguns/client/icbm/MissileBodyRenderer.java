package com.gregguns.client.icbm;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.TextureStitchEvent;

import org.lwjgl.opengl.GL11;

import com.gregguns.icbm.missile.BlockMissileBody;
import com.gregguns.icbm.missile.MissileRender;
import com.gregguns.icbm.missile.MissileSeat;
import com.gregguns.icbm.missile.MissileTier;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws a placed missile body as its full-height OBJ model, added to the chunk mesh like the warheads (D31): a static
 * model, no per-tick renderer. On a Galacticraft pad the model is drawn 0.8 lower so it stands on the pad's surface.
 * Client only.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-bodies.md (Model and rendering)
@SideOnly(Side.CLIENT)
public final class MissileBodyRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;
    private final Map<MissileTier, IIcon> icons = new EnumMap<>(MissileTier.class);

    private MissileBodyRenderer(int renderId) {
        this.renderId = renderId;
    }

    /** Registers the handler and the atlas sprites. Call once from the client module's preInit. */
    public static void register() {
        int id = RenderingRegistry.getNextAvailableRenderId();
        MissileRender.id = id;
        MissileBodyRenderer handler = new MissileBodyRenderer(id);
        RenderingRegistry.registerBlockHandler(handler);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(handler);
    }

    /** @param event the atlas stitch: adds the sprites */
    @SubscribeEvent
    public void onStitchPre(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) {
            return;
        }
        for (MissileTier t : MissileTier.values()) {
            event.map.setTextureEntry(
                WarheadModels.bodySpriteName(t),
                new WarheadSprite(WarheadModels.bodySpriteName(t), WarheadModels.bodyTextureFile(t)));
        }
    }

    /** @param event after stitching: caches the icons */
    @SubscribeEvent
    public void onStitchPost(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0) {
            return;
        }
        icons.clear();
        for (MissileTier t : MissileTier.values()) {
            icons.put(t, event.map.getAtlasSprite(WarheadModels.bodySpriteName(t)));
        }
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        MissileTier tier = ((BlockMissileBody) block).missileTier();
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        // The model is several blocks tall; scale it into the item slot and center it.
        float scale = 1.0F / (tier.heightBlocks() * 0.55F);
        GL11.glScalef(scale, scale, scale);
        GL11.glTranslatef(-0.5F, -tier.heightBlocks() / 2.0F, -0.5F);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setBrightness(0xF000F0);
        ObjDraw.draw(WarheadModels.body(tier), icons.get(tier), 0, 0, 0);
        t.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        MissileTier tier = ((BlockMissileBody) block).missileTier();
        Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        return ObjDraw.draw(WarheadModels.body(tier), icons.get(tier), x, y - MissileSeat.bodyDrop(world, x, y, z), z);
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }
}
