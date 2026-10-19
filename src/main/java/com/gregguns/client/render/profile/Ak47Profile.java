package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.p66;
import static com.gregguns.client.anim.GunCurves.yF;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The AK-47's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/ak47.md}: proportions, silhouettes, palettes), by an
 * agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures. The
 * look: a long, slim two-tone Kalashnikov, 98 texels long and 26 tall but only 4 wide. Walnut-brown wood makes the
 * stepped buttstock and the two-piece handguard; everything else is near-black steel: the receiver with a narrower
 * dust cover and a sloped rear, a thin sight leaf, a slim diamond-section barrel with a gas tube and strut above it
 * and three short collars, a front-sight post with a leaning brace, a raked pistol grip, an open trigger guard with a
 * small trigger, and a banana magazine of two stacked slabs that curves forward. The layout lives in
 * {@code tools/asset-gen/gun-paint/ak47.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns the UV
 * origins and writes the generated block below, and {@code gun_paint.py paint} paints the 128x64 default sheet and
 * the Blue polymer camo from the role map {@code gun-paint/ak47.json}. {@code Ak47ProfileTest} checks the boxes
 * against the layout file and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as authored.
 * Y = 0 is the top line of the dust cover, handguard and gas tube; X = 0 is the back face of the butt plate. One texel
 * is a sixteenth of a block. Slanted pieces (rear slope, grip, magazine slabs, strut, sight brace, trigger) are boxes
 * tilted about Z, and the barrel, collars and gas tube are square bars turned 45 degrees about X, so they read as
 * diamonds. The model has one part and no moving parts, as in the original: all motion is whole-gun transforms.
 *
 * <p>
 * Spec: model-notes/ak47.md sections 6 and 7 (framing, timing); rendering-framework.md section 6.1 (X-family), 8.
 */
public final class Ak47Profile {

    /** Texture resource paths of the model sheet: the default (wood) and Blue (polymer). */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/ak47.png",
        "gregguns:textures/models/guns/ak47_camo1.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down; the barrel tip (texel 98) on its axis (texel 5). */
    public static final float[] MUZZLE = { 98.0f / 16.0f, 5.0f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole rifle moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("stock_slab", 0, 0, 18, 26, 4, 4, 0f, 4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stock_comb", 0, 0, 31, 12, 1, 4, 0f, 3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stock_bump", 0, 116, 27, 2, 1, 4, 24f, 3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stock_step1", 0, 0, 26, 21, 1, 4, 0f, 8f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stock_step2", 0, 78, 27, 15, 1, 4, 0f, 9f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stock_step3", 0, 50, 30, 10, 1, 4, 0f, 10f, -2f, NO_PIVOT, 0f),
        new BoxSpec("stock_step4", 0, 78, 32, 7, 1, 4, 0f, 11f, -2f, NO_PIVOT, 0f),
        new BoxSpec("receiver", 0, 46, 0, 28, 5, 4, 26f, 3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("dust_cover", 0, 60, 18, 23, 3, 3, 29f, 0f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("cover_slope", 0, 60, 24, 4, 3, 3, 25f, 0f, -1.5f, new float[] { 29f, 0f, 0f }, -50f),
        new BoxSpec("sight_block", 0, 82, 9, 7, 3, 4, 52f, 0f, -2f, NO_PIVOT, 0f),
        new BoxSpec("sight_leaf", 0, 110, 7, 7, 1, 1, 51f, 0f, -0.5f, new float[] { 51f, 0f, 0f }, -8f),
        new BoxSpec("hand_lower", 0, 46, 9, 14, 5, 4, 54f, 3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("hand_upper", 0, 104, 9, 9, 3, 3, 59f, 0f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel", 0, 0, 36, 27, 2, 2, 71f, 4f, -1f, new float[] { 71f, 5f, 0f }, 0f, 45f),
        new BoxSpec("collar_rear", 0, 34, 11, 3, 3, 3, 68f, 3.5f, -1.5f, new float[] { 68f, 5f, 0f }, 0f, 45f),
        new BoxSpec("gas_block", 0, 112, 15, 3, 3, 3, 77f, 3.5f, -1.5f, new float[] { 77f, 5f, 0f }, 0f, 45f),
        new BoxSpec("collar_muzzle", 0, 16, 11, 6, 3, 3, 86f, 3.5f, -1.5f, new float[] { 86f, 5f, 0f }, 0f, 45f),
        new BoxSpec("gas_tube", 0, 100, 32, 9, 2, 2, 68f, 0.4f, -1f, new float[] { 68f, 1.4f, 0f }, 0f, 45f),
        new BoxSpec("gas_strut", 0, 32, 31, 5, 2, 2, 75.7f, 0.4f, -1f, new float[] { 75.7f, 1.4f, 0f }, 55f),
        new BoxSpec("sight_post", 0, 114, 0, 1, 6, 1, 90f, -1f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("sight_brace", 0, 110, 0, 1, 6, 1, 89.2f, -1.4f, -0.5f, new float[] { 90.2f, -1.4f, 0f }, 21f),
        new BoxSpec("sight_tip", 0, 104, 15, 2, 1, 1, 89f, -2f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 32, 0, 4, 8, 3, 29f, 11f, -1.5f, new float[] { 33f, 11f, 0f }, 21.3f),
        new BoxSpec("grip_filler", 0, 118, 21, 1, 2, 3, 28f, 8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("trigger_rear", 0, 0, 12, 5, 3, 3, 29f, 8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("guard_floor", 0, 78, 24, 7, 1, 2, 31f, 11f, -1f, NO_PIVOT, 0f),
        new BoxSpec("guard_post", 0, 112, 21, 1, 4, 2, 38f, 8f, -1f, NO_PIVOT, 0f),
        new BoxSpec("well_lip", 0, 118, 0, 2, 3, 3, 39f, 8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 124, 15, 1, 3, 1, 34f, 7.5f, -0.5f, new float[] { 34.5f, 7.5f, 0f }, -15f),
        new BoxSpec("mag_upper", 0, 16, 0, 5, 8, 3, 41f, 7f, -1.5f, new float[] { 43.5f, 7f, 0f }, -9f),
        new BoxSpec("mag_upper_front", 0, 66, 35, 2, 8, 2, 46f, 7f, -1f, new float[] { 43.5f, 7f, 0f }, -9f),
        new BoxSpec("mag_lower", 0, 0, 0, 5, 9, 3, 42.3f, 14.7f, -1.5f, new float[] { 44.8f, 14.7f, 0f }, -20f),
        new BoxSpec("mag_lower_front", 0, 58, 35, 2, 8, 2, 47.3f, 15.1f, -1f, new float[] { 44.8f, 14.7f, 0f }, -20f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first person S(1.4) with an offset measured from Techguns' own origin. The scale
    // follows the FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's precedent;
    // the AK is 6.1 blocks long at model scale), and the idle position puts this model's box centre on the family
    // anchor. The notes' fire and reload offsets are multiplied by K so they move the rifle as far on screen as the
    // notes intend.
    private static final double SHEET_FP_SCALE = 1.4;
    /** On-screen length of the rifle in first person, blocks of the scaled frame. */
    private static final double RIFLE_LENGTH = 2.0;
    private static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), RIFLE_LENGTH);
    private static final double FP_AMP = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE));
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the AK-47. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "ak47",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private Ak47Profile() {}

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
        // Firing (full auto, 5-tick window): a small tight kick, about 5 degrees of pitch and 0.15 block back at peak
        // (yF peaks at -0.5, so the factors are 10 and 0.3).
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 10.0 * yF(p))
            .translate((p, part) -> FP_X + FP_AMP * 0.3 * yF(p), TransformProgram.c(FP_Y), TransformProgram.c(FP_Z))
            .build();
        // Reload: the rifle pitches up about 50 degrees, held through the middle (p66 plateau), so the magazine is
        // exposed, then comes back.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -50.0 * p66(p))
            .translate(
                (p, part) -> FP_X - FP_AMP * 0.1 * p66(p),
                (p, part) -> FP_Y - FP_AMP * 0.1 * p66(p),
                TransformProgram.c(FP_Z))
            .build();
        // Third person: rotX180, rotY225, S(0.6), then T = minus the grip (texel 31, 13), so the pistol grip sits in
        // the hand and the long gun extends forward. Aimed extra translate (0, 0.2, -0.25), as the other long gun.
        // Deviation: the notes' scale 0.7 is for Techguns' frame; 0.6 keeps the 6.1-block rifle about 3.7 blocks
        // long, close to the bolt-action's 4.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(0.6)
            .translate(-1.9, -0.6, 0)
            .build();
        // Recoil about (0, 0.3, -0.9): about 15 degrees of pitch over the first half of the window.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -15.0 * kick(p)))
            .build();
        // Reload: pitch about 30 degrees and yaw about 40 degrees at mid-reload.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * p66(p))
            .rotY((p, part) -> 40.0 * p66(p))
            .build();
        // Ground: scale 0.45 as the notes say, centred on the model's middle (texel 49, 11) so it lies on the ground.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.45)
            .translate(-3.06, -0.45, 0)
            .build();
        // Icon: side-on, muzzle to the right, scaled so the 6.1-block rifle is about 2 blocks wide (the notes' 0.33),
        // centred on the model's middle.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.33)
            .translate(-3.06, -0.6875, 0)
            .build();
        // Muzzle flash: muzzleflash2 sheet at scale 1.5 (the notes), additive because the GregGuns sheet is
        // black-backed. The first-person offset is drawn in a fixed frame, not on the gun. It was carried over from
        // the bolt-action's offset, which was solved on screen for the same anchor and the same 2-block length, by the
        // difference in the two muzzles' projected positions (X +0.036, Z +0.010 at scale 1.85) and then re-expressed
        // for scale 1.5. Headless work cannot look at the screen, so this is unverified in game.
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
            .parts(1)
            .timing(5, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.2, -0.25 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
