package com.gregguns.client.utility;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.modularui2.GTGuiThemes;
import gregtech.api.modularui2.GTModularScreen;

/** Client only: wraps the charge panel in GT's standard theme, so it matches the machine screens. */
// Spec: docs/gregguns-spec/09-machines-utility/explosive-charges.md (GGS-09-40)
@SideOnly(Side.CLIENT)
public final class ChargeScreens {

    private ChargeScreens() {}

    /**
     * @param panel the charge panel
     * @return the screen
     */
    public static ModularScreen create(ModularPanel panel) {
        return new GTModularScreen(panel, GTGuiThemes.STANDARD);
    }
}
