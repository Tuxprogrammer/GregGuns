package com.gregguns.item.gun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.def.AkimboRevolversDef;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.def.RevolverDef;
import com.gregguns.gun.logic.AkimboNbt;
import com.gregguns.gun.logic.GunController;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ReloadMode;
import com.gregguns.gun.logic.ReloadStateMachine;
import com.gregguns.item.ammo.AmmoTypes;
import com.gregguns.item.ammo.MagazineLogic;
import com.gregguns.item.ammo.MagazineType;

/**
 * The no-dupe proof for the reload rules of design record 0026, run against the real state machine, controller and
 * {@link StoreAmmoInventory} refund.
 *
 * <h2>Magazine-fed guns</h2>
 * Measure everything in loose rounds a player could get back: {@code L = loose + roundsPerMagazine x loaded magazines
 * + floor(rounds in the gun / R)}, where {@code R = GunItems.roundsPerLooseItem} is the forced-reload refund rate.
 * <ul>
 * <li>Refilling or unloading a magazine moves {@code roundsPerMagazine} loose rounds in or out: L is unchanged.</li>
 * <li>A magazine swap with {@code g} rounds in the gun refunds {@code floor(g / R)} and loads a full gun, so L changes
 * by {@code floor(clip / R) - itemsPerReload x roundsPerMagazine}, which is at most 0
 * ({@link #aFullGunNeverRefundsMoreThanTheMagazinesItTook}).</li>
 * <li>A single-round load takes one loose item per round and adds as many rounds, so L changes by
 * {@code floor((g + n) / R) - floor(g / R) - n}, at most 0 because {@code R >= 1}.</li>
 * <li>Firing only lowers L.</li>
 * </ul>
 * So no sequence of these raises L: {@link #noSequenceOfReloadsRefillsAndShotsCreatesAmmo} drives random sequences
 * and checks it after every step.
 *
 * <h2>Chamber guns</h2>
 * A chamber gun never hands rounds back, and an item becomes exactly {@code roundsPerItem} rounds (one now, the rest
 * as credit), so {@code loose x roundsPerItem + loaded + credit + fired} is constant: nothing is made and nothing lost,
 * whatever the interrupts ({@link #chamberGunsConserveEveryRoundUnderInterrupts}). The akimbo combine and split carry
 * the credit along ({@link #theAkimboRecipesKeepTheChamberCredit}).
 */
class ReloadEconomyTest {

    private static MagazineType magazineOf(GunStats stats) {
        for (MagazineType m : AmmoTypes.allMagazines()
            .values()) {
            if (m.loadedItemName()
                .equals(stats.ammoType())) {
                return m;
            }
        }
        return null;
    }

    @Test
    void aFullGunNeverRefundsMoreThanTheMagazinesItTook() {
        int checked = 0;
        for (GunDefinition def : GunDefinitions.ALL) {
            GunStats s = def.stats();
            if (!s.loadsSingleRounds()) {
                continue;
            }
            MagazineType mag = magazineOf(s);
            assertNotNull(mag, def.name() + " has no magazine type for " + s.ammoType());
            int r = GunItems.roundsPerLooseItem(s);
            assertTrue(r >= 1, def.name());
            assertTrue(
                s.magazineSize() / r <= s.ammoItemsPerReload() * mag.roundsPerMagazine(),
                def.name() + ": a full gun refunds "
                    + s.magazineSize() / r
                    + " loose rounds but its magazines took "
                    + s.ammoItemsPerReload() * mag.roundsPerMagazine());
            checked++;
        }
        assertTrue(checked >= 16, "every magazine-fed gun is checked: " + checked);
    }

    @Test
    void noSequenceOfReloadsRefillsAndShotsCreatesAmmo() {
        for (GunDefinition def : GunDefinitions.ALL) {
            GunStats s = def.stats();
            if (!s.loadsSingleRounds()) {
                continue;
            }
            MagazineType mag = magazineOf(s);
            int r = GunItems.roundsPerLooseItem(s);
            String loose = s.looseAmmoType();
            String loaded = s.ammoType();
            String empty = s.emptyContainerType();
            Random rng = new Random(
                def.name()
                    .hashCode());
            StoreAmmoInventoryTest.FakeStore store = new StoreAmmoInventoryTest.FakeStore().with(loose, 40)
                .with(loaded, 2)
                .with(empty, 1);
            StoreAmmoInventory inv = new StoreAmmoInventory(store, r);
            GunState gun = new GunState(0);
            int bodies = store.count(loaded) + store.count(empty);
            long last = value(store, mag, loose, loaded, gun, r);
            for (int step = 0; step < 3000; step++) {
                int op = rng.nextInt(6);
                String what;
                if (op == 0) {
                    what = "fire";
                    gun.use(1 + rng.nextInt(Math.max(1, s.ammoPerShot() * 3)));
                } else if (op == 1 || op == 2) {
                    ReloadStateMachine m = new ReloadStateMachine();
                    what = "reload";
                    if (m.start(s, gun, inv) == ReloadStateMachine.Event.STARTED) {
                        what = "reload " + m.load();
                        int ticks = rng.nextInt(4) == 0 ? rng.nextInt(m.totalTicks()) : m.totalTicks();
                        for (int t = 0; t < ticks; t++) {
                            m.tick(s, gun, inv);
                        }
                        m.cancel(ReloadStateMachine.CancelReason.WEAPON_SWITCH);
                    }
                } else if (op == 3) {
                    what = "refill";
                    int n = MagazineLogic.fillable(mag, store.count(empty), store.count(loose));
                    if (n > 0) {
                        store.remove(empty, 1);
                        store.remove(loose, MagazineLogic.roundsConsumed(mag, 1));
                        store.add(loaded, 1);
                    }
                } else if (op == 4) {
                    what = "unload";
                    if (store.count(loaded) > 0) {
                        store.remove(loaded, 1);
                        store.add(empty, 1);
                        store.add(loose, MagazineLogic.roundsReturned(mag, 1));
                    }
                } else {
                    what = "top up loose";
                    if (rng.nextInt(10) == 0) {
                        store.add(loose, 1); // a pickup; counted as new value below
                        last++;
                    }
                }
                long now = value(store, mag, loose, loaded, gun, r);
                assertTrue(
                    now <= last,
                    def.name() + " step " + step + " (" + what + ") made ammo: " + last + " -> " + now);
                assertEquals(bodies, store.count(loaded) + store.count(empty), def.name() + ": magazines conserved");
                assertTrue(store.dropped.isEmpty(), "nothing dropped");
                last = now;
            }
        }
    }

    private static long value(StoreAmmoInventoryTest.FakeStore store, MagazineType mag, String loose, String loaded,
        GunState gun, int r) {
        return store.count(loose) + (long) mag.roundsPerMagazine() * store.count(loaded) + gun.loaded() / r;
    }

    @Test
    void chamberGunsConserveEveryRoundUnderInterrupts() {
        for (GunDefinition def : GunDefinitions.ALL) {
            GunStats s = def.stats();
            if (s.reloadMode() != ReloadMode.CHAMBER) {
                continue;
            }
            int perItem = s.roundsPerItem();
            Random rng = new Random(
                def.name()
                    .hashCode());
            StoreAmmoInventoryTest.FakeStore store = new StoreAmmoInventoryTest.FakeStore().with(s.ammoType(), 30);
            StoreAmmoInventory inv = new StoreAmmoInventory(store, GunItems.roundsPerLooseItem(s));
            GunController c = new GunController();
            GunState gun = new GunState(0);
            long total = 30L * perItem;
            long fired = 0;
            for (int t = 0; t < 6000; t++) {
                int roll = rng.nextInt(40);
                if (roll == 0) {
                    c.triggerPressed();
                } else if (roll == 1) {
                    c.triggerReleased();
                } else if (roll == 2) {
                    c.forceReload(s, gun, inv);
                }
                boolean held = rng.nextInt(200) != 0;
                int before = gun.loaded();
                boolean shot = c.tick(s, gun, inv, held, false)
                    .shot()
                    .wasFired();
                if (shot) {
                    // A shot never shares a tick with a chamber going in (the reload is canceled first).
                    fired += before - gun.loaded();
                }
                c.idleReload(s, gun, inv, 10);
                long now = (long) store.count(s.ammoType()) * perItem + gun.loaded() + gun.credit() + fired;
                assertEquals(total, now, def.name() + " tick " + t);
                assertTrue(gun.credit() < perItem, "credit stays under one item");
            }
            assertTrue(fired > 0, def.name() + " fired");
        }
    }

    @Test
    void theAkimboRecipesKeepTheChamberCredit() {
        GunStats single = RevolverDef.STATS;
        GunStats pair = AkimboRevolversDef.STATS;
        NBTTagCompound a = new NBTTagCompound();
        GunState sa = new GunState(4);
        sa.setCredit(single, 3);
        GunNbt.write(a, sa);
        NBTTagCompound b = new NBTTagCompound();
        GunState sb = new GunState(1);
        sb.setCredit(single, 5);
        GunNbt.write(b, sb);

        NBTTagCompound combined = AkimboNbt.combine(a, b, single, pair);
        GunState both = GunNbt.read(combined, pair);
        assertEquals(5, both.loaded());
        assertEquals(3, both.credit(), "the pair loads from the first gun's credit");

        both.setCredit(pair, 1); // the pair loaded two chambers from it
        both.setLoaded(pair, 7);
        GunNbt.write(combined, both);
        NBTTagCompound[] split = AkimboNbt.split(combined, single, pair);
        GunState first = GunNbt.read(split[0], single);
        GunState second = GunNbt.read(split[1], single);
        assertEquals(7, first.loaded() + second.loaded());
        assertEquals(1, first.credit());
        assertEquals(5, second.credit(), "the second gun's own credit waited in its stored tag");
    }

    @Test
    void creditIsClampedToUnderOneItemAndOnlyOnChamberGuns() {
        GunState st = new GunState(0);
        st.setCredit(RevolverDef.STATS, 99);
        assertEquals(5, st.credit());
        st.setCredit(com.gregguns.gun.def.ThompsonDef.STATS, 3);
        assertEquals(0, st.credit());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setShort(GunNbt.KEY_AMMO, (short) 2);
        tag.setByte(GunNbt.KEY_CREDIT, (byte) 40);
        assertEquals(
            5,
            GunNbt.read(tag, RevolverDef.STATS)
                .credit(),
            "a forged tag cannot hold more than one bullet's worth");
        GunState none = new GunState(3);
        GunNbt.write(tag, none);
        assertTrue(!tag.hasKey(GunNbt.KEY_CREDIT), "no credit, no key");
    }
}
