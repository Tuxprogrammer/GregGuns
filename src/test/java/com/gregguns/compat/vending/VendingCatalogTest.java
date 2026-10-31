package com.gregguns.compat.vending;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.item.GunsItemOrder;
import com.gregguns.item.NeiOrder;
import com.gregguns.item.coin.CoinDenomination;

class VendingCatalogTest {

    private static final List<VendingCatalog.Offer> OFFERS = VendingCatalog.build();

    private static Map<String, VendingCatalog.Offer> byKey() {
        Map<String, VendingCatalog.Offer> out = new HashMap<>();
        for (VendingCatalog.Offer o : OFFERS) {
            out.put(o.key(), o);
        }
        return out;
    }

    @Test
    void keysAndGroupIdsAreUniqueAndOurs() {
        Set<String> keys = new HashSet<>();
        Set<UUID> ids = new HashSet<>();
        for (VendingCatalog.Offer o : OFFERS) {
            assertTrue(keys.add(o.key()), "duplicate offer " + o.key());
            UUID id = VendingTradeNbt.groupId(o.key());
            assertTrue(ids.add(id), "group id clash " + o.key());
            assertEquals(VendingTradeNbt.ID_HIGH, id.getMostSignificantBits());
        }
        assertTrue(OFFERS.size() > 150, "the shop should be big: " + OFFERS.size());
    }

    /** The one number the docs quote: config.md and coins-and-vending.md say 176. */
    @Test
    void tradeCountIsTheDocumentedOne() {
        int misc = 0;
        int components = 0;
        for (VendingCatalog.Offer o : OFFERS) {
            if (o.category() == VendingCatalog.Category.MISC) {
                misc++;
            } else {
                components++;
            }
        }
        assertEquals("139 Misc + 37 Components = 176", misc + " Misc + " + components + " Components = " + OFFERS.size());
    }

    @Test
    void onlyEggsOfKindsThatCannotSpawnAreHeldBack() {
        // Nothing is registered in a unit test, so every egg reads as unspawnable; no other offer is touched.
        for (VendingCatalog.Offer o : OFFERS) {
            assertEquals(
                !o.key()
                    .startsWith("egg:"),
                VendingCatalog.spawnable(o),
                o.key());
        }
    }

    @Test
    void everyItemIsAGregGunsItem() {
        Set<String> names = new HashSet<>(NeiOrder.names());
        for (VendingCatalog.Offer o : OFFERS) {
            assertTrue(names.contains(o.item()), o.key() + ": not a GregGuns item name: " + o.item());
            assertTrue(o.count() >= 1 && o.count() <= 64, o.key() + ": count " + o.count());
            assertTrue(
                o.price()
                    .count() <= CoinDenomination.MAX_STACK,
                o.key());
        }
    }

    @Test
    void priceFollowsKindAndTier() {
        Map<String, VendingCatalog.Offer> m = byKey();
        expect(m, "item:pistol_9mm", 1, CoinDenomination.TEN, 6);
        expect(m, "item:handcannon", 1, CoinDenomination.TEN, 3);
        expect(m, "item:ammo_9mm_bullet", 32, CoinDenomination.TEN, 2);
        expect(m, "item:mag_9mm_pistol", 4, CoinDenomination.TEN, 2);
        expect(m, "item:ammo_rocket", 8, CoinDenomination.HUNDRED, 1);
        expect(m, "item:ammo_rocket_nuke", 1, CoinDenomination.TEN_THOUSAND, 1);
        expect(m, "item:part_mechanical_1", 4, CoinDenomination.TEN, 1);
        expect(m, "item:t1_scout_helmet", 1, CoinDenomination.TEN, 2);
        for (VendingCatalog.Offer o : OFFERS) {
            assertEquals(QuestGate.introKey(o.tier()), o.questKey());
        }
    }

    @Test
    void akimboCostsMoreThanTheSingleGun() {
        Map<String, VendingCatalog.Offer> m = byKey();
        VendingCatalog.Offer single = m.get("item:pistol_9mm");
        VendingCatalog.Offer akimbo = m.get("item:akimbo_pistols");
        assertNotNull(akimbo, "the akimbo pistols should be sold");
        assertTrue(
            akimbo.price()
                .value()
                > single.price()
                    .value());
    }

    @Test
    void emptyMagazinesAndHiddenItemsAreNotSold() {
        Set<String> sold = new HashSet<>();
        for (VendingCatalog.Offer o : OFFERS) {
            sold.add(o.item());
        }
        for (GunsItemOrder.Slot slot : GunsItemOrder.build()) {
            if (slot.kind() == GunsItemOrder.Kind.MAGAZINE_EMPTY) {
                assertFalse(sold.contains(slot.name()), slot.name());
            }
            if (slot.kind() == GunsItemOrder.Kind.GUN || slot.kind() == GunsItemOrder.Kind.AMMO) {
                assertTrue(sold.contains(slot.name()), "every gun and round is sold: " + slot.name());
            }
        }
        assertTrue(sold.contains("cell_nuclear"), "the Nuclear Power Cell is a Death Ray input and is sold");
    }

    @Test
    void everyEggMobIsSoldAndBossesAreExpensive() {
        Map<String, VendingCatalog.Offer> m = byKey();
        for (NpcMob mob : NpcMob.values()) {
            VendingCatalog.Offer o = m.get("egg:" + mob.registryName());
            if (!mob.hasEgg() || mob.isDevOnly()) {
                assertEquals(null, o, mob + " has no egg and must not be sold");
                continue;
            }
            assertNotNull(o, mob + ": every egg mob needs an entry in VendingCatalog.EGGS");
            assertEquals(NpcRegistry.EGG_NAME, o.item());
            assertEquals(mob.entityId(), o.meta());
            assertEquals(1, o.count());
        }
        NpcMob[] bosses = { NpcMob.ATTACK_HELICOPTER, NpcMob.CYBER_DEMON, NpcMob.ALIEN_BUG, NpcMob.DICTATOR_DAVE };
        long cheapestBoss = Long.MAX_VALUE;
        for (NpcMob boss : bosses) {
            VendingCatalog.Offer o = m.get("egg:" + boss.registryName());
            assertTrue(
                o.price()
                    .value() >= 10L * VendingCatalog.unit(o.tier()),
                boss + " costs at least ten units of its tier");
            cheapestBoss = Math.min(
                cheapestBoss,
                o.price()
                    .value());
        }
        for (VendingCatalog.Offer o : OFFERS) {
            if (o.key()
                .startsWith("egg:")
                && !o.key()
                    .contains("AttackHelicopter")
                && !o.key()
                    .contains("CyberDemon")
                && !o.key()
                    .contains("AlienBug")
                && !o.key()
                    .contains("DictatorDave")) {
                assertTrue(
                    o.price()
                        .value() < cheapestBoss,
                    o.key() + " should be cheaper than any boss");
            }
        }
        assertEquals(
            2000L,
            m.get("egg:CyberDemon")
                .price()
                .value());
        assertEquals(
            5000L,
            m.get("egg:DictatorDave")
                .price()
                .value());
    }

    @Test
    void unitsRiseWithTier() {
        int last = 0;
        for (Tier t : Tier.values()) {
            int u = VendingCatalog.unit(t);
            assertTrue(u > last, t + " unit " + u);
            assertEquals(0, u % 2, "half a unit must be a whole coin: " + t);
            last = u;
        }
    }

    private static void expect(Map<String, VendingCatalog.Offer> m, String key, int count, CoinDenomination d,
        int coins) {
        VendingCatalog.Offer o = m.get(key);
        assertNotNull(o, "missing " + key);
        assertEquals(count, o.count(), key + " count");
        assertEquals(
            d,
            o.price()
                .denomination(),
            key + " denomination");
        assertEquals(
            coins,
            o.price()
                .count(),
            key + " coins");
    }
}
