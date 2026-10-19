package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.p66;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Thompson SMG's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/thompson.md}: proportions, silhouettes, palettes), by an
 * agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures. The
 * look: a 1920s tommy gun in chunky boxes, 52 texels long and 14 tall, banded brown-gray-brown along its length: a
 * stepped wooden stock, a flat gray receiver with a darker base ledge, a wooden slotted forend over a 2x2 diamond
 * barrel that leaves a 4 texel bare stub, a tall gray slab under the receiver, a wooden pistol grip tilted 25 degrees
 * back, and two small sights. The layout lives in {@code tools/asset-gen/gun-paint/thompson_smg.layout.json};
 * {@code tools/asset-gen/gun_paint.py pack} assigns the UV origins and writes the generated block below, and
 * {@code gun_paint.py paint} paints the 64x64 sheet from the role map {@code gun-paint/thompson_smg.json}.
 * {@code ThompsonProfileTest} checks the boxes against the layout file and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as authored.
 * X = 0 is the back of the butt plate and Y = 0 the top of the receiver (so the two sights sit at negative Y). One
 * texel is a sixteenth of a block. The slab, grip and sights are static: all motion is whole-gun transforms.
 *
 * <p>
 * Spec: gun-sheets.md "Thompson" (rows and timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class ThompsonProfile {

    /** Texture resource path of the model sheet (the Thompson has a single skin). */
    public static final String TEXTURE = "gregguns:textures/models/guns/thompson_smg.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the barrel's front face (texel 52) on its axis. */
    public static final float[] MUZZLE = { 52.0f / 16.0f, 1.5f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("receiver", 0, 10, 9, 22, 3, 3, 12f, 0f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel", 0, 0, 30, 18, 2, 2, 34f, 0.5f, -1f, new float[] { 34f, 1.5f, 0f }, 0f, 45f),
        new BoxSpec("front_sight", 0, 30, 6, 1, 2, 1, 50.5f, -1f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("rear_leaf_a", 0, 48, 6, 2, 1, 0, 13f, -1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rear_leaf_b", 0, 52, 6, 2, 1, 0, 13f, -1f, 1f, NO_PIVOT, 0f),
        new BoxSpec("rear_cross", 0, 34, 6, 0, 1, 2, 14f, -1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("ledge", 0, 26, 15, 15, 1, 4, 12f, 3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("front_plate", 0, 0, 26, 4, 1, 3, 30f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("forend", 0, 30, 0, 13, 3, 3, 35f, 1.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("slab", 0, 0, 0, 3, 10, 2, 27f, 3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rail", 0, 34, 25, 7, 1, 3, 20f, 4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("grip_top", 0, 14, 21, 3, 2, 3, 20f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("guard_post", 0, 26, 25, 1, 2, 3, 26f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("guard_floor", 0, 38, 6, 3, 0, 2, 23f, 7f, -1f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 20, 0, 3, 6, 2, 20f, 6.5f, -1f, new float[] { 23f, 6.5f, 0f }, 25f),
        new BoxSpec("butt", 0, 10, 0, 2, 6, 3, 0f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_body", 0, 0, 15, 10, 3, 3, 2f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("comb", 0, 44, 20, 6, 2, 3, 2f, 8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("foot", 0, 54, 25, 2, 1, 3, 8f, 8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("bridge_a", 0, 14, 26, 3, 1, 3, 9f, 4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("bridge_b", 0, 26, 20, 5, 1, 4, 12f, 4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("bridge_c", 0, 0, 21, 4, 2, 3, 12f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("notch_a", 0, 40, 29, 1, 1, 3, 16f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("notch_b", 0, 48, 29, 1, 1, 3, 17f, 5f, -1.5f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the sheet says first-person S(1.8) and idle T(0.1, -0.35, -0.5), recorded for Techguns' model. The
    // scale follows the FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action rifle's
    // precedent), and the idle position puts this model's box centre on the family anchor. The sheet's fire and
    // reload offsets are multiplied by K so they move the gun the same distance on screen as the sheet intends.
    private static final double SHEET_FP_SCALE = 1.8;
    private static final InHandFit.Placement FIT = InHandFit
        .fit("thompson_smg", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0, 0 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double K = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    private static final double[] IDLE = FIT.fpIdle;

    /** The sheet's render transforms, timing and muzzle flash for the Thompson. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "thompson_smg",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        new String[] { TEXTURE },
        MUZZLE,
        true,
        TRANSFORMS);

    private ThompsonProfile() {}

    private static GunTransforms buildTransforms() {
        // First person (X-family): rotX180, rotY-45, S(FP_SCALE), then the state row.
        double x0 = IDLE[0];
        double y0 = IDLE[1];
        double z0 = IDLE[2];
        TransformProgram fpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(-45)
            .scale(FP_SCALE)
            .build();
        TransformProgram fpIdle = TransformProgram.builder()
            .translate(x0, y0, z0)
            .build();
        // Firing: rotZ(10 yF), the gun rocks up and slides back 0.5 (sheet) each shot.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 10.0 * yF(p))
            .translate((p, part) -> x0 + K * 0.5 * yF(p), TransformProgram.c(y0), TransformProgram.c(z0))
            .build();
        // Reload: s = P66(p); the gun tips up 25 degrees, turns 15 away and slides aside, then returns.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> 25.0 * p66(p))
            .rotY((p, part) -> -15.0 * p66(p))
            .translate(
                (p, part) -> x0 + K * 0.5 * p66(p),
                (p, part) -> y0 + K * 0.25 * p66(p),
                (p, part) -> z0 + K * 0.25 * p66(p))
            .build();
        // Third person: rotX180, rotY225, S(1.15), T(-0.85, -0.6, 0). The sheet has no aimed offset.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil about (0, 0.3, -0.9): rotZ(-20 K); reload rotX(30 yR), rotY(40 yR).
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -20.0 * kick(p)))
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .build();
        // Ground: rotX180, rotY90, S(0.8), T(0, -0.45, 0), with the 3.25 block model centred along its length.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.8)
            .translate(-1.625, -0.45, 0)
            .build();
        // Icon: the sheet's rotX180, rotY180; the scale is cut to fit the slot and the offset centres this model
        // (centre at texel 26, 6), as for the bolt-action rifle.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.6)
            .translate(-1.625, -0.375, 0)
            .build();
        // Deviation: the sheet's flash offset (-0.5, -2.0, 1.0) is where Techguns' muzzle was on screen; this offset
        // puts the fixed-frame first-person flash on this model's muzzle (the bolt-action rifle's solved offset,
        // shifted for this barrel's 0.117 block higher axis). muzzleFlash2 sheet, additive: it is black-backed.
        double[] fo = FIT.flashOffset(MUZZLE, 1.5);
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            fo[0],
            fo[1],
            fo[2],
            1.5,
            new double[] { 0, 0, 0 },
            true,
            0.5);
        return GunTransforms.builder()
            .parts(1)
            .timing(5, 5, 40)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0, 0 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
