package com.gregguns.item.part;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;

class PartCatalogTest {

    @Test
    void theComponentIndexNamesAreReserved() {
        // GGS-06-47: the names areas 02, 05 and 07 to 09 reference.
        String[] index = { "part_barrel_stone", "part_barrel_iron", "part_barrel_hardened", "part_barrel_carbon",
            "part_barrel_laser", "part_stock_wooden", "part_stock_plastic", "part_stock_carbon", "part_pump_mechanism",
            "part_receiver_iron", "part_receiver_steel", "part_receiver_hardened", "part_receiver_carbon",
            "part_mechanical_1", "part_mechanical_2", "part_mechanical_3", "part_laser_focus", "part_uv_emitter",
            "part_sonic_emitter", "part_rad_emitter", "part_anti_gravity_core", "part_cybernetic",
            "part_glider_backpack", "part_glider_wing", "part_heavy_cloth", "part_treated_leather",
            "part_power_plating", "part_gas_mask_filter", "part_nuclear_warhead", "tank_fuel", "tank_bio", "tank_air",
            "cell_nuclear", "cell_nuclear_depleted", "bandages", "medkit" };
        for (String name : index) {
            assertNotNull(PartCatalog.get(name), name);
        }
        assertEquals(
            index.length,
            PartCatalog.entries()
                .size());
    }

    @Test
    void theEnergyCellAndGtMappedItemsAreNotRegistered() {
        // GGS-06-41, GGS-06-44
        assertEquals(null, PartCatalog.get("cell_energy"));
        for (PartCatalog.Entry entry : PartCatalog.entries()) {
            assertFalse(
                entry.name()
                    .startsWith("plate"),
                entry.name());
            assertFalse(
                entry.name()
                    .startsWith("circuit"),
                entry.name());
            assertFalse(
                entry.name()
                    .startsWith("wire"),
                entry.name());
            assertFalse(
                entry.name()
                    .startsWith("ingot"),
                entry.name());
        }
        assertFalse(PartCatalog.GT_MAPPED.isEmpty());
    }

    @Test
    void everyComponentHasATextureAndALangKey() throws Exception {
        String lang = new String(
            java.nio.file.Files.readAllBytes(new File("src/main/resources/assets/gregguns/lang/en_US.lang").toPath()),
            java.nio.charset.StandardCharsets.UTF_8);
        for (PartCatalog.Entry entry : PartCatalog.entries()) {
            File texture = new File("src/main/resources/assets/gregguns/textures/items/" + entry.name() + ".png");
            assertTrue(texture.exists(), "missing texture " + entry.name());
            if (entry.recipe() != null && entry.recipe()
                .contains("(m3)")) {
                continue; // registered, with lang, by the m3 branches that have not merged yet
            }
            assertTrue(lang.contains("item.gregguns." + entry.name() + ".name="), "missing lang key " + entry.name());
        }
    }

    @Test
    void ownersPartitionTheNamesAndOnlyTheDepletedCellHasNoRecipe() {
        Set<String> seen = new HashSet<>();
        for (PartCatalog.Entry entry : PartCatalog.entries()) {
            assertTrue(seen.add(entry.name()));
        }
        assertEquals(
            1,
            PartCatalog.noRecipeAllowlist()
                .size());
        assertTrue(
            PartCatalog.noRecipeAllowlist()
                .containsKey("cell_nuclear_depleted"));
        assertEquals(Tier.ZPM, PartCatalog.tierOf("cell_nuclear"));
    }

    @Test
    void tankMathFillsToCapacityAndDrainsWhatItHolds() {
        assertEquals(1000, TankMath.accept(0, 5000, TankMath.CAPACITY));
        assertEquals(250, TankMath.accept(750, 5000, TankMath.CAPACITY));
        assertEquals(0, TankMath.accept(1000, 10, TankMath.CAPACITY));
        assertEquals(0, TankMath.accept(0, -4, TankMath.CAPACITY));
        assertEquals(300, TankMath.take(300, 1000));
        assertEquals(100, TankMath.take(300, 100));
        assertEquals(0, TankMath.take(0, 100));
    }

    @Test
    void consumablesFollowTheTechgunsNumbers() {
        assertEquals(60, ConsumableSpec.BANDAGES.useTicks());
        assertEquals(2.0F, ConsumableSpec.BANDAGES.healHp());
        assertEquals(0, ConsumableSpec.BANDAGES.regenAmplifier());
        assertEquals(50, ConsumableSpec.MEDKIT.useTicks());
        assertEquals(6.0F, ConsumableSpec.MEDKIT.healHp());
        assertEquals(1, ConsumableSpec.MEDKIT.regenAmplifier());
        assertTrue(ConsumableSpec.canUse(10F, 20F));
        assertFalse(ConsumableSpec.canUse(20F, 20F));
    }
}
