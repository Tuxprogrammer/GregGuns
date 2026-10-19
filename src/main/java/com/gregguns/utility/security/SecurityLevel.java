package com.gregguns.utility.security;

/**
 * Who a Turret leaves alone and who may change its settings. Pure logic, both sides, no Minecraft types.
 *
 * <p>
 * The ids are saved in the world and sent in packets, so they never change.
 */
// Spec: docs/techguns-spec/09-machines-utility/turret.md (security level 0 public, 1 friends, 2 owner),
// docs/gregguns-spec/09-machines-utility/turret.md (GGS-09-04)
public enum SecurityLevel {

    /** Level 0, the default after placement: anyone may use it; it shoots mobs only. */
    PUBLIC(0),
    /** Level 1: the owner and the owner's friends may use it; everyone else is a target. */
    FRIENDS(1),
    /** Level 2: only the owner may use it; everyone but the owner's friends is a target. */
    OWNER(2);

    private final int id;

    SecurityLevel(int id) {
        this.id = id;
    }

    /** @return the saved and wire id, 0 to 2 */
    public int id() {
        return id;
    }

    /**
     * @param id a saved or received id
     * @return the level; an unknown id means {@link #PUBLIC}, the default after placement
     */
    public static SecurityLevel fromId(int id) {
        for (SecurityLevel level : values()) {
            if (level.id == id) {
                return level;
            }
        }
        return PUBLIC;
    }

    /** @return the next level in the button cycle PUBLIC, FRIENDS, OWNER, PUBLIC */
    public SecurityLevel next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
