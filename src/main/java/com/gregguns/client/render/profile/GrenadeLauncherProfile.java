package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.p66;
import static com.gregguns.client.anim.GunCurves.yF;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Grenade Launcher's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/grenade_launcher.md}: proportions, silhouettes,
 * palette), by an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or
 * textures. The look: a near-black, boxy receiver with a ribbed rail, a tiny rear bump and a two-post fork sight; a
 * thin stock rod ending in a small plate; a pistol grip raked back 20 degrees with a bare guard bar; and in the middle
 * a drum much wider and taller than the body. The drum, the silver launch-tube sleeve and the white-silver end plates
 * are the bright parts. The layout lives in {@code tools/asset-gen/gun-paint/grenade_launcher.layout.json};
 * {@code tools/asset-gen/gun_paint.py pack} assigns the UV origins and writes the generated block below, and
 * {@code gun_paint.py paint} paints {@code grenade_launcher.png} (128x128) from the role map
 * {@code gun-paint/grenade_launcher.json}. {@code GrenadeLauncherProfileTest} checks the boxes against the layout file
 * and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels (the notes are Y-up, so
 * the layout's Y is 13 minus the notes' Y), and drawn as authored. X = 0 is the back face of the stock plate; Y = 0 is
 * the tip of the fork sight; the drum's axis is at X = 26, Y = 13. One texel is a sixteenth of a block.
 *
 * <h2>Approximated shapes</h2>
 * The notes' hexagons (drum, end plates, launch tube, sleeve) are each three rectangles turned 0, 60 and 120 degrees
 * about X ({@link BoxSpec#tiltXDegrees()}): a rectangle as wide as the hexagon's flats and as tall as its side has its
 * corners on the hexagon's vertices, so the union is the hexagon. Their end faces are staggered by 0.02 texel so the
 * coplanar faces do not z-fight. The chamfer on the upper housing is a two-step stair.
 *
 * <h2>Parts</h2>
 * Two parts. Part 0 is the drum (its three rectangles and the fourteen chamber bumps); part 1 is everything else,
 * including the white-silver end plates, so the plates stay still while the drum turns between them. The static body
 * is the last part, which is the one the third-person muzzle flash is drawn in, so the flash does not turn with the
 * drum. On every shot the drum turns 60 degrees (one chamber) about its own axis; the hexagon is six-fold symmetric,
 * so the pose at the end of the window equals the rest pose.
 *
 * <p>
 * Spec: model-notes/grenade_launcher.md sections 6 and 7 (framing, timing, motion); rendering-framework.md section 6.1
 * (X-family) and section 8.
 */
public final class GrenadeLauncherProfile {

    /** Texture resource path of the model sheet (one texture, no camo). */
    public static final String TEXTURE = "gregguns:textures/models/guns/grenade_launcher.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 128;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down; the muzzle plate's front (texel 44) on the tube axis. */
    public static final float[] MUZZLE = { 44.0f / 16.0f, 8.8f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    /** The drum's axis in the gun frame, blocks: x forward (texel 26), y down (texel 13). */
    private static final double DRUM_X = 26.0 / 16.0;
    private static final double DRUM_Y = 13.0 / 16.0;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** Two parts: 0 is the turning drum, 1 is the static body. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("stock_plate", 1, 120, 0, 1, 7, 3, 0f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_buffer", 1, 120, 10, 2, 5, 2, 1f, 5.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("stock_rod", 1, 0, 84, 6, 2, 2, 3f, 6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("collar", 1, 100, 80, 1, 3, 3, 9f, 5.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rec_main", 1, 100, 48, 9, 6, 4, 10f, 5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("hous_rear", 1, 0, 63, 2, 4, 5, 13f, 3f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("hous_front", 1, 100, 37, 4, 6, 5, 15f, 3f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("top_plate", 1, 108, 80, 3, 1, 4, 11f, 4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("underplate", 1, 34, 85, 5, 1, 3, 11.5f, 11f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("shield", 1, 112, 22, 2, 9, 5, 19f, 3f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("bridge", 1, 96, 67, 10, 3, 4, 21f, 4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cover", 1, 0, 72, 9, 1, 5, 21f, 3f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("front_block", 1, 14, 63, 2, 2, 5, 30f, 3f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("rail", 1, 34, 81, 16, 1, 3, 14f, 2f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("sight_rear", 1, 118, 45, 1, 1, 2, 14.5f, 1.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("fork_l", 1, 124, 17, 1, 2, 1, 29f, 0f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("fork_r", 1, 124, 45, 1, 2, 1, 29f, 0f, 0.5f, NO_PIVOT, 0f),
        new BoxSpec("plate_r0", 1, 90, 0, 1, 8, 14, 21f, 9f, -7f, NO_PIVOT, 0f),
        new BoxSpec("plate_r1", 1, 0, 22, 1, 8, 14, 20.98f, 9f, -7f, new float[] { 20.98f, 13f, 0f }, 0f, 60f),
        new BoxSpec("plate_r2", 1, 30, 22, 1, 8, 14, 20.96f, 9f, -7f, new float[] { 20.96f, 13f, 0f }, 0f, 120f),
        new BoxSpec("plate_f0", 1, 0, 0, 1, 8, 14, 30f, 9f, -7f, NO_PIVOT, 0f),
        new BoxSpec("plate_f1", 1, 30, 0, 1, 8, 14, 30.02f, 9f, -7f, new float[] { 30.02f, 13f, 0f }, 0f, 60f),
        new BoxSpec("plate_f2", 1, 60, 0, 1, 8, 14, 30.04f, 9f, -7f, new float[] { 30.04f, 13f, 0f }, 0f, 120f),
        new BoxSpec("drum0", 0, 60, 22, 8, 7, 12, 22f, 9.5f, -6f, NO_PIVOT, 0f),
        new BoxSpec("drum1", 0, 60, 41, 8, 7, 12, 22f, 9.5f, -6f, new float[] { 22f, 13f, 0f }, 0f, 60f),
        new BoxSpec("drum2", 0, 0, 44, 8, 7, 12, 22f, 9.5f, -6f, new float[] { 22f, 13f, 0f }, 0f, 120f),
        new BoxSpec("bf0", 0, 52, 44, 1, 3, 3, 30.75f, 7.3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("bf1", 0, 52, 50, 1, 3, 3, 30.75f, 9.4f, 2.14f, NO_PIVOT, 0f),
        new BoxSpec("bf2", 0, 40, 54, 1, 3, 3, 30.75f, 13.6f, 2.14f, NO_PIVOT, 0f),
        new BoxSpec("bf3", 0, 28, 63, 1, 3, 3, 30.75f, 15.7f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("bf4", 0, 118, 74, 1, 3, 3, 30.75f, 13.6f, -5.14f, NO_PIVOT, 0f),
        new BoxSpec("bf5", 0, 52, 75, 1, 3, 3, 30.75f, 9.4f, -5.14f, NO_PIVOT, 0f),
        new BoxSpec("bfc", 0, 60, 75, 1, 3, 3, 30.5f, 11.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("br0", 0, 68, 75, 1, 3, 3, 20.25f, 7.3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("br1", 0, 76, 75, 1, 3, 3, 20.25f, 9.4f, 2.14f, NO_PIVOT, 0f),
        new BoxSpec("br2", 0, 0, 78, 1, 3, 3, 20.25f, 13.6f, 2.14f, NO_PIVOT, 0f),
        new BoxSpec("br3", 0, 8, 78, 1, 3, 3, 20.25f, 15.7f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("br4", 0, 16, 78, 1, 3, 3, 20.25f, 13.6f, -5.14f, NO_PIVOT, 0f),
        new BoxSpec("br5", 0, 84, 80, 1, 3, 3, 20.25f, 9.4f, -5.14f, NO_PIVOT, 0f),
        new BoxSpec("brc", 0, 92, 80, 1, 3, 3, 20.5f, 11.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("front_plate", 1, 100, 22, 1, 10, 5, 31f, 5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("tube0", 1, 28, 69, 11, 2, 4, 32f, 7.8f, -2f, NO_PIVOT, 0f),
        new BoxSpec("tube1", 1, 58, 69, 11, 2, 4, 32.02f, 7.8f, -2f, new float[] { 32.02f, 8.8f, 0f }, 0f, 60f),
        new BoxSpec("tube2", 1, 88, 74, 11, 2, 4, 32.04f, 7.8f, -2f, new float[] { 32.04f, 8.8f, 0f }, 0f, 120f),
        new BoxSpec("sleeve0", 1, 100, 58, 8, 3, 6, 33f, 7.3f, -3f, NO_PIVOT, 0f),
        new BoxSpec("sleeve1", 1, 40, 60, 8, 3, 6, 33.02f, 7.3f, -3f, new float[] { 33.02f, 8.8f, 0f }, 0f, 60f),
        new BoxSpec("sleeve2", 1, 68, 60, 8, 3, 6, 33.04f, 7.3f, -3f, new float[] { 33.04f, 8.8f, 0f }, 0f, 120f),
        new BoxSpec("muzzle_plate", 1, 40, 44, 1, 5, 5, 43f, 6.3f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("handguard", 1, 28, 75, 8, 2, 4, 32f, 12.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("hg_collar", 1, 24, 81, 1, 1, 4, 39f, 11.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("grip", 1, 118, 36, 3, 7, 2, 12f, 10f, -1f, new float[] { 13.5f, 10f, 0f }, 20f),
        new BoxSpec("trigger", 1, 120, 17, 1, 3, 1, 16.5f, 10.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("guard", 1, 48, 56, 4, 1, 1, 13.5f, 13.5f, -0.5f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first person S(2.0) with an offset measured from Techguns' own origin. The scale
    // follows the FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's precedent), and
    // the idle position puts this model's box centre on the family anchor. The notes' fire and reload offsets are
    // multiplied by K so they move the gun as far on screen as the notes intend.
    private static final double SHEET_FP_SCALE = 2.0;
    /** On-screen length of the launcher in first person, blocks of the scaled frame. */
    private static final double LONG_GUN_LENGTH = 2.0;
    private static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), LONG_GUN_LENGTH);
    private static final double FP_AMP = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE));
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the Grenade Launcher. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "grenade_launcher",
        BOXES,
        2,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        new String[] { TEXTURE },
        MUZZLE,
        true,
        TRANSFORMS);

    private GrenadeLauncherProfile() {}

    /** @return the drum's turn in degrees at progress p of the fire window: 60 on part 0 (eased), 0 on the body */
    static double drumTurn(double p, int part) {
        if (part != 0) {
            return 0.0;
        }
        double q = Math.max(0.0, Math.min(1.0, p));
        return 60.0 * q * q * (3.0 - 2.0 * q);
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
        // Firing: the whole gun rolls up to 10 degrees at mid-window (yF peaks at -0.5, so the factor is 20) and
        // shifts back 0.25 and up 0.1; the drum turns one chamber about its own axis (the last call acts first).
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 20.0 * yF(p))
            .translate(
                (p, part) -> FP_X + FP_AMP * 0.5 * yF(p),
                (p, part) -> FP_Y + FP_AMP * 0.2 * yF(p),
                TransformProgram.c(FP_Z))
            .about(DRUM_X, DRUM_Y, 0, b -> b.rotX(GrenadeLauncherProfile::drumTurn))
            .build();
        // Reload: roll -25 and yaw about -40 degrees (the notes' -25 to -55 net), the gun drifting back and down;
        // the drum does not move.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -25.0 * p66(p))
            .rotY((p, part) -> -40.0 * p66(p))
            .translate(
                (p, part) -> FP_X - FP_AMP * 0.25 * p66(p),
                (p, part) -> FP_Y - FP_AMP * 0.2 * p66(p),
                (p, part) -> FP_Z - FP_AMP * 0.15 * p66(p))
            .build();
        // Third person: rotX180, rotY225, S(1.1), then T = minus the grip (texel 13.5, about 11), so the pistol grip
        // sits in the hand and the launcher extends forward. Aimed extra translate (0, 0.2, -0.25), as the long guns.
        // Deviation: the notes' S(1.3) is for Techguns' frame; 1.1 keeps the 2.75-block launcher about 3 blocks long,
        // between the pistol-size guns and the 3.7-block rifles.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(1.1)
            .translate(-0.85, -0.7, 0)
            .build();
        // Recoil: the gun pitches up in the first half of the window, then settles; the drum turns one chamber.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -25.0 * kick(p)))
            .about(DRUM_X, DRUM_Y, 0, b -> b.rotX(GrenadeLauncherProfile::drumTurn))
            .build();
        // Reload: the notes' 40 degree roll at mid-animation. The drum does not move.
        TransformProgram tpReload = TransformProgram.builder()
            .rotZ((p, part) -> -40.0 * p66(p))
            .build();
        // Ground: scale 0.8 as the notes say, centred on the model's middle (texel 22, 10.5) so it lies on the ground.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.8)
            .translate(-1.375, -0.656, 0)
            .build();
        // Icon: side-on, muzzle to the right, scaled so the 2.75-block launcher is about 2 blocks wide (the notes'
        // 0.7), centred on the model's middle.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.7)
            .translate(-1.375, -0.656, 0)
            .build();
        // Muzzle flash: muzzleflash2 sheet at scale 1.5, additive because the GregGuns sheet is black-backed. The
        // first-person offset is drawn in a fixed frame, not on the gun; it is carried over from the AK-47's, which
        // shares the long-gun family anchor and 2-block length, and is unverified in game (headless work).
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.024,
            0.006,
            0.453,
            1.5,
            new double[] { 0, 0, 0 },
            true,
            0.7);
        return GunTransforms.builder()
            .parts(2)
            .timing(5, 5, 100)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.2, -0.25 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
