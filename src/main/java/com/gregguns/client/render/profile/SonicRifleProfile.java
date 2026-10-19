package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Sonic Rifle's model boxes and render transforms, as data. Listed in
 * {@link ClientGunDefinitions#ALL}; the gun definition is {@code SonicRifleDef}.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/sonic_rifle.md}: proportions, silhouettes, palettes), by
 * an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures.
 * The look: a long, dark blue-black rifle 59 texels long and 19 tall. A thumbhole stock (butt plate with two horns, a
 * lighter inset belly, a slanted strut, a grip raked 18 degrees) leads to a receiver whose pale-cyan glow core
 * ({@code core_glow}) is seen through a 4x2 window in each side wall. Over it floats an 18-long carry-handle rail that
 * ramps down 45 degrees onto the barrel. The 16-long barrel is two plates around an open slot, bridged by three
 * collars; each of the two gaps holds a pair of silver diamond-section coil rods. A 7-long flared near-black horn
 * (four side plates tilted 12 degrees, a black slot in its core) ends the gun, and an 8x7x4 magazine with two painted
 * teal gauge stripes hangs under the receiver.
 * The layout lives in {@code tools/asset-gen/gun-paint/sonic_rifle.layout.json}; {@code tools/asset-gen/gun_paint.py
 * pack} assigns the UV origins and writes the generated block below, and {@code gun_paint.py paint} paints the
 * 128x64 sheet from the role map {@code gun-paint/sonic_rifle.json}. The notes list no camo variants.
 *
 * <h2>Frame of reference</h2>
 * X-family: muzzle toward +X, Y growing downward, in texels, as authored. Y = 0 is about the receiver's upper third
 * and the barrel slot sits at Y = -1; X = 0 is the rear face of the receiver's rear block plus 2; Z = 0 is the
 * mid-plane (the gun is mirror-symmetric). One texel is a sixteenth of a block. The model has no moving parts (the
 * whole gun moves as one part).
 *
 * <h2>Glow</h2>
 * The glow core is the box named {@code core_glow} (7x3x6, pale cyan, recessed 0.5 behind each side wall). It is meant
 * to be drawn fullbright. The profile has no emissive flag yet; the name is stable so a later FX layer can find it and
 * pulse it. The painted gauge stripes on the magazine are ordinary paint and are not glow.
 *
 * <p>
 * Spec: gun-sheets.md (timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class SonicRifleProfile {

    /** Texture resource paths of the model sheet: the default only (the notes list no camo). */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/sonic_rifle.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the horn tip (texel 35) on the slot axis (-1). */
    public static final float[] MUZZLE = { 35.0f / 16.0f, -1.0f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("hs_lintel", 0, 36, 11, 8, 1, 7, 2f, -3f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("hs_lip", 0, 66, 18, 8, 1, 7, 2f, 0f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("hs_frame_r", 0, 106, 9, 2, 2, 7, 8f, -2f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("hs_frame_b", 0, 88, 9, 2, 2, 7, 2f, -2f, -3.5f, NO_PIVOT, 0f),
        new BoxSpec("core_glow", 0, 88, 0, 7, 3, 6, 2.5f, -2.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("hs_top1", 0, 0, 34, 8, 1, 6, 2f, -3.5f, -3f, NO_PIVOT, 0f),
        new BoxSpec("hs_top2", 0, 92, 40, 8, 1, 4, 2f, -4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("hs_bot1", 0, 36, 19, 8, 2, 6, 2f, 1f, -3f, NO_PIVOT, 0f),
        new BoxSpec("hs_bot2", 0, 28, 34, 8, 2, 5, 2f, 2.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("rear_block", 0, 20, 11, 4, 5, 4, -2f, -3.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rear_riser", 0, 114, 49, 4, 1, 3, -2f, -4.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rear_post", 0, 66, 57, 2, 1, 2, -1f, -6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("fwd_plate", 0, 78, 10, 1, 4, 4, 1f, 1f, -2f, NO_PIVOT, 0f),
        new BoxSpec("fwd_plate_foot", 0, 120, 53, 1, 1, 3, 1f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("bulkhead", 0, 0, 0, 1, 9, 4, 10f, -4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("bulkhead_foot", 0, 44, 41, 1, 1, 3, 10f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("magazine", 0, 20, 0, 8, 7, 4, 2f, 4.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rail", 0, 8, 57, 18, 1, 2, -0.5f, -6.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("mid_riser_a", 0, 122, 4, 1, 1, 2, 10f, -6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("mid_riser_b", 0, 0, 54, 1, 1, 3, 10f, -5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("bead_rear", 0, 124, 9, 1, 1, 1, -0.4f, -7.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("bead_front", 0, 122, 7, 1, 1, 1, 16.5f, -7.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("ramp", 0, 48, 57, 3, 1, 2, 17.5f, -6.5f, -1f, new float[] { 17.5f, -6.5f, 0f }, 45f),
        new BoxSpec("front_rail", 0, 52, 54, 8, 1, 2, 19f, -4.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("front_cap", 0, 118, 44, 1, 1, 3, 27f, -4.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel_upper", 0, 22, 27, 16, 2, 5, 11f, -3.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel_ridge", 0, 0, 46, 16, 1, 3, 11f, -4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("barrel_lower", 0, 82, 26, 16, 2, 5, 11f, 0f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("under_tube", 0, 54, 40, 16, 3, 3, 11f, 2f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("forend", 0, 54, 34, 10, 2, 4, 14f, 3.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("forend_low", 0, 38, 46, 9, 1, 3, 14.5f, 5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("collar_0", 0, 0, 13, 2, 2, 5, 11f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("collar_1", 0, 22, 20, 2, 2, 5, 18f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("collar_2", 0, 112, 33, 2, 2, 5, 25f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("rod_0l", 0, 64, 50, 5, 2, 2, 13f, -2f, -2.4f, new float[] { 13f, -1f, -1.4f }, 0f, 45f),
        new BoxSpec("rod_0r", 0, 78, 53, 5, 2, 2, 13f, -2f, 0.4f, new float[] { 13f, -1f, 1.4f }, 0f, 45f),
        new BoxSpec("rod_1l", 0, 92, 53, 5, 2, 2, 20f, -2f, -2.4f, new float[] { 20f, -1f, -1.4f }, 0f, 45f),
        new BoxSpec("rod_1r", 0, 106, 53, 5, 2, 2, 20f, -2f, 0.4f, new float[] { 20f, -1f, 1.4f }, 0f, 45f),
        new BoxSpec("flange", 0, 10, 0, 1, 8, 4, 27f, -4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("horn_core", 0, 64, 26, 5, 4, 4, 28f, -3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("horn_cap_top", 0, 98, 49, 5, 1, 3, 28f, -4f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("horn_cap_bot", 0, 82, 49, 5, 1, 3, 28f, 1f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("horn_top", 0, 22, 41, 7, 1, 4, 28f, -4.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("horn_bot", 0, 0, 41, 7, 1, 4, 28f, 1.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("horn_ur", 0, 48, 50, 7, 3, 1, 28f, -4.5f, 1f, new float[] { 28f, -4.5f, 1.5f }, 0f, 12f),
        new BoxSpec("horn_ul", 0, 32, 50, 7, 3, 1, 28f, -4.5f, -2f, new float[] { 28f, -4.5f, -1.5f }, 0f, -12f),
        new BoxSpec("horn_lr", 0, 16, 50, 7, 3, 1, 28f, -0.5f, 1f, new float[] { 28f, 2.5f, 1.5f }, 0f, -12f),
        new BoxSpec("horn_ll", 0, 0, 50, 7, 3, 1, 28f, -0.5f, -2f, new float[] { 28f, 2.5f, -1.5f }, 0f, 12f),
        new BoxSpec("butt", 0, 66, 0, 2, 7, 4, -24f, -0.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("butt_horn_top", 0, 66, 11, 2, 3, 3, -24f, -3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("butt_horn_bot", 0, 10, 28, 2, 2, 3, -24f, 6.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_body", 0, 44, 0, 8, 8, 3, -22f, -3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("stock_belly", 0, 0, 20, 9, 6, 2, -22f, 2.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("top_spine", 0, 8, 54, 20, 1, 2, -22f, -3.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("cheek", 0, 96, 18, 10, 4, 4, -18f, -4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("cheek_cap", 0, 92, 45, 10, 1, 3, -18f, -4.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("mid_body", 0, 82, 33, 12, 4, 3, -14f, -3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("under_plate", 0, 62, 46, 8, 2, 2, -14f, 0.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("strut", 0, 114, 0, 2, 7, 2, -14.8f, 7.54f, -1f, new float[] { -13.8f, 7.54f, 0f }, -66f),
        new BoxSpec("grip", 0, 78, 0, 3, 8, 2, -6f, 3f, -1f, new float[] { -6f, 3f, 0f }, 18f),
        new BoxSpec("grip_block", 0, 0, 28, 3, 4, 2, -6f, 1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("gusset_a", 0, 58, 57, 2, 1, 2, -14.1f, 1.8f, -1f, new float[] { -13.1f, 2.3f, 0f }, -42f),
        new BoxSpec("gusset_b", 0, 82, 46, 3, 1, 2, -8f, 1f, -1f, new float[] { -8f, 2f, 0f }, 28f),
        new BoxSpec("guard", 0, 116, 40, 4, 2, 2, -3f, 1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("guard_post", 0, 122, 0, 1, 2, 2, 0f, 3f, -1f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale and an idle offset measured from the origin of Techguns' model.
    // InHandFit solves the scale and the idle offset from the per-gun targets in InHandTargets (the in-hand notes,
    // section 7), so this model's length, centre and muzzle land where the original's do on screen.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("sonic_rifle", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.2, -0.25 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the Sonic Rifle. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms; the gameplay batch lists it in
     * {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "sonic_rifle",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private SonicRifleProfile() {}

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
        // Firing: the notes give a roll up to -8 degrees and a slide back along the gun axis of up to 0.6 block.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> -8.0 * kick(p))
            .translate((p, part) -> FP_X - 0.6 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: the notes give a roll up to 25 degrees (times 1.5), a yaw up to -15 and a shift up to
        // (0.5, 0.25, 0.25).
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -37.0 * yR(p))
            .rotY((p, part) -> -15.0 * yR(p))
            .translate(
                (p, part) -> FP_X + 0.5 / FP_SCALE * yR(p),
                (p, part) -> FP_Y + 0.25 / FP_SCALE * yR(p),
                (p, part) -> FP_Z + 0.25 / FP_SCALE * yR(p))
            .build();
        // Third person: rotX180, rotY225, scale 1.25 (the notes). The grip (about 0.35 block behind the origin,
        // 0.45 down) lands at the hand.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // The notes: a pitch of about 20 degrees about a point 0.9 block behind and 0.3 up.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -20.0 * kick(p)))
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .build();
        // Ground: scale 0.65 (the notes), the model centred (centre at texel 5.5, 2) so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.65)
            .translate(-0.34, -0.125, 0)
            .build();
        // Icon: side-on, the model centred in the slot; 0.55 is the notes' scale.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.55)
            .translate(-0.34, -0.125, 0)
            .build();
        // Deviation: the notes ask for a sonic-wave flash; the flash offset is solved from this model's muzzle in the
        // fitted frame. Unverified in game.
        double[] fo = FIT.flashOffset(MUZZLE, 1.85);
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            fo[0],
            fo[1],
            fo[2],
            1.85,
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
