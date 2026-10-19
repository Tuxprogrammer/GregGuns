package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.p66;
import static com.gregguns.client.anim.GunCurves.yF;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The LMG's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/lmg.md}: proportions, silhouettes, palettes), by an
 * agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures. The
 * look: a belt-fed light machine gun in near-black gunmetal, 73 texels long and 23.5 tall. A long slab receiver with a
 * flat top cover and a striped rail; a thin diamond barrel with a shorter gas tube beneath it and a fat diamond collar
 * where they leave the body; a leaning A-frame front sight; a vertical diamond foregrip under the handguard rail; a
 * raked pistol grip with an open trigger guard; a short, deep, dark green ammo box under the middle, 13 across with a
 * 45 degree shoulder on its right; a column of six brass belt rounds climbing its left side; a stepped two-plate stock
 * that narrows toward the butt; and a boxed red-dot sight with a roof on top, the highest point. The layout lives in
 * {@code tools/asset-gen/gun-paint/lmg.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns the UV origins
 * and writes the generated block below, and {@code gun_paint.py paint} paints the 128x128 sheet from the role map
 * {@code gun-paint/lmg.json}. {@code LmgProfileTest} checks the boxes against the layout file and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the muzzle points toward +X, Y grows downward and +Z is the shooter's left (the belt side), in texels (one
 * texel is a sixteenth of a block). Y = 0 is the top face of the main top cover and X = 0 the rear face of the butt;
 * the muzzle tip is at X = 73. Slanted pieces (grip, trigger, stock tube, front sight strut) are boxes tilted about Z;
 * the barrel, gas tube, collar and belt rounds are square bars turned 45 degrees about X so they read as diamonds; the
 * foregrip is turned 45 degrees about Y; the stock plates are yawed 6 degrees about Y; the ammo box's shoulder is a
 * slab
 * turned 45 degrees about X.
 *
 * <h2>Parts</h2>
 * Seven parts. Part 0 is the gun. Parts 1 to 6 are the six belt rounds, bottom to top, so the part-visibility rule can
 * hide them as the magazine empties ({@link #beltRoundVisible}).
 *
 * <p>
 * Spec: model-notes/lmg.md sections 6 and 7 (framing, timing, belt visibility); rendering-framework.md section 6.1
 * (X-family), 8.
 */
public final class LmgProfile {

    /** Texture resource paths of the model sheet. The LMG has no camo skins. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/lmg.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 128;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down; the barrel tip (texel 73) on its axis (texel 2.9). */
    public static final float[] MUZZLE = { 73.0f / 16.0f, 2.9f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;
    /** Total parts: the gun and six belt rounds. */
    public static final int PARTS = 7;
    /** The number of belt rounds drawn on the gun. */
    public static final int BELT_ROUNDS = 6;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** Seven parts: the gun, then the belt rounds from the bottom one up. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("cover_rear", 0, 70, 31, 20, 1, 4, 17f, 0f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cover_fwd", 0, 40, 41, 16, 1, 3, 37f, 0f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rail", 0, 0, 44, 17, 1, 2, 17.5f, -1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("shoulder", 0, 70, 36, 18, 1, 4, 37f, 1f, -2f, NO_PIVOT, 0f),
        new BoxSpec("body_main", 0, 0, 19, 21, 5, 5, 16f, 1f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("body_fwd", 0, 72, 12, 18, 6, 5, 37f, 2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("body_low", 0, 52, 23, 10, 3, 5, 27f, 6f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("block_rear", 0, 26, 29, 8, 3, 3, 19f, 6f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("plug", 0, 116, 6, 2, 2, 3, 17f, 6f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel", 0, 0, 40, 18, 2, 2, 55f, 1.9f, -1f, new float[] { 55f, 2.9f, 0f }, 0f, 45f),
        new BoxSpec("gas_tube", 0, 48, 37, 8, 2, 2, 57f, 4.9f, -1f, new float[] { 57f, 5.9f, 0f }, 0f, 45f),
        new BoxSpec("collar", 0, 116, 0, 2, 3, 3, 55f, 4.4f, -1.5f, new float[] { 55f, 5.9f, 0f }, 0f, 45f),
        new BoxSpec("sight_base", 0, 108, 28, 4, 2, 1, 57f, 3f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("sight_post", 0, 122, 20, 1, 3, 1, 60f, 0f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("sight_cap", 0, 122, 24, 2, 2, 1, 61f, -2f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("sight_strut", 0, 124, 11, 1, 4, 1, 60f, -1f, -0.5f, new float[] { 60.5f, -1f, 0f }, 25f),
        new BoxSpec("underrail", 0, 38, 45, 12, 1, 2, 37f, 8f, -1f, NO_PIVOT, 0f),
        new BoxSpec("fg_mount", 0, 108, 23, 4, 2, 3, 41.5f, 8.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("foregrip", 0, 108, 0, 2, 8, 2, 42.5f, 10.5f, -1f, new float[] { 43.5f, 14.5f, 0f }, 0f, 0f, 45f),
        new BoxSpec("grip", 0, 72, 0, 4, 9, 3, 19f, 9f, -1.5f, new float[] { 23f, 9f, 0f }, 25.6f),
        new BoxSpec("guard_post", 0, 118, 11, 1, 3, 2, 26f, 9f, -1f, NO_PIVOT, 0f),
        new BoxSpec("guard_floor", 0, 114, 36, 5, 1, 2, 21f, 11f, -1f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 118, 32, 1, 2, 1, 23f, 8.5f, -0.5f, new float[] { 23.5f, 8.5f, 0f }, -15f),
        new BoxSpec("box_low", 0, 0, 0, 7, 6, 13, 28f, 12f, -7f, NO_PIVOT, 0f),
        new BoxSpec("box_up", 0, 40, 0, 7, 4, 9, 28f, 8f, -3f, NO_PIVOT, 0f),
        new BoxSpec(
            "box_chamfer",
            0,
            52,
            13,
            7,
            6,
            3,
            28f,
            8.06f,
            -5.44f,
            new float[] { 28f, 11.06f, -3.94f },
            0f,
            -45f),
        new BoxSpec("round_1", 1, 86, 10, 6, 1, 1, 28.5f, 7.7f, 4.5f, new float[] { 28.5f, 8.2f, 5f }, 0f, 45f),
        new BoxSpec("round_2", 2, 100, 10, 6, 1, 1, 28.5f, 6.7f, 4f, new float[] { 28.5f, 7.2f, 4.5f }, 0f, 45f),
        new BoxSpec("round_3", 3, 66, 45, 6, 1, 1, 28.5f, 5.7f, 4f, new float[] { 28.5f, 6.2f, 4.5f }, 0f, 45f),
        new BoxSpec("round_4", 4, 80, 45, 6, 1, 1, 28.5f, 4.7f, 4f, new float[] { 28.5f, 5.2f, 4.5f }, 0f, 45f),
        new BoxSpec("round_5", 5, 94, 45, 6, 1, 1, 28.5f, 3.7f, 3.5f, new float[] { 28.5f, 4.2f, 4f }, 0f, 45f),
        new BoxSpec("round_6", 6, 108, 45, 6, 1, 1, 28.5f, 3.2f, 2.5f, new float[] { 28.5f, 3.7f, 3f }, 0f, 45f),
        new BoxSpec("butt", 0, 86, 0, 8, 7, 3, 0f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("stair_1", 0, 78, 41, 7, 1, 3, 0f, 2f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("stair_2", 0, 98, 41, 6, 1, 3, 0f, 1f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("heel", 0, 118, 28, 1, 1, 3, 8f, 9f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_tube", 0, 48, 31, 8, 3, 3, 10f, 5f, -1.5f, new float[] { 18f, 5f, 0f }, 10f),
        new BoxSpec("plate_left", 0, 82, 23, 10, 5, 3, 6f, 2f, -0.5f, new float[] { 16f, 4.5f, 1f }, 0f, 0f, -6f),
        new BoxSpec("plate_right", 0, 0, 29, 10, 5, 3, 6f, 2f, -2.5f, new float[] { 16f, 4.5f, -1f }, 0f, 0f, 6f),
        new BoxSpec("rd_base", 0, 26, 35, 8, 2, 3, 21.5f, -2f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rd_cheek_l", 0, 0, 37, 5, 2, 1, 22.5f, -2f, 1f, NO_PIVOT, 0f),
        new BoxSpec("rd_cheek_r", 0, 12, 37, 5, 2, 1, 22.5f, -2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rd_post_l", 0, 40, 13, 4, 3, 1, 22.5f, -5f, 1f, NO_PIVOT, 0f),
        new BoxSpec("rd_post_r", 0, 118, 16, 4, 3, 1, 22.5f, -5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rd_roof", 0, 116, 39, 4, 1, 2, 22.5f, -5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rd_window", 0, 118, 20, 0, 1, 2, 23.5f, -3.5f, -1f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first person S(1.4) with an offset measured from Techguns' own origin. The scale
    // follows the FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's precedent),
    // and the idle position puts this model's box centre on the family anchor. The notes' fire and reload offsets are
    // multiplied by K so they move the gun as far on screen as the notes intend.
    private static final double SHEET_FP_SCALE = 1.4;
    private static final InHandFit.Placement FIT = InHandFit
        .fit("lmg", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.2, -0.25 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double FP_AMP = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    private static final double[] CENTRE = FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE);
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the LMG. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, texture and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "lmg",
        BOXES,
        PARTS,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private LmgProfile() {}

    /**
     * Whether a part is drawn. Belt round {@code k} (part {@code k}, counted from the bottom, 1 to 6) shows while more
     * than {@code k} rounds are loaded, so the top rounds vanish first as the magazine runs down and all six show above
     * six rounds. All six are hidden in the first half of a reload and come back in the second half under the same
     * rule. The gun body (part 0) is always drawn.
     *
     * <p>
     * Deviation: the notes' sentence orders the thresholds "top round with more than 1 loaded" while their checklist
     * says "the top rounds disappear as ammo drops". The two read opposite ways round; the checklist is followed, as it
     * is the intended look (the belt is eaten from the top, shortest when nearly empty).
     *
     * @param part           the part index
     * @param ammoLeft       rounds loaded
     * @param reloadProgress reload progress 0 to 1, or above 1 when not reloading
     * @return true if the part is drawn
     */
    public static boolean beltRoundVisible(int part, int ammoLeft, float reloadProgress) {
        if (part < 1 || part > BELT_ROUNDS) {
            return true;
        }
        return reloadProgress >= 0.5f && ammoLeft > part;
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
        // Firing (full auto, 5-tick window): a mild kick, about 5 degrees of pitch and 0.15 block back at peak (yF
        // peaks at -0.5, so the factors are 10 and 0.3).
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 10.0 * yF(p))
            .translate((p, part) -> FP_X + FP_AMP * 0.3 * yF(p), TransformProgram.c(FP_Y), TransformProgram.c(FP_Z))
            .build();
        // Reload: the gun lifts 55 degrees, ramping in over the first 30 percent, held to 70 percent, ramping out (the
        // p66 plateau), with no sideways movement.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -55.0 * p66(p))
            .translate(TransformProgram.c(FP_X), TransformProgram.c(FP_Y), TransformProgram.c(FP_Z))
            .build();
        // Third person: rotX180, rotY225, S(0.8), then T = minus the grip (texel 21, 13), so the pistol grip sits in
        // the hand and the gun extends forward. Aimed extra translate (0, 0.2, -0.25), as the other long guns.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil about (0, 0.3, -0.9): about 15 degrees of pitch over the first half of the window.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -15.0 * kick(p)))
            .build();
        // Reload: pitch 30 degrees and yaw -40 degrees at mid-reload.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * p66(p))
            .rotY((p, part) -> -40.0 * p66(p))
            .build();
        // Ground: scale 0.6 as the notes say, centred on the model's middle so it lies on the ground.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.6)
            .translate(-CENTRE[0], -CENTRE[1], 0)
            .build();
        // Icon: side-on, muzzle to the right, scale 0.48 as the notes say, centred on the model's middle.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.48)
            .translate(-CENTRE[0], -CENTRE[1], 0)
            .build();
        // Muzzle flash: muzzleflash2 sheet at scale 1.5 (the notes), additive because the GregGuns sheet is
        // black-backed. The first-person offset is drawn in a fixed frame, not on the gun, so it is the AK-47's: both
        // guns are fitted to 2 blocks on screen about the same anchor with the muzzle at the front end (the muzzle
        // height differs by under 0.02 block). Headless work cannot look at the screen, so this is unverified in game.
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
            .parts(PARTS)
            .timing(5, 5, 100)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.2, -0.25 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .partVisibility(LmgProfile::beltRoundVisible)
            .build();
    }
}
