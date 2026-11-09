package com.gregguns.entity.npc.mob;

/**
 * Where the Cyberdemon may appear by the natural-spawn check. Pure: no Minecraft types. It belongs to the Nether
 * spawn table (a rare boss deep in the Nether, D353) and sits behind the {@code cyberdemon} config flag, so this check
 * passes only in a dimension of {@code npcs.netherSpawnDimensions} with the flag on. The natural-spawn gate adds the
 * danger and the boss spacing; a spawner that asks {@code getCanSpawnHere} gets this check alone. Eggs and commands
 * place it directly and bypass the check by design.
 *
 * <p>
 * Usage: {@code CyberDemonRules.canSpawnAt(world.provider.dimensionId, NpcConfig.netherSpawnDimensions,
 * NpcConfig.cyberdemon)}. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (11. Cyberdemon), docs/design/0005-world-presence.md, D353
public final class CyberDemonRules {

    /** The Nether's dimension id. */
    public static final int NETHER = -1;

    private CyberDemonRules() {}

    /**
     * @param dimensionId the dimension id of the world
     * @param netherDims  the Nether spawn dimensions ({@code npcs.netherSpawnDimensions})
     * @param flagOn      the {@code cyberdemon} config flag
     * @return true only in a Nether spawn dimension with the flag on
     */
    public static boolean canSpawnAt(int dimensionId, int[] netherDims, boolean flagOn) {
        if (!flagOn || netherDims == null) {
            return false;
        }
        for (int d : netherDims) {
            if (d == dimensionId) {
                return true;
            }
        }
        return false;
    }
}
