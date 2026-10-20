package com.gregguns.entity.npc.mob;

/**
 * Where the Cyberdemon may appear by the natural-spawn check. Pure: no Minecraft types. It is Nether-only and sits
 * behind the {@code cyberdemon} config flag; design 0005 gives it no natural spawn, so this gate only matters to a
 * spawn path that asks {@code getCanSpawnHere} (a spawner, a future camp rule). Eggs and commands place it directly
 * and bypass the gate by design.
 *
 * <p>
 * Usage: {@code CyberDemonRules.canSpawnAt(world.provider.dimensionId, NpcConfig.cyberdemon)}. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (11. Cyberdemon), docs/design/0005-world-presence.md
public final class CyberDemonRules {

    /** The Nether's dimension id. */
    public static final int NETHER = -1;

    private CyberDemonRules() {}

    /**
     * @param dimensionId the dimension id of the world
     * @param flagOn      the {@code cyberdemon} config flag
     * @return true only in the Nether with the flag on
     */
    public static boolean canSpawnAt(int dimensionId, boolean flagOn) {
        return flagOn && dimensionId == NETHER;
    }
}
