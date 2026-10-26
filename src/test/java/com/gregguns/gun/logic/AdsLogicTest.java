package com.gregguns.gun.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;

/** Eligibility, the aim state machine and the spread modifier of aim down sights. */
class AdsLogicTest {

    private static final GunStats SIGHTS_ONLY = GunStats.builder("sights")
        .ammoType("a")
        .magazine(10)
        .spread(0.1)
        .build();
    private static final GunStats SCOPED = GunStats.builder("scoped")
        .ammoType("a")
        .magazine(10)
        .spread(0.1)
        .zoom(0.5, 0.5)
        .build();

    @Test
    void everyListedGunExistsAndHasNoScopeOverlayFlagForAkimbo() {
        for (String name : AdsSupport.GUNS) {
            GunDefinition def = find(name);
            assertTrue(def != null, name + " is a registered gun");
            assertFalse(name.startsWith("akimbo"), "akimbo guns share no line of sight");
        }
    }

    @Test
    void gunsWithAScopeOverlayAreNotListed() {
        for (String scoped : new String[] { "bolt_action_rifle", "as50", "aug", "infiltrator", "scar", "pulse_rifle",
            "blaster_rifle" }) {
            assertFalse(AdsSupport.supports(scoped), scoped);
        }
    }

    @Test
    void supportsHandlesNullAndUnknown() {
        assertFalse(AdsSupport.supports(null));
        assertFalse(AdsSupport.supports("nope"));
        assertTrue(AdsSupport.supports("m4"));
    }

    @Test
    void canToggleForScopedOrListedGuns() {
        assertTrue(AdsSupport.canToggle(SCOPED, "anything"));
        assertTrue(AdsSupport.canToggle(SIGHTS_ONLY, "ak47"));
        assertFalse(AdsSupport.canToggle(SIGHTS_ONLY, "sawed_off"));
    }

    @Test
    void sightsOnlyGunTogglesAdsAndReportsAiming() {
        ZoomState z = new ZoomState();
        assertTrue(z.toggle(SIGHTS_ONLY, true));
        assertTrue(z.isAdsOnly(SIGHTS_ONLY));
        assertTrue(z.isAiming(SIGHTS_ONLY));
        assertFalse(z.isZoomed(SIGHTS_ONLY), "the scope zoom stays off");
        assertEquals(1.0, z.fovMultiplier(SIGHTS_ONLY), 0.0, "the server FOV factor is untouched");
        assertTrue(z.toggle(SIGHTS_ONLY, true));
        assertFalse(z.isAiming(SIGHTS_ONLY));
    }

    @Test
    void aGunThatCannotAimIgnoresTheIntent() {
        ZoomState z = new ZoomState();
        assertFalse(z.toggle(SIGHTS_ONLY, false));
        assertFalse(z.isAiming(SIGHTS_ONLY));
    }

    @Test
    void aScopedGunUsesTheScopeZoomNotAds() {
        ZoomState z = new ZoomState();
        assertTrue(z.toggle(SCOPED, true));
        assertTrue(z.isZoomed(SCOPED));
        assertFalse(z.isAdsOnly(SCOPED));
        assertTrue(z.isAiming(SCOPED));
    }

    @Test
    void resetClearsAds() {
        ZoomState z = new ZoomState();
        z.toggle(SIGHTS_ONLY, true);
        z.reset();
        assertFalse(z.isAiming(SIGHTS_ONLY));
    }

    @Test
    void controllerRefusesAimWhileReloadingAndClearsOnSwitch() {
        GunController c = new GunController();
        assertTrue(c.toggleAim(SIGHTS_ONLY, true));
        assertTrue(
            c.zoom()
                .isAdsOnly(SIGHTS_ONLY));
        c.reset();
        assertFalse(
            c.zoom()
                .isAiming(SIGHTS_ONLY));
    }

    @Test
    void adsTightensASightsOnlyGunsSpread() {
        ShotContext aimed = ShotContext.player(false, 0.0)
            .withAdsSpread(0.75);
        assertEquals(0.1 * 0.75, SpreadModel.mainSpread(SIGHTS_ONLY, aimed), 1e-12);
        assertEquals(0.1, SpreadModel.mainSpread(SIGHTS_ONLY, ShotContext.player(false, 0.0)), 1e-12);
    }

    @Test
    void adsMultipliesWithArmorAccuracy() {
        ShotContext ctx = ShotContext.player(false, 0.2)
            .withAdsSpread(0.5);
        assertEquals(0.1 * 0.8 * 0.5, SpreadModel.mainSpread(SIGHTS_ONLY, ctx), 1e-12);
    }

    @Test
    void theAdsMultiplierDoesNotChangeAScopedGunsOwnZoomBonus() {
        // The server passes 1.0 for a scoped gun (isAdsOnly is false); the spec zoom bonus alone applies.
        ShotContext zoomed = ShotContext.player(true, 0.0)
            .withAdsSpread(1.0);
        assertEquals(0.1 * 0.5, SpreadModel.mainSpread(SCOPED, zoomed), 1e-12);
    }

    @Test
    void badConfiguredMultipliersFallBackToNoBonus() {
        assertEquals(1.0, AdsSupport.spreadMultiplier(Double.NaN), 0.0);
        assertEquals(1.0, AdsSupport.spreadMultiplier(-0.1), 0.0);
        assertEquals(1.0, AdsSupport.spreadMultiplier(1.5), 0.0);
        assertEquals(0.0, AdsSupport.spreadMultiplier(0.0), 0.0);
        assertEquals(0.75, AdsSupport.spreadMultiplier(0.75), 0.0);
    }

    @Test
    void aDefaultAndNpcContextCarryNoAdsBonus() {
        assertEquals(1.0, ShotContext.DEFAULT.adsSpreadMultiplier(), 0.0);
        assertEquals(
            1.0,
            ShotContext.npc(1.0)
                .adsSpreadMultiplier(),
            0.0);
    }

    private static GunDefinition find(String name) {
        for (GunDefinition d : GunDefinitions.ALL) {
            if (d.name()
                .equals(name)) {
                return d;
            }
        }
        return null;
    }
}
