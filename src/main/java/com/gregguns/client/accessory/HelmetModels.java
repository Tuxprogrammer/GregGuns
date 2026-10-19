package com.gregguns.client.accessory;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.model.ModelBiped;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The armor models of the helmet-slot masks: a {@code ModelBiped} whose head carries the accessory boxes instead of the
 * plain head box, so the vanilla armor renderer follows the head's turn and the sneak pose by itself and binds the
 * item's layer texture ({@code getArmorTexture}). Models are built on first use and shared, as vanilla shares its
 * armor models. Client only.
 */
@SideOnly(Side.CLIENT)
public final class HelmetModels {

    private static final Map<String, ModelBiped> CACHE = new HashMap<>();

    private HelmetModels() {}

    /**
     * @param id the accessory id, for example {@code gas_mask}
     * @return the armor model for it, or null when there is no such model (the renderer then uses its default)
     */
    public static synchronized ModelBiped forId(String id) {
        ModelBiped model = CACHE.get(id);
        if (model == null) {
            AccessoryDef def = AccessoryDefs.byId(id);
            if (def == null) {
                return null;
            }
            model = new Helmet(new AccessoryModel(def));
            CACHE.put(id, model);
        }
        return model;
    }

    /** A biped whose head shows only the accessory. */
    private static final class Helmet extends ModelBiped {

        Helmet(AccessoryModel accessory) {
            super(0.0f);
            bipedHead.cubeList.clear();
            bipedHeadwear.cubeList.clear();
            bipedHead.addChild(accessory.root());
        }
    }
}
