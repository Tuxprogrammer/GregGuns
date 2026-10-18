package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Bolt Action Rifle's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built the way {@link RevolverProfile} was: nothing is taken from Techguns
 * code or box lists. The model texture {@code textures/models/guns/bolt_action_rifle.png} (128x32, with two camo
 * variants of the same layout) is a packed atlas painted by {@code tools/asset-gen/models.py}, and
 * {@code tools/asset-gen/model_layouts.md} lists the six boxes it was painted for, each as {@code u v w h d} in texels
 * (receiver, barrel, stock, scope, magazine, bipod). Those sizes and texture origins are used here exactly, so every
 * unwrap rectangle lands on painted pixels (checked by {@code BoltActionRifleProfileTest}, which reads the PNG). The
 * positions are authored from the sizes and the words of the spec: a scoped hunting rifle with a long barrel in front
 * of a receiver, a wooden stock behind it, a scope tube riding on top and a magazine hanging below (gun-models.md,
 * "Bolt-action", in words only).
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as authored
 * (the sheet's X-family rows, first person rotX 180 / rotY -45 and third person rotY 225, expect a muzzle along +X).
 * One texel is a sixteenth of a block, so the rifle is about five blocks long in model space before the rows scale it.
 *
 * <h2>Scope</h2>
 * The rifle is scoped: while zoomed the renderer hides the model and draws the overlay {@link #SCOPE_TEXTURE}
 * (rendering-framework.md section 9.2). The kick values are fractions of the screen height; the sheet's default scope
 * recoil (0.2, 0.25) is in Techguns' own screen units and would throw the overlay across the screen, so GregGuns uses
 * a smaller shove.
 *
 * <p>
 * Spec: gun-sheets.md "Bolt-action Rifle" (rows and timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class BoltActionRifleProfile {

    /** Texture resource paths of the model sheet: the default and the two camo variants (Olive, Tactical Black). */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/bolt_action_rifle.png",
        "gregguns:textures/models/guns/bolt_action_rifle_camo1.png",
        "gregguns:textures/models/guns/bolt_action_rifle_camo2.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Scope overlay texture. */
    public static final String SCOPE_TEXTURE = "gregguns:textures/fx/testscope.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 32;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z. The barrel's front face is at texel 46. */
    public static final float[] MUZZLE = { 46.0f / 16.0f, -1.5f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    // Deviation: the sheet says first-person S(1.5) and T(-0.09, -0.37, 0.07) / idle T(0.1, -0.25, -0.5). It was
    // recorded for Techguns' 53 texel rifle; this one is 82 texels long and the sheet scale would fill the screen
    // (the pistol needed half the sheet's scale for the same reason). Scale 0.45 and the offsets below put the rifle
    // low on the right with the muzzle forward, tuned against screenshots (m2boltaction visual autotest). The sheet's
    // fire and reload offsets are multiplied by FP_AMP so they move the rifle the same distance on screen.
    private static final double FP_SCALE = 0.45;
    private static final double FP_AMP = 1.5 / FP_SCALE;
    private static final double FP_X = 1.0;
    private static final double FP_Y = -1.0;
    private static final double FP_Z = -1.5;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole rifle moves as a unit (the bolt is static geometry, as in the spec). */
    public static final BoxSpec[] BOXES = {
        // receiver: the action, in front of the stock
        new BoxSpec("receiver", 0, 56, 0, 24, 7, 5, -14f, -4f, -2.5f, NO_PIVOT, 0f),
        // barrel: out of the front of the receiver, upper half
        new BoxSpec("barrel", 0, 0, 25, 38, 3, 3, 8f, -3f, -1.5f, NO_PIVOT, 0f),
        // stock: behind the receiver, wrist flush with its top
        new BoxSpec("stock", 0, 0, 0, 24, 9, 4, -36f, -1f, -2f, NO_PIVOT, 0f),
        // scope: a tube resting on the top of the receiver
        new BoxSpec("scope", 0, 38, 13, 24, 4, 4, -6f, -8f, -2f, NO_PIVOT, 0f),
        // magazine: hangs under the front of the receiver
        new BoxSpec("magazine", 0, 0, 13, 5, 9, 3, -2f, 3f, -1.5f, NO_PIVOT, 0f),
        // bipod: a folded leg plate under the fore-end
        new BoxSpec("bipod", 0, 16, 13, 9, 9, 2, 24f, 0f, -1f, NO_PIVOT, 0f), };

    /** The sheet's render transforms, timing, scope and muzzle flash for the rifle. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    private BoltActionRifleProfile() {}

    private static GunTransforms buildTransforms() {
        // First person (X-family): rotX180, rotY-45, S(FP_SCALE), then the state row.
        TransformProgram fpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(-45)
            .scale(FP_SCALE)
            .build();
        TransformProgram fpIdle = TransformProgram.builder()
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Firing: p < 0.25 the kick, y = yF(4p): rotZ(25 y), the rifle jumps back. p >= 0.25 the rechamber, s =
        // sin((p - 0.25) 1.333): the rifle cants and pulls in as if the bolt is worked.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> p < 0.25 ? 25.0 * yF(4.0 * p) : 4.0 * rechamber(p))
            .rotY((p, part) -> p < 0.25 ? 0.0 : -16.0 * rechamber(p))
            .rotX((p, part) -> p < 0.25 ? 0.0 : -4.0 * rechamber(p))
            .translate(
                (p, part) -> FP_X + FP_AMP * (p < 0.25 ? yF(4.0 * p) : 0.25 * rechamber(p)),
                (p, part) -> FP_Y + (p < 0.25 ? 0.0 : FP_AMP * 0.05 * rechamber(p)),
                (p, part) -> FP_Z + (p < 0.25 ? 0.0 : FP_AMP * 0.05 * rechamber(p)))
            .build();
        // Reload: y = yR; the rifle tips away and down while the player loads it.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -10.0 * yR(p))
            .rotY((p, part) -> 50.0 * yR(p))
            .rotX((p, part) -> 10.0 * yR(p))
            .translate(
                (p, part) -> FP_X - FP_AMP * yR(p),
                (p, part) -> FP_Y - FP_AMP * 0.5 * yR(p),
                (p, part) -> FP_Z - FP_AMP * 0.25 * yR(p))
            .build();
        // Third person: rotX180, rotY225, S(0.85), T(-1.0, -0.4, 0). Aimed extra translate (0, 0.2, -0.25).
        // Deviation: the sheet's S(1.5) and T(-0.65, -0.25, 0) are for Techguns' smaller model.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(0.85)
            .translate(-1.0, -0.4, 0)
            .build();
        // Recoil about (0, 0.3, -0.9): rotZ(-20 K).
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -20.0 * kick(p)))
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .build();
        // Ground: rotX180, rotY90, S(0.55), T(0, -0.25, 0). Icon: the sheet's rows with the scale cut to fit the slot
        // and the offset set to centre this model (centre at texel 5, 2).
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.55)
            .translate(0, -0.25, 0)
            .build();
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.4)
            .translate(-0.31, -0.125, 0)
            .build();
        // muzzleFlash2 sheet, additive because the GregGuns sheet is black-backed (docs/assets/MANIFEST.md).
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.4,
            -3.0,
            1.15,
            1.85,
            new double[] { 0, 0, 0 },
            true,
            0.7);
        return GunTransforms.builder()
            .parts(1)
            .timing(15, 5, 50)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.2, -0.25 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .scope(new ScopeSpec(SCOPE_TEXTURE, 0.04, 0.02))
            .build();
    }

    /** The rechamber swing: 0 at p = 0.25, a half sine to 0 again at p = 1. */
    private static double rechamber(double p) {
        return Math.sin((p - 0.25) * (Math.PI / 0.75));
    }
}
