package com.gregguns.client.render.profile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The hammers and the Chainsaw in the client list: skins, the chain frame rule and the piston. */
// Spec: docs/gregguns-spec/03-gun-visuals-audio/index.md; docs/techguns-spec/03-gun-visuals-audio/model-notes
class ToolGunWiringTest {

    @Test
    void eachHammerSelectsItsOwnSkin() {
        ClientGunDefinition plain = ClientGunDefinitions.byName("power_hammer");
        ClientGunDefinition adv = ClientGunDefinitions.byName("power_hammer_adv");
        assertNotNull(plain);
        assertNotNull(adv);
        assertArrayEquals(new String[] { PowerHammerProfile.TEXTURES[0] }, plain.textures());
        assertArrayEquals(new String[] { PowerHammerProfile.TEXTURES[1] }, adv.textures());
        assertEquals(plain.boxes().length, adv.boxes().length);
        assertEquals(plain.transforms(), adv.transforms(), "one geometry, one set of rows");
    }

    @Test
    void theChainsawIsListed() {
        assertNotNull(ClientGunDefinitions.byName("chainsaw"));
    }

    @Test
    void exactlyOneChainFrameIsDrawnAtAnyFireProgress() {
        GunTransforms t = ChainsawProfile.TRANSFORMS;
        for (int i = 0; i <= 80; i++) {
            float p = i / 80.0f;
            boolean a = t.partVisible(ChainsawProfile.CHAIN_PART_A, 100, 2.0f, p);
            boolean b = t.partVisible(ChainsawProfile.CHAIN_PART_B, 100, 2.0f, p);
            assertTrue(a ^ b, "one frame at progress " + p);
            assertEquals(a ? ChainsawProfile.CHAIN_PART_A : ChainsawProfile.CHAIN_PART_B, ChainsawProfile.chainPart(p));
            assertTrue(t.partVisible(0, 100, 2.0f, p), "the saw body is always drawn");
        }
        assertTrue(t.partVisible(ChainsawProfile.CHAIN_PART_A, 100, 2.0f, 0.0f), "frame A at rest");
        assertFalse(t.partVisible(ChainsawProfile.CHAIN_PART_B, 100, 2.0f, 0.0f));
        assertTrue(t.partVisible(ChainsawProfile.CHAIN_PART_B, 100, 2.0f, 1.0f / ChainsawProfile.CHAIN_STEPS));
    }

    @Test
    void thePistonRetractsInTheFirstPersonFireRow() {
        assertEquals(0.0, PowerHammerProfile.pistonRetract(0.0));
        assertEquals(1.0, PowerHammerProfile.pistonRetract(0.25), 1e-9);
        assertEquals(0.0, PowerHammerProfile.pistonRetract(1.0));
    }
}
