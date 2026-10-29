package com.gregguns.entity.npc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.gregguns.GregGuns;
import com.gregguns.core.config.CompatConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInterModComms;

/**
 * Keeps EnderIO's spawner features away from GregGuns NPCs (D273). In GTNH, EnderIO handles every
 * {@code minecraft:mob_spawner} a survival player breaks: unless the spawner's mob is on the Powered Spawner blacklist,
 * it drops a Broken Spawner of that mob (the pack sets the chance to 1.0) and runs 0 to 6 extra spawn cycles at once;
 * a spawner broken by anything else, a blast for example, also drops one. A Broken Spawner of a guard would farm planet
 * guards anywhere, and the burst would spring extra guards on whoever broke the post.
 *
 * <p>
 * One IMC message per registered NPC puts it on that blacklist. EnderIO checks the blacklist on both break paths
 * before it queues the drop or runs the burst, so this one switch covers both; it also keeps GregGuns NPCs out of the
 * Powered Spawner. No EnderIO class is named: the messages are plain strings, sent only when EnderIO is loaded and
 * {@code compat.enderioSpawnerBlacklist} is on.
 *
 * <p>
 * Usage: {@code EnderIoSpawners.send()} from the npcs module's init, after the NPCs are registered.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (spawner marks), docs/gregguns-spec/INTEGRATION.md
// Source: https://github.com/GTNewHorizons/EnderIO/blob/2.10.48/src/main/java/crazypants/enderio/api/IMC.java#L72-L76
// (the poweredSpawner:blacklist:add string message)
// Source:
// https://github.com/GTNewHorizons/EnderIO/blob/2.10.48/src/main/java/crazypants/enderio/machine/spawner/BlockPoweredSpawner.java#L238-L318
// (BreakEvent and HarvestDropsEvent: drop and extra cycles only when !isBlackListed(name))
// Source:
// https://github.com/GTNewHorizons/EnderIO/blob/2.10.48/src/main/java/crazypants/enderio/EnderIO.java#L846-L864
// (IMCEvent and loadComplete both feed addToBlacklist; the user JSON is read before, in the config's constructor)
public final class EnderIoSpawners {

    /** EnderIO's mod id. */
    public static final String MOD_ID = "EnderIO";
    /** The IMC key that adds one entity name to the Powered Spawner blacklist. */
    public static final String BLACKLIST_ADD = "poweredSpawner:blacklist:add";

    private EnderIoSpawners() {}

    /**
     * The decision, pure: which entity names to put on EnderIO's blacklist.
     *
     * @param enderIoLoaded whether EnderIO is loaded
     * @param enabled       the {@code compat.enderioSpawnerBlacklist} switch
     * @param registered    the NPC kinds registered this run
     * @return the full entity name of every registered NPC, in order; empty without EnderIO or with the switch off
     */
    public static List<String> blacklist(boolean enderIoLoaded, boolean enabled, Collection<NpcMob> registered) {
        if (!enderIoLoaded || !enabled) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (NpcMob mob : registered) {
            out.add(mob.fullName());
        }
        return out;
    }

    /**
     * Sends the blacklist messages when EnderIO is loaded and {@code compat.enderioSpawnerBlacklist} is on. Call during
     * init, after {@link NpcRegistry#register}.
     */
    public static void send() {
        List<String> names = blacklist(
            Loader.isModLoaded(MOD_ID),
            CompatConfig.enderioSpawnerBlacklist,
            NpcRegistry.registered());
        for (String name : names) {
            FMLInterModComms.sendMessage(MOD_ID, BLACKLIST_ADD, name);
        }
        if (!names.isEmpty()) {
            GregGuns.LOG.info("Asked EnderIO to blacklist {} GregGuns NPCs from its spawner drops", names.size());
        }
    }
}
