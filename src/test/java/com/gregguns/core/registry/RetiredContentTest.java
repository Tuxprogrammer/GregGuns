package com.gregguns.core.registry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.item.part.PartCatalog;

class RetiredContentTest {

    @Test
    void theHeatRayFocusIsRetired() {
        assertTrue(RetiredContent.isRetired("gregguns:part_heat_ray_focus"));
        assertFalse(RetiredContent.isRetired("gregguns:part_laser_focus"));
        assertFalse(RetiredContent.isRetired("othermod:part_heat_ray_focus"));
        assertFalse(RetiredContent.isRetired(null));
    }

    @Test
    void theFourSpawnerBlocksAreRetired() {
        for (String name : new String[] { "zombie_hole", "bug_hole", "silo_guard", "silo_elite" }) {
            assertTrue(RetiredContent.isRetired("gregguns:" + name), name);
        }
        assertFalse(RetiredContent.isRetired("gregguns:bug_nest"));
        assertFalse(RetiredContent.isRetired("minecraft:mob_spawner"));
    }

    @Test
    void aRetiredNameIsNeverCataloged() {
        for (String name : RetiredContent.names()) {
            assertNull(PartCatalog.get(name), name + " is retired but still cataloged");
        }
    }
}
