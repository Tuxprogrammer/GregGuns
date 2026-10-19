package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.yR;

/**
 * The Power Hammer's model boxes and render transforms, as data. The gun is not registered yet: it is not in
 * {@link ClientGunDefinitions#ALL} until the gameplay batch adds it, together with its gun definition.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/power_hammer.md}: proportions, silhouettes, palettes), by
 * an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures.
 * The look: a short, heavy pneumatic hammer 31 texels long and about 14.8 tall; a dark rust-brown body with a gray
 * shell, a stepped butt joined by three short gray rods, a grip raked 15 degrees, a silver block with a black cap
 * between two bracket plates, a gusset fin on the front flange and a sideways crank pin; out of the front a pale cream
 * piston (a fat diamond core and four thin diamond rods) ends in a dark head with a striker plate.
 *
 * <h2>Parts and motion</h2>
 * Two parts. Part 0 is the static body. Part 1 is the piston and head ("piston"): a pure slide along X with no pivot,
 * up
 * to {@link #PISTON_TRAVEL} blocks (7.2 texels) back from rest. The first-person fire row below plays the notes'
 * primary-attack curve on it; see {@link #pistonRetract(double)}.
 *
 * <h2>Skins</h2>
 * One geometry, two sheets: {@link #TEXTURES} index 0 is the plain hammer and index 1 the Advanced one. They differ
 * only in the striker plate box ({@code striker}). The layout lives in
 * {@code tools/asset-gen/gun-paint/power_hammer.layout.json}; {@code gun_paint.py pack} assigns the UV origins and
 * writes the block below, and {@code gun_paint.py paint} paints both sheets from {@code power_hammer.json}.
 *
 * <h2>Frame of reference</h2>
 * X-family: the piston points toward +X, Y grows downward, in texels. X = 0 is the rear edge of the gear boss and Y = 0
 * its top edge. Z = 0 is the mid-plane (the notes' z = 0.5), and +Z is the crank side with the pin. One texel is a
 * sixteenth of a block.
 *
 * <p>
 * Spec: gun-sheets.md "Power Hammer"; rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class PowerHammerProfile {

    /** Texture resource paths of the model sheet: index 0 is the plain hammer, 1 is the Advanced hammer. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/power_hammer.png",
        "gregguns:textures/models/guns/power_hammer_adv.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The business end in the gun frame, blocks: the striker's front face (texel 15) on the piston axis. */
    public static final float[] MUZZLE = { 15.0f / 16.0f, 1.5f / 16.0f, 0.0f };
    /** The piston's part index. */
    public static final int PISTON_PART = 1;
    /** Full piston retraction, blocks (7.2 texels): the head's back plate then touches the muzzle collar. */
    public static final double PISTON_TRAVEL = 0.45;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** Two parts: part 0 the body, part 1 the piston and head. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("butt_main", 0, 50, 0, 2, 5, 5, -15f, -1.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("butt_step", 0, 28, 29, 1, 5, 3, -15.5f, -1.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("butt_top", 0, 14, 47, 2, 1, 4, -16f, -2.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("butt_bot", 0, 0, 37, 2, 2, 4, -16f, 3.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("neck_low", 0, 10, 56, 3, 2, 2, -13f, 1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("neck_up_a", 0, 56, 36, 3, 1, 1, -13f, -1f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("neck_up_b", 0, 56, 38, 3, 1, 1, -13f, -1f, 0.5f, NO_PIVOT, 0f),
        new BoxSpec("collar", 0, 54, 10, 1, 5, 4, -10f, -1.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("core", 0, 0, 20, 13, 4, 5, -9f, -1f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("shell", 0, 16, 0, 11, 4, 6, -7f, -0.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("under_strip", 0, 12, 42, 12, 1, 4, -9f, 3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("under_rail", 0, 10, 52, 8, 1, 3, -5f, 4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("top_rear", 0, 26, 47, 2, 1, 4, -9f, -2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("bracket_r", 0, 14, 29, 1, 2, 6, -7f, -2.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("silver", 0, 0, 13, 4, 2, 4, -6f, -3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cap", 0, 0, 43, 3, 1, 3, -5.5f, -4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("bracket_f", 0, 0, 29, 1, 2, 6, -2f, -2.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("top_front", 0, 12, 37, 5, 1, 4, -1.5f, -2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("flange", 0, 0, 0, 1, 6, 7, 4f, -0.5f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("muz_collar", 0, 16, 10, 2, 4, 6, 4f, -0.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("jaw_up_a", 0, 0, 47, 3, 1, 4, 3f, -1.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("jaw_up_b", 0, 44, 52, 3, 1, 3, 3f, -2f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("jaw_lo_a", 0, 44, 42, 3, 1, 4, 3f, 3.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("jaw_lo_b", 0, 32, 52, 3, 1, 3, 3f, 4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("fin", 0, 36, 20, 4, 1, 7, 1f, -0.5f, -3.5f, new float[] { 5f, -0.5f, 0f }, 45f),
        new BoxSpec("boss", 0, 38, 47, 3, 3, 2, 0f, 0f, 2f, NO_PIVOT, 0f),
        new BoxSpec("pin", 0, 36, 28, 2, 2, 6, 0.5f, 0.5f, 3f, new float[] { 1.5f, 1.5f, 0f }, 45f),
        new BoxSpec("grip", 0, 52, 28, 3, 6, 2, -5f, 5f, -1f, new float[] { -3.5f, 5f, 0f }, 15f),
        new BoxSpec("guard_floor", 0, 52, 56, 3, 0, 2, -3.5f, 7f, -1f, NO_PIVOT, 0f),
        new BoxSpec("guard_side", 0, 58, 19, 0, 3, 2, -0.5f, 4.1f, -1f, new float[] { -0.5f, 4.1f, 0f }, 15f),
        new BoxSpec("trigger", 0, 62, 19, 0, 2, 1, -1.5f, 4.5f, -0.5f, new float[] { -1.5f, 4.5f, 0f }, -12f),
        new BoxSpec("piston_core", 1, 36, 36, 7, 3, 3, 6f, 0f, -1.5f, new float[] { 6f, 1.5f, 0f }, 0f, 45f),
        new BoxSpec("rod_ul", 1, 20, 58, 7, 1, 1, 6f, -1.2f, -1.5f, new float[] { 6f, -0.7f, -1f }, 0f, 45f),
        new BoxSpec("rod_ur", 1, 36, 58, 7, 1, 1, 6f, -1.2f, 0.5f, new float[] { 6f, -0.7f, 1f }, 0f, 45f),
        new BoxSpec("rod_ll", 1, 20, 56, 7, 1, 1, 6f, 3.2f, -1.5f, new float[] { 6f, 3.7f, -1f }, 0f, 45f),
        new BoxSpec("rod_lr", 1, 36, 56, 7, 1, 1, 6f, 3.2f, 0.5f, new float[] { 6f, 3.7f, 1f }, 0f, 45f),
        new BoxSpec("head_back", 1, 32, 10, 1, 5, 5, 13f, -1f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("head_top", 1, 0, 52, 1, 1, 4, 13f, -2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("head_bot", 1, 48, 47, 1, 1, 4, 13f, 4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("striker", 1, 44, 10, 1, 6, 4, 14f, -1.5f, -2f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the in-hand fit rule places the hammer on the notes' section 7 targets (length, centre, muzzle) with
    // the X-family hold, instead of the notes' scale 1.5 from the original frame's own origin.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("power_hammer", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.1, 0 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms and timing for the Power Hammer. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms; the gameplay batch lists it in
     * {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "power_hammer",
        BOXES,
        2,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private PowerHammerProfile() {}

    /**
     * The piston's retraction during a primary attack, as a fraction of {@link #PISTON_TRAVEL}. It snaps back linearly
     * to full by progress 0.25, then returns to rest over the remaining 75 percent with an ease-out (quick at first,
     * then slowing to a stop).
     *
     * @param p fire-animation progress, 0 to 1
     * @return 0 at rest, 1 fully retracted
     */
    public static double pistonRetract(double p) {
        if (p <= 0.0 || p >= 1.0) {
            return 0.0;
        }
        if (p < 0.25) {
            return p / 0.25;
        }
        double left = 1.0 - (p - 0.25) / 0.75;
        return left * left;
    }

    /** The body's forward lunge during a primary attack, blocks: 0.15 at progress 0.5, zero at both ends. */
    private static double lunge(double p) {
        return 0.15 * Math.sin(Math.PI * p);
    }

    private static double pistonOffset(double p, int part) {
        return part == PISTON_PART ? -PISTON_TRAVEL * pistonRetract(p) : 0.0;
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
        // Firing (notes, primary attack): the body rolls +-1.5 degrees and lunges forward 0.15 block; the piston
        // slides back along the model X axis (the second translate runs in the gun frame, per part).
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 1.5 * Math.sin(2.0 * Math.PI * p))
            .translate((p, part) -> FP_X + lunge(p) / FP_SCALE, TransformProgram.c(FP_Y), TransformProgram.c(FP_Z))
            .translate(PowerHammerProfile::pistonOffset, TransformProgram.c(0), TransformProgram.c(0))
            .build();
        // Reload: the whole gun rolls about 32 degrees, then comes back (notes: plateau about a quarter to three
        // quarters of the 45 ticks).
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -32.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225, scale 1.25. The grip (about 0.28 block behind the origin, 0.5 down) lands
        // at the hand.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Reload: about 30 degrees about the long axis and 40 about the vertical, then back (notes).
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .build();
        // Ground: scale 0.8 (the notes), the model centred (centre at texel -0.5, 3.4) so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.8)
            .translate(0.03, -0.21, 0)
            .build();
        // Icon: side-on, scale 0.75, the model centred in the slot.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.75)
            .translate(0.03, -0.21, 0)
            .build();
        // No muzzle flash (the notes: disabled for this tool) and no scope.
        return GunTransforms.builder()
            .parts(2)
            .timing(10, 0, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.1, 0 }, TransformProgram.EMPTY, tpReload)
            .world(ground, icon)
            .build();
    }
}
