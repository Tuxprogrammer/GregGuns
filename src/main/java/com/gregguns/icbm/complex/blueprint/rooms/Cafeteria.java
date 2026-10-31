package com.gregguns.icbm.complex.blueprint.rooms;

import com.gregguns.icbm.complex.blueprint.Kit;
import com.gregguns.icbm.complex.blueprint.RoomBlueprint;

/** The cafeteria (D305): the mess galley's layout at a larger size, with two salad bars in the front band. */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing: Cafeteria); D259; D305
public final class Cafeteria implements RoomBlueprint {

    @Override
    public void lay(Kit k) {
        MessGalley.dining(k, true);
    }
}
