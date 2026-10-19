package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.sin;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Minigun's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/minigun.md}: proportions, silhouettes, palette), by an
 * agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures.
 * The look: a six-barrel rotary cannon. A stepped-octagon gearbox, a dark yoke, Y-shaped top frame, carry handle and
 * trapezoid rear loop (all part 0, the housing), a pale ammo drum on the shooter's right and a diamond drive housing
 * on the left, and the rotor (part 1): six 2x2 barrels on a 2.5 radius ring at 60 degree spacing, six 1x1 tips and
 * three near-black clamp discs. The layout lives in {@code tools/asset-gen/gun-paint/minigun.layout.json};
 * {@code tools/asset-gen/gun_paint.py pack} assigns the UV origins and writes the generated block below, and
 * {@code gun_paint.py paint} paints {@code minigun.png} (128x128, one texture, no camo) from the role map
 * {@code gun-paint/minigun.json}. {@code MinigunProfileTest} checks the boxes against the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: muzzle toward +X, Y growing downward, in texels. The origin is on the barrel axis (Y = 0, Z = 0), so a
 * turn about model X through the origin is the rotor's spin. One texel is a sixteenth of a block.
 *
 * <h2>Rotor spin</h2>
 * The rotor is part {@value #ROTOR_PART}. The fire rows turn it one full turn per recoil window (5 ticks, 250 ms) as
 * the notes describe the original. Deviation not built: the notes' optional spin-up and spin-down ramp (about 0.5 s)
 * needs the renderer to hand the model a persistent spin phase that outlives the fire window; the renderer only gives
 * a part the window progress, so the rotor stops at angle 0 when the window ends. The hook needed is a per-item
 * "spin phase" in {@code ModelState}, advanced by the client each frame while firing and eased out afterward, that
 * a fire and idle row can read through {@link #rotorTurnDegrees}.
 *
 * <p>
 * Spec: gun-sheets.md "Minigun"; rendering-framework.md section 6.1 (X-family) and section 8.
 */
public final class MinigunProfile {

    /** Texture resource path of the model sheet. */
    public static final String TEXTURE = "gregguns:textures/models/guns/minigun.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 128;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** Index of the rotor (barrel cluster) part; the housing is part 0. */
    public static final int ROTOR_PART = 1;
    /** Recoil window and turn length of the rotor, ticks (250 ms). */
    public static final int RECOIL_TICKS = 5;
    /** The muzzle in the gun frame, blocks: the front of the cluster, 38 texels forward of the origin, on the axis. */
    public static final float[] MUZZLE = { 38.0f / 16.0f, 0.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** Two parts: part 0 the housing, part 1 the rotor. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("barrel_r0", 1, 30, 70, 12, 2, 2, 5f, -3.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("barrel_f0", 1, 108, 74, 8, 2, 2, 18f, -3.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("tip0", 1, 40, 85, 12, 1, 1, 26f, -3f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel_r1", 1, 58, 70, 12, 2, 2, 5f, -3.5f, -1f, new float[] { 0f, 0f, 0f }, 0f, 60f),
        new BoxSpec("barrel_f1", 1, 24, 78, 8, 2, 2, 18f, -3.5f, -1f, new float[] { 0f, 0f, 0f }, 0f, 60f),
        new BoxSpec("tip1", 1, 66, 85, 12, 1, 1, 26f, -3f, -0.5f, new float[] { 0f, 0f, 0f }, 0f, 60f),
        new BoxSpec("barrel_r2", 1, 86, 70, 12, 2, 2, 5f, -3.5f, -1f, new float[] { 0f, 0f, 0f }, 0f, 120f),
        new BoxSpec("barrel_f2", 1, 44, 78, 8, 2, 2, 18f, -3.5f, -1f, new float[] { 0f, 0f, 0f }, 0f, 120f),
        new BoxSpec("tip2", 1, 92, 85, 12, 1, 1, 26f, -3f, -0.5f, new float[] { 0f, 0f, 0f }, 0f, 120f),
        new BoxSpec("barrel_r3", 1, 0, 71, 12, 2, 2, 5f, -3.5f, -1f, new float[] { 0f, 0f, 0f }, 0f, 180f),
        new BoxSpec("barrel_f3", 1, 64, 78, 8, 2, 2, 18f, -3.5f, -1f, new float[] { 0f, 0f, 0f }, 0f, 180f),
        new BoxSpec("tip3", 1, 0, 86, 12, 1, 1, 26f, -3f, -0.5f, new float[] { 0f, 0f, 0f }, 0f, 180f),
        new BoxSpec("barrel_r4", 1, 28, 74, 12, 2, 2, 5f, -3.5f, -1f, new float[] { 0f, 0f, 0f }, 0f, 240f),
        new BoxSpec("barrel_f4", 1, 84, 78, 8, 2, 2, 18f, -3.5f, -1f, new float[] { 0f, 0f, 0f }, 0f, 240f),
        new BoxSpec("tip4", 1, 26, 87, 12, 1, 1, 26f, -3f, -0.5f, new float[] { 0f, 0f, 0f }, 0f, 240f),
        new BoxSpec("barrel_r5", 1, 56, 74, 12, 2, 2, 5f, -3.5f, -1f, new float[] { 0f, 0f, 0f }, 0f, 300f),
        new BoxSpec("barrel_f5", 1, 104, 78, 8, 2, 2, 18f, -3.5f, -1f, new float[] { 0f, 0f, 0f }, 0f, 300f),
        new BoxSpec("tip5", 1, 52, 87, 12, 1, 1, 26f, -3f, -0.5f, new float[] { 0f, 0f, 0f }, 0f, 300f),
        new BoxSpec("hub_mid", 1, 52, 16, 2, 4, 8, 3f, -2f, -4f, NO_PIVOT, 0f),
        new BoxSpec("hub_stt", 1, 16, 59, 2, 1, 6, 3f, -3f, -3f, NO_PIVOT, 0f),
        new BoxSpec("hub_stb", 1, 0, 59, 2, 1, 6, 3f, 2f, -3f, NO_PIVOT, 0f),
        new BoxSpec("hub_cpt", 1, 108, 65, 2, 1, 4, 3f, -4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("hub_cpb", 1, 96, 65, 2, 1, 4, 3f, 3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("clamp_mid", 1, 72, 16, 1, 4, 8, 17f, -2f, -4f, NO_PIVOT, 0f),
        new BoxSpec("clamp_stt", 1, 82, 45, 1, 1, 6, 17f, -3f, -3f, NO_PIVOT, 0f),
        new BoxSpec("clamp_stb", 1, 114, 44, 1, 1, 6, 17f, 2f, -3f, NO_PIVOT, 0f),
        new BoxSpec("clamp_cpt", 1, 10, 66, 1, 1, 4, 17f, -4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("clamp_cpb", 1, 0, 66, 1, 1, 4, 17f, 3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("front_mid", 1, 90, 16, 1, 3, 7, 35f, -1.5f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("front_stt", 1, 108, 59, 1, 1, 5, 35f, -2.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("front_stb", 1, 96, 59, 1, 1, 5, 35f, 1.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("front_cpt", 1, 30, 66, 1, 1, 3, 35f, -3.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("front_cpb", 1, 120, 65, 1, 1, 3, 35f, 2.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("gear_core", 0, 0, 0, 16, 6, 10, -13f, -3f, -5f, NO_PIVOT, 0f),
        new BoxSpec("gear_s1t", 0, 48, 36, 16, 1, 8, -13f, -4f, -4f, NO_PIVOT, 0f),
        new BoxSpec("gear_s1b", 0, 0, 30, 16, 1, 8, -13f, 3f, -4f, NO_PIVOT, 0f),
        new BoxSpec("gear_s2t", 0, 0, 52, 16, 1, 6, -13f, -5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("gear_s2b", 0, 38, 45, 16, 1, 6, -13f, 4f, -3f, NO_PIVOT, 0f),
        new BoxSpec("yoke_top", 0, 94, 0, 3, 2, 14, 4f, -7.5f, -7f, NO_PIVOT, 0f),
        new BoxSpec("yoke_pl_r", 0, 32, 59, 2, 6, 1, 4.5f, -3.5f, -6.5f, NO_PIVOT, 0f),
        new BoxSpec("yoke_pl_l", 0, 122, 23, 2, 6, 1, 4.5f, -3.5f, 5.5f, NO_PIVOT, 0f),
        new BoxSpec("yoke_cb_r", 0, 114, 70, 3, 2, 2, 4f, -5.5f, -7f, NO_PIVOT, 0f),
        new BoxSpec("yoke_cb_l", 0, 20, 66, 3, 3, 2, 4f, -5.5f, 5f, NO_PIVOT, 0f),
        new BoxSpec("yoke_bot", 0, 102, 26, 2, 1, 8, 4.5f, 4.5f, -4f, NO_PIVOT, 0f),
        new BoxSpec("yoke_gus_r", 0, 32, 47, 2, 4, 1, 4.5f, 1.75f, -5.5f, new float[] { 0f, 3.75f, -5f }, 0f, 45f),
        new BoxSpec("yoke_gus_l", 0, 122, 30, 2, 4, 1, 4.5f, 1.75f, 4.5f, new float[] { 0f, 3.75f, 5f }, 0f, -45f),
        new BoxSpec("top_plate", 0, 40, 82, 13, 1, 2, -13f, -7f, -1f, NO_PIVOT, 0f),
        new BoxSpec("top_bridge", 0, 110, 82, 2, 1, 2, -2f, -6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("strut_r", 0, 90, 82, 8, 1, 2, 0f, -7.02f, -1f, new float[] { 0f, -6.5f, 0f }, 0f, 0f, 36f),
        new BoxSpec("strut_l", 0, 70, 82, 8, 1, 2, 0f, -7.04f, -1f, new float[] { 0f, -6.5f, 0f }, 0f, 0f, -36f),
        new BoxSpec("carry_bar", 0, 96, 44, 2, 1, 7, 4.5f, -11f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("carry_leg_r", 0, 22, 82, 2, 3, 1, 4.5f, -10.5f, -4.5f, NO_PIVOT, 0f),
        new BoxSpec("carry_leg_l", 0, 16, 79, 2, 3, 1, 4.5f, -10.5f, 3.5f, NO_PIVOT, 0f),
        new BoxSpec("loop_front", 0, 44, 16, 2, 11, 2, -15f, -7f, -1f, NO_PIVOT, 0f),
        new BoxSpec("loop_end", 0, 118, 16, 2, 5, 2, -24f, -4f, -1f, NO_PIVOT, 0f),
        new BoxSpec("loop_up", 0, 0, 75, 10, 2, 2, -24f, -4f, -1f, new float[] { -24f, -3f, 0f }, -15f),
        new BoxSpec("loop_lo", 0, 84, 74, 10, 2, 2, -24f, -1f, -1f, new float[] { -24f, 0f, 0f }, 15f),
        new BoxSpec("drum_core", 0, 0, 16, 14, 6, 8, -11f, 2f, -12f, NO_PIVOT, 0f),
        new BoxSpec("drum_cst", 0, 84, 52, 14, 1, 6, -11f, 1f, -11f, NO_PIVOT, 0f),
        new BoxSpec("drum_csb", 0, 44, 52, 14, 1, 6, -11f, 8f, -11f, NO_PIVOT, 0f),
        new BoxSpec("drum_out", 0, 52, 0, 12, 7, 9, -10f, 1.5f, -12.5f, NO_PIVOT, 0f),
        new BoxSpec("drum_ost", 0, 0, 39, 12, 1, 7, -10f, 0.5f, -11.5f, NO_PIVOT, 0f),
        new BoxSpec("drum_osb", 0, 52, 28, 12, 1, 7, -10f, 8.5f, -11.5f, NO_PIVOT, 0f),
        new BoxSpec("drum_capt", 0, 38, 65, 12, 1, 4, -10f, -0.5f, -10f, NO_PIVOT, 0f),
        new BoxSpec("drum_capb", 0, 0, 47, 12, 1, 4, -10f, 9.5f, -10f, NO_PIVOT, 0f),
        new BoxSpec("drum_plate", 0, 70, 65, 12, 4, 1, -10f, 3f, -13.5f, NO_PIVOT, 0f),
        new BoxSpec("drive_tube", 0, 96, 36, 10, 4, 4, -10f, -0.5f, 4f, new float[] { 0f, 1.5f, 6f }, 0f, 45f),
        new BoxSpec("drive_core", 0, 38, 59, 12, 3, 3, -11f, 0f, 4.5f, new float[] { 0f, 1.5f, 6f }, 0f, 45f),
        new BoxSpec("drive_disc_a", 0, 106, 16, 1, 5, 5, -8.5f, -1f, 3.5f, new float[] { 0f, 1.5f, 6f }, 0f, 45f),
        new BoxSpec("drive_disc_b", 0, 90, 26, 1, 5, 5, -2.5f, -1f, 3.5f, new float[] { 0f, 1.5f, 6f }, 0f, 45f),
        new BoxSpec("pin_r", 0, 82, 59, 4, 3, 3, -2f, -6f, -7.5f, new float[] { 0f, -4.5f, -6f }, 0f, 45f),
        new BoxSpec("pin_rear_r", 0, 34, 82, 1, 2, 2, -3f, -5.5f, -7f, new float[] { 0f, -4.5f, -6f }, 0f, 45f),
        new BoxSpec("pin_front_r", 0, 8, 79, 2, 2, 2, 2f, -5.5f, -7f, new float[] { 0f, -4.5f, -6f }, 0f, 45f),
        new BoxSpec("pin_stub_r", 0, 120, 59, 2, 4, 2, -1f, -4.9f, -5.4f, new float[] { 0f, -2.9f, -4.4f }, 0f, 45f),
        new BoxSpec("pin_l", 0, 68, 59, 4, 3, 3, -2f, -6f, 4.5f, new float[] { 0f, -4.5f, 6f }, 0f, 45f),
        new BoxSpec("pin_rear_l", 0, 28, 82, 1, 2, 2, -3f, -5.5f, 5f, new float[] { 0f, -4.5f, 6f }, 0f, 45f),
        new BoxSpec("pin_front_l", 0, 0, 79, 2, 2, 2, 2f, -5.5f, 5f, new float[] { 0f, -4.5f, 6f }, 0f, 45f),
        new BoxSpec("pin_stub_l", 0, 38, 39, 2, 4, 2, -1f, -4.9f, 3.4f, new float[] { 0f, -2.9f, 4.4f }, 0f, -45f),
        // END GENERATED BOXES
    };

    /** The sheet's first-person scale, recorded for Techguns' model. */
    private static final double SHEET_FP_SCALE = 1.5;
    /** On-screen length in first person, blocks of the scaled frame (a long gun, as the bolt-action rifle). */
    private static final double LENGTH = 2.0;
    /** The first-person scale from {@link FirstPersonFit}. */
    public static final double FP_SCALE = FirstPersonFit.scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), LENGTH);
    private static final double K = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE));
    /** Blocks from the origin to the middle of the gun's length: centres the model for the world rows. */
    private static final double CENTRE_X = 7.0 / 16.0;

    /** The render transforms, timing and muzzle flash for the Minigun. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition. It is not listed in {@link ClientGunDefinitions} yet: a later gameplay
     * batch registers the gun.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "minigun",
        BOXES,
        2,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        new String[] { TEXTURE },
        MUZZLE,
        true,
        TRANSFORMS);

    private MinigunProfile() {}

    /**
     * @param p progress through the recoil window, 0 to 1
     * @return the rotor's turn about the barrel axis in degrees: one full turn per window, a straight line in time
     */
    public static double rotorTurnDegrees(double p) {
        return 360.0 * p;
    }

    /**
     * The rotor's term for a fire row: a turn about the barrel axis (the program's Z, because the model's quarter turn
     * about Y sends model X to program -Z), for the rotor part only.
     */
    private static double spin(double p, int part) {
        return part == ROTOR_PART ? -rotorTurnDegrees(p) : 0.0;
    }

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
        // Firing: a very small shudder (roll up to 1.5 degrees, 0.05 block shove, one sine cycle per window), and the
        // rotor turns once. The spin is the last step, so it turns the part about its own axis.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 1.5 * Math.sin(p * 2.0 * Math.PI))
            .translate(
                (p, part) -> x0 + K * 0.05 * Math.sin(p * 2.0 * Math.PI),
                TransformProgram.c(y0),
                TransformProgram.c(z0))
            .rotZ(MinigunProfile::spin)
            .build();
        // Reload (100 ticks): the gun tips 25 degrees and slides up and aside; the rotor does not move.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> 25.0 * sin(p))
            .rotY((p, part) -> -15.0 * yR(p))
            .translate(
                (p, part) -> x0 + K * 0.5 * yR(p),
                (p, part) -> y0 - K * 0.25 * yR(p),
                (p, part) -> z0 + K * 0.25 * yR(p))
            .build();
        // Third person: rotX180, rotY225, a half-size frame, the hand near the yoke (texel 10 forward of the origin).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(0.5)
            .translate(-0.6, -0.1, 0)
            .build();
        // No whole-gun recoil in third person; only the rotor turns.
        TransformProgram tpRecoil = TransformProgram.builder()
            .rotZ(MinigunProfile::spin)
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .build();
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.7)
            .translate(-CENTRE_X, 0, 0)
            .build();
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.5)
            .translate(-CENTRE_X, 0, 0)
            .build();
        // Deviation: the flash offset is solved for this model's muzzle in the fixed first-person frame; additive
        // because the GregGuns sheet is black-backed (docs/assets/MANIFEST.md). Drawn at 1.65 times scale.
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.055,
            0.005,
            0.357,
            1.65,
            new double[] { 0, 0, 0 },
            true,
            0.8);
        return GunTransforms.builder()
            .parts(2)
            .timing(RECOIL_TICKS, 5, 100)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { -0.05, -0.8, 0.95 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
