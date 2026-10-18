package com.gregguns.item.ammo;

import java.util.Objects;

import com.gregguns.api.Tier;

/**
 * One loose-round calibre, for example 9mm. Plain data with no Minecraft types, so it is unit-testable and shared by
 * both sides. The {@link #id()} is the key guns use to say which ammo they take.
 *
 * <p>
 * Spec: docs/techguns-spec/06-items/ammo.md (gun to ammo table), docs/design/0001-tier-map.md (ammo tiers).
 */
public final class AmmoType {

    private final String id;
    private final String roundItemName;
    private final Tier tier;

    /**
     * @param id            short lowercase id, e.g. {@code "9mm"}
     * @param roundItemName registry name (without the {@code gregguns:} prefix) of the loose round item
     * @param tier          the round's D1 tier
     */
    public AmmoType(String id, String roundItemName, Tier tier) {
        this.id = requireId(id);
        this.roundItemName = Objects.requireNonNull(roundItemName, "roundItemName");
        this.tier = Objects.requireNonNull(tier, "tier");
    }

    static String requireId(String id) {
        if (id == null || !id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("ammo id must be lower case [a-z0-9_]+: " + id);
        }
        return id;
    }

    /** @return the id guns use, e.g. {@code "9mm"} */
    public String id() {
        return id;
    }

    /** @return the loose round's registry name, e.g. {@code "ammo_9mm_bullet"} */
    public String roundItemName() {
        return roundItemName;
    }

    /** @return the round's tier */
    public Tier tier() {
        return tier;
    }
}
