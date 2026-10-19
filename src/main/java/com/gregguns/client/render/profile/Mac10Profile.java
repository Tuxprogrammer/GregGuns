package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Mac-10's model boxes and render transforms, as data. The Akimbo Mac-10 reuses this one standalone model.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, designed in {@code tools/asset-gen/gun-paint/mac10.layout.json} from the
 * written model notes ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/mac10.md}) alone: a bread-box
 * receiver with a 3-unit diamond barrel stub, a vertical grip under the rear third with a long pale magazine, a small
 * open trigger guard, tiny sight furniture and an open wire stock. The texture
 * {@code textures/models/guns/mac10.png} is painted by {@code tools/asset-gen/gun_paint.py} from the same layout.
 *
 * <h2>Frame of reference</h2>
 * One texel is 1/32 block here (half the pistol's), so the notes' half-unit details (0.6 wire, 1.4 magazine) are
 * representable. The boxes are laid out with the muzzle toward +X and Y growing downward (model space), the barrel tip
 * 2 texels ahead of the model origin as the pistol's is, so the third-person wrist pivot lands on the grip. The model
 * class turns that into the Z-family convention with a quarter turn about Y.
 *
 * <p>
 * Round 2: the magazine is 1.5 wide and 2.5 long (the box pipeline has whole-texel sizes, so the notes' 1.4 and 2.4
 * are not reachable) but 6.4 tall visible: its top is buried 0.1 unit in the grip so the bottom is at Y 17.4. The
 * backstrap plates and trigger are hinged on the planes the round 2 notes give, with 1.0-thick plates (not 0.8).
 *
 * <p>
 * Deviation: the notes' vertical wire posts and the sight post are diamond-section; the box pipeline can only turn a
 * bar about its long axis, so those are plain square sections and only the barrel is a diamond.
 *
 * <p>
 * Spec: gun-models.md "ModelMac10"; rendering-framework.md section 6.1 (Z-family) and section 8 (texture size).
 */
public final class Mac10Profile {

    /** Texture resource path of the model sheet. */
    public static final String TEXTURE = "gregguns:textures/models/guns/mac10.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 64;
    /** GL units per texel: 1/32 block, so the 44-texel model is 22 sixteenths long, as the notes. */
    public static final float MODEL_SCALE = 1.0f / 32.0f;
    /**
     * The muzzle in the gun frame (after the model's own quarter turn), blocks: x, y down, z. The barrel tip is 2
     * texels ahead of the model origin and its axis 4 texels below the receiver's top.
     */
    public static final float[] MUZZLE = { 0.0f, 4.0f / 32.0f, -2.0f / 32.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6 (frame 7 is empty). */
    public static final int MUZZLE_FLASH_FRAMES = 7;
    /** On-screen length of the Mac-10 in first person, blocks: a little longer than a pistol, still a one-hand gun. */
    public static final double FP_LENGTH = 1.1;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** Every box is part 0: the notes give the model no moving parts. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("receiver", 0, 36, 0, 34, 8, 6, -38f, 0f, -3f, NO_PIVOT, 0f),
        new BoxSpec("barrel", 0, 0, 18, 12, 2, 2, -10f, 3f, -1f, new float[] { -10f, 4f, 0f }, 0f, 45f),
        new BoxSpec("sight_post", 0, 28, 16, 2, 2, 2, -17.8f, -1.4f, -1f, NO_PIVOT, 0f),
        new BoxSpec("front_blade", 0, 112, 18, 4, 1, 1, -8.4f, -1f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("ear_l", 0, 20, 16, 2, 2, 0, -7.4f, -2f, 2f, NO_PIVOT, 0f),
        new BoxSpec("ear_r", 0, 24, 16, 2, 2, 0, -7.4f, -2f, -2f, NO_PIVOT, 0f),
        new BoxSpec("guard_post", 0, 72, 14, 2, 6, 2, -18f, 8f, -1f, NO_PIVOT, 0f),
        new BoxSpec("guard_bar", 0, 112, 14, 6, 2, 2, -24f, 12f, -1f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 104, 14, 2, 4, 2, -25f, 7f, -1f, new float[] { -23f, 7f, 0f }, -23.4f),
        new BoxSpec("grip", 0, 0, 0, 6, 14, 4, -30f, 8f, -2f, NO_PIVOT, 0f),
        new BoxSpec("strap_up", 0, 60, 14, 2, 6, 4, -31.8f, 7f, -2f, new float[] { -29.8f, 7f, 0f }, -12.5f),
        new BoxSpec("strap_low", 0, 116, 0, 2, 10, 4, -30.4f, 12.4f, -2f, new float[] { -28.4f, 12.4f, 0f }, 7.4f),
        new BoxSpec("magazine", 0, 20, 0, 5, 13, 3, -29.5f, 21.8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("post_l", 0, 36, 14, 1, 10, 1, -41.5f, -2f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("post_r", 0, 40, 14, 1, 10, 1, -41.5f, -2f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("rod_l", 0, 80, 20, 11, 1, 1, -41f, -1f, 1.5f, NO_PIVOT, 0f),
        new BoxSpec("rod_r", 0, 104, 20, 11, 1, 1, -41f, -1f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("cross_bar", 0, 92, 14, 1, 1, 5, -31.3f, -1f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("butt", 0, 44, 14, 2, 4, 6, -42f, 6f, -3f, NO_PIVOT, 0f),
        new BoxSpec("connector", 0, 80, 14, 2, 2, 4, -40f, 6f, -2f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    /** Where the box centre of a Z-family handgun sits in the scaled gun frame (the pistol's tuned place). */
    private static final double[] ANCHOR = { -1.2, -0.684, -1.4275 };
    /** First-person scale from {@link FirstPersonFit}. */
    public static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), FP_LENGTH);
    private static final double[] IDLE = FirstPersonFit.idleOffset(ANCHOR, FP_SCALE, gunFrameCentre());

    /** The notes' render transforms, timing and muzzle flash for the Mac-10. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /** The client half of the gun's definition, listed in {@link ClientGunDefinitions}. */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "mac10",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        new String[] { TEXTURE },
        MUZZLE,
        false,
        TRANSFORMS);

    private Mac10Profile() {}

    /** The box centre in the gun frame: the model's quarter turn about Y sends (x, y, z) to (z, y, -x). */
    private static double[] gunFrameCentre() {
        double[] c = FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE);
        return new double[] { c[2], c[1], -c[0] };
    }

    private static GunTransforms buildTransforms() {
        // First person (Z-family): rotX180, rotY225, S(FP_SCALE), then the state row.
        // Deviation: the notes say S(2.0) and T(-0.55, -0.45, -0.8), measured for a smaller model; the scale and idle
        // position follow FirstPersonFit instead (the gun drawn FP_LENGTH blocks long, box centre on the pistol's
        // tuned anchor).
        TransformProgram fpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FP_SCALE)
            .build();
        TransformProgram fpIdle = TransformProgram.builder()
            .translate(IDLE[0], IDLE[1], IDLE[2])
            .build();
        // Firing: a tiny, fast jolt: pitch up to 3.5 degrees and slide back 0.15 block at the dip.
        TransformProgram fpFire = TransformProgram.builder()
            .rotX((p, part) -> 7.0 * yF(p))
            .translate(
                TransformProgram.c(IDLE[0]),
                TransformProgram.c(IDLE[1]),
                (p, part) -> IDLE[2] - yF(p) * 0.3 / FP_SCALE)
            .build();
        // Reload: tip the muzzle up to 50 degrees and back.
        TransformProgram fpReload = TransformProgram.builder()
            .rotX((p, part) -> -50.0 * yR(p))
            .translate(IDLE[0], IDLE[1], IDLE[2])
            .build();
        // Third person: rotX180, rotY135, S(1.0), T(0.05, -0.6, 0.4). Aimed extra translate (0, 0, 0.2).
        // Deviation: the notes' S(1.2) was for the smaller Techguns model; this model is already larger.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(135)
            .scale(1.0)
            .translate(0.05, -0.6, 0.4)
            .build();
        // Recoil about the wrist (0, 0, 0.9): up to 10 degrees.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0, 0.9, b -> b.rotX((p, part) -> -10.0 * kick(p)))
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .about(0, 0.3, 0.9, b -> b.rotX((p, part) -> -40.0 * yR(p)))
            .build();
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .scale(0.7)
            .translate(0, -0.5, -0.3)
            .build();
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .scale(1.0)
            .rotY(90)
            .translate(0.3, -0.5, 0)
            .build();
        // Deviation: additive, because the GregGuns flash sheet is black-backed (docs/assets/MANIFEST.md). The offset
        // is the fixed first-person flash frame's image of the muzzle (solved from the same projection as the
        // pistol's, FP_SCALE and IDLE above).
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.52,
            -0.95,
            1.65,
            1.0,
            new double[] { 0, 0, 0 },
            true,
            0.45);
        return GunTransforms.builder()
            .parts(1)
            .timing(2, 5, 40)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0, 0.2 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
