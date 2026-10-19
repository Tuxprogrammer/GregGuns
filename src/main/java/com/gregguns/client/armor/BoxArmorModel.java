package com.gregguns.client.armor;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

import org.jetbrains.annotations.Nullable;

import com.gregguns.armor.model.ArmorBoxDef;
import com.gregguns.armor.model.ArmorModelDef;
import com.gregguns.armor.model.ArmorModels;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A worn-armor model built from an {@link ArmorModelDef}: a {@link ModelBiped} whose six parts carry the set's own
 * boxes instead of the vanilla ones. One instance draws one piece (helmet, chestplate, leggings or boots), so Forge's
 * per-slot part visibility and the vanilla walk, sneak and aim animation keep working: every box is a child of a
 * biped part and follows its rotation.
 *
 * <p>
 * Client only. Instances are cached per set and piece by {@link #forPiece}.
 */
// Spec: docs/gregguns-spec/05-armor/implementation-notes.md ARM-DEV-20
@SideOnly(Side.CLIENT)
public final class BoxArmorModel extends ModelBiped {

    private static final Map<ArmorBoxDef.Slot, Map<String, BoxArmorModel>> CACHE = new EnumMap<>(
        ArmorBoxDef.Slot.class);

    /**
     * @param def  the set's boxes
     * @param slot the piece this instance draws; only boxes of this piece are added
     */
    public BoxArmorModel(ArmorModelDef def, ArmorBoxDef.Slot slot) {
        super(0.0F, 0.0F, def.textureWidth(), def.textureHeight());
        // The vanilla constructor made its own boxes; this model draws only the set's.
        bipedHead.cubeList.clear();
        bipedHeadwear.cubeList.clear();
        bipedBody.cubeList.clear();
        bipedRightArm.cubeList.clear();
        bipedLeftArm.cubeList.clear();
        bipedRightLeg.cubeList.clear();
        bipedLeftLeg.cubeList.clear();
        bipedEars.cubeList.clear();
        bipedCloak.cubeList.clear();
        for (ArmorBoxDef b : def.boxesFor(slot)) {
            part(b.part()).addChild(build(b));
        }
    }

    private ModelRenderer part(ArmorBoxDef.Part p) {
        switch (p) {
            case HEAD:
                return bipedHead;
            case BODY:
                return bipedBody;
            case RIGHT_ARM:
                return bipedRightArm;
            case LEFT_ARM:
                return bipedLeftArm;
            case RIGHT_LEG:
                return bipedRightLeg;
            default:
                return bipedLeftLeg;
        }
    }

    private ModelRenderer build(ArmorBoxDef b) {
        ModelRenderer r = new ModelRenderer(this, b.u(), b.v());
        // Source: Forge 1.7.10 / vanilla ModelRenderer: the mirror flag must be set before addBox, which reads it.
        r.mirror = b.mirror();
        float[] off = b.offset();
        int[] size = b.size();
        r.addBox(off[0], off[1], off[2], size[0], size[1], size[2], b.inflate());
        float[] pivot = b.pivot();
        if (pivot != null) {
            r.setRotationPoint(pivot[0], pivot[1], pivot[2]);
        } else {
            r.setRotationPoint(0.0F, 0.0F, 0.0F);
        }
        float[] rot = b.rotateDegrees();
        if (rot != null) {
            r.rotateAngleX = (float) Math.toRadians(rot[0]);
            r.rotateAngleY = (float) Math.toRadians(rot[1]);
            r.rotateAngleZ = (float) Math.toRadians(rot[2]);
        }
        return r;
    }

    /**
     * @param setId     a set id
     * @param armorType the vanilla armor type of the piece, 0 helmet to 3 boots
     * @return the cached model for that piece, or null when the set has no custom model (the caller then lets vanilla
     *         draw its layer)
     */
    public static synchronized @Nullable BoxArmorModel forPiece(String setId, int armorType) {
        ArmorModelDef def = ArmorModels.get(setId);
        if (def == null) {
            return null;
        }
        ArmorBoxDef.Slot slot = ArmorBoxDef.Slot.fromArmorType(armorType);
        Map<String, BoxArmorModel> bySet = CACHE.get(slot);
        if (bySet == null) {
            bySet = new java.util.HashMap<>();
            CACHE.put(slot, bySet);
        }
        BoxArmorModel m = bySet.get(setId);
        if (m == null) {
            m = new BoxArmorModel(def, slot);
            bySet.put(setId, m);
        }
        return m;
    }
}
