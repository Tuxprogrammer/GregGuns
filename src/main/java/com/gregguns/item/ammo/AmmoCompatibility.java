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
    private final Map<String, List<Use>> usesByItem = new LinkedHashMap<>();
    private final Map<String, List<Use>> costsByRound = new LinkedHashMap<>();

    /** One name with a count: a gun and the rounds an item gives it, or a magazine and the loose items it takes. */
    public static final class Use {

        private final String name;
        private final int count;

        Use(String name, int count) {
            this.name = name;
            this.count = count;
        }

        /** @return the item registry name (a gun, or a magazine) */
        public String name() {
            return name;
        }

        /** @return the rounds a gun gets per item, or the loose items a magazine takes */
        public int count() {
            return count;
        }
    }

    /**
     * @param ammo the ammo definitions
     * @param guns the gun definitions
     */
    public AmmoCompatibility(List<AmmoDefinition> ammo, List<GunDefinition> guns) {
        for (AmmoDefinition def : ammo) {
            List<String> mags = new ArrayList<>();
            List<Use> costs = new ArrayList<>();
            for (MagazineType magazine : def.magazines()) {
                mags.add(magazine.loadedItemName());
                costs.add(new Use(magazine.loadedItemName(), magazine.roundsPerMagazine()));
            }
            costsByRound.put(
                def.type()
                    .roundItemName(),
                costs);
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
            usesByItem.computeIfAbsent(
                gun.stats()
                    .ammoType(),
                k -> new ArrayList<>())
                .add(
                    new Use(
                        gun.name(),
                        gun.stats()
                            .roundsPerItem()));
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

    /**
     * @param itemName a loaded magazine, tank, charge or loose round registry name
     * @return each gun that consumes it with the rounds one item gives that gun (its clip divided by the items one
     *         reload takes), in gun order
     */
    public List<Use> roundsPerGun(String itemName) {
        List<Use> found = usesByItem.get(itemName);
        return found == null ? Collections.<Use>emptyList() : Collections.unmodifiableList(found);
    }

    /**
     * @param roundName a loose round's registry name
     * @return each loaded magazine it fills with the loose items one magazine takes, in definition order
     */
    public List<Use> magazineCosts(String roundName) {
        List<Use> found = costsByRound.get(roundName);
        return found == null ? Collections.<Use>emptyList() : Collections.unmodifiableList(found);
    }

    private static List<String> get(Map<String, List<String>> map, String key) {
        List<String> found = map.get(key);
        return found == null ? Collections.<String>emptyList() : Collections.unmodifiableList(found);
    }
}
