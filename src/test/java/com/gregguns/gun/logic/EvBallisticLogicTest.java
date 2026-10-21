package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.def.MinigunDef;
import com.gregguns.gun.def.PdwDef;
import com.gregguns.gun.def.PulseRifleDef;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.GateRule;
import com.gregguns.recipe.ProductionTable;

/**
 * Unit tests of the EV ballistic batch (PDW, Pulse Rifle, Minigun) through the pure gun logic: the sheets' figures,
 * fire rates, burst-line sizes, the shared Advanced Magazine and the drum, the forced-reload refunds, the camo counts
 * and the titanium gate of the assembly rows.
 */
// Spec: docs/techguns-spec/02-guns/sheets-rifles-shotguns.md (PDW, Pulse Rifle, Minigun)
class EvBallisticLogicTest {

    private static final int HELD_TICKS = 20;

    private static int shotsHeld(GunStats stats) {
        GunController c = new GunController();
        GunState st = new GunState(stats.magazineSize());
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        int shots = 0;
        // A spin-up gun fires nothing until its barrels are at speed: count from that tick.
        int lead = stats.spinUpTicks() > 0 ? stats.spinUpTicks() - 1 : 0;
        for (int i = 0; i < lead + HELD_TICKS; i++) {
            if (c.tick(stats, st, inv, true, false)
                .shot()
                .wasFired()) {
                shots++;
            }
        }
        return shots;
    }

    @Test
    void theSheetsFiguresAreKept() {
        GunStats pdw = PdwDef.STATS;
        assertEquals(6.0, pdw.damage());
        assertEquals(3.0, pdw.damageMin());
        assertEquals(16, pdw.dropStart());
        assertEquals(25, pdw.dropEnd());
        assertEquals(25, pdw.lifetimeTicks());
        assertEquals(1.5, pdw.projectileSpeed());
        assertEquals(0.03, pdw.spread());
        assertEquals(0.25, pdw.penetration());
        assertEquals(20, pdw.magazineSize());
        assertEquals(40, pdw.reloadTicks());
        assertFalse(pdw.hasScope());

        GunStats pulse = PulseRifleDef.STATS;
        assertEquals(10.0, pulse.damage());
        assertEquals(8.0, pulse.damageMin());
        assertEquals(25, pulse.dropStart());
        assertEquals(35, pulse.dropEnd());
        assertEquals(50, pulse.lifetimeTicks());
        assertEquals(2.25, pulse.projectileSpeed(), 1e-9, "1.5x the usual bullet speed");
        assertEquals(0.024, pulse.spread());
        assertEquals(12, pulse.magazineSize());
        assertEquals(45, pulse.reloadTicks());
        assertEquals(8, pulse.recoilTicks());

        GunStats minigun = MinigunDef.STATS;
        assertEquals(5.0, minigun.damage());
        assertEquals(3.0, minigun.damageMin());
        assertEquals(20, minigun.dropStart());
        assertEquals(40, minigun.dropEnd());
        assertEquals(0.025, minigun.spread());
        assertEquals(0.15, minigun.penetration());
        assertEquals(200, minigun.magazineSize());
        assertEquals(100, minigun.reloadTicks());
        assertFalse(minigun.hasScope());
    }

    @Test
    void fireRatesAreEverySecondSeventhAndEveryTick() {
        assertEquals(10, shotsHeld(PdwDef.STATS)); // ticks 0, 2, ... 18
        assertEquals(3, shotsHeld(PulseRifleDef.STATS)); // ticks 0, 7, 14
        assertEquals(HELD_TICKS, shotsHeld(MinigunDef.STATS), "the Minigun fires on every tick");
        for (GunStats stats : new GunStats[] { PdwDef.STATS, PulseRifleDef.STATS, MinigunDef.STATS }) {
            assertEquals(FireMode.AUTO, stats.fireMode());
        }
    }

    @Test
    void burstLinesMakeTwoAndThreeProjectilesFromOneRound() {
        assertEquals(
            2,
            PdwDef.STATS.pattern()
                .projectileCount());
        assertEquals(
            3,
            PulseRifleDef.STATS.pattern()
                .projectileCount());
        assertFalse(
            MinigunDef.STATS.pattern()
                .isActive());
        assertEquals(1, PdwDef.STATS.ammoPerShot(), "one round per pull");
        assertEquals(1, PulseRifleDef.STATS.ammoPerShot());
        GunController c = new GunController();
        GunState st = new GunState(PdwDef.STATS.magazineSize());
        c.triggerPressed();
        c.tick(PdwDef.STATS, st, new MapAmmoInventory(), true, false);
        assertEquals(19, st.loaded(), "a burst spends one round");
    }

    @Test
    void theBurstLineIsATrainShiftedByFractionsOfATick() {
        ShotOffset[] pulse = SpreadModel.roll(new Random(5), PulseRifleDef.STATS, ShotContext.DEFAULT, 0);
        assertEquals(3, pulse.length);
        ShotOffset[] pdw = SpreadModel.roll(new Random(5), PdwDef.STATS, ShotContext.DEFAULT, 0);
        assertEquals(2, pdw.length);
    }

    @Test
    void thePulseRifleScopeZoomsToThirtyFivePercent() {
        GunStats stats = PulseRifleDef.STATS;
        assertTrue(stats.hasScope());
        GunController c = new GunController();
        assertTrue(c.toggleZoom(stats));
        assertEquals(
            0.35,
            c.zoom()
                .fovMultiplier(stats),
            1e-9);
        assertEquals(stats.spread() * 0.5, SpreadModel.mainSpread(stats, ShotContext.player(true, 0.0)), 1e-9);
    }

    @Test
    void theAdvancedMagazineIsSharedAndTheDrumIsTheMiniguns() {
        assertEquals(AmmoIds.ADVANCED_MAGAZINE, PdwDef.STATS.ammoType());
        assertEquals(AmmoIds.ADVANCED_MAGAZINE, PulseRifleDef.STATS.ammoType());
        assertEquals(AmmoIds.ADVANCED_MAGAZINE_EMPTY, PdwDef.STATS.emptyContainerType());
        assertEquals(AmmoIds.ADVANCED_BULLET, PdwDef.STATS.looseAmmoType());
        assertEquals(AmmoIds.MINIGUN_DRUM, MinigunDef.STATS.ammoType());
        assertEquals(AmmoIds.MINIGUN_DRUM_EMPTY, MinigunDef.STATS.emptyContainerType());
        assertEquals(AmmoIds.RIFLE, MinigunDef.STATS.looseAmmoType());
    }

    @Test
    void forcedReloadRefundsFollowTheGunsOwnStats() {
        // 20 rounds from 3 bullets: 6 whole rounds per bullet; 12 from 3: 4; 200 from 16 bullets: 12.
        assertEquals(6, GunItems.roundsPerLooseItem(PdwDef.STATS));
        assertEquals(4, GunItems.roundsPerLooseItem(PulseRifleDef.STATS));
        assertEquals(12, GunItems.roundsPerLooseItem(MinigunDef.STATS));
        GunStats stats = PdwDef.STATS;
        GunController c = new GunController();
        GunState st = new GunState(17);
        MapAmmoInventory inv = new MapAmmoInventory().with(stats.ammoType(), 1);
        assertEquals(ReloadStateMachine.Event.STARTED, c.forceReload(stats, st, inv));
        for (int i = 0; i < stats.reloadTicks(); i++) {
            c.tick(stats, st, inv, true, false);
        }
        assertEquals(20, st.loaded());
        assertEquals(17, inv.rounds(AmmoIds.ADVANCED_BULLET), "the 17 rounds left come back as loose rounds");
        assertTrue(17 / GunItems.roundsPerLooseItem(stats) <= 3, "never more bullets than the magazine is worth");
        assertEquals(1, inv.items(AmmoIds.ADVANCED_MAGAZINE_EMPTY));
    }

    @Test
    void theMinigunHasASpinUpStartSoundAndTheOthersDoNot() {
        assertEquals("guns.minigun.start", MinigunDef.DEFINITION.startEvent());
        assertNull(PdwDef.DEFINITION.startEvent());
        assertNull(PulseRifleDef.DEFINITION.startEvent());
    }

    @Test
    void camosAreIndexOneAndTwoForTheBurstGunsAndNoneForTheMinigun() {
        assertEquals(3, PdwDef.DEFINITION.camoCount());
        assertEquals(3, PulseRifleDef.DEFINITION.camoCount());
        assertEquals(1, MinigunDef.DEFINITION.camoCount());
    }

    @Test
    void theAssemblyRowsAreEvAndCarryTheTitaniumGate() {
        for (String name : new String[] { "pdw", "pulse_rifle", "minigun" }) {
            ProductionTable.Row row = GunDefinitions.byName(name)
                .assembly();
            assertEquals(Tier.EV, row.tier(), name);
            assertEquals("Titanium", GateRule.gateMaterial(row.tier()));
            assertTrue(
                row.inputs()
                    .stream()
                    .anyMatch(i -> "plate".equals(i.prefix()) && "Titanium".equals(i.material())),
                name + " titanium plate");
            assertTrue(
                GateRule.satisfied(row, ProductionTable.byOutput(), ProductionTable.GATE_EXEMPT),
                name + " meets the gate rule");
        }
        assertEquals(
            6,
            GunDefinitions.byName("minigun")
                .assembly()
                .inputs()
                .get(0)
                .count());
    }
}
