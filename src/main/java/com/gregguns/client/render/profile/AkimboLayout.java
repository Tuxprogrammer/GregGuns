package com.gregguns.client.render.profile;

/**
 * The pure arithmetic of drawing an akimbo gun as two copies of its single gun's model: how many copies a view draws,
 * which copy animates for a shot, how the second copy is mirrored to the other side of the screen, and how the loaded
 * rounds split between the hands. No Minecraft or GL types, so unit tests cover it; {@code GunItemRenderer} applies
 * the results.
 *
 * <p>
 * Hands: 0 is the main (right) gun, 1 the second (left). The shot's attack type is the hand that fired (0 or 1).
 *
 * <p>
 * Spec: rendering-framework.md section 6 (Akimbo), section 3 (attackType), model-notes/revolver.md section 6.
 * Deviation: Techguns shifts the second gun about 1.75 blocks sideways without mirroring it, and draws the off-hand
 * gun on the third-person body with a separate layer renderer. Here the second gun is a true mirror image about the
 * screen centre in first person (equal depth and height, right negated; see {@link #MIRROR_SHIFT}); in third person
 * only the main-hand gun is drawn (the held-item renderer is handed
 * only the right-hand matrix), and it recoils on every shot so no shot goes unseen.
 */
public final class AkimboLayout {

    /** Hands an akimbo gun has. */
    public static final int HANDS = 2;

    /**
     * The shift of the second gun's mirror, in item-frame units, along both X and Z. The item frame is turned 45
     * degrees from the screen, so the plane that splits the screen down the middle is the slanted plane
     * {@code x + z = 1 - 2 x 0.56 / 0.4 / sqrt(2) x sqrt(2)} of the frame, not a plane of constant X. Reflecting across
     * it sends {@code (x, y, z)} to {@code (-z + s, y, -x + s)} with {@code s = 1 - 2 x 0.56 / (0.4 x sqrt(2))}, which
     * mirrors a gun's camera position to {@code (-right, up, same depth)}. The numbers are the item frame's: camera
     * offset right 0.56, frame scale 0.4, helper shift 0.5 (in-hand-transforms notes, section 0).
     */
    public static final double MIRROR_SHIFT = 1.0 - 2.0 * 0.56 / (0.4 * Math.sqrt(2.0));

    /** The yaw, degrees, that turns the item frame square to the screen for the mirror. */
    public static final double MIRROR_YAW = 45.0;

    private AkimboLayout() {}

    /**
     * @param akimbo      true if the item is an akimbo gun
     * @param firstPerson true for the first-person view
     * @return how many copies of the single model one draw produces: two for akimbo in first person, else one
     */
    public static int handsDrawn(boolean akimbo, boolean firstPerson) {
        return akimbo && firstPerson ? HANDS : 1;
    }

    /**
     * Whether the recoil and muzzle flash windows of the last shot apply to a hand's gun.
     *
     * @param akimbo      true if the item is an akimbo gun
     * @param firstPerson true for the first-person view
     * @param hand        the gun being drawn, 0 main or 1 second
     * @param attackType  the hand that fired the last shot (0 for a non-akimbo gun; any non-zero value reads as the
     *                    second hand)
     * @return true if this gun animates
     */
    public static boolean animates(boolean akimbo, boolean firstPerson, int hand, int attackType) {
        if (!akimbo || !firstPerson) {
            return hand == 0; // the only drawn gun takes every shot
        }
        return hand == (attackType == 0 ? 0 : 1);
    }

    /**
     * As {@link #animates(boolean, boolean, int, int)}, for guns that may fire both hands in one pull.
     *
     * @param akimbo      true if the item is an akimbo gun
     * @param firstPerson true for the first-person view
     * @param hand        the gun being drawn, 0 main or 1 second
     * @param attackType  the hand that fired the last shot
     * @param doubleShot  true if every pull fires both guns (the Akimbo Mac10s), so both recoil
     * @return true if this gun animates
     */
    public static boolean animates(boolean akimbo, boolean firstPerson, int hand, int attackType, boolean doubleShot) {
        if (akimbo && firstPerson && doubleShot) {
            return true;
        }
        return animates(akimbo, firstPerson, hand, attackType);
    }

    /**
     * @param hand 0 main or 1 second
     * @return the X scale of the hand's copy in the screen-aligned frame: 1, or -1 for the mirrored second gun
     */
    public static double mirrorScaleX(int hand) {
        return hand == 0 ? 1.0 : -1.0;
    }

    /**
     * @param hand 0 main or 1 second
     * @return the translation, item-frame units, applied first for the hand's copy: zero for the main gun, else
     *         {@link #MIRROR_SHIFT} on X and Z. The full mirror is translate, rotate Y -{@link #MIRROR_YAW}, scale X
     *         by {@link #mirrorScaleX}, rotate Y {@link #MIRROR_YAW}.
     */
    public static double[] mirrorTranslate(int hand) {
        return hand == 0 ? new double[3] : new double[] { MIRROR_SHIFT, 0.0, MIRROR_SHIFT };
    }

    /**
     * Applies a hand's mirror to a point in the item frame (the same math the GL calls perform).
     *
     * @param p    a point of the main gun, item frame
     * @param hand 0 main or 1 second
     * @return where the point lands
     */
    public static double[] mirrored(double[] p, int hand) {
        if (hand == 0) {
            return p.clone();
        }
        return new double[] { -p[2] + MIRROR_SHIFT, p[1], -p[0] + MIRROR_SHIFT };
    }

    /**
     * Splits the loaded rounds between the hands. The main gun fires when the rounds left are even, so with an odd
     * total it holds the extra round.
     *
     * @param total rounds loaded in the akimbo gun
     * @param hand  0 main or 1 second
     * @return the rounds in that hand's gun
     */
    public static int handAmmo(int total, int hand) {
        int t = Math.max(0, total);
        return hand == 0 ? (t + 1) / 2 : t / 2;
    }
}
