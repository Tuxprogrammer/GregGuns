package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The PDW's model boxes and render transforms, as data. Listed in {@link ClientGunDefinitions#ALL}; the gun
 * definition is {@code PdwDef}.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/pdw.md}: proportions, silhouettes, palettes), by an agent
 * that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures. The look:
 * a compact P90-style bullpup 32 texels long and 18.8 tall, with a pale ice-blue receiver (dark lower band, three vent
 * slots and one red stripe) over dark slate underparts; a flat carry-handle rail floating above it on a rear pillar
 * and a front post with an open window beneath, a short black diamond muzzle, a stepped underbody, a pistol grip
 * raked 25 degrees with a thumbhole slit behind it, a diagonal floor bar rising to the rear, and a stepped butt block.
 * The layout lives in {@code tools/asset-gen/gun-paint/pdw.layout.json}; {@code tools/asset-gen/gun_paint.py pack}
 * assigns the UV origins and writes the generated block below, and {@code gun_paint.py paint} paints the 64x64 default
 * sheet and the two camo sheets (Silver, Dark Green) from the role map {@code gun-paint/pdw.json}.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, as authored. Y = 0 is
 * the top of the receiver and X = 0 a point 13 texels behind its front face (the butt plate is at X -15); Z = 0 is the
 * mid-plane. One texel is a sixteenth of a block. The model has no moving parts (the whole gun moves as one part).
 *
 * <p>
 * Spec: gun-sheets.md "PDW" (timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class PdwProfile {

    /** Texture resource paths of the model sheet: index 0 is the default, 1 is Silver and 2 is Dark Green. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/pdw.png",
        "gregguns:textures/models/guns/pdw_camo1.png", "gregguns:textures/models/guns/pdw_camo2.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the barrel tip (texel 17) on its axis (texel 1.5). */
    public static final float[] MUZZLE = { 17.0f / 16.0f, 1.5f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("receiver", 0, 0, 10, 25, 3, 4, -13f, 0f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cover", 0, 0, 22, 25, 1, 3, -13f, -1f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("hump", 0, 38, 6, 9, 1, 3, -13f, -2f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rail", 0, 0, 30, 13, 1, 2, -3.5f, -4f, -1f, NO_PIVOT, 0f),
        new BoxSpec("pillar", 0, 18, 17, 3, 2, 3, -4f, -3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("sight_ear_l", 0, 56, 32, 2, 2, 1, -3.9f, -4.5f, 0.4f, NO_PIVOT, 0f),
        new BoxSpec("sight_ear_r", 0, 54, 35, 2, 2, 1, -3.9f, -4.5f, -1.4f, NO_PIVOT, 0f),
        new BoxSpec("support", 0, 0, 33, 8, 1, 2, -1f, -2f, -1f, NO_PIVOT, 0f),
        new BoxSpec("front_support", 0, 56, 29, 2, 1, 2, 9f, -2f, -1f, NO_PIVOT, 0f),
        new BoxSpec("front_post", 0, 58, 10, 1, 2, 2, 9.5f, -4f, -1f, NO_PIVOT, 0f),
        new BoxSpec("sight_mid", 0, 28, 8, 1, 1, 0, 9.5f, -4.5f, 0f, NO_PIVOT, 0f),
        new BoxSpec("sight_horn_l", 0, 62, 6, 1, 1, 0, 9.5f, -5f, 0.5f, NO_PIVOT, 0f),
        new BoxSpec("sight_horn_r", 0, 62, 7, 1, 1, 0, 9.5f, -5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("muz_base", 0, 58, 14, 1, 2, 2, 12f, 0.5f, -1f, new float[] { 12f, 1.5f, 0f }, 0f, 45f),
        new BoxSpec("muz_ring", 0, 20, 8, 1, 1, 1, 13f, 1f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("muz_ring2", 0, 24, 8, 1, 1, 1, 13f, 1f, -0.5f, new float[] { 13f, 1.5f, 0f }, 0f, 45f, 0f, -0.01f),
        new BoxSpec("muz_tube", 0, 42, 26, 3, 2, 2, 14f, 0.5f, -1f, new float[] { 14f, 1.5f, 0f }, 0f, 45f),
        new BoxSpec("muz_ear_l", 0, 60, 0, 1, 2, 1, 12f, 0.5f, 1f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("muz_ear_r", 0, 60, 3, 1, 2, 1, 12f, 0.5f, -2f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("gas_block", 0, 16, 8, 1, 1, 1, 11.5f, 3.5f, -0.5f, new float[] { 11.5f, 4f, 0f }, 0f, 45f),
        new BoxSpec("ub_rear", 0, 40, 17, 2, 2, 3, -1.5f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("ub_mid", 0, 38, 0, 4, 3, 3, 0.5f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("ub_front", 0, 0, 17, 6, 2, 3, 4.5f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("belly", 0, 38, 33, 6, 1, 2, 4.5f, 5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("front_wall", 0, 52, 0, 1, 3, 3, 10.5f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("foot", 0, 58, 18, 1, 1, 2, 10.5f, 6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 0, 0, 4, 8, 2, -0.5f, 5f, -1f, new float[] { 3.5f, 5f, 0f }, 25f),
        new BoxSpec("strap", 0, 12, 0, 1, 8, 1, 3.5f, 5f, -0.5f, new float[] { 3.5f, 5f, 0f }, 25f),
        new BoxSpec(
            "floor_bar",
            0,
            30,
            30,
            11,
            1,
            2,
            -5.6f,
            12.8f,
            -1f,
            new float[] { 3.5f, 5f, 0f },
            25f,
            0f,
            0f,
            -0.02f),
        new BoxSpec("strut", 0, 28, 0, 3, 6, 2, -6.9f, 3.5f, -1f, new float[] { -5.4f, 3.5f, 0f }, 25f),
        new BoxSpec("strap_a", 0, 16, 26, 5, 1, 3, -6.5f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("strap_b", 0, 20, 33, 7, 1, 2, -13f, 3f, -1f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("brace", 0, 52, 26, 2, 1, 2, -8.7f, 3f, -1f, new float[] { -6.7f, 3f, 0f }, -50f, 0f, 0f, -0.02f),
        new BoxSpec("leg", 0, 0, 26, 6, 2, 2, -13.5f, 6f, -1f, new float[] { -13.5f, 6f, 0f }, 15f, 0f, 0f, -0.01f),
        new BoxSpec("heel_hook", 0, 50, 17, 2, 3, 2, -15f, 6.5f, -1f, new float[] { -15f, 6.5f, 0f }, -27.7f),
        new BoxSpec("butt_cap", 0, 32, 26, 2, 1, 3, -15f, -2.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("butt_main", 0, 16, 0, 2, 4, 4, -15f, -1.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("butt_low", 0, 30, 17, 2, 2, 3, -15f, 2.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("butt_heel", 0, 56, 22, 2, 2, 2, -15f, 4.5f, -1f, NO_PIVOT, 0f, 0f, 0f, -0.02f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 2.1 and an idle offset measured from the origin of the original
    // model. InHandFit solves the scale and the idle offset from the per-gun targets in InHandTargets (the in-hand
    // notes, section 7), so this model's length, centre and muzzle land where the original's do on screen.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("pdw", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.1, 0 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the PDW. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "pdw",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private PdwProfile() {}

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
        // Firing: a tiny kick (the notes: at most 2.5 degrees, 0.125 back) per burst round.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 5.0 * yF(p))
            .translate((p, part) -> FP_X + 0.25 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: the gun rolls 25 degrees at mid-reload and comes back, over the 40 ticks.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -25.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225 (the notes' Z-family yaw 225 turned to the X-family), scale 1.25 (the
        // notes). The pistol grip (about 0.1 block from the origin, 0.3 down) lands at the hand. Aimed extra
        // (0, 0.1, 0).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil: the gun pitches up 20 degrees about the hand, in the first quarter of the first half.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -20.0 * kick(p)))
            .build();
        // Reload: about 15 degrees pitch plus 20 degrees yaw at mid-animation, then back.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 15.0 * yR(p))
            .rotY((p, part) -> 20.0 * yR(p))
            .build();
        // Ground: scale 0.8 (the notes), the model centred (centre at texel 1, 4.4) so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.8)
            .translate(-0.0625, -0.275, 0)
            .build();
        // Icon: side-on, scale 0.75 (the notes), the model centred in the slot.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.75)
            .translate(-0.0625, -0.275, 0)
            .build();
        // Deviation: the first-person flash is a fixed-frame screen effect, solved from this model's muzzle in the
        // fitted
        // frame. Headless work cannot look at the screen, so this is unverified in game. muzzleFlash2 sheet, additive
        // because the GregGuns sheet is black-backed (docs/assets/MANIFEST.md).
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
            0.7);
        return GunTransforms.builder()
            .parts(1)
            .timing(5, 5, 40)
            .checkRecoil()
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.1, 0 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
