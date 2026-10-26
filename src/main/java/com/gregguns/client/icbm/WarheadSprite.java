package com.gregguns.client.icbm;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import com.gregguns.GregGuns;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * An atlas sprite that loads its image from a file outside {@code textures/blocks/}. A static block model is drawn
 * into the chunk mesh, which has the block atlas bound, so a model texture must live in the atlas; this lets the
 * texture sit with its model under {@code textures/models/} (D47 layout). Client only.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Model and rendering)
@SideOnly(Side.CLIENT)
final class WarheadSprite extends TextureAtlasSprite {

    private final ResourceLocation file;

    WarheadSprite(String name, ResourceLocation file) {
        super(name);
        this.file = file;
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    @Override
    public boolean load(IResourceManager manager, ResourceLocation location) {
        // Source: Forge 1.7.10 TextureMap skips stitching when load returns false, so true means "stitch me".
        try (InputStream in = manager.getResource(file)
            .getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            int mip = Minecraft.getMinecraft().gameSettings.mipmapLevels;
            BufferedImage[] levels = new BufferedImage[1 + mip];
            levels[0] = image;
            loadSprite(levels, null, Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1);
            return true;
        } catch (Exception e) {
            GregGuns.LOG.error("Could not load the warhead texture {}", file, e);
            return false;
        }
    }
}
