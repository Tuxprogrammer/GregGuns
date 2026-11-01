package com.gregguns.entity.npc.spawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
 * of their realm's biomes (so they share the monster cap, GGS-10-25, Mobs Info lists their biomes, and the pack can
 * retune them with EnderZoo, GGS-10-27): the Overworld rows in every biome that is neither Nether nor End, the Nether
 * rows in the Nether biomes and the End rows in the End biomes (Hardcore Ender Expansion's End keeps other mods' rows
 * with its pack config, {@code overrideBiomeMobs=false}). The {@code CheckSpawn} handler then denies a candidate that
 * fails {@link SpawnGate} (realm, faction flag, danger, the End keep-out, caps, boss spacing), and the
 * {@code SpecialSpawn} handler gives a freshly spawned NPC its danger and rolls its equipment with it. No placeholder
 * entity exists (GGS-10-26). Eggs and camps do not pass through here. Server side.
 *
 * <p>
 * Usage: {@code NpcSpawnHandler.registerSpawns()} in postInit; register an instance on the Forge event bus.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md (GGS-10-23 to GGS-10-27, GGS-10-41 to GGS-10-43),
// docs/techguns-spec/10-entities-npcs/spawning.md (1.3)
// Source: Hardcore Ender Expansion 1.12.28-GTNH swaps the End biome in preInit and keeps other mods' monster rows
// unless overrideBiomeMobs is on (the pack has it off):
// https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/blob/1.12.28-GTNH/src/main/java/chylex/hee/world/DimensionOverride.java#L27-L33
// https://github.com/GTNewHorizons/Hardcore-Ender-Expansion/blob/1.12.28-GTNH/src/main/java/chylex/hee/world/biome/BiomeGenHardcoreEnd.java#L29-L43
public final class NpcSpawnHandler {

    /**
     * Registers every natural-spawn row that is enabled, has weight above 0 and is bound to a class, in the biomes of
     * its realm. Mobs not implemented yet are skipped. Call once in postInit.
     */
    public static void registerSpawns() {
        SpawnTable table = SpawnTable.build(SpawnTable.Settings.fromConfig(), NpcRegistry::isRegistered);
        Map<SpawnRealm, BiomeGenBase[]> biomes = new EnumMap<>(SpawnRealm.class);
        for (SpawnRealm realm : SpawnRealm.values()) {
            biomes.put(realm, biomesOf(realm));
        }
        for (SpawnTable.Entry e : table.entries()) {
            Class<? extends EntityLiving> clazz = NpcRegistry.boundClass(e.mob());
            BiomeGenBase[] array = biomes.get(e.realm());
            if (clazz != null && array.length > 0) {
                EntityRegistry.addSpawn(clazz, e.weight(), e.groupMin(), e.groupMax(), EnumCreatureType.monster, array);
                GregGuns.LOG.debug(
                    "Spawn: {} weight {} in {} {} biomes",
                    e.mob()
                        .fullName(),
                    e.weight(),
                    array.length,
                    e.realm());
            }
        }
    }

    /**
     * @param realm a realm
     * @return the registered biomes whose spawn lists hold that realm's rows
     */
    public static BiomeGenBase[] biomesOf(SpawnRealm realm) {
        List<BiomeGenBase> out = new ArrayList<>();
        for (BiomeGenBase b : BiomeGenBase.getBiomeGenArray()) {
            if (b != null && realmOfBiome(b) == realm) {
                out.add(b);
            }
        }
        return out.toArray(new BiomeGenBase[0]);
    }

    private static SpawnRealm realmOfBiome(BiomeGenBase b) {
        if (BiomeDictionary.isBiomeOfType(b, Type.NETHER)) {
            return SpawnRealm.NETHER;
        }
        return BiomeDictionary.isBiomeOfType(b, Type.END) ? SpawnRealm.END : SpawnRealm.OVERWORLD;
    }

    /**
     * @param world a world
     * @param x     block X
     * @param z     block Z
     * @return the spawn danger 0 to 5 there: by the rule of the dimension's realm ({@link SpawnRealm#danger}); a
     *         dimension in no spawn list (a planet with a silo complex) uses the Overworld rule
     */
    public static int dangerAt(World world, double x, double z) {
        SpawnRealm realm = SpawnRealm.of(world.provider.dimensionId, SpawnRealm.Dimensions.fromConfig());
        double distance;
        if (realm == SpawnRealm.END) {
            distance = Math.sqrt(x * x + z * z);
        } else {
            ChunkCoordinates spawn = world.getSpawnPoint();
            // In the Nether the spawn point is the Overworld's (shared world info): measure where the spot leads.
            double scale = realm == SpawnRealm.NETHER ? SpawnRealm.NETHER_SCALE : 1.0;
            double dx = x - spawn.posX / scale;
            double dz = z - spawn.posZ / scale;
            distance = Math.sqrt(dx * dx + dz * dz);
        }
        BiomeGenBase biome = world.getBiomeGenForCoords((int) Math.floor(x), (int) Math.floor(z));
        return (realm == null ? SpawnRealm.OVERWORLD : realm).danger(
            distance,
            biomeClass(biome).danger(),
            NpcConfig.distanceLevel0,
            NpcConfig.distanceLevel1,
            NpcConfig.distanceLevel2);
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
        SpawnRealm realm = SpawnRealm.of(world.provider.dimensionId, SpawnRealm.Dimensions.fromConfig());
        double center = Math.sqrt(x * x + z * z);
        SpawnGate.Limits limits = SpawnGate.Limits.fromConfig();
        // Cheap rules first: a pass with no caps and nothing counted yields the rule-only answer.
        SpawnGate.Decision rules = SpawnGate
            .decide(table, mob, realm, danger, center, 0, 0, 0, SpawnGate.Limits.rulesOnly(limits.endMinDistance));
        if (rules != SpawnGate.Decision.ALLOW) {
            return rules;
        }
        int inChunk = NpcCounter.countInChunk(world, (int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        EntityPlayer near = world.getClosestPlayer(x, y, z, -1.0);
        int nearPlayer = near == null ? 0 : NpcCounter.countNearPlayer(world, near);
        SpawnTable.Entry entry = table.entry(mob, realm);
        int bosses = entry != null && entry.boss() ? bossesNear(world, mob, x, z, NpcConfig.bossSpacing) : 0;
        return SpawnGate.decide(table, mob, realm, danger, center, inChunk, nearPlayer, bosses, limits);
    }

    /**
     * Counts the loaded NPCs of one kind within a square of the given half-size around a spot, full height. Never
     * loads a chunk (WGEN-27).
     *
     * @param world   the world
     * @param mob     the kind
     * @param x       block X
     * @param z       block Z
     * @param spacing the half-size, blocks
     * @return how many stand there
     */
    public static int bossesNear(World world, NpcMob mob, double x, double z, int spacing) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        return NpcCounter.countInBox(
            world,
            Collections.singletonList(mob.registryName()),
            bx - spacing,
            0,
            bz - spacing,
            bx + spacing,
            255,
            bz + spacing);
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
