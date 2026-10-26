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
    private float uScale = 1.0F;
    private float vScale = 1.0F;

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
        // Source: Forge 1.7.10 TextureMap.loadTextureAtlas: for a custom-loader sprite it calls
        // "if (!sprite.load(...)) stitcher.addSprite(sprite);" and moves on, so false means "stitch me" and true means
        // "already handled, leave it out of the atlas". Returning true here left the sprite unstitched (missing
        // texture).
        // Decompiled: confirmed by the black and magenta models of the I1 client shots.
        try (InputStream in = manager.getResource(file)
            .getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            // Source: Forge 1.7.10 TextureAtlasSprite.loadSprite throws "broken aspect ratio and not an animation" for
            // a
            // sprite that is not square, which left the 256x128 body textures out of the atlas (magenta in the world
            // and in the inventory). Pad to a square canvas and tell the drawer how much of it the texture covers.
            // Decompiled: confirmed by the I2 client shots (magenta body block).
            int size = Math.max(image.getWidth(), image.getHeight());
            if (image.getWidth() != image.getHeight()) {
                BufferedImage square = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                square.getGraphics()
                    .drawImage(image, 0, 0, null);
                uScale = image.getWidth() / (float) size;
                vScale = image.getHeight() / (float) size;
                image = square;
            }
            int mip = Minecraft.getMinecraft().gameSettings.mipmapLevels;
            BufferedImage[] levels = new BufferedImage[1 + mip];
            levels[0] = image;
            loadSprite(levels, null, Minecraft.getMinecraft().gameSettings.anisotropicFiltering > 1);
            return false;
        } catch (Exception e) {
            GregGuns.LOG.error("Could not load the warhead texture {}", file, e);
            return true;
        }
    }

    /** @return the share of the sprite's width the texture covers, 1 for a square one */
    float uScale() {
        return uScale;
    }

    /** @return the share of the sprite's height the texture covers, 1 for a square one */
    float vScale() {
        return vScale;
    }
}
