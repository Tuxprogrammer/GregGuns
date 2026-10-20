package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

/**
 * The Nuclear Death Ray's model boxes and render transforms, as data. Registered in
 * {@link ClientGunDefinitions#ALL} with the gun definition.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/nuclear_death_ray.md}), by an agent that never saw the
 * original model or texture. Nothing is taken from Techguns code, box lists or textures. The look: a bulky sci-fi
 * radiation cannon 43 texels long. A skeletal 1 thick shoulder bracket (red bar, hook and neck, a zero-thickness web, a
 * dark end block), a hex tube of two 3 long rings (six slabs each) around a narrower 8 long sleeve whose three wide
 * flats carry dark windows with a yellow and black checker, a three-tier rail with a red fin tilted 45 degrees, a
 * pale reactor with a plus-shaped cross-section and two capped ends, a pale pistol grip and red trigger shifted two
 * texels to the gun's right, three prongs at 12, 4 and 8 o'clock that bend 45 degrees inward into a dark hex core and
 * a ragged-rimmed hex cup, and a red diamond-section mast on the lower-left prong. The layout lives in
 * {@code tools/asset-gen/gun-paint/nuclear_death_ray.layout.json}; {@code gun_paint.py pack} writes the block below
 * and {@code gun_paint.py paint} paints the 128x64 sheet from {@code gun-paint/nuclear_death_ray.json}. No camo
 * variants.
 *
 * <h2>Parts</h2>
 * Part 0 is the body, drawn in the gun's own matrix. Part 1 is the firing starburst: one 7x5x1 blade named
 * {@code starburst_glow_0}, rolled 25 degrees about the barrel axis (round-3 measurement: the original has exactly one
 * such plate in the reactor region), painted faint translucent yellow (alpha 28) and hidden by
 * {@link #buildTransforms()}'s part rule until the gun fires, then pulsed by {@link #starburstScaleAt(double)} on a
 * render clock. Part 2 is
 * the fin and part 3 is the mast
 * with its connector cube, so later code can animate them; both rest at the notes' pose.
 *
 * <h2>Glow parts</h2>
 * Deviation: the profile has no per-box lighting flag yet, so the glow is painted into the sheet. The boxes named
 * {@code *_glow*} are the ones a renderer may draw without lighting: {@code core_glow} (the hidden 7x4x4 core) and the
 * starburst blade. The optional side windows were dropped in round 3 (the original has no such boxes).
 *
 * <h2>Frame of reference</h2>
 * X-family: muzzle toward +X, Y growing downward, in texels. X = 0 is the rear tip of the shoulder bracket, Y = 0 the
 * barrel axis, Z = 0 the mid-plane, -Z the gun's right (grip), +Z its left (mast). One texel is a sixteenth of a
 * block. Tilted boxes: the hex slabs, prongs and starburst blades turn about the barrel axis ({@code tiltX}); the
 * claw tips and the mast turn about two axes at once (the three tilts are applied in the model renderer's Z, Y, X
 * order, so the pair is an Euler decomposition); the fin is tilted 45 degrees and the trigger 23 degrees about Z.
 * Deviation: the sheet is 128x64 (the original's 64x64 cannot hold 77 boxes).
 *
 * <p>
 * Spec: rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class NuclearDeathRayProfile {

    /** Texture resource paths of the model sheet: only the default, there are no camo variants. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/nuclear_death_ray.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** Part index of the body. */
    public static final int PART_BODY = 0;
    /** Part index of the firing starburst. */
    public static final int PART_STARBURST = 1;
    /** Part index of the fin. */
    public static final int PART_FIN = 2;
    /** Part index of the mast and its connector cube. */
    public static final int PART_MAST = 3;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the cup's rim (texel 43) on the barrel axis. */
    public static final float[] MUZZLE = { 43.0f / 16.0f, 0.0f, 0.0f };

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** All boxes, in four parts (body, starburst, fin, mast). */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("bracket_bar", 0, 28, 53, 6, 2, 1, 0f, -2f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("bracket_hook", 0, 124, 0, 1, 4, 1, 0f, 0f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("bracket_web", 0, 72, 50, 5, 4, 0, 1f, 0f, 0f, NO_PIVOT, 0f),
        new BoxSpec("bracket_stud", 0, 118, 8, 1, 1, 1, 1f, 0f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("bracket_neck", 0, 20, 49, 3, 4, 1, 6f, -2f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("cheek_r", 0, 0, 51, 2, 2, 1, 7f, -1f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("cheek_l", 0, 120, 50, 2, 2, 1, 7f, -1f, 0.5f, NO_PIVOT, 0f),
        new BoxSpec("bracket_block", 0, 96, 0, 2, 6, 4, 9f, -3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("ring_rear_0", 0, 48, 19, 3, 3, 5, 11f, -4.3301f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("ring_rear_1", 0, 64, 19, 3, 3, 5, 11f, -4.3301f, -2.5f, new float[] { 11f, 0f, 0f }, 0f, 60f),
        new BoxSpec("ring_rear_2", 0, 80, 19, 3, 3, 5, 11f, -4.3301f, -2.5f, new float[] { 11f, 0f, 0f }, 0f, 120f),
        new BoxSpec("ring_rear_3", 0, 96, 26, 3, 3, 5, 11f, -4.3301f, -2.5f, new float[] { 11f, 0f, 0f }, 0f, 180f),
        new BoxSpec("ring_rear_4", 0, 112, 26, 3, 3, 5, 11f, -4.3301f, -2.5f, new float[] { 11f, 0f, 0f }, 0f, 240f),
        new BoxSpec("ring_rear_5", 0, 0, 27, 3, 3, 5, 11f, -4.3301f, -2.5f, new float[] { 11f, 0f, 0f }, 0f, 300f),
        new BoxSpec("ring_front_0", 0, 108, 0, 3, 3, 5, 24f, -4.3301f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("ring_front_1", 0, 0, 11, 3, 3, 5, 24f, -4.3301f, -2.5f, new float[] { 24f, 0f, 0f }, 0f, 60f),
        new BoxSpec("ring_front_2", 0, 104, 18, 3, 3, 5, 24f, -4.3301f, -2.5f, new float[] { 24f, 0f, 0f }, 0f, 120f),
        new BoxSpec("ring_front_3", 0, 0, 19, 3, 3, 5, 24f, -4.3301f, -2.5f, new float[] { 24f, 0f, 0f }, 0f, 180f),
        new BoxSpec("ring_front_4", 0, 16, 19, 3, 3, 5, 24f, -4.3301f, -2.5f, new float[] { 24f, 0f, 0f }, 0f, 240f),
        new BoxSpec("ring_front_5", 0, 32, 19, 3, 3, 5, 24f, -4.3301f, -2.5f, new float[] { 24f, 0f, 0f }, 0f, 300f),
        new BoxSpec("plate_rear", 0, 10, 0, 1, 7, 4, 14f, -4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("plate_front", 0, 0, 0, 1, 7, 4, 23f, -4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("sleeve_0", 0, 16, 27, 8, 3, 3, 15f, -3.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("sleeve_1", 0, 20, 10, 8, 3, 6, 15f, -2.8f, -2.6f, new float[] { 15f, 1.1f, 0.4f }, 0f, 60f),
        new BoxSpec("sleeve_2", 0, 38, 27, 8, 3, 3, 15f, -2.8f, -1.9f, new float[] { 15f, 1.1f, -0.4f }, 0f, 120f),
        new BoxSpec("sleeve_3", 0, 48, 10, 8, 3, 6, 15f, -3.5f, -3f, new float[] { 15f, 0.4f, 0f }, 0f, 180f),
        new BoxSpec("sleeve_4", 0, 60, 27, 8, 3, 3, 15f, -2.8f, -1.1f, new float[] { 15f, 1.1f, 0.4f }, 0f, 240f),
        new BoxSpec("sleeve_5", 0, 76, 10, 8, 3, 6, 15f, -2.8f, -3.4f, new float[] { 15f, 1.1f, -0.4f }, 0f, 300f),
        new BoxSpec("rail_1", 0, 16, 39, 14, 1, 4, 12f, -5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rail_2", 0, 28, 49, 10, 1, 3, 16f, -6f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rail_3", 0, 54, 49, 6, 1, 3, 17f, -7f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("fin", 2, 100, 40, 4, 1, 4, 14.6f, -8.7f, -2f, new float[] { 18.6f, -8.2f, 0f }, -45f),
        new BoxSpec("grip_block", 0, 82, 50, 3, 2, 2, 23.5f, 3f, -3f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 96, 19, 2, 5, 2, 24f, 5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 124, 5, 1, 3, 1, 25.2f, 4f, -2.5f, new float[] { 25.7f, 4f, -2f }, -23f),
        new BoxSpec("reactor_a", 0, 46, 0, 7, 6, 4, 28f, -3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("reactor_b", 0, 20, 0, 7, 4, 6, 28f, -2f, -3f, NO_PIVOT, 0f),
        new BoxSpec("cap_rear", 0, 82, 0, 1, 4, 6, 27f, -2f, -3f, NO_PIVOT, 0f),
        new BoxSpec("cap_rear_top", 0, 118, 45, 1, 1, 4, 27f, -3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cap_rear_bot", 0, 108, 45, 1, 1, 4, 27f, 2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cap_front", 0, 68, 0, 1, 4, 6, 35f, -2f, -3f, NO_PIVOT, 0f),
        new BoxSpec("cap_front_top", 0, 98, 45, 1, 1, 4, 35f, -3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cap_front_bot", 0, 88, 45, 1, 1, 4, 35f, 2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("core_glow", 0, 104, 10, 7, 4, 4, 28f, -2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("knob_top", 0, 112, 50, 2, 2, 2, 25.5f, -5.3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("bar_top", 0, 62, 54, 9, 1, 1, 27f, -4.8f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("tip_top", 0, 92, 54, 4, 1, 1, 35.7f, -4.8f, -0.5f, new float[] { 35.7f, -4.3f, -0f }, 45f),
        new BoxSpec("knob_right", 0, 102, 50, 3, 2, 2, 24.5f, -5.3f, -1f, new float[] { 24.5f, 0f, 0f }, 0f, 120f),
        new BoxSpec("bar_right", 0, 0, 54, 9, 1, 1, 27f, -4.8f, -0.5f, new float[] { 27f, 0f, 0f }, 0f, 120f),
        new BoxSpec(
            "tip_right",
            0,
            82,
            54,
            4,
            1,
            1,
            35.7f,
            1.65f,
            -4.2239f,
            new float[] { 35.7f, 2.15f, -3.7239f },
            -26.5651f,
            129.232f,
            -37.7612f),
        new BoxSpec("knob_left", 0, 92, 50, 3, 2, 2, 24.5f, -5.3f, -1f, new float[] { 24.5f, 0f, 0f }, 0f, -120f),
        new BoxSpec("bar_left", 0, 42, 53, 9, 1, 1, 27f, -4.8f, -0.5f, new float[] { 27f, 0f, 0f }, 0f, -120f),
        new BoxSpec(
            "tip_left",
            0,
            108,
            8,
            4,
            1,
            1,
            35.7f,
            1.65f,
            3.2239f,
            new float[] { 35.7f, 2.15f, 3.7239f },
            -26.5651f,
            -129.232f,
            37.7612f),
        new BoxSpec("mast_cube", 3, 16, 11, 1, 1, 1, 25f, -6f, -0.5f, new float[] { 25f, 0f, 0f }, 0f, -120f),
        new BoxSpec(
            "mast",
            3,
            120,
            18,
            2,
            6,
            2,
            24.5f,
            -12f,
            -1f,
            new float[] { 25.5f, 0f, 0f },
            -40.8934f,
            -112.208f,
            -20.7048f),
        new BoxSpec("emitter_core_0", 0, 116, 40, 3, 2, 3, 35.7f, -2.6f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("emitter_core_1", 0, 28, 44, 3, 2, 3, 35.7f, -2.6f, -1.5f, new float[] { 35.7f, 0f, 0f }, 0f, 60f),
        new BoxSpec("emitter_core_2", 0, 40, 44, 3, 2, 3, 35.7f, -2.6f, -1.5f, new float[] { 35.7f, 0f, 0f }, 0f, 120f),
        new BoxSpec("emitter_core_3", 0, 52, 44, 3, 2, 3, 35.7f, -2.6f, -1.5f, new float[] { 35.7f, 0f, 0f }, 0f, 180f),
        new BoxSpec("emitter_core_4", 0, 64, 44, 3, 2, 3, 35.7f, -2.6f, -1.5f, new float[] { 35.7f, 0f, 0f }, 0f, 240f),
        new BoxSpec("emitter_core_5", 0, 76, 45, 3, 2, 3, 35.7f, -2.6f, -1.5f, new float[] { 35.7f, 0f, 0f }, 0f, 300f),
        new BoxSpec("cup_0", 0, 82, 27, 3, 1, 4, 39f, -3.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cup_1", 0, 52, 39, 4, 1, 4, 39f, -3.5f, -2f, new float[] { 39f, 0f, 0f }, 0f, 60f),
        new BoxSpec("cup_2", 0, 0, 41, 3, 1, 4, 39f, -3.5f, -2f, new float[] { 39f, 0f, 0f }, 0f, 120f),
        new BoxSpec("cup_3", 0, 68, 39, 4, 1, 4, 39f, -3.5f, -2f, new float[] { 39f, 0f, 0f }, 0f, 180f),
        new BoxSpec("cup_4", 0, 14, 44, 3, 1, 4, 39f, -3.5f, -2f, new float[] { 39f, 0f, 0f }, 0f, 240f),
        new BoxSpec("cup_5", 0, 84, 40, 4, 1, 4, 39f, -3.5f, -2f, new float[] { 39f, 0f, 0f }, 0f, 300f),
        new BoxSpec("starburst_glow_0", 1, 16, 33, 7, 5, 1, 28f, -5.975f, -0.5f, new float[] { 28f, 0f, 0f }, 0f, 25f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 1.1 and an idle offset measured from the origin of the original
    // model. The scale, idle position and third-person placement are solved by InHandFit from the per-gun targets of
    // the in-hand notes (section 7), which already count the original's double idle step at rest, so the gun lands
    // where Techguns' sits on screen. Aimed extra (0, 0, -0.15) as in the notes.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("nuclear_death_ray", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0, -0.15 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];
    /** The starburst pulse pivots on the barrel axis at the blade's centre along the barrel, blocks. */
    private static final double STARBURST_PIVOT_X = 31.5 / 16.0;

    /**
     * The notes' render transforms, timing for the Nuclear Death Ray (the muzzle flash sprite is the beam effect, not
     * geometry).
     */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms; it is listed in
     * {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "nuclear_death_ray",
        BOXES,
        4,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private NuclearDeathRayProfile() {}

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
        // Firing: a subliminal shake (notes): up to 0.05 block in X and 0.025 in Y, roll up to 0.5 degree about Z.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 0.5 * yF(p))
            .translate(
                (p, part) -> FP_X + 0.05 / FP_SCALE * yF(p),
                (p, part) -> FP_Y + 0.025 / FP_SCALE * yF(p),
                (p, part) -> FP_Z)
            .about(STARBURST_PIVOT_X, 0, 0, pulse -> pulse.scale((p, part) -> starburstScale(part)))
            .build();
        // Reload: roll 20 degrees about Z, yaw -10 degrees and a small shift at mid-reload, then back (the notes).
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -20.0 * yR(p))
            .rotY((p, part) -> -10.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225, then the scale and translate InHandFit solved so the box centre lands at the
        // hand and the gun is the notes' 1.26 world blocks long.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil: the notes say a slide of up to 0.25 block along the barrel in the first eighth, no rotation.
        TransformProgram tpRecoil = TransformProgram.builder()
            .translate((p, part) -> -0.25 * kick(p), (p, part) -> 0.0, (p, part) -> 0.0)
            .about(STARBURST_PIVOT_X, 0, 0, pulse -> pulse.scale((p, part) -> starburstScale(part)))
            .build();
        // Reload: 40 degrees about the long axis and 50 degrees about the vertical at mid-animation, then back.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 40.0 * yR(p))
            .rotY((p, part) -> 50.0 * yR(p))
            .build();
        // Ground: yaw 90, scale 0.7 (the notes' 0.75, matched to the model length), centred on the box centre.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.7)
            .translate(-0.67, -0.13, 0)
            .build();
        // Icon: side-on, scale 0.7, the model centred in the slot: centre at texel 21.5 along the barrel.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.7)
            .translate(-1.34, -0.25, 0)
            .build();
        // No muzzle flash geometry: the notes' beam flash is a sprite effect. The starburst part is drawn only while
        // the gun is firing (fire progress above 0) and pulses with it.
        return GunTransforms.builder()
            .parts(4)
            .partVisibility(new StarburstWhileFiring())
            .timing(4, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0, -0.15 }, tpRecoil, tpReload)
            .world(ground, icon)
            .build();
    }

    /** Lowest scale of the starburst pulse. */
    public static final double STARBURST_MIN_SCALE = 0.8;
    /** Highest scale of the starburst pulse. */
    public static final double STARBURST_MAX_SCALE = 1.0;

    /** Seconds per starburst pulse cycle (ten ticks). */
    public static final double STARBURST_PERIOD_SECONDS = 0.5;

    /**
     * The starburst's pulse: a uniform scale from 0.8 to 1.0, one sine cycle per {@link #STARBURST_PERIOD_SECONDS},
     * starting at 0.9 (it first rises to 1.0, then falls to 0.8 and returns to 0.9). Pure.
     *
     * @param seconds time on a continuous clock, seconds
     * @return the scale
     */
    // Spec: docs/techguns-spec/03-gun-visuals-audio/model-notes/nuclear_death_ray.md (starburst pulse)
    // Deviation: the beam fires every tick, so the per-shot fire progress never leaves its first quarter; the pulse
    // runs on a render clock instead and only the visibility rule uses the fire window.
    public static double starburstScaleAt(double seconds) {
        return starburstScale(seconds / STARBURST_PERIOD_SECONDS);
    }

    /**
     * One sine cycle of the pulse per unit of {@code cycles}.
     *
     * @param cycles elapsed cycles (fractional)
     * @return the scale, 0.8 to 1.0
     */
    public static double starburstScale(double cycles) {
        double mid = (STARBURST_MIN_SCALE + STARBURST_MAX_SCALE) / 2.0;
        double amplitude = (STARBURST_MAX_SCALE - STARBURST_MIN_SCALE) / 2.0;
        return mid + amplitude * Math.sin(2.0 * Math.PI * cycles);
    }

    /** The scale term of a part: the starburst pulses on the render clock, every other part is unchanged. */
    private static double starburstScale(int part) {
        return part == PART_STARBURST ? starburstScaleAt(System.nanoTime() * 1e-9) : 1.0;
    }

    /** Draws the starburst only while the gun fires; every other part always. */
    private static final class StarburstWhileFiring implements GunTransforms.PartVisibility {

        @Override
        public boolean visible(int part, int ammoLeft, float reloadProgress) {
            return part != PART_STARBURST;
        }

        @Override
        public boolean visible(int part, int ammoLeft, float reloadProgress, float fireProgress) {
            return part != PART_STARBURST || fireProgress > 0.0f;
        }
    }
}
