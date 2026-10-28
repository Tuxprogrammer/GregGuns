package com.gregguns.worldgen.spawner;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;

import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.worldgen.RolledCeiling;
import com.gregguns.worldgen.StructureHooks;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.WorldgenSettings;

/**
 * Places GregGuns spawners: each spawner mark becomes a vanilla {@code minecraft:mob_spawner} whose tile NBT
 * ({@link SpawnerNbt}) spawns the kind's registered NPCs. As the spawner step of a stamped structure it answers the
 * {@code spawner.zombie_hole} and {@code spawner.bug_hole} marks; ICBM complexes call {@link #place} for their posts.
 * Only places and configures; vanilla spawns, on the spawner's own tick, in a loaded chunk, with a player near. A kind
 * with no registered NPC this run places nothing, so a spawner never names a missing entity. Writes only through the
 * clipped sink. Server side.
 */
// Spec: PLAN-M8 G11, docs/gregguns-spec/11-worldgen/structure-placement.md (spawner marks), GGS-11-08 (clipped writes)
public final class VanillaSpawners implements StructureHooks.SpawnerHook {

    @Override
    public void spawnerMarked(World world, WindowedSink sink, StructurePlan plan, String tag, int x, int y, int z) {
        // A structure on Ross128b or Ross128ba carries its rolled ceiling into the spawner, so its defenders scale
        // (0019).
        place(sink, SpawnerKind.byLayoutTag(tag), x, y, z, WorldgenSettings.rolledCeiling(plan));
    }

    /**
     * @param sink    the clipped sink
     * @param kind    the spawner kind; null places nothing
     * @param x       world X
     * @param y       world Y
     * @param z       world Z
     * @param ceiling the structure's rolled loot ceiling (GT voltage index) or 0 when it did not roll; sets how
     *                dangerous the NPCs start at least
     * @return true when a spawner was placed
     */
    public static boolean place(WindowedSink sink, SpawnerKind kind, int x, int y, int z, int ceiling) {
        NBTTagCompound tag = nbt(kind, x, y, z, ceiling);
        if (tag == null || !sink.setBlock(x, y, z, Blocks.mob_spawner, 0)) {
            return false;
        }
        return sink.setTile(x, y, z, tile(tag));
    }

    /**
     * Places a spawner straight into the world (scenarios and tools; worldgen uses the sink).
     *
     * @return true when a spawner was placed
     */
    public static boolean placeInWorld(World world, SpawnerKind kind, int x, int y, int z, int ceiling) {
        NBTTagCompound tag = nbt(kind, x, y, z, ceiling);
        if (tag == null || !world.setBlock(x, y, z, Blocks.mob_spawner, 0, 2)) {
            return false;
        }
        world.setTileEntity(x, y, z, tile(tag));
        return true;
    }

    /**
     * @param kind    the kind; may be null
     * @param x       world X
     * @param y       world Y
     * @param z       world Z
     * @param ceiling the rolled ceiling, or 0
     * @return the spawner tile NBT, or null when the kind is null or none of its NPCs is registered
     */
    public static NBTTagCompound nbt(SpawnerKind kind, int x, int y, int z, int ceiling) {
        if (kind == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (NpcMob mob : kind.registered(NpcRegistry::isRegistered)) {
            names.add(mob.fullName());
        }
        if (names.isEmpty()) {
            return null;
        }
        // The first cycle's mob is a function of the position, so regenerating the same spot gives the same spawner.
        int first = (x * 31 + y * 17 + z * 13) & 0xFFFF;
        NBTTagCompound tag = SpawnerNbt.build(names, first, kind.settings(), RolledCeiling.danger(ceiling));
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        return tag;
    }

    /**
     * @param tile a tile entity; may be null
     * @return its saved NBT when it is a mob spawner, else null (for scenarios that read a placed spawner back)
     */
    public static NBTTagCompound read(TileEntity tile) {
        if (!(tile instanceof TileEntityMobSpawner)) {
            return null;
        }
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        return tag;
    }

    private static TileEntityMobSpawner tile(NBTTagCompound tag) {
        TileEntityMobSpawner tile = new TileEntityMobSpawner();
        tile.readFromNBT(tag);
        return tile;
    }
}
