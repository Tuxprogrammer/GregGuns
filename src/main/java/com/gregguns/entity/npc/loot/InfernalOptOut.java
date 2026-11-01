package com.gregguns.entity.npc.loot;

/**
 * Whether an NPC skips Infernal Mobs modifier roll (GGS-10-13). Spawner NPCs roll like any other NPC; they opt out
 * only when the config asks for the farm guard. Pure.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/infernal-mobs.md (GGS-10-13)
public final class InfernalOptOut {

    /**
     * The marker written into the entity's persistent data under the Infernal Mobs key. It names no modifier, so
     * Infernal Mobs finds a non-empty saved string, skips its rolling path and adds nothing.
     */
    public static final String MARKER = "gregguns_no_infernal";

    private InfernalOptOut() {}

    /**
     * @param fromSpawnerBlock true when a spawner block places the NPC
     * @param spawnerOptOut    the {@code infernalSpawnerOptOut} config value (default false)
     * @return true when the NPC must carry the marker
     */
    public static boolean shouldOptOut(boolean fromSpawnerBlock, boolean spawnerOptOut) {
        return fromSpawnerBlock && spawnerOptOut;
    }
}
