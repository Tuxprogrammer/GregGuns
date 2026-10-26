package com.gregguns.client.render.profile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The client table of aim-down-sights data: for each gun that can aim ({@code com.gregguns.gun.logic.AdsSupport}),
 * which model boxes are its sights. The renderer turns that into a first-person offset with {@link AdsOffset}. Pure
 * data; a test pins this table to {@code AdsSupport.GUNS} and to the box names of the profiles.
 *
 * <p>
 * To tune a gun by eye after a client check, edit its nudge here (texels, in the model's axes: x along the barrel, y
 * down, z across). The nudge moves the point that lands on the screen center, so move it toward the part of the model
 * that should sit at the center. Try one texel at a time.
 *
 * <p>
 * Spec: docs/gregguns-spec/03-gun-visuals-audio/ads.md. Golden Gun wears the 9mm Pistol's model, so it shares its
 * boxes.
 */
public final class AdsTable {

    private static final double[] NONE = { 0.0, 0.0, 0.0 };

    /** Every gun that can aim, one line each. */
    public static final List<AdsSpec> ALL = Collections.unmodifiableList(
        Arrays.asList(
            new AdsSpec("pistol_9mm", NONE, "rear_sight", "front_sight"),
            new AdsSpec("golden_gun", NONE, "rear_sight", "front_sight"),
            new AdsSpec("revolver", NONE, "sight"),
            new AdsSpec("golden_revolver", NONE, "sight_rear", "sight_front"),
            new AdsSpec("thompson_smg", NONE, "front_sight", "rear_leaf_a", "rear_leaf_b"),
            new AdsSpec("mac10", NONE, "sight_post", "front_blade"),
            new AdsSpec("pdw", NONE, "sight_ear_l", "sight_ear_r", "front_post"),
            new AdsSpec("m4", NONE, "red_dot"),
            new AdsSpec("ak47", NONE, "sight_leaf", "sight_tip"),
            new AdsSpec("vector", NONE, "red_dot"),
            new AdsSpec("lmg", NONE, "sight_base", "sight_cap"),
            new AdsSpec("laser_rifle", NONE, "dot"),
            new AdsSpec("tesla_rifle", NONE, "red_dot"),
            new AdsSpec("scatter_laser_rifle", NONE, "sight_glow_window"),
            new AdsSpec("combat_shotgun", NONE, "rear_sight", "front_sight"),
            new AdsSpec("grenade_launcher", NONE, "sight_rear")));

    private AdsTable() {}

    /**
     * @param gunName a gun's registry name without the mod id
     * @return its ADS data, or null if the gun cannot aim
     */
    public static AdsSpec of(String gunName) {
        for (AdsSpec spec : ALL) {
            if (spec.gunName()
                .equals(gunName)) {
                return spec;
            }
        }
        return null;
    }

    /**
     * @param def a gun's client definition
     * @return the first-person item-frame shift that puts its sights on the screen center, or null if the gun cannot
     *         aim (or is akimbo)
     */
    public static double[] shiftOf(ClientGunDefinition def) {
        AdsSpec spec = of(def.gunName());
        if (spec == null || def.akimbo()) {
            return null;
        }
        return AdsOffset.itemShift(def.transforms(), def.boxes(), def.modelScale(), spec);
    }
}
