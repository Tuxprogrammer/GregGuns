package com.gregguns.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import gregtech.api.hazards.Hazard;

class DamageTypeTest {

    private static Properties lang() throws IOException {
        Properties p = new Properties();
        try (InputStream in = DamageTypeTest.class.getResourceAsStream("/assets/gregguns/lang/en_US.lang")) {
            assertNotNull(in, "en_US.lang must be on the test classpath");
            p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    @Test
    void tenTypesWithOneSourceNameEach() {
        assertEquals(10, DamageType.values().length);
        for (DamageType t : DamageType.values()) {
            assertEquals(
                "gregguns." + t.name()
                    .toLowerCase(),
                t.sourceName());
        }
    }

    @Test
    void onlyUnresistableBypassesArmour() {
        for (DamageType t : DamageType.values()) {
            assertEquals(t == DamageType.UNRESISTABLE, t.bypassesArmour(), t.toString());
        }
    }

    @Test
    void hazardMapping() {
        assertEquals(HazardKind.HEAT, DamageType.FIRE.hazard());
        assertEquals(HazardKind.FROST, DamageType.ICE.hazard());
        assertEquals(HazardKind.RADIOLOGICAL, DamageType.RADIATION.hazard());
        assertEquals(HazardKind.BIOLOGICAL, DamageType.POISON.hazard());
        assertEquals(HazardKind.ELECTRICAL, DamageType.LIGHTNING.hazard());
        assertNull(DamageType.PHYSICAL.hazard());
        assertNull(DamageType.PROJECTILE.hazard());
        assertNull(DamageType.EXPLOSION.hazard());
        assertNull(DamageType.ENERGY.hazard());
        assertNull(DamageType.UNRESISTABLE.hazard());
    }

    @Test
    void everyHazardMapsToTheMatchingGregTechHazard() {
        assertEquals(Hazard.HEAT, GregTechHazmat.toGt(HazardKind.HEAT));
        assertEquals(Hazard.FROST, GregTechHazmat.toGt(HazardKind.FROST));
        assertEquals(Hazard.RADIOLOGICAL, GregTechHazmat.toGt(HazardKind.RADIOLOGICAL));
        assertEquals(Hazard.BIOLOGICAL, GregTechHazmat.toGt(HazardKind.BIOLOGICAL));
        assertEquals(Hazard.ELECTRICAL, GregTechHazmat.toGt(HazardKind.ELECTRICAL));
    }

    @Test
    void secondaryEffectsExistExactlyForHazardTypes() {
        for (DamageType t : DamageType.values()) {
            assertEquals(t.hazard() != null, SecondaryEffects.forType(t) != null, t.toString());
        }
        assertEquals(
            SecondaryEffect.Kind.IGNITE,
            SecondaryEffects.forType(DamageType.FIRE)
                .kind());
    }

    @Test
    void ignoreFramesDefaultsFollowTheTechgunsFactories() {
        assertTrue(DamageType.PROJECTILE.ignoresInvulnerabilityFramesByDefault());
        assertTrue(DamageType.FIRE.ignoresInvulnerabilityFramesByDefault());
        assertTrue(DamageType.ENERGY.ignoresInvulnerabilityFramesByDefault());
        assertTrue(DamageType.RADIATION.ignoresInvulnerabilityFramesByDefault());
        assertTrue(DamageType.LIGHTNING.ignoresInvulnerabilityFramesByDefault());
        assertTrue(DamageType.POISON.ignoresInvulnerabilityFramesByDefault());
        assertFalse(DamageType.EXPLOSION.ignoresInvulnerabilityFramesByDefault());
        assertFalse(DamageType.PHYSICAL.ignoresInvulnerabilityFramesByDefault());
    }

    @Test
    void doesNotReproduceGUN43_everyTypeHasNameAndDeathMessageKeys() throws IOException {
        Properties lang = lang();
        for (DamageType t : DamageType.values()) {
            assertNotNull(lang.getProperty(t.nameLangKey()), "missing " + t.nameLangKey());
            assertNotNull(lang.getProperty(t.deathMessageKey()), "missing " + t.deathMessageKey());
            assertNotNull(lang.getProperty(t.deathMessageKey() + ".item"), "missing .item for " + t);
            assertNotNull(lang.getProperty(t.deathMessageKey() + ".player"), "missing .player for " + t);
        }
        assertEquals("Unresistable", lang.getProperty(DamageType.UNRESISTABLE.nameLangKey()));
    }
}
