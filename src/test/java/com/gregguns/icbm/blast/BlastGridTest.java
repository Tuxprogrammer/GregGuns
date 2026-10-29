package com.gregguns.icbm.blast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** D274: a GregGuns guard spawner on its origin is stone to a blast; every other block keeps the pack's resistance. */
class BlastGridTest {

    /** GregTech's harderMobSpawner: setResistance(6000000) gives 18,000,000 / 5 against a null exploder. */
    private static final float GT_SPAWNER = 3600000.0F;

    @Test
    void aGregGunsPostCountsAsStone() {
        assertEquals((float) BlastRayCaster.REF_RESISTANCE, BlastGrid.cellResistance(true, GT_SPAWNER));
        assertEquals(6.0F, BlastGrid.cellResistance(true, GT_SPAWNER));
    }

    @Test
    void foreignAndMovedSpawnersKeepThePackValue() {
        assertEquals(GT_SPAWNER, BlastGrid.cellResistance(false, GT_SPAWNER));
        assertEquals(1.0F, BlastGrid.cellResistance(false, 1.0F));
    }

    @Test
    void thePostCellCostsWhatAStoneCellCosts() {
        assertEquals(
            BlastGrid.encode((float) BlastRayCaster.REF_RESISTANCE),
            BlastGrid.encode(BlastGrid.cellResistance(true, GT_SPAWNER)));
        assertEquals(
            BlastGrid.encode(BlastGrid.MAX_RESISTANCE),
            BlastGrid.encode(BlastGrid.cellResistance(false, GT_SPAWNER)));
    }
}
