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
            BoltActionRifleProfile.DEFINITION));

    private ClientGunDefinitions() {}

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
