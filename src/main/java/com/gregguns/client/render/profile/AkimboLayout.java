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
 * screen centre in first person; in third person only the main-hand gun is drawn (the held-item renderer is handed
 * only the right-hand matrix), and it recoils on every shot so no shot goes unseen.
 */
public final class AkimboLayout {

    /** Hands an akimbo gun has. */
    public static final int HANDS = 2;

    /**
     * Where the screen centre sits on the item frame's X axis in first person, in frame units: the right hand's origin
     * is about 0.56 world units right of the view centre and the frame is scaled by 0.4. Pending the batched in-game
     * check.
     */
    public static final double SCREEN_CENTRE_X = -1.4;

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
     * @return the X scale of the hand's copy: 1, or -1 for the mirrored second gun
     */
    public static double mirrorScaleX(int hand) {
        return hand == 0 ? 1.0 : -1.0;
    }

    /**
     * @param hand 0 main or 1 second
     * @return the X translation, in item-frame units, applied before the mirror scale so the copy lands on the other
     *         side of {@link #SCREEN_CENTRE_X}: 0 for the main gun
     */
    public static double mirrorTranslateX(int hand) {
        return hand == 0 ? 0.0 : 2.0 * SCREEN_CENTRE_X;
    }

    /**
     * Applies a hand's mirror to an X coordinate in the item frame (the same math the GL calls perform).
     *
     * @param x    the coordinate of a point of the main gun
     * @param hand 0 main or 1 second
     * @return where the point lands
     */
    public static double mirroredX(double x, int hand) {
        return mirrorTranslateX(hand) + mirrorScaleX(hand) * x;
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
