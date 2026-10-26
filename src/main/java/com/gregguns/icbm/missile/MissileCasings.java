package com.gregguns.icbm.missile;

import com.gregguns.icbm.warhead.WarheadCasing;

/**
 * Looks up a warhead casing by the number a missile entity stores (1 for T1, and so on), so a missile of any tier
 * restores, disarms and renders the casing it carries. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md
public final class MissileCasings {

    private MissileCasings() {}

    /**
     * @param number a stored casing number
     * @return the casing with that {@link WarheadCasing#number()}, or the T1 casing for a number that matches none
     */
    public static WarheadCasing byNumber(int number) {
        for (WarheadCasing c : WarheadCasing.values()) {
            if (c.number() == number) {
                return c;
            }
        }
        return WarheadCasing.T1;
    }
}
