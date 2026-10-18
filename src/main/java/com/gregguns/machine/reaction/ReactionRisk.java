package com.gregguns.machine.reaction;

/**
 * What a failed reaction does, besides losing its inputs. Pure data.
 *
 * <p>
 * The kept subset of Techguns' {@code RiskType}: {@code EXPLOSION_HIGH} and {@code UNFORSEEN_CONSEQUENCES} are left out
 * until a recipe needs them.
 */
// Spec: docs/gregguns-spec/08-machines-production/reaction-chamber-multiblock.md (GT5-Unofficial: risks, radiation and
// explosions)
public enum ReactionRisk {

    /** Nothing more than the lost inputs. */
    NONE,
    /** The input item is destroyed (it already is); nothing more. */
    BREAK_ITEM,
    /** A small blast on the controller, the nearby glass ring is lost. */
    EXPLOSION_LOW,
    /** A medium blast on the controller. */
    EXPLOSION_MEDIUM,
    /** Weak radiation around the machine. */
    RAD_LOW,
    /** Medium radiation around the machine. */
    RAD_MEDIUM,
    /** Strong radiation around the machine. */
    RAD_HIGH;

    /** @return whether this risk radiates nearby players */
    public boolean isRadiation() {
        return this == RAD_LOW || this == RAD_MEDIUM || this == RAD_HIGH;
    }

    /** @return whether this risk explodes the controller */
    public boolean isExplosion() {
        return this == EXPLOSION_LOW || this == EXPLOSION_MEDIUM;
    }

    /**
     * @param name a stored name; {@code null} or unknown gives {@link #NONE}
     * @return the risk
     */
    public static ReactionRisk byName(String name) {
        if (name != null) {
            for (ReactionRisk risk : values()) {
                if (risk.name()
                    .equals(name)) {
                    return risk;
                }
            }
        }
        return NONE;
    }
}
