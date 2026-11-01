package com.gregguns.worldgen.camp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.gregguns.core.config.NpcConfig;
import com.gregguns.core.config.UtilityConfig;
import com.gregguns.core.config.WorldgenConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.DictatorDave;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.gregguns.utility.WorldBlockRegistration;
import com.gregguns.utility.camp.TileCampFlag;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.FlagPos;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.StructureBiomes.CampColor;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.WorldTarget;
import com.gregguns.worldgen.layout.PaletteKeys;

/**
 * The red and blue Military Base (D353), in the real world: a fixed-seed base is written window by window with the red
 * chance forced to 100 or 0, then its flag tile is asked to place its commander the way its tick does. A red base has
 * the red flag, pad and standards, and puts Dictator Dave at his post once, only while {@code npcs.miniBosses} is on;
 * a blue base has the blue flag and pad and no commander. Every block is restored, Dave removed and the config put
 * back. Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (Military Base: faction and command post), D353
public final class CampFactionScenarios {

    private static final long SEED = 0x52454442L;
    private static final int ROTATION = 2;

    private CampFactionScenarios() {}

    /**
     * @param registry where to add the scenarios
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new RedBase());
        registry.register(new BlueBase());
    }

    /** Writes through to the world and remembers every block it changed, for the restore. */
    private static final class Undo implements WindowedSink.Target {

        private final World world;
        private final WorldTarget real;
        private final Map<Long, Object[]> before = new LinkedHashMap<>();

        Undo(World world) {
            this.world = world;
            this.real = new WorldTarget(world);
        }

        @Override
        public void setBlock(int x, int y, int z, Block block, int meta) {
            long k = ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
            if (!before.containsKey(k)) {
                before.put(k, new Object[] { x, y, z, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z) });
            }
            real.setBlock(x, y, z, block, meta);
        }

        @Override
        public void setTile(int x, int y, int z, TileEntity tile) {
            real.setTile(x, y, z, tile);
        }

        @Override
        public void spawn(Entity entity, double x, double y, double z) {
            real.spawn(entity, x, y, z);
        }

        void restore() {
            List<Object[]> all = new ArrayList<>(before.values());
            for (int i = all.size() - 1; i >= 0; i--) {
                Object[] o = all.get(i);
                world.setBlock((Integer) o[0], (Integer) o[1], (Integer) o[2], (Block) o[3], (Integer) o[4], 2);
            }
        }
    }

    /** A base written at the scenario origin with a forced red chance. */
    private static final class Built {

        PlacedCamp camp;
        Undo undo;
        int flags;
    }

    private static Built build(ScenarioContext ctx, Block flagBlock, int redPercent) {
        World world = ctx.world();
        CampPlan plan = CampPlan.generate(SEED);
        int ax = ctx.originX() - plan.sizeX() / 2;
        int az = ctx.originZ() - plan.sizeZ() / 2;
        Built b = new Built();
        b.camp = plan.place(ax, ctx.originY(), az, ROTATION);
        b.undo = new Undo(world);
        int c0x = Math.floorDiv(b.camp.boundsMinX() - 8, 16);
        int c1x = Math.floorDiv(b.camp.boundsMaxX() - 8, 16);
        int c0z = Math.floorDiv(b.camp.boundsMinZ() - 8, 16);
        int c1z = Math.floorDiv(b.camp.boundsMaxZ() - 8, 16);
        for (int cx = c0x; cx <= c1x + 1; cx++) {
            for (int cz = c0z; cz <= c1z + 1; cz++) {
                world.getChunkProvider()
                    .loadChunk(cx, cz); // dev scenario only: load the test area first
            }
        }
        int saved = WorldgenConfig.redBaseChanceMilitaryBase;
        try {
            WorldgenConfig.redBaseChanceMilitaryBase = redPercent;
            CampPlacer placer = new CampPlacer(PaletteKeys.resolveForPack(s -> {}), CampPlaceholders.all());
            for (int cx = c0x; cx <= c1x; cx++) {
                for (int cz = c0z; cz <= c1z; cz++) {
                    PopulateWindow window = new PopulateWindow(cx, cz);
                    WindowedSink sink = new ClippingSink(window, b.undo);
                    placer.write(sink, b.camp, window, CampColor.WOODLAND, (t, x, y, z) -> {});
                    if (placer.flagStep(b.camp, flagBlock, UtilityConfig.campFlagCapFactor, p -> {})
                        .run(sink, window)) {
                        b.flags++;
                    }
                }
            }
        } finally {
            WorldgenConfig.redBaseChanceMilitaryBase = saved;
        }
        return b;
    }

    /** Counts the pad's wool and clay in a color, and the standards' fence posts, around the flag. */
    private static int[] pad(World world, FlagPos f, int color) {
        int wool = 0;
        int clay = 0;
        int fences = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int x = f.x + dx;
                int z = f.z + dz;
                Block b = world.getBlock(x, f.y - 1, z);
                int m = world.getBlockMetadata(x, f.y - 1, z);
                wool += b == Blocks.wool && m == color ? 1 : 0;
                clay += b == Blocks.stained_hardened_clay && m == color ? 1 : 0;
                for (int dy = 0; dy < CommandPost.STANDARD_POLE; dy++) {
                    fences += world.getBlock(x, f.y + dy, z) == Blocks.fence ? 1 : 0;
                }
            }
        }
        return new int[] { wool, clay, fences };
    }

    private static List<DictatorDave> davesNear(World world, FlagPos f) {
        List<DictatorDave> out = new ArrayList<>();
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(f.x - 16, f.y - 4, f.z - 16, f.x + 17, f.y + 8, f.z + 17);
        for (Object o : world.getEntitiesWithinAABB(DictatorDave.class, box)) {
            DictatorDave d = (DictatorDave) o;
            if (!d.isDead) {
                out.add(d);
            }
        }
        return out;
    }

    private static void removeDaves(World world, FlagPos f) {
        for (DictatorDave d : davesNear(world, f)) {
            world.removeEntity(d);
            d.setDead();
        }
    }

    private static final class RedBase implements Scenario {

        @Override
        public String name() {
            return "worldgen.camp_red_base";
        }

        @Override
        public String description() {
            return "A red Military Base (chance forced to 100): the flag is red (metadata 1), a red wool ring and clay"
                + " pad sit under it, four red standards stand at its corners, and the flag holds Dictator Dave as its"
                + " commander. With npcs.miniBosses off the flag places nobody; on, it puts Dave at his post once,"
                + " with the post saved on him, and never again.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Block flagBlock = WorldBlockRegistration.campFlag();
            if (flagBlock == null) {
                return ScenarioResult.fail(name(), "needs the utility module (the Camp Flag block is not registered)");
            }
            if (!NpcRegistry.isRegistered(NpcMob.DICTATOR_DAVE)) {
                return ScenarioResult.fail(name(), "needs the npcs module (Dictator Dave is not registered)");
            }
            World world = ctx.world();
            boolean miniBosses = NpcConfig.miniBosses;
            Built b = build(ctx, flagBlock, 100);
            FlagPos f = b.camp.flagPos();
            try {
                removeDaves(world, f);
                out.checkEquals("flags placed", 1, b.flags);
                out.check(world.getBlock(f.x, f.y, f.z) == flagBlock, "the flag stands at " + f);
                out.checkEquals("the flag's cloth", CampFaction.RED.flagMeta(), world.getBlockMetadata(f.x, f.y, f.z));
                int[] pad = pad(world, f, CampFaction.RED.woolMeta());
                out.checkEquals("red wool ring", 16, pad[0]);
                out.checkEquals("red clay pad", 9, pad[1]);
                out.checkEquals("standard fence posts", 4 * CommandPost.STANDARD_POLE, pad[2]);
                TileEntity te = world.getTileEntity(f.x, f.y, f.z);
                out.check(te instanceof TileCampFlag, "the flag holds its tile");
                if (!(te instanceof TileCampFlag)) {
                    return out.build();
                }
                TileCampFlag tile = (TileCampFlag) te;
                out.checkEquals("commander", "DictatorDave", tile.commander());
                out.check(tile.commanderPending(), "Dave waits for the flag's tick, not for worldgen");
                out.checkEquals("nobody spawned during placement", 0, davesNear(world, f).size());

                NpcConfig.miniBosses = false;
                out.check(!tile.placeCommander(), "with miniBosses off the post stays empty");
                out.check(tile.commanderPending(), "and the flag keeps trying");
                out.checkEquals("no Dave while mini-bosses are off", 0, davesNear(world, f).size());

                NpcConfig.miniBosses = true;
                out.check(tile.placeCommander(), "with miniBosses on Dave takes his post");
                out.check(!tile.commanderPending(), "he is placed once");
                List<DictatorDave> daves = davesNear(world, f);
                out.checkEquals("one Dave", 1, daves.size());
                if (daves.size() == 1) {
                    DictatorDave dave = daves.get(0);
                    int[] post = CommandPost.post(b.camp);
                    int px = f.x + post[0];
                    int pz = f.z + post[2];
                    out.check(
                        Math.abs(dave.posX - (px + 0.5)) < 0.01 && Math.abs(dave.posZ - (pz + 0.5)) < 0.01,
                        "he stands at his post " + px + "," + pz + ": " + dave.posX + "," + dave.posZ);
                    int[] held = dave.post();
                    out.check(
                        held != null && held[0] == px && held[2] == pz && held[3] == CommandPost.POST_RADIUS,
                        "he holds the post: " + java.util.Arrays.toString(held));
                    out.check(dave.getHeldItem() != null, "he carries his golden revolver");
                    NBTTagCompound tag = new NBTTagCompound();
                    dave.writeEntityToNBT(tag);
                    DictatorDave copy = new DictatorDave(world);
                    copy.readEntityFromNBT(tag);
                    out.check(
                        java.util.Arrays.equals(held, copy.post()),
                        "the post is saved with him: " + java.util.Arrays.toString(copy.post()));
                }
                out.check(!tile.placeCommander(), "a second call places nobody");
                out.checkEquals("still one Dave", 1, davesNear(world, f).size());
                NBTTagCompound flagTag = new NBTTagCompound();
                tile.writeToNBT(flagTag);
                out.check(flagTag.getBoolean("commanderPlaced"), "the flag saves that Dave was placed");
            } finally {
                NpcConfig.miniBosses = miniBosses;
                removeDaves(world, f);
                b.undo.restore();
            }
            out.check(world.getBlock(f.x, f.y, f.z) != flagBlock, "restored");
            return out.build();
        }
    }

    private static final class BlueBase implements Scenario {

        @Override
        public String name() {
            return "worldgen.camp_blue_base";
        }

        @Override
        public String description() {
            return "A blue Military Base (red chance forced to 0): the flag is blue (metadata 0), a blue wool ring and"
                + " clay pad sit under it, no standards stand, the flag has no commander and places no Dave.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Block flagBlock = WorldBlockRegistration.campFlag();
            if (flagBlock == null) {
                return ScenarioResult.fail(name(), "needs the utility module (the Camp Flag block is not registered)");
            }
            World world = ctx.world();
            Built b = build(ctx, flagBlock, 0);
            FlagPos f = b.camp.flagPos();
            try {
                removeDaves(world, f);
                out.checkEquals("flags placed", 1, b.flags);
                out.checkEquals("the flag's cloth", CampFaction.BLUE.flagMeta(), world.getBlockMetadata(f.x, f.y, f.z));
                int[] pad = pad(world, f, CampFaction.BLUE.woolMeta());
                out.checkEquals("blue wool ring", 16, pad[0]);
                out.checkEquals("blue clay pad", 9, pad[1]);
                out.checkEquals("no standards", 0, pad[2]);
                out.checkEquals("no red pad", 0, pad(world, f, CampFaction.RED.woolMeta())[0]);
                TileEntity te = world.getTileEntity(f.x, f.y, f.z);
                out.check(te instanceof TileCampFlag, "the flag holds its tile");
                if (te instanceof TileCampFlag) {
                    TileCampFlag tile = (TileCampFlag) te;
                    out.checkEquals("no commander", "", tile.commander());
                    out.check(!tile.placeCommander(), "the flag places nobody");
                }
                out.checkEquals("no Dave at a blue base", 0, davesNear(world, f).size());
            } finally {
                removeDaves(world, f);
                b.undo.restore();
            }
            return out.build();
        }
    }
}
