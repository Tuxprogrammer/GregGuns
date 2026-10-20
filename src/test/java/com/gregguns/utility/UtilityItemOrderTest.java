package com.gregguns.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.utility.UtilityItemOrder.Kind;
import com.gregguns.utility.UtilityItemOrder.Slot;

class UtilityItemOrderTest {

    private final List<Slot> order = UtilityItemOrder.build();

    @Test
    void platesComeFirstFromTheLowestTierUp() {
        List<Tier> tiers = new ArrayList<>();
        int i = 0;
        for (; i < order.size() && order.get(i)
            .kind() == Kind.PLATE; i++) {
            tiers.add(
                order.get(i)
                    .tier());
        }
        assertEquals(TurretPlate.values().length, i, "every plate comes before any block");
        for (int j = 1; j < tiers.size(); j++) {
            assertTrue(
                tiers.get(j)
                    .ordinal()
                    >= tiers.get(j - 1)
                        .ordinal());
        }
    }

    @Test
    void blocksFollowChargeStorageFlagBlob() {
        List<String> names = new ArrayList<>();
        for (Slot slot : order) {
            if (slot.kind() != Kind.PLATE) {
                names.add(slot.name());
            }
        }
        assertEquals(
            java.util.Arrays.asList(
                "explosive_charge",
                WorldBlockRegistration.REINFORCED_CHEST,
                WorldBlockRegistration.WEAPON_CHEST,
                WorldBlockRegistration.CAMP_FLAG,
                WorldBlockRegistration.BIO_BLOB),
            names);
    }

    @Test
    void everyItemAppearsOnce() {
        Set<String> seen = new HashSet<>();
        for (Slot slot : order) {
            assertTrue(seen.add(slot.name()), "duplicate " + slot.name());
        }
    }
}
