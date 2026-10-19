package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The 9mm Pistol's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry. Nothing was taken from Techguns code or box lists. The model texture
 * {@code textures/models/guns/pistol_9mm.png} (128x64) is a packed atlas painted by {@code tools/asset-gen/models.py},
 * and {@code tools/asset-gen/model_layouts.md} lists the seven boxes it was painted for, each as {@code u v w h d} in
 * texels (slide, frame, barrel, grip, magazine, sight, guard). Those sizes and texture origins are used here exactly,
 * so every unwrap rectangle lands on painted pixels (checked by {@code Pistol9mmProfileTest}, which reads the PNG).
 * The positions are authored from the sizes and the one fact the spec records about the shape: a modern pistol with a
 * moving slide over a frame, a barrel poking past the slide, a rear-tilted grip holding a magazine, and a trigger
 * guard (gun-models.md, "Pistol": two parts, frame and slide).
 *
 * <h2>Frame of reference</h2>
 * The boxes are laid out with the muzzle toward +X and Y growing downward (model space), in texels. The model class
 * turns that into the Z-family convention of the pistol sheet (muzzle toward -Z) with a quarter turn about Y, so the
 * transform rows below are the sheet's numbers unchanged. The model is scaled so its overall length matches the
 * 15-unit extent of the sheet's pistol.
 *
 * <p>
 * Spec: gun-sheets.md "Pistol"; rendering-framework.md section 6.1 (Z-family) and section 8 (texture size).
 */
public final class Pistol9mmProfile {

    /** Texture resource path of the model sheet. */
    public static final String TEXTURE = "gregguns:textures/models/guns/pistol_9mm.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 32;
    /** GL units per texel: the 15-texel pistol is 15 sixteenths of a block long, as the sheet's pistol. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /**
     * The muzzle in the gun frame (after the model's own quarter turn), blocks: x, y down, z. The barrel tip is 2
     * texels ahead of the model origin and its axis 1 texel below the top of the slide.
     */
    public static final float[] MUZZLE = { 0.0f, 1.0f / 16.0f, -2.0f / 16.0f };
    /**
     * Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Frame 7 is empty (the sheet's cell 7 is fully
     * transparent), so playing eight frames made the flash vanish for the last eighth of its window.
     */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /**
     * Part 0 is the frame group (rail, barrel stub, grip, guard, trigger); part 1 is the slide group (the slide and
     * both sights), which the firing transform moves further.
     */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("slide", 1, 10, 0, 14, 2, 3, -13f, 0f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rear_sight", 1, 48, 5, 1, 1, 1, -11.5f, -0.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("front_sight", 1, 44, 4, 1, 1, 1, -1.5f, -0.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("rail", 0, 10, 5, 14, 1, 3, -13f, 2f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel", 0, 48, 3, 6, 1, 1, -4f, 0.5f, -0.5f, new float[] { -4f, 1f, 0f }, 0f, 45f),
        new BoxSpec("grip", 0, 0, 0, 3, 7, 2, -11f, 3f, -1f, new float[] { -8f, 3f, 0f }, 12.5f),
        new BoxSpec("guard_post", 0, 44, 0, 1, 3, 1, -6f, 3f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("guard_bar", 0, 52, 0, 4, 1, 1, -9f, 5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 48, 0, 1, 2, 1, -8f, 2.6f, -0.5f, new float[] { -7.5f, 2.6f, 0f }, -23f),
        // END GENERATED BOXES
    };

    /**
     * Where the box centre of a Z-family handgun sits in the scaled gun frame, blocks: the place the previous pistol
     * was tuned to ({@code S(1.0)}, {@code T(-1.2, -1.0, -1.7)}), so the new model is framed the same way.
     */
    private static final double[] ANCHOR = { -1.2, -0.684, -1.4275 };
    /** First-person scale from {@link FirstPersonFit}: the model drawn 15/16 of a block long. */
    public static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), FirstPersonFit.HANDGUN_LENGTH);
    private static final double[] IDLE = FirstPersonFit.idleOffset(ANCHOR, FP_SCALE, gunFrameCentre());

    /** The sheet's render transforms, timing and muzzle flash for the pistol. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "pistol_9mm",
        BOXES,
        2,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        new String[] { TEXTURE },
        MUZZLE,
        false,
        TRANSFORMS);

    private Pistol9mmProfile() {}

    /** The box centre in the gun frame: the model's quarter turn about Y sends (x, y, z) to (z, y, -x). */
    private static double[] gunFrameCentre() {
        double[] c = FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE);
        return new double[] { c[2], c[1], -c[0] };
    }

    private static GunTransforms buildTransforms() {
        // First person (Z-family): rotX180, rotY225, S(FP_SCALE), then the state row.
        // Deviation: the notes say S(2.0) and T(-0.55, -0.45, -0.8). A 15/16 block long pistol at that scale ran off
        // the screen (M1 visual autotest), so the scale and idle position follow FirstPersonFit (S about 1.0, box
        // centre on the pistol's tuned anchor). The slide group travels 0.1 block behind the frame, which is the
        // notes' 1.6 texels, and the frame kicks back 0.15 block.
        TransformProgram fpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FP_SCALE)
            .build();
        TransformProgram fpIdle = TransformProgram.builder()
            .translate(IDLE[0], IDLE[1], IDLE[2])
            .build();
        // Firing: the gun pitches 5 degrees at the dip and pushes back 0.15; the slide (part 1) goes 0.1 further.
        TransformProgram fpFire = TransformProgram.builder()
            .rotX((p, part) -> 10.0 * yF(p))
            .translate(
                TransformProgram.c(IDLE[0]),
                TransformProgram.c(IDLE[1]),
                (p, part) -> IDLE[2] - yF(p) * (0.3 + 0.2 * part))
            .build();
        // Reload: tilt the muzzle up to 50 degrees and back.
        TransformProgram fpReload = TransformProgram.builder()
            .rotX((p, part) -> -50.0 * yR(p))
            .translate(IDLE[0], IDLE[1], IDLE[2])
            .build();
        // Third person: rotX180, rotY135, S(1.5), T(0.05, -0.55, 0.2). Aimed extra translate (0, 0, 0.2).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(135)
            .scale(1.5)
            .translate(0.05, -0.55, 0.2)
            .build();
        // Recoil about (0, 0, 0.9): rotX(-15 K); the slide also slides back 0.15 x 2p while p < 0.5.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0, 0.9, b -> b.rotX((p, part) -> -15.0 * kick(p)))
            .translate(
                TransformProgram.c(0),
                TransformProgram.c(0),
                (p, part) -> part == 1 && p < 0.5 ? 0.15 * 2.0 * p : 0.0)
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .about(0, 0.3, 0.9, b -> b.rotX((p, part) -> -40.0 * yR(p)))
            .build();
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .translate(0, -0.35, -0.3)
            .build();
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .scale(1.5)
            .rotY(90)
            .translate(0.3, -0.5, 0)
            .build();
        // Deviation: the sheet says "default blend" for the pistol flash, but the GregGuns sheet muzzleflash2.png is
        // an additive black-background sheet (docs/assets/MANIFEST.md), so it is drawn additively.
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.65,
            -0.75,
            1.55,
            1.0,
            new double[] { 0, 0, 0 },
            true,
            0.55);
        return GunTransforms.builder()
            .parts(2)
            .timing(3, 5, 35)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0, 0.2 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
