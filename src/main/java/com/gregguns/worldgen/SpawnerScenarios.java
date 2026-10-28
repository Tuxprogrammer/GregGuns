package com.gregguns.worldgen;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
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
import com.gregguns.worldgen.spawner.SpawnerKind;
import com.gregguns.worldgen.spawner.SpawnerNbt;
import com.gregguns.worldgen.spawner.SpawnerSettings;
import com.gregguns.worldgen.spawner.VanillaSpawners;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The spawner scenarios (M8 G11): GregGuns spawners are vanilla mob spawners with GregGuns NBT. They check the
 * caps, the player gate, the danger floor, the light rule, the Infernal marker, the client preview path and the Big
 * Bunker's two spawners. The vanilla spawner logic is driven by calling the tile's own update, so every check runs the
 * real code.
 * Every NPC is killed at the end with its drops cancelled. Dev environment only; needs {@code worldgen} (and so
 * {@code npcs}) in {@code GG_MODULES}. Server side.
 */
// Spec: PLAN-M8 section 2 (Big Bunker spawners) and G11, contract addendum (spawner marker),
// docs/gregguns-spec/10-entities-npcs/spawning.md (spawner NPCs)
public final class SpawnerScenarios {

    /** Update calls that cover the first cycle (40 ticks) and at least nine more (at most 120 ticks each). */
    private static final int RUN_TICKS = 1200;

    private SpawnerScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Caps());
        registry.register(new Idle());
        registry.register(new LightRule());
        registry.register(new InfernalMarker());
        registry.register(new Preview());
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

    /** One scenario's cast: the spawner, its player, difficulty and the cleanup of all of it. */
    private static final class Rig {

        final ScenarioContext ctx;
        final World world;
        final DropCanceller drops = new DropCanceller();
        final EnumDifficulty priorDifficulty;
        FakePlayer player;

        Rig(ScenarioContext ctx) {
            this.ctx = ctx;
            this.world = ctx.world();
            this.priorDifficulty = world.difficultySetting;
            if (world.difficultySetting == EnumDifficulty.PEACEFUL) {
                world.difficultySetting = EnumDifficulty.NORMAL; // monsters never spawn in peaceful
            }
            MinecraftForge.EVENT_BUS.register(drops);
            ctx.clearArea(12, 10);
        }

        /** Puts a GregGuns spawner of the kind at the origin offset; cleanup restores the block. */
        TileEntityMobSpawner spawner(SpawnerKind kind, int dx, int ceiling) {
            ctx.setBlock(dx, 0, 0, Blocks.mob_spawner, 0);
            VanillaSpawners.placeInWorld(world, kind, ctx.originX() + dx, ctx.originY(), ctx.originZ(), ceiling);
            TileEntity t = world.getTileEntity(ctx.originX() + dx, ctx.originY(), ctx.originZ());
            return t instanceof TileEntityMobSpawner ? (TileEntityMobSpawner) t : null;
        }

        /** A sturdy fake player {@code dx} blocks east of the origin. */
        void playerAt(double dx) {
            if (player == null) {
                String name = "[GGSpawner]";
                player = new FakePlayer(
                    (WorldServer) world,
                    new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
                new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), player);
                player.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                    .setBaseValue(1000.0);
                player.setHealth(1000.0F);
                player.capabilities.disableDamage = true;
                player.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
                ctx.spawn(player);
            } else {
                player.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            }
        }

        /** Living mobs of the kind in the box vanilla counts around the spawner at origin offset {@code dx}. */
        List<EntityLiving> near(SpawnerKind kind, int dx) {
            int x = ctx.originX() + dx, y = ctx.originY(), z = ctx.originZ();
            int r = kind.settings()
                .countRadius();
            List<String> names = kind.mobNames();
            List<EntityLiving> out = new ArrayList<>();
            for (Object o : world.getEntitiesWithinAABB(
                EntityLiving.class,
                AxisAlignedBB.getBoundingBox(x - r, y - 4, z - r, x + 1 + r, y + 5, z + 1 + r))) {
                EntityLiving e = (EntityLiving) o;
                if (!e.isDead && names.contains(EntityList.getEntityString(e))) out.add(e);
            }
            return out;
        }

        /** Kills every NPC within reach of the origin, drops cancelled, then undoes the scenario. */
        void finish() {
            try {
                AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                    ctx.originX() - 40,
                    0,
                    ctx.originZ() - 40,
                    ctx.originX() + 40,
                    255,
                    ctx.originZ() + 40);
                for (Object o : world.getEntitiesWithinAABB(EntityLiving.class, box)) {
                    EntityLiving e = (EntityLiving) o;
                    if (EntityList.getEntityString(e) == null || !EntityList.getEntityString(e)
                        .startsWith("gregguns.")) {
                        continue;
                    }
                    drops.track(e);
                    e.setHealth(0.0F);
                    e.onDeath(DamageSource.generic);
                    e.setDead();
                }
            } finally {
                MinecraftForge.EVENT_BUS.unregister(drops);
                world.difficultySetting = priorDifficulty;
                ctx.cleanup();
            }
        }
    }

    /** Runs the spawner's own update {@code ticks} times; returns the most mobs one call added. */
    private static int drive(Rig rig, TileEntityMobSpawner tile, SpawnerKind kind, int ticks) {
        int worst = 0;
        for (int i = 0; i < ticks; i++) {
            int before = rig.near(kind, 0)
                .size();
            tile.updateEntity();
            worst = Math.max(
                worst,
                rig.near(kind, 0)
                    .size() - before);
        }
        return worst;
    }

    /** With a player near, a zombie spawner fills to 5 of each type and never more, at most 3 a cycle. */
    private static final class Caps implements Scenario {

        @Override
        public String name() {
            return "worldgen.spawner_caps";
        }

        @Override
        public String description() {
            return "A Big Bunker spawner (vanilla mob spawner, UHV ceiling) with a player 6 blocks away, run for 1200 of"
                + " its own updates: it spawns only zombie NPCs, at most 3 a cycle and 5 of each type in vanilla's count"
                + " box, every NPC starts at danger 5 (the floor) with its equipment rolled, and the tile NBT carries"
                + " the GregGuns settings (PLAN-M8 G11).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Rig rig = new Rig(ctx);
            try {
                TileEntityMobSpawner tile = rig.spawner(SpawnerKind.ZOMBIE, 0, RolledCeiling.MAX);
                out.check(tile != null, "the spawner should carry a mob spawner tile");
                if (tile == null) return out.build();
                NBTTagCompound nbt = VanillaSpawners.read(tile);
                SpawnerSettings s = SpawnerKind.ZOMBIE.settings();
                out.check(SpawnerNbt.isMarked(nbt), "the tile NBT carries GregGuns spawn data");
                out.checkEquals("potentials", SpawnerKind.ZOMBIE.mobNames(), SpawnerNbt.entities(nbt));
                out.checkEquals("player range", s.playerRange(), (int) nbt.getShort("RequiredPlayerRange"));
                out.checkEquals("max nearby", s.maxNearby(), (int) nbt.getShort("MaxNearbyEntities"));
                out.checkEquals("spawn count", s.spawnCount(), (int) nbt.getShort("SpawnCount"));
                out.checkEquals("danger floor", 5, SpawnerNbt.minDanger(nbt));
                rig.playerAt(6.0);
                int worst = drive(rig, tile, SpawnerKind.ZOMBIE, RUN_TICKS);
                List<EntityLiving> all = rig.near(SpawnerKind.ZOMBIE, 0);
                out.check(!all.isEmpty(), "the spawner should spawn zombies with a player 6 blocks away");
                out.check(worst <= s.spawnCount(), "one update spawned " + worst + ", more than 3");
                Map<String, Integer> perType = new HashMap<>();
                int unset = 0;
                for (EntityLiving e : all) {
                    perType.merge(EntityList.getEntityString(e), 1, Integer::sum);
                    if (!(e instanceof NpcEntity) || ((NpcEntity) e).danger() != 5) unset++;
                }
                out.checkEquals("NPCs not at the danger floor 5", 0, unset);
                for (Map.Entry<String, Integer> c : perType.entrySet()) {
                    out.check(c.getValue() <= s.maxNearby(), c.getValue() + " " + c.getKey() + ", more than 5");
                }
                // Ten even picks over three types all landing on one has odds of about 1 in 6,500.
                out.check(perType.size() >= 2, "the potentials should give more than one zombie type: " + perType);
                out.note("spawned " + perType + ", worst cycle " + worst);
            } finally {
                rig.finish();
            }
            return out.build();
        }
    }

    /** With no player within 24 blocks a spawner does nothing; a player at 23 wakes it. */
    private static final class Idle implements Scenario {

        @Override
        public String name() {
            return "worldgen.spawner_idle";
        }

        @Override
        public String description() {
            return "A Big Bunker spawner with its only player 25 blocks away spawns nothing over 300 of its own updates;"
                + " with the player 23 blocks away it spawns within the next 300 (player range 24).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Rig rig = new Rig(ctx);
            try {
                TileEntityMobSpawner tile = rig.spawner(SpawnerKind.ZOMBIE, 0, 0);
                if (tile == null) {
                    out.check(false, "the spawner should carry a mob spawner tile");
                    return out.build();
                }
                rig.playerAt(25.0);
                drive(rig, tile, SpawnerKind.ZOMBIE, 300);
                out.checkEquals(
                    "zombies after 300 updates with the player at 25",
                    0,
                    rig.near(SpawnerKind.ZOMBIE, 0)
                        .size());
                rig.playerAt(23.0);
                drive(rig, tile, SpawnerKind.ZOMBIE, 300);
                out.check(
                    !rig.near(SpawnerKind.ZOMBIE, 0)
                        .isEmpty(),
                    "the spawner should wake with the player at 23 blocks");
            } finally {
                rig.finish();
            }
            return out.build();
        }
    }

    /**
     * Light: in a brightly lit spot the vanilla monster checks refuse an NPC, and still refuse it as a natural-spawn
     * candidate or beside a plain vanilla spawner; only the GregGuns spawner that is placing it lifts them.
     */
    private static final class LightRule implements Scenario {

        @Override
        public String name() {
            return "worldgen.spawner_light";
        }

        @Override
        public String description() {
            return "On a glowstone floor (block light 14+): a GregGuns zombie spawner spawns; a Zombie Soldier beside it"
                + " passes getCanSpawnHere only while that spawner's current entry is its own type; the same NPC after"
                + " the CheckSpawn event (a natural candidate), one away from any spawner, and one beside a plain"
                + " vanilla spawner of its type all fail it: natural spawn rules are unchanged.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Rig rig = new Rig(ctx);
            try {
                for (int dx = -9; dx <= 9; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        ctx.setBlock(dx, -1, dz, Blocks.glowstone, 0);
                    }
                }
                TileEntityMobSpawner tile = rig.spawner(SpawnerKind.ZOMBIE, -6, 0);
                // A plain vanilla spawner, no GregGuns data, of the Zombie Soldier.
                ctx.setBlock(6, 0, 0, Blocks.mob_spawner, 0);
                TileEntity plainTile = rig.world.getTileEntity(ctx.originX() + 6, ctx.originY(), ctx.originZ());
                if (tile == null || !(plainTile instanceof TileEntityMobSpawner)) {
                    out.check(false, "both spawners should carry mob spawner tiles");
                    return out.build();
                }
                ((TileEntityMobSpawner) plainTile).func_145881_a()
                    .setEntityName(NpcMob.ZOMBIE_SOLDIER.fullName());
                // Spots half a block into their cell, clear of the spawner blocks' own collision boxes.
                int lx = ctx.originX() - 5, ly = ctx.originY(), lz = ctx.originZ() + 1;
                out.check(
                    rig.world.getBlockLightValue(lx, ly, lz) >= 14,
                    "the spot is lit: " + rig.world.getBlockLightValue(lx, ly, lz));
                String own = tile.func_145881_a()
                    .getEntityNameToSpawn();
                out.check(
                    SpawnerKind.ZOMBIE.mobNames()
                        .contains(own),
                    "the spawner's current entry is a zombie: " + own);

                out.check(
                    candidate(rig, own, lx, ly, lz).getCanSpawnHere(),
                    "beside its GregGuns spawner it may spawn");
                EntityLiving natural = candidate(rig, own, lx, ly, lz);
                ForgeEventFactory.canEntitySpawn(natural, rig.world, lx + 0.5F, ly, lz + 0.5F); // fires CheckSpawn
                out.check(!natural.getCanSpawnHere(), "a natural candidate keeps the light rule beside the spawner");
                out.check(
                    !candidate(rig, own, ctx.originX(), ly, lz).getCanSpawnHere(),
                    "away from any spawner the light rule holds");
                out.check(
                    !candidate(rig, NpcMob.ZOMBIE_SOLDIER.fullName(), ctx.originX() + 5, ly, lz).getCanSpawnHere(),
                    "beside a plain vanilla spawner the light rule holds");
                String other = null;
                for (String n : SpawnerKind.ZOMBIE.mobNames()) {
                    if (!n.equals(own)) other = n;
                }
                out.check(
                    !candidate(rig, other, lx, ly, lz).getCanSpawnHere(),
                    "a zombie the spawner is not spawning this cycle keeps the light rule");

                rig.playerAt(-2.0);
                drive(rig, tile, SpawnerKind.ZOMBIE, 300);
                out.check(
                    !rig.near(SpawnerKind.ZOMBIE, -6)
                        .isEmpty(),
                    "the GregGuns spawner spawns on the lit floor");
            } finally {
                rig.finish();
            }
            return out.build();
        }

        /** A fresh NPC standing in the middle of block (x, y, z), never added to the world. */
        private static EntityLiving candidate(Rig rig, String name, int x, int y, int z) {
            EntityLiving e = (EntityLiving) EntityList.createEntityByName(name, rig.world);
            e.setLocationAndAngles(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
            return e;
        }
    }

    /** Each NPC a spawner places carries the Infernal marker and its danger before it joins the world. */
    private static final class InfernalMarker implements Scenario {

        @Override
        public String name() {
            return "worldgen.spawner_infernal_marker";
        }

        @Override
        public String description() {
            return "Every NPC a GregGuns spawner places already has its danger set when it joins the world, and with"
                + " Infernal Mobs loaded it already carries the opt-out marker then, so Infernal never rolls it.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Rig rig = new Rig(ctx);
            boolean allow = NpcConfig.infernalAllowSpawnerNpcs;
            NpcConfig.infernalAllowSpawnerNpcs = false;
            JoinWatcher watcher = new JoinWatcher();
            MinecraftForge.EVENT_BUS.register(watcher);
            try {
                TileEntityMobSpawner tile = rig.spawner(SpawnerKind.ZOMBIE, 0, 0);
                if (tile == null) {
                    out.check(false, "the spawner should carry a mob spawner tile");
                    return out.build();
                }
                rig.playerAt(6.0);
                drive(rig, tile, SpawnerKind.ZOMBIE, 200);
                out.check(watcher.joined > 0, "the spawner should place NPCs");
                out.checkEquals("NPCs that joined without a danger", 0, watcher.noDanger);
                if (InfernalHooks.available()) {
                    out.checkEquals("NPCs that joined without the Infernal marker", 0, watcher.unmarked);
                    out.note("Infernal Mobs present: the marker was in each NPC's persistent data at join");
                } else {
                    out.note("Infernal Mobs absent: the hook writes nothing; danger checked at join");
                }
            } finally {
                MinecraftForge.EVENT_BUS.unregister(watcher);
                NpcConfig.infernalAllowSpawnerNpcs = allow;
                rig.finish();
            }
            return out.build();
        }

        /** Records what each GregGuns NPC carries at the moment it joins the world. */
        public static final class JoinWatcher {

            int joined;
            int noDanger;
            int unmarked;

            @SubscribeEvent
            public void onJoin(EntityJoinWorldEvent event) {
                if (event.world.isRemote || !(event.entity instanceof NpcEntity)) return;
                joined++;
                if (((NpcEntity) event.entity).danger() == NpcEntity.DANGER_UNSET) noDanger++;
                boolean has = false;
                NBTTagCompound data = event.entity.getEntityData();
                for (Object key : data.func_150296_c()) {
                    has |= InfernalOptOut.MARKER.equals(data.getString((String) key));
                }
                if (!has) unmarked++;
            }
        }
    }

    /**
     * The client draws a spinning mob inside every spawner by creating the entity with no world and reading the spawn
     * data into it. Running that same path here proves every GregGuns spawner mob survives it.
     */
    private static final class Preview implements Scenario {

        @Override
        public String name() {
            return "worldgen.spawner_preview";
        }

        @Override
        public String description() {
            return "For every spawner kind and mob: the spawner NBT builds, the entity is created with a null world, and"
                + " vanilla's func_98265_a (the client preview path) reads the GregGuns spawn data into it without"
                + " throwing and without rolling equipment.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (SpawnerKind kind : SpawnerKind.values()) {
                NBTTagCompound nbt = VanillaSpawners.nbt(kind, 0, 64, 0, 0);
                out.check(nbt != null, kind + ": no spawner NBT (no registered mob)");
                if (nbt == null) continue;
                for (String name : SpawnerNbt.entities(nbt)) {
                    TileEntityMobSpawner tile = new TileEntityMobSpawner();
                    NBTTagCompound one = (NBTTagCompound) nbt.copy();
                    one.setString("EntityId", name);
                    tile.readFromNBT(one);
                    Entity e = EntityList.createEntityByName(name, null);
                    out.check(e != null, kind + ": " + name + " cannot be created with no world");
                    if (e == null) continue;
                    try {
                        Entity shown = tile.func_145881_a()
                            .func_98265_a(e);
                        out.check(shown == e, kind + ": " + name + " preview returned another entity");
                        if (e instanceof EntityLiving) {
                            out.check(
                                ((EntityLiving) e).getHeldItem() == null,
                                kind + ": " + name + " rolled equipment with no world");
                        }
                    } catch (RuntimeException ex) {
                        out.check(false, kind + ": " + name + " preview threw " + ex);
                    }
                }
            }
            return out.build();
        }
    }

    /** The placed Big Bunker carries two GregGuns zombie spawners at its two marks, and no other spawner. */
    private static final class BigBunker implements Scenario {

        @Override
        public String name() {
            return "worldgen.big_bunker_spawners";
        }

        @Override
        public String description() {
            return "Forces the Big Bunker through the populator with the real spawner hook: its two spawner.zombie_hole"
                + " marks become two vanilla mob spawners of the zombie NPCs with GregGuns spawn data, no other spawner"
                + " appears, and cleanup removes them.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructurePlan plan = PlacementHarness.plan(world, StructureId.BIG_BUNKER, 1);
            Placement p = PlacementHarness.place(world, plan, false, false, new VanillaSpawners());
            try {
                out.check(p.decision.isPlaced(), "the Big Bunker should be placed, got " + p.decision.reason());
                if (!p.decision.isPlaced()) {
                    return out.build();
                }
                out.checkEquals("spawner marks reported", 2, p.spawnerPos.size());
                List<String> seen = new ArrayList<>();
                int spawners = 0;
                int badTiles = 0;
                for (int x = plan.anchorX(); x <= plan.maxX(); x++) {
                    for (int z = plan.anchorZ(); z <= plan.maxZ(); z++) {
                        for (int y = p.minY(); y <= p.maxY(); y++) {
                            if (world.getBlock(x, y, z) == Blocks.mob_spawner) {
                                spawners++;
                                seen.add(x + "," + y + "," + z);
                                NBTTagCompound t = VanillaSpawners.read(world.getTileEntity(x, y, z));
                                if (t == null || !SpawnerNbt.isMarked(t)
                                    || !SpawnerNbt.entities(t)
                                        .equals(SpawnerKind.ZOMBIE.mobNames())) {
                                    badTiles++;
                                }
                            }
                        }
                    }
                }
                out.checkEquals("mob spawners in the bunker", 2, spawners);
                out.checkEquals("spawners without marked zombie NBT", 0, badTiles);
                for (int[] s : p.spawnerPos) {
                    out.check(
                        seen.contains(s[0] + "," + s[1] + "," + s[2]),
                        "no spawner at the mark " + s[0] + "," + s[1] + "," + s[2]);
                }
                out.checkEquals("writes dropped outside their window", 0, p.rejectedWrites);
            } finally {
                p.cleanup();
            }
            int left = 0;
            for (int x = plan.anchorX(); x <= plan.maxX(); x++) {
                for (int z = plan.anchorZ(); z <= plan.maxZ(); z++) {
                    for (int y = p.minY(); y <= p.maxY(); y++) {
                        if (world.getBlock(x, y, z) == Blocks.mob_spawner) {
                            left++;
                        }
                    }
                }
            }
            out.checkEquals("cleanup: mob spawners left", 0, left);
            return out.build();
        }
    }
}
