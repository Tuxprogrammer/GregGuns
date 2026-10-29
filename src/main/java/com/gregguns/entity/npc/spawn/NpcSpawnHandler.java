package com.gregguns.entity.npc.spawn;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

import com.gregguns.GregGuns;
import com.gregguns.core.config.NpcConfig;
import com.gregguns.entity.npc.NpcCounter;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.EntityRegistry;

/**
 * Natural NPC spawning. {@link #registerSpawns} adds the real NPC classes to the vanilla {@code monster} spawn lists
 * (so they share the monster cap, GGS-10-25, and the pack can retune them with EnderZoo, GGS-10-27). The
 * {@code CheckSpawn} handler then denies a candidate that fails {@link SpawnGate} (dimension, faction flag, danger,
 * caps), and the {@code SpecialSpawn} handler gives a freshly spawned NPC its danger and rolls its equipment with it.
 * No placeholder entity exists (GGS-10-26). Eggs and camps do not pass through here. Server side.
 *
 * <p>
 * Usage: {@code NpcSpawnHandler.registerSpawns()} in postInit; register an instance on the Forge event bus.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md (GGS-10-23 to GGS-10-27),
// docs/techguns-spec/10-entities-npcs/spawning.md (1.3)
public final class NpcSpawnHandler {

    private static final int GROUP_MIN = 1;
    private static final int GROUP_MAX = 2;

    /**
     * Registers every natural-spawn mob that is enabled, has weight above 0 and is bound to a class, in every biome
     * except the Nether and End ones. Mobs not implemented yet are skipped. Call once in postInit.
     */
    public static void registerSpawns() {
        SpawnTable table = SpawnTable.build(SpawnTable.Settings.fromConfig(), NpcRegistry::isRegistered);
        List<BiomeGenBase> biomes = new ArrayList<>();
        for (BiomeGenBase b : BiomeGenBase.getBiomeGenArray()) {
            if (b != null && !BiomeDictionary.isBiomeOfType(b, Type.NETHER)
                && !BiomeDictionary.isBiomeOfType(b, Type.END)) {
                biomes.add(b);
            }
        }
        BiomeGenBase[] array = biomes.toArray(new BiomeGenBase[0]);
        for (SpawnTable.Entry e : table.entries()) {
            Class<? extends EntityLiving> clazz = NpcRegistry.boundClass(e.mob());
            if (clazz != null) {
                EntityRegistry.addSpawn(clazz, e.weight(), GROUP_MIN, GROUP_MAX, EnumCreatureType.monster, array);
                GregGuns.LOG.debug(
                    "Spawn: {} weight {} in {} biomes",
                    e.mob()
                        .fullName(),
                    e.weight(),
                    array.length);
            }
        }
    }

    /**
     * @param world a world
     * @param x     block X
     * @param z     block Z
     * @return the spawn danger 0 to 5 there: distance from the world spawn plus the biome
     */
    public static int dangerAt(World world, double x, double z) {
        ChunkCoordinates spawn = world.getSpawnPoint();
        double dx = x - spawn.posX;
        double dz = z - spawn.posZ;
        int distance = SpawnDanger.distance(
            Math.sqrt(dx * dx + dz * dz),
            NpcConfig.distanceLevel0,
            NpcConfig.distanceLevel1,
            NpcConfig.distanceLevel2);
        BiomeGenBase biome = world.getBiomeGenForCoords((int) Math.floor(x), (int) Math.floor(z));
        return SpawnDanger.total(distance, biomeClass(biome).danger());
    }

    /**
     * Maps a biome to its danger class. The first match wins: Nether and End 0; wasteland, desert, frozen, swamp and
     * magical 2; mountain and cold biomes 1; everything else 0.
     *
     * @param biome a biome, may be null
     * @return its class
     */
    public static SpawnDanger.BiomeClass biomeClass(BiomeGenBase biome) {
        if (biome == null || BiomeDictionary.isBiomeOfType(biome, Type.NETHER)
            || BiomeDictionary.isBiomeOfType(biome, Type.END)) {
            return SpawnDanger.BiomeClass.NEUTRAL;
        }
        if (BiomeDictionary.isBiomeOfType(biome, Type.WASTELAND) || BiomeDictionary.isBiomeOfType(biome, Type.DESERT)
            || BiomeDictionary.isBiomeOfType(biome, Type.FROZEN)
            || BiomeDictionary.isBiomeOfType(biome, Type.SWAMP)
            || BiomeDictionary.isBiomeOfType(biome, Type.MAGICAL)) {
            return SpawnDanger.BiomeClass.HARSH;
        }
        if (BiomeDictionary.isBiomeOfType(biome, Type.MOUNTAIN) || biome.temperature < 0.2F) {
            return SpawnDanger.BiomeClass.SPARSE;
        }
        return SpawnDanger.BiomeClass.NEUTRAL;
    }

    /**
     * Decides a natural spawn of one kind at a position. Counting never loads chunks (WGEN-27).
     *
     * @param mob   the candidate kind
     * @param world the world
     * @param x     block X
     * @param y     block Y
     * @param z     block Z
     * @return the gate's decision
     */
    public static SpawnGate.Decision decide(NpcMob mob, World world, double x, double y, double z) {
        return decide(
            SpawnTable.build(SpawnTable.Settings.fromConfig(), NpcRegistry::isRegistered),
            mob,
            world,
            x,
            y,
            z);
    }

    /**
     * As {@link #decide(NpcMob, World, double, double, double)} with an explicit table (scenarios use one that treats
     * every mob as bound).
     *
     * @param table the spawn table
     * @param mob   the candidate kind
     * @param world the world
     * @param x     block X
     * @param y     block Y
     * @param z     block Z
     * @return the gate's decision
     */
    public static SpawnGate.Decision decide(SpawnTable table, NpcMob mob, World world, double x, double y, double z) {
        if (!SpawnGate.isNatural(mob)) {
            return SpawnGate.Decision.NOT_MANAGED;
        }
        int danger = dangerAt(world, x, z);
        int dim = world.provider.dimensionId;
        // Cheap rules first: a pass with caps at 0 and no counting yields the rule-only answer.
        SpawnGate.Decision rules = SpawnGate
            .decide(table, mob, dim, NpcConfig.spawnDimensions, danger, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        if (rules != SpawnGate.Decision.ALLOW) {
            return rules;
        }
        int inChunk = NpcCounter.countInChunk(world, (int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        EntityPlayer near = world.getClosestPlayer(x, y, z, -1.0);
        int nearPlayer = near == null ? 0 : NpcCounter.countNearPlayer(world, near);
        return SpawnGate.decide(
            table,
            mob,
            dim,
            NpcConfig.spawnDimensions,
            danger,
            inChunk,
            nearPlayer,
            NpcConfig.perChunkCap,
            NpcConfig.perPlayerCap);
    }

    /**
     * Marks every GregGuns natural-spawn candidate, so it keeps the full vanilla spot rules even beside a GregGuns
     * spawner ({@link SpawnerOrigin}), then denies a natural spawn that fails the gate. Forge fires this event only
     * from natural spawning, before {@code getCanSpawnHere}.
     */
    @SubscribeEvent
    public void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.entityLiving instanceof SpawnerOrigin.NaturalCandidate) {
            ((SpawnerOrigin.NaturalCandidate) event.entityLiving).markNaturalCandidate();
        }
        if (event.world.isRemote || !(event.entityLiving instanceof NpcEntity)) {
            return;
        }
        NpcMob mob = NpcMob.byName(EntityList.getEntityString(event.entityLiving));
        SpawnGate.Decision d = decide(mob, event.world, event.x, event.y, event.z);
        if (d != SpawnGate.Decision.NOT_MANAGED && d != SpawnGate.Decision.ALLOW) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** Carries the danger of the spawn position onto the new NPC, then rolls its equipment with it. */
    @SubscribeEvent
    public void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        if (event.world.isRemote || !(event.entityLiving instanceof NpcEntity)) {
            return;
        }
        NpcEntity npc = (NpcEntity) event.entityLiving;
        if (npc.danger() == NpcEntity.DANGER_UNSET) {
            npc.setDanger(dangerAt(event.world, event.x, event.z));
            npc.rollEquipment();
        }
    }
}
