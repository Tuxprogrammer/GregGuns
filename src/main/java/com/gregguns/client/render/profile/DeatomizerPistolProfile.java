package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The De-Atomizer Pistol's model boxes and render transforms, as data. This one profile serves both items: the single
 * pistol and the Akimbo pair. The akimbo item draws the same un-mirrored model twice (see below). The gun is not
 * registered yet: it is not in {@link ClientGunDefinitions#ALL} until the gameplay batch adds it, together with its gun
 * definition.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/deatomizer_pistol.md}: proportions, silhouettes,
 * palettes), by an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists
 * or textures. The look: a small pale-gray sci-fi pistol 18.35 texels long and 11.9 tall. Its front half is a "flower"
 * of twelve thin square rods ({@code rod_NN}, each capped by a cube {@code cap_NN}) round a white core ({@code core},
 * {@code core_cap}); in front of the core stands a 2x2 blue emitter disc ({@code disc_glow}) between four white prongs
 * ({@code prong_*}) that are turned 23 degrees in towards the beam. Behind the cluster is a 9-long ribbed housing (a
 * slab, a bottom bar, two flat side strips and four corner strips at 28 degrees), a rear plate, a black grip raked 12
 * degrees back with one light stripe, a skinny open trigger guard with a small trigger, a thin diamond laser rod on top
 * ({@code laser}, held by two paper-thin plates and a post) with an orange front face, and a
 * single diamond button on the left (-Z) side (a 1x1 cube turned 45 degrees about Z). Round 3 applied the measured
 * corrections: the laser post and trigger moved, the muzzle fork closed to 1.4 apart, and the ring caps pulled in. The
 * layout lives in
 * {@code tools/asset-gen/gun-paint/deatomizer_pistol.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns
 * the UV origins and writes the generated block below, and {@code gun_paint.py paint} paints the 64x64 sheet from the
 * role map {@code gun-paint/deatomizer_pistol.json}. There are no camo variants.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, as authored. X = 0 is
 * the rear of the housing (the rear plate's lower edge reaches -0.5), Y = 0 the top of the laser rod and Z = 0 the
 * mid-plane; the bore axis is at Y 3.5. One texel is a sixteenth of a block.
 *
 * <h2>Glow parts</h2>
 * {@code disc_glow} (the blue emitter, #28A8FF) is the one part a renderer may draw fullbright. The laser rod's
 * front face is painted orange (#FF5E1E) on the rod itself; round 3 removed the separate zero-thickness tip plane.
 * The notes call for no other glow.
 *
 * <h2>Akimbo</h2>
 * The notes say the original does not mirror the geometry: the left gun is a second, identical copy moved sideways, so
 * the one asymmetric detail (the button on -Z) is on the same side of both guns. So there is no separate left-handed
 * model. The akimbo renderer needs the same {@link #DEFINITION}, the same texture, a sideways offset of the second copy
 * along the gun's width axis (model Z; about two gun widths apart in first person), and the muzzle flash of the second
 * gun mirrored about the pair's centre (flip the sign of the flash's sideways offset; see {@link AkimboLayout}).
 *
 * <h2>Motion</h2>
 * The model has no moving parts: the whole gun moves as one part (part 0). Every ring rod, cap, the disc and each
 * prong is its own named box, so a later change can move them to separate parts to spin or pulse them.
 *
 * <p>
 * Spec: sheets-energy-special.md "De-Atomizer Pistol" (timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class DeatomizerPistolProfile {

    /** Texture resource paths of the model sheet; there is one (no camo variants). */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/deatomizer_pistol.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /**
     * The muzzle in the gun frame, blocks: x forward, y down, z: the prong tips (texel 17.84) on the bore axis (3.5).
     */
    public static final float[] MUZZLE = { 17.84f / 16.0f, 3.5f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash_blue.png} that are painted: 0 to 7. */
    public static final int MUZZLE_FLASH_FRAMES = 8;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("core", 0, 38, 0, 6, 2, 2, 9f, 2.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("core_cap", 0, 54, 0, 1, 2, 2, 15f, 2.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("disc_glow", 0, 16, 4, 1, 2, 2, 16f, 2.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rod_00", 0, 36, 11, 6, 1, 1, 9f, 3f, 0.8f, NO_PIVOT, 0f),
        new BoxSpec("cap_00", 0, 60, 9, 1, 1, 1, 15f, 3f, 0.8f, NO_PIVOT, 0f),
        new BoxSpec("rod_01", 0, 50, 11, 6, 1, 1, 9f, 2.25f, 0.669f, new float[] { 12f, 2.75f, 1.169f }, 0f, 30f),
        new BoxSpec("cap_01", 0, 34, 17, 1, 1, 1, 15f, 2.25f, 0.669f, new float[] { 15.5f, 2.75f, 1.169f }, 0f, 30f),
        new BoxSpec("rod_02", 0, 0, 13, 6, 1, 1, 9f, 1.85f, 0.175f, new float[] { 12f, 2.35f, 0.675f }, 0f, -30f),
        new BoxSpec("cap_02", 0, 38, 17, 1, 1, 1, 15f, 2.05f, 0.175f, new float[] { 15.5f, 2.55f, 0.675f }, 0f, -30f),
        new BoxSpec("rod_03", 0, 14, 13, 6, 1, 1, 9f, 1.7f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("cap_03", 0, 42, 17, 1, 1, 1, 15f, 1.9f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("rod_04", 0, 28, 13, 6, 1, 1, 9f, 1.85f, -1.175f, new float[] { 12f, 2.35f, -0.675f }, 0f, 30f),
        new BoxSpec("cap_04", 0, 46, 17, 1, 1, 1, 15f, 2.05f, -1.175f, new float[] { 15.5f, 2.55f, -0.675f }, 0f, 30f),
        new BoxSpec("rod_05", 0, 42, 13, 6, 1, 1, 9f, 2.25f, -1.669f, new float[] { 12f, 2.75f, -1.169f }, 0f, -30f),
        new BoxSpec("cap_05", 0, 50, 17, 1, 1, 1, 15f, 2.25f, -1.669f, new float[] { 15.5f, 2.75f, -1.169f }, 0f, -30f),
        new BoxSpec("rod_06", 0, 0, 15, 6, 1, 1, 9f, 3f, -1.8f, NO_PIVOT, 0f),
        new BoxSpec("cap_06", 0, 54, 17, 1, 1, 1, 15f, 3f, -1.8f, NO_PIVOT, 0f),
        new BoxSpec("rod_07", 0, 14, 15, 6, 1, 1, 9f, 3.8f, -1.669f, new float[] { 12f, 4.3f, -1.169f }, 0f, 30f),
        new BoxSpec("cap_07", 0, 58, 17, 1, 1, 1, 15f, 3.75f, -1.669f, new float[] { 15.5f, 4.25f, -1.169f }, 0f, 30f),
        new BoxSpec("rod_08", 0, 28, 15, 6, 1, 1, 9f, 4.15f, -1.175f, new float[] { 12f, 4.65f, -0.675f }, 0f, -30f),
        new BoxSpec("cap_08", 0, 0, 19, 1, 1, 1, 15f, 3.95f, -1.175f, new float[] { 15.5f, 4.45f, -0.675f }, 0f, -30f),
        new BoxSpec("rod_09", 0, 42, 15, 6, 1, 1, 9f, 4.3f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("cap_09", 0, 4, 19, 1, 1, 1, 15f, 4.1f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("rod_10", 0, 0, 17, 6, 1, 1, 9f, 4.15f, 0.175f, new float[] { 12f, 4.65f, 0.675f }, 0f, 30f),
        new BoxSpec("cap_10", 0, 8, 19, 1, 1, 1, 15f, 3.95f, 0.175f, new float[] { 15.5f, 4.45f, 0.675f }, 0f, 30f),
        new BoxSpec("rod_11", 0, 14, 17, 6, 1, 1, 9f, 3.8f, 0.669f, new float[] { 12f, 4.3f, 1.169f }, 0f, -30f),
        new BoxSpec("cap_11", 0, 12, 19, 1, 1, 1, 15f, 3.75f, 0.669f, new float[] { 15.5f, 4.25f, 1.169f }, 0f, -30f),
        new BoxSpec("prong_top", 0, 28, 17, 2, 1, 1, 15.8f, 1.9f, -0.5f, new float[] { 15.8f, 2.4f, 0f }, 23f),
        new BoxSpec("prong_bot", 0, 10, 5, 2, 1, 1, 15.8f, 4.1f, -0.5f, new float[] { 15.8f, 4.6f, 0f }, -23f),
        new BoxSpec("prong_l", 0, 10, 7, 2, 1, 1, 15.8f, 3f, 0.95f, new float[] { 15.8f, 3.5f, 1.45f }, 0f, 0f, 23f),
        new BoxSpec(
            "prong_r",
            0,
            56,
            15,
            2,
            1,
            1,
            15.8f,
            3f,
            -1.95f,
            new float[] { 15.8f, 3.5f, -1.45f },
            0f,
            0f,
            -23f),
        new BoxSpec("slab", 0, 16, 0, 9, 2, 2, 0f, 2f, -1f, NO_PIVOT, 0f),
        new BoxSpec("bar", 0, 22, 4, 9, 1, 2, 0f, 4f, -1f, NO_PIVOT, 0f),
        new BoxSpec("strip_l", 0, 40, 9, 9, 1, 1, 0f, 3f, 0.5f, NO_PIVOT, 0f),
        new BoxSpec("strip_r", 0, 0, 11, 9, 1, 1, 0f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("cs_ul", 0, 0, 9, 9, 1, 1, 0f, 2.3f, 0.3f, new float[] { 4.5f, 2.8f, 0.8f }, 0f, 28f),
        new BoxSpec("cs_ur", 0, 20, 9, 9, 1, 1, 0f, 2.3f, -1.3f, new float[] { 4.5f, 2.8f, -0.8f }, 0f, -28f),
        new BoxSpec("cs_ll", 0, 22, 7, 9, 1, 1, 0f, 3.7f, 0.3f, new float[] { 4.5f, 4.2f, 0.8f }, 0f, -28f),
        new BoxSpec("cs_lr", 0, 42, 7, 9, 1, 1, 0f, 3.7f, -1.3f, new float[] { 4.5f, 4.2f, -0.8f }, 0f, 28f),
        new BoxSpec("rear_plate", 0, 10, 0, 1, 3, 2, 0f, 2f, -1f, new float[] { 0.5f, 2f, 0f }, 10f),
        new BoxSpec("rear_wedge", 0, 44, 4, 2, 1, 2, 0.7f, 4.2f, -1f, new float[] { 0.7f, 4.2f, 0f }, 27f),
        new BoxSpec("grip", 0, 0, 0, 3, 7, 2, 2f, 5f, -1f, new float[] { 5f, 5f, 0f }, 12f),
        new BoxSpec("guard_post", 0, 60, 0, 1, 3, 1, 7f, 5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("guard_bar", 0, 56, 13, 3, 1, 1, 4f, 7f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 56, 4, 1, 2, 1, 4.3f, 4.7f, -0.5f, new float[] { 4.8f, 4.7f, 0f }, -31f),
        new BoxSpec("laser", 0, 20, 11, 7, 1, 1, 2f, 0.2f, -0.5f, new float[] { 5.5f, 0.7f, 0f }, 0f, 45f),
        new BoxSpec("laser_plate_l", 0, 16, 19, 6, 1, 0, 2f, 1f, 0.3f, NO_PIVOT, 0f),
        new BoxSpec("laser_plate_r", 0, 28, 19, 6, 1, 0, 2f, 1f, -0.3f, NO_PIVOT, 0f),
        new BoxSpec("laser_post", 0, 52, 4, 1, 2, 1, 1.4f, 1.1f, -0.5f, new float[] { 1.9f, 2.1f, 0f }, 31f),
        new BoxSpec("button", 0, 60, 4, 1, 1, 1, 4.8f, 3f, -2.1f, new float[] { 5.3f, 3.5f, -1.6f }, 45f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 2.5 and an idle shift measured from the origin of the original
    // model. The scale, idle position and third-person placement are solved by InHandFit from the per-gun targets of
    // the in-hand notes (section 7). The akimbo item draws this same definition twice (AkimboLayout mirrors the left
    // gun about the screen centre, so it sits at the notes' -1.02 right at equal depth).
    private static final InHandFit.Placement FIT = InHandFit
        .fit("deatomizer_pistol", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.05, 0.1 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] CENTER = FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE);
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the De-Atomizer Pistol. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms; the gameplay batch lists it in
     * {@link ClientGunDefinitions}, for the single pistol and for the Akimbo item alike.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "deatomizer_pistol",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private DeatomizerPistolProfile() {}

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
        // Firing: the notes give up to 12 degrees of pitch, peaking mid-animation, with a small kick back.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 12.0 * yF(p))
            .translate((p, part) -> FP_X + 0.2 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: about 35 degrees of pitch at mid-animation, rising and falling smoothly.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> 35.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225 (the notes' yaw 135 in the Z-family, turned to the X-family), then the scale
        // and translate InHandFit solved so the box centre lands at the hand and the pistol is the notes' 0.63 world
        // blocks long. Aimed extra (0, 0.05, 0.1).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil: about -15 degrees of pitch in the first quarter of the first half, about the hand.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0, 0, b -> b.rotZ((p, part) -> -15.0 * kick(p)))
            .build();
        // Reload: about 35 degrees of pitch at the middle of the animation, about the same pivot.
        TransformProgram tpReload = TransformProgram.builder()
            .about(0, 0, 0, b -> b.rotZ((p, part) -> -35.0 * yR(p)))
            .build();
        // Ground: the model centred so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.8)
            .translate(-CENTER[0], -CENTER[1], 0)
            .build();
        // Icon: side-on, the model centred in the slot.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.85)
            .translate(-CENTER[0], -CENTER[1], 0)
            .build();
        // Deviation: the notes call for the alien-blaster flash tinted cyan-green (0.2, 1.0, 0.7). A flash spec has
        // no tint, so the blue energy flash sheet stands in (it is already blue). The first-person offset follows the
        // solved placement (InHandFit.Placement.flashOffset). Headless work cannot look at the screen, so this is
        // unverified in game. Additive because the GregGuns sheet is black-backed (docs/assets/MANIFEST.md).
        double[] fo = FIT.flashOffset(MUZZLE, 1.0);
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash_blue.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            fo[0],
            fo[1],
            fo[2],
            1.0,
            new double[] { 0, 0, 0 },
            true,
            0.7);
        return GunTransforms.builder()
            .parts(1)
            .timing(5, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.05, 0.1 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
