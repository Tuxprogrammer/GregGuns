package com.gregguns.icbm.silo;

import java.util.Collections;
import java.util.List;

import net.minecraft.util.StatCollector;

import com.gregguns.icbm.control.MTELaunchControl;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.util.IGTHatchAdder;

/**
 * The Launch Control as a part of the Missile Silo's structure (D285): a hatch-like element every hatch position of the
 * silo accepts (walls, glass rings, floor ring), so a player builds the Launch Control into the silo in place of any
 * one of those blocks. It is optional and the silo takes at most one: a second one is refused by the adder, its
 * position then fails like a wrong block, and the silo does not form. The structure check binds the block to the
 * controller ({@link MTEMissileSilo#addLaunchControl}), so the silo knows its Launch Control and the Launch Control
 * knows its silo, with no search. The block keeps its own texture (no casing index is applied). Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (Structure),
// docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (Finding the pad or the silo)
// Source (the pattern: a custom IHatchElement in a hatch adder's atLeast list; MTEAmmoWorks.BenchHatch in this repo):
// https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/interfaces/IHatchElement.java
public enum SiloLaunchControl implements IHatchElement<MTEMissileSilo> {

    /** The one element. */
    ELEMENT;

    private static final String NAME_KEY = "gregguns.machine.missile_silo.structure.launch_control.element";

    @Override
    public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
        return Collections.singletonList(MTELaunchControl.class);
    }

    @Override
    public IGTHatchAdder<? super MTEMissileSilo> adder() {
        return (silo, tile, casingIndex) -> silo.addLaunchControl(tile);
    }

    @Override
    public long count(MTEMissileSilo silo) {
        return silo.launchControl() == null ? 0 : 1;
    }

    /** Our own lang key, so GT writes nothing into its GregTech.lang (D65). */
    @Override
    public String getDisplayName() {
        return StatCollector.translateToLocal(NAME_KEY);
    }

    @Override
    public String getDescriptionLangKey() {
        return NAME_KEY;
    }
}
