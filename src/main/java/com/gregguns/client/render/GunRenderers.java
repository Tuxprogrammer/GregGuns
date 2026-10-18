package com.gregguns.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.MinecraftForgeClient;

import com.gregguns.GregGuns;
import com.gregguns.client.render.profile.BoltActionRifleProfile;
import com.gregguns.client.render.profile.GunTransforms;
import com.gregguns.client.render.profile.HandCannonProfile;
import com.gregguns.client.render.profile.Pistol9mmProfile;
import com.gregguns.client.render.profile.RevolverProfile;
import com.gregguns.client.render.profile.SawedOffProfile;
import com.gregguns.client.render.profile.ScopeSpec;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The registry of gun item renderers. The gun item's client side calls {@link #register} once per gun, during client
 * init (the item must already be registered):
 *
 * <pre>
 * {@code
 * GunRenderers.registerPistol9mm(GunItems.PISTOL_9MM);
 * // or, for any gun:
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
        if (item == null || model == null || transforms == null) {
            throw new IllegalArgumentException("item, model and transforms are required");
        }
        if (REGISTRY.put(item, new Entry(transforms)) != null) {
            GregGuns.LOG.warn("Gun renderer for {} registered twice; the later one wins", item.getUnlocalizedName());
        }
        MinecraftForgeClient.registerItemRenderer(item, new GunItemRenderer(model, transforms));
        if (transforms.scope() != null) {
            ScopeTextures.install();
        }
    }

    /**
     * Registers the 9mm Pistol: the original box model built against {@code pistol_9mm.png}, and its sheet
     * transforms.
     */
    public static void registerPistol9mm(Item pistol) {
        register(
            pistol,
            new BoxGunModel(
                Pistol9mmProfile.BOXES,
                2,
                Pistol9mmProfile.TEXTURE_WIDTH,
                Pistol9mmProfile.TEXTURE_HEIGHT,
                Pistol9mmProfile.MODEL_SCALE,
                new String[] { Pistol9mmProfile.TEXTURE },
                Pistol9mmProfile.MUZZLE),
            Pistol9mmProfile.TRANSFORMS);
    }

    /**
     * Registers the Sawed Off: the original X-family box model built against {@code sawed_off.png}, and its sheet
     * transforms.
     */
    public static void registerSawedOff(Item sawedOff) {
        register(
            sawedOff,
            new BoxGunModel(
                SawedOffProfile.BOXES,
                1,
                SawedOffProfile.TEXTURE_WIDTH,
                SawedOffProfile.TEXTURE_HEIGHT,
                SawedOffProfile.MODEL_SCALE,
                new String[] { SawedOffProfile.TEXTURE },
                SawedOffProfile.MUZZLE,
                true),
            SawedOffProfile.TRANSFORMS);
    }

    /**
     * Registers the Revolver: the original X-family box model built against {@code revolver.png}, and its sheet
     * transforms.
     */
    public static void registerRevolver(Item revolver) {
        register(
            revolver,
            new BoxGunModel(
                RevolverProfile.BOXES,
                1,
                RevolverProfile.TEXTURE_WIDTH,
                RevolverProfile.TEXTURE_HEIGHT,
                RevolverProfile.MODEL_SCALE,
                new String[] { RevolverProfile.TEXTURE },
                RevolverProfile.MUZZLE,
                true),
            RevolverProfile.TRANSFORMS);
    }

    /**
     * Registers the Hand Cannon: the original X-family box model built against {@code handcannon.png}, and its sheet
     * transforms.
     */
    public static void registerHandCannon(Item handcannon) {
        register(
            handcannon,
            new BoxGunModel(
                HandCannonProfile.BOXES,
                1,
                HandCannonProfile.TEXTURE_WIDTH,
                HandCannonProfile.TEXTURE_HEIGHT,
                HandCannonProfile.MODEL_SCALE,
                new String[] { HandCannonProfile.TEXTURE },
                HandCannonProfile.MUZZLE,
                true),
            HandCannonProfile.TRANSFORMS);
    }

    /**
     * Registers the Bolt Action Rifle: the original X-family box model built against {@code bolt_action_rifle.png}
     * (three camo textures, chosen by the stack's camo index), its sheet transforms and its scope overlay.
     */
    public static void registerBoltAction(Item rifle) {
        register(
            rifle,
            new BoxGunModel(
                BoltActionRifleProfile.BOXES,
                1,
                BoltActionRifleProfile.TEXTURE_WIDTH,
                BoltActionRifleProfile.TEXTURE_HEIGHT,
                BoltActionRifleProfile.MODEL_SCALE,
                BoltActionRifleProfile.TEXTURES,
                BoltActionRifleProfile.MUZZLE,
                true),
            BoltActionRifleProfile.TRANSFORMS);
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

    /** @return the transform data of a registered gun, or null */
    static GunTransforms transformsOf(Item item) {
        Entry e = REGISTRY.get(item);
        return e == null ? null : e.transforms;
    }
}
