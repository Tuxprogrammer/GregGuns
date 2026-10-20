package com.gregguns.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.item.GunsItemOrder.Kind;
import com.gregguns.item.GunsItemOrder.Slot;
import com.gregguns.item.ammo.def.AmmoDefinition;
import com.gregguns.item.ammo.def.AmmoDefinitions;

class GunsItemOrderTest {

    private final List<Slot> order = GunsItemOrder.build();

    private int indexOf(String name) {
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i)
                .name()
                .equals(name)) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void tierNeverDecreasesDownTheList() {
        Tier previous = Tier.STEAM;
        for (Slot slot : order) {
            assertTrue(
                slot.tier()
                    .compareTo(previous) >= 0,
                slot.name() + " (" + slot.tier() + ") after " + previous);
            previous = slot.tier();
        }
    }

    @Test
    void everyItemIsListedExactlyOnce() {
        Set<String> seen = new HashSet<>();
        for (Slot slot : order) {
            assertTrue(seen.add(slot.name()), "duplicate " + slot.name());
        }
        for (GunDefinition gun : GunDefinitions.ALL) {
            assertTrue(seen.contains(gun.name()), "missing gun " + gun.name());
        }
        for (AmmoDefinition def : AmmoDefinitions.ALL) {
            assertTrue(
                seen.contains(
                    def.type()
                        .roundItemName()),
                "missing " + def.type()
                    .roundItemName());
            def.magazines()
                .forEach(m -> {
                    assertTrue(seen.contains(m.emptyItemName()), "missing " + m.emptyItemName());
                    assertTrue(seen.contains(m.loadedItemName()), "missing " + m.loadedItemName());
                });
        }
    }

    @Test
    void gunTierMatchesItsDefinition() {
        for (GunDefinition gun : GunDefinitions.ALL) {
            assertEquals(
                gun.tier(),
                order.get(indexOf(gun.name()))
                    .tier(),
                gun.name());
        }
    }

    @Test
    void magazinesAndAmmoFollowTheirGunWithinTheTierGroup() {
        for (Slot slot : order) {
            if (slot.gun() == null) {
                continue;
            }
            int gunIndex = indexOf(slot.gun());
            int itemIndex = indexOf(slot.name());
            assertTrue(gunIndex >= 0 && gunIndex < itemIndex, slot.name() + " must come after " + slot.gun());
            assertEquals(
                order.get(gunIndex)
                    .tier(),
                slot.tier(),
                slot.name() + " must share " + slot.gun() + "'s tier");
        }
    }

    @Test
    void akimboVariantFollowsItsSingleGunDirectly() {
        for (GunDefinition gun : GunDefinitions.ALL) {
            if (gun.singleGun() == null) {
                continue;
            }
            GunDefinition single = GunDefinitions.byName(gun.singleGun());
            if (single.tier() == gun.tier()) {
                assertEquals(indexOf(single.name()) + 1, indexOf(gun.name()), gun.name());
            }
        }
    }

    @Test
    void emptyMagazineComesBeforeItsFullMagazine() {
        for (AmmoDefinition def : AmmoDefinitions.ALL) {
            def.magazines()
                .forEach(m -> assertTrue(indexOf(m.emptyItemName()) + 1 == indexOf(m.loadedItemName()), m.id()));
        }
    }

    @Test
    void pistolFamilyReadsInBuildOrder() {
        // 9mm Pistol, its akimbo pair, then the empty and full magazine, then the bullet.
        assertEquals(
            Kind.GUN,
            order.get(indexOf("pistol_9mm"))
                .kind());
        int pistol = indexOf("pistol_9mm");
        assertTrue(indexOf("mag_9mm_pistol_empty") > pistol);
        assertTrue(indexOf("ammo_9mm_bullet") > indexOf("mag_9mm_pistol"));
    }
}
