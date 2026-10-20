package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Blaster Rifle's model boxes and render transforms, as data. The gun is not registered yet: it is not in
 * {@link ClientGunDefinitions#ALL} until the gameplay batch adds it, together with its gun definition.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/blaster_rifle.md}: proportions, silhouettes, palettes),
 * by an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or
 * textures. The look: a near-black sci-fi energy rifle 50 texels long and about 20 tall. A 28 long receiver with a top
 * rail carries a short three-tube diamond scope; a 19 long diamond-section barrel guard (5.7 across) wears two crossed
 * fin blades whose tips poke 0.5 out of its flat faces and ends in a thin diamond muzzle plate with a black mark on
 * each end; a slim lighter rail bar and a slanted fairing run under it; a 7 across diamond cap sits behind the
 * receiver; a grip raked 25.6 degrees with a checkerboard, an open trigger guard and a small tilted trigger hang
 * under it. On the +Z side only sits the cell kit: a tall box beside the scope, a socket box, two diamond rods and a
 * flat slab that sticks about 8 out sideways. The layout lives in
 * {@code tools/asset-gen/gun-paint/blaster_rifle.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns the
 * UV origins and writes the generated block below, and {@code gun_paint.py paint} paints the 64x64 sheet from the role
 * map {@code gun-paint/blaster_rifle.json}. There are no camo variants and no glow parts (the gun is black end to
 * end).
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, as authored. Y = 0 is
 * the barrel axis, X = 0 a point 23 texels in front of the rear cap's back face (the rear cap is at X -23, the grip's
 * hinge at X -5, like the other rifles of this family); Z = 0 is the mid-plane, +Z the side that carries the cell kit.
 * One texel is a sixteenth of a block. The model has no moving parts (the whole gun moves as one part).
 *
 * <p>
 * Spec: rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class BlasterRifleProfile {

    /** Texture resource paths of the model sheet: only the default, there are no camo variants. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/blaster_rifle.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Scope overlay texture, drawn while zoomed. */
    public static final String SCOPE_TEXTURE = "gregguns:textures/fx/testscope.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the muzzle plate's front (texel 27) on the axis. */
    public static final float[] MUZZLE = { 27.0f / 16.0f, 0.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("rear_cap", 0, 14, 0, 2, 5, 5, -23f, -3f, -2.5f, new float[] { -23f, -0.5f, 0f }, 0f, 45f),
        new BoxSpec("receiver", 0, 0, 12, 28, 5, 4, -21f, -2.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("top_rail", 0, 0, 53, 27, 1, 2, -21f, -3.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("lower_recv", 0, 0, 48, 15, 2, 3, -9f, 2.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("guard", 0, 0, 21, 19, 4, 4, 7f, -2f, -2f, new float[] { 7f, 0f, 0f }, 0f, 45f),
        new BoxSpec("fin_a", 0, 0, 36, 16, 1, 5, 7f, -0.5f, -2.5f, new float[] { 7f, 0f, 0f }, 0f, 45f),
        new BoxSpec("fin_b", 0, 0, 42, 16, 1, 5, 7f, -0.5f, -2.5f, new float[] { 7f, 0f, 0f }, 0f, -45f),
        new BoxSpec("muzzle", 0, 50, 0, 1, 3, 3, 26f, -1.5f, -1.5f, new float[] { 26f, 0f, 0f }, 0f, 45f),
        new BoxSpec("rail_bar", 0, 28, 8, 15, 2, 2, 6f, 2f, -1f, NO_PIVOT, 0f),
        new BoxSpec("fairing", 0, 42, 35, 7, 2, 3, 18f, 2.8f, -1.5f, new float[] { 18f, 3.8f, 0f }, -13f),
        new BoxSpec("scope_rear", 0, 42, 29, 8, 3, 3, -15f, -7f, -1.5f, new float[] { -15f, -5.5f, 0f }, 0f, 45f),
        new BoxSpec("scope_mid", 0, 46, 21, 5, 3, 3, -7f, -6.5f, -1.5f, new float[] { -7f, -5f, 0f }, 0f, 45f),
        new BoxSpec("scope_front", 0, 50, 40, 2, 2, 2, -2f, -5.5f, -1f, new float[] { -2f, -4.5f, 0f }, 0f, 45f),
        new BoxSpec("side_box", 0, 28, 0, 8, 5, 3, -13f, -5.5f, 2f, NO_PIVOT, 0f),
        new BoxSpec("socket", 0, 0, 29, 7, 3, 4, -2f, -1.5f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("slab", 0, 22, 29, 5, 2, 5, -0.5f, -1f, 5.5f, NO_PIVOT, 0f),
        new BoxSpec("rod_a", 0, 14, 10, 6, 1, 1, -1f, -2.25f, 2.75f, new float[] { -1f, -1.75f, 3.25f }, 0f, 45f),
        new BoxSpec("rod_b", 0, 46, 27, 6, 1, 1, -1f, -2.25f, 4.25f, new float[] { -1f, -1.75f, 4.75f }, 0f, 45f),
        new BoxSpec("cap_plate", 0, 42, 40, 1, 1, 3, -2f, -2.5f, 2.5f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 0, 0, 4, 9, 3, -9f, 4.5f, -1.5f, new float[] { -5f, 4.5f, 0f }, 25.6f),
        new BoxSpec("guard_post", 0, 58, 0, 1, 3, 2, -2f, 4.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("guard_floor", 0, 42, 44, 5, 1, 2, -7f, 6.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 58, 5, 1, 2, 1, -5f, 4f, -0.5f, new float[] { -4.5f, 4f, 0f }, -15f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 1.4 and an idle offset measured from the origin of the original
    // model. The scale, idle position and third-person placement are solved by InHandFit from the per-gun targets of
    // the in-hand notes (section 7), so the gun lands where Techguns' sits on screen.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("blaster_rifle", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.1, -0.2 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing, scope and muzzle flash for the Blaster Rifle. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms; the gameplay batch lists it in
     * {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "blaster_rifle",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private BlasterRifleProfile() {}

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
        // Firing: a pitch kick of up to 5 degrees and a pushback along the gun axis (the notes: 0.15 block), both
        // peaking mid-animation.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> -5.0 * kick(p))
            .translate((p, part) -> FP_X + 0.15 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: the gun tilts 50 degrees at mid-reload and comes back (the notes: smooth rise and fall).
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -50.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225, then the scale and translate InHandFit solved so the box centre lands at the
        // hand and the gun is the notes' 1.11 world blocks long.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil: the gun pitches 15 degrees about the hand in the first quarter of the first half (the notes).
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -15.0 * kick(p)))
            .build();
        // Reload: about 30 degrees pitch plus 40 degrees yaw at mid-animation, then back (the notes).
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .build();
        // Ground: scale 0.75 like the other long guns, lying flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.75)
            .translate(0, -0.15, 0)
            .build();
        // Icon: side-on, scale 0.6 so the 3 block model fits the slot, centred in it (box centre at texel 2, 2.5).
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.6)
            .translate(-0.125, -0.155, 0)
            .build();
        // Deviation: the first-person flash is a fixed-frame screen effect, so its offset follows the gun's solved
        // placement (InHandFit.Placement.flashOffset). Headless work cannot look at the screen, so this is unverified
        // in game. muzzleFlash2 sheet, additive because the GregGuns sheet is black-backed (docs/assets/MANIFEST.md).
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
            .timing(4, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.1, -0.2 }, tpRecoil, tpReload)
            .world(ground, icon)
            .scope(new ScopeSpec(SCOPE_TEXTURE, 0.04, 0.02))
            .muzzleFlash(flash)
            .build();
    }
}
