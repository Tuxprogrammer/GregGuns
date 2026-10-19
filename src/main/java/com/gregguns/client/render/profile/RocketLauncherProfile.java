package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.p66;
import static com.gregguns.client.anim.GunCurves.yF;

/**
 * The Rocket Launcher's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/rocket_launcher.md}: proportions, silhouettes,
 * palettes), by an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists
 * or textures. The look: a bazooka, 45 texels long. One plain blue-gray octagonal tube, 36 long and 6 across, built
 * from three overlapping slabs; a flared black cap at the back and a longer black muzzle ring at the front, both
 * 8 by 8 plus shapes; a black collar in the front-middle with two side plates, a small top bridge and a tiny two-vane
 * flip-up sight; one raked pistol-grip slab and a trigger stub. The loaded rocket's red three-step nose is a second
 * part, drawn only when a rocket is loaded. The layout lives in
 * {@code tools/asset-gen/gun-paint/rocket_launcher.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns
 * the UV origins and writes the generated block below, and {@code gun_paint.py paint} paints the 128x64 sheet from
 * the role map {@code gun-paint/rocket_launcher.json}. {@code RocketLauncherProfileTest} checks the boxes against the
 * layout file and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the muzzle points toward +X, Y grows downward, in texels (one texel is a sixteenth of a block). Y = 0 and
 * Z = 0 are the axis of the tube; X = 0 is the back face of the rear cap, X = 45 the front face of the muzzle ring.
 *
 * <h2>Parts</h2>
 * Part 0 is the launcher. Part 1 is the rocket's nose (three boxes past the muzzle ring, X 45 to 48), hidden while the
 * launcher is empty or in the first half of a reload (see {@link #noseVisible}). Both parts get the same transforms.
 *
 * <p>
 * Spec: model-notes/rocket_launcher.md sections 6 and 7 (framing, timing); rendering-framework.md section 6.1
 * (X-family), 10 (ammo-dependent geometry).
 */
public final class RocketLauncherProfile {

    /** Texture resource paths of the model sheet. The launcher has no camo skins. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/rocket_launcher.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down; the front face of the muzzle ring on the axis. */
    public static final float[] MUZZLE = { 45.0f / 16.0f, 0.0f, 0.0f };
    /** The part index of the loaded rocket's nose. */
    public static final int NOSE_PART = 1;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** Two parts: the launcher, and the rocket nose. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("tube_core", 0, 0, 12, 36, 5, 5, 3f, -2.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_vert", 0, 0, 31, 36, 6, 3, 3f, -3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("tube_wide", 0, 0, 22, 36, 3, 6, 3f, -1.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("cap_core", 0, 46, 0, 3, 6, 6, 0f, -3f, -3f, NO_PIVOT, 0f),
        new BoxSpec("cap_top", 0, 98, 6, 3, 1, 4, 0f, -4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cap_bottom", 0, 84, 6, 3, 1, 4, 0f, 3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cap_left", 0, 120, 0, 3, 4, 1, 0f, -2f, -4f, NO_PIVOT, 0f),
        new BoxSpec("cap_right", 0, 120, 5, 3, 4, 1, 0f, -2f, 3f, NO_PIVOT, 0f),
        new BoxSpec("ring_core", 0, 0, 0, 6, 6, 6, 39f, -3f, -3f, NO_PIVOT, 0f),
        new BoxSpec("ring_top", 0, 100, 11, 6, 1, 3, 39f, -4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("ring_bottom", 0, 82, 11, 6, 1, 3, 39f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("ring_left", 0, 82, 15, 6, 3, 1, 39f, -1.5f, -4f, NO_PIVOT, 0f),
        new BoxSpec("ring_right", 0, 96, 15, 6, 3, 1, 39f, -1.5f, 3f, NO_PIVOT, 0f),
        new BoxSpec("collar", 0, 24, 0, 6, 7, 5, 25f, -3.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("plate_left", 0, 84, 0, 6, 5, 1, 25f, -2.5f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("plate_right", 0, 98, 0, 6, 5, 1, 25f, -2.5f, 2.5f, NO_PIVOT, 0f),
        new BoxSpec("bridge", 0, 110, 15, 3, 1, 3, 26f, -4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("sight_vane_left", 0, 118, 6, 1, 3, 0, 29f, -6.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("sight_vane_right", 0, 74, 8, 1, 3, 0, 29f, -6.5f, 1f, NO_PIVOT, 0f),
        new BoxSpec("sight_top", 0, 76, 8, 1, 0, 2, 29f, -6.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 64, 0, 3, 7, 2, 26f, 3.5f, -1f, new float[] { 29f, 3.5f, 0f }, 17f),
        new BoxSpec("trigger", 0, 64, 9, 2, 1, 1, 28f, 3.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("rocket_collar", 1, 74, 0, 1, 4, 4, 45f, -2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rocket_step", 1, 112, 0, 1, 3, 3, 46f, -1.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rocket_nose", 1, 112, 6, 1, 2, 2, 47f, -1f, -1f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first person S(2.25) from the original frame's own origin. The scale follows the
    // FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's precedent), and the idle
    // position puts this model's box centre on the family anchor. The notes' fire and reload offsets are multiplied by
    // K so they move the launcher as far on screen as the notes intend.
    private static final double SHEET_FP_SCALE = 2.25;
    /** On-screen length of the launcher in first person, blocks of the scaled frame. */
    private static final double LAUNCHER_LENGTH = 2.0;
    private static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), LAUNCHER_LENGTH);
    private static final double FP_AMP = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    private static final double[] CENTRE = FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE);
    private static final double[] IDLE = FirstPersonFit.idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, CENTRE);
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms and timing for the Rocket Launcher. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, texture and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "rocket_launcher",
        BOXES,
        2,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private RocketLauncherProfile() {}

    /**
     * Whether the loaded rocket's nose is drawn: only with a round in the tube, and not in the first half of a reload
     * (it appears at the reload's midpoint).
     *
     * @param part           the part index; only {@link #NOSE_PART} is ever hidden
     * @param ammoLeft       rounds loaded
     * @param reloadProgress reload progress 0 to 1, or above 1 when not reloading
     * @return true if the part is drawn
     */
    public static boolean noseVisible(int part, int ammoLeft, float reloadProgress) {
        return part != NOSE_PART || (ammoLeft > 0 && reloadProgress >= 0.5f);
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
        // Firing: a roll to -12.5 degrees at mid-window and a shove of 0.4 back along the barrel (yF peaks at -0.5,
        // so the factors are 25 and 0.8).
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 25.0 * yF(p))
            .translate((p, part) -> FP_X + FP_AMP * 0.8 * yF(p), TransformProgram.c(FP_Y), TransformProgram.c(FP_Z))
            .build();
        // Reload: a roll up to 40 degrees and a yaw of -20, held through the middle, with a shift of (0.5, 0.15,
        // 0.15) at the peak, rising and falling smoothly. The roll is negative like the other long guns.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -40.0 * p66(p))
            .rotY((p, part) -> -20.0 * p66(p))
            .translate(
                (p, part) -> FP_X + FP_AMP * 0.5 * p66(p),
                (p, part) -> FP_Y + FP_AMP * 0.15 * p66(p),
                (p, part) -> FP_Z + FP_AMP * 0.15 * p66(p))
            .build();
        // Third person: rotX180, rotY225, S(1.4), then T = minus the grip, so the grip sits in the hand and the tube
        // lies forward over the shoulder. Aimed extra translate as the other long guns.
        // Deviation: the notes' scale 1.5 is for the original frame; 1.4 keeps the 2.8-block launcher about 3.9
        // blocks long, close to the bolt-action's 4.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(1.4)
            .translate(-1.7, -0.4, 0)
            .build();
        // Recoil about (0, 0.3, -0.9): the launcher rolls about the shoulder to -20 degrees in the first eighth and
        // eases back over the rest of the first half.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -20.0 * kick(p)))
            .build();
        // Reload: pitch -20 degrees and yaw -30 degrees at mid-reload.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> -20.0 * p66(p))
            .rotY((p, part) -> -30.0 * p66(p))
            .build();
        // Ground: yaw 90, scale 0.9 (the notes' 1.0 trimmed to suit the 2.8-block launcher), centred on the model's
        // middle so it lies on the ground.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.9)
            .translate(-CENTRE[0], -CENTRE[1], 0)
            .build();
        // Icon: side-on, muzzle to the right, scale 0.68 as the notes say, centred on the model's middle.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.68)
            .translate(-CENTRE[0], -CENTRE[1], 0)
            .build();
        return GunTransforms.builder()
            .parts(2)
            .timing(10, 5, 40)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.2, -0.25 }, tpRecoil, tpReload)
            .world(ground, icon)
            .partVisibility(RocketLauncherProfile::noseVisible)
            .build();
    }
}
