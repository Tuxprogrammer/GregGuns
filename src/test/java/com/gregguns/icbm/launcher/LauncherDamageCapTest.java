package com.gregguns.icbm.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.gun.def.GrenadeLauncherDef;
import com.gregguns.gun.def.NukeLauncherDef;
import com.gregguns.gun.def.RocketLauncherDef;

/**
 * D154: a launcher warhead's blast does what its type does, not many times the gun's hit. The center damage is capped
 * at 1.25 times the launcher's own projectile damage (never above 1.5 times), and the radius stays the fill-1 radius.
 */
class LauncherDamageCapTest {

    @Test
    void capsAreDerivedFromTheGunStats() {
        assertEquals(60.0 * 1.25, LauncherWarheads.damageCap(LauncherKind.ROCKET), 1e-9);
        assertEquals(36.0 * 1.25, LauncherWarheads.damageCap(LauncherKind.GRENADE), 1e-9);
        assertEquals(400.0 * 1.25, LauncherWarheads.damageCap(LauncherKind.NUKE), 1e-9);
        assertEquals(75.0, LauncherWarheads.damageCap(LauncherKind.ROCKET), 1e-9);
        assertEquals(45.0, LauncherWarheads.damageCap(LauncherKind.GRENADE), 1e-9);
        assertEquals(500.0, LauncherWarheads.damageCap(LauncherKind.NUKE), 1e-9);
        assertEquals(60.0, RocketLauncherDef.STATS.damage(), 1e-9);
        assertEquals(36.0, GrenadeLauncherDef.STATS.damage(), 1e-9);
        assertEquals(400.0, NukeLauncherDef.STATS.damage(), 1e-9);
    }

    @Test
    void theFactorStaysAtOrBelowOneAndAHalf() {
        assertTrue(LauncherRules.DAMAGE_CAP_FACTOR <= 1.5);
    }

    @Test
    void everyWarheadHitsNoHarderThanItsCapAndKeepsTheFillOneRadius() {
        for (LauncherWarhead w : LauncherWarhead.values()) {
            double damage = LauncherWarheads.centerDamage(w);
            assertTrue(
                damage <= LauncherWarheads.damageCap(w.kind()) + 1e-9,
                w + " center damage " + damage + " passes the cap");
            double radius = LauncherWarheads.blastRadius(w);
            assertEquals(
                com.gregguns.icbm.explosive.Explosives.get(w.blastId())
                    .radius(LauncherRules.FILL),
                radius,
                1e-9,
                w + " radius must stay the fill-1 radius");
            System.out
                .println(String.format("TABLE %s blast=%s radius=%.2f damage=%.1f", w, w.blastId(), radius, damage));
        }
    }

    @Test
    void anUncappedHighExplosiveShotWouldHaveBeenNineTimesTheGun() {
        // The review's finding: condensed at fill 1 is 537, 9 times the Rocket Launcher's 60. The cap brings it to 75.
        double uncapped = com.gregguns.icbm.explosive.Explosives.get("condensed") == null ? 0.0
            : com.gregguns.icbm.explosive.Explosives.get("condensed")
                .damage(1);
        assertTrue(uncapped == 0.0 || uncapped > 500.0);
        assertEquals(75.0, LauncherWarheads.centerDamage(LauncherWarhead.HE), 1e-9);
    }

    @Test
    void combineNeverLosesCharges() {
        assertEquals(4, LauncherRules.combine(null, 0, "he"));
        assertEquals(8, LauncherRules.combine("he", 4, "he"));
        assertEquals(16, LauncherRules.combine("he", 12, "he"));
        assertEquals(-1, LauncherRules.combine("he", 13, "he"));
        assertEquals(-1, LauncherRules.combine("he", 16, "he"));
        assertEquals(-1, LauncherRules.combine("he", 12, "emp"));
        assertEquals(4, LauncherRules.combine("he", 0, "emp"));
    }
}
