package com.gregguns.core.testing;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;

import com.gregguns.entity.npc.loot.InfernalOptOut;

/**
 * Keeps pack mods from swapping, healing or modifying the mobs a scenario spawns. In the GTNH pack two mods change
 * every hostile
 * mob that joins the world, so a scenario that keeps its own {@code new EntityZombie} reference ends up holding a dead
 * entity whose health never moves (the "lost NaN" reports):
 * <ul>
 * <li><b>SpecialMobs</b> queues each replaceable {@code IMob} for replacement at the end of the next server tick, then
 * removes the original and spawns a random variant (Plague Zombie, ...). It skips mobs that
 * {@code isNoDespawnRequired()} or carry a non-zero {@code smi} tag; the guard uses the tag so mobs still despawn.
 * Source:
 * {@code MobHelper.canReplace} and {@code TickHandler.onServerTick} at
 * https://github.com/GTNewHorizons/SpecialMobs/blob/3.7.8/src/main/java/toast/specialMobs/MobHelper.java and
 * https://github.com/GTNewHorizons/SpecialMobs/blob/3.7.8/src/main/java/toast/specialMobs/TickHandler.java</li>
 * <li><b>Infernal Mobs</b> turns a random share of joining {@code IMob}s into elites (extra abilities and health). On
 * join it uses the {@code InfernalMobsMod} tag when it is not empty and skips the random roll; a tag that names no
 * ability yields no modifier. Source: {@code EntityEventHandler.onEntityJoinedWorld} and
 * {@code InfernalMobsCore.processEntitySpawn} at
 * https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/</li>
 * <li><b>EnderZoo</b> difficulty scaling: every joining {@code IMob} is queued and, at the end of the next server tick,
 * gets its base max health and base attack damage multiplied by the difficulty modifier (0.9 on easy, 1.1 on hard in
 * the
 * pack's {@code config/enderzoo/EnderZoo.cfg}) and is then set to full health, which also revives a mob a scenario has
 * just emptied or whose max health the scenario set. It skips a mob whose entity data has {@code ezModsApp} true.
 * Source: {@code MobSpawnEventHandler.onEntityJoinWorld}, {@code onServerTick} and {@code addjustBaseHealth} at
 * https://github.com/GTNewHorizons/EnderZoo/blob/1.3.6/src/main/java/crazypants/enderzoo/spawn/MobSpawnEventHandler.java</li>
 * <li><b>AngerMod</b> spawn protection: every player that joins a world (fake players included) gets
 * {@code capabilities.disableDamage = true} for up to 90 s while it stays within 5 blocks, and monsters never target a
 * player who cannot be damaged. A scenario's victim player was therefore untargetable and unhurtable (Psycho Steve
 * "never hit", NPCs that never fire). Source: {@code PlayerSpawnProtection.onPlayerSpawn} and
 * {@code protectPlayer} at
 * https://github.com/GTNewHorizons/AngerMod/blob/1.0.7/src/main/java/com/github/namikon/angermod/events/PlayerSpawnProtection.java;
 * the 90 s and 5 block values are the pack's {@code config/angermod.cfg}. Its tick handler ignores a player whose
 * {@code disableDamage} is already false, so clearing the flag ends the protection for good.</li>
 * </ul>
 * None of these mods is a dependency: both are reached only through the tags they read. Dev and test hooks only.
 */
public final class PackMobGuard {

    /** Infernal Mobs' saved-modifier tag; any non-empty value without a known ability keeps the mob plain. */
    static final String INFERNAL_TAG = "InfernalMobsMod";
    /** Value written to {@link #INFERNAL_TAG}: names no ability. */
    static final String INFERNAL_NONE = InfernalOptOut.MARKER;

    /** SpecialMobs' "do not replace" byte; {@code MobHelper.canReplace} requires it to be 0. */
    static final String SPECIAL_MOBS_SKIP = "smi";

    /** EnderZoo's "difficulty modifiers already applied" tag. */
    static final String ENDER_ZOO_APPLIED = "ezModsApp";

    private PackMobGuard() {}

    /**
     * Opts a mob out of pack-mod replacement and elite rolls. Call before the entity joins the world.
     *
     * @param entity the entity about to be spawned; non-mobs are left alone
     */
    public static void protect(Entity entity) {
        if (entity instanceof EntityLiving) {
            // SpecialMobs does not replace a mob with a non-zero smi byte. Not persistenceRequired: that would also
            // stop the mob despawning, which scenarios such as npc.bosses_persist must still observe.
            entity.getEntityData()
                .setByte(SPECIAL_MOBS_SKIP, (byte) 1);
            entity.getEntityData()
                .setString(INFERNAL_TAG, INFERNAL_NONE);
            entity.getEntityData()
                .setBoolean(ENDER_ZOO_APPLIED, true);
        }
    }

    /**
     * Ends the join-time protection some pack mods give a player. Call right after the entity joined the world.
     *
     * @param entity the entity that just joined; non-players are left alone
     */
    public static void afterJoin(Entity entity) {
        if (entity instanceof EntityPlayer) {
            ((EntityPlayer) entity).capabilities.disableDamage = false;
        }
    }
}
