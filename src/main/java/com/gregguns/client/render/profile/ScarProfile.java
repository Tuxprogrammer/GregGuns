package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.p66;
import static com.gregguns.client.anim.GunCurves.yF;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The SCAR's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/scar.md}: proportions, silhouettes, palettes), by an
 * agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures. The
 * look: a long, lean, desert-tan battle rifle, 71.8 texels long and 23.3 tall but only 4 wide at the receiver. A
 * 37-long slab receiver under a smooth top rail (the teeth are painted), a black dumbbell scope on a riser, a hooked
 * stock with a sloped cheek, a flat underside, a raked heel and a tall black buttpad, a stippled pistol grip raked
 * back 25.6 degrees, an open trigger window with a blade in it, a straight 2-wide magazine, an open-triangle angled
 * foregrip under black vented handguard rails, and a black barrel with a gray collar, a gas block, a front sight and
 * a fatter muzzle device. The layout lives in {@code tools/asset-gen/gun-paint/scar.layout.json};
 * {@code tools/asset-gen/gun_paint.py pack} assigns the UV origins and writes the generated block below, and
 * {@code gun_paint.py paint} paints the 256x128 default (tan) and Black sheets from the role map
 * {@code gun-paint/scar.json}. {@code ScarProfileTest} checks the boxes against the layout file and the notes' key
 * facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as authored.
 * Y = 0 is the top of the scope bells; X = 0 is the back face of the buttpad; Z = 0 is the mid-plane (-Z is the
 * shooter's right). One texel is a sixteenth of a block. Slanted pieces (grip, trigger, cheek, heel, foregrip
 * struts, sight pivot) are boxes tilted about Z; the scope, collar, gas block and muzzle device are square bars turned
 * 45 degrees about X so they read as diamonds, and the hinge pin and top turret are turned 45 degrees about Y.
 *
 * <h2>Parts</h2>
 * Two parts. Part 0 is the whole rifle; part 1 is the charging-handle knob on the left side, the only moving piece:
 * it slides back along its painted 12-long slot when the gun fires (11 texels at 15 percent of the cycle) and returns.
 *
 * <h2>Scope</h2>
 * The rifle always has its scope: while zoomed the renderer hides the model and draws the overlay
 * {@link #SCOPE_TEXTURE} (rendering-framework.md section 9.2).
 *
 * <p>
 * Spec: model-notes/scar.md sections 6 and 7 (framing, timing, motion); rendering-framework.md section 6.1 (X-family),
 * 8, 9.
 */
public final class ScarProfile {

    /** Texture resource paths of the model sheet: the default (tan) and Black. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/scar.png",
        "gregguns:textures/models/guns/scar_camo1.png" };
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
    /**
     * The muzzle in the gun frame, blocks: x forward, y down; the muzzle device's front face (texel 71.8) on the axis.
     */
    public static final float[] MUZZLE = { 71.8f / 16.0f, 9.5f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;
    /** How far the charging knob slides back, in model blocks: 11 texels, the length of its painted slot. */
    public static final double KNOB_TRAVEL = 11.0 / 16.0;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** Two parts: the rifle (0) and the charging-handle knob (1). */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("receiver", 0, 62, 0, 37, 5, 4, 15.8f, 6.3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("rail", 0, 62, 14, 36, 1, 2, 16.3f, 5.3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("lower", 0, 208, 0, 16, 3, 3, 17.8f, 11.3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("magwell", 0, 234, 6, 8, 3, 3, 25.8f, 14.3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("chin_rear", 0, 62, 9, 2, 2, 3, 15.8f, 10.8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("chin_front", 0, 246, 0, 2, 2, 3, 33.8f, 10.8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("mag", 0, 174, 0, 7, 6, 2, 26.3f, 17.3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 36, 0, 4, 9, 3, 17.8f, 14.3f, -1.5f, new float[] { 21.8f, 14.3f, 0f }, 25.6f),
        new BoxSpec("guard", 0, 26, 16, 6, 1, 2, 19.8f, 16.3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("trigger", 0, 202, 13, 1, 2, 1, 22.3f, 13.8f, -0.5f, new float[] { 22.8f, 13.8f, 0f }, -15f),
        new BoxSpec("stock_hinge", 0, 50, 0, 2, 7, 4, 14.3f, 5.8f, -2f, NO_PIVOT, 0f),
        new BoxSpec("hinge_pin", 0, 204, 0, 1, 6, 1, 14.8f, 6.3f, -2.5f, new float[] { 15.3f, 9.3f, -2f }, 0f, 0f, 45f),
        new BoxSpec("cheek", 0, 144, 0, 11, 4, 4, 3.8f, 5.8f, -2f, new float[] { 14.8f, 5.8f, 0f }, -10f),
        new BoxSpec("stock_under", 0, 208, 6, 10, 3, 3, 4.8f, 9.8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("heel", 0, 24, 0, 3, 10, 3, 3.8f, 8.8f, -1.5f, new float[] { 6.8f, 8.8f, 0f }, 15f),
        new BoxSpec("column", 0, 12, 0, 3, 11, 3, 1f, 7.8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("buttpad", 0, 0, 0, 2, 12, 4, 0f, 7.3f, -2f, NO_PIVOT, 0f),
        new BoxSpec("latch", 0, 250, 15, 2, 1, 1, 2.5f, 10.8f, 0.8f, new float[] { 3.5f, 11.3f, 1.3f }, 0f, 45f),
        new BoxSpec("scope_rear", 0, 160, 8, 3, 3, 3, 16.8f, 0.6f, -1.5f, new float[] { 16.8f, 2.1f, 0f }, 0f, 45f),
        new BoxSpec("scope_tube", 0, 126, 9, 6, 2, 2, 19.8f, 1.1f, -1f, new float[] { 19.8f, 2.1f, 0f }, 0f, 45f),
        new BoxSpec("scope_front", 0, 144, 8, 5, 3, 3, 25.8f, 0.6f, -1.5f, new float[] { 25.8f, 2.1f, 0f }, 0f, 45f),
        new BoxSpec("scope_mount", 0, 206, 12, 4, 1, 3, 23.3f, 4.8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("scope_riser", 0, 190, 8, 6, 3, 2, 21.8f, 2.3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("turret_top", 0, 20, 14, 1, 1, 1, 22.3f, 0.3f, -0.5f, new float[] { 22.8f, 0.8f, 0f }, 0f, 0f, 45f),
        new BoxSpec("turret_side", 0, 176, 16, 2, 1, 1, 21.8f, 1.6f, 0.9f, new float[] { 22.8f, 2.1f, 1.4f }, 0f, 45f),
        new BoxSpec("rear_base", 0, 220, 12, 2, 1, 3, 19.3f, 4.8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rear_pivot", 0, 230, 12, 1, 1, 3, 18.3f, 4.8f, -1.5f, new float[] { 18.8f, 5.3f, 0f }, 45f),
        new BoxSpec("rear_notch", 0, 12, 14, 3, 1, 1, 15.3f, 4.6f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("front_block", 0, 24, 13, 4, 1, 2, 54.3f, 5.3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("front_post", 0, 244, 12, 1, 2, 2, 53.3f, 5.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("guard_left", 0, 176, 13, 12, 2, 1, 40.3f, 8.1f, 2f, NO_PIVOT, 0f),
        new BoxSpec("guard_right", 0, 0, 16, 12, 2, 1, 40.3f, 8.1f, -3f, NO_PIVOT, 0f),
        new BoxSpec("bottom_rail", 0, 138, 14, 17, 1, 2, 35.8f, 11.3f, -1f, NO_PIVOT, 0f),
        new BoxSpec("mount_front", 0, 172, 8, 6, 2, 3, 43.3f, 10.8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("mount_rear", 0, 94, 9, 5, 1, 3, 38.3f, 11.8f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("strut_front", 0, 192, 0, 1, 6, 2, 43.8f, 11.8f, -1f, new float[] { 44.3f, 11.8f, 0f }, 40f),
        new BoxSpec("strut_rear", 0, 198, 0, 1, 5, 2, 37.8f, 12.2f, -1f, new float[] { 38.3f, 12.2f, 0f }, -27f),
        new BoxSpec("nub_front", 0, 42, 16, 2, 1, 1, 48.1f, 12.6f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("nub_strut", 0, 250, 12, 1, 2, 1, 43.8f, 13.4f, 1f, new float[] { 44.3f, 11.8f, 0f }, 40f),
        new BoxSpec("barrel_root", 0, 238, 12, 1, 2, 2, 52.8f, 8.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("collar", 0, 50, 11, 4, 2, 2, 53.8f, 8.5f, -1f, new float[] { 53.8f, 9.5f, 0f }, 0f, 45f),
        new BoxSpec("gas_block", 0, 36, 12, 5, 2, 2, 52.8f, 6.7f, -1f, new float[] { 52.8f, 7.7f, 0f }, 0f, 45f),
        new BoxSpec("barrel", 0, 72, 9, 9, 2, 2, 57.3f, 8.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("muzzle", 0, 110, 9, 6, 2, 2, 65.8f, 8.5f, -1f, new float[] { 65.8f, 9.5f, 0f }, 0f, 45f),
        new BoxSpec("knob", 1, 50, 15, 2, 1, 2, 37.8f, 7f, 2f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first person S(1.4) with an offset measured from Techguns' own origin. The scale
    // follows the FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's precedent), and
    // the idle position puts this model's box centre on the family anchor. The notes' fire and reload offsets are
    // multiplied by K so they move the rifle as far on screen as the notes intend.
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

    /** The notes' render transforms, timing, scope and muzzle flash for the SCAR. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "scar",
        BOXES,
        2,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private ScarProfile() {}

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
        // Firing (5 ticks): a small quiet recoil, up to 5 degrees of pitch and a 0.15 push back at mid-cycle (yF peaks
        // at -0.5, so the factors are 10 and 0.3). Only part 1 moves on its own: the knob slides back along the gun
        // axis by KNOB_TRAVEL, peaking at 15 percent of the cycle, then returns.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 10.0 * yF(p))
            .translate(
                (p, part) -> FP_X + FP_AMP * 0.3 * yF(p) - (part == 1 ? KNOB_TRAVEL * knob(p) : 0.0),
                TransformProgram.c(FP_Y),
                TransformProgram.c(FP_Z))
            .build();
        // Reload: the rifle tilts up to 50 degrees about the cross axis and rises with it, so the muzzle drops and the
        // magazine comes up toward the eye, held through the middle (p66 plateau).
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -50.0 * p66(p))
            .translate(TransformProgram.c(FP_X), (p, part) -> FP_Y - FP_AMP * 0.5 * p66(p), TransformProgram.c(FP_Z))
            .build();
        // Third person: rotX180, rotY225, S(0.8), T = minus the grip (texel 20, 15.5) so the grip sits in the hand and
        // the rifle extends forward. Aimed extra translate (0, 0.2, -0.25) as the other long guns.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(0.8)
            .translate(-1.25, -1.0, 0)
            .build();
        // Recoil about (0, 0.3, -0.9): the gun pitches up to 15 degrees and settles.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -15.0 * kick(p)))
            .build();
        // Reload: pitch about 30 degrees and yaw about 40 degrees at mid-reload.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * p66(p))
            .rotY((p, part) -> 40.0 * p66(p))
            .build();
        // Ground: lies on its side at the notes' 0.6, centred on the model's middle (texel 35.9, 11.65).
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.6)
            .translate(-2.245, -0.73, 0)
            .build();
        // Icon: side-on, muzzle to the right, the 4.5-block gun fitted to the slot (about 2 blocks like the other long
        // guns; the notes' 0.52 would overflow), centred on the model's middle.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.45)
            .translate(-2.245, -0.73, 0)
            .build();
        // Muzzle flash: muzzleflash2 sheet at scale 1.5 (the notes), additive because the GregGuns sheet is
        // black-backed. The first-person offset is drawn in a fixed frame, not on the gun; it is carried over from the
        // AK-47's (same anchor, same 2-block length, same flash scale). Headless work cannot look at the screen, so
        // this is unverified in game.
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
            .timing(5, 5, 45)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.2, -0.25 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .scope(new ScopeSpec(SCOPE_TEXTURE, 0.04, 0.02))
            .build();
    }

    /** The knob's stroke: 0 to 1 at 15 percent of the cycle (a quarter sine), then back to 0 at the end. */
    static double knob(double p) {
        if (p <= 0.0 || p >= 1.0) {
            return 0.0;
        }
        return p < 0.15 ? Math.sin(p / 0.15 * (Math.PI / 2.0)) : Math.cos((p - 0.15) / 0.85 * (Math.PI / 2.0));
    }
}
