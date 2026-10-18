package com.gregguns.armor.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** The repair cost rule from armor-catalog.md section 3 (getRepairMats). */
class RepairSpecTest {

    private static final RepairSpec HELMET = new RepairSpec("plate", "Steel", "part_heavy_cloth", 0.5, 2);
    private static final RepairSpec LEGGINGS = new RepairSpec("plate", "Steel", "part_heavy_cloth", 1.0 / 3.0, 3);
    private static final RepairSpec CLOTH_ONLY = new RepairSpec(null, null, "part_heavy_cloth", 0.5, 4);

    @Test
    void anUndamagedPieceCostsNothing() {
        assertEquals(
            0,
            HELMET.cost(0, 825)
                .metal());
        assertEquals(
            0,
            HELMET.cost(0, 825)
                .cloth());
    }

    @Test
    void aWornOutPieceCostsTheFullTotal() {
        RepairSpec.Cost c = HELMET.cost(824, 825);
        assertEquals(1, c.metal());
        assertEquals(1, c.cloth());
        RepairSpec.Cost legs = LEGGINGS.cost(824, 825);
        assertEquals(1, legs.metal());
        assertEquals(2, legs.cloth());
    }

    @Test
    void aBarelyScratchedPieceCostsTheCeiling() {
        // ceil(2 * 1/824) = 1 part; ceil(1 * 0.5) = 1 metal, 0 cloth.
        RepairSpec.Cost c = HELMET.cost(1, 825);
        assertEquals(1, c.metal());
        assertEquals(0, c.cloth());
    }

    @Test
    void halfWornSplitsLikeTechguns() {
        // Chestplate: 4 mats at half damage = 2; metal ceil(2*0.5) = 1.
        RepairSpec chest = new RepairSpec("plate", "Steel", "part_heavy_cloth", 0.5, 4);
        RepairSpec.Cost c = chest.cost(412, 825);
        assertEquals(1, c.metal());
        assertEquals(1, c.cloth());
    }

    @Test
    void aSpecWithoutMetalNeverAsksForMetal() {
        RepairSpec.Cost c = CLOTH_ONLY.cost(824, 825);
        assertEquals(0, c.metal());
        assertEquals(4, c.cloth());
        assertNull(CLOTH_ONLY.metalPrefix());
    }

    @Test
    void everyT1PieceCanBeRepairedWithHeavyCloth() {
        for (SetDef set : java.util.Arrays
            .asList(ArmorCatalog.T1_SCOUT, ArmorCatalog.T1_MINER, ArmorCatalog.T1_COMBAT)) {
            for (PieceDef p : set.pieces()
                .values()) {
                assertNotNull(p.repair(), set.id() + " " + p.slot());
                assertEquals(
                    "part_heavy_cloth",
                    p.repair()
                        .clothItem());
            }
        }
    }

    @Test
    void repairMetalIsAGtPlateOfTheSetsMaterialNeverATechgunsItem() {
        assertEquals(
            "Steel",
            ArmorCatalog.T1_COMBAT.pieces()
                .get(ArmorSlot.HELMET)
                .repair()
                .metalMaterial());
        assertEquals(
            "CastIron",
            ArmorCatalog.T1_MINER.pieces()
                .get(ArmorSlot.HELMET)
                .repair()
                .metalMaterial());
        assertNull(
            ArmorCatalog.T1_SCOUT.pieces()
                .get(ArmorSlot.HELMET)
                .repair()
                .metalPrefix());
        for (SetDef set : ArmorCatalog.ALL) {
            for (PieceDef p : set.pieces()
                .values()) {
                if (p.repair() == null) {
                    // The Commando set has no repair mats (armor-catalog 5.6).
                    assertEquals("t2_commando", set.id());
                    continue;
                }
                String prefix = p.repair()
                    .metalPrefix();
                assertEquals(
                    prefix == null,
                    p.repair()
                        .metalMaterial() == null);
            }
        }
    }
}
