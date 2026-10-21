package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.gun.def.AugDef;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.def.InfiltratorDef;
import com.gregguns.gun.def.M4Def;
import com.gregguns.gun.def.ScarDef;
import com.gregguns.gun.def.VectorDef;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.ReloadStateMachine.Event;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.ProductionTable;

/**
 * Unit tests of the HV batch B3 guns (AUG, SCAR, Vector, Infiltrator) through the pure gun logic: fire rate, the
 * scopes, the SCAR clip of 20 on a 30-round magazine, the silenced flag and the Infiltrator upgrade row.
 */
// Spec: docs/techguns-spec/02-guns/sheets-rifles-shotguns.md (AUG, SCAR, Infiltrator); sheets-handguns-smgs.md (Vector)
class HvRiflesLogicTest {

    private static final int HELD_TICKS = 13;

    private static final GunStats[] ALL = { AugDef.STATS, ScarDef.STATS, VectorDef.STATS, InfiltratorDef.STATS };

    private static int shotsHeld(GunStats stats) {
        GunController c = new GunController();
        GunState st = new GunState(stats.magazineSize());
        MapAmmoInventory inv = new MapAmmoInventory();
        c.triggerPressed();
        int shots = 0;
        for (int i = 0; i < HELD_TICKS; i++) {
            if (c.tick(stats, st, inv, true, false)
                .shot()
                .wasFired()) {
                shots++;
            }
        }
        return shots;
    }

    @Test
    void fireRatesAreThreeFourTwoAndThreeTicks() {
        assertEquals(5, shotsHeld(AugDef.STATS)); // ticks 0, 3, 6, 9, 12
        assertEquals(4, shotsHeld(ScarDef.STATS)); // ticks 0, 4, 8, 12
        assertEquals(7, shotsHeld(VectorDef.STATS)); // ticks 0, 2, ... 12
        assertEquals(5, shotsHeld(InfiltratorDef.STATS));
        for (GunStats stats : ALL) {
            assertEquals(FireMode.AUTO, stats.fireMode());
        }
    }

    @Test
    void theSheetsFiguresAreKept() {
        assertEquals(11.0, AugDef.STATS.damage());
        assertEquals(9.5, AugDef.STATS.damageMin());
        assertEquals(25, AugDef.STATS.dropStart());
        assertEquals(40, AugDef.STATS.dropEnd());
        assertEquals(0.01, AugDef.STATS.spread());
        assertEquals(14.0, ScarDef.STATS.damage());
        assertEquals(11.5, ScarDef.STATS.damageMin());
        assertEquals(30, ScarDef.STATS.dropStart());
        assertEquals(40, ScarDef.STATS.dropEnd());
        assertEquals(50, ScarDef.STATS.lifetimeTicks());
        assertEquals(0.25, ScarDef.STATS.penetration());
        assertEquals(0.015, ScarDef.STATS.spread());
        assertEquals(8.0, VectorDef.STATS.damage());
        assertEquals(5.5, VectorDef.STATS.damageMin());
        assertEquals(17, VectorDef.STATS.dropStart());
        assertEquals(25, VectorDef.STATS.dropEnd());
        assertEquals(30, VectorDef.STATS.lifetimeTicks());
        assertEquals(0.05, VectorDef.STATS.spread());
        assertEquals(2, VectorDef.STATS.recoilTicks());
        assertEquals(40, VectorDef.STATS.reloadTicks());
        assertEquals(25, VectorDef.STATS.magazineSize());
        assertEquals(45, AugDef.STATS.reloadTicks());
        assertEquals(45, ScarDef.STATS.reloadTicks());
    }

    @Test
    void theScopesZoomByTheirOwnFactors() {
        assertScope(AugDef.STATS, 0.5, 0.5);
        assertScope(ScarDef.STATS, 0.65, 0.5);
        assertScope(VectorDef.STATS, 0.75, 0.35);
        assertScope(InfiltratorDef.STATS, 0.5, 0.5);
    }

    private static void assertScope(GunStats stats, double fov, double spreadFactor) {
        assertTrue(stats.hasScope());
        GunController c = new GunController();
        assertTrue(c.toggleZoom(stats));
        assertEquals(
            fov,
            c.zoom()
                .fovMultiplier(stats),
            1e-9);
        assertEquals(stats.spread() * spreadFactor, SpreadModel.mainSpread(stats, ShotContext.player(true, 0.0)), 1e-9);
        assertEquals(stats.spread(), SpreadModel.mainSpread(stats, ShotContext.player(false, 0.0)), 1e-9);
    }

    @Test
    void theScarHoldsTwentyRoundsOfAThirtyRoundMagazineAndRefundsItsOwnShare() {
        GunStats stats = ScarDef.STATS;
        assertEquals(20, stats.magazineSize());
        assertEquals(M4Def.STATS.ammoType(), stats.ammoType(), "the SCAR shares the M4 magazine");
        assertEquals(6, GunItems.roundsPerLooseItem(stats));
        GunController c = new GunController();
        GunState st = new GunState(18);
        MapAmmoInventory inv = new MapAmmoInventory().with(stats.ammoType(), 1);
        assertEquals(Event.STARTED, c.forceReload(stats, st, inv));
        for (int i = 0; i < stats.reloadTicks(); i++) {
            c.tick(stats, st, inv, true, false);
        }
        assertEquals(20, st.loaded(), "the clip refills to 20, not the 30 of the other rifles");
        assertEquals(18, inv.rounds(AmmoIds.RIFLE));
        assertTrue(18 / GunItems.roundsPerLooseItem(stats) <= 3, "never more bullets than the magazine is worth");
        assertEquals(1, inv.items(AmmoIds.ASSAULT_RIFLE_MAGAZINE_EMPTY));
    }

    @Test
    void theVectorUsesTheSmgMagazineWithTwelveRoundsPerBullet() {
        GunStats stats = VectorDef.STATS;
        assertEquals(AmmoIds.SMG_MAGAZINE, stats.ammoType());
        assertEquals(AmmoIds.NINE_MM, stats.looseAmmoType());
        assertEquals(12, GunItems.roundsPerLooseItem(stats));
    }

    @Test
    void everyB3GunReloadsFromOneMagazineAndReturnsTheEmptyOne() {
        for (GunStats stats : ALL) {
            GunController c = new GunController();
            GunState st = new GunState(0);
            MapAmmoInventory inv = new MapAmmoInventory().with(stats.ammoType(), 1);
            c.triggerPressed();
            assertEquals(
                Event.STARTED,
                c.tick(stats, st, inv, true, false)
                    .reload());
            for (int i = 0; i < stats.reloadTicks(); i++) {
                c.tick(stats, st, inv, true, false);
            }
            assertEquals(stats.magazineSize(), st.loaded(), stats.id());
            assertEquals(1, inv.items(stats.emptyContainerType()), stats.id());
        }
    }

    @Test
    void onlyTheInfiltratorIsSilenced() {
        assertTrue(InfiltratorDef.STATS.silenced());
        assertFalse(M4Def.STATS.silenced());
        assertFalse(AugDef.STATS.silenced());
        assertFalse(ScarDef.STATS.silenced());
        assertFalse(VectorDef.STATS.silenced());
        assertEquals("guns.infiltrator.fire", InfiltratorDef.DEFINITION.fireEvent());
    }

    @Test
    void theInfiltratorIsMadeFromAnM4AtAnyDamage() {
        ProductionTable.Row row = InfiltratorDef.DEFINITION.assembly();
        assertNotNull(row);
        assertEquals("GR-33", row.id());
        assertEquals(Tier.HV, row.tier());
        ProductionTable.Input base = row.inputs()
            .get(0);
        assertEquals("m4", base.itemName());
        assertEquals(1, base.count());
        assertTrue(base.anyDamage(), "a used or camouflaged M4 is accepted (GGS-02-40)");
    }

    @Test
    void theB3GunsAreHvAndCamoCountsFollowTheSheets() {
        for (String name : new String[] { "aug", "scar", "vector", "infiltrator" }) {
            assertEquals(
                Tier.HV,
                GunDefinitions.byName(name)
                    .tier());
        }
        assertEquals(5, AugDef.DEFINITION.camoCount());
        assertEquals(2, ScarDef.DEFINITION.camoCount());
        assertEquals(2, VectorDef.DEFINITION.camoCount());
        assertEquals(1, InfiltratorDef.DEFINITION.camoCount());
    }
}
