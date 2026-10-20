package com.gregguns.client.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.client.render.profile.BoxSpec;
import com.gregguns.client.render.profile.ClientGunDefinition;
import com.gregguns.client.render.profile.ClientGunDefinitions;

/** Every box must name a part the gun declares, or BoxGunModel throws IndexOutOfBounds when the client boots. */
class GunBoxPartIndexTest {

    @Test
    void everyBoxPartIsBelowThePartCount() {
        for (ClientGunDefinition def : ClientGunDefinitions.ALL) {
            for (BoxSpec box : def.boxes()) {
                assertTrue(
                    box.part() >= 0 && box.part() < def.parts(),
                    def.gunName() + " box " + box.name() + " uses part " + box.part() + " but declares " + def.parts());
            }
        }
    }
}
