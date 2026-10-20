package com.gregguns.client.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** {@link MobRenderers#texture(String)} accepts a bare name, a gregguns path or a full location. */
class MobTexturePathTest {

    @Test
    void bareNameGetsEntityFolderAndExtension() {
        assertEquals(
            "gregguns:textures/entity/bandit.png",
            MobRenderers.texture("bandit")
                .toString());
    }

    @Test
    void fullPathIsUsedAsIs() {
        assertEquals(
            "gregguns:textures/entity/zombie_soldier.png",
            MobRenderers.texture("textures/entity/zombie_soldier.png")
                .toString());
        assertEquals(
            "gregguns:custom/x.png",
            MobRenderers.texture("custom/x.png")
                .toString());
    }

    @Test
    void domainQualifiedLocationKeepsItsDomain() {
        assertEquals(
            "minecraft:textures/entity/zombie_pigman.png",
            MobRenderers.texture("minecraft:textures/entity/zombie_pigman.png")
                .toString());
    }
}
