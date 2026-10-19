package com.gregguns.gun.def;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Every gun, one line each. <b>This list is the only common-side list a new gun touches</b> (the client side has
 * {@code ClientGunDefinitions.ALL}). The gun's item, recipe rows, hooks and scenarios, and every table-driven test,
 * follow from its {@link GunDefinition}.
 *
 * <p>
 * Registry order is list order. Both sides.
 */
// Spec: docs/gregguns-spec/02-guns/index.md
public final class GunDefinitions {

    /** Every gun, in registration order. */
    public static final List<GunDefinition> ALL = Collections.unmodifiableList(
        Arrays.asList(
            PistolDef.DEFINITION,
            RevolverDef.DEFINITION,
            SawedOffDef.DEFINITION,
            HandCannonDef.DEFINITION,
            BoltActionDef.DEFINITION));

    private GunDefinitions() {}

    /**
     * @param name a gun's registry name without the mod id, for example {@code "pistol_9mm"}
     * @return its definition, or null
     */
    public static GunDefinition byName(String name) {
        for (GunDefinition def : ALL) {
            if (def.name()
                .equals(name)) {
                return def;
            }
        }
        return null;
    }
}
