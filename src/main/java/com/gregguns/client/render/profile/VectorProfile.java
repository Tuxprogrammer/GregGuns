package com.gregguns.client.render.profile;

import static com.gregguns.client.anim.GunCurves.kick;
import static com.gregguns.client.anim.GunCurves.yF;
import static com.gregguns.client.anim.GunCurves.yR;

import com.gregguns.client.anim.SpriteSheet;

/**
 * The Vector submachine gun's model boxes and render transforms, as data.
 *
 * <h2>Where the model comes from (originality record)</h2>
 * The boxes are original GregGuns geometry, built clean-room from written design notes only
 * ({@code docs/techguns-spec/03-gun-visuals-audio/model-notes/vector.md}: proportions, silhouettes, palettes), by an
 * agent that never saw the original model or texture. Nothing is taken from Techguns code, box lists or textures. The
 * look: a thin, boxy SMG 44 units long and 27.4 tall with a dark receiver under a slotted top rail and a low red-dot
 * sight, a skeletal stock (top bar, butt pad, one diagonal strut and two diamond cross bars around an open triangular
 * window), a flat-fronted handguard with a two-step diamond barrel stub, a short diamond foregrip on a bottom rail, and
 * one big light mass: a raked lower made of two slabs tilted 20 degrees, a parallel raked pistol grip with a
 * see-through trigger slot and a small leaning trigger, flat floor plates, and a narrow magazine that continues the
 * slabs' slanted front edge in one straight line. The layout lives in
 * {@code tools/asset-gen/gun-paint/vector.layout.json}; {@code tools/asset-gen/gun_paint.py pack} assigns the UV
 * origins and writes the generated block below, and {@code gun_paint.py paint} paints the 256x128 default sheet (tan
 * lower parts) and the Black camo from the role map {@code gun-paint/vector.json}. {@code VectorProfileTest} checks
 * the boxes against the layout file and the notes' key facts.
 *
 * <h2>Frame of reference</h2>
 * X-family: the boxes are laid out with the muzzle toward +X and Y growing downward, in texels, and drawn as authored.
 * X = 0 is the back of the butt pad and Y = 0 the top of the red-dot roof; Z = 0 is the mid-plane. One texel is half
 * of the notes' unit (the notes' sizes of 0.5 to 3.2 units need half-unit steps), so the scale is 1/32 block per texel
 * and the sheet is 256x128. The model has no moving parts (the whole gun moves as one part). Tilted slabs hinge at the
 * notes' corners (a turn about Z through the pivot, bottom swinging toward the rear); square bars shown as diamonds use
 * a 45 degree turn about their own axis ({@code tiltX}, or {@code tiltY} for the vertical foregrip).
 *
 * <p>
 * Round 3 (measured corrections): the upper receiver, the red-dot base and the hood roof are 4.0 wide (the only wide
 * parts); everything under the receiver stays 3.0. Every Y except the roof plate sits 0.1 unit lower than in the first
 * round. The base is 9.0 long, the hood walls and roof 4.0 long with a 1.4 channel, the collar a full 2 by 2 bar
 * turned 45 degrees (2.8 across), the foregrip centred 0.1 toward +Z and the trigger 0.3 farther back.
 *
 * <p>
 * Deviation: the box pipeline has whole-texel sizes, so the notes' 3.2 thickness is 4.0 for the receiver (the
 * measured round-3 value) and 3.0 elsewhere.
 *
 * <h2>Scope</h2>
 * The red-dot sight is the sight: aiming only narrows the field of view (zoom x0.75), so the profile sets no scope
 * overlay.
 *
 * <p>
 * Spec: gun-sheets.md "Vector" (timing); rendering-framework.md section 6.1 (X-family), 8 and 9.
 */
public final class VectorProfile {

    /** Texture resource paths of the model sheet: the default tan camo and Black. */
    public static final String[] TEXTURES = { "gregguns:textures/models/guns/vector.png",
        "gregguns:textures/models/guns/vector_camo1.png" };
    /** The default texture, which the tests read. */
    public static final String TEXTURE = TEXTURES[0];
    /** Model sheet width in texels. */
    public static final int TEXTURE_WIDTH = 256;
    /** Model sheet height in texels. */
    public static final int TEXTURE_HEIGHT = 128;
    /** GL units per texel: 1/32 block, half of the notes' unit. */
    public static final float MODEL_SCALE = 1.0f / 32.0f;
    /** The muzzle in the gun frame, blocks: x forward, y down, z: the collar's front (unit 44) on the barrel axis. */
    public static final float[] MUZZLE = { 88.0f / 32.0f, 18.8f / 32.0f, 0.0f };
    /** Frames of {@code muzzleflash2.png} that are painted: 0 to 6. Cell 7 is empty. */
    public static final int MUZZLE_FLASH_FRAMES = 7;

    private static final float[] NO_PIVOT = { 0, 0, 0 };

    /** One part: the whole gun moves as a unit. */
    public static final BoxSpec[] BOXES = {
        // BEGIN GENERATED BOXES (tools/asset-gen/gun_paint.py pack; edit the layout, not this)
        new BoxSpec("receiver", 0, 132, 22, 50, 6, 8, 32f, 10.4f, -4f, NO_PIVOT, 0f),
        new BoxSpec("top_rail", 0, 20, 44, 46, 2, 4, 34f, 8.4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("dot_base", 0, 160, 0, 18, 6, 8, 47f, 4.4f, -4f, NO_PIVOT, 0f),
        new BoxSpec("hood_wall_l", 0, 56, 38, 8, 4, 2, 49.2f, 1.4f, 1.4f, NO_PIVOT, 0f),
        new BoxSpec("hood_wall_r", 0, 76, 38, 8, 4, 2, 49.2f, 1.4f, -3.4f, NO_PIVOT, 0f),
        new BoxSpec("hood_roof", 0, 212, 0, 8, 2, 8, 49.2f, 0f, -4f, NO_PIVOT, 0f),
        new BoxSpec("red_dot", 0, 156, 18, 0, 2, 2, 52f, 2.2f, -1f, NO_PIVOT, 0f),
        new BoxSpec("handguard", 0, 64, 26, 20, 6, 6, 62f, 16.4f, -3f, NO_PIVOT, 0f),
        new BoxSpec("barrel", 0, 144, 18, 4, 2, 2, 82f, 17.8f, -1f, new float[] { 82f, 18.8f, 0f }, 0f, 45f),
        new BoxSpec("collar", 0, 244, 0, 2, 4, 4, 86f, 16.8f, -2f, new float[] { 86f, 18.8f, 0f }, 0f, 45f),
        new BoxSpec("bottom_rail", 0, 20, 38, 14, 2, 4, 67f, 22.4f, -2f, NO_PIVOT, 0f),
        new BoxSpec("fg_mount", 0, 212, 10, 8, 2, 6, 71f, 22.6f, -3f, NO_PIVOT, 0f),
        new BoxSpec("foregrip", 0, 144, 0, 4, 14, 4, 73f, 24.4f, -1.8f, new float[] { 75f, 31.4f, 0.2f }, 0f, 0f, 45f),
        new BoxSpec("slab_front", 0, 0, 0, 10, 22, 6, 60f, 16.4f, -3f, new float[] { 70f, 16.4f, 0f }, 20f),
        new BoxSpec("slab_rear", 0, 32, 0, 10, 22, 6, 50f, 16.4f, -3f, new float[] { 60f, 16.4f, 0f }, 20f),
        new BoxSpec("floor_upper", 0, 116, 36, 18, 2, 6, 43f, 33.2f, -3f, NO_PIVOT, 0f),
        new BoxSpec("floor_lower", 0, 160, 14, 20, 2, 6, 42.4f, 35.2f, -3f, NO_PIVOT, 0f),
        new BoxSpec("magazine", 0, 112, 0, 6, 22, 4, 56.6f, 34.4f, -2f, new float[] { 62.6f, 34.4f, 0f }, 20f),
        new BoxSpec("grip", 0, 64, 0, 8, 20, 6, 34.2f, 16.2f, -3f, new float[] { 42.2f, 16.2f, 0f }, 20f),
        new BoxSpec("guard_floor", 0, 164, 36, 8, 2, 6, 35.3f, 34.2f, -3f, new float[] { 39.3f, 35.2f, 0f }, 17.8f),
        new BoxSpec("guard_wedge", 0, 0, 38, 4, 2, 6, 39.8f, 33.6f, -3f, new float[] { 41.8f, 34.6f, 0f }, -20f),
        new BoxSpec("trigger", 0, 248, 18, 2, 6, 2, 41.9f, 15.4f, -1f, new float[] { 42.9f, 15.4f, 0f }, -10f),
        new BoxSpec("stock_bar", 0, 0, 28, 22, 4, 6, 10f, 12.4f, -3f, NO_PIVOT, 0f),
        new BoxSpec("stock_ramp", 0, 216, 36, 6, 2, 6, 26.5f, 11.4f, -3f, new float[] { 29.5f, 12.4f, 0f }, -20f),
        new BoxSpec("butt_pad", 0, 92, 0, 4, 20, 6, 0f, 16f, -3f, NO_PIVOT, 0f),
        new BoxSpec("strut", 0, 132, 0, 4, 20, 2, 9.8f, 14.2f, -1f, new float[] { 13.8f, 14.2f, 0f }, 20f),
        new BoxSpec("cross_bar", 0, 192, 36, 8, 4, 4, 4f, 18f, -2f, new float[] { 8f, 20f, 0f }, 0f, 45f),
        new BoxSpec("cross_block", 0, 240, 10, 4, 4, 4, 4f, 29f, -2f, new float[] { 6f, 31f, 0f }, 0f, 45f),
        // END GENERATED BOXES
    };

    // Deviation: the notes give first-person scale 1.4 and an idle offset measured from the origin of Techguns' model.
    // The scale follows the FirstPersonFit rule with the long-gun target of 2 blocks on screen (the bolt-action's
    // precedent), and the idle position puts this model's box centre on the family anchor.
    private static final InHandFit.Placement FIT = InHandFit
        .fit("vector", BOXES, MODEL_SCALE, true, -45, 225, new double[] { 0, 0.1, 0 });
    private static final double FP_SCALE = FIT.fpScale;
    private static final double[] IDLE = FIT.fpIdle;
    private static final double FP_X = IDLE[0];
    private static final double FP_Y = IDLE[1];
    private static final double FP_Z = IDLE[2];

    /** The notes' render transforms, timing and muzzle flash for the Vector. */
    public static final GunTransforms TRANSFORMS = buildTransforms();

    /**
     * The client half of the gun's definition: model, textures and transforms, listed in {@link ClientGunDefinitions}.
     */
    public static final ClientGunDefinition DEFINITION = new ClientGunDefinition(
        "vector",
        BOXES,
        1,
        TEXTURE_WIDTH,
        TEXTURE_HEIGHT,
        MODEL_SCALE,
        TEXTURES,
        MUZZLE,
        true,
        TRANSFORMS);

    private VectorProfile() {}

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
        // Firing: a tiny muzzle-up kick, 5 degrees at the dip (yF = -0.5), and 0.15 block back on screen.
        TransformProgram fpFire = TransformProgram.builder()
            .rotZ((p, part) -> 10.0 * yF(p))
            .translate((p, part) -> FP_X + 0.42 / FP_SCALE * yF(p), (p, part) -> FP_Y, (p, part) -> FP_Z)
            .build();
        // Reload: the gun tips up to 50 degrees at mid-reload and comes back, over the 40 ticks.
        TransformProgram fpReload = TransformProgram.builder()
            .rotZ((p, part) -> -50.0 * yR(p))
            .translate(FP_X, FP_Y, FP_Z)
            .build();
        // Third person: rotX180, rotY225 (the notes' Z-family yaw 135, turned to the X-family). The pistol grip
        // (about 1.2 blocks from the butt, 0.75 down) lands at the hand as the M4's does. Aimed extra (0, 0.1, 0).
        TransformProgram tpBase = TransformProgram.builder()
            .rotX(180)
            .rotY(225)
            .scale(FIT.tpScale)
            .translate(FIT.tpTranslate[0], FIT.tpTranslate[1], FIT.tpTranslate[2])
            .build();
        // Recoil: the gun pitches up 15 degrees about the hand, in the first quarter of the first half.
        TransformProgram tpRecoil = TransformProgram.builder()
            .about(0, 0.3, -0.9, b -> b.rotZ((p, part) -> -15.0 * kick(p)))
            .build();
        // Reload: about 30 degrees pitch plus 40 degrees yaw at mid-animation, then back.
        TransformProgram tpReload = TransformProgram.builder()
            .rotX((p, part) -> 30.0 * yR(p))
            .rotY((p, part) -> 40.0 * yR(p))
            .build();
        // Ground: scale 0.6 (the notes), the model centred (centre at unit 22, 13.7) so it lies flat.
        TransformProgram ground = TransformProgram.builder()
            .rotX(180)
            .rotY(90)
            .scale(0.6)
            .translate(-1.375, -0.856, 0)
            .build();
        // Icon: side-on, scale 0.75 (the notes), the model centred in the slot.
        TransformProgram icon = TransformProgram.builder()
            .rotX(180)
            .rotY(180)
            .scale(0.75)
            .translate(-1.375, -0.856, 0)
            .build();
        // Deviation: the first-person flash is a fixed-frame screen effect. Its offset is the AK-47's, whose muzzle
        // sits at the same place on screen (same 2-block length and anchor, muzzle on the same side of the box
        // centre). Headless work cannot look at the screen, so this is unverified in game. muzzleFlash2 sheet,
        // additive because the GregGuns sheet is black-backed (docs/assets/MANIFEST.md).
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
            .parts(1)
            .timing(2, 5, 40)
            .firstPerson(fpBase, fpIdle, fpFire, fpReload)
            .thirdPerson(tpBase, new double[] { 0, 0.1, 0 }, tpRecoil, tpReload)
            .world(ground, icon)
            .muzzleFlash(flash)
            .build();
    }
}
