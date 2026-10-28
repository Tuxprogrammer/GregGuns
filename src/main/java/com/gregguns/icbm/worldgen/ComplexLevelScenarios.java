package com.gregguns.icbm.worldgen;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.NpcCounter;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.complex.gen.GtMachineHook;
import com.gregguns.icbm.complex.gen.FurnitureFiller;
import com.gregguns.icbm.complex.gen.WorldBlocks;
import com.gregguns.icbm.control.ControlMachines;
import com.gregguns.icbm.warhead.BlockWarhead;
import com.gregguns.icbm.worldgen.Placement.Kind;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Footprint;
import com.gregguns.worldgen.PlacementHarnessAccess;
import com.gregguns.worldgen.PoolEntry;
import com.gregguns.worldgen.PopulateGuard;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.RolledCeiling;
import com.gregguns.worldgen.SizeClass;
import com.gregguns.worldgen.StructureGrid;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructurePopulator;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.WorldColumnReader;
import com.gregguns.worldgen.WorldTarget;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.spawner.HoleCaps;
import com.gregguns.worldgen.spawner.HoleKind;
import com.gregguns.worldgen.spawner.HoleSpawnerRegistration;
import com.gregguns.worldgen.spawner.HoleSpawning;
import com.gregguns.worldgen.spawner.TileHoleSpawner;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * The activity-level scenarios of the silo complex (I6 exit box 2): a forced complex on a flat slab at each level, then
 * its damage, spawners, lights, flooding, chests and machines are read back out of the world and compared with what
 * the contents pass said it would place. Dev environment only; needs {@code worldgen} and {@code icbm}. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Tests), loot-and-ceilings.md
public final class ComplexLevelScenarios {

    private static final int GROUND = 63;
    private static final long SEED = 0x51105L;

    private ComplexLevelScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Levels());
        registry.register(new Guards());
        SalvageScenarios.registerAll(registry);
    }

    // --- shared rig (the complex slab; the layout scenarios keep their own copies private) ---------------------

    /** The plan of the fixed test cell. */
    static StructurePlan plan(World world) {
        StructureGrid g = new StructureGrid(
            64,
            64,
            64,
            Collections.singletonMap(
                SizeClass.BIG,
                Collections.singletonList(new PoolEntry(ComplexWriter.ID, 1, 1, new Footprint(240, 12, 240)))));
        return g.planAt(SEED, world.provider.dimensionId, SizeClass.BIG, 3, 4)
            .get();
    }

    static void load(World world, StructurePlan p) {
        com.gregguns.icbm.complex.ComplexArena.load(world, p);
    }

    /** Stone to the ground with a grass top and air above to the chunk's top, or all air when {@code clear}. */
    static void slab(World world, StructurePlan p, boolean clear) {
        com.gregguns.icbm.complex.ComplexArena.slab(world, p, clear, (x, z) -> GROUND, 0, GROUND);
    }

    /** @return the level floor the placer writes at over this scenario's ground in the world */
    static int floor(World world) {
        return com.gregguns.icbm.complex.ComplexArena.floor(world, GROUND);
    }

    static ComplexPlacer placer() {
        return new ComplexPlacer(new WorldBlocks(), new GtMachineHook(), new FurnitureFiller(), new ComplexPlacer.Settings() {

            public boolean dimensionEnabled(String n) {
                return true;
            }

            public int chancePercent() {
                return 100;
            }

            public Set<Type> overworldBiomes() {
                return Collections.emptySet();
            }
        }).withStyles(com.gregguns.icbm.complex.gen.PaletteStyles.FACTORY);
    }

    /**
     * Generates the whole complex through the populator, one window at a time.
     *
     * @return {rejected, violations, windows that wrote, blocks written, total microseconds, most microseconds in one
     *         window}
     */
    static int[] generate(World world, StructurePlan plan, ComplexPlacer placer, String stateName) {
        placer.enabledIn(world);
        StructureState state = new StructureState(stateName);
        StructurePopulator populator = PlacementHarnessAccess.populator();
        int rejected = 0, busy = 0, blocks = 0;
        long total = 0, most = 0;
        int before = PopulateGuard.violationCount();
        for (int[] w : PlacementHarnessAccess.windowsOf(plan, false)) {
            PopulateWindow window = new PopulateWindow(w[0], w[1]);
            ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
            long t0 = System.nanoTime();
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
            long dt = (System.nanoTime() - t0) / 1000;
            if (sink.acceptedWrites() > 0) {
                busy++;
                blocks += sink.acceptedWrites();
                total += dt;
                most = Math.max(most, dt);
            }
            rejected += sink.rejectedWrites();
        }
        return new int[] { rejected, PopulateGuard.violationCount() - before, busy, blocks, (int) total, (int) most };
    }

    // --- the level scenario -----------------------------------------------------------------------------------

    /** The counts one level read back from the world. */
    private static final class Tally {

        int spawners;
        int guard;
        int elite;
        int badTiles;
        int lightsPlaced;
        int lightMarks;
        int lightsLit;
        /** Lowest block light on an open floor cell of any section (15 when nothing was read), and where. */
        int floorMinLight = 15;
        String darkest = "";
        int floodMarks;
        int floodWet;
        int collapses;
        int collapseRubble;
        int holes;
        int rubble;
        int chests;
        int chestsEmpty;
        int overCeiling;
        int machinesWanted;
        int machinesRight;
        int machinesSkipped;
        int casings;
        int liveWarheadBlocks;
        int warheadItems;
        int nasaBad;
        int floodSpill;
        int physics; // damage cells that no longer read as placed (a falling block moved)
    }

    private static final class Levels implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.levels";
        }

        @Override
        public String description() {
            return "Forces each activity level (and a second ceiling) on a generated complex and reads the world back:"
                + " collapses, holes, rubble, lights, the flooded pump room, spawner count and kinds, chest loot under"
                + " the ceiling and the placed machines (real, within the ceiling, no live warhead) match what the"
                + " contents pass placed, and the levels order as D36 says (exit box 2, D129).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructurePlan plan = plan(world);
            SiloChests.register();
            Map<String, Tally> tallies = new HashMap<>();
            try {
                load(world, plan);
                Object[][] runs = { { ActivityLevel.ABANDONED, SiloCeiling.EV },
                    { ActivityLevel.PARTLY_ABANDONED, SiloCeiling.EV }, { ActivityLevel.OPERATIONAL, SiloCeiling.EV },
                    { ActivityLevel.OPERATIONAL, SiloCeiling.UMV } };
                for (Object[] run : runs) {
                    ActivityLevel level = (ActivityLevel) run[0];
                    int ceiling = (Integer) run[1];
                    String tag = level + "@" + SiloCeiling.name(ceiling);
                    slab(world, plan, false);
                    ComplexPlacer placer = placer().forceLevel(level)
                        .forceCeiling(ceiling);
                    int[] r = generate(world, plan, placer, "gregguns_complex_levels");
                    out.checkEquals(tag + ": writes dropped outside a window", 0, r[0]);
                    out.checkEquals(tag + ": PopulateGuard violations", 0, r[1]);
                    if (r[2] > 0) {
                        out.note(
                            String.format(
                                java.util.Locale.ROOT,
                                "%s cost with contents: %d windows, %d blocks, %.1f ms mean and %.1f ms most per"
                                    + " window, %.0f ms in all",
                                tag,
                                r[2],
                                r[3],
                                r[4] / 1000.0 / r[2],
                                r[5] / 1000.0,
                                r[4] / 1000.0));
                    }
                    Tally t = read(out, tag, world, plan, placer, level, ceiling);
                    tallies.put(tag, t);
                    out.note(
                        tag + ": spawners "
                            + t.spawners
                            + " (guard "
                            + t.guard
                            + ", elite "
                            + t.elite
                            + "), lights "
                            + t.lightsLit
                            + "/"
                            + t.lightMarks
                            + ", collapses "
                            + t.collapses
                            + " (rubble "
                            + t.collapseRubble
                            + "), holes "
                            + t.holes
                            + ", rubble "
                            + t.rubble
                            + ", flood "
                            + t.floodWet
                            + "/"
                            + t.floodMarks
                            + ", chests "
                            + t.chests
                            + ", machines "
                            + t.machinesRight
                            + "/"
                            + t.machinesWanted
                            + " (skipped "
                            + t.machinesSkipped
                            + "), casings "
                            + t.casings
                            + ", fell "
                            + t.physics);
                }
                compare(out, tallies);
            } finally {
                slab(world, plan, true);
            }
            return out.build();
        }

        private void compare(ScenarioResult.Builder out, Map<String, Tally> t) {
            Tally ab = t.get("ABANDONED@EV");
            Tally pa = t.get("PARTLY_ABANDONED@EV");
            Tally op = t.get("OPERATIONAL@EV");
            if (ab == null || pa == null || op == null) {
                return;
            }
            out.checkEquals("abandoned spawners", 0, ab.spawners);
            out.check(pa.spawners > 0, "a partly abandoned complex has some guards");
            out.check(
                op.spawners > pa.spawners,
                "operational has the most guards: " + op.spawners + " vs " + pa.spawners);
            out.check(ab.collapses > 0, "an abandoned complex has collapsed rooms");
            out.checkEquals("operational collapses", 0, op.collapses);
            out.check(ab.collapses >= pa.collapses, "collapses fall with activity");
            out.check(ab.holes + ab.rubble > pa.holes + pa.rubble, "damage falls with activity (abandoned > partly)");
            out.check(pa.holes + pa.rubble > op.holes + op.rubble, "damage falls with activity (partly > operational)");
            out.check(
                ab.lightsLit * 4 < ab.lightMarks,
                "an abandoned complex is dark: " + ab.lightsLit + "/" + ab.lightMarks);
            out.checkEquals("operational lights lit", op.lightMarks, op.lightsLit);
            out.check(
                op.floorMinLight >= 8,
                "an operational complex reads block light 8 or more on every open floor cell of every hall and room: "
                    + op.floorMinLight
                    + " at "
                    + op.darkest);
            out.check(ab.floodMarks > 0 ? ab.floodWet == ab.floodMarks : true, "abandoned pump room is flooded");
            out.checkEquals("operational flooding", 0, op.floodWet);
        }

        private Tally read(ScenarioResult.Builder out, String tag, World world, StructurePlan plan,
            ComplexPlacer placer, ActivityLevel level, int ceiling) {
            Tally t = new Tally();
            ComplexPlan cp = placer.rasterOf(plan)
                .plan();
            int ax = plan.anchorX(), az = plan.anchorZ(), floor = floor(world);
            int[] other = { plan.anchorX() + 64 * 16, plan.anchorZ() };
            List<Placement> placements = ComplexContents.build(cp, level, ceiling, other);
            // Last writer wins per cell; collapses are boxes, tallied on their own.
            Map<Long, Placement> last = new HashMap<>();
            List<Placement> collapses = new ArrayList<>();
            for (Placement p : placements) {
                if (p.kind() == Kind.COLLAPSE) {
                    collapses.add(p);
                } else {
                    last.put(key(p.x(), p.y(), p.z()), p);
                }
            }
            BlockHoleSpawnerCheck spawners = new BlockHoleSpawnerCheck();
            com.gregguns.icbm.complex.gen.Styles styles = placer.stylesOf(plan);
            for (Placement p : last.values()) {
                int x = ax + p.x(), y = floor + p.y(), z = az + p.z();
                Block b = world.getBlock(x, y, z);
                switch (p.kind()) {
                    case SPAWNER:
                        t.spawners++;
                        if (p.spawner() == HoleKind.SILO_ELITE) t.elite++;
                        else t.guard++;
                        TileEntity st = world.getTileEntity(x, y, z);
                        if (b != HoleSpawnerRegistration.block(p.spawner()) || !(st instanceof TileHoleSpawner)
                            || ((TileHoleSpawner) st).kind() != p.spawner()
                            || ((TileHoleSpawner) st).ceiling() != ceiling) {
                            t.badTiles++;
                        }
                        break;
                    case LIGHT:
                        t.lightsPlaced++;
                        if (b != litLamp(styles, p.x(), p.y(), p.z())) t.physics++;
                        break;
                    case AIR:
                        t.holes++;
                        if (b != Blocks.air) t.physics++;
                        break;
                    case RUBBLE:
                        t.rubble++;
                        if (b != Blocks.cobblestone && b != Blocks.gravel) t.physics++;
                        break;
                    case WATER:
                        break; // read through the flood marks below
                    case CHEST:
                        t.chests++;
                        TileEntity ct = world.getTileEntity(x, y, z);
                        if (b != Blocks.chest || !(ct instanceof TileEntityChest)) {
                            t.chestsEmpty++;
                            break;
                        }
                        t.overCeiling += outsideTable((IInventory) ct, p, ceiling);
                        t.warheadItems += warheadItems((IInventory) ct);
                        if (empty((IInventory) ct)) t.chestsEmpty++;
                        break;
                    case MACHINE:
                        int rightBefore = t.machinesRight;
                        machine(t, world, p, x, y, z);
                        if (t.machinesRight == rightBefore && t.machinesSkipped == 0) {
                            out.note(tag + ": machine " + p + " reads " + b + " " + world.getTileEntity(x, y, z));
                        }
                        break;
                    default:
                        break;
                }
            }
            for (Placement c : collapses) {
                t.collapses++;
                if (rubbleIn(world, ax, az, floor, c.box())) t.collapseRubble++;
            }
            marks(t, world, cp, styles, ax, az, floor);
            t.liveWarheadBlocks += warheadBlocksIn(world, ax, az, floor);
            out.checkEquals(tag + ": spawner blocks with the wrong tile, kind or ceiling", 0, t.badTiles);
            out.checkEquals(tag + ": chests missing or empty", 0, t.chestsEmpty);
            out.checkEquals(tag + ": chest stacks outside their table", 0, t.overCeiling);
            out.checkEquals(tag + ": machines placed wrong", t.machinesWanted - t.machinesSkipped, t.machinesRight);
            out.checkEquals(tag + ": live warhead blocks", 0, t.liveWarheadBlocks);
            out.checkEquals(tag + ": warhead items in chests (D17)", 0, t.warheadItems);
            out.checkEquals(tag + ": flood water standing above the floor pools", 0, t.floodSpill);
            out.checkEquals(
                tag + ": NASA Workbench footprint cells that are not Galacticraft dummy blocks",
                0,
                t.nasaBad);
            out.check(t.casings > 0, tag + ": the magazine is built of casing frames");
            out.checkEquals(tag + ": collapsed rooms with no rubble", t.collapses, t.collapseRubble);
            out.check(
                t.physics * 50 <= t.holes + t.rubble + t.lightsPlaced,
                tag + ": placed cells that read differently: " + t.physics);
            out.check(
                spawners.totalIn(world, ax, az, floor) == t.spawners,
                tag + ": spawner blocks in the world equal the placements: " + spawners.last + " vs " + t.spawners);
            return t;
        }

        /** @return how many warhead block items an inventory holds (D17 allows none in a complex) */
        private static int warheadItems(IInventory inv) {
            int n = 0;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s != null && Block.getBlockFromItem(s.getItem()) instanceof BlockWarhead) {
                    n++;
                }
            }
            return n;
        }

        /** @return how many warhead blocks (or fillers) stand anywhere in the complex's box */
        private static int warheadBlocksIn(World world, int ax, int az, int floor) {
            int n = 0;
            for (int x = ax; x < ax + 240; x++) {
                for (int z = az; z < az + 240; z++) {
                    for (int y = floor; y < floor + ComplexRaster.LEVELS; y++) {
                        if (world.getBlock(x, y, z) instanceof BlockWarhead) {
                            n++;
                        }
                    }
                }
            }
            return n;
        }

        /** Counts every silo-post block in the complex, to catch strays the placements do not list. */
        private static final class BlockHoleSpawnerCheck {

            int last;

            int totalIn(World world, int ax, int az, int floor) {
                Block g = HoleSpawnerRegistration.block(HoleKind.SILO_GUARD);
                Block e = HoleSpawnerRegistration.block(HoleKind.SILO_ELITE);
                int n = 0;
                for (int x = ax; x < ax + 240; x++) {
                    for (int z = az; z < az + 240; z++) {
                        for (int y = floor; y < floor + ComplexRaster.LEVELS; y++) {
                            Block b = world.getBlock(x, y, z);
                            if (b == g || b == e) n++;
                        }
                    }
                }
                last = n;
                return n;
            }
        }

        private static long key(int x, int y, int z) {
            return ((long) x * 4096 + z) * 4096 + y;
        }

        private static boolean empty(IInventory inv) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (inv.getStackInSlot(i) != null) return false;
            }
            return true;
        }

        private static String id(ItemStack s) {
            return net.minecraft.item.Item.itemRegistry.getNameForObject(s.getItem()) + "@" + s.getItemDamage();
        }

        /** @return stacks in the chest that are neither on the chest's table at the ceiling nor one of its bonuses */
        private static int outsideTable(IInventory inv, Placement p, int ceiling) {
            Set<String> allowed = new HashSet<>();
            for (SiloLootTables.Line l : SiloLootTables.lines(p.key(), ceiling)) {
                ItemStack s = LootCategories.stack(l.ref(), 1);
                if (s != null) allowed.add(id(s));
            }
            for (Placement.Bonus b : p.bonuses()) {
                ItemStack s = SiloBonusItems.stack(b, p);
                if (s != null) allowed.add(id(s));
            }
            int bad = 0;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s != null && !allowed.contains(id(s))) bad++;
            }
            return bad;
        }

        private static boolean rubbleIn(World world, int ax, int az, int floor, com.gregguns.icbm.complex.Box box) {
            for (int x = box.minX + 1; x < box.maxX - 1; x++) {
                for (int z = box.minZ + 1; z < box.maxZ - 1; z++) {
                    for (int y = box.minY + 1; y < box.minY + 4; y++) {
                        Block b = world.getBlock(ax + x, floor + y, az + z);
                        if (b == Blocks.cobblestone || b == Blocks.gravel) return true;
                    }
                }
            }
            return false;
        }

        /** The block a lit lamp of the section at a complex-local cell is: the palette's, else glowstone. */
        private static Block litLamp(com.gregguns.icbm.complex.gen.Styles styles, int x, int y, int z) {
            com.gregguns.worldgen.layout.ResolvedBlock r = styles.litLamp(x, y, z);
            return r == null ? Blocks.glowstone : r.block;
        }

        /** Lights and flooding are read at the layout's own marks. */
        private static void marks(Tally t, World world, ComplexPlan cp, com.gregguns.icbm.complex.gen.Styles styles,
            int ax, int az, int floor) {
            List<Mark> all = cp.allMarks();
            for (Mark m : all) {
                int x = ax + m.x(), y = floor + m.y(), z = az + m.z();
                if (m.kind() == MarkKind.LIGHT) {
                    t.lightMarks++;
                    if (world.getBlock(x, y, z) == litLamp(styles, m.x(), m.y(), m.z())) t.lightsLit++;
                } else if (m.kind() == MarkKind.FLOOD_CELL) {
                    t.floodMarks++;
                    Block b = world.getBlock(x, y - 1, z); // the pool is sunk into the floor under the mark
                    if (b == Blocks.water || b == Blocks.flowing_water) t.floodWet++;
                    if (world.getBlock(x, y, z) == Blocks.water || world.getBlock(x, y, z) == Blocks.flowing_water) {
                        t.floodSpill++;
                    }
                }
            }
            // Every open floor cell of every section: the lamp grid keeps it at 8 or more when lit (D217, D230).
            for (int id = 0; id < cp.sectionCount(); id++) {
                com.gregguns.icbm.complex.Box b = cp.sectionBox(id);
                for (int x = b.minX + 1; x <= b.maxX - 2; x++) {
                    for (int z = b.minZ + 1; z <= b.maxZ - 2; z++) {
                        if (!world.isAirBlock(ax + x, floor + 1, az + z)) continue;
                        int l = world
                            .getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block, ax + x, floor + 1, az + z);
                        if (l < t.floorMinLight) {
                            t.floorMinLight = l;
                            t.darkest = "section " + id + " local " + x + "," + z;
                        }
                    }
                }
            }
        }

        private void machine(Tally t, World world, Placement p, int x, int y, int z) {
            MachineRole role = p.role();
            t.machinesWanted++;
            Block b = world.getBlock(x, y, z);
            TileEntity te = world.getTileEntity(x, y, z);
            if (role == MachineRole.MAGAZINE_CASING) {
                ItemStack frame = ComplexApplier.casingFrame();
                if (frame == null) {
                    t.machinesSkipped++; // GT lacks the frame box: the applier skips it
                } else if (b == Block.getBlockFromItem(frame.getItem())
                    && world.getBlockMetadata(x, y, z) == frame.getItemDamage()
                    && !(b instanceof BlockWarhead)) {
                        t.machinesRight++;
                        t.casings++;
                    }
                return;
            }
            if (role == MachineRole.NASA_WORKBENCH && !p.dead()) {
                if (SiloGc.nasaWorkbench() == null) {
                    t.machinesSkipped++; // Galacticraft absent: the applier skips it
                } else if (b == SiloGc.nasaWorkbench()) {
                    t.machinesRight++;
                    for (int[] c : SiloGc.footprint()) {
                        if (c[1] == 0) {
                            continue;
                        }
                        Block part = world.getBlock(x + c[0], y + c[1], z + c[2]);
                        TileEntity pt = world.getTileEntity(x + c[0], y + c[1], z + c[2]);
                        if (part != SiloGc.fakeBlock() || pt == null) {
                            t.nasaBad++;
                        }
                    }
                }
                return;
            }
            int expectId;
            int tier;
            if (role == MachineRole.LAUNCH_CONTROL && !p.dead()) {
                expectId = ControlMachines.stack(1)
                    .getItemDamage();
                tier = 0;
            } else {
                MachinePicker.Pick pick = MachinePicker.pick(role, Math.max(1, p.tier()), p.dead());
                if (pick == null) {
                    t.machinesSkipped++;
                    return;
                }
                expectId = pick.stack()
                    .getItemDamage();
                tier = pick.tier();
            }
            if (te instanceof IGregTechTileEntity && ((IGregTechTileEntity) te).getMetaTileID() == expectId
                && (tier == 0 || tier <= Math.max(1, p.tier()))) {
                t.machinesRight++;
            }
        }
    }

    // --- guards ----------------------------------------------------------------------------------------------

    private static final int PAD_HALF = 32;

    /** Cancels the drops of the NPCs it knows, so killing them leaves no item entity. */
    public static final class Drops {

        private final Set<Entity> tracked = Collections.newSetFromMap(new IdentityHashMap<Entity, Boolean>());

        void track(Entity e) {
            tracked.add(e);
        }

        @SubscribeEvent
        public void onDrops(LivingDropsEvent event) {
            if (tracked.contains(event.entityLiving)) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * Two silo posts 30 blocks apart on a stone pad far from spawn, a fake player between them. Reads: right mobs,
     * danger from the stored ceiling, the cap, an attack on the player, a broken post staying broken through a chunk
     * save, unload and reload, and the intact post reloading intact.
     */
    private static final class Guards implements Scenario {

        private static final int TIMEOUT = 1000;
        private static final int CEILING = SiloCeiling.UHV - 2; // ZPM: danger 4
        private static final int WAIT = 160;

        @Override
        public String name() {
            return "icbm.complex.guards";
        }

        @Override
        public String description() {
            return "Silo guard and elite posts spawn their own mobs (Army Soldier and Commando; Commando and"
                + " Stormtrooper) at the stored ceiling's danger, within the M8 cap, with a fake player near; the guards"
                + " attack that player; a broken post spawns nothing more, also after its chunk is saved, unloaded and"
                + " reloaded, while the intact post comes back with its kind and ceiling (GGI-08-41..43).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!com.gregguns.core.module.ModuleState.isActive(com.gregguns.core.module.ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), com.gregguns.icbm.IcbmAvailability.offReason());
            }
            final World world = ctx.world();
            StructurePlan plan = plan(world);
            final int cx0 = plan.anchorX() + PAD_HALF + 8, cz0 = plan.anchorZ() + PAD_HALF + 8;
            final int y = GROUND + 1;
            final int gx = cx0 - 15, ex = cx0 + 15;
            final EnumDifficulty prior = world.difficultySetting;
            if (prior == EnumDifficulty.PEACEFUL) {
                world.difficultySetting = EnumDifficulty.NORMAL;
            }
            final ChunkProviderServer cps = (ChunkProviderServer) world.getChunkProvider();
            for (int cx = (cx0 - 48) >> 4; cx <= (cx0 + 48) >> 4; cx++) {
                for (int cz = (cz0 - 48) >> 4; cz <= (cz0 + 48) >> 4; cz++) {
                    cps.loadChunk(cx, cz);
                }
            }
            pad(world, cx0, cz0, false);
            final Drops drops = new Drops();
            MinecraftForge.EVENT_BUS.register(drops);
            final List<EntityLiving> npcs = new ArrayList<>();
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final Runnable cleanup = () -> {
                try {
                    killAll(npcs, drops);
                    for (Object o : world.getEntitiesWithinAABB(
                        NpcEntity.class,
                        AxisAlignedBB.getBoundingBox(cx0 - 60, 0, cz0 - 60, cx0 + 60, 255, cz0 + 60))) {
                        drops.track((Entity) o);
                        ((EntityLiving) o).setDead();
                    }
                    pad(world, cx0, cz0, true);
                } finally {
                    MinecraftForge.EVENT_BUS.unregister(drops);
                    world.difficultySetting = prior;
                    ctx.cleanup();
                }
            };
            try {
                world.setBlock(gx, y, cz0, HoleSpawnerRegistration.block(HoleKind.SILO_GUARD), 0, 3);
                world.setTileEntity(gx, y, cz0, new TileHoleSpawner(HoleKind.SILO_GUARD, CEILING));
                world.setBlock(ex, y, cz0, HoleSpawnerRegistration.block(HoleKind.SILO_ELITE), 0, 3);
                world.setTileEntity(ex, y, cz0, new TileHoleSpawner(HoleKind.SILO_ELITE, CEILING));
                TileHoleSpawner guardTile = (TileHoleSpawner) world.getTileEntity(gx, y, cz0);
                TileHoleSpawner eliteTile = (TileHoleSpawner) world.getTileEntity(ex, y, cz0);
                if (guardTile == null || eliteTile == null) {
                    cleanup.run();
                    return ScenarioResult.fail(name(), "the posts carry no tile");
                }
                final EntityPlayer player = fakePlayer(ctx, cx0 + 0.5, y, cz0 + 0.5);
                HoleSpawning.Placer placer = npc -> {
                    drops.track(npc);
                    npcs.add(npc);
                    ctx.spawn(npc);
                    return true;
                };
                int danger = RolledCeiling.danger(CEILING);
                List<EntityLiving> guards = new ArrayList<>();
                List<EntityLiving> elites = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    guards.addAll(guardTile.attempt(HoleSpawning.MARKER, placer));
                    elites.addAll(eliteTile.attempt(HoleSpawning.MARKER, placer));
                }
                out.check(!guards.isEmpty(), "the guard post placed nothing with a player 15 blocks away");
                out.check(!elites.isEmpty(), "the elite post placed nothing with a player 15 blocks away");
                out.check(guards.size() <= HoleCaps.MAX_NEARBY, "guard cap: " + guards.size());
                out.check(elites.size() <= HoleCaps.MAX_NEARBY, "elite cap: " + elites.size());
                checkMobs(out, "guard", guards, HoleKind.SILO_GUARD, danger);
                checkMobs(out, "elite", elites, HoleKind.SILO_ELITE, danger);
                out.note("guards " + guards.size() + ", elites " + elites.size() + ", danger floor " + danger);
                final int[] phase = { 0 };
                final int[] mark = { 0 };
                return ScenarioResult.pending(name(), TIMEOUT, ticks -> {
                    boolean finished = false;
                    try {
                        switch (phase[0]) {
                            case 0: {
                                boolean hit = player.getHealth() < player.getMaxHealth();
                                for (EntityLiving g : npcs) {
                                    if (g.getAttackTarget() == player) hit = true;
                                }
                                if (hit) {
                                    out.note("a guard went for the fake player after " + ticks + " ticks");
                                    phase[0] = 1;
                                } else if (ticks >= 400) {
                                    out.check(false, "no guard attacked the nearby fake player in 400 ticks");
                                    phase[0] = 1;
                                }
                                return null;
                            }
                            case 1: {
                                world.setBlockToAir(gx, y, cz0);
                                out.check(
                                    world.getTileEntity(gx, y, cz0) == null,
                                    "breaking the post removes its tile");
                                killAll(npcs, drops);
                                mark[0] = ticks;
                                phase[0] = 2;
                                return null;
                            }
                            case 2: {
                                if (ticks - mark[0] < WAIT) return null;
                                out.checkEquals(
                                    "army soldiers " + WAIT + " ticks after the guard post was broken",
                                    0,
                                    soldiers(world, gx, y, cz0));
                                out.check(
                                    HoleSpawning.countNearby(world, HoleKind.SILO_ELITE, ex, y, cz0) > 0,
                                    "the intact elite post keeps spawning, so the test can tell a dead post from a live one");
                                killAll(npcs, drops);
                                out.check(
                                    saveUnloadReload(world, cps, gx >> 4, cz0 >> 4),
                                    "the post's chunk really unloaded");
                                out.check(
                                    world.getBlock(gx, y, cz0) == Blocks.air,
                                    "the broken post is still air after the reload");
                                out.check(
                                    world.getTileEntity(gx, y, cz0) == null,
                                    "the broken post has no tile after the reload");
                                out.check(
                                    saveUnloadReload(world, cps, ex >> 4, cz0 >> 4),
                                    "the elite post's chunk really unloaded");
                                TileEntity et = world.getTileEntity(ex, y, cz0);
                                out.check(
                                    et instanceof TileHoleSpawner
                                        && ((TileHoleSpawner) et).kind() == HoleKind.SILO_ELITE
                                        && ((TileHoleSpawner) et).ceiling() == CEILING,
                                    "the intact post reloads with its kind and ceiling: " + et);
                                mark[0] = ticks;
                                phase[0] = 3;
                                return null;
                            }
                            default: {
                                if (ticks - mark[0] < WAIT) return null;
                                out.checkEquals(
                                    "army soldiers " + WAIT + " ticks after the reload",
                                    0,
                                    soldiers(world, gx, y, cz0));
                                finished = true;
                                return out.build();
                            }
                        }
                    } finally {
                        if (finished || ticks >= TIMEOUT) {
                            cleanup.run();
                        }
                    }
                });
            } catch (RuntimeException e) {
                cleanup.run();
                throw e;
            }
        }

        private static void checkMobs(ScenarioResult.Builder out, String what, List<EntityLiving> placed, HoleKind kind,
            int danger) {
            for (EntityLiving e : placed) {
                NpcMob mob = NpcMob.byName(EntityList.getEntityString(e));
                out.check(
                    mob != null && kind.mobs()
                        .contains(mob),
                    what + " post placed " + mob);
                if (e instanceof NpcEntity) {
                    out.check(
                        ((NpcEntity) e).danger() >= danger,
                        what + " danger " + ((NpcEntity) e).danger() + " < " + danger);
                }
            }
        }

        /** NPCs of the guard kind's own mob (Army Soldier, which the elite post never places) near a point. */
        private static int soldiers(World world, int x, int y, int z) {
            return NpcCounter.countInBox(
                world,
                Collections.singletonList(NpcMob.ARMY_SOLDIER.fullName()),
                x - 30,
                y - 6,
                z - 30,
                x + 30,
                y + 8,
                z + 30);
        }

        private static void killAll(List<EntityLiving> npcs, Drops drops) {
            for (EntityLiving e : npcs) {
                drops.track(e);
                e.setHealth(0.0F);
                e.onDeath(DamageSource.generic);
                e.setDead();
            }
            npcs.clear();
        }

        /** Saves the chunk to disk, unloads it for real and loads it back; false when it would not unload. */
        private static boolean saveUnloadReload(World world, ChunkProviderServer cps, int cx, int cz) {
            cps.saveChunks(true, null);
            cps.currentChunkLoader.saveExtraData();
            cps.unloadChunksIfNotNearSpawn(cx, cz);
            cps.unloadQueuedChunks();
            boolean gone = !cps.chunkExists(cx, cz);
            cps.loadChunk(cx, cz);
            return gone;
        }

        private static void pad(World world, int cx, int cz, boolean clear) {
            for (int x = cx - PAD_HALF; x <= cx + PAD_HALF; x++) {
                for (int z = cz - PAD_HALF; z <= cz + PAD_HALF; z++) {
                    for (int y = GROUND + 8; y >= GROUND - 1; y--) {
                        Block b = clear || y > GROUND ? Blocks.air : Blocks.stone;
                        if (world.getBlock(x, y, z) != b) {
                            world.setBlock(x, y, z, b, 0, 2);
                        }
                        if (y > GROUND && world.getTileEntity(x, y, z) != null) {
                            world.removeTileEntity(x, y, z);
                        }
                    }
                }
            }
        }

        /**
         * A fake player the tracker can serve (it needs a channel-less connection), tough enough to outlast the test.
         */
        private static EntityPlayer fakePlayer(ScenarioContext ctx, double x, double y, double z) {
            String name = "[GGSiloGuards]";
            FakePlayer p = new FakePlayer(
                (WorldServer) ctx.world(),
                new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
            new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
            p.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                .setBaseValue(1000.0);
            p.setHealth(1000.0F);
            p.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
            return ctx.spawn(p);
        }
    }
}
