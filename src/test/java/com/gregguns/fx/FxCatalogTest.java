package com.gregguns.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The shipped M5 FX catalog (I8): it parses, is complete, and holds no slow-path effect (GGS-04-07). */
class FxCatalogTest {

    /** Techguns classes that cannot emit one quad per particle (GGS-04-07), by the names they were spawned under. */
    private static final Set<String> SLOW_PATH = new HashSet<>(
        Arrays.asList("entityparticleanimated", "entitylaserbeamfx", "laserbeam", "railguntrail"));

    private static final FxRegistry REG = FxResources.shared();

    @Test
    void theShippedDataParsesCleanWithNoDanglingMembers() {
        assertEquals(
            0,
            REG.errors()
                .size(),
            String.valueOf(REG.errors()));
        assertTrue(
            REG.danglingMembers()
                .isEmpty(),
            String.valueOf(REG.danglingMembers()));
    }

    @Test
    void everyM5NameResolvesToAtLeastOneSystem() {
        for (String n : FxStress.M5_NAMES) {
            assertFalse(
                REG.resolve(n)
                    .isEmpty(),
                n + " resolves to nothing");
        }
    }

    @Test
    void theNukeListHasAllEightTechgunsSystems() {
        assertEquals(
            8,
            REG.resolve("NukeExplosion")
                .size());
        assertEquals(
            4,
            REG.resolve("RocketExplosion")
                .size());
    }

    @Test
    void GGS_04_07_noShippedNameIsASlowPathClass() {
        Set<String> names = new HashSet<>(REG.listNames());
        names.addAll(REG.systemNames());
        for (String n : names) {
            assertFalse(SLOW_PATH.contains(n.toLowerCase(Locale.ROOT)), n + " is a slow-path effect");
        }
    }

    @Test
    void GGS_04_07_everySystemIsOneQuadWithAShippedTextureAndAValidFrameGrid() {
        for (String n : REG.systemNames()) {
            ParticleSystemDef d = REG.system(n);
            assertNotNull(d.renderMode(), n);
            String tex = d.texture()
                .toLowerCase(Locale.ROOT);
            assertNotNull(
                FxCatalogTest.class.getResource("/assets/gregguns/textures/fx/" + tex),
                n + " names a missing texture " + tex);
            assertTrue(d.frames() <= d.columns() * d.rows(), n + " has more frames than cells");
        }
    }

    @Test
    void techgunsRawZeroAlphaFlaresAreOpaqueNotInvisible() {
        // Techguns TeslaFlare data is "Alpha 0 0 / 0 1", which its engine drew fully opaque; here Alpha is opacity.
        for (String n : new String[] { "TeslaFlare", "AlienFlare", "BlueBlasterFlare" }) {
            ParticleSystemDef d = REG.system(n);
            assertEquals(1F, ParticleCurve.alpha(d.alphaKeys(), 0F), 1e-4, n);
            assertEquals(1F, ParticleCurve.alpha(d.alphaKeys(), 0.6F), 1e-4, n);
        }
    }

    @Test
    void fadeEffectsStartAndEndTransparent() {
        ParticleSystemDef d = REG.system("LaserFlareEnd");
        assertEquals(0F, ParticleCurve.alpha(d.alphaKeys(), 0F), 1e-4);
        assertEquals(1F, ParticleCurve.alpha(d.alphaKeys(), 0.25F), 1e-4);
        assertEquals(0F, ParticleCurve.alpha(d.alphaKeys(), 1F), 1e-4);
    }

    @Test
    void theNukeBlastwaveExtendsTheLargeBlastwaveAndOverridesItsSize() {
        ParticleSystemDef big = REG.system("NukeExplosionBlastwave");
        ParticleSystemDef small = REG.system("LargeExplosionBlastwave");
        assertEquals(small.texture(), big.texture());
        assertEquals(
            5,
            big.systemLifetime()
                .max());
        assertEquals(
            1,
            small.systemLifetime()
                .max());
    }
}
