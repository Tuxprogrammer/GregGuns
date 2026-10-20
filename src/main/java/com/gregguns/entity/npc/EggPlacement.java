package com.gregguns.entity.npc;

/**
 * Where a spawn egg puts its mob. Pure arithmetic with no Minecraft types. The mob always stands at the center of the
 * block column it is placed in (+0.5 on X and Z), including when the egg is used on liquid (fixes NPC-20, which used
 * the integer corner), and on the top face of a fence it is lifted half a block. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/spawning.md (2. Spawn eggs), BUGS.md NPC-18 NPC-19 NPC-20
public final class EggPlacement {

    private static final int[] DX = { 0, 0, 0, 0, -1, 1 };
    private static final int[] DY = { -1, 1, 0, 0, 0, 0 };
    private static final int[] DZ = { 0, 0, -1, 1, 0, 0 };
    private static final int TOP_FACE = 1;

    private EggPlacement() {}

    /**
     * @param x        clicked block X
     * @param y        clicked block Y
     * @param z        clicked block Z
     * @param side     the clicked face, 0 (bottom) to 5 (east), as vanilla numbers them
     * @param fenceTop true if the clicked block is a fence (render type 11)
     * @return {x, y, z} of the spawn point: the neighbor block's center, half a block higher on top of a fence
     */
    public static double[] onBlockFace(int x, int y, int z, int side, boolean fenceTop) {
        int s = Math.max(0, Math.min(5, side));
        double lift = fenceTop && s == TOP_FACE ? 0.5 : 0.0;
        return new double[] { x + DX[s] + 0.5, y + DY[s] + lift, z + DZ[s] + 0.5 };
    }

    /**
     * @param x liquid block X
     * @param y liquid block Y
     * @param z liquid block Z
     * @return {x, y, z} of the spawn point: the center of the liquid block's column at its floor (never the corner)
     */
    public static double[] atLiquid(int x, int y, int z) {
        return new double[] { x + 0.5, y, z + 0.5 };
    }
}
