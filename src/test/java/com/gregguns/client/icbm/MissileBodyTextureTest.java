package com.gregguns.client.icbm;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.gregguns.icbm.missile.MissileTier;

/** The item and block renderers bind each body's texture file; it must exist in the jar. */
class MissileBodyTextureTest {

    @Test
    void everyBodyTextureAndModelResolves() {
        for (MissileTier t : MissileTier.values()) {
            String tex = "/assets/gregguns/" + WarheadModels.bodyTextureFile(t)
                .getResourcePath();
            String obj = "/assets/gregguns/" + WarheadModels.bodyModelLocation(t)
                .getResourcePath();
            assertNotNull(MissileBodyTextureTest.class.getResource(tex), tex);
            assertNotNull(MissileBodyTextureTest.class.getResource(obj), obj);
        }
    }
}
