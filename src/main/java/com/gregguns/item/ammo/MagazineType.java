package com.gregguns.item.ammo;

import java.util.Objects;

import com.gregguns.api.Tier;

/**
 * One magazine family: a loaded item, its empty counterpart, and how many loose rounds fill one. A magazine is either
 * full or empty (Techguns behavior kept, ammo.md "Magazine items"); how many shots a loaded magazine gives a gun is
 * the gun's clip size and belongs to the gun, not here.
 *
 * <p>
 * Spec: docs/techguns-spec/06-items/ammo.md (magazine items), docs/gregguns-spec/06-items/recipes.md (refill).
 */
public final class MagazineType {

    private final String id;
    private final AmmoType ammo;
    private final String loadedItemName;
    private final String emptyItemName;
    private final int roundsPerMagazine;
    private final Tier tier;

    /**
     * @param id                short lowercase id guns use, e.g. {@code "9mm_pistol"}
     * @param ammo              the loose round this magazine is filled with
     * @param loadedItemName    registry name of the loaded magazine
     * @param emptyItemName     registry name of the empty magazine
     * @param roundsPerMagazine loose rounds one refill consumes and one unload returns; at least 1
     * @param tier              the magazine's D1 tier
     */
    public MagazineType(String id, AmmoType ammo, String loadedItemName, String emptyItemName, int roundsPerMagazine,
        Tier tier) {
        if (roundsPerMagazine < 1) {
            throw new IllegalArgumentException("roundsPerMagazine must be >= 1: " + roundsPerMagazine);
        }
        this.id = AmmoType.requireId(id);
        this.ammo = Objects.requireNonNull(ammo, "ammo");
        this.loadedItemName = Objects.requireNonNull(loadedItemName, "loadedItemName");
        this.emptyItemName = Objects.requireNonNull(emptyItemName, "emptyItemName");
        this.roundsPerMagazine = roundsPerMagazine;
        this.tier = Objects.requireNonNull(tier, "tier");
    }

    /** @return the id guns use, e.g. {@code "9mm_pistol"} */
    public String id() {
        return id;
    }

    /** @return the loose round type */
    public AmmoType ammo() {
        return ammo;
    }

    /** @return the loaded magazine's registry name */
    public String loadedItemName() {
        return loadedItemName;
    }

    /** @return the empty magazine's registry name */
    public String emptyItemName() {
        return emptyItemName;
    }

    /** @return loose rounds per magazine */
    public int roundsPerMagazine() {
        return roundsPerMagazine;
    }

    /** @return the magazine's tier */
    public Tier tier() {
        return tier;
    }
}
