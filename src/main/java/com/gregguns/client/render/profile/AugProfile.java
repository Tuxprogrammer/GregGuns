package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The AUG's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/aug.md}: proportions, silhouettes, palettes), by an agent
 * that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures. The look:
 * a dark-green bullpup rifle 70 texels long and about 25 tall: a 44-long diamond-section receiver running back to a
 * tall butt plate over a deepening, sloped rear body, a long thin black barrel (23 plus a 3-long muzzle cap) with a
 * short gas tube on one side, a black two-stage scope on two forward-leaning green struts that leave a skewed open
 * window, a vertical diamond-section foregrip post in a lighter green, a raked pistol grip with a closed trigger-guard
 * loop and a gray magazine in two segments behind the grip, bowing forward. The layout lives in
 * {@code tools/asset-gen/gun-paint/aug.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns the UV origins
 * and writes the generated block below, and {@code gun_paint.py paint} paints the 128x64 default sheet and the four
 * camo sheets (Gray, Wood, Sand, Nether) from the role map {@code gun-paint/aug.json}. {@code AugProfileTest} checks
 * the boxes against the layout file and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the muzzle is toward +X and Y grows downward, in texels. X = 0 is the back of the butt plate and Y = 0 the
 * top of the scope's rear housing; Z = 0 is the mid-plane. One texel is a sixteenth of a block. Part 1 is the charging
 * handle (on the +Z side, with the gas tube on -Z), which slides back on a shot; everything else is part 0. Square
 * bars shown as diamonds use a 45 degree turn about their long axis ({@code tiltX}); the foregrip post uses one about
 * the vertical ({@code tiltY}).
 *
 * <h2>Scope</h2>
 * The scope overlay is the shared test scope texture; aiming also zooms to x0.5 (the gun definition).
 *
 * <p>
 * Spec: gun-sheets.md "AUG" (timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class AugProfile {

    /** Texture resource paths of the model sheet: the default and the four camos (Gray, Wood, Sand, Nether). */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/aug.png",
        "gregguns:textures/models/guns/aug_camo1.png", "gregguns:textures/models/guns/aug_camo2.png",
        "gregguns:textures/models/guns/aug_camo3.png", "gregguns:textures/models/guns/aug_camo4.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the barrel's tip (texel 70) on its axis (Y 8.4). */
    public static final float[] MUZZLE = { 70.0f / 16.0f, 8.4f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    /** The charging handle's travel, model blocks (9.6 texels, the notes). */
    public static final double HANDLE_TRAVEL = 9.6 / 16.0;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** Two parts: 0 is the rifle, 1 is the charging handle. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("receiver", 0, 16, 12, 44, 4, 4, 0f, 7.3f, -2f, new float[] { 22f, 9.3f, 0f }, 0f, 45f),
        new BoxSpec("lower_plate", 0, 0, 26, 18, 2, 3, 24f, 10.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel", 0, 42, 26, 23, 2, 2, 44f, 7.4f, -1f, new float[] { 55.5f, 8.4f, 0f }, 0f, 45f),
        new BoxSpec("muzzle_cap", 0, 118, 0, 3, 2, 2, 67f, 7.4f, -1f, new float[] { 68.5f, 8.4f, 0f }, 0f, 45f),
        new BoxSpec("gas_tube", 0, 50, 10, 6, 1, 1, 44f, 9f, -2f, new float[] { 47f, 9.5f, -1.5f }, 0f, 45f),
        new BoxSpec("scope_rear", 0, 26, 20, 10, 3, 3, 18f, 0.62f, -1.5f, new float[] { 23f, 2.12f, 0f }, 0f, 45f),
        new BoxSpec("scope_front", 0, 94, 20, 9, 2, 2, 28f, 0.9f, -1f, new float[] { 32.5f, 1.9f, 0f }, 0f, 45f),
        new BoxSpec("strut_front", 0, 80, 0, 1, 8, 2, 35.1f, 1.8f, -1f, new float[] { 35.6f, 1.8f, 0f }, -35f),
        new BoxSpec("strut_rear", 0, 86, 0, 1, 7, 2, 26f, 1.7f, -1f, new float[] { 26.5f, 1.7f, 0f }, -25f),
        new BoxSpec("charge", 1, 86, 9, 2, 2, 1, 40f, 6.55f, 2.05f, new float[] { 41f, 7.55f, 2.55f }, 0f, 45f),
        new BoxSpec("foregrip", 0, 30, 0, 3, 9, 3, 41.5f, 11.5f, -1.4f, new float[] { 43f, 16f, 0.1f }, 0f, 0f, 45f),
        new BoxSpec(
            "grip",
            0,
            16,
            0,
            4,
            9,
            3,
            25.4f,
            10.8f,
            -1.5f,
            new float[] { 25.4f, 10.8f, 0f },
            25.6f,
            0f,
            0f,
            -0.01f),
        new BoxSpec(
            "guard_front",
            0,
            42,
            0,
            1,
            9,
            3,
            35.1f,
            12.3f,
            -1.5f,
            new float[] { 35.6f, 12.3f, 0f },
            34f,
            0f,
            0f,
            -0.01f),
        new BoxSpec("guard_floor", 0, 92, 8, 6, 1, 3, 25f, 19.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 118, 4, 2, 3, 1, 28f, 12f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("slope", 0, 74, 20, 7, 3, 3, 17f, 9.5f, -1.5f, new float[] { 24f, 9.5f, 0f }, -18f, 0f, 0f, -0.03f),
        new BoxSpec("cheek", 0, 112, 14, 4, 2, 3, 17f, 9.5f, -1.5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("rear_block", 0, 92, 0, 10, 5, 3, 8f, 9.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("ramp", 0, 0, 20, 10, 3, 3, 1f, 12.8f, -1.5f, new float[] { 11f, 12.8f, 0f }, -26f, 0f, 0f, -0.01f),
        new BoxSpec("neck", 0, 66, 0, 4, 7, 3, 4f, 9.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("butt", 0, 0, 0, 4, 12, 4, 0f, 9.5f, -2f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec(
            "socket",
            0,
            52,
            20,
            8,
            3,
            3,
            10f,
            12.5f,
            -1.5f,
            new float[] { 18f, 12.5f, 0f },
            -12f,
            0f,
            0f,
            -0.02f),
        new BoxSpec("mag_upper", 0, 50, 0, 6, 5, 2, 12.2f, 16.1f, -1f, new float[] { 15.2f, 18.6f, 0f }, -12f),
        new BoxSpec(
            "mag_lower",
            0,
            112,
            8,
            6,
            4,
            2,
            12.4f,
            19.5f,
            -1f,
            new float[] { 18.4f, 19.5f, 0f },
            -21f,
            0f,
            0f,
            -0.01f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 1.4 and an idle offset measured from the origin of Techguns' model.
    // The scale follows the FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's
    // precedent), and the idle position puts this model's box centre on the family anchor. The notes' small fire kick
    // (5 degrees, 0.15 back) is applied at that scale: a screen shift d is d / FP_SCALE in model axes.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("aug", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.1, 0 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the rifle. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "aug",
        BOXES,
        2,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private AugProfile() {}

    /**
     * How far the charging handle is back, as a fraction of its full travel: it reaches full travel in the first 15
     * percent of the shot animation, then eases forward linearly over the remaining 85 percent.
     *
     * @param p animation progress 0 to 1
     * @return 0 at rest, 1 at full travel
     */
    public static double handleSlide(double p) {
        if (p <= 0.0 || p >= 1.0) {
            return 0.0;
        }
        return p < 0.15 ? p / 0.15 : (1.0 - p) / 0.85;
    }

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
        // Firing: a light muzzle-up kick, 5 degrees at the dip (yF = -0.5), and 0.15 block back on screen.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 10.0 * yF(p))
            .translate(
                (p, part) -> FP_X + 0.42 / FP_SCALE * yF(p) - (part == 1 ? HANDLE_TRAVEL * handleSlide(p) : 0.0),
                (p, part) -> FP_Y,
                (p, part) -> FP_Z)
            .build();
        // Reload: the barrel dips 25 degrees at mid-reload and comes back, over the 45 ticks.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -25.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225 (the notes' Z-family yaw 135, turned to the X-family), scaled for the
        // 4.3-block model (the bolt-action's 1.2 for its 3.3 blocks). Aimed extra translate (0, 0.1, 0).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil: the gun pitches up 15 degrees about the hand, in the first quarter of the first half; the charging
        // handle slides back with the same travel as in first person.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -15.0 * kick(p)))
            .translate(
                (p, part) -> part == 1 ? -HANDLE_TRAVEL * handleSlide(p) : 0.0,
                TransformProgram.c(0),
                TransformProgram.c(0))
            .build();
        // Reload: about 30 degrees pitch plus 40 degrees yaw at mid-animation, then back.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .build();
        // Ground: scale 0.6 (the notes), the model centred (centre at texel 35, 12.7) so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.6)
            .translate(-2.1875, -0.794, 0)
            .build();
        // Icon: side-on, scale 0.48 (the notes), the model centred in the slot.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.48)
            .translate(-2.1875, -0.794, 0)
            .build();
        // Deviation: the first-person flash is a fixed-frame screen effect. Its offset is the bolt-action's solved
        // offset moved by the difference between the two muzzles' projected positions (this model is longer and
        // taller, so FP_SCALE differs and the muzzle sits elsewhere). muzzleFlash2 sheet, additive because the
        // GregGuns sheet is black-backed (docs/assets/MANIFEST.md).
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
            .parts(2)
            .timing(5, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.1, 0 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .scope(new ScopeSpec(As50Profile.SCOPE_TEXTURE, 0.04, 0.02))
            .build();
    }
}
