package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Combat Shotgun's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/combat_shotgun.md}: proportions, silhouettes,
 * palettes), by an agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists
 * or textures. The look: a long, low military pump shotgun, 55 texels long and about 12.6 tall, with a light gunmetal
 * barrel (a 3x3 plus-shaped section) over a thin magazine tube joined by a small clamp, a fat black 4-wide pump
 * wrapped around both, a squat two-step receiver, a steeply raked pistol grip with a stub trigger and no guard, and a
 * skeleton stock (butt block, hinge block, two rising struts with air between them). The layout lives in
 * {@code tools/asset-gen/gun-paint/combat_shotgun.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns the
 * UV origins and writes the generated block below, and {@code gun_paint.py paint} paints the 128x32 sheet from the role
 * map {@code gun-paint/combat_shotgun.json}. {@code CombatShotgunProfileTest} checks the boxes against the layout file
 * and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as authored.
 * X = 0 is the back of the butt plate, Y = 0 the tops of the two sights. One texel is a sixteenth of a block.
 *
 * <h2>Parts</h2>
 * Part 0 is everything rigid. Part 1 is the pump (fore-end), a single 12x5x4 box that the firing and reload programs
 * slide back along X (about 9 texels, the pump's rack) and return, as the pistol's slide group does.
 *
 * <p>
 * Spec: model-notes/combat_shotgun.md sections 6 and 7; rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class CombatShotgunProfile {

    /** Texture resource paths of the model sheet (the notes list no camo variants). */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/combat_shotgun.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 128;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 32;
    /** GL units per texel. */
    public static final float MODEL_SCALE = 1.0f / 16.0f;
    /** The muzzle in the gun frame, blocks: the barrel's front face (texel 55) on its axis (texel 2.5). */
    public static final float[] MUZZLE = { 55.0f / 16.0f, 2.5f / 16.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;
    /** How far the pump racks back, in model blocks: 9 texels, inside the notes' 7 to 10. */
    public static final double PUMP_TRAVEL = 9.0 / 16.0;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** Part 0 is the rigid gun; part 1 is the pump. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("butt_plate", 0, 68, 0, 2, 5, 3, 0f, 3.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("toe", 0, 110, 0, 2, 3, 2, 0f, 8.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("hinge", 0, 78, 0, 2, 5, 3, 2f, 3f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("brace", 0, 118, 0, 1, 2, 2, 2f, 8f, -1f, NO_PIVOT, 0f),
        new BoxSpec("strut_lower", 0, 92, 7, 12, 2, 2, 3f, 6f, -1f, new float[] { 3f, 6f, 0f }, -15f),
        new BoxSpec("strut_upper", 0, 98, 11, 10, 1, 1, 3.6f, 3.8f, -0.5f, new float[] { 3.6f, 3.8f, 0f }, -8f),
        new BoxSpec("recv_rear", 0, 88, 0, 8, 4, 3, 13f, 2f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("plate_rear", 0, 24, 9, 7, 1, 2, 14f, 1f, -1f, NO_PIVOT, 0f),
        new BoxSpec("rear_sight", 0, 120, 6, 2, 1, 1, 15.5f, 0f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("grip", 0, 32, 0, 3, 7, 2, 14f, 5f, -1f, new float[] { 14f, 5f, 0f }, 19f),
        new BoxSpec("trigger", 0, 120, 8, 2, 1, 1, 16f, 6f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("recv_main", 0, 42, 0, 10, 5, 3, 21f, 1.5f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("plate_main", 0, 0, 9, 10, 1, 2, 21f, 0.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("barrel_slab", 0, 42, 8, 24, 3, 1, 31f, 1f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("rail_right", 0, 48, 14, 24, 1, 1, 31f, 2f, -1.5f, NO_PIVOT, 0f),
        new BoxSpec("rail_left", 0, 48, 12, 24, 1, 1, 31f, 2f, 0.5f, NO_PIVOT, 0f),
        new BoxSpec("tube", 0, 0, 12, 22, 2, 2, 31f, 4.5f, -1f, NO_PIVOT, 0f),
        new BoxSpec("clamp", 0, 118, 4, 2, 1, 1, 50f, 4f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("front_sight", 0, 110, 5, 2, 1, 1, 52f, 0f, -0.5f, NO_PIVOT, 0f),
        new BoxSpec("pump", 1, 0, 0, 12, 5, 4, 30f, 2.5f, -2f, NO_PIVOT, 0f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first person S(2.25), which runs the stock off screen. The scale follows the
    // FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's precedent) and the idle
    // position puts this model's box centre on the family anchor. The sheet's reload shift is multiplied by K as in
    // the bolt-action profile (a 1.5 sheet scale, the same class of long gun).
    private static final double SHEET_FP_SCALE = 1.5;
    /** On-screen length of the shotgun in first person, blocks of the scaled frame. */
    private static final double GUN_LENGTH = 2.0;
    private static final double FP_SCALE = FirstPersonFit
        .scale(FirstPersonFit.lengthBlocks(BOXES, MODEL_SCALE), GUN_LENGTH);
    private static final double FP_AMP = FirstPersonFit.translationFactor(SHEET_FP_SCALE, FP_SCALE);
    private static final double[] IDLE = FirstPersonFit
        .idleOffset(FirstPersonFit.ANCHOR_X_FAMILY, FP_SCALE, FirstPersonFit.centerBlocks(BOXES, MODEL_SCALE));
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the Combat Shotgun. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "combat_shotgun",
        BOXES,
        2,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        false,
        TRANSFORMS);

    private CombatShotgunProfile() {}

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
        // Firing (12 ticks): the first quarter kicks the gun back with a 10 degree roll; the rest is the rechamber, a
        // half sine in which the gun cants (roll 8, yaw -18, pitch -6 at the peak), moves slightly forward and the
        // pump (part 1) racks back along the barrel.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> p < 0.25 ? 20.0 * yF(4.0 * p) : 8.0 * rechamber(p))
            .rotY((p, part) -> p < 0.25 ? 0.0 : -18.0 * rechamber(p))
            .rotX((p, part) -> p < 0.25 ? 0.0 : -6.0 * rechamber(p))
            .translate(
                (p, part) -> FP_X + (p < 0.25 ? 0.6 * yF(4.0 * p) : 0.15 * rechamber(p))
                    - (part == 1 && p >= 0.25 ? PUMP_TRAVEL * rechamber(p) : 0.0),
                TransformProgram.c(FP_Y),
                TransformProgram.c(FP_Z))
            .build();
        // Reload: the first 80 percent turns the receiver toward the camera (roll 10, yaw -50, pitch -10) with a
        // shift, the last 20 percent racks the pump.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -10.0 * loading(p))
            .rotY((p, part) -> 50.0 * loading(p))
            .rotX((p, part) -> 10.0 * loading(p))
            .translate(
                (p, part) -> FP_X - FP_AMP * loading(p) - (part == 1 ? PUMP_TRAVEL * endRack(p) : 0.0),
                (p, part) -> FP_Y - FP_AMP * 0.5 * loading(p),
                (p, part) -> FP_Z - FP_AMP * 0.25 * loading(p))
            .build();
        // Third person: rotX180, rotY225, S(1.15), T = minus the grip (texel 15.5, 8) so the grip sits in the hand and
        // the muzzle points forward (the notes' (-0.8, -0.4, 0) is the same place in their frame). Aimed extra
        // translate (0, 0.2, -0.25) as the other long guns.
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(1.15)
            .translate(-0.95, -0.5, 0)
            .build();
        // Recoil: a 20 degree roll in the first half; in the second half the pump slides back 0.5 and returns.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -20.0 * kick(p)))
            .translate(
                (p, part) -> part == 1 && p >= 0.5 ? -0.5 * Math.sin(2.0 * Math.PI * (p - 0.5)) : 0.0,
                TransformProgram.c(0),
                TransformProgram.c(0))
            .build();
        // Reload: pitch 30 and yaw 40 at mid-reload; near the end the pump is pulled back 0.55.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .translate((p, part) -> part == 1 ? -0.55 * endRack(p) : 0.0, TransformProgram.c(0), TransformProgram.c(0))
            .build();
        // Ground: scale 0.55, centred on the model's middle (texel 27.5, 6.3) so it lies on the ground.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.55)
            .translate(-1.72, -0.3, 0)
            .build();
        // Icon: side-on, muzzle to the right, the 3.4-block gun fitted to the slot (the notes' 0.6), centred.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.58)
            .translate(-1.72, -0.39, 0)
            .build();
        // Muzzle flash: muzzleflash2 sheet at scale 1.85 (the notes), additive because the GregGuns sheet is
        // black-backed. The first-person offset is drawn in a fixed frame; this model has the bolt-action's length
        // and the same anchor, so its solved offset is reused. Headless work cannot look at the screen, so this is
        // unverified in game.
        MuzzleFlashSpec flash = new MuzzleFlashSpec(
            "gregguns:textures/fx/muzzleflash2.png",
            new SpriteSheet(4, 4, MUZZLE_FLASH_FRAMES),
            -0.055,
            0.005,
            0.357,
            1.85,
            new double[] { 0, 0, 0 },
            true,
            0.7);
        return GunTransforms.builder()
            .parts(2)
            .timing(12, 5, 50)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.2, -0.25 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }

    /** The rechamber swing: 0 at p = 0.25, a half sine to 0 again at p = 1. */
    private static double rechamber(double p) {
        return Math.sin((p - 0.25) * (Math.PI / 0.75));
    }

    /** The shell-loading dip over the first 80 percent of a reload (0 to -1 to 0), then 0. */
    private static double loading(double p) {
        return p < 0.8 ? yR(p / 0.8) : 0.0;
    }

    /** The pump rack at the end of a reload: 0 before p = 0.8, a half sine to 0 again at p = 1. */
    private static double endRack(double p) {
        return p < 0.8 ? 0.0 : Math.sin((p - 0.8) * (Math.PI / 0.2));
    }
}
