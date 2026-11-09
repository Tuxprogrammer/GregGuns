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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
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
import com.gregguns.icbm.complex.gen.FurnitureFiller;
import com.gregguns.icbm.complex.gen.GtMachineHook;
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
import com.gregguns.worldgen.spawner.SpawnerKind;
import com.gregguns.worldgen.spawner.SpawnerNbt;
import com.gregguns.worldgen.spawner.SpawnerSettings;
import com.gregguns.worldgen.spawner.VanillaSpawners;
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

    /** The slab's ground height. */
    static final int GROUND = 63;
    private static final long SEED = 0x51105L;

    private ComplexLevelScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Levels());
        registry.register(new Guards());
        registry.register(new Armory());
        SalvageScenarios.registerAll(registry);
        ChestLootScenarios.registerAll(registry);
        RoomScenarios.registerAll(registry);
        FurnitureFillScenarios.registerAll(registry);
        PropFacingScenarios.registerAll(registry);
        DoorLightScenarios.registerAll(registry);
        VendingEggScenarios.registerAll(registry);
    }

    // --- shared rig (the complex slab; the layout scenarios keep their own copies private) ---------------------

    /** The plan of the fixed test cell. */
    static StructurePlan plan(World world) {
        return planAt(world, 3, 4);
    }

    /** @return the plan of a test cell, or null when the grid places none there */
    static StructurePlan planAt(World world, int cellX, int cellZ) {
        StructureGrid g = new StructureGrid(
            64,
            64,
            64,
            Collections.singletonMap(
                SizeClass.BIG,
                Collections.singletonList(new PoolEntry(ComplexWriter.ID, 1, 1, new Footprint(240, 12, 240)))));
        return g.planAt(SEED, world.provider.dimensionId, SizeClass.BIG, cellX, cellZ)
            .orElse(null);
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
        return new ComplexPlacer(
            new WorldBlocks(),
            new GtMachineHook(),
            new FurnitureFiller(),
            new ComplexPlacer.Settings() {

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

    // --- the armory scenario ----------------------------------------------------------------------------------

    /** Generates an operational complex and reads its armories back: display walls, frames, ammo chests. */
    private static final class Armory implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.armory";
        }

        @Override
        public String description() {
            return "Generates an operational complex and checks every armory: guns hang in item frames on short"
                + " interior partitions of the room's wall block (open air above them, clear of the outer walls),"
                + " none on the outer walls, and a chest stands right under each gun holding that gun's ammo.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructurePlan plan = plan(world);
            SiloChests.register();
            try {
                load(world, plan);
                slab(world, plan, false);
                ComplexPlacer placer = placer().forceLevel(ActivityLevel.OPERATIONAL)
                    .forceCeiling(SiloCeiling.EV);
                int[] r = generate(world, plan, placer, "gregguns_complex_armory");
                out.checkEquals("writes dropped outside a window", 0, r[0]);
                read(out, world, plan, placer);
            } finally {
                slab(world, plan, true);
            }
            return out.build();
        }

        private void read(ScenarioResult.Builder out, World world, StructurePlan plan, ComplexPlacer placer) {
            ComplexPlan cp = placer.rasterOf(plan)
                .plan();
            int ax = plan.anchorX(), az = plan.anchorZ(), floor = floor(world);
            List<Placement> ps = ComplexContents.build(cp, ActivityLevel.OPERATIONAL, SiloCeiling.EV, null);
            int armories = 0, guns = 0, wrongAmmo = 0, noChest = 0, noWall = 0, noGun = 0, tall = 0, outer = 0;
            for (com.gregguns.icbm.complex.Room room : cp.rooms()) {
                if (room.kind() != com.gregguns.icbm.complex.RoomKind.ARMORY) continue;
                armories++;
                com.gregguns.icbm.complex.Box b = room.bounds();
                // No frame on the outer walls: every frame in the room hangs on a block that is not on the perimeter.
                @SuppressWarnings("unchecked")
                List<?> inRoom = world.getEntitiesWithinAABB(
                    net.minecraft.entity.item.EntityItemFrame.class,
                    AxisAlignedBB
                        .getBoundingBox(ax + b.minX, floor, az + b.minZ, ax + b.maxX, floor + b.sizeY(), az + b.maxZ));
                for (Object o : inRoom) {
                    net.minecraft.entity.item.EntityItemFrame f = (net.minecraft.entity.item.EntityItemFrame) o;
                    int[] dx = { 0, -1, 0, 1 }, dz = { 1, 0, -1, 0 };
                    // The block it hangs on: one step behind the cell it floats in.
                    int wx = (int) Math.floor(f.posX) - dx[f.hangingDirection] - ax,
                        wz = (int) Math.floor(f.posZ) - dz[f.hangingDirection] - az;
                    if (wx <= b.minX + 1 || wx >= b.maxX - 2 || wz <= b.minZ + 1 || wz >= b.maxZ - 2) outer++;
                }
                for (Placement p : ps) {
                    if (p.kind() != Kind.FRAME_WALL || p.x() <= b.minX
                        || p.x() >= b.maxX - 1
                        || p.z() <= b.minZ
                        || p.z() >= b.maxZ - 1) {
                        continue;
                    }
                    guns++;
                    int x = ax + p.x(), z = az + p.z(), y = floor + p.y();
                    int bx = p.facing() == 1 ? 1 : p.facing() == 3 ? -1 : 0,
                        bz = p.facing() == 0 ? -1 : p.facing() == 2 ? 1 : 0;
                    if (w(world, x + bx, y, z + bz) == Blocks.air) noWall++;
                    if (world.getBlock(x + bx, floor + 3, z + bz) != Blocks.air) tall++;
                    @SuppressWarnings("unchecked")
                    List<?> es = world.getEntitiesWithinAABB(
                        net.minecraft.entity.item.EntityItemFrame.class,
                        AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1));
                    ItemStack gun = es.isEmpty() ? null
                        : ((net.minecraft.entity.item.EntityItemFrame) es.get(0)).getDisplayedItem();
                    if (gun == null) {
                        noGun++;
                        continue;
                    }
                    TileEntity t = world.getTileEntity(x, y - 1, z);
                    if (!(t instanceof TileEntityChest)) {
                        noChest++;
                        continue;
                    }
                    String gunName = net.minecraft.item.Item.itemRegistry.getNameForObject(gun.getItem())
                        .toString();
                    List<String> allowed = ArmoryDisplay.itemsFor(gunName);
                    boolean any = false;
                    boolean energy = allowed.isEmpty(); // an energy gun's chest draws the armory table
                    TileEntityChest chest = (TileEntityChest) t;
                    for (int i = 0; i < chest.getSizeInventory(); i++) {
                        ItemStack s = chest.getStackInSlot(i);
                        if (s == null) continue;
                        any = true;
                        String n = net.minecraft.item.Item.itemRegistry.getNameForObject(s.getItem())
                            .toString();
                        if (!energy && !allowed.contains(n.substring(n.indexOf(':') + 1))) {
                            if (wrongAmmo++ < 3)
                                out.note("gun " + gunName + " chest holds " + n + ", allowed " + allowed);
                        }
                    }
                    if (!any) noChest++;
                }
            }
            out.note("armories " + armories + ", display guns " + guns);
            out.check(armories > 0, "the complex has an armory");
            out.check(guns >= 1, "each armory shows its guns: " + guns);
            out.checkEquals("frames on an outer wall", 0, outer);
            out.checkEquals("frames with no wall behind", 0, noWall);
            out.checkEquals("partitions that reach the ceiling", 0, tall);
            out.checkEquals("frames holding no gun", 0, noGun);
            out.checkEquals("guns with no ammo chest under them", 0, noChest);
            out.checkEquals("ammo stacks that do not fit the gun above", 0, wrongAmmo);
        }

        private static Block w(World world, int x, int y, int z) {
            return world.getBlock(x, y, z);
        }
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
        /** Lowest block light on an open floor cell of any section (15 when nothing was read). */
        int floorMinLight = 15;
        /** Every open floor cell under light 8, as "light at section id (kind) local x,z", in section order. */
        final List<String> darkCells = new ArrayList<>();
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
        /** Machine placements that are not husks: the working machines a player could take (D284). */
        int machinesWorking;
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
                    long fixturesBefore = FixtureWriter.spentNanos();
                    int[] r = generate(world, plan, placer, "gregguns_complex_levels");
                    long fixturesMs = (FixtureWriter.spentNanos() - fixturesBefore) / 1_000_000L;
                    out.checkEquals(tag + ": writes dropped outside a window", 0, r[0]);
                    out.checkEquals(tag + ": PopulateGuard violations", 0, r[1]);
                    if (r[2] > 0) {
                        out.note(
                            String.format(
                                java.util.Locale.ROOT,
                                "%s cost with contents: %d windows, %d blocks, %.1f ms mean and %.1f ms most per"
                                    + " window, %.0f ms in all (blast doors and service runs %d ms)",
                                tag,
                                r[2],
                                r[3],
                                r[4] / 1000.0 / r[2],
                                r[5] / 1000.0,
                                r[4] / 1000.0,
                                fixturesMs));
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
                            + ", working machines "
                            + t.machinesWorking
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
            out.check(ab.spawners > 0, "an abandoned complex still holds some spawners (D326): " + ab.spawners);
            out.checkEquals("abandoned elite spawners", 0, ab.elite);
            out.check(
                ab.machinesWorking * 4 <= op.machinesWorking && ab.chests <= op.chests,
                "an abandoned complex has far fewer working machines (D284) and no more chests (D315): machines "
                    + ab.machinesWorking
                    + " vs "
                    + op.machinesWorking
                    + ", chests "
                    + ab.chests
                    + " vs "
                    + op.chests);
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
                op.darkCells.isEmpty(),
                "an operational complex reads block light 8 or more on every open floor cell of every hall and room: "
                    + op.darkCells.size()
                    + " dark (lowest "
                    + op.floorMinLight
                    + ")"
                    + (op.darkCells.isEmpty() ? ""
                        : ": " + String.join("; ", op.darkCells.subList(0, Math.min(12, op.darkCells.size())))
                            + (op.darkCells.size() > 12 ? "; ..." : "")));
            out.check(ab.floodMarks > 0 ? ab.floodWet == ab.floodMarks : true, "abandoned pump room is flooded");
            out.checkEquals("operational flooding", 0, op.floodWet);
        }

        /**
         * The hub's "Silo Complex / Class X" sign and, one under it, "Activity Level:" over the level's code name
         * (D301), read from the world.
         */
        private void hallSigns(ScenarioResult.Builder out, String tag, World world,
            com.gregguns.icbm.complex.ComplexRaster raster, int ax, int floor, int az, ActivityLevel level,
            int ceiling) {
            com.gregguns.icbm.complex.Mark hall = com.gregguns.icbm.complex.gen.ComplexSigns.hallSign(raster.plan());
            int[] at = hall == null ? null : com.gregguns.icbm.complex.gen.ComplexSigns.resolve(raster, hall);
            out.check(at != null, tag + ": the hub has its tier sign");
            if (at == null) return;
            net.minecraft.tileentity.TileEntity t = world.getTileEntity(ax + at[0], floor + at[1], az + at[2]);
            out.check(
                t instanceof net.minecraft.tileentity.TileEntitySign
                    && "Silo Complex".equals(((net.minecraft.tileentity.TileEntitySign) t).signText[0])
                    && ("Class " + SiloCeiling.name(ceiling))
                        .equals(((net.minecraft.tileentity.TileEntitySign) t).signText[1]),
                tag + ": the tier sign reads Silo Complex, Class " + SiloCeiling.name(ceiling));
            String want = level == ActivityLevel.ABANDONED ? "Alpha"
                : level == ActivityLevel.PARTLY_ABANDONED ? "Bravo" : "Charlie";
            net.minecraft.tileentity.TileEntity a = world.getTileEntity(ax + at[0], floor + at[1] - 1, az + at[2]);
            out.check(
                a instanceof net.minecraft.tileentity.TileEntitySign
                    && world.getBlockMetadata(ax + at[0], floor + at[1] - 1, az + at[2]) == at[3]
                    && "Activity Level:".equals(((net.minecraft.tileentity.TileEntitySign) a).signText[0])
                    && want.equals(((net.minecraft.tileentity.TileEntitySign) a).signText[1]),
                tag + ": the sign under it reads Activity Level: " + want);
        }

        private Tally read(ScenarioResult.Builder out, String tag, World world, StructurePlan plan,
            ComplexPlacer placer, ActivityLevel level, int ceiling) {
            Tally t = new Tally();
            ComplexPlan cp = placer.rasterOf(plan)
                .plan();
            int ax = plan.anchorX(), az = plan.anchorZ(), floor = floor(world);
            hallSigns(out, tag, world, placer.rasterOf(plan), ax, floor, az, level, ceiling);
            int[] other = { plan.anchorX() + 64 * 16, plan.anchorZ() };
            List<Placement> placements = ComplexContents.build(cp, level, ceiling, other);
            // Last writer wins per cell; collapses are boxes, tallied on their own.
            Map<Long, Placement> last = new HashMap<>();
            List<Placement> collapses = new ArrayList<>();
            for (Placement p : placements) {
                if (p.kind() == Kind.COLLAPSE) {
                    collapses.add(p);
                } else if (p.kind() != Kind.STOCK) { // a piece's stock shares its cell (D323)
                    last.put(key(p.x(), p.y(), p.z()), p);
                }
            }
            // The magazine's casing racks are furniture (CASING_FRAME pieces, D308): GT frame boxes, never a warhead
            // block (D17); a collapsed or worn rack counts only where its frame still stands.
            ItemStack casing = ComplexApplier.casingFrame();
            if (casing != null) {
                Block cf = Block.getBlockFromItem(casing.getItem());
                for (com.gregguns.icbm.complex.Room room : cp.rooms()) {
                    if (room.kind() != com.gregguns.icbm.complex.RoomKind.WARHEAD_MAGAZINE) continue;
                    for (com.gregguns.icbm.complex.Mark m : room.marks()) {
                        if (m.kind() != com.gregguns.icbm.complex.MarkKind.FURNITURE
                            || com.gregguns.icbm.complex.FurniturePiece.of(m.key())
                                != com.gregguns.icbm.complex.FurniturePiece.CASING_FRAME)
                            continue;
                        int x = ax + m.x(), y = floor + m.y(), z = az + m.z();
                        Block b = world.getBlock(x, y, z);
                        if (b == cf && world.getBlockMetadata(x, y, z) == casing.getItemDamage()
                            && !(b instanceof BlockWarhead)) t.casings++;
                    }
                }
            }
            SpawnerCheck spawners = new SpawnerCheck();
            com.gregguns.icbm.complex.gen.Styles styles = placer.stylesOf(plan);
            for (Placement p : last.values()) {
                int x = ax + p.x(), y = floor + p.y(), z = az + p.z();
                Block b = world.getBlock(x, y, z);
                switch (p.kind()) {
                    case SPAWNER:
                        t.spawners++;
                        if (p.spawner() != null && p.spawner()
                            .isEliteSiloPost()) t.elite++;
                        else t.guard++;
                        NBTTagCompound st = VanillaSpawners.read(world.getTileEntity(x, y, z));
                        if (b != Blocks.mob_spawner || st == null
                            || !SpawnerNbt.isMarked(st)
                            || !SpawnerNbt.entities(st)
                                .equals(
                                    p.spawner()
                                        .mobNames())
                            || SpawnerNbt.minDanger(st) != RolledCeiling.danger(ceiling)) {
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
            out.checkEquals(tag + ": spawners with the wrong mobs or danger floor", 0, t.badTiles);
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
            spawnerSpread(out, tag, world, cp, ax, floor, az, level, t);
            return t;
        }

        /**
         * D326: counts the marked spawner blocks standing in each room (the hub included) of the generated complex and
         * checks the spread: no room holds more than two, a staffed complex has one in every room that offers a spot,
         * a ruin has them in a minority, and the rooms with spawners reach beyond the first ring around the hub.
         */
        private void spawnerSpread(ScenarioResult.Builder out, String tag, World world, ComplexPlan cp, int ax,
            int floor, int az, ActivityLevel level, Tally t) {
            List<com.gregguns.icbm.complex.Room> rooms = new ArrayList<>(cp.rooms());
            rooms.add(cp.hall());
            int[] hist = new int[3];
            int tooMany = 0, far = 0, staffedBare = 0;
            com.gregguns.icbm.complex.Box hub = cp.hall()
                .bounds();
            for (com.gregguns.icbm.complex.Room r : rooms) {
                com.gregguns.icbm.complex.Box b = r.bounds();
                int n = 0;
                for (int x = b.minX; x < b.maxX; x++) {
                    for (int z = b.minZ; z < b.maxZ; z++) {
                        if (world.getBlock(ax + x, floor + 1, az + z) != Blocks.mob_spawner) continue;
                        NBTTagCompound st = VanillaSpawners.read(world.getTileEntity(ax + x, floor + 1, az + z));
                        if (st != null && SpawnerNbt.isMarked(st)) n++;
                    }
                }
                if (n > 2) {
                    tooMany++;
                }
                hist[Math.min(n, 2)]++;
                int dist = Math.abs(b.centerX() - hub.centerX()) + Math.abs(b.centerZ() - hub.centerZ());
                if (n > 0 && dist >= 60) {
                    far++;
                }
                boolean offers = false;
                for (com.gregguns.icbm.complex.Mark m : r.marks()) {
                    offers |= m.kind() == com.gregguns.icbm.complex.MarkKind.SPAWNER;
                }
                if (level == ActivityLevel.OPERATIONAL && n == 0 && offers) {
                    staffedBare++;
                }
            }
            out.note(
                tag + ": "
                    + rooms.size()
                    + " rooms with 0/1/2 spawners "
                    + hist[0]
                    + "/"
                    + hist[1]
                    + "/"
                    + hist[2]
                    + ", "
                    + far
                    + " with spawners 60 or more cells from the hub");
            out.checkEquals(tag + ": rooms holding more than two spawners", 0, tooMany);
            out.checkEquals(tag + ": staffed rooms with a spot and no spawner", 0, staffedBare);
            out.checkEquals(tag + ": every placed spawner stands inside a room", t.spawners, hist[1] + 2 * hist[2]);
            if (level != ActivityLevel.ABANDONED) {
                out.check(far > 0, tag + ": the outer rooms hold spawners, not only the middle");
            }
            if (level == ActivityLevel.ABANDONED) {
                out.check(hist[0] > hist[1] + hist[2], tag + ": most rooms of a ruin hold none");
            }
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

        /** Counts every GregGuns-marked mob spawner in the complex, to catch strays the placements do not list. */
        private static final class SpawnerCheck {

            int last;

            int totalIn(World world, int ax, int az, int floor) {
                int n = 0;
                for (int x = ax; x < ax + 240; x++) {
                    for (int z = az; z < az + 240; z++) {
                        for (int y = floor; y < floor + ComplexRaster.LEVELS; y++) {
                            if (world.getBlock(x, y, z) != Blocks.mob_spawner) continue;
                            NBTTagCompound t = VanillaSpawners.read(world.getTileEntity(x, y, z));
                            if (t != null && SpawnerNbt.isMarked(t)) n++;
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
            return SiloChests.itemId(s);
        }

        /** @return stacks in the chest that are neither on the chest's table at the ceiling nor one of its bonuses */
        private static int outsideTable(IInventory inv, Placement p, int ceiling) {
            Set<String> allowed = new HashSet<>();
            if (com.gregguns.icbm.complex.LootKeys.ARMORY_AMMO.equals(p.key())) {
                // An armory display chest holds its gun's ammo (checked by icbm.complex.armory), else the armory table.
                for (String item : ArmoryDisplay.everyItem()) {
                    ItemStack s = LootCategories.stack("gregguns:" + item, 1);
                    if (s != null) allowed.add(id(s));
                }
            }
            // Lines, junk, spoil (D317), from the table of the ceiling held to loot.maxTier (D364).
            allowed.addAll(SiloChests.allowedIds(p.key(), ComplexContents.lootTable(ceiling)));
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
            // Every dark cell is listed, so one fix never hides another. A sunk pool's water is no floor (D321).
            java.util.Set<Long> pool = new java.util.HashSet<>();
            for (Mark m : all) {
                if (m.kind() == MarkKind.FURNITURE && m.y() == 0
                    && com.gregguns.icbm.complex.FurniturePiece.of(m.key())
                        == com.gregguns.icbm.complex.FurniturePiece.POOL_WATER) {
                    pool.add(((long) m.x() << 32) ^ (m.z() & 0xffffffffL));
                }
            }
            for (int id = 0; id < cp.sectionCount(); id++) {
                com.gregguns.icbm.complex.Box b = cp.sectionBox(id);
                for (int x = b.minX + 1; x <= b.maxX - 2; x++) {
                    for (int z = b.minZ + 1; z <= b.maxZ - 2; z++) {
                        if (!world.isAirBlock(ax + x, floor + 1, az + z)) continue;
                        if (pool.contains(((long) x << 32) ^ (z & 0xffffffffL))) continue;
                        int l = world
                            .getSavedLightValue(net.minecraft.world.EnumSkyBlock.Block, ax + x, floor + 1, az + z);
                        t.floorMinLight = Math.min(t.floorMinLight, l);
                        if (l < 8) {
                            t.darkCells
                                .add(l + " at section " + id + " (" + sectionName(cp, id) + ") local " + x + "," + z);
                        }
                    }
                }
            }
        }

        /** @return a section's kind for a report: the hub, a room's kind, or a major hall or minor hallway */
        private static String sectionName(ComplexPlan cp, int id) {
            Object o = cp.section(id);
            if (o == cp.hall()) return "hub";
            if (o instanceof com.gregguns.icbm.complex.Room) return ((com.gregguns.icbm.complex.Room) o).kind()
                .name();
            return ((com.gregguns.icbm.complex.Tunnel) o).major() ? "major hall" : "minor hallway";
        }

        private void machine(Tally t, World world, Placement p, int x, int y, int z) {
            MachineRole role = p.role();
            t.machinesWanted++;
            if (!p.dead()) t.machinesWorking++;
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
                MachinePicker.Pick pick = MachinePicker.pick(role, Math.max(1, p.tier()), p.dead(), p.variant());
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
            return "Silo guard and elite posts are vanilla mob spawners that, on a lit pad, spawn their own mobs (Army"
                + " Soldier and Commando; Commando and Stormtrooper) at the ceiling's danger or above, armed, at most 3"
                + " a cycle and 5 of a type, with a fake player near; the guards attack that player; a broken post"
                + " spawns nothing more, also after its chunk is saved, unloaded and reloaded, while the intact post"
                + " comes back with its mobs and danger floor (GGI-08-41..43).";
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
                // The pad is open to the sky, and glowstone in the floor beside each post makes it brighter still: the
                // vanilla monster light checks would refuse every spot here, so any guard proves the spawner rule.
                for (int[] d : new int[][] { { -2, 0 }, { 2, 0 }, { 0, -2 }, { 0, 2 } }) {
                    world.setBlock(gx + d[0], GROUND, cz0 + d[1], Blocks.glowstone, 0, 3);
                    world.setBlock(ex + d[0], GROUND, cz0 + d[1], Blocks.glowstone, 0, 3);
                }
                boolean placed = VanillaSpawners.placeInWorld(world, SpawnerKind.SILO_GUARD, gx, y, cz0, CEILING)
                    && VanillaSpawners.placeInWorld(world, SpawnerKind.SILO_ELITE, ex, y, cz0, CEILING);
                TileEntity guardTile = world.getTileEntity(gx, y, cz0);
                TileEntity eliteTile = world.getTileEntity(ex, y, cz0);
                if (!placed || !(guardTile instanceof TileEntityMobSpawner)
                    || !(eliteTile instanceof TileEntityMobSpawner)) {
                    cleanup.run();
                    return ScenarioResult.fail(name(), "the posts are not vanilla mob spawners");
                }
                out.check(
                    world.getBlockLightValue(gx, y, cz0 + 1) >= 12,
                    "the guard post is lit: block light " + world.getBlockLightValue(gx, y, cz0 + 1));
                final EntityPlayer player = fakePlayer(ctx, cx0 + 0.5, y, cz0 + 0.5);
                int danger = RolledCeiling.danger(CEILING);
                // Run the vanilla spawner logic directly: the first cycle after 40 ticks, then every 100 to 120.
                int worstStep = 0;
                for (int i = 0; i < 400; i++) {
                    int before = near(world, SpawnerKind.SILO_GUARD, gx, y, cz0).size();
                    guardTile.updateEntity();
                    worstStep = Math.max(worstStep, near(world, SpawnerKind.SILO_GUARD, gx, y, cz0).size() - before);
                    before = near(world, SpawnerKind.SILO_ELITE, ex, y, cz0).size();
                    eliteTile.updateEntity();
                    worstStep = Math.max(worstStep, near(world, SpawnerKind.SILO_ELITE, ex, y, cz0).size() - before);
                }
                List<EntityLiving> guards = near(world, SpawnerKind.SILO_GUARD, gx, y, cz0);
                List<EntityLiving> elites = near(world, SpawnerKind.SILO_ELITE, ex, y, cz0);
                for (EntityLiving e : guards) {
                    drops.track(e);
                    npcs.add(e);
                }
                for (EntityLiving e : elites) {
                    drops.track(e);
                    npcs.add(e);
                }
                SpawnerSettings s = SpawnerKind.SILO_GUARD.settings();
                out.check(!guards.isEmpty(), "the lit guard post spawned nothing with a player 15 blocks away");
                out.check(!elites.isEmpty(), "the lit elite post spawned nothing with a player 15 blocks away");
                out.check(worstStep <= s.spawnCount(), "one cycle placed " + worstStep + ", more than 3");
                checkMobs(out, "guard", guards, SpawnerKind.SILO_GUARD, danger, s.maxNearby());
                checkMobs(out, "elite", elites, SpawnerKind.SILO_ELITE, danger, s.maxNearby());
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
                                killAround(world, cx0, cz0, drops);
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
                                    !near(world, SpawnerKind.SILO_ELITE, ex, y, cz0).isEmpty(),
                                    "the intact elite post keeps spawning, so the test can tell a dead post from a live one");
                                killAll(npcs, drops);
                                killAround(world, cx0, cz0, drops);
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
                                NBTTagCompound en = VanillaSpawners.read(et);
                                out.check(
                                    en != null && SpawnerNbt.isMarked(en)
                                        && SpawnerNbt.entities(en)
                                            .equals(SpawnerKind.SILO_ELITE.mobNames())
                                        && SpawnerNbt.minDanger(en) == RolledCeiling.danger(CEILING),
                                    "the intact post reloads with its mobs and danger floor: " + en);
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

        /**
         * Every NPC is one of the post's mobs, starts at the danger floor or above (its equipment was rolled with it),
         * no mob type passes the cap, and at least one holds a weapon.
         */
        private static void checkMobs(ScenarioResult.Builder out, String what, List<EntityLiving> placed,
            SpawnerKind kind, int danger, int cap) {
            Map<NpcMob, Integer> perType = new HashMap<>();
            int armed = 0;
            for (EntityLiving e : placed) {
                NpcMob mob = NpcMob.byName(EntityList.getEntityString(e));
                out.check(
                    mob != null && kind.mobs()
                        .contains(mob),
                    what + " post placed " + mob);
                perType.merge(mob, 1, Integer::sum);
                if (e instanceof NpcEntity) {
                    out.check(
                        ((NpcEntity) e).danger() >= danger,
                        what + " danger " + ((NpcEntity) e).danger() + " < " + danger);
                }
                if (e.getHeldItem() != null) armed++;
            }
            for (Map.Entry<NpcMob, Integer> c : perType.entrySet()) {
                out.check(c.getValue() <= cap, what + " post: " + c.getValue() + " " + c.getKey() + ", cap " + cap);
            }
            out.check(placed.isEmpty() || armed > 0, what + " post: no NPC holds a weapon");
        }

        /** Living NPCs of the kind's mobs in the box a vanilla spawner at (x, y, z) counts. */
        private static List<EntityLiving> near(World world, SpawnerKind kind, int x, int y, int z) {
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

        /** Kills every NPC around the pad, drops cancelled, including ones the posts spawned on their own tick. */
        private static void killAround(World world, int cx0, int cz0, Drops drops) {
            for (Object o : world.getEntitiesWithinAABB(
                NpcEntity.class,
                AxisAlignedBB.getBoundingBox(cx0 - 60, 0, cz0 - 60, cx0 + 60, 255, cz0 + 60))) {
                drops.track((Entity) o);
                ((EntityLiving) o).setDead();
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
            com.gregguns.core.testing.QuietConnection.attach(p);
            p.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                .setBaseValue(1000.0);
            p.setHealth(1000.0F);
            p.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
            return ctx.spawn(p);
        }
    }
}
