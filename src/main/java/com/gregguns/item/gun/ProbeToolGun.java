package com.gregguns.item.gun;

import com.gregguns.api.HiddenFromCreativeTab;
import com.gregguns.gun.def.ToolProbeDef;

/** The test-only tool gun item of {@link ToolProbeDef}; dev environment only, hidden from the creative tab. */
public final class ProbeToolGun extends ItemToolGun implements HiddenFromCreativeTab {

    /** Creates the probe. */
    public ProbeToolGun() {
        super(ToolProbeDef.DEFINITION);
    }
}
