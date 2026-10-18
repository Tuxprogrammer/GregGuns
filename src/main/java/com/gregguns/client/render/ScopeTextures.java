package com.gregguns.client.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import com.gregguns.GregGuns;
import com.gregguns.client.anim.ImageScaler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Loads scope overlay textures at a bounded size. The overlays on disk are 2048 or 3072 pixels square; drawing them
 * straight from the resource manager would keep tens of megabytes on the GPU for one HUD quad. This class reads the PNG
 * itself, shrinks it with {@link ImageScaler} to at most {@link ImageScaler#SCOPE_MAX_SIZE} pixels and uploads that as
 * a dynamic texture. The source file is never touched. Textures are loaded when the resource manager (re)loads, and on
 * first use if a load failed or came late. Client only, main thread.
 */
@SideOnly(Side.CLIENT)
public final class ScopeTextures implements IResourceManagerReloadListener {

    private static final ScopeTextures INSTANCE = new ScopeTextures();

    private final Map<String, ResourceLocation> loaded = new HashMap<>();
    private int counter;
    private boolean installed;

    private ScopeTextures() {}

    /** Hooks the resource reload so textures are rebuilt after F3+T. Call once from client init. */
    static void install() {
        if (INSTANCE.installed) {
            return;
        }
        INSTANCE.installed = true;
        IResourceManager manager = Minecraft.getMinecraft()
            .getResourceManager();
        if (manager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) manager).registerReloadListener(INSTANCE);
        }
    }

    /**
     * @param path the overlay's resource path, for example {@code gregguns:textures/fx/testscope.png}
     * @return the texture to bind: the bounded copy, or the original resource if it could not be loaded
     */
    static ResourceLocation get(String path) {
        ResourceLocation cached = INSTANCE.loaded.get(path);
        if (cached == null) {
            cached = INSTANCE.load(path);
            INSTANCE.loaded.put(path, cached);
        }
        return cached;
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        // The dynamic textures are owned by the texture manager; dropping our handles lets them be rebuilt.
        loaded.clear();
        for (String path : GunRenderers.scopeTexturePaths()) {
            loaded.put(path, load(path));
        }
    }

    private ResourceLocation load(String path) {
        ResourceLocation source = new ResourceLocation(path);
        try (InputStream in = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(source)
            .getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("not an image");
            }
            int w = image.getWidth();
            int h = image.getHeight();
            int[] size = ImageScaler.fit(w, h, ImageScaler.SCOPE_MAX_SIZE);
            int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
            if (size[0] != w || size[1] != h) {
                pixels = ImageScaler.downsample(pixels, w, h, size[0], size[1]);
            }
            DynamicTexture texture = new DynamicTexture(size[0], size[1]);
            System.arraycopy(pixels, 0, texture.getTextureData(), 0, pixels.length);
            texture.updateDynamicTexture();
            GregGuns.LOG.info("Scope overlay {} loaded at {}x{} (source {}x{})", path, size[0], size[1], w, h);
            return Minecraft.getMinecraft()
                .getTextureManager()
                .getDynamicTextureLocation("gregguns_scope_" + counter++, texture);
        } catch (IOException | RuntimeException e) {
            GregGuns.LOG.error("Could not load scope overlay {}; drawing the original", path, e);
            return source;
        }
    }
}
