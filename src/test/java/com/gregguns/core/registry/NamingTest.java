package com.gregguns.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NamingTest {

    @Test
    void unlocalizedNameProducesTheDocumentedLangKeyOnceVanillaAddsItsPrefix() {
        // Vanilla 1.7.10: Item.getUnlocalizedName() is "item." + name; Block is "tile." + name.
        assertEquals("item.gregguns.pistol_9mm", "item." + Naming.unlocalizedName("pistol_9mm"));
        assertEquals("tile.gregguns.steel_crate", "tile." + Naming.unlocalizedName("steel_crate"));
    }

    @Test
    void langKeysCarryTheNameSuffix() {
        assertEquals("item.gregguns.pistol_9mm.name", Naming.itemLangKey("pistol_9mm"));
        assertEquals("tile.gregguns.steel_crate.name", Naming.blockLangKey("steel_crate"));
        assertEquals("entity.gregguns.turret.name", Naming.entityLangKey("turret"));
    }

    @Test
    void registryAndTextureNames() {
        assertEquals("pistol_9mm", Naming.registryName("pistol_9mm"));
        assertEquals("gregguns:pistol_9mm", Naming.qualifiedName("pistol_9mm"));
        assertEquals("gregguns:pistol_9mm", Naming.textureName("pistol_9mm"));
        assertEquals("gregguns:turret", Naming.tileEntityId("turret"));
    }

    @Test
    void badContentNamesAreRejected() {
        for (String bad : new String[] { "", "Pistol", "9mm", "has space", "dot.ted", "dash-ed", "gregguns:x", null }) {
            assertThrows(IllegalArgumentException.class, () -> Naming.unlocalizedName(bad), String.valueOf(bad));
        }
    }

    @Test
    void soundEventsAllowDotsAndAreNamespaced() {
        assertEquals("gregguns:gun.pistol.fire", Naming.soundEvent("gun.pistol.fire"));
        assertEquals("gregguns:click", Naming.soundEvent("click"));
    }

    @Test
    void badSoundEventsAreRejected() {
        for (String bad : new String[] { "", "Gun.fire", "gun..fire", ".gun", "gun.", "a:b", null }) {
            assertThrows(IllegalArgumentException.class, () -> Naming.soundEvent(bad), String.valueOf(bad));
        }
    }
}
