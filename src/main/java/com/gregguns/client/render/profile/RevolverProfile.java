package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.p66;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Revolver's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/revolver.md}: proportions, silhouettes, palette), by an
 * agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures.
 * The look: a long diamond-section barrel with a short ejector rod under it, a wider-than-the-frame drum made of three
 * stepped slabs with painted seams, a stepped frame under a thin top strap, a recoil shield, a dark hammer, an open
 * two-post trigger guard and a four-slab wooden grip that leans back. The layout lives in
 * {@code tools/asset-gen/gun-paint/revolver.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns the UV
 * origins and writes the generated block below, and {@code gun_paint.py paint} paints {@code revolver.png} (64x32) and
 * {@code golden_revolver.png} from the role map {@code gun-paint/revolver.json}. {@code RevolverProfileTest} checks the
 * boxes against the layout file and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as authored,
 * because the sheet's X-family transform rows (first-person rotX 180, rotY -45; third-person rotY 225) expect a muzzle
 * along +X. Y = 0 is the top of the top strap and X = 0 the frame's back face. One texel is a sixteenth of a block, so
 * the 28-texel gun is a little under two blocks of model space before the rows scale it. The barrel and rod are
 * square bars turned 45 degrees about X ({@link BoxSpec#tiltXDegrees()}) so they show as diamonds.
 *
 * <p>
 * Spec: gun-sheets.md "Revolver" (rows and timing); rendering-framework.md section 6.1 (X-family) and section 8.
 */
public final class RevolverProfile {

    /** Texture resource path of the model sheet. */
    public static final String TEXTURE = "gregguns:textures/models/guns/revolver.png";
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 64;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 32;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /**
     * The muzzle in the gun frame, blocks: x forward, y down, z. The barrel's front face is 23 texels forward of the
     * frame's back face and its axis is 1.5 texels below the top of the top strap.
     */
    public static final float[] MUZZLE = { 23.0f / 16.0f, 1.5f / 16.0f, 0.0f };
    /**
     * Frames of {@code muzzleflash4x4.png} that are painted: 0 to 8. Cell 9 and later are empty, so the sheet's
     * "10 frames" in the asset manifest would show nothing for the last tenth of the window.
     */
    public static final int MUZZLE_FLASH_FRAMES = 9;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole revolver moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("strap", 0, 0, 18, 9, 1, 2, 0f, 0f, -1f, NO_PIVOT, 0f),
        new BoxSpec("frame_upper", 0, 38, 0, 9, 2, 3, 0f, 1f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("frame_lower", 0, 0, 14, 9, 2, 2, 0f, 3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("belly", 0, 44, 14, 6, 1, 2, 2f, 5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("shield", 0, 32, 0, 1, 4, 2, -1f, 1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("drum_top", 0, 0, 7, 5, 1, 4, 1.5f, 0.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("drum_mid", 0, 0, 0, 5, 2, 5, 1.5f, 1.5f, -2.5f, NO_PIVOT, 0f),
        new BoxSpec("drum_bottom", 0, 38, 5, 5, 1, 4, 1.5f, 3.5f, -2f, NO_PIVOT, 0f),
        new BoxSpec("barrel", 0, 18, 10, 14, 2, 2, 9f, 0.5f, -1f, new float[] { 9f, 1.5f, 0f }, 0f, 45f),
        new BoxSpec("rod", 0, 0, 12, 8, 1, 1, 9f, 3f, -0.5f, new float[] { 9f, 3.5f, 0f }, 0f, 45f),
        new BoxSpec("sight", 0, 24, 7, 2, 1, 1, 20.5f, -0.4f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("hammer", 0, 18, 7, 2, 2, 1, -2.5f, 1.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("hammer_spur", 0, 60, 14, 1, 1, 1, -3.5f, 1.5f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("guard_rear", 0, 38, 14, 1, 2, 2, 2f, 6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("guard_front", 0, 32, 6, 1, 2, 2, 6f, 6f, -1f, NO_PIVOT, 0f),
        new BoxSpec("guard_floor", 0, 44, 17, 3, 0, 2, 3f, 8f, -1f, NO_PIVOT, 0f),
        new BoxSpec("grip_crown", 0, 56, 5, 2, 2, 2, -3f, 3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("grip_web", 0, 22, 14, 6, 2, 2, -4f, 5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("grip_neck", 0, 50, 10, 4, 2, 2, -4f, 7f, -1f, NO_PIVOT, 0f),
        new BoxSpec("grip_butt", 0, 20, 0, 4, 5, 2, -5f, 9f, -1f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    /** The sheet's first-person scale, recorded for Techguns' smaller model. */
    private static final double SHEET_FP_SCALE = 1.5;
    /**
     * The first-person scale from {@link FirstPersonFit}: the 31-texel revolver is drawn as long on screen as the
     * 9mm Pistol (15/16 of a block), which is about S(0.48), not the sheet's S(1.5).
     */
    public static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), FirstPersonFit.HANDGUN_LENGTH);
    /** The sheet's fire and reload translation deltas are multiplied by this, because the scale changed. */
    private static final double K = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    /**
     * Nudge (FirstPersonFit step 4): the revolver's grip hangs 14 texels below the top strap, so its box centre sits
     * low and the grip ran off the bottom-right of the screen at the family anchor (first-person screenshot). The
     * anchor is moved up the screen and toward the middle so the whole gun, grip included, is in view, as the design
     * notes' framing target asks (side plus the back of the frame).
     */
    private static final double[] ANCHOR = { FirstPersonFit.ANCHOR_X_FAMILY[0] + 0.7, FirstPersonFit.ANCHOR_X_FAMILY[1],
        FirstPersonFit.ANCHOR_X_FAMILY[2] - 0.4 };
    /** The idle first-person translation from {@link FirstPersonFit}: this model's box centre on the family anchor. */
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(ANCHOR, FP_SCALE, FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE));

    /** The sheet's render transforms, timing and muzzle flash for the revolver. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "revolver",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        new String[] { TEXTURE },
        MUZZLE,
        true,
        TRANSFORMS);

    private RevolverProfile() {}

    private static GunTransforms buildTransforms() {
        // First person (X-family): rotX180, rotY-45, S(FP_SCALE), then the state row.
        // Deviation: the sheet says S(1.5), which drew the revolver far too big (our model is larger than the one the
        // sheet was written for). The scale and the idle position follow the FirstPersonFit rule; the sheet's fire
        // and reload deltas are multiplied by K so the gun moves as far on screen as the sheet intends.
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
        // Firing: a strong muzzle flip, rotZ(35 yF), the gun jumping up and back.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 35.0 * yF(p))
            .translate((p, part) -> x0 + K * 0.5 * yF(p), (p, part) -> y0 + K * 0.4 * yF(p), TransformProgram.c(z0))
            .build();
        // Reload: s = P66(p); the gun tips out to the side to open the cylinder, then back.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> 25.0 * p66(p))
            .rotY((p, part) -> -20.0 * p66(p))
            .translate(
                (p, part) -> x0 + K * 0.6 * p66(p),
                (p, part) -> y0 + K * 0.3 * p66(p),
                (p, part) -> z0 + K * 0.25 * p66(p))
            .build();
        // Third person: rotX180, rotY225, S(0.85), T(-1.4, -0.8, 0). Aimed extra translate (0, -0.05, 0.2).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(0.85)
            .translate(-1.4, -0.8, 0)
            .build();
        // Recoil about (0, 0.25, 1.0): while p < 0.5, rotZ(-30 K).
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.25, 1.0, b -> b.rotZ((p, part) -> p < 0.5 ? -30.0 * kick(p) : 0.0))
            .build();
        TransformProgram tpReload = TransformProgram.builder()
            .about(0, 0.25, 1.0, b -> b.rotZ((p, part) -> -40.0 * yR(p)))
            .build();
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .translate(0, -0.35, 0)
            .scale(0.8)
            .build();
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(1.0)
            .translate(-0.5, -0.2, 0)
            .build();
        // muzzleFlash sheet, (-0.4, -2.5, 0.7), scale 1.85; additive because the GregGuns sheet is black-backed.
        // Deviation: the sheet's first-person flash offset was placed for Techguns' model. The flash is drawn in a
        // fixed
        // frame, not on the gun, so after the fit-rule rescale it floated above the muzzle. Measured on screen
        // (960x540 sweep): +0.5 on x moves it 33 px up, +0.5 on z moves it 49 px right. (-1.25, -2.5, 2.0) puts it on
        // the muzzle of an X-family handgun held at FirstPersonFit.ANCHOR_X_FAMILY.
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash4x4.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -1.25,
            -2.5,
            2.0,
            1.85,
            new double[] { 0, 0, 0 },
            true,
            0.7);
        return GunTransforms.builder()
            .parts(1)
            .timing(6, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, -0.05, 0.2 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
