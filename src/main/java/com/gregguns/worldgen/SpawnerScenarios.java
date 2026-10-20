package com.gregguns.worldgen;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import com.gregguns.core.config.NpcConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.InfernalHooks;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.loot.InfernalOptOut;
import com.gregguns.worldgen.PlacementHarness.Placement;
import com.gregguns.worldgen.spawner.BlockHoleSpawner;
import com.gregguns.worldgen.spawner.HoleCaps;
import com.gregguns.worldgen.spawner.HoleKind;
import com.gregguns.worldgen.spawner.HoleSpawnerHook;
import com.gregguns.worldgen.spawner.HoleSpawnerRegistration;
import com.gregguns.worldgen.spawner.HoleSpawning;
import com.gregguns.worldgen.spawner.TileHoleSpawner;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The hole spawner scenarios (M8 G11): the caps, the player gate, the Infernal marker and the Big Bunker's two holes.
 * Every NPC is placed through {@link ScenarioContext#spawn} so cleanup finds it, and every one is killed at the end
 * with
 * its drops cancelled. Dev environment only; needs {@code worldgen} (and so {@code npcs}) in {@code GG_MODULES}.
 * Server side.
 */
// Spec: PLAN-M8 section 2 (Big Bunker spawners) and G11, contract addendum (spawner marker)
public final class SpawnerScenarios {

    private SpawnerScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Caps());
        registry.register(new Idle());
        registry.register(new InfernalMarker());
        registry.register(new BigBunker());
    }

    /** Cancels the drops of the NPCs it was told about, so killing them leaves no item entity. */
    public static final class DropCanceller {

        private final Map<Entity, Boolean> tracked = new IdentityHashMap<>();

        void track(Entity e) {
            tracked.put(e, Boolean.TRUE);
        }

        @SubscribeEvent
        public void onDrops(LivingDropsEvent event) {
            if (tracked.containsKey(event.entityLiving)) {
                event.setCanceled(true);
            }
        }
    }

    /** One scenario's cast: the hole, its player, the NPCs it placed and the cleanup of all of it. */
    private static final class Rig {

        final ScenarioContext ctx;
        final World world;
        final DropCanceller drops = new DropCanceller();
        final List<EntityLiving> npcs = new ArrayList<>();
        final HoleSpawning.Placer placer;
        FakePlayer player;
        TileHoleSpawner tile;

        Rig(ScenarioContext ctx) {
            this.ctx = ctx;
            this.world = ctx.world();
            MinecraftForge.EVENT_BUS.register(drops);
            this.placer = npc -> {
                drops.track(npc);
                npcs.add(npc);
                ctx.spawn(npc); // already positioned by the spawner; spawnAt would move it
                return true;
            };
        }

        /** Clears a box of air and puts a hole of the kind at the origin. */
        TileHoleSpawner hole(HoleKind kind) {
            ctx.clearArea(10, 10);
            ctx.setBlock(0, 0, 0, HoleSpawnerRegistration.block(kind), 0);
            TileEntity t = world.getTileEntity(ctx.originX(), ctx.originY(), ctx.originZ());
            tile = t instanceof TileHoleSpawner ? (TileHoleSpawner) t : null;
            return tile;
        }

        /** A sturdy fake player {@code dx} blocks east of the hole. */
        void playerAt(double dx) {
            if (player == null) {
                String name = "[GGHoleSpawner]";
                player = new FakePlayer(
                    (WorldServer) world,
                    new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
                new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), player);
                player.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                    .setBaseValue(1000.0);
                player.setHealth(1000.0F);
                player.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
                ctx.spawn(player);
            } else {
                player.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            }
        }

        /** Kills every NPC it saw (drops cancelled), and any stray one within reach of the hole. */
        void finish() {
            try {
                List<EntityLiving> all = new ArrayList<>(npcs);
                AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                    ctx.originX() - 40,
                    0,
                    ctx.originZ() - 40,
                    ctx.originX() + 40,
                    255,
                    ctx.originZ() + 40);
                for (Object o : world.getEntitiesWithinAABB(NpcEntity.class, box)) {
                    if (!all.contains(o)) {
                        all.add((EntityLiving) o);
                    }
                }
                for (EntityLiving e : all) {
                    drops.track(e);
                    e.setHealth(0.0F);
                    e.onDeath(DamageSource.generic);
                    e.setDead();
                }
            } finally {
                MinecraftForge.EVENT_BUS.unregister(drops);
                ctx.cleanup();
            }
        }
    }

    /** With a player near, a hole fills to 5 NPCs and never more, and no attempt places more than 3. */
    private static final class Caps implements Scenario {

        @Override
        public String name() {
            return "worldgen.hole_spawner_caps";
        }

        @Override
        public String description() {
            return "A Zombie Hole with a player 6 blocks away places 3, then 2, then 0 NPCs over three attempts (at most 3"
                + " per attempt, 5 near it at most), and more attempts add nothing (PLAN-M8 G11).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Rig rig = new Rig(ctx);
            try {
                TileHoleSpawner tile = rig.hole(HoleKind.ZOMBIE);
                out.check(
                    tile != null && tile.kind() == HoleKind.ZOMBIE,
                    "the Zombie Hole block should carry its tile");
                if (tile == null) {
                    return out.build();
                }
                rig.playerAt(6.0);
                out.checkEquals("NPCs near before any attempt", 0, count(rig, ctx));
                int[] expected = { 3, 2, 0, 0, 0 };
                int total = 0;
                for (int i = 0; i < expected.length; i++) {
                    List<EntityLiving> placed = tile.attempt(HoleSpawning.MARKER, rig.placer);
                    total += placed.size();
                    out.check(placed.size() <= HoleCaps.MAX_PER_ATTEMPT, "attempt " + (i + 1) + " placed more than 3");
                    out.checkEquals("attempt " + (i + 1) + " placed", expected[i], placed.size());
                    out.check(count(rig, ctx) <= HoleCaps.MAX_NEARBY, "more than 5 NPCs near the hole");
                }
                out.checkEquals("NPCs near after five attempts", HoleCaps.MAX_NEARBY, count(rig, ctx));
                out.checkEquals("NPCs placed in all", HoleCaps.MAX_NEARBY, total);
                for (EntityLiving e : rig.npcs) {
                    NpcMob mob = NpcMob.byName(net.minecraft.entity.EntityList.getEntityString(e));
                    out.check(
                        mob != null && HoleKind.ZOMBIE.mobs()
                            .contains(mob),
                        "a zombie hole placed " + mob);
                }
            } finally {
                rig.finish();
            }
            return out.build();
        }

        private static int count(Rig rig, ScenarioContext ctx) {
            return HoleSpawning.countNearby(rig.world, HoleKind.ZOMBIE, ctx.originX(), ctx.originY(), ctx.originZ());
        }
    }

    /** With no player within 24 blocks a hole does nothing, by attempt and by tick; a player at 23 wakes it. */
    private static final class Idle implements Scenario {

        @Override
        public String name() {
            return "worldgen.hole_spawner_idle";
        }

        @Override
        public String description() {
            return "A Zombie Hole with its only player 25 blocks away places nothing, by direct attempt and over 150 ticks"
                + " of its own update; with the player 23 blocks away it places NPCs (player range 24).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Rig rig = new Rig(ctx);
            try {
                TileHoleSpawner tile = rig.hole(HoleKind.ZOMBIE);
                if (tile == null) {
                    out.check(false, "the Zombie Hole block should carry its tile");
                    return out.build();
                }
                rig.playerAt(25.0);
                out.checkEquals(
                    "attempt with the player at 25",
                    0,
                    tile.attempt(HoleSpawning.MARKER, rig.placer)
                        .size());
                for (int i = 0; i < 150; i++) {
                    tile.updateEntity(); // production path: spawns through the world directly, caught by finish()
                }
                out.checkEquals(
                    "NPCs near after 150 idle ticks",
                    0,
                    HoleSpawning.countNearby(rig.world, HoleKind.ZOMBIE, ctx.originX(), ctx.originY(), ctx.originZ()));
                rig.playerAt(23.0);
                out.check(
                    !tile.attempt(HoleSpawning.MARKER, rig.placer)
                        .isEmpty(),
                    "the hole should wake with the player at 23 blocks");
            } finally {
                rig.finish();
            }
            return out.build();
        }
    }

    /** Each NPC a hole places is handed to the Infernal marker before it joins the world. */
    private static final class InfernalMarker implements Scenario {

        @Override
        public String name() {
            return "worldgen.hole_spawner_infernal_marker";
        }

        @Override
        public String description() {
            return "Every NPC a Zombie Hole places is passed to InfernalHooks.markSpawnerNpc exactly once, before it joins"
                + " the world; with Infernal Mobs loaded it carries the opt-out marker in its persistent data.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Rig rig = new Rig(ctx);
            boolean allow = NpcConfig.infernalAllowSpawnerNpcs;
            NpcConfig.infernalAllowSpawnerNpcs = false;
            try {
                TileHoleSpawner tile = rig.hole(HoleKind.ZOMBIE);
                if (tile == null) {
                    out.check(false, "the Zombie Hole block should carry its tile");
                    return out.build();
                }
                rig.playerAt(6.0);
                List<EntityLiving> marked = new ArrayList<>();
                List<Boolean> alreadyInWorld = new ArrayList<>();
                List<Boolean> written = new ArrayList<>();
                HoleSpawning.Marker recording = npc -> {
                    marked.add((EntityLiving) npc);
                    alreadyInWorld.add(rig.world.loadedEntityList.contains(npc));
                    boolean w = HoleSpawning.MARKER.mark(npc); // the production marker, unchanged
                    written.add(w);
                    return w;
                };
                List<EntityLiving> placed = tile.attempt(recording, rig.placer);
                out.check(!placed.isEmpty(), "the hole should place NPCs");
                out.checkEquals("NPCs handed to the marker", placed.size(), marked.size());
                for (int i = 0; i < marked.size(); i++) {
                    out.check(
                        marked.get(i) == placed.get(i),
                        "marker " + i + " saw a different entity than the one placed");
                    out.check(!alreadyInWorld.get(i), "NPC " + i + " was already in the world when marked");
                    out.checkEquals("marker " + i + " wrote", InfernalHooks.available(), written.get(i));
                }
                if (InfernalHooks.available()) {
                    for (EntityLiving e : placed) {
                        boolean has = false;
                        for (Object key : e.getEntityData()
                            .func_150296_c()) {
                            has |= InfernalOptOut.MARKER.equals(
                                e.getEntityData()
                                    .getString((String) key));
                        }
                        out.check(has, "a spawner NPC lacks the Infernal opt-out marker");
                    }
                    out.note("Infernal Mobs present: marker found in each NPC's persistent data");
                } else {
                    out.note(
                        "Infernal Mobs absent: hook called once per NPC before spawn; it writes nothing without the mod");
                }
            } finally {
                NpcConfig.infernalAllowSpawnerNpcs = allow;
                rig.finish();
            }
            return out.build();
        }
    }

    /** The placed Big Bunker carries two Zombie Holes, each with a Zombie tile, and no other spawner block. */
    private static final class BigBunker implements Scenario {

        @Override
        public String name() {
            return "worldgen.big_bunker_spawners";
        }

        @Override
        public String description() {
            return "Forces the Big Bunker through the populator with the real spawner hook: its two spawner.zombie_hole"
                + " marks become two Zombie Hole blocks with Zombie tiles, no Bug Hole appears, and cleanup removes them.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            BlockHoleSpawner zombie = HoleSpawnerRegistration.block(HoleKind.ZOMBIE);
            BlockHoleSpawner bug = HoleSpawnerRegistration.block(HoleKind.BUG);
            out.check(zombie != null && bug != null, "the hole blocks should be registered");
            if (zombie == null || bug == null) {
                return out.build();
            }
            StructurePlan plan = PlacementHarness.plan(world, StructureId.BIG_BUNKER, 1);
            Placement p = PlacementHarness.place(world, plan, false, false, new HoleSpawnerHook());
            try {
                out.check(p.decision.isPlaced(), "the Big Bunker should be placed, got " + p.decision.reason());
                if (!p.decision.isPlaced()) {
                    return out.build();
                }
                out.checkEquals("spawner marks reported", 2, p.spawnerPos.size());
                List<String> seen = new ArrayList<>();
                int zombies = 0;
                int bugs = 0;
                int badTiles = 0;
                for (int x = plan.anchorX(); x <= plan.maxX(); x++) {
                    for (int z = plan.anchorZ(); z <= plan.maxZ(); z++) {
                        for (int y = p.minY(); y <= p.maxY(); y++) {
                            if (world.getBlock(x, y, z) == zombie) {
                                zombies++;
                                seen.add(x + "," + y + "," + z);
                                TileEntity t = world.getTileEntity(x, y, z);
                                if (!(t instanceof TileHoleSpawner)
                                    || ((TileHoleSpawner) t).kind() != HoleKind.ZOMBIE) {
                                    badTiles++;
                                }
                            } else if (world.getBlock(x, y, z) == bug) {
                                bugs++;
                            }
                        }
                    }
                }
                out.checkEquals("Zombie Hole blocks in the bunker", 2, zombies);
                out.checkEquals("Bug Hole blocks in the bunker", 0, bugs);
                out.checkEquals("hole blocks without a Zombie tile", 0, badTiles);
                for (int[] s : p.spawnerPos) {
                    out.check(
                        seen.contains(s[0] + "," + s[1] + "," + s[2]),
                        "no Zombie Hole at the spawner mark " + s[0] + "," + s[1] + "," + s[2]);
                }
                out.checkEquals("writes dropped outside their window", 0, p.rejectedWrites);
            } finally {
                p.cleanup();
            }
            int left = 0;
            for (int x = plan.anchorX(); x <= plan.maxX(); x++) {
                for (int z = plan.anchorZ(); z <= plan.maxZ(); z++) {
                    for (int y = p.minY(); y <= p.maxY(); y++) {
                        if (world.getBlock(x, y, z) == zombie) {
                            left++;
                        }
                    }
                }
            }
            out.checkEquals("cleanup: Zombie Hole blocks left", 0, left);
            return out.build();
        }
    }
}
