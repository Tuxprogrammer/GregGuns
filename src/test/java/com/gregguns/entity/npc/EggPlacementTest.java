package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

/** Spawn egg placement (M7 N7; NPC-18/19/20). */
class EggPlacementTest {

    @Test
    void topFaceCentersOnTheBlockAbove() {
        assertArrayEquals(new double[] { 10.5, 65.0, -3.5 }, EggPlacement.onBlockFace(10, 64, -4, 1, false), 1e-9);
    }

    @Test
    void sideFacesOffsetOneBlock() {
        assertArrayEquals(new double[] { 0.5, 5.0, -0.5 }, EggPlacement.onBlockFace(0, 5, 0, 2, false), 1e-9);
        assertArrayEquals(new double[] { 1.5, 5.0, 0.5 }, EggPlacement.onBlockFace(0, 5, 0, 5, false), 1e-9);
        assertArrayEquals(new double[] { 0.5, 4.0, 0.5 }, EggPlacement.onBlockFace(0, 5, 0, 0, false), 1e-9);
    }

    @Test
    void fenceTopLiftsHalfABlock() {
        assertArrayEquals(new double[] { 0.5, 6.5, 0.5 }, EggPlacement.onBlockFace(0, 5, 0, 1, true), 1e-9);
        assertArrayEquals(new double[] { 0.5, 5.0, -0.5 }, EggPlacement.onBlockFace(0, 5, 0, 2, true), 1e-9);
    }

    @Test
    void doesNotReproduceNPC20_liquidSpawnIsAtTheBlockCenter() {
        assertArrayEquals(new double[] { 3.5, 62.0, 7.5 }, EggPlacement.atLiquid(3, 62, 7), 1e-9);
        assertArrayEquals(new double[] { -2.5, 62.0, -0.5 }, EggPlacement.atLiquid(-3, 62, -1), 1e-9);
    }
}
