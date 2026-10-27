package com.gregguns.icbm.compat.oc;

import com.gregguns.GregGuns;

import li.cil.oc.api.Driver;

/**
 * Registers the ICBM machines with OpenComputers as components. Loaded only after {@code Loader.isModLoaded
 * ("OpenComputers")} has said yes, so a pack without OpenComputers never touches a class of it (GGI-01-01). Common
 * side, server use only.
 *
 * <p>
 * Deviation: a GT machine's tile entity is GT's {@code BaseMetaTileEntity}, which we cannot make an OpenComputers
 * environment, so each machine is a block driver and a computer reaches it through an Adapter block next to it (the
 * way any other tile without its own environment is reached).
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (Integration points)
public final class OcCompat {

    private OcCompat() {}

    /** Registers the drivers. Call once, from the ICBM module's init, only when OpenComputers is loaded. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/Driver.java#L66
        Driver.add(new EmpTowerDriver());
        GregGuns.LOG.info("ICBM: registered OpenComputers drivers (gg_emp_tower)");
    }
}
