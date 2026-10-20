package com.gregguns.item.ammo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.ammo.def.AmmoDefinition;
import com.gregguns.item.ammo.def.AmmoDefinitions;

/**
 * Which magazines a round fills and which guns take a round or magazine, derived from the ammo and gun definitions so
 * a tooltip can never drift from the data. Pure: item registry names in, item registry names out, no Minecraft types.
 *
 * <p>
 * A gun's {@code ammoType} is the registry name of whatever it consumes: a loaded magazine, a loose round or a tank.
 * Energy guns take no ammo item and never appear.
 *
 * <p>
 * Usage: {@code AmmoCompatibility.DEFAULT.gunsTaking("mag_smg")}.
 */
// Spec: docs/gregguns-spec/06-items/ammo.md (tooltip compatibility notes, a GregGuns addition)
public final class AmmoCompatibility {

    /** The compatibility of every registered definition. */
    public static final AmmoCompatibility DEFAULT = new AmmoCompatibility(AmmoDefinitions.ALL, GunDefinitions.ALL);

    private final Map<String, List<String>> magazinesByRound = new LinkedHashMap<>();
    private final Map<String, List<String>> gunsByItem = new LinkedHashMap<>();

    /**
     * @param ammo the ammo definitions
     * @param guns the gun definitions
     */
    public AmmoCompatibility(List<AmmoDefinition> ammo, List<GunDefinition> guns) {
        for (AmmoDefinition def : ammo) {
            List<String> mags = new ArrayList<>();
            for (MagazineType magazine : def.magazines()) {
                mags.add(magazine.loadedItemName());
            }
            magazinesByRound.put(
                def.type()
                    .roundItemName(),
                mags);
        }
        for (GunDefinition gun : guns) {
            if (gun.stats()
                .isEnergy()) {
                continue;
            }
            gunsByItem.computeIfAbsent(
                gun.stats()
                    .ammoType(),
                k -> new ArrayList<>())
                .add(gun.name());
        }
    }

    /**
     * @param roundName a loose round's registry name
     * @return the loaded magazines filled with it, in definition order; empty if none
     */
    public List<String> magazinesFilledWith(String roundName) {
        return get(magazinesByRound, roundName);
    }

    /**
     * @param itemName a round, loaded magazine or tank registry name
     * @return the guns that consume that item directly, in gun order; empty if none
     */
    public List<String> gunsTaking(String itemName) {
        return get(gunsByItem, itemName);
    }

    /**
     * @param magazine a magazine family
     * @return the guns that use that magazine
     */
    public List<String> gunsForMagazine(MagazineType magazine) {
        return gunsTaking(magazine.loadedItemName());
    }

    private static List<String> get(Map<String, List<String>> map, String key) {
        List<String> found = map.get(key);
        return found == null ? Collections.<String>emptyList() : Collections.unmodifiableList(found);
    }
}
