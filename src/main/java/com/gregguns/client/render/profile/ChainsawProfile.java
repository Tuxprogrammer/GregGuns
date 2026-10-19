package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.yR;

/**
 * The Chainsaw's model boxes and render transforms, as data. The gun is not registered yet: it is not in
 * {@link ClientGunDefinitions#ALL} until the gameplay batch adds it, together with its gun definition.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/chainsaw.md}: proportions, silhouettes, palettes), by an
 * agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures. The
 * look: a heavy safety-orange saw 70 texels long. An orange engine block (13 x 9 x 11) with a taller rear column, a
 * cover and a silver top plate; a 33-long gray cutting bar with a stepped nose, bolted to the -Z flank behind a
 * clamp; a black tube frame (an arching top handle with a long raked rear leg, a side post with a strut, and a
 * ladder hand guard); an orange rear loop handle with a slanted black rubber grip; a gray square, a black grille
 * square and a diamond emblem on the +Z flank. The layout lives in
 * {@code tools/asset-gen/gun-paint/chainsaw.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns the UV
 * origins and writes the generated block below, and {@code gun_paint.py paint} paints the 128x96 sheet from the role
 * map {@code gun-paint/chainsaw.json}. There are no camo variants.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the cutting bar toward +X and Y growing downward, in texels, as authored. X = 0
 * is 3 texels in front of the rear face of the engine housing, Y = 0 is 4 texels below its top face; the bar's
 * mid-plane
 * is at Z = -3 (the engine is centered on Z = 0.5). One texel is a sixteenth of a block.
 *
 * <h2>The running chain (two animated parts)</h2>
 * The chain is a zero-thickness plane 36 x 8 in the middle of the bar slab, so only a 1-texel serrated lip shows above
 * and below the bar and the extra texels that fill out the stepped nose. It has two frames whose teeth are offset by
 * one
 * texel. The profile format has no per-part texture frames, so each frame is its own part: {@link #CHAIN_PART_A} (box
 * {@code chain}) and {@link #CHAIN_PART_B} (box {@code chain_b}), identical in place, differing only in the texels
 * painted on the sheet. A renderer draws exactly one of the two: {@link #chainPart(double)} gives the part to draw from
 * the fire-animation progress (part A at rest and for every even half-step, part B for odd ones); skip the other.
 * Drawing both shows a solid lip, which is the wrong static look.
 *
 * <p>
 * Spec: gun-sheets.md "Chainsaw" (timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class ChainsawProfile {

    /** Texture resource paths of the model sheet: only the default (the notes list no camo variants). */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/chainsaw.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 96;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /**
     * The muzzle in the gun frame, blocks: x forward, y down, z: the bar tip (texel 48) at its mid-plane (texel -3).
     */
    public static final float[] MUZZLE = { 48.0f / 16.0f, 0.5f / 16.0f, -3.0f / 16.0f };

    /** Part index of chain frame A (box {@code chain}): drawn at rest and on even half-steps. */
    public static final int CHAIN_PART_A = 1;
    /** Part index of chain frame B (box {@code chain_b}): the same plane with the teeth shifted one texel. */
    public static final int CHAIN_PART_B = 2;
    /** The chain flips every 1/40 of the fire animation. */
    public static final int CHAIN_STEPS = 40;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** Part 0 is the whole saw; parts 1 and 2 are the two chain frames. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("housing", 0, 0, 0, 13, 9, 11, -3f, -4f, -5f, NO_PIVOT, 0f),
        new BoxSpec("rear_column", 0, 48, 0, 5, 12, 8, -8f, -7f, -3f, NO_PIVOT, 0f),
        new BoxSpec("top_cover", 0, 90, 12, 10, 3, 8, -3f, -7f, -3f, NO_PIVOT, 0f),
        new BoxSpec("top_plate", 0, 90, 23, 8, 2, 6, -3f, -9f, -2f, NO_PIVOT, 0f),
        new BoxSpec("plate_strip", 0, 72, 32, 1, 1, 6, -4f, -8f, -2f, NO_PIVOT, 0f),
        new BoxSpec("plate_step", 0, 106, 31, 3, 1, 6, 5f, -8f, -2f, NO_PIVOT, 0f),
        new BoxSpec("flange", 0, 74, 0, 1, 7, 7, 10f, -3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("clamp", 0, 74, 14, 2, 8, 2, 10f, -3.5f, -4f, NO_PIVOT, 0f),
        new BoxSpec("skid", 0, 90, 0, 3, 1, 11, 4.5f, 4.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("panel_p", 0, 92, 50, 3, 3, 1, -7f, -3f, 5f, NO_PIVOT, 0f),
        new BoxSpec("grille", 0, 112, 45, 4, 4, 1, -7.5f, 0.5f, 5f, NO_PIVOT, 0f),
        new BoxSpec("emblem", 0, 100, 45, 5, 5, 1, -1f, -2f, 6f, new float[] { 1.5f, 0.5f, 6.5f }, 45f),
        new BoxSpec("panel_n", 0, 56, 47, 3, 3, 1, -7f, -2.5f, -4f, NO_PIVOT, 0f),
        new BoxSpec("black_block", 0, 68, 46, 2, 3, 2, -6.5f, -4f, -5f, NO_PIVOT, 0f),
        new BoxSpec("top_bar", 0, 0, 20, 2, 2, 7, 5f, -13f, 0f, NO_PIVOT, 0f),
        new BoxSpec("cross", 0, 90, 31, 2, 2, 6, 5.1f, -13f, -5.4f, new float[] { 6.1f, -12f, 0.6f }, 0f, 0f, 35f),
        new BoxSpec("end_block", 0, 40, 47, 2, 2, 2, 1.5f, -13f, -5f, NO_PIVOT, 0f),
        new BoxSpec("leg", 0, 18, 20, 12, 2, 2, -5.65f, -4.36f, -5f, new float[] { -5.65f, -3.36f, -4f }, -49f),
        new BoxSpec("post", 0, 118, 0, 2, 10, 2, 5f, -7f, 9f, NO_PIVOT, 0f),
        new BoxSpec("strut", 0, 82, 14, 2, 7, 2, 5f, -14f, 9f, new float[] { 6f, -7f, 10f }, 0f, 35f),
        new BoxSpec("foot_a", 0, 100, 39, 2, 1, 1, 5f, 3.5f, 7f, NO_PIVOT, 0f),
        new BoxSpec("foot_b", 0, 48, 47, 2, 2, 2, 5f, 3f, 8f, NO_PIVOT, 0f),
        new BoxSpec("foot_c", 0, 100, 41, 2, 1, 1, 5f, 3f, 10f, NO_PIVOT, 0f),
        new BoxSpec("rung_1", 0, 106, 38, 1, 1, 6, 11.5f, -14f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rung_2", 0, 72, 39, 1, 1, 6, 11.5f, -12f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rung_3", 0, 86, 39, 1, 1, 6, 11.5f, -10f, -2f, NO_PIVOT, 0f),
        new BoxSpec("guard_post_n", 0, 86, 32, 1, 5, 1, 11.5f, -14f, -3f, NO_PIVOT, 0f),
        new BoxSpec("guard_post_p", 0, 124, 37, 1, 5, 1, 11.5f, -14f, 4f, NO_PIVOT, 0f),
        new BoxSpec("brace_n", 0, 124, 23, 1, 6, 1, 11.5f, -9f, -3f, new float[] { 12f, -9f, -2.5f }, 42f),
        new BoxSpec("brace_p", 0, 124, 30, 1, 6, 1, 11.5f, -9f, 4f, new float[] { 12f, -9f, 4.5f }, 42f),
        new BoxSpec("loop_lower", 0, 0, 47, 9, 2, 2, -19f, 3f, 0f, NO_PIVOT, 0f),
        new BoxSpec("loop_post", 0, 84, 46, 2, 3, 2, -10f, 2f, 0f, NO_PIVOT, 0f),
        new BoxSpec("loop_upright", 0, 118, 23, 1, 5, 2, -9f, -3f, 0f, NO_PIVOT, 0f),
        new BoxSpec("loop_upper", 0, 22, 47, 3, 2, 2, -11f, -5f, 0f, NO_PIVOT, 0f),
        new BoxSpec("cap_main", 0, 76, 46, 2, 3, 2, -22f, -1f, 0f, NO_PIVOT, 0f),
        new BoxSpec("cap_top", 0, 6, 29, 1, 1, 2, -22f, -2f, 0f, NO_PIVOT, 0f),
        new BoxSpec("cap_bot", 0, 0, 29, 1, 1, 2, -22f, 2f, 0f, NO_PIVOT, 0f),
        new BoxSpec("cap_blk_t", 0, 32, 47, 2, 2, 2, -21f, -3f, 0f, NO_PIVOT, 0f),
        new BoxSpec("cap_blk_b", 0, 92, 46, 2, 2, 2, -21f, 2f, 0f, NO_PIVOT, 0f),
        new BoxSpec("loop_foot", 0, 12, 29, 1, 1, 2, -20f, 4f, 0f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 46, 20, 9, 2, 2, -19f, -3f, 0f, new float[] { -19f, -1f, 1f }, -13f),
        new BoxSpec("bar", 0, 0, 40, 33, 6, 1, 12f, -2.5f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("tip1", 0, 120, 38, 1, 5, 1, 45f, -2f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("tip2", 0, 68, 20, 1, 3, 1, 46f, -1f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("chain", 1, 18, 24, 36, 8, 0, 12f, -3.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("chain_b", 2, 0, 32, 36, 8, 0, 12f, -3.5f, -3f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the in-hand fit rule places the saw on the notes' section 7 targets (length, centre, muzzle) with the
    // X-family hold, instead of the notes' scale 1.35 from the original frame's own origin.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("chainsaw", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, -0.15, 0.3 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms and timing for the Chainsaw (no muzzle flash). */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, texture and transforms; the gameplay batch lists it in
     * {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "chainsaw",
        BOXES,
        3,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private ChainsawProfile() {}

    /**
     * Which chain frame to draw.
     *
     * @param fireProgress progress 0 to 1 of the fire animation; 0 when idle, in the inventory or on the ground
     * @return {@link #CHAIN_PART_A} or {@link #CHAIN_PART_B}: the frame index is the rounded value of
     *         {@code fireProgress x 40}, modulo 2, so rest shows frame A
     */
    public static int chainPart(double fireProgress) {
        long step = Math.round(Math.max(0.0, fireProgress) * CHAIN_STEPS);
        return step % 2 == 0 ? CHAIN_PART_A : CHAIN_PART_B;
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
        // Firing: one sine cycle of shake across the animation (the notes: a 0.15 block slide along X, and rotations
        // of -5 about Z, -2.5 about Y, -1.5 about X). The slide is in scaled-frame blocks, so it is divided by the
        // scale like the other guns' kicks.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> -5.0 * Math.sin(2.0 * Math.PI * p))
            .rotY((p, part) -> -2.5 * Math.sin(2.0 * Math.PI * p))
            .rotX((p, part) -> -1.5 * Math.sin(2.0 * Math.PI * p))
            .translate(
                (p, part) -> FP_X + 0.15 / FP_SCALE * Math.sin(2.0 * Math.PI * p),
                (p, part) -> FP_Y,
                (p, part) -> FP_Z)
            .build();
        // Reload: roll up to 32 degrees about Z and yaw 15 (the notes), then back.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -32.0 * yR(p))
            .rotY((p, part) -> 15.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225, scale 0.6 (Deviation: the notes' 1.30 was for a model of unknown size; this
        // one is 4.4 blocks long, so 0.6 holds it at about 2.6 blocks like the other long guns). The top handle (about
        // 0.375 forward and 0.75 above the model origin) lands at the hand. Aimed extra (0, -0.15, 0.3) as in the
        // notes.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // No recoil on the body in third person (the notes).
        TransformProgram tpRecoil = TransformProgram.builder()
            .build();
        // Reload: about 30 degrees about X and 40 about Y at mid-animation, then back.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .build();
        // Ground: yaw 90, scaled to lie at about 1.8 blocks, centred (box centre at texel 13, -4.25, 3).
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.4)
            .translate(-0.8125, 0.265, -0.19)
            .build();
        // Icon: side-on, scale 0.3 so the 4.4 block saw fits the slot, centred.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.3)
            .translate(-0.8125, 0.265, -0.19)
            .build();
        return GunTransforms.builder()
            .parts(3)
            .partVisibility(new ChainFrames())
            .timing(3, 0, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, -0.15, 0.3 }, tpRecoil, tpReload)
            .world(ground, icon)
            .build();
    }

    /** Draws exactly one of the two chain frames, chosen by the fire animation progress; every other part always. */
    private static final class ChainFrames implements GunTransforms.PartVisibility {

        @Override
        public boolean visible(int part, int ammoLeft, float reloadProgress) {
            return visible(part, ammoLeft, reloadProgress, 0.0f);
        }

        @Override
        public boolean visible(int part, int ammoLeft, float reloadProgress, float fireProgress) {
            if (part != CHAIN_PART_A && part != CHAIN_PART_B) {
                return true;
            }
            return part == chainPart(fireProgress);
        }
    }
}
