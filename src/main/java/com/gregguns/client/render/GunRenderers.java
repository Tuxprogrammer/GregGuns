package com.gregguns.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.MinecraftForgeClient;

import com.gregguns.GregGuns;
import com.gregguns.client.render.profile.ClientGunDefinition;
import com.gregguns.client.render.profile.GunTransforms;
import com.gregguns.client.render.profile.ScopeSpec;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The registry of gun item renderers. {@code GunClientSetup} calls {@link #register} once per entry of
 * {@code ClientGunDefinitions.ALL}, during client
 * init (the item must already be registered):
 *
 * <pre>
 * {@code
 * GunRenderers.register(Pistol9mmProfile.DEFINITION, GunItems.pistol());
 * // or, with a hand-built model:
 * GunRenderers.register(item, model, transforms);
 * }
 * </pre>
 *
 * It binds a {@link GunItemRenderer} for every render type the framework handles: first person, third person,
 * inventory and dropped item. Client only; reached through {@code GunsClientModule.init}.
 *
 * <p>
 * Spec: rendering-framework.md section 1 (registration).
 */
@SideOnly(Side.CLIENT)
public final class GunRenderers {

    private static final Map<Item, Entry> REGISTRY = new HashMap<>();
    private static ScopeSpec debugScope;

    private GunRenderers() {}

    /** One registered gun. */
    private static final class Entry {

        final GunTransforms transforms;

        Entry(GunTransforms transforms) {
            this.transforms = transforms;
        }
    }

    /**
     * Registers the renderer for a gun item.
     *
     * @param item       the gun item
     * @param model      its model
     * @param transforms its timing, transform rows, muzzle flash and optional scope
     */
    public static void register(Item item, GunModel model, GunTransforms transforms) {
        register(item, model, transforms, false);
    }

    /**
     * As {@link #register(Item, GunModel, GunTransforms)}, for an akimbo gun that draws the model once per hand.
     *
     * @param item       the gun item
     * @param model      the single gun's model
     * @param transforms the single gun's transforms
     * @param akimbo     true to draw two copies in first person (see {@code AkimboLayout})
     */
    public static void register(Item item, GunModel model, GunTransforms transforms, boolean akimbo) {
        if (item == null || model == null || transforms == null) {
            throw new IllegalArgumentException("item, model and transforms are required");
        }
        if (REGISTRY.put(item, new Entry(transforms)) != null) {
            GregGuns.LOG.warn("Gun renderer for {} registered twice; the later one wins", item.getUnlocalizedName());
        }
        MinecraftForgeClient.registerItemRenderer(item, new GunItemRenderer(model, transforms, akimbo));
        if (transforms.scope() != null) {
            ScopeTextures.install();
        }
    }

    /**
     * Registers the renderer for a gun from its client definition: builds the box model (one texture per camo) and
     * binds the item renderer.
     *
     * @param definition the gun's client definition
     * @param item       the registered gun item
     */
    public static void register(ClientGunDefinition definition, Item item) {
        register(
            item,
            new BoxGunModel(
                definition.boxes(),
                definition.parts(),
                definition.textureWidth(),
                definition.textureHeight(),
                definition.modelScale(),
                definition.textures(),
                definition.muzzle(),
                definition.xFamily()),
            definition.transforms(),
            definition.akimbo());
    }

    /**
     * Dev autotest only: draws a scope overlay on the HUD whatever is held, so a screenshot can show how an overlay
     * texture loads and looks before a scoped gun exists. Pass {@code null} to stop.
     *
     * @param texturePath resource path of the overlay, for example {@code gregguns:textures/fx/testscope.png}
     */
    public static void setDebugScope(String texturePath) {
        debugScope = texturePath == null ? null : new ScopeSpec(texturePath, 0.0, 0.0);
        if (debugScope != null) {
            ScopeTextures.install();
        }
    }

    /** @return the forced overlay of {@link #setDebugScope}, or null */
    static ScopeSpec debugScope() {
        return debugScope;
    }

    /** @return true if this item is a registered gun whose render profile has a scope overlay */
    public static boolean hasScopeOverlay(Item item) {
        GunTransforms t = transformsOf(item);
        return t != null && t.scope() != null;
    }

    /** @return the overlay texture paths of every registered gun that has a scope, for the loader to preload */
    static List<String> scopeTexturePaths() {
        List<String> paths = new ArrayList<>();
        for (Entry e : REGISTRY.values()) {
            ScopeSpec scope = e.transforms.scope();
            if (scope != null && !paths.contains(scope.texture())) {
                paths.add(scope.texture());
            }
        }
        return paths;
    }

    /** @return true if this stack is a gun with a registered renderer */
    public static boolean isGun(ItemStack stack) {
        return stack != null && REGISTRY.containsKey(stack.getItem());
    }

    /**
     * The muzzle of a held gun in first-person eye space (x right, y up, negative z forward, blocks), for effects that
     * must start at the barrel and not at the camera.
     *
     * @param stack the held stack
     * @return {@code {x, y, z}}, or null if it is not a gun with a first-person muzzle flash spec
     */
    public static double[] firstPersonMuzzle(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        GunTransforms t = transformsOf(stack.getItem());
        if (t == null || t.muzzleFlash() == null) {
            return null;
        }
        com.gregguns.client.render.profile.MuzzleFlashSpec f = t.muzzleFlash();
        return com.gregguns.client.render.profile.InHandFit.flashFrame(f.scale())
            .apply(new double[] { f.offX(), f.offY(), f.offZ() });
    }

    /** @return the transform data of a registered gun, or null */
    static GunTransforms transformsOf(Item item) {
        Entry e = REGISTRY.get(item);
        return e == null ? null : e.transforms;
    }
}
