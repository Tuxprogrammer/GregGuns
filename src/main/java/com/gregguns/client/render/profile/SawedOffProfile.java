package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.sin;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Sawed Off's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built the way {@link RevolverProfile} was: nothing is taken from
 * Techguns code or box lists. The model texture {@code textures/models/guns/sawed_off.png} (64x32) is a packed atlas
 * painted by {@code tools/asset-gen/models.py}, and {@code tools/asset-gen/model_layouts.md} lists the six boxes it was
 * painted for, each as {@code u v w h d} in texels (receiver, barrel, pump, stock, grip, tube). Those sizes and
 * texture origins are used here exactly, so every unwrap rectangle lands on painted pixels (checked by
 * {@code SawedOffProfileTest}, which reads the PNG). The positions are authored from the sizes and the one fact the
 * spec records about the shape: a side-by-side double barrel cut down to a pistol grip (gun-models.md, "Sawed Off", in
 * words only). The "barrel" box is the pair of barrels as one block four texels wide, the "tube" a rib along its top,
 * and the "pump" the forend hung under the barrels.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as authored.
 * One texel is a sixteenth of a block.
 *
 * <p>
 * Spec: gun-sheets.md "Sawed-off" (rows and timing); rendering-framework.md section 6.1 (X-family) and section 8.
 */
public final class SawedOffProfile {

    /** Texture resource path of the model sheet. */
    public static final String TEXTURE = "gregguns:textures/models/guns/sawed_off.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 32;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /**
     * The muzzle in the gun frame, blocks: x forward, y down, z. The barrels' front face is 37 texels forward of the
     * model origin and the bore axis sits 2.5 texels above it.
     */
    public static final float[] MUZZLE = { 37.0f / 16.0f, -2.5f / 16.0f, 0.0f };
    /**
     * Frames of {@code muzzleflash4x4.png} that are painted: 0 to 8 (see {@link RevolverProfile#MUZZLE_FLASH_FRAMES}).
     */
    public static final int MUZZLE_FLASH_FRAMES = 9;

    private static final double FP_SCALE = 0.9;
    private static final double FP_X = 0.33;
    private static final double FP_Y = -0.83;
    private static final double FP_Z = -0.75;

    private static final float[] GRIP_PIVOT = { 3.0f, 2.0f, 0.0f };
    private static final float[] NO_PIVOT = { 0, 0, 0 };
    private static final float GRIP_TILT = 12.0f;

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // receiver: the breech block behind the barrels
        new BoxSpec("receiver", 0, 0, 0, 14, 6, 4, 0f, -4f, -2f, NO_PIVOT, 0f),
        // barrel: both tubes as one block, top flush with the receiver
        new BoxSpec("barrel", 0, 0, 19, 23, 3, 4, 14f, -4f, -2f, NO_PIVOT, 0f),
        // pump: the forend, under the barrels
        new BoxSpec("pump", 0, 34, 10, 11, 4, 4, 15f, -1f, -2f, NO_PIVOT, 0f),
        // stock: the short butt behind the receiver
        new BoxSpec("stock", 0, 0, 10, 14, 6, 3, -14f, -3f, -1.5f, NO_PIVOT, 0f),
        // grip: hangs from the rear of the receiver, tilted about its top edge
        new BoxSpec("grip", 0, 36, 0, 4, 7, 3, 1f, 2f, -1.5f, GRIP_PIVOT, GRIP_TILT),
        // tube: the rib along the top of the barrels
        new BoxSpec("tube", 0, 0, 26, 20, 2, 2, 16f, -6f, -1f, NO_PIVOT, 0f), };

    /** The sheet's render transforms, timing and muzzle flash for the Sawed Off. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "sawed_off",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        new String[] { TEXTURE },
        MUZZLE,
        true,
        TRANSFORMS);

    private SawedOffProfile() {}

    private static GunTransforms buildTransforms() {
        // First person (X-family): rotX180, rotY-45, S(FP_SCALE), then the state row.
        // Deviation: the sheet says S(2.2) with idle T(0.1, -0.1, -0.5), recorded for Techguns' much smaller model.
        // The GregGuns model is about 3 blocks long (51 texels), so the scale is 0.9 and the offset is
        // (0.33, -0.83, -0.75): the Revolver's world shift (its 1.5 scale times (0.2, -0.5, -0.45)) divided by 0.9.
        // Tuned against client screenshots (mp.visual.sawedoff): the muzzle sits left of centre, the grip in the
        // lower right, and the fixed-frame muzzle flash lands on the barrels. The first try, S(0.6) with
        // (-0.3, -0.5, -0.9), left only a corner of the gun on screen.
        TransformProgram fpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(-45)
            .scale(FP_SCALE)
            .build();
        TransformProgram fpIdle = TransformProgram.builder()
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Firing: rotZ(30 yF), the gun jumping up and back.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 30.0 * yF(p))
            .translate((p, part) -> FP_X + 0.5 * yF(p), (p, part) -> FP_Y + 0.3 * yF(p), TransformProgram.c(FP_Z))
            .build();
        // Reload: s = sin(p); the gun breaks open: tips up 70 degrees and swings out.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> 70.0 * sin(p))
            .translate((p, part) -> FP_X + sin(p), (p, part) -> FP_Y - sin(p) / 2.0, TransformProgram.c(FP_Z))
            .build();
        // Third person: rotX180, rotY225, S(1.5), T(-0.5, -0.25, 0). Aimed extra translate (0, 0.15, 0).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(1.5)
            .translate(-0.5, -0.25, 0)
            .build();
        // Recoil about (-0.2, -0.1, 0): rotZ(-40 K).
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(-0.2, -0.1, 0, b -> b.rotZ((p, part) -> -40.0 * kick(p)))
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .about(-0.2, -0.1, 0, b -> b.rotZ((p, part) -> -40.0 * yR(p)))
            .build();
        // Deviation: the sheet has no scale for the ground row; the 3 block long model is drawn at 0.6 so a dropped
        // Sawed Off is about as long as a block and a half.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.6)
            .build();
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.7)
            .build();
        // muzzleFlash sheet, (-0.55, -2.15, 1.05), scale 1.85; additive because the GregGuns sheet is black-backed.
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash4x4.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.55,
            -2.15,
            1.05,
            1.85,
            new double[] { 0, 0, 0 },
            true,
            0.1);
        return GunTransforms.builder()
            .parts(1)
            .timing(5, 5, 28)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.15, 0 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
