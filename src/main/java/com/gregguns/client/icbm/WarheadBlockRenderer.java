package com.gregguns.client.icbm;

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

import com.gregguns.icbm.missile.MissileSeat;
import com.gregguns.icbm.warhead.BlockWarhead;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadRender;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Draws a placed warhead as its OBJ model, added to the chunk mesh like any block (an ISBRH, not a per-tick renderer,
 * D31). Texture coordinates are mapped into the block atlas sprite registered by {@link WarheadSprite}. Inventory,
 * hand and dropped items use the same method. Client only.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Model and rendering)
@SideOnly(Side.CLIENT)
public final class WarheadBlockRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;
    private final Map<String, IIcon> icons = new java.util.HashMap<>();

    private WarheadBlockRenderer(int renderId) {
        this.renderId = renderId;
    }

    /** Registers the handler and the atlas sprites. Call once from the client module's preInit. */
    public static void register() {
        int id = RenderingRegistry.getNextAvailableRenderId();
        WarheadRender.id = id;
        WarheadBlockRenderer handler = new WarheadBlockRenderer(id);
        RenderingRegistry.registerBlockHandler(handler);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(handler);
    }

    /** @param event the atlas stitch: adds the sprites before the block atlas, fetches the icons after it */
    @SubscribeEvent
    public void onStitchPre(TextureStitchEvent.Pre event) {
        if (event.map.getTextureType() != 0) {
            return;
        }
        for (String c : WarheadModels.allNames()) {
            event.map.setTextureEntry(
                WarheadModels.spriteName(c),
                new WarheadSprite(WarheadModels.spriteName(c), WarheadModels.textureFile(c)));
        }
    }

    /** @param event after stitching: caches the icons */
    @SubscribeEvent
    public void onStitchPost(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0) {
            return;
        }
        icons.clear();
        for (String c : WarheadModels.allNames()) {
            icons.put(c, event.map.getAtlasSprite(WarheadModels.spriteName(c)));
        }
        WarheadModels.clear();
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        WarheadCasing casing = ((BlockWarhead) block).casing();
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setBrightness(0xF000F0);
        draw(casing.blockName(), 0, 0, 0);
        t.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        // A warhead seated on a body that stands on a pad is drawn down with it (see MissileSeat).
        String name = WarheadModels.modelName(((BlockWarhead) block).casing(), explosiveAt(world, x, y, z));
        return ObjDraw
            .draw(WarheadModels.get(name), icons.get(name), x, y - MissileSeat.warheadDrop(world, x, y, z), z);
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }

    private boolean draw(String name, int bx, int by, int bz) {
        return ObjDraw.draw(WarheadModels.get(name), icons.get(name), bx, by, bz);
    }

    /** @return the placed warhead's explosive id, or null when the tile is not loaded on this client yet */
    private static String explosiveAt(IBlockAccess world, int x, int y, int z) {
        net.minecraft.tileentity.TileEntity te = world.getTileEntity(x, y, z);
        return te instanceof com.gregguns.icbm.warhead.TileWarhead ? ((com.gregguns.icbm.warhead.TileWarhead) te).data()
            .explosive() : null;
    }
}
