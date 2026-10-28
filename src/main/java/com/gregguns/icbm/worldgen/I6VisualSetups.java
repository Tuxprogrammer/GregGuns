package com.gregguns.icbm.worldgen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.DimensionManager;

import com.gregguns.GregGuns;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.MissingPart;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.complex.SiloSlot;
import com.gregguns.icbm.complex.Tunnel;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.complex.gen.GtMachineHook;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.Footprint;
import com.gregguns.worldgen.PlacementHarnessAccess;
import com.gregguns.worldgen.PlanKey;
import com.gregguns.worldgen.PoolEntry;
import com.gregguns.worldgen.PopulateGuard;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.SizeClass;
import com.gregguns.worldgen.StructureGrid;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.WorldColumnReader;
import com.gregguns.worldgen.WorldTarget;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Dev-only setups for the I6 client session (two connected clients on a silo complex). They generate a complex at a
 * chosen activity level and leave it standing, dump its plan in world coordinates ("I6C" log lines), and run small
 * operations read from {@code i6c-ops.txt} in the server's working directory (cameras, a roof cut-away, taking the
 * missing silo part out of a chest, powering a radar). None is part of {@code test all}. Server side.
 */
public final class I6VisualSetups {

    private static final int GROUND = 32;
    private static final long SEED = 0x51105L;
    private static final File OPS = new File("i6c-ops.txt");

    /** The cell of the flat slab complex; the {@code cell} operation changes it. */
    private static int cellX = 3;
    private static int cellZ = 4;
    /** The last complex dumped, for the operations. */
    private static StructurePlan lastPlan;
    private static ComplexPlan lastCp;
    private static World lastWorld;
    private static int lastFloor;

    private I6VisualSetups() {}

    /** @param registry where to add the setups */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Gen("ab", ActivityLevel.ABANDONED, SiloCeiling.EV));
        registry.register(new Gen("pa", ActivityLevel.PARTLY_ABANDONED, SiloCeiling.EV));
        registry.register(new Gen("op", ActivityLevel.OPERATIONAL, SiloCeiling.EV));
        registry.register(new Gen("op_umv", ActivityLevel.OPERATIONAL, SiloCeiling.UMV));
        registry.register(new Planet("moon", 4));
        registry.register(new Planet("mars", 4));
        registry.register(new Planet("moon_fast", 100000));
        registry.register(new Planet("mars_fast", 100000));
        registry.register(new Do());
        registry.register(new Info());
        registry.register(new Scan());
    }

    private abstract static class Setup implements Scenario {

        @Override
        public boolean includedInAll() {
            return false;
        }

        @Override
        public int areaRadius() {
            return 4;
        }
    }

    // --- planning ----------------------------------------------------------------------------------------------

    private static StructurePlan planOf(World world, int cx, int cz) {
        StructureGrid g = new StructureGrid(
            64,
            64,
            64,
            Collections.singletonMap(
                SizeClass.BIG,
                Collections.singletonList(new PoolEntry(ComplexWriter.ID, 1, 1, new Footprint(240, 12, 240)))));
        return g.planAt(SEED, world.provider.dimensionId, SizeClass.BIG, cx, cz)
            .get();
    }

    @SuppressWarnings("unchecked")
    private static void killEntities(World w, StructurePlan p) {
        for (Object o : new ArrayList<Object>(w.loadedEntityList)) {
            Entity e = (Entity) o;
            if (e instanceof EntityPlayerMP) continue;
            if (e.posX >= p.anchorX() - 8 && e.posX < p.anchorX() + 248
                && e.posZ >= p.anchorZ() - 8
                && e.posZ < p.anchorZ() + 248) {
                e.setDead();
            }
        }
    }

    // --- gen ---------------------------------------------------------------------------------------------------

    private static final class Gen extends Setup {

        private final String tag;
        private final ActivityLevel level;
        private final int ceiling;

        Gen(String tag, ActivityLevel level, int ceiling) {
            this.tag = tag;
            this.level = level;
            this.ceiling = ceiling;
        }

        @Override
        public String name() {
            return "testing.icbm_i6_gen_" + tag;
        }

        @Override
        public String description() {
            return "I6 client-session setup (dev only): generates the complex of the chosen cell at " + level
                + " and leaves it standing. Not part of test all.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final World world = ctx.world();
            final StructurePlan plan = planOf(world, cellX, cellZ);
            final ComplexPlacer placer = ComplexLevelScenarios.placer()
                .forceLevel(level)
                .forceCeiling(ceiling);
            placer.enabledIn(world);
            final ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
            final List<int[]> chunks = new ArrayList<int[]>();
            for (int cx = Math.floorDiv(plan.anchorX(), 16) - 2; cx
                <= Math.floorDiv(plan.anchorX() + 240, 16) + 2; cx++) {
                for (int cz = Math.floorDiv(plan.anchorZ(), 16) - 2; cz
                    <= Math.floorDiv(plan.anchorZ() + 240, 16) + 2; cz++) {
                    chunks.add(new int[] { cx, cz });
                }
            }
            final List<int[]> windows = PlacementHarnessAccess.windowsOf(plan, false);
            final StructureState state = new StructureState("gregguns_complex_i6c_" + tag);
            final StructurePopulator populator = PlacementHarnessAccess.populator();
            final int[] at = { 0, 0, 0, 0 }; // chunk index, window index, rejected, violations
            final int before = PopulateGuard.violationCount();
            // The whole build runs in slices of a few hundred milliseconds so the connected clients keep their
            // keep-alives (a single 80 s tick disconnects them).
            return ScenarioResult.pending(name(), 40000, ticks -> {
                long end = System.nanoTime() + 250_000_000L;
                while (System.nanoTime() < end) {
                    if (at[0] < chunks.size()) {
                        int[] c = chunks.get(at[0]++);
                        cps.loadChunk(c[0], c[1]);
                    } else if (at[1] < windows.size()) {
                        int[] w = windows.get(at[1]++);
                        PopulateWindow window = new PopulateWindow(w[0], w[1]);
                        ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
                        try (PopulateGuard.Scope ignored = PopulateGuard.enter(w[0], w[1])) {
                            populator.populateComplex(
                                plan,
                                placer,
                                window,
                                sink,
                                new WorldColumnReader(world),
                                (x, z) -> Collections.singleton(Type.PLAINS),
                                state);
                        }
                        at[2] += sink.rejectedWrites();
                    } else {
                        at[3] = PopulateGuard.violationCount() - before;
                        Decision d = state.get(PlanKey.of(plan));
                        GregGuns.LOG.info("I6C gen {} rejected={} violations={} decision={}", tag, at[2], at[3], d);
                        dump(world, plan, ComplexPlacer.floorY(world, d == null ? 32 : d.groundY()), tag);
                        return ScenarioResult.builder(name())
                            .build();
                    }
                }
                return null;
            });
        }
    }

    // --- the dump ----------------------------------------------------------------------------------------------

    private static void dump(World world, StructurePlan plan, int floor, String tag) {
        ComplexPlan cp = ComplexLayout.fromSeed(plan.chestSeedBase(), plan.cellX(), plan.cellZ());
        lastPlan = plan;
        lastCp = cp;
        lastWorld = world;
        lastFloor = floor;
        int ax = plan.anchorX(), az = plan.anchorZ();
        GregGuns.LOG.info(
            "I6C plan tag={} dim={} anchor={},{} floor={} cell={},{} silos={} radars={}",
            tag,
            world.provider.dimensionId,
            ax,
            az,
            floor,
            plan.cellX(),
            plan.cellZ(),
            cp.silos()
                .size(),
            cp.radarStations()
                .size());
        logRoom(ax, az, floor, cp.hall(), "HALL");
        for (Room r : cp.rooms()) {
            logRoom(ax, az, floor, r, "ROOM");
        }
        int i = 0;
        for (SiloSlot s : cp.silos()) {
            Box b = s.bounds();
            GregGuns.LOG.info(
                "I6C silo {} min={},{} max={},{} variant={} missing={}",
                i++,
                ax + b.minX,
                az + b.minZ,
                ax + b.maxX,
                az + b.maxZ,
                s.variant(),
                s.missing());
        }
        i = 0;
        for (int[] r : cp.radarStations()) {
            GregGuns.LOG.info("I6C radar {} at={},{}", i++, ax + r[0], az + r[1]);
        }
        GregGuns.LOG.info("I6C escape at={},{} meta={}", ax + cp.escapeX(), az + cp.escapeZ(), cp.escapeMeta());
        for (Tunnel t : cp.tunnels()) {
            Box b = t.bounds();
            GregGuns.LOG.info(
                "I6C tunnel wing={} min={},{} max={},{}",
                t.wing(),
                ax + b.minX,
                az + b.minZ,
                ax + b.maxX,
                az + b.maxZ);
        }
        // Chests: where each stands and whether it holds a silo part.
        ItemStack ctl = com.gregguns.icbm.worldgen.SiloBonusItems.stack(Placement.Bonus.SILO_CONTROLLER, null);
        ItemStack hatch = com.gregguns.icbm.worldgen.SiloBonusItems.stack(Placement.Bonus.SILO_ENERGY_HATCH, null);
        int chests = 0, found = 0;
        for (Mark m : cp.allMarks()) {
            if (m.kind() != MarkKind.LOOT_CHEST) continue;
            chests++;
            TileEntity t = world.getTileEntity(ax + m.x(), floor + m.y(), az + m.z());
            if (!(t instanceof TileEntityChest)) continue;
            found++;
            TileEntityChest c = (TileEntityChest) t;
            int n = 0;
            StringBuilder parts = new StringBuilder();
            for (int s = 0; s < c.getSizeInventory(); s++) {
                ItemStack st = c.getStackInSlot(s);
                if (st == null) continue;
                n++;
                if (same(st, ctl)) parts.append(" CONTROLLER");
                if (same(st, hatch)) parts.append(" ENERGY_HATCH");
            }
            GregGuns.LOG.info(
                "I6C chest at={},{},{} key={} stacks={}{}",
                ax + m.x(),
                floor + m.y(),
                az + m.z(),
                m.key(),
                n,
                parts);
        }
        GregGuns.LOG.info("I6C chests marks={} realChests={}", chests, found);
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return a != null && b != null && a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage();
    }

    private static void logRoom(int ax, int az, int floor, Room r, String what) {
        Box b = r.bounds();
        String door = "none";
        if (r.wing() >= 0) {
            boolean alongZ = r.wing() == ComplexLayout.N || r.wing() == ComplexLayout.S;
            if (alongZ) {
                boolean high = b.maxX <= 118;
                door = "wallX=" + (ax + (high ? b.maxX - 1 : b.minX))
                    + " z="
                    + (az + b.centerZ() - 1)
                    + ".."
                    + (az + b.centerZ())
                    + " inward="
                    + (high ? "-X" : "+X");
            } else {
                boolean high = b.maxZ <= 118;
                door = "wallZ=" + (az + (high ? b.maxZ - 1 : b.minZ))
                    + " x="
                    + (ax + b.centerX() - 1)
                    + ".."
                    + (ax + b.centerX())
                    + " inward="
                    + (high ? "-Z" : "+Z");
            }
        }
        GregGuns.LOG.info(
            "I6C {} id={} kind={} wing={} min={},{} max={},{} door[{}]",
            what,
            r.id(),
            r.kind(),
            r.wing(),
            ax + b.minX,
            az + b.minZ,
            ax + b.maxX,
            az + b.maxZ,
            door);
        for (Mark m : r.marks()) {
            if (m.kind() == MarkKind.LIGHT || m.kind() == MarkKind.DAMAGE_CELL) continue;
            GregGuns.LOG.info(
                "I6C mark room={} {} key={} at={},{},{} facing={} extent={}",
                r.id(),
                m.kind(),
                m.key(),
                ax + m.x(),
                floor + m.y(),
                az + m.z(),
                m.facing(),
                m.extent());
        }
    }

    private static final class Info extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i6_info";
        }

        @Override
        public String description() {
            return "I6 client-session setup (dev only): dumps the plan of the chosen cell in world coordinates. Not part of test all.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            dump(world, planOf(world, cellX, cellZ), ComplexPlacer.floorY(world, GROUND), "info");
            return ScenarioResult.builder(name())
                .build();
        }
    }

    // --- scan: which cell has the rooms I need -------------------------------------------------------------------

    private static final class Scan extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i6_scan";
        }

        @Override
        public String description() {
            return "I6 client-session setup (dev only): logs the silo count, radar count and notable rooms of cells 0..9 so the session can pick one. Not part of test all.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            for (int cx = 0; cx < 10; cx++) {
                for (int cz = 0; cz < 10; cz++) {
                    StructurePlan p = planOf(world, cx, cz);
                    ComplexPlan cp = ComplexLayout.fromSeed(p.chestSeedBase(), p.cellX(), p.cellZ());
                    int workshop = 0, pump = 0, stock = 0, armory = 0, sw = 0;
                    StringBuilder stockSides = new StringBuilder();
                    for (Room r : cp.rooms()) {
                        if (r.kind() == RoomKind.WORKSHOP) workshop++;
                        if (r.kind() == RoomKind.PUMP_ROOM) pump++;
                        if (r.kind() == RoomKind.ARMORY) armory++;
                        if (r.kind() == RoomKind.STOCKPILE) {
                            stock++;
                            stockSides.append(r.wing())
                                .append(
                                    r.bounds()
                                        .centerX() < 120 ? "L" : "H")
                                .append(' ');
                        }
                    }
                    boolean nasa = false;
                    for (Mark m : cp.allMarks()) {
                        if (m.kind() == MarkKind.MACHINE && m.key()
                            .equals("NASA_WORKBENCH")) nasa = true;
                    }
                    GregGuns.LOG.info(
                        "I6C scan cell={},{} anchor={},{} silos={} radars={} workshop={} nasa={} pump={} armory={} stock={} [{}] rooms={}",
                        cx,
                        cz,
                        p.anchorX(),
                        p.anchorZ(),
                        cp.silos()
                            .size(),
                        cp.radarStations()
                            .size(),
                        workshop,
                        nasa,
                        pump,
                        armory,
                        stock,
                        stockSides.toString()
                            .trim(),
                        cp.rooms()
                            .size());
                }
            }
            return ScenarioResult.builder(name())
                .build();
        }
    }

    // --- operations --------------------------------------------------------------------------------------------

    private static EntityPlayerMP player(String name) {
        return MinecraftServer.getServer()
            .getConfigurationManager()
            .func_152612_a(name);
    }

    private static final class Do extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i6_do";
        }

        @Override
        public String description() {
            return "I6 client-session setup (dev only): runs the operations in i6c-ops.txt (cell, cam, tp, mode, cut, sig, part, placepart, hatchradar, breakblock, shellcheck, planet, dim). Not part of test all.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            List<String> lines;
            try {
                if (!OPS.exists()) {
                    GregGuns.LOG.info("I6C do: no ops file");
                    return ScenarioResult.builder(name())
                        .build();
                }
                lines = Files.readAllLines(OPS.toPath(), StandardCharsets.UTF_8);
                Files.move(
                    OPS.toPath(),
                    new File("i6c-ops.done").toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                return ScenarioResult.fail(name(), e.toString());
            }
            for (String line : lines) {
                String l = line.trim();
                if (l.isEmpty() || l.startsWith("#")) continue;
                String[] t = l.split("\\s+");
                try {
                    op(ctx, t);
                } catch (RuntimeException e) {
                    GregGuns.LOG.warn("I6C op failed: {}: {}", l, e.toString());
                }
            }
            return ScenarioResult.builder(name())
                .build();
        }

        private void op(ScenarioContext ctx, String[] t) {
            switch (t[0]) {
                case "cell":
                    cellX = Integer.parseInt(t[1]);
                    cellZ = Integer.parseInt(t[2]);
                    break;
                case "cam": {
                    EntityPlayerMP p = player(t[1]);
                    double[] v = new double[6];
                    for (int i = 0; i < 6; i++) v[i] = Double.parseDouble(t[2 + i]);
                    com.gregguns.icbm.missile.MissileShotsScenario.cam(p, v[0], v[1], v[2], v[3], v[4], v[5]);
                    break;
                }
                case "tp": {
                    EntityPlayerMP p = player(t[1]);
                    float yaw = t.length > 5 ? Float.parseFloat(t[5]) : p.rotationYaw;
                    float pitch = t.length > 6 ? Float.parseFloat(t[6]) : p.rotationPitch;
                    p.capabilities.allowFlying = true;
                    p.sendPlayerAbilities();
                    p.playerNetServerHandler.setPlayerLocation(
                        Double.parseDouble(t[2]),
                        Double.parseDouble(t[3]),
                        Double.parseDouble(t[4]),
                        yaw,
                        pitch);
                    break;
                }
                case "mode": {
                    EntityPlayerMP p = player(t[1]);
                    p.setGameType(
                        "survival".equals(t[2]) ? WorldSettings.GameType.SURVIVAL : WorldSettings.GameType.CREATIVE);
                    p.setHealth(p.getMaxHealth());
                    break;
                }
                case "cut":
                    cut(lastWorld, t);
                    break;
                case "sig": {
                    EntityPlayerMP p = player(t[1]);
                    String[] rest = new String[t.length - 3];
                    System.arraycopy(t, 3, rest, 0, rest.length);
                    p.addChatMessage(new ChatComponentText(AutotestSignal.format(t[2], rest)));
                    break;
                }
                case "part":
                    part(player(t[2]), Integer.parseInt(t[1]));
                    break;
                case "placepart":
                    placePart(Integer.parseInt(t[1]));
                    break;
                case "hatchradar":
                    hatchRadar(Integer.parseInt(t[1]));
                    break;
                case "breakblock": {
                    EntityPlayerMP p = player(t[1]);
                    int x = Integer.parseInt(t[2]), y = Integer.parseInt(t[3]), z = Integer.parseInt(t[4]);
                    boolean ok = p.theItemInWorldManager.tryHarvestBlock(x, y, z);
                    GregGuns.LOG.info("I6C breakblock {},{},{} -> {}", x, y, z, ok);
                    break;
                }
                case "shellcheck":
                    shellCheck(Integer.parseInt(t[1]));
                    break;
                case "dim":
                    dim(player(t[1]), Integer.parseInt(t[2]));
                    break;
                case "probe": {
                    int x = Integer.parseInt(t[1]), y = Integer.parseInt(t[2]), z = Integer.parseInt(t[3]);
                    int r = t.length > 4 ? Integer.parseInt(t[4]) : 0;
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dy = -r; dy <= r; dy++) {
                            for (int dz = -r; dz <= r; dz++) {
                                Block b = lastWorld.getBlock(x + dx, y + dy, z + dz);
                                if (b == Blocks.air && r > 0) continue;
                                TileEntity te = lastWorld.getTileEntity(x + dx, y + dy, z + dz);
                                GregGuns.LOG.info(
                                    "I6C probe {},{},{} {} meta={} tile={}",
                                    x + dx,
                                    y + dy,
                                    z + dz,
                                    Block.blockRegistry.getNameForObject(b),
                                    lastWorld.getBlockMetadata(x + dx, y + dy, z + dz),
                                    te == null ? "-"
                                        : te.getClass()
                                            .getSimpleName());
                            }
                        }
                    }
                    break;
                }
                case "ents": {
                    EntityPlayerMP p = player(t[1]);
                    double r = Double.parseDouble(t[2]);
                    java.util.Map<String, Integer> n = new java.util.TreeMap<String, Integer>();
                    for (Object o : new ArrayList<Object>(p.worldObj.loadedEntityList)) {
                        Entity e = (Entity) o;
                        if (e instanceof EntityPlayerMP || e.getDistanceToEntity(p) > r) continue;
                        String k = e.getClass()
                            .getSimpleName();
                        n.merge(k, 1, Integer::sum);
                        if (e instanceof net.minecraft.entity.item.EntityItemFrame) {
                            ItemStack shown = ((net.minecraft.entity.item.EntityItemFrame) e).getDisplayedItem();
                            GregGuns.LOG.info(
                                "I6C frame at {},{},{} shows {}",
                                (int) Math.floor(e.posX),
                                (int) Math.floor(e.posY),
                                (int) Math.floor(e.posZ),
                                shown == null ? "NOTHING" : shown.getDisplayName());
                            continue;
                        }
                        if (!(e instanceof net.minecraft.entity.item.EntityItem)) {
                            GregGuns.LOG.info(
                                "I6C ent {} at {},{},{} hp={}",
                                k,
                                (int) e.posX,
                                (int) e.posY,
                                (int) e.posZ,
                                e instanceof net.minecraft.entity.EntityLivingBase
                                    ? ((net.minecraft.entity.EntityLivingBase) e).getHealth()
                                    : -1);
                        }
                    }
                    GregGuns.LOG.info("I6C ents within {} of {}: {}", r, t[1], n);
                    break;
                }
                case "kill":
                    killEntities(lastWorld, lastPlan);
                    break;
                default:
                    GregGuns.LOG.warn("I6C unknown op {}", t[0]);
            }
        }
    }

    /** {@code cut x0 z0 x1 z1 y0 y1}: air in the box, direct writes (the roof cut-away for overviews). */
    private static void cut(World w, String[] t) {
        int x0 = Integer.parseInt(t[1]), z0 = Integer.parseInt(t[2]), x1 = Integer.parseInt(t[3]),
            z1 = Integer.parseInt(t[4]), y0 = Integer.parseInt(t[5]), y1 = Integer.parseInt(t[6]);
        int n = 0;
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                for (int y = y0; y <= y1; y++) {
                    if (w.getBlock(x, y, z) != Blocks.air) {
                        w.setBlock(x, y, z, Blocks.air, 0, 2);
                        n++;
                    }
                }
            }
        }
        GregGuns.LOG.info("I6C cut removed {} blocks", n);
    }

    private static ItemStack partFor(SiloSlot s) {
        return SiloBonusItems.stack(
            s.missing() == MissingPart.CONTROLLER ? Placement.Bonus.SILO_CONTROLLER : Placement.Bonus.SILO_ENERGY_HATCH,
            null);
    }

    /** Moves the missing part of silo i from the first complex chest that holds it into the player's inventory. */
    private static void part(EntityPlayerMP p, int silo) {
        SiloSlot s = lastCp.silos()
            .get(silo);
        ItemStack want = partFor(s);
        int ax = lastPlan.anchorX(), az = lastPlan.anchorZ();
        for (Mark m : lastCp.allMarks()) {
            if (m.kind() != MarkKind.LOOT_CHEST) continue;
            TileEntity t = lastWorld.getTileEntity(ax + m.x(), lastFloor + m.y(), az + m.z());
            if (!(t instanceof TileEntityChest)) continue;
            TileEntityChest c = (TileEntityChest) t;
            for (int i = 0; i < c.getSizeInventory(); i++) {
                ItemStack st = c.getStackInSlot(i);
                if (same(st, want)) {
                    ItemStack taken = c.decrStackSize(i, 1);
                    p.inventory.addItemStackToInventory(taken);
                    p.inventoryContainer.detectAndSendChanges();
                    GregGuns.LOG.info(
                        "I6C part silo={} missing={} taken from chest {},{},{} ({})",
                        silo,
                        s.missing(),
                        c.xCoord,
                        c.yCoord,
                        c.zCoord,
                        m.key());
                    return;
                }
            }
        }
        GregGuns.LOG.info("I6C part silo={} missing={}: NO chest of this complex holds it", silo, s.missing());
    }

    /** Places the silo's missing part the way the scenarios do and checks the structure. */
    private static void placePart(int silo) {
        SiloSlot s = lastCp.silos()
            .get(silo);
        ItemStack part = partFor(s);
        int ox = lastPlan.anchorX() + s.bounds().minX, oz = lastPlan.anchorZ() + s.bounds().minZ;
        boolean noController = s.missing() == MissingPart.CONTROLLER;
        int y = lastFloor + (noController ? 2 : 1);
        GtMachineHook.placeMte(lastWorld, part.getItemDamage(), ox + 2, y, oz);
        TileEntity ctl = lastWorld.getTileEntity(ox + 2, lastFloor + 2, oz);
        if (ctl instanceof IGregTechTileEntity) {
            IGregTechTileEntity g = (IGregTechTileEntity) ctl;
            g.setOwnerName("complex-test");
            g.setOwnerUuid(UUID.nameUUIDFromBytes("complex-test".getBytes(StandardCharsets.UTF_8)));
        }
        GregGuns.LOG.info(
            "I6C placepart silo={} at {},{},{} formed={} controller=({},{},{})",
            silo,
            ox + 2,
            y,
            oz,
            com.gregguns.machine.DevStructureBuilder.check(lastWorld, ox + 2, lastFloor + 2, oz),
            ox + 2,
            lastFloor + 2,
            oz);
    }

    /** Finds the radar controller of hut n, swaps its Radar Casing for an HV Energy Hatch, powers it and checks. */
    private static void hatchRadar(int n) {
        int[] r = lastCp.radarStations()
            .get(n);
        int cx = lastPlan.anchorX() + r[0], cz = lastPlan.anchorZ() + r[1];
        for (int y = 5; y < 120; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    TileEntity t = lastWorld.getTileEntity(cx + dx, y, cz + dz);
                    if (t instanceof IGregTechTileEntity && ((IGregTechTileEntity) t)
                        .getMetaTileEntity() instanceof com.gregguns.icbm.defense.MTERadar) {
                        IGregTechTileEntity c = (IGregTechTileEntity) t;
                        c.setOwnerName("complex-test");
                        c.setOwnerUuid(UUID.nameUUIDFromBytes("complex-test".getBytes(StandardCharsets.UTF_8)));
                        boolean before = com.gregguns.machine.DevStructureBuilder.check(lastWorld, cx + dx, y, cz + dz);
                        GtMachineHook.placeMte(
                            lastWorld,
                            ItemList.Hatch_Energy_HV.get(1)
                                .getItemDamage(),
                            cx + dx,
                            y - 1,
                            cz + dz);
                        TileEntity h = lastWorld.getTileEntity(cx + dx, y - 1, cz + dz);
                        if (h instanceof IGregTechTileEntity) {
                            ((IGregTechTileEntity) h).increaseStoredEnergyUnits(100_000_000L, true);
                        }
                        boolean after = com.gregguns.machine.DevStructureBuilder.check(lastWorld, cx + dx, y, cz + dz);
                        GregGuns.LOG.info(
                            "I6C hatchradar {} controller={},{},{} formedBefore={} formedAfter={}",
                            n,
                            cx + dx,
                            y,
                            cz + dz,
                            before,
                            after);
                        return;
                    }
                }
            }
        }
        GregGuns.LOG.info("I6C hatchradar {}: no radar controller near {},{}", n, cx, cz);
    }

    /** Counts the non-solid cells of a room's perimeter shell (floor-level to ceiling), doorway cells excluded. */
    private static void shellCheck(int roomId) {
        Room room = null;
        for (Room r : lastCp.rooms()) if (r.id() == roomId) room = r;
        if (room == null) {
            GregGuns.LOG.info("I6C shellcheck: no room {}", roomId);
            return;
        }
        Box b = room.bounds();
        int ax = lastPlan.anchorX(), az = lastPlan.anchorZ();
        int holes = 0;
        StringBuilder where = new StringBuilder();
        for (int x = b.minX; x < b.maxX; x++) {
            for (int z = b.minZ; z < b.maxZ; z++) {
                boolean edge = x == b.minX || x == b.maxX - 1 || z == b.minZ || z == b.maxZ - 1;
                if (!edge) continue;
                for (int y = 0; y <= 6; y++) {
                    Block blk = lastWorld.getBlock(ax + x, lastFloor + y, az + z);
                    if (blk == Blocks.air || !blk.getMaterial()
                        .isSolid()) {
                        holes++;
                        if (where.length() < 400) where.append(" ")
                            .append(ax + x)
                            .append(",")
                            .append(lastFloor + y)
                            .append(",")
                            .append(az + z);
                    }
                }
            }
        }
        GregGuns.LOG
            .info("I6C shellcheck room={} kind={} nonSolidShellCells={} [{}]", roomId, room.kind(), holes, where);
    }

    // --- planets -----------------------------------------------------------------------------------------------

    private static WorldServer worldNamed(String dimName) {
        MinecraftServer srv = MinecraftServer.getServer();
        if (dimName.equalsIgnoreCase("Overworld")) return srv.worldServerForDimension(0);
        for (int id : DimensionManager.getStaticDimensionIDs()) {
            try {
                WorldProvider p = DimensionManager.createProviderFor(id);
                if (p != null && dimName.equalsIgnoreCase(p.getDimensionName())) return srv.worldServerForDimension(id);
            } catch (Throwable t) {
                // a provider that cannot name itself without a world is not the one we want
            }
        }
        return null;
    }

    /** Loads chunks around the first far cell that rolls a complex on the planet, through the real generator. */
    private static final class Planet extends Setup {

        private final String dimName;
        private final String tag;
        private final int perTick;

        /** @param tag the dimension name, with "_fast" to load every chunk in one tick like the headless scenario */
        Planet(String tag, int perTick) {
            this.tag = tag;
            this.dimName = tag.replace("_fast", "");
            this.perTick = perTick;
        }

        @Override
        public String name() {
            return "testing.icbm_i6_planet_" + tag;
        }

        @Override
        public String description() {
            return "I6 client-session setup (dev only): generates a real complex on '" + dimName
                + "' through the chunk provider (chance forced to 100 percent) and leaves it. Not part of test all.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            final WorldServer world = worldNamed(dimName);
            if (world == null) {
                GregGuns.LOG.info("I6C planet {}: not present in this environment", dimName);
                return ScenarioResult.skip(name(), "dimension '" + dimName + "' is not present");
            }
            final ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
            PopulateGuard.register();
            final StructureState state = StructureState.forWorld(world);
            StructurePlan found = null;
            for (int ring = 4; ring < 40 && found == null; ring++) {
                for (int k = 0; k < 4 && found == null; k++) {
                    int cx = k % 2 == 0 ? ring : -ring, cz = k < 2 ? ring : -ring;
                    Optional<StructurePlan> op = StructurePopulator
                        .complexPlanAt(world.getSeed(), world.provider.dimensionId, cx, cz, 100);
                    if (op.isPresent() && state.get(PlanKey.of(op.get())) == null) found = op.get();
                }
            }
            if (found == null) return ScenarioResult.fail(name(), "no free cell");
            final StructurePlan plan = found;
            final List<int[]> chunks = new ArrayList<int[]>();
            for (int x = Math.floorDiv(plan.anchorX(), 16) - 1; x <= Math.floorDiv(plan.anchorX() + 239, 16) + 1; x++) {
                for (int z = Math.floorDiv(plan.anchorZ(), 16) - 1; z
                    <= Math.floorDiv(plan.anchorZ() + 239, 16) + 1; z++) {
                    chunks.add(new int[] { x, z });
                }
            }
            final int chance = IcbmConfig.siloComplexChancePercent;
            IcbmConfig.siloComplexChancePercent = 100;
            final int[] at = { 0 };
            return ScenarioResult.pending(name(), 40000, ticks -> {
                long end = System.nanoTime() + 250_000_000L;
                for (int i = 0; i < perTick && (perTick > 1000 || System.nanoTime() < end)
                    && at[0] < chunks.size(); i++) {
                    int[] c = chunks.get(at[0]++);
                    cps.loadChunk(c[0], c[1]);
                }
                if (at[0] < chunks.size()) return null;
                IcbmConfig.siloComplexChancePercent = chance;
                Decision d = state.get(PlanKey.of(plan));
                net.minecraft.world.chunk.Chunk mid = cps
                    .loadChunk(Math.floorDiv(plan.anchorX() + 120, 16), Math.floorDiv(plan.anchorZ() + 120, 16));
                GregGuns.LOG.info(
                    "I6C planet probe: populates generator {}, stamped {}, midChunkPopulated {}, loadedChunks {}, provider {}, anchor {},{}",
                    com.gregguns.worldgen.StructureGenerator.populateCount(),
                    com.gregguns.worldgen.StructureGenerator.stampedCount(),
                    mid.isTerrainPopulated,
                    cps.getLoadedChunkCount(),
                    cps.currentChunkProvider.getClass()
                        .getName(),
                    plan.anchorX(),
                    plan.anchorZ());
                GregGuns.LOG.info(
                    "I6C planet {} dim={} provider={} cell={},{} decision={}",
                    dimName,
                    world.provider.dimensionId,
                    world.provider.getDimensionName(),
                    plan.cellX(),
                    plan.cellZ(),
                    d);
                if (d != null && d.isPlaced()) {
                    dump(world, plan, ComplexPlacer.floorY(world, d.groundY()), "planet-" + dimName);
                    GregGuns.LOG.info("I6C planet ground={}", d.groundY());
                }
                return ScenarioResult.builder(name())
                    .build();
            });
        }
    }

    private static void dim(EntityPlayerMP p, int id) {
        WorldServer target = MinecraftServer.getServer()
            .worldServerForDimension(id);
        if (target == null || p.dimension == id) return;
        MinecraftServer.getServer()
            .getConfigurationManager()
            .transferPlayerToDimension(p, id, new Teleporter(target) {

                @Override
                public void placeInPortal(Entity e, double x, double y, double z, float yaw) {}

                @Override
                public boolean placeInExistingPortal(Entity e, double x, double y, double z, float yaw) {
                    return true;
                }

                @Override
                public boolean makePortal(Entity e) {
                    return true;
                }
            });
        GregGuns.LOG.info("I6C dim {} now in dimension {}", p.getCommandSenderName(), p.dimension);
    }
}
