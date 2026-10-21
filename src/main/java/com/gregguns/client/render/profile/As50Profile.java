package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.p66;
import static com.gregguns.client.anim.GunCurves.yF;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The AS50 Sniper Rifle's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/as50.md}: proportions, silhouettes, palettes), by an
 * agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures. The
 * look: a very long, near-black anti-materiel rifle, 100 texels long and about 30 tall but only 4 wide at the body.
 * A tall stepped buttpad, a flat stock with a diamond-section cheek rest, a slab receiver with a stepped cap and
 * rail, a big stepped scope floating on two rings and a mount base (eyepiece, tube, front tube, fat objective bell,
 * an elevation turret on top and a windage turret on the right side only), a fat diamond shroud with a floating
 * ribbed front rail and a sloped strut, a thin diamond barrel with a gas tube and two collars on flat feet, a
 * three-baffle side-vented muzzle brake, a raked pistol grip with a closed trigger guard, and a long, shallow, pale
 * box magazine in front of the trigger. The layout lives in {@code tools/asset-gen/gun-paint/as50.layout.json};
 * {@code tools/asset-gen/gun_paint.py pack} assigns the UV origins and writes the generated block below, and
 * {@code gun_paint.py paint} paints the 256x128 sheet from the role map {@code gun-paint/as50.json}. The gun has one
 * skin and no camo variants. {@code As50ProfileTest} checks the boxes against the layout file and the notes' key
 * facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as authored.
 * Y = 0 is the top of the scope's elevation turret; X = 0 is the back face of the buttpad; Z = 0 is the mid-plane (-Z
 * is the shooter's right). One texel is a sixteenth of a block. Slanted pieces (grip, magazine holder and floor plate,
 * trigger, front strut, gas brace, windage turret) are boxes tilted about Z; the cheek rest, scope tubes, shroud,
 * barrel, collars and brake are square bars turned 45 degrees about X, so they read as diamonds; the elevation
 * turret is turned 45 degrees about Y. One part, no moving parts: all motion is whole-gun transforms.
 *
 * <h2>Scope</h2>
 * The rifle always has its scope: while zoomed the renderer hides the model and draws the overlay
 * {@link #SCOPE_TEXTURE} (rendering-framework.md section 9.2).
 *
 * <p>
 * Spec: model-notes/as50.md sections 6 and 7 (framing, timing); rendering-framework.md section 6.1 (X-family), 8, 9.
 */
public final class As50Profile {

    /** Texture resource paths of the model sheet: the only skin. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/as50.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Scope overlay texture. */
    public static final String SCOPE_TEXTURE = "gregguns:textures/fx/testscope.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 256;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 128;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down; the end cap's front face (texel 100) on the axis. */
    public static final float[] MUZZLE = { 100.0f / 16.0f, 13.9f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole rifle moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("pad_rear", 0, 0, 0, 1, 10, 5, 0f, 12.5f, -2.5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("pad_front", 0, 12, 0, 1, 10, 4, 1f, 12.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stock_body", 0, 156, 0, 14, 5, 4, 2f, 15.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cheek_rest", 0, 192, 0, 16, 4, 4, 0f, 10.33f, -2f, new float[] { 0f, 12.33f, 0f }, 0f, 45f),
        new BoxSpec("cheek_joiner", 0, 202, 21, 14, 2, 1, 2f, 13.8f, -0.5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("underplate", 0, 110, 16, 10, 2, 3, 2f, 20.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("receiver", 0, 36, 0, 32, 6, 4, 16f, 11.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("receiver_cap", 0, 34, 17, 32, 1, 3, 16f, 10.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rail", 0, 0, 21, 25, 2, 2, 22f, 8.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("trigger_housing", 0, 36, 10, 17, 3, 4, 16f, 17.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("keel", 0, 54, 21, 13, 1, 3, 18f, 20.5f, -1.5f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("grip", 0, 22, 0, 4, 9, 3, 23f, 21.5f, -1.5f, new float[] { 27f, 21.5f, 0f }, 26f),
        new BoxSpec("guard_floor", 0, 234, 20, 6, 1, 2, 25f, 23.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("guard_post", 0, 234, 16, 1, 2, 2, 30f, 21.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 240, 23, 1, 2, 1, 27f, 21f, -0.5f, new float[] { 27.5f, 21f, 0f }, -15f),
        new BoxSpec("mag_holder", 0, 78, 10, 13, 4, 3, 32f, 15.5f, -1.5f, new float[] { 45f, 15.5f, 0f }, -5f),
        new BoxSpec("mag_body", 0, 108, 0, 12, 8, 2, 32.5f, 19.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec(
            "mag_floor",
            0,
            118,
            21,
            11,
            2,
            2,
            33f,
            25.5f,
            -1f,
            new float[] { 44f, 25.5f, 0f },
            -5f,
            0f,
            0f,
            -0.01f),
        new BoxSpec("mag_tab", 0, 240, 16, 1, 2, 2, 32.5f, 26.5f, -1f, NO_PIVOT, 0f, 0f, 0f, -0.02f),
        new BoxSpec("scope_eyepiece", 0, 232, 0, 7, 4, 4, 20f, 2.1f, -2f, new float[] { 20f, 4.1f, 0f }, 0f, 45f),
        new BoxSpec("scope_tube", 0, 110, 10, 14, 3, 3, 27f, 2.6f, -1.5f, new float[] { 27f, 4.1f, 0f }, 0f, 45f),
        new BoxSpec("scope_cube", 0, 156, 9, 4, 4, 4, 32f, 2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("scope_front", 0, 228, 8, 5, 4, 4, 41f, 2.1f, -2f, new float[] { 41f, 4.1f, 0f }, 0f, 45f),
        new BoxSpec("scope_bell", 0, 136, 0, 5, 5, 5, 46f, 1.6f, -2.5f, new float[] { 46f, 4.1f, 0f }, 0f, 45f),
        new BoxSpec("turret_elev", 0, 0, 15, 3, 2, 3, 32.5f, 0f, -1.5f, new float[] { 34f, 0f, 0f }, 0f, 0f, 45f),
        new BoxSpec("turret_wind", 0, 136, 16, 3, 3, 2, 32.5f, 2.6f, -3.8f, new float[] { 34f, 4.1f, 0f }, 45f),
        new BoxSpec("ring_rear", 0, 246, 14, 1, 3, 3, 29f, 4.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("ring_front", 0, 12, 14, 1, 3, 3, 38f, 4.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("mount_base", 0, 144, 21, 10, 1, 3, 29f, 7.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("shroud", 0, 192, 8, 14, 4, 4, 48f, 11.85f, -2f, new float[] { 48f, 13.85f, 0f }, 0f, 45f),
        new BoxSpec("shroud_spine", 0, 86, 21, 14, 2, 2, 48f, 10.5f, -1f, NO_PIVOT, 0f, 0f, 0f, -0.01f),
        new BoxSpec("front_rail", 0, 170, 21, 14, 1, 2, 47f, 8.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rib_1", 0, 186, 14, 1, 1, 2, 49f, 9.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rib_2", 0, 104, 17, 1, 1, 2, 52f, 9.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rib_3", 0, 28, 18, 1, 1, 2, 55f, 9.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rib_4", 0, 250, 20, 1, 1, 2, 58f, 9.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec(
            "front_strut",
            0,
            186,
            9,
            1,
            3,
            2,
            60.05f,
            8.65f,
            -1f,
            new float[] { 60.55f, 8.65f, 0f },
            -18f,
            0f,
            0f,
            -0.02f),
        new BoxSpec("gas_tube", 0, 170, 24, 19, 1, 1, 62f, 10.7f, -0.5f, new float[] { 62f, 11.2f, 0f }, 0f, 45f),
        new BoxSpec("gas_brace", 0, 146, 16, 2, 3, 2, 79.2f, 11.1f, -1f, new float[] { 80.2f, 11.1f, 0f }, -38f),
        new BoxSpec("barrel", 0, 154, 17, 38, 2, 2, 54f, 12.9f, -1f, new float[] { 54f, 13.9f, 0f }, 0f, 45f),
        new BoxSpec("collar_mid", 0, 22, 12, 3, 3, 3, 80f, 12.4f, -1.5f, new float[] { 80f, 13.9f, 0f }, 0f, 45f),
        new BoxSpec("foot_mid", 0, 232, 23, 2, 1, 2, 80.5f, 15f, -1f, NO_PIVOT, 0f),
        new BoxSpec("collar_front", 0, 144, 10, 3, 3, 3, 92f, 12.4f, -1.5f, new float[] { 92f, 13.9f, 0f }, 0f, 45f),
        new BoxSpec("foot_front", 0, 20, 18, 2, 1, 2, 92.5f, 15f, -1f, NO_PIVOT, 0f),
        new BoxSpec("brake_slab", 0, 172, 9, 3, 4, 4, 95f, 11.9f, -2f, new float[] { 95f, 13.9f, 0f }, 0f, 45f),
        new BoxSpec("brake_post_top", 0, 248, 23, 1, 1, 1, 98f, 12f, -0.5f, new float[] { 98f, 12.5f, 0f }, 0f, 45f),
        new BoxSpec(
            "brake_post_bottom",
            0,
            244,
            23,
            1,
            1,
            1,
            98f,
            14.8f,
            -0.5f,
            new float[] { 98f, 15.3f, 0f },
            0f,
            45f),
        new BoxSpec("brake_cap", 0, 246, 8, 1, 3, 3, 99f, 12.4f, -1.5f, new float[] { 99f, 13.9f, 0f }, 0f, 45f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first person S(1.4) with an offset measured from Techguns' own origin. The scale
    // follows the FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's precedent; the
    // AS50 is 6.25 blocks long at model scale), and the idle position puts this model's box centre on the family
    // anchor. The notes' fire and reload offsets are multiplied by K so they move the rifle as far on screen as the
    // notes intend.
    private static final double SHEET_FP_SCALE = 1.4;
    private static final InHandFit.Placement FIT = InHandFit
        .fit("as50", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.2, -0.25 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double FP_AMP = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing, scope and muzzle flash for the AS50. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "as50",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private As50Profile() {}

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
        // Firing (10-tick window): a quiet, heavy recoil, up to 6 degrees of pitch and a 0.25 push back at mid-window
        // (yF peaks at -0.5, so the factors are 12 and 0.5).
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 12.0 * yF(p))
            .translate((p, part) -> FP_X + FP_AMP * 0.5 * yF(p), TransformProgram.c(FP_Y), TransformProgram.c(FP_Z))
            .build();
        // Reload: the rifle tilts up to 30 degrees about the cross axis and 15 about the vertical, held through the
        // middle (p66 plateau), and rises 0.5 as it tilts, so the magazine comes up toward the eye.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -30.0 * p66(p))
            .rotY((p, part) -> 15.0 * p66(p))
            .translate(TransformProgram.c(FP_X), (p, part) -> FP_Y - FP_AMP * 0.5 * p66(p), TransformProgram.c(FP_Z))
            .build();
        // Third person: rotX180, rotY225, S(0.7) (the notes' scale: about 4.4 blocks long), then T = minus the grip,
        // so the pistol grip sits in the hand and the rifle extends forward. Aimed extra translate (0, 0.2, -0.25).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil about (0, 0.3, -0.9): the gun pitches up about 15 degrees and settles.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -15.0 * kick(p)))
            .build();
        // Reload: pitch about 30 degrees and yaw about 40 degrees at mid-reload.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * p66(p))
            .rotY((p, part) -> 40.0 * p66(p))
            .build();
        // Ground: lies on its side, scaled to 0.45 so the 6.25-block rifle is about 2.8 blocks long, centred on the
        // model's middle (texel 50, 15).
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.45)
            .translate(-3.125, -0.9, 0)
            .build();
        // Icon: side-on, muzzle to the right, scale 0.33 as the notes say, centred on the model's middle.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.33)
            .translate(-3.125, -0.925, 0)
            .build();
        // Muzzle flash: muzzleflash2 sheet at scale 2.0 (the notes: the largest of the rifles), additive because the
        // GregGuns sheet is black-backed. The first-person offset is drawn in a fixed frame, not on the gun. It was
        // carried over from the AK-47's, which was solved for the same anchor and the same 2-block length, by the
        // difference in the two muzzles' heights above the box centre and re-expressed for scale 2.0. Headless work
        // cannot look at the screen, so this is unverified in game.
        double[] fo = FIT.flashOffset(MUZZLE, 2.0);
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            fo[0],
            fo[1],
            fo[2],
            2.0,
            new double[] { 0, 0, 0 },
            true,
            0.7);
        return GunTransforms.builder()
            .parts(1)
            .timing(10, 5, 80)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.2, -0.25 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .scope(new ScopeSpec(SCOPE_TEXTURE, 0.04, 0.02))
            .build();
    }
}
