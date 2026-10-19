package com.gregguns.client.render.profile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Where each M2 gun should sit and how big it should look in the player's hands: one table, as data. Pure.
 *
 * <p>
 * Every number is a target measured from Techguns' behavior and written down in
 * {@code docs/techguns-spec/03-gun-visuals-audio/model-notes/in-hand-transforms.md} section 7; none is code or an asset
 * copy. {@link InHandFit} solves each gun's scale and idle offset from these, so a gun's profile carries no
 * hand-tuned first-person scale of its own.
 *
 * <h2>Units</h2>
 * <ul>
 * <li><b>Camera blocks</b>: first-person coordinates {@code (right, up, depth)}; depth is the distance in front of the
 * camera. A length in camera blocks is 0.4 of the same length in item-frame ("H") blocks.</li>
 * <li><b>World blocks</b>: third-person lengths. One H-block is 0.375 world blocks.</li>
 * </ul>
 *
 * <p>
 * Spec: model-notes/in-hand-transforms.md sections 0, 2, 3 and 7.
 */
public final class InHandTargets {

    /**
     * One gun's targets.
     *
     * @param fpLength first-person length from rear tip to muzzle tip, camera blocks
     * @param fpCentre first-person box centre {@code (right, up, depth)}, camera blocks
     * @param fpMuzzle first-person muzzle {@code (right, up, depth)}, camera blocks
     * @param tpLength third-person length, world blocks
     */
    public static final class Target {

        private final double fpLength;
        private final double[] fpCentre;
        private final double[] fpMuzzle;
        private final double tpLength;

        Target(double fpLength, double[] fpCentre, double[] fpMuzzle, double tpLength) {
            this.fpLength = fpLength;
            this.fpCentre = fpCentre;
            this.fpMuzzle = fpMuzzle;
            this.tpLength = tpLength;
        }

        /** @return first-person length, camera blocks */
        public double fpLength() {
            return fpLength;
        }

        /** @return first-person box centre (right, up, depth), camera blocks */
        public double[] fpCentre() {
            return fpCentre.clone();
        }

        /** @return first-person muzzle (right, up, depth), camera blocks */
        public double[] fpMuzzle() {
            return fpMuzzle.clone();
        }

        /** @return third-person length, world blocks */
        public double tpLength() {
            return tpLength;
        }
    }

    private static final Map<String, Target> TABLE = new HashMap<>();

    static {
        // name, fp length, centre (R, U, D), muzzle (R, U, D), tp length. Section 7 of the in-hand notes.
        put("handcannon", 2.44, 0.75, -0.78, 0.98, 0.75, -0.72, 2.20, 1.37);
        put("pistol_9mm", 0.75, 0.74, -0.59, 1.14, 0.74, -0.44, 1.51, 0.53);
        put("revolver", 1.05, 0.51, -0.58, 1.18, 0.51, -0.38, 1.70, 0.56);
        put("golden_revolver", 1.09, 0.51, -0.58, 1.20, 0.51, -0.37, 1.74, 0.58);
        put("sawed_off", 2.15, 0.69, -0.66, 0.95, 0.69, -0.55, 2.02, 1.37);
        put("mac10", 1.10, 0.74, -0.71, 1.11, 0.74, -0.41, 1.66, 0.62);
        put("thompson_smg", 2.34, 0.59, -0.63, 0.84, 0.59, -0.40, 2.01, 1.40);
        put("m4", 2.42, 0.60, -0.57, 1.31, 0.60, -0.42, 2.52, 1.29);
        // Deviation: the notes give 3.81 camera blocks for a 109-texel Infiltrator; ours is the 76-texel rifle part, so
        // the M4's ratio is used (2.42 per 69 texels = 2.665), the centre keeps the M4's right and up, and the butt
        // stays on the camera plane (centre and muzzle depths grow by half the extra length).
        put("infiltrator", 2.665, 0.60, -0.57, 1.43, 0.60, -0.42, 2.76, 1.42);
        put("ak47", 3.43, 0.60, -0.67, 1.40, 0.60, -0.45, 3.11, 1.61);
        put("aug", 2.45, 0.59, -0.57, 1.29, 0.60, -0.42, 2.52, 1.31);
        put("scar", 2.51, 0.59, -0.50, 1.37, 0.60, -0.43, 2.62, 1.35);
        put("vector", 1.54, 0.66, -0.61, 0.91, 0.66, -0.45, 1.68, 0.83);
        put("as50", 3.50, 0.61, -0.36, 1.68, 0.60, -0.33, 3.43, 1.64);
        put("lmg", 2.55, 0.62, -0.52, 1.35, 0.60, -0.38, 2.62, 1.37);
        put("combat_shotgun", 3.09, 0.70, -0.65, 1.06, 0.70, -0.41, 2.61, 1.48);
        put("rocket_launcher", 2.70, 0.70, -0.63, 1.14, 0.70, -0.52, 2.49, 1.69);
        put("grenade_launcher", 2.20, 0.96, -0.72, 1.36, 0.96, -0.63, 2.46, 1.34);
        put("flamethrower", 2.15, 0.81, -0.62, 1.33, 0.81, -0.47, 2.41, 1.51);
        put("laser_rifle", 1.72, 0.50, -0.40, 0.92, 0.50, -0.33, 1.78, 1.55);
        put("tesla_rifle", 1.61, 0.52, -0.44, 0.68, 0.52, -0.35, 1.49, 1.45);
        put("sonic_rifle", 2.95, 0.79, -0.73, 1.16, 0.79, -0.51, 2.63, 1.73);
        put("pdw", 1.68, 0.78, -0.70, 1.36, 0.78, -0.55, 2.20, 0.94);
        put("pulse_rifle", 2.52, 0.78, -0.66, 1.41, 0.78, -0.55, 2.67, 1.41);
        put("minigun", 2.33, 0.71, -0.53, 1.40, 0.71, -0.54, 2.57, 1.82);
        put("bio_gun", 1.90, 0.57, -0.78, 0.52, 0.57, -0.54, 1.47, 1.34);
        put("bolt_action_rifle", 2.01, 0.54, -0.35, 1.02, 0.52, -0.29, 2.02, 1.88);
        // M5 batch B4 (IV energy guns); the akimbo item draws the pistol twice and has no row of its own.
        put("blaster_rifle", 1.75, 0.41, -0.49, 1.12, 0.41, -0.38, 1.99, 1.11);
        put("deatomizer_pistol", 1.12, 1.03, -0.76, 1.65, 1.03, -0.61, 2.21, 0.63);
        put("scatter_laser_rifle", 2.20, 0.59, -0.50, 1.21, 0.59, -0.33, 2.31, 1.18);
        // M5 tool guns (section 7; third-person length from section 3). The Advanced hammer has the same geometry.
        put("power_hammer", 1.16, 0.34, -0.40, 0.70, 0.34, -0.33, 1.28, 1.05);
        put("power_hammer_adv", 1.16, 0.34, -0.40, 0.70, 0.34, -0.33, 1.28, 1.05);
        // Deviation: the bar is 5.5 texels off the box centre's side, so with the centre on 1.06 the bar tip lands at
        // 1.26
        // right (the notes put the tip on the centre line); the centre, length and depth keep the notes' figures.
        put("chainsaw", 2.36, 1.06, -0.58, 1.75, 1.26, -0.74, 2.93, 2.13);
    }

    private InHandTargets() {}

    private static void put(String name, double len, double cr, double cu, double cd, double mr, double mu, double md,
        double tpLen) {
        TABLE.put(name, new Target(len, new double[] { cr, cu, cd }, new double[] { mr, mu, md }, tpLen));
    }

    /**
     * @param gunName a gun's registry name without the mod id
     * @return its targets, or null if the gun is not on the table (it keeps {@link FirstPersonFit}'s class rule)
     */
    public static Target of(String gunName) {
        return TABLE.get(gunName);
    }

    /** @return the names of the guns on the table */
    public static Iterable<String> names() {
        return Collections.unmodifiableSet(TABLE.keySet());
    }
}
