package com.gregguns.entity.npc;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.core.config.NpcConfig;
import com.gregguns.utility.camp.CampSpawnPort;

/**
 * The NPC module's side of the Camp Flag contract (M8): counts, spawns and answers a broken flag.
 *
 * <p>
 * Contract: counting never loads a chunk (WGEN-27); spawning only happens when the flag ticks and refuses a spot whose
 * chunk is not loaded; names are matched through {@link NpcMob#byName}, so short and full names agree (NPC-17). World
 * generation never spawns NPCs itself: it places and initializes {@code TileCampFlag} and this port does the rest from
 * the flag's tick. Installed by {@code NpcModule} into {@code CampSpawnPorts}. Server side.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md section 2, docs/techguns-spec/10-entities-npcs/
// spawning.md section 4
public final class NpcCampPort implements CampSpawnPort {

    /** Horizontal distance (blocks) from the broken flag at which the helicopter appears. */
    static final double HELICOPTER_DISTANCE = 48.0;
    /** Height (blocks) above the flag at which the helicopter appears. */
    static final double HELICOPTER_HEIGHT = 20.0;
    /** Only a player this close (blocks) to the broken flag is hunted. */
    static final double HELICOPTER_PLAYER_RANGE = 64.0;

    @Override
    public int countNpcs(World world, List<String> names, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return NpcCounter.countInBox(world, names, minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public boolean spawn(World world, String npcName, double x, double y, double z) {
        NpcMob mob = NpcMob.byName(npcName);
        if (world.isRemote || mob == null || !NpcRegistry.isRegistered(mob)) {
            return false;
        }
        int cx = (int) Math.floor(x) >> 4;
        int cz = (int) Math.floor(z) >> 4;
        if (!world.getChunkProvider()
            .chunkExists(cx, cz)) {
            return false;
        }
        Entity e = EntityList.createEntityByName(mob.fullName(), world);
        if (!(e instanceof EntityLiving)) {
            return false;
        }
        EntityLiving living = (EntityLiving) e;
        living.setLocationAndAngles(x, y, z, world.rand.nextFloat() * 360.0F, 0.0F);
        living.onSpawnWithEgg(null);
        return world.spawnEntityInWorld(living);
    }

    @Override
    public void onFlagBroken(World world, int x, int y, int z) {
        if (world.isRemote || !NpcConfig.helicopter || !NpcRegistry.isRegistered(NpcMob.ATTACK_HELICOPTER)) {
            return;
        }
        EntityPlayer target = world.getClosestPlayer(x + 0.5, y, z + 0.5, HELICOPTER_PLAYER_RANGE);
        if (target == null) {
            return;
        }
        double angle = world.rand.nextDouble() * Math.PI * 2;
        double hx = x + 0.5 + Math.cos(angle) * HELICOPTER_DISTANCE;
        double hz = z + 0.5 + Math.sin(angle) * HELICOPTER_DISTANCE;
        if (!spawn(world, NpcMob.ATTACK_HELICOPTER.fullName(), hx, y + HELICOPTER_HEIGHT, hz)) {
            GregGuns.LOG.debug("Camp flag at {} {} {} broke but the helicopter could not spawn", x, y, z);
        }
    }
}
