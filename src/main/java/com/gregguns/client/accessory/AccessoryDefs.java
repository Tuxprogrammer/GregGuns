package com.gregguns.client.accessory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.client.accessory.AccessoryDef.Mount;

/**
 * The eight accessory models, authored as boxes. <b>The box lists below are PLACEHOLDERS</b>: rough shapes that make
 * every accessory visible while a clean-room builder writes the real ones from a written spec. To replace a model,
 * rewrite only its own private method ({@code gasMask()}, {@code goggles()}, ...) between its
 * {@code BEGIN BOXES}/{@code END BOXES} marks; everything else (rendering, textures, tests, exporter) follows from the
 * data. Texture sheets are repainted by {@code tools/asset-gen/accessory_models.py} (one painter per accessory, also
 * placeholders).
 *
 * <p>
 * Head models sit in the head's frame (head box: x -4..4, y -8..0, z -4..4, face toward -Z). Back models sit in the
 * torso's frame with z = 0 on the back surface, growing outward. Pure; safe on either side.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (worn models); docs/gregguns-spec/03-gun-visuals-audio (box
// models)
public final class AccessoryDefs {

    /** Gas Mask (helmet slot). */
    public static final AccessoryDef GAS_MASK = gasMask();
    /** Oxygen Mask (head bauble slot or Galacticraft mask slot). */
    public static final AccessoryDef OXYGEN_MASK = oxygenMask();
    /** Tactical Mask (helmet slot), four camos. */
    public static final AccessoryDef TACTICAL_MASK = tacticalMask();
    /** Night Vision Goggles (head bauble slot). */
    public static final AccessoryDef NIGHT_VISION_GOGGLES = goggles();
    /** Oxygen Tanks (wings slot or Galacticraft tank slots). */
    public static final AccessoryDef OXYGEN_TANKS = oxygenTanks();
    /** Jump Pack (wings slot). */
    public static final AccessoryDef JUMP_PACK = jumpPack();
    /** Anti Gravity Device (wings slot), five camos. */
    public static final AccessoryDef ANTI_GRAVITY_PACK = antiGravityPack();
    /** Glider (wings slot); parts 1 and 2 are the outer wings, hinged to fold. */
    public static final AccessoryDef GLIDER = glider();

    /** Every model, in the order the exporter writes them. */
    public static final List<AccessoryDef> ALL = Collections.unmodifiableList(
        Arrays.asList(
            GAS_MASK,
            OXYGEN_MASK,
            TACTICAL_MASK,
            NIGHT_VISION_GOGGLES,
            OXYGEN_TANKS,
            JUMP_PACK,
            ANTI_GRAVITY_PACK,
            GLIDER));

    private AccessoryDefs() {}

    /**
     * @param id a model id
     * @return the model, or null when there is none
     */
    public static AccessoryDef byId(String id) {
        for (AccessoryDef def : ALL) {
            if (def.id()
                .equals(id)) {
                return def;
            }
        }
        return null;
    }

    private static AccessoryDef gasMask() {
        return AccessoryDef.builder("gas_mask", Mount.HEAD, 64, 64, 1.0f / 16.0f)
            // BEGIN BOXES (placeholder)
            .textures("gas_mask")
            // A full-head shell a little larger than the head, the way a helmet layer is.
            .box("shell", 9, 9, 9, -4.5f, -8.5f, -4.5f)
            .box("mouthpiece", 4, 4, 1, -2.0f, -3.5f, -5.5f)
            .box("filter", 4, 4, 3, -2.0f, -2.8f, -8.8f)
            .pivot(0.0f, -0.8f, -7.3f)
            .rot(-25.0f, 0.0f, 0.0f)
            // END BOXES
            .build();
    }

    private static AccessoryDef oxygenMask() {
        return AccessoryDef.builder("oxygen_mask", Mount.HEAD, 64, 64, 1.0f / 16.0f)
            // BEGIN BOXES (placeholder)
            .textures("oxygen_mask")
            .box("strap_front", 10, 1, 1, -5.0f, -5.0f, -5.0f)
            .box("strap_back", 10, 1, 1, -5.0f, -5.0f, 4.0f)
            .box("strap_left", 1, 1, 8, 4.0f, -5.0f, -4.0f)
            .box("strap_right", 1, 1, 8, -5.0f, -5.0f, -4.0f)
            .box("cup", 5, 3, 2, -2.5f, -4.0f, -6.0f)
            .box("valve", 1, 1, 1, -0.5f, -6.0f, -5.0f)
            .box("hose", 1, 1, 2, 2.5f, -2.5f, -4.0f)
            // END BOXES
            .build();
    }

    private static AccessoryDef tacticalMask() {
        return AccessoryDef.builder("tactical_mask", Mount.HEAD, 64, 64, 1.0f / 16.0f)
            // BEGIN BOXES (placeholder)
            .textures("tactical_mask", "tactical_mask_green", "tactical_mask_white", "tactical_mask_lightgreen")
            .box("hood", 9, 9, 9, -4.5f, -8.5f, -4.5f)
            // END BOXES
            .build();
    }

    private static AccessoryDef goggles() {
        return AccessoryDef.builder("night_vision_goggles", Mount.HEAD, 64, 64, 1.0f / 16.0f)
            // BEGIN BOXES (placeholder)
            .textures("night_vision_goggles")
            .box("band_front", 10, 2, 1, -5.0f, -7.0f, -5.0f)
            .box("band_left", 1, 2, 9, 4.0f, -7.0f, -4.0f)
            .box("band_right", 1, 2, 9, -5.0f, -7.0f, -4.0f)
            .box("strap", 8, 1, 1, -4.0f, -6.5f, 4.0f)
            .box("bridge", 2, 5, 1, -1.0f, -7.0f, -6.0f)
            .box("mount_left", 1, 1, 4, 1.7f, -6.0f, -9.0f)
            .pivot(2.2f, -5.5f, -5.0f)
            .rot(40.0f, 0.0f, 0.0f)
            .box("mount_right", 1, 1, 4, -2.7f, -6.0f, -9.0f)
            .pivot(-2.2f, -5.5f, -5.0f)
            .rot(40.0f, 0.0f, 0.0f)
            .box("lens_left", 2, 2, 4, 1.2f, -4.0f, -9.0f)
            .rot(0.0f, 0.0f, 45.0f)
            .box("lens_right", 2, 2, 4, -3.2f, -4.0f, -9.0f)
            .rot(0.0f, 0.0f, 45.0f)
            // END BOXES
            .build();
    }

    private static AccessoryDef oxygenTanks() {
        return AccessoryDef.builder("oxygen_tanks", Mount.BACK, 64, 64, 1.0f / 16.0f)
            // BEGIN BOXES
            // Source: docs/techguns-spec/05-armor/model-notes/oxygen_tanks.md (notes z minus 2: z = 0 is the back).
            .textures("oxygen_tanks")
            .box("tank_right", 3, 9, 3, -4.0f, 0.0f, 0.5f)
            .box("tank_left", 3, 9, 3, 1.0f, 0.0f, 0.5f)
            .box("harness", 2, 6, 3, -1.0f, 1.0f, 0.0f)
            .box("valve_right", 2, 1, 2, -3.5f, -1.0f, 1.0f)
            .box("valve_left", 2, 1, 2, 1.5f, -1.0f, 1.0f)
            .build();
    }

    private static AccessoryDef jumpPack() {
        return AccessoryDef.builder("jump_pack", Mount.BACK, 64, 64, 1.0f / 16.0f)
            // BEGIN BOXES
            // Source: docs/techguns-spec/05-armor/model-notes/jump_pack.md (notes z minus 2: z = 0 is the back).
            .textures("jump_pack")
            .box("spine", 4, 7, 4, -2.0f, 1.0f, 0.0f)
            .box("cylinder_right", 3, 9, 3, -5.0f, 0.0f, 0.5f)
            .box("cylinder_left", 3, 9, 3, 2.0f, 0.0f, 0.5f)
            .box("cap_right", 2, 2, 2, -4.5f, -2.0f, 1.0f)
            .box("cap_left", 2, 2, 2, 2.5f, -2.0f, 1.0f)
            .build();
    }

    private static AccessoryDef antiGravityPack() {
        return AccessoryDef.builder("anti_gravity_pack", Mount.BACK, 64, 64, 1.0f / 16.0f)
            // BEGIN BOXES
            // Source: docs/techguns-spec/05-armor/model-notes/anti_gravity_pack.md (notes z minus 2: z = 0 is the
            // back).
            .textures(
                "anti_gravity_pack",
                "anti_gravity_pack_bronze",
                "anti_gravity_pack_black",
                "anti_gravity_pack_dark_green",
                "anti_gravity_pack_silver")
            .box("slab", 6, 8, 3, -3.0f, 1.0f, 0.0f)
            .box("pillar_right", 2, 10, 2, -5.0f, 0.0f, 0.5f)
            .box("pillar_left", 2, 10, 2, 3.0f, 0.0f, 0.5f)
            .box("plate", 5, 4, 1, -2.5f, 2.0f, 3.0f)
            // The core: a 3x3x3 cube turned 45 degrees about the front-to-back axis, centered at x 0, y 4.
            .box("core", 3, 3, 3, -1.5f, 2.5f, 2.5f)
            .rot(0.0f, 0.0f, 45.0f)
            .box("clamp_right_upper", 2, 1, 1, -4.0f, 2.5f, 2.5f)
            .box("clamp_right_lower", 2, 1, 1, -4.0f, 4.5f, 2.5f)
            .box("clamp_left_upper", 2, 1, 1, 2.0f, 2.5f, 2.5f)
            .box("clamp_left_lower", 2, 1, 1, 2.0f, 4.5f, 2.5f)
            .build();
    }

    private static AccessoryDef glider() {
        return AccessoryDef.builder("glider", Mount.BACK, 64, 64, 1.0f / 16.0f)
            // BEGIN BOXES
            // Source: docs/techguns-spec/05-armor/model-notes/glider.md (notes z minus 2: z = 0 is the back).
            // Part 0 is the folded pack (always worn); parts 1 (left) and 2 (right) are the flat wings, drawn only
            // while deployed. They do not rotate: the hinges just group them.
            .textures("glider")
            .hinge(1, 3.5f, 0.0f, 2.0f)
            .hinge(2, -3.5f, 0.0f, 2.0f)
            .box("pack", 7, 10, 4, -3.5f, 0.5f, 0.0f)
            .box("inner_rim_left", 6, 1, 2, 3.5f, 1.0f, 2.0f)
            .part(1)
            .box("inner_rib_left", 1, 7, 2, 8.5f, 2.0f, 2.0f)
            .part(1)
            .box("inner_sail_left", 5, 7, 1, 3.5f, 2.0f, 2.5f)
            .part(1)
            .box("outer_rim_left", 7, 1, 2, 9.5f, 2.0f, 2.0f)
            .part(1)
            .box("outer_rib_left", 1, 6, 2, 15.5f, 3.0f, 2.0f)
            .part(1)
            .box("outer_sail_left", 6, 6, 1, 9.5f, 3.0f, 2.5f)
            .part(1)
            .box("inner_rim_right", 6, 1, 2, -9.5f, 1.0f, 2.0f)
            .part(2)
            .box("inner_rib_right", 1, 7, 2, -9.5f, 2.0f, 2.0f)
            .part(2)
            .box("inner_sail_right", 5, 7, 1, -8.5f, 2.0f, 2.5f)
            .part(2)
            .box("outer_rim_right", 7, 1, 2, -16.5f, 2.0f, 2.0f)
            .part(2)
            .box("outer_rib_right", 1, 6, 2, -16.5f, 3.0f, 2.0f)
            .part(2)
            .box("outer_sail_right", 6, 6, 1, -15.5f, 3.0f, 2.5f)
            .part(2)
            .build();
    }
}
