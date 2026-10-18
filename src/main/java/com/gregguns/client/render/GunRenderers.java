package com.gregguns.client.render;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.MinecraftForgeClient;

import com.gregguns.GregGuns;
import com.gregguns.client.render.profile.GunTransforms;
import com.gregguns.client.render.profile.Pistol9mmProfile;

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
