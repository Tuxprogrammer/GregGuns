package com.gregguns.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.gregguns.core.config.UtilityConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.utility.blob.BlobState;
import com.gregguns.utility.blob.BlockBioBlob;
import com.gregguns.utility.blob.TileBioBlob;
import com.gregguns.utility.camp.CampSpawnPort;
import com.gregguns.utility.camp.CampSpawnPorts;
import com.gregguns.utility.camp.TileCampFlag;
import com.gregguns.utility.storage.ContainerGregChest;
import com.gregguns.utility.storage.TileGregChest;

/**
 * Integration scenarios for the chests, the Bio Blob and the Camp Flag. Dev environment only. Server side.
 */
// Spec: docs/techguns-spec/09-machines-utility/misc-tile-entities.md; docs/ROADMAP.md (M6 exit gate: a scenario per
// block)
public final class WorldBlockScenarios {

    private WorldBlockScenarios() {}

    /**
     * @param registry where to add the scenarios
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new ChestRoundtrip());
        registry.register(new BlobLifecycle());
        registry.register(new BlobPoison());
        registry.register(new CampFlagSpawns());
        registry.register(new CampFlagBreakDrop());
    }

    private static TileEntity tileAt(ScenarioContext ctx, int dx, int dy, int dz) {
        return ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
    }

    private static EntityPlayerMP fakePlayer(ScenarioContext ctx) {
        return FakePlayerFactory.getMinecraft((WorldServer) ctx.world());
    }

    /** Insert and extract on both chests; the Weapon Chest filter; the container and its shift-click. */
    private static final class ChestRoundtrip implements Scenario {

        @Override
        public String name() {
            return "utility.chest_roundtrip";
        }

        @Override
        public String description() {
            return "Both chests store and return items; the Weapon Chest takes only GregGuns items; shift-click, the viewer count and redstone behave.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.setBlock(0, 0, 0, WorldBlockRegistration.reinforcedChest(), 3);
            ctx.setBlock(3, 0, 0, WorldBlockRegistration.weaponChest(), 3);
            TileEntity a = tileAt(ctx, 0, 0, 0);
            TileEntity b = tileAt(ctx, 3, 0, 0);
            out.check(a instanceof TileGregChest, "the Reinforced Chest has its tile entity");
            out.check(b instanceof TileGregChest, "the Weapon Chest has its tile entity");
            if (!(a instanceof TileGregChest) || !(b instanceof TileGregChest)) {
                return out.build();
            }
            TileGregChest reinforced = (TileGregChest) a;
            TileGregChest weapon = (TileGregChest) b;
            Item plate = UtilityParts.plate(TurretPlate.IRON);

            // Reinforced: anything goes in and comes out.
            out.checkEquals("slot count", 36, reinforced.getSizeInventory());
            out.check(reinforced.insert(new ItemStack(Blocks.dirt, 10)), "the Reinforced Chest takes dirt");
            out.check(reinforced.insert(new ItemStack(plate, 2)), "the Reinforced Chest takes a GregGuns item");
            out.check(reinforced.insert(new ItemStack(Blocks.dirt, 70)), "the dirt stacks and overflows to a new slot");
            out.checkEquals("first dirt slot", 64, reinforced.getStackInSlot(0).stackSize);
            ItemStack taken = reinforced.decrStackSize(0, 24);
            out.checkEquals("taken from the first slot", 24, taken == null ? 0 : taken.stackSize);
            out.checkEquals("left in the first slot", 40, reinforced.getStackInSlot(0).stackSize);

            // Weapon: GregGuns non-block items only.
            out.check(!weapon.isItemValidForSlot(0, new ItemStack(Blocks.dirt)), "the Weapon Chest refuses dirt");
            out.check(!weapon.insert(new ItemStack(Blocks.dirt, 5)), "insert of dirt into the Weapon Chest fails");
            out.check(!weapon.insert(new ItemStack(Items.iron_ingot)), "a vanilla item is refused");
            out.check(
                !weapon.insert(new ItemStack(Item.getItemFromBlock(WorldBlockRegistration.weaponChest()))),
                "a GregGuns item block is refused");
            out.check(weapon.insert(new ItemStack(plate, 3)), "the Weapon Chest takes a GregGuns item");
            out.checkEquals(
                "weapon slot 0",
                3,
                weapon.getStackInSlot(0) == null ? 0 : weapon.getStackInSlot(0).stackSize);

            // NBT round trip.
            NBTTagCompound tag = new NBTTagCompound();
            reinforced.writeToNBT(tag);
            TileGregChest copy = new TileGregChest();
            copy.readFromNBT(tag);
            out.checkEquals(
                "round trip slot 0",
                40,
                copy.getStackInSlot(0) == null ? 0 : copy.getStackInSlot(0).stackSize);
            out.checkEquals(
                "round trip slot 1 item",
                plate,
                copy.getStackInSlot(1) == null ? null
                    : copy.getStackInSlot(1)
                        .getItem());

            // Container, shift-click, viewers (MUTL-34, MUTL-36).
            EntityPlayerMP player = fakePlayer(ctx);
            player.inventory.clearInventory(null, -1);
            Object gui = UtilityGuis
                .serverElement(UtilityGuis.CHEST, player, ctx.world(), ctx.originX() + 3, ctx.originY(), ctx.originZ());
            out.check(gui instanceof ContainerGregChest, "the GUI dispatcher gives a chest container");
            if (gui instanceof ContainerGregChest) {
                ContainerGregChest container = (ContainerGregChest) gui;
                out.checkEquals("viewers while open", 1, weapon.viewers());
                player.inventory.setInventorySlotContents(0, new ItemStack(Blocks.dirt, 8));
                // Player hotbar slot 0 is container slot 36 + 27.
                int hotbar0 = TileGregChest.SLOTS + 27;
                out.check(
                    container.transferStackInSlot(player, hotbar0) == null,
                    "shift-click of dirt into the Weapon Chest moves nothing");
                out.checkEquals("dirt stays in the hotbar", 8, player.inventory.getStackInSlot(0).stackSize);
                player.inventory.setInventorySlotContents(0, new ItemStack(plate, 4));
                container.transferStackInSlot(player, hotbar0);
                out.check(player.inventory.getStackInSlot(0) == null, "the plate moved out of the hotbar");
                out.checkEquals("weapon slot 0 after shift-click", 7, weapon.getStackInSlot(0).stackSize);
                // Back out again: chest slot to the player inventory, not to special slots.
                container.transferStackInSlot(player, 0);
                out.check(weapon.getStackInSlot(0) == null, "shift-click out of the chest empties the slot");
                container.onContainerClosed(player);
                out.checkEquals("viewers after close", 0, weapon.viewers());
            }

            // Redstone: neither chest gives power (MUTL-35), even with a viewer.
            Block w = WorldBlockRegistration.weaponChest();
            out.check(!w.canProvidePower(), "the Weapon Chest does not give redstone power");
            out.checkEquals(
                "weak power",
                0,
                w.isProvidingWeakPower(ctx.world(), ctx.originX() + 3, ctx.originY(), ctx.originZ(), 1));
            out.check(
                !WorldBlockRegistration.reinforcedChest()
                    .canProvidePower(),
                "the Reinforced Chest does not give redstone power");
            return out.build();
        }
    }

    /** Growth to 3, the burst on 4 with no terrain damage, decay one step per interval. */
    private static final class BlobLifecycle implements Scenario {

        @Override
        public String name() {
            return "utility.bioblob_lifecycle";
        }

        @Override
        public String description() {
            return "A Bio Blob grows to size 3, bursts on a fourth hit without damaging terrain, and decays one step per decay interval.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            int ox = ctx.originX();
            int oy = ctx.originY();
            int oz = ctx.originZ();
            int interval = UtilityConfig.bioBlobDecayTicks;

            // Burst first: a size-3 blob hit once vanishes and the stone next to it stays.
            ctx.setBlock(4, 0, 0, WorldBlockRegistration.bioBlob(), 1);
            ctx.setBlock(5, 0, 0, Blocks.stone, 0);
            TileEntity burst = world.getTileEntity(ox + 4, oy, oz);
            out.check(burst instanceof TileBioBlob, "a placed blob has its tile entity");
            if (burst instanceof TileBioBlob) {
                ((TileBioBlob) burst).setSize(3);
                ((TileBioBlob) burst).hitBlob(1, null);
            }
            out.check(world.isAirBlock(ox + 4, oy, oz), "a fourth hit bursts the blob");
            out.check(world.getBlock(ox + 5, oy, oz) == Blocks.stone, "the burst does not damage terrain");

            // Growth through the projectile entry point: floor hit places size 1, two more hits make 3.
            boolean placed = BlockBioBlob.onProjectileImpact(world, ox, oy - 1, oz, 1, 1, null);
            out.check(placed, "a projectile on the floor places a blob");
            TileEntity tile = world.getTileEntity(ox, oy, oz);
            out.check(tile instanceof TileBioBlob, "the placed blob has its tile entity");
            if (!(tile instanceof TileBioBlob)) {
                return out.build();
            }
            TileBioBlob blob = (TileBioBlob) tile;
            out.checkEquals("size after placement", 1, blob.size());
            out.checkEquals("meta is the hit face", 1, world.getBlockMetadata(ox, oy, oz));
            BlockBioBlob.onProjectileImpact(world, ox, oy, oz, 1, 2, null);
            out.checkEquals("size after a power-2 hit", 3, blob.size());

            // Decay: record the poll at which each step happens.
            final int[] at = { -1, -1, -1 };
            final ScenarioResult early = out.build();
            if (!early.passed()) {
                return early;
            }
            return ScenarioResult.pending(name(), interval * 3 + 80, ticks -> {
                if (at[0] < 0 && blob.size() == 2) {
                    at[0] = ticks;
                }
                if (at[1] < 0 && blob.size() == 1) {
                    at[1] = ticks;
                }
                if (at[2] < 0 && world.isAirBlock(ox, oy, oz)) {
                    at[2] = ticks;
                }
                if (at[2] < 0) {
                    return null;
                }
                ScenarioResult.Builder res = ScenarioResult.builder(name());
                for (String line : early.messages()) {
                    res.note(line);
                }
                res.check(Math.abs(at[0] - interval) <= 2, "3 to 2 after one interval, at poll " + at[0]);
                res.check(Math.abs(at[1] - at[0] - interval) <= 2, "2 to 1 one interval later, at poll " + at[1]);
                res.check(Math.abs(at[2] - at[1] - interval) <= 2, "1 to gone one interval later, at poll " + at[2]);
                return res.build();
            });
        }
    }

    /** Poison on a creeper and a player; the config spares players; the collision box follows the size. */
    private static final class BlobPoison implements Scenario {

        @Override
        public String name() {
            return "utility.bioblob_poison";
        }

        @Override
        public String description() {
            return "A Bio Blob poisons a creeper and a player (Poison III), spares players when configured, and collides with its per-size box.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            int ox = ctx.originX();
            int oy = ctx.originY();
            int oz = ctx.originZ();
            ctx.setBlock(0, 0, 0, WorldBlockRegistration.bioBlob(), 1);
            TileEntity tile = world.getTileEntity(ox, oy, oz);
            if (tile instanceof TileBioBlob) {
                ((TileBioBlob) tile).setSize(3);
            }
            Block blob = world.getBlock(ox, oy, oz);
            AxisAlignedBB box = blob.getCollisionBoundingBoxFromPool(world, ox, oy, oz);
            out.check(box != null, "the blob has a collision box");
            if (box != null) {
                out.check(
                    Math.abs(box.maxY - (oy + BlobState.thickness(3))) < 1e-6,
                    "collision height follows size 3, got " + (box.maxY - oy));
                out.check(Math.abs(box.maxX - box.minX - BlobState.width(3)) < 1e-6, "collision width follows size 3");
            }

            EntityCreeper creeper = ctx.spawnAt(new EntityCreeper(world), ox + 0.5, oy, oz + 0.5, 0.0F, 0.0F);
            blob.onEntityCollidedWithBlock(world, ox, oy, oz, creeper);
            PotionEffect creeperEffect = creeper.getActivePotionEffect(Potion.poison);
            out.check(creeperEffect != null, "the creeper is poisoned");
            if (creeperEffect != null) {
                out.checkEquals("creeper poison amplifier", 2, creeperEffect.getAmplifier());
            }

            // A plain player entity: a FakePlayer has no network handler and fails on potion packets.
            EntityPlayer player = new EntityPlayer(
                world,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "blobtest")) {

                @Override
                public void addChatMessage(net.minecraft.util.IChatComponent message) {}

                @Override
                public boolean canCommandSenderUseCommand(int level, String command) {
                    return false;
                }

                @Override
                public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() {
                    return new net.minecraft.util.ChunkCoordinates(0, 0, 0);
                }
            };
            boolean spared = UtilityConfig.bioBlobSparesPlayers;
            try {
                UtilityConfig.bioBlobSparesPlayers = false;
                blob.onEntityCollidedWithBlock(world, ox, oy, oz, player);
                out.check(player.getActivePotionEffect(Potion.poison) != null, "the player is poisoned by default");
                player.getActivePotionEffects()
                    .clear();
                UtilityConfig.bioBlobSparesPlayers = true;
                blob.onEntityCollidedWithBlock(world, ox, oy, oz, player);
                out.check(player.getActivePotionEffect(Potion.poison) == null, "the config spares players");
                EntityCreeper second = ctx.spawn(new EntityCreeper(world));
                blob.onEntityCollidedWithBlock(world, ox, oy, oz, second);
                out.check(
                    second.getActivePotionEffect(Potion.poison) != null,
                    "mobs are poisoned even when players are spared");
            } finally {
                UtilityConfig.bioBlobSparesPlayers = spared;
                player.getActivePotionEffects()
                    .clear();
            }
            world.setBlockToAir(ox, oy, oz);
            return out.build();
        }
    }

    /** A counting stand-in for the NPC module's port. */
    private static final class CountingPort implements CampSpawnPort {

        final List<int[]> spawns = new ArrayList<>();
        final List<Integer> spawnTicks = new ArrayList<>();
        /** Spawns by flags outside the scenario's own square (other scenarios', worldgen camps'), passed on. */
        final List<int[]> foreign = new ArrayList<>();
        int broken;
        int tick;
        /** Where other flags' calls go, or null to count every call. */
        CampSpawnPort passTo;
        int ownX;
        int ownZ;
        int ownRadius;

        CountingPort() {}

        /** Counts only spawns inside {@code radius} blocks of (x, z); every other flag's calls go to passTo. */
        CountingPort(CampSpawnPort passTo, int x, int z, int radius) {
            this.passTo = passTo;
            this.ownX = x;
            this.ownZ = z;
            this.ownRadius = radius;
        }

        private boolean own(double x, double z) {
            return passTo == null || Math.abs(x - ownX) <= ownRadius && Math.abs(z - ownZ) <= ownRadius;
        }

        @Override
        public int countNpcs(World world, List<String> names, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ) {
            if (passTo != null && !(minX <= ownX && ownX <= maxX && minZ <= ownZ && ownZ <= maxZ)) {
                return passTo.countNpcs(world, names, minX, minY, minZ, maxX, maxY, maxZ);
            }
            return spawns.size();
        }

        @Override
        public boolean spawn(World world, String npcName, double x, double y, double z) {
            if (!own(x, z)) {
                foreign.add(new int[] { (int) Math.floor(x), (int) y, (int) Math.floor(z) });
                return passTo.spawn(world, npcName, x, y, z);
            }
            spawns.add(new int[] { (int) Math.floor(x), (int) y, (int) Math.floor(z) });
            spawnTicks.add(tick);
            return true;
        }

        @Override
        public void onFlagBroken(World world, int x, int y, int z) {
            broken++;
        }
    }

    /** Init, a spawn every interval below the cap, safety when uninitialized, NBT round trip. */
    private static final class CampFlagSpawns implements Scenario {

        @Override
        public String name() {
            return "utility.campflag_spawns";
        }

        @Override
        public String description() {
            return "An initialized Camp Flag spawns through the port every 200 ticks until its cap; an uninitialized flag does nothing; the cap survives NBT.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            int ox = ctx.originX();
            int oy = ctx.originY();
            int oz = ctx.originZ();
            ctx.setBlock(0, 0, 0, WorldBlockRegistration.campFlag(), 0);
            ctx.setBlock(6, 0, 0, WorldBlockRegistration.campFlag(), 0);
            TileEntity a = world.getTileEntity(ox, oy, oz);
            TileEntity b = world.getTileEntity(ox + 6, oy, oz);
            ScenarioResult.Builder pre = ScenarioResult.builder(name());
            pre.check(a instanceof TileCampFlag && b instanceof TileCampFlag, "both flags have their tile entity");
            if (!(a instanceof TileCampFlag) || !(b instanceof TileCampFlag)) {
                return pre.build();
            }
            TileCampFlag flag = (TileCampFlag) a;
            TileCampFlag idle = (TileCampFlag) b;
            pre.check(!flag.isInitialized(), "a fresh flag is uninitialized");
            flag.init(32, 32, 0, 0, 0, new int[] { 1, 0, 1, 3, 0, 3 }, Arrays.asList("test_npc"), 2);
            pre.checkEquals("explicit cap", 2, flag.maxNpcs());

            TileCampFlag defaulted = new TileCampFlag();
            defaulted.init(32, 32, 0, 0, 0, null, Collections.<String>emptyList(), -1);
            pre.checkEquals("default cap int(((32 + 32) / 2) * 0.75)", 24, defaulted.maxNpcs());

            NBTTagCompound tag = new NBTTagCompound();
            flag.writeToNBT(tag);
            TileCampFlag loaded = new TileCampFlag();
            loaded.readFromNBT(tag);
            pre.checkEquals("maxNPCs survives NBT", 2, loaded.maxNpcs());
            pre.check(loaded.isInitialized(), "initialized survives NBT");
            pre.checkEquals("spawn points survive NBT", 2, loaded.spawnPointCount());
            pre.checkEquals("npc names survive NBT", Arrays.asList("test_npc"), loaded.npcNames());
            NBTTagCompound idleTag = new NBTTagCompound();
            idle.writeToNBT(idleTag);
            TileCampFlag idleLoaded = new TileCampFlag();
            idleLoaded.readFromNBT(idleTag);
            pre.check(!idleLoaded.isInitialized(), "an uninitialized flag stays so after NBT");
            // An uninitialized flag ticks safely (MUTL-39); its tile has no world, and the in-world one is ticked by
            // the game.
            idleLoaded.updateEntity();
            final ScenarioResult first = pre.build();
            if (!first.passed()) {
                return first;
            }

            // Only flags within 8 blocks of the origin count; any other flag in the world (another scenario's,
            // a generated camp) keeps the real port.
            CountingPort port = new CountingPort(CampSpawnPorts.get(), ox, oz, 8);
            CampSpawnPort old = CampSpawnPorts.install(port);
            int interval = UtilityConfig.campFlagSpawnIntervalTicks;
            // Done 100 ticks after the cap is reached (a third spawn would take a whole interval), or at the limit.
            int limit = interval * 4 + 200;
            return ScenarioResult.pending(name(), limit + 120, ticks -> {
                port.tick = ticks;
                int last = port.spawnTicks.isEmpty() ? -1 : port.spawnTicks.get(port.spawnTicks.size() - 1);
                boolean capped = port.spawns.size() >= 2 && ticks >= last + 100;
                if (!capped && ticks < limit) {
                    return null;
                }
                CampSpawnPorts.install(old);
                ScenarioResult.Builder res = ScenarioResult.builder(name());
                for (String line : first.messages()) {
                    res.note(line);
                }
                res.note(
                    "campflag diagnostics: ticks " + ticks
                        + ", own spawns "
                        + port.spawns.size()
                        + " at ticks "
                        + port.spawnTicks
                        + ", foreign spawns "
                        + port.foreign.size()
                        + ", flag chunk loaded "
                        + world.getChunkProvider()
                            .chunkExists(ox >> 4, oz >> 4)
                        + ", flag tile still there "
                        + (world.getTileEntity(ox, oy, oz) == flag)
                        + ", origin "
                        + ox
                        + ","
                        + oy
                        + ","
                        + oz);
                res.checkEquals("spawns until the cap, none from the uninitialized flag", 2, port.spawns.size());
                if (port.spawnTicks.size() == 2) {
                    res.check(
                        Math.abs(port.spawnTicks.get(0) - interval) <= 3,
                        "first spawn near one interval: " + port.spawnTicks.get(0));
                    res.check(
                        Math.abs(port.spawnTicks.get(1) - 2 * interval) <= 3,
                        "second spawn one interval later: " + port.spawnTicks.get(1));
                    for (int[] at : port.spawns) {
                        boolean atPoint = at[0] == ox + 1 && at[2] == oz + 1 || at[0] == ox + 3 && at[2] == oz + 3;
                        res.check(atPoint, "spawn at a configured point, got " + Arrays.toString(at));
                    }
                }
                return res.build();
            });
        }
    }

    /** Breaking a flag drops one iron ingot and calls the port once. */
    private static final class CampFlagBreakDrop implements Scenario {

        @Override
        public String name() {
            return "utility.campflag_break_drop";
        }

        @Override
        public String description() {
            return "Breaking a Camp Flag drops exactly one iron ingot (damage 0) and calls the break port once, unless the config switches it off.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            int ox = ctx.originX();
            int oy = ctx.originY();
            int oz = ctx.originZ();
            CountingPort port = new CountingPort();
            CampSpawnPort old = CampSpawnPorts.install(port);
            boolean heli = UtilityConfig.campFlagBreakHelicopter;
            try {
                ctx.setBlock(0, 0, 0, WorldBlockRegistration.campFlag(), 0);
                world.func_147480_a(ox, oy, oz, true);
                out.check(world.isAirBlock(ox, oy, oz), "the flag is gone");
                out.checkEquals("break port calls", 1, port.broken);
                AxisAlignedBB around = AxisAlignedBB.getBoundingBox(ox - 2, oy - 2, oz - 2, ox + 3, oy + 3, oz + 3);
                List<?> items = world.getEntitiesWithinAABB(EntityItem.class, around);
                int ingots = 0;
                for (Object o : items) {
                    EntityItem item = (EntityItem) o;
                    ItemStack stack = item.getEntityItem();
                    if (stack.getItem() == Items.iron_ingot && stack.getItemDamage() == 0) {
                        ingots += stack.stackSize;
                    } else {
                        out.check(false, "unexpected drop " + stack);
                    }
                    item.setDead();
                }
                out.checkEquals("iron ingots dropped", 1, ingots);

                UtilityConfig.campFlagBreakHelicopter = false;
                ctx.setBlock(0, 0, 0, WorldBlockRegistration.campFlag(), 0);
                world.func_147480_a(ox, oy, oz, false);
                out.checkEquals("break port calls with the helicopter switched off", 1, port.broken);
            } finally {
                UtilityConfig.campFlagBreakHelicopter = heli;
                CampSpawnPorts.install(old);
            }
            return out.build();
        }
    }
}
