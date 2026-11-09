package com.gregguns.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.event.ForgeEventFactory;

import com.gregguns.core.config.NpcConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.entity.npc.spawn.NpcSpawnHandler;
import com.gregguns.entity.npc.spawn.SpawnRealm;
import com.gregguns.entity.npc.spawn.SpawnTable;

import cpw.mods.fml.common.eventhandler.Event;

/**
 * Scenarios of the Nether and End spawn tables (D353, GGS-10-41 to GGS-10-43). Each runs in the real Nether or End
 * world of the test server: the realm's spawn lists hold the GregGuns rows (what the vanilla spawner and Mobs Info
 * read), the world's own creature picker draws them, and forced spawn attempts on a built pad go through the real
 * {@code CheckSpawn} event (every handler on the bus, Hardcore Ender Expansion's and EnderZoo's included in the real
 * pack), {@code getCanSpawnHere} and {@code SpecialSpawn}. Every block is restored and every NPC removed. Dev
 * environment only. Server side.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/spawning.md (GGS-10-41 to GGS-10-43), D353
final class NpcRealmScenarios {

    /** Draws of the world's creature picker; GregGuns' rows are a small share of each realm's list. */
    private static final int DRAWS = 4000;

    private NpcRealmScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new NetherSpawn());
        registry.register(new EndSpawn());
    }

    /** Blocks changed in another dimension, with their old state, restored in reverse order. */
    private static final class Patch {

        private final WorldServer world;
        private final Map<Long, Object[]> before = new LinkedHashMap<>();
        private final List<Entity> spawned = new ArrayList<>();

        Patch(WorldServer world) {
            this.world = world;
        }

        /** A floor of {@code floor} with four blocks of air over it, 2 out each way from the column. */
        void pad(int x, int y, int z, Block floor) {
            world.getChunkProvider()
                .loadChunk(x >> 4, z >> 4); // dev scenario only: the spot must exist
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    set(x + dx, y - 1, z + dz, floor);
                    for (int dy = 0; dy < 4; dy++) {
                        set(x + dx, y + dy, z + dz, Blocks.air);
                    }
                }
            }
        }

        private void set(int x, int y, int z, Block b) {
            long k = ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
            if (!before.containsKey(k)) {
                before.put(k, new Object[] { x, y, z, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z) });
            }
            world.setBlock(x, y, z, b, 0, 2);
        }

        void restore() {
            for (Entity e : spawned) {
                world.removeEntity(e);
                e.setDead();
            }
            List<Object[]> all = new ArrayList<>(before.values());
            for (int i = all.size() - 1; i >= 0; i--) {
                Object[] o = all.get(i);
                world.setBlock((Integer) o[0], (Integer) o[1], (Integer) o[2], (Block) o[3], (Integer) o[4], 2);
            }
        }
    }

    /** The outcome of one forced natural spawn attempt. */
    private static final class Attempt {

        Event.Result check;
        boolean canSpawnHere;
        NpcEntity spawned;
    }

    /**
     * One natural spawn attempt as {@code SpawnerAnimals} makes it: the {@code CheckSpawn} event, then (unless denied)
     * {@code getCanSpawnHere}, then (if both pass and {@code place}) the spawn and the {@code SpecialSpawn} event.
     */
    private static Attempt attempt(Patch patch, NpcMob mob, double x, double y, double z, boolean place) {
        Attempt a = new Attempt();
        Entity e = EntityList.createEntityByName(mob.fullName(), patch.world);
        if (!(e instanceof NpcEntity)) {
            return a;
        }
        NpcEntity npc = (NpcEntity) e;
        npc.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        a.check = ForgeEventFactory.canEntitySpawn(npc, patch.world, (float) x, (float) y, (float) z);
        if (a.check == Event.Result.DENY) {
            return a;
        }
        a.canSpawnHere = a.check == Event.Result.ALLOW || npc.getCanSpawnHere();
        if (a.canSpawnHere && place) {
            patch.world.spawnEntityInWorld(npc);
            patch.spawned.add(npc);
            if (!ForgeEventFactory.doSpecialSpawn(npc, patch.world, (float) x, (float) y, (float) z)) {
                npc.onSpawnWithEgg(null);
            }
            a.spawned = npc;
        }
        return a;
    }

    /** Counts the GregGuns kinds the world's own picker draws at a spot, as the vanilla spawner asks it. */
    private static Map<NpcMob, Integer> draws(WorldServer world, int x, int y, int z) {
        Map<NpcMob, Integer> out = new EnumMap<>(NpcMob.class);
        for (int i = 0; i < DRAWS; i++) {
            BiomeGenBase.SpawnListEntry entry = world.spawnRandomCreature(EnumCreatureType.monster, x, y, z);
            if (entry != null) {
                NpcMob mob = NpcMob.byName((String) EntityList.classToStringMapping.get(entry.entityClass));
                if (mob != null) {
                    out.merge(mob, 1, Integer::sum);
                }
            }
        }
        return out;
    }

    /** True when the biome's monster list holds the bound class of every row of the realm. */
    private static void checkLists(ScenarioResult.Builder out, BiomeGenBase biome, SpawnRealm realm) {
        SpawnTable table = SpawnTable.build(SpawnTable.Settings.fromConfig(), NpcRegistry::isRegistered);
        List<?> list = biome.getSpawnableList(EnumCreatureType.monster);
        for (SpawnTable.Entry e : table.entries(realm)) {
            boolean found = false;
            for (Object o : list) {
                BiomeGenBase.SpawnListEntry s = (BiomeGenBase.SpawnListEntry) o;
                if (s.entityClass == NpcRegistry.boundClass(e.mob())) {
                    found = true;
                    out.checkEquals(e.mob() + " weight in " + biome.biomeName, e.weight(), s.itemWeight);
                    out.checkEquals(e.mob() + " largest group", e.groupMax(), s.maxGroupCount);
                }
            }
            out.check(found, e.mob() + " is in the monster list of " + biome.biomeName + " (Mobs Info reads it)");
        }
    }

    private static WorldServer worldFor(int dimension) {
        return MinecraftServer.getServer()
            .worldServerForDimension(dimension);
    }

    private static final class NetherSpawn implements Scenario {

        @Override
        public String name() {
            return "npc.spawn_nether";
        }

        @Override
        public String description() {
            return "In the Nether, the biome's monster list holds the Zombie Pigman Soldier, the three Super Mutants and"
                + " the Cyberdemon at their weights, and the world's creature picker draws them. Forced spawn attempts"
                + " on a netherrack pad: a Pigman Soldier spawns by the portal-side spot, a Cyberdemon is denied there"
                + " (danger) and spawns 400 Nether blocks out with its danger 4 gear roll, and a second one beside it is"
                + " denied (boss spacing). An Overworld Bandit is denied in the Nether.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            WorldServer nether = worldFor(-1);
            if (nether == null || SpawnRealm.of(-1, SpawnRealm.Dimensions.fromConfig()) != SpawnRealm.NETHER) {
                return ScenarioResult.skip(name(), "no Nether world, or -1 is not a Nether spawn dimension");
            }
            ChunkCoordinates sp = nether.getSpawnPoint();
            int nx = sp.posX / SpawnRealm.NETHER_SCALE;
            int nz = sp.posZ / SpawnRealm.NETHER_SCALE;
            int y = 70;
            int fx = nx + 400;
            Patch patch = new Patch(nether);
            EnumDifficulty difficulty = nether.difficultySetting;
            try {
                nether.difficultySetting = EnumDifficulty.NORMAL; // monsters never spawn on Peaceful
                patch.pad(nx, y, nz, Blocks.netherrack);
                patch.pad(fx, y, nz, Blocks.netherrack);
                patch.pad(fx + 6, y, nz, Blocks.netherrack);
                checkLists(out, nether.getBiomeGenForCoords(fx, nz), SpawnRealm.NETHER);
                Map<NpcMob, Integer> drawn = draws(nether, fx, y, nz);
                out.check(
                    drawn.getOrDefault(NpcMob.ZOMBIE_PIGMAN_SOLDIER, 0) > 0,
                    "the Nether's picker draws the Pigman Soldier: " + drawn);
                out.check(
                    drawn.getOrDefault(NpcMob.SUPER_MUTANT_BASIC, 0) > 0,
                    "the Nether's picker draws the Super Mutant: " + drawn);

                out.checkEquals("danger by the portal-side spot", 1, NpcSpawnHandler.dangerAt(nether, nx, nz));
                out.checkEquals("danger 400 Nether blocks out", 4, NpcSpawnHandler.dangerAt(nether, fx + 0.5, nz));
                Attempt pig = attempt(patch, NpcMob.ZOMBIE_PIGMAN_SOLDIER, nx + 0.5, y, nz + 0.5, true);
                out.check(pig.check != Event.Result.DENY, "the Pigman Soldier passes CheckSpawn: " + pig.check);
                out.check(pig.spawned != null, "the Pigman Soldier spawns on the pad");
                if (pig.spawned != null) {
                    out.checkEquals("the Pigman Soldier's danger", 1, pig.spawned.danger());
                }
                Attempt early = attempt(patch, NpcMob.CYBER_DEMON, nx + 1.5, y, nz + 0.5, false);
                out.checkEquals("no Cyberdemon by the portal-side spot", Event.Result.DENY, early.check);
                Attempt elite = attempt(patch, NpcMob.SUPER_MUTANT_ELITE, nx + 1.5, y, nz + 1.5, false);
                out.checkEquals("no Elite Super Mutant there either", Event.Result.DENY, elite.check);

                Attempt demon = attempt(patch, NpcMob.CYBER_DEMON, fx + 0.5, y, nz + 0.5, true);
                out.check(demon.check != Event.Result.DENY, "the Cyberdemon passes CheckSpawn deep out");
                out.check(demon.spawned != null, "the Cyberdemon spawns deep out");
                if (demon.spawned != null) {
                    out.checkEquals("the Cyberdemon's danger", 4, demon.spawned.danger());
                    out.check(demon.spawned.getHeldItem() != null, "the Cyberdemon rolled its Nether Blaster");
                }
                Attempt second = attempt(patch, NpcMob.CYBER_DEMON, fx + 6.5, y, nz + 0.5, false);
                out.checkEquals("a second Cyberdemon within the spacing", Event.Result.DENY, second.check);
                Attempt bandit = attempt(patch, NpcMob.BANDIT, fx + 6.5, y, nz + 1.5, false);
                out.checkEquals("an Overworld kind in the Nether", Event.Result.DENY, bandit.check);
                out.note("Nether draws over " + DRAWS + ": " + drawn);
            } finally {
                nether.difficultySetting = difficulty;
                patch.restore();
            }
            return out.build();
        }
    }

    private static final class EndSpawn implements Scenario {

        @Override
        public String name() {
            return "npc.spawn_end";
        }

        @Override
        public String description() {
            return "In the End, the biome's monster list holds the Commando squad row and the world's creature picker"
                + " draws it. Forced spawn attempts on an end stone pad: a Commando is denied 100 blocks from the"
                + " center (the dragon's island stays clear) and spawns 600 blocks out at End danger 3; a Zombie Pigman"
                + " Soldier is denied in the End.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            WorldServer end = worldFor(1);
            if (end == null || SpawnRealm.of(1, SpawnRealm.Dimensions.fromConfig()) != SpawnRealm.END) {
                return ScenarioResult.skip(name(), "no End world, or 1 is not an End spawn dimension");
            }
            int y = 70;
            int near = 100;
            int far = 600;
            Patch patch = new Patch(end);
            EnumDifficulty difficulty = end.difficultySetting;
            try {
                end.difficultySetting = EnumDifficulty.NORMAL;
                patch.pad(near, y, 0, Blocks.end_stone);
                patch.pad(far, y, 0, Blocks.end_stone);
                checkLists(out, end.getBiomeGenForCoords(far, 0), SpawnRealm.END);
                Map<NpcMob, Integer> drawn = draws(end, far, y, 0);
                out.check(drawn.getOrDefault(NpcMob.COMMANDO, 0) > 0, "the End's picker draws Commandos: " + drawn);
                out.checkEquals("End danger 600 out", 3, NpcSpawnHandler.dangerAt(end, far + 0.5, 0.5));
                Attempt close = attempt(patch, NpcMob.COMMANDO, near + 0.5, y, 0.5, false);
                out.checkEquals(
                    "no Commando within " + NpcConfig.endMinDistance + " of the center",
                    Event.Result.DENY,
                    close.check);
                Attempt squad = attempt(patch, NpcMob.COMMANDO, far + 0.5, y, 0.5, true);
                out.check(squad.check != Event.Result.DENY, "a Commando passes CheckSpawn far out: " + squad.check);
                out.check(squad.canSpawnHere, "a Commando may stand on the dark end stone pad");
                out.check(squad.spawned != null, "a Commando spawns far out");
                if (squad.spawned != null) {
                    out.checkEquals("the Commando's danger", 3, squad.spawned.danger());
                    out.check(squad.spawned.getHeldItem() != null, "the Commando rolled a gun");
                    out.check(!((EntityLiving) squad.spawned).isNoDespawnRequired(), "End Commandos despawn as usual");
                }
                Attempt pig = attempt(patch, NpcMob.ZOMBIE_PIGMAN_SOLDIER, far + 1.5, y, 1.5, false);
                out.checkEquals("a Nether kind in the End", Event.Result.DENY, pig.check);
                out.note("End draws over " + DRAWS + ": " + drawn);
            } finally {
                end.difficultySetting = difficulty;
                patch.restore();
            }
            return out.build();
        }
    }
}
