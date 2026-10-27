package com.gregguns.icbm.component;

import com.gregguns.icbm.emp.EmpMode;
import com.gregguns.icbm.emp.EmpRules;

/**
 * What the {@code gg_emp_tower} component needs from a tower. The real tower implements it, and unit tests use a
 * fake, so the callbacks are checked without a world. No OpenComputers types, server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_emp_tower)
public interface EmpTowerView {

    /** @return the pulse radius, blocks */
    int radius();

    /** @param blocks the requested radius; the tower clamps it */
    void setRadius(int blocks);

    /** @return what a pulse reaches */
    EmpMode mode();

    /** @param mode what a pulse should reach */
    void setMode(EmpMode mode);

    /** @return ticks until the next pulse may go */
    int cooldownLeft();

    /** @return the EU one pulse costs at the current radius */
    long cost();

    /** @return EU stored */
    long stored();

    /** @return how many pulses it has fired since it loaded */
    int pulses();

    /** @return the shared gate right now */
    EmpRules.Gate gate();

    /** @return the gate that decided; {@link EmpRules.Gate#READY} means the pulse went */
    EmpRules.Gate fire();
}
