package com.gregguns.item.ammo.def;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Every ammo type, one line each. This list and the definition class it names are all a new caliber touches (plus the
 * item's lang line and texture): the items are registered, the {@code AmmoTypes} index, the press rows, the refills
 * and the recycling hooks all follow from it.
 *
 * <p>
 * Registry order is list order. Both sides.
 */
// Spec: docs/gregguns-spec/06-items/ammo.md
public final class AmmoDefinitions {

    /** Every ammo type, in registration order. */
    public static final List<AmmoDefinition> ALL = Collections.unmodifiableList(
        Arrays.asList(
            RifleAmmo.DEFINITION,
            NineMmAmmo.DEFINITION,
            StoneAmmo.DEFINITION,
            ShotgunShellAmmo.DEFINITION,
            RocketAmmo.DEFINITION,
            Grenade40mmAmmo.DEFINITION,
            HighCaliberAmmo.DEFINITION));

    private AmmoDefinitions() {}

    /**
     * @param id an ammo id such as {@code "9mm"}
     * @return its definition, or null
     */
    public static AmmoDefinition byId(String id) {
        for (AmmoDefinition def : ALL) {
            if (def.type()
                .id()
                .equals(id)) {
                return def;
            }
        }
        return null;
    }
}
