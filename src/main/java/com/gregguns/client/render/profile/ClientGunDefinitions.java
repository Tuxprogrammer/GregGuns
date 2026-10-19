package com.gregguns.client.render.profile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The client side of every gun, one line each. <b>With {@code GunDefinitions.ALL}, this is the only list a new gun
 * touches.</b> {@code GunClientSetup} registers a renderer for each entry; the table-driven tests walk the list.
 *
 * <p>
 * Order does not matter to the game; keep it the same as {@code GunDefinitions.ALL}. Client data, safe to load on
 * either side.
 */
// Spec: rendering-framework.md section 1 (registration)
public final class ClientGunDefinitions {

    /** Every gun's client definition. */
    public static final List<ClientGunDefinition> ALL = Collections.unmodifiableList(
        Arrays.asList(
            Pistol9mmProfile.DEFINITION,
            RevolverProfile.DEFINITION,
            SawedOffProfile.DEFINITION,
            HandCannonProfile.DEFINITION,
            BoltActionRifleProfile.DEFINITION,
            ThompsonProfile.DEFINITION,

            Mac10Profile.DEFINITION,

            M4Profile.DEFINITION,
            InfiltratorProfile.DEFINITION,

            AugProfile.DEFINITION,

            Ak47Profile.DEFINITION,

            VectorProfile.DEFINITION,

            CombatShotgunProfile.DEFINITION,
            RocketLauncherProfile.DEFINITION,
            GrenadeLauncherProfile.DEFINITION,
            As50Profile.DEFINITION,
            LmgProfile.DEFINITION,
            GoldenRevolverProfile.DEFINITION,
            FlamethrowerProfile.DEFINITION,
            ScarProfile.DEFINITION,
            BioGunProfile.DEFINITION,
            NetherBlasterProfile.DEFINITION,
            // One model, two items: each selects its own skin (the profile lists both sheets).
            ClientGunDefinition
                .variantOf("power_hammer", PowerHammerProfile.DEFINITION, PowerHammerProfile.TEXTURES[0]),
            ClientGunDefinition
                .variantOf("power_hammer_adv", PowerHammerProfile.DEFINITION, PowerHammerProfile.TEXTURES[1]),
            ChainsawProfile.DEFINITION,

            // The akimbo guns draw their single gun's model once per hand.
            ClientGunDefinition.akimboOf("akimbo_pistols", Pistol9mmProfile.DEFINITION),
            ClientGunDefinition.akimboOf("akimbo_revolvers", RevolverProfile.DEFINITION),
            ClientGunDefinition.akimboOf("akimbo_mac10s", Mac10Profile.DEFINITION),
            ClientGunDefinition.akimboOf("akimbo_golden_revolvers", GoldenRevolverProfile.DEFINITION)));

    /**
     * The guns that have no 3D profile yet and are drawn as their flat item icon in hand, in the world and in the
     * inventory: nothing is registered for them in {@code GunRenderers}, so vanilla's item rendering draws the icon.
     * Zoom, sounds, recoil timing and the HUD all work without a profile (they fall back to the shared defaults). A
     * test pins this list to the guns that actually lack a profile, and <b>the M2 exit gate requires it empty</b>:
     * giving a gun its clean-room profile means removing its name here.
     */
    public static final List<String> FLAT_ICON_FALLBACK = Collections.unmodifiableList(Arrays.asList());

    private ClientGunDefinitions() {}

    /**
     * @param gunName a gun's registry name without the mod id
     * @return true if the gun has no 3D profile and is drawn as its flat icon
     */
    public static boolean usesFallback(String gunName) {
        return byName(gunName) == null;
    }

    /**
     * @param gunName a gun's registry name without the mod id
     * @return its client definition, or null
     */
    public static ClientGunDefinition byName(String gunName) {
        for (ClientGunDefinition def : ALL) {
            if (def.gunName()
                .equals(gunName)) {
                return def;
            }
        }
        return null;
    }
}
