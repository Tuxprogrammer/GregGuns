package com.gregguns.icbm.complex;

import java.util.Collections;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary.Type;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.gen.ComplexPlacer;
import com.gregguns.icbm.complex.gen.GtMachineHook;
import com.gregguns.icbm.complex.gen.MarkFiller;
import com.gregguns.icbm.complex.gen.PaletteStyles;
import com.gregguns.icbm.complex.gen.WorldBlocks;
import com.gregguns.worldgen.ClippingSink;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.Footprint;
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

/**
 * The silo complex scenarios: a forced complex on a prepared flat slab, built one populate window at a time. Dev
 * environment only; needs {@code worldgen} and {@code icbm} in {@code GG_MODULES}. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md; exit gate boxes 1 and the D128 layout check
public final class ComplexScenarios {

    private static final int GROUND = 63;
    private static final long SEED = 0x51105L;

    private ComplexScenarios() {}

    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Places());
        registry.register(new Skips());
        ComplexDimScenarios.registerAll(registry);
        ComplexStartScenarios.registerAll(registry);
        ComplexShellScenarios.registerAll(registry);
        com.gregguns.icbm.complex.gen.LocateScenarios.registerAll(registry);
    }

    private static StructurePlan plan(World world) {
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

    private static void load(World world, StructurePlan p) {
        ComplexArena.load(world, p);
    }

    /** Stone up to the ground with a grass top, air above to the chunk's top (ComplexArena). */
    private static void slab(World world, StructurePlan p, boolean clear) {
        ComplexArena.slab(world, p, clear, (x, z) -> GROUND, 0, GROUND);
    }

    /** As above with a ground height per column and an optional water depth over it. */
    private static void slab(World world, StructurePlan p, boolean clear, java.util.function.IntBinaryOperator ground,
        int water) {
        ComplexArena.slab(world, p, clear, ground, water, GROUND);
    }

    private static long snapshot(World world, StructurePlan p) {
        long h = 1125899906842597L;
        for (int x = p.anchorX(); x < p.anchorX() + 240; x++) {
            for (int z = p.anchorZ(); z < p.anchorZ() + 240; z++) {
                for (int y = ComplexArena.floor(world, GROUND); y <= GROUND + 2; y++) {
                    h = 31 * h + (Block.getIdFromBlock(world.getBlock(x, y, z)) << 4 | world.getBlockMetadata(x, y, z));
                }
            }
        }
        return h;
    }

    /**
     * The owner's layout (D299): a 3x3 clear glass window around the controller and a hatch column on each side of it,
     * two out from the controller: the Input Bus over the Energy Hatch on the viewer's right with the silo's own Launch
     * Control on top, the Input Hatch over the Maintenance Hatch on the left with concrete on top, and a concrete band
     * over the five-wide face. The left and right of the viewer in front of the controller are computed from the facing
     * here, not from the shape's columns.
     */
    private static void siloHatchLayout(World world, SiloSlot s, int ax, int az, int floor,
        ScenarioResult.Builder out) {
        // A viewer in front of the controller faces opposite to it: facing 0 (north) looks south, so their right is
        // west; 1 (east) looks west, right is north; 2 looks north, right is east; 3 looks east, right is south.
        int rx = new int[] { -1, 0, 1, 0 }[s.facing()];
        int rz = new int[] { 0, -1, 0, 1 }[s.facing()];
        int cx = ax + s.controllerX(), cz = az + s.controllerZ();
        int y = floor + 2;
        boolean energyThere = s.missing() != MissingPart.ENERGY_HATCH;
        Block conc = gregtech.api.GregTechAPI.sBlockConcretes;
        out.check(hatchIs(world, cx + 2 * rx, y, cz + 2 * rz, 'B'), "Input Bus in the right hatch column");
        out.check(
            energyThere ? hatchIs(world, cx + 2 * rx, y - 1, cz + 2 * rz, 'E')
                : world.getBlock(cx + 2 * rx, y - 1, cz + 2 * rz) == conc,
            "Energy Hatch under the Input Bus, or concrete where it is the missing part (" + s.missing() + ")");
        out.check(hatchIs(world, cx - 2 * rx, y, cz - 2 * rz, 'H'), "Input Hatch in the left hatch column");
        out.check(hatchIs(world, cx - 2 * rx, y - 1, cz - 2 * rz, 'M'), "Maintenance Hatch under the Input Hatch");
        TileEntity mt = world.getTileEntity(cx - 2 * rx, y - 1, cz - 2 * rz);
        out.check(
            mt instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity
                && ((gregtech.api.interfaces.tileentity.IGregTechTileEntity) mt).isActive(),
            "the Maintenance Hatch is switched on as a placed one is (no duct-tape cross on its face)");
        out.check(hatchIs(world, cx + 2 * rx, y + 1, cz + 2 * rz, 'L'), "the Launch Control tops the right column");
        out.check(world.getBlock(cx - 2 * rx, y + 1, cz - 2 * rz) == conc, "concrete tops the left column");
        for (int a = -1; a <= 1; a++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (a == 0 && dy == 0) continue;
                int gx = cx + a * rx, gz = cz + a * rz, gy = y + dy;
                out.check(
                    com.gregguns.icbm.silo.SiloGlass
                        .accepts(world.getBlock(gx, gy, gz), world.getBlockMetadata(gx, gy, gz)),
                    "clear glass at window cell (" + a + ", " + dy + ") of a silo facing " + s.facing());
            }
        }
        // The walkway in front of the face is open across all five columns, from the floor to the band over the
        // window, and the band itself is the silo's concrete.
        int fx = s.frontX() - s.controllerX(), fz = s.frontZ() - s.controllerZ();
        for (int a = -2; a <= 2; a++) {
            out.check(
                world.getBlock(cx + a * rx, y + 2, cz + a * rz) == conc,
                "concrete over the window at column " + a + " of a silo facing " + s.facing());
            for (int dy = -1; dy <= 2; dy++) {
                int wx = cx + a * rx + fx, wz = cz + a * rz + fz;
                boolean sign = a == 0 && dy == 2;
                out.check(
                    sign || world.isAirBlock(wx, y + dy, wz),
                    "the walkway is open at column " + a + ", height " + (dy + 2) + " of a silo facing " + s.facing());
            }
        }
    }

    private static boolean hatchIs(World world, int x, int y, int z, char kind) {
        TileEntity t = world.getTileEntity(x, y, z);
        if (!(t instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity)) return false;
        Object m = ((gregtech.api.interfaces.tileentity.IGregTechTileEntity) t).getMetaTileEntity();
        switch (kind) {
            case 'E':
                return m instanceof gregtech.api.metatileentity.implementations.MTEHatchEnergy;
            case 'B':
                return m instanceof gregtech.api.metatileentity.implementations.MTEHatchInputBus;
            case 'H':
                return m instanceof gregtech.api.metatileentity.implementations.MTEHatchInput;
            case 'L':
                return m instanceof com.gregguns.icbm.control.MTELaunchControl;
            default:
                return m instanceof gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
        }
    }

    /** Our facing index (0 north, 1 east, 2 south, 3 west) as a ForgeDirection ordinal. */
    private static final int[] FACING_TO_FORGE = { 2, 5, 3, 4 };

    private static final class Places implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.places";
        }

        @Override
        public String description() {
            return "Forces a silo complex (fixed seed) onto a flat slab one populate window at a time: placed, no write"
                + " outside a window, no PopulateGuard violation; every silo has its pad, open bore and shaft to the"
                + " surface, exactly one part missing and forms once the part is added; the escape ladder reaches a"
                + " trapdoor; the opposite window order gives an identical world (D128, GGS-11-08, GGS-11-09).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructurePlan plan = plan(world);
            ComplexPlacer placer = new ComplexPlacer(
                new WorldBlocks(),
                new GtMachineHook(),
                MarkFiller.NONE,
                new ComplexPlacer.Settings() {

                    public boolean dimensionEnabled(String n) {
                        return true;
                    }

                    public int chancePercent() {
                        return 100;
                    }

                    public java.util.Set<Type> overworldBiomes() {
                        return Collections.emptySet();
                    }
                }).withContents(false) // order identity is the shell's; the contents pass has falling rubble and
                                       // flooding
                    .withStyles(PaletteStyles.FACTORY);
            try {
                load(world, plan);
                long first = 0;
                for (int pass = 0; pass < 2; pass++) {
                    slab(world, plan, false);
                    placer.enabledIn(world);
                    StructureState state = new StructureState("gregguns_complex_test");
                    StructurePopulator populator = com.gregguns.worldgen.PlacementHarnessAccess.populator();
                    int rejected = 0;
                    int accepted = 0;
                    int before = PopulateGuard.violationCount();
                    java.util.List<int[]> windows = com.gregguns.worldgen.PlacementHarnessAccess
                        .windowsOf(plan, pass == 1);
                    int stamped = 0;
                    // Generation cost (D236): wall time and blocks per populate window that holds part of the complex.
                    long totalNanos = 0, maxNanos = 0;
                    int busy = 0, maxBlocks = 0;
                    for (int[] w : windows) {
                        PopulateWindow window = new PopulateWindow(w[0], w[1]);
                        ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
                        long t0 = System.nanoTime();
                        int acceptedBefore = sink.acceptedWrites();
                        try (PopulateGuard.Scope ignored = PopulateGuard.enter(w[0], w[1])) {
                            stamped += populator.populateComplex(
                                plan,
                                placer,
                                window,
                                sink,
                                new WorldColumnReader(world),
                                (x, z) -> Collections.singleton(Type.PLAINS),
                                state);
                        }
                        long dt = System.nanoTime() - t0;
                        int wrote = sink.acceptedWrites() - acceptedBefore;
                        if (wrote > 0) {
                            busy++;
                            totalNanos += dt;
                            maxNanos = Math.max(maxNanos, dt);
                            maxBlocks = Math.max(maxBlocks, wrote);
                        }
                        rejected += sink.rejectedWrites();
                        accepted += sink.acceptedWrites();
                    }
                    if (busy > 0) {
                        out.note(
                            String.format(
                                java.util.Locale.ROOT,
                                "pass %d cost: %d windows wrote %d blocks (mean %d, most %d per window), %.1f ms mean,"
                                    + " %.1f ms most per window, %.0f ms in all (shell only, first pass includes the"
                                    + " plan and raster)",
                                pass,
                                busy,
                                accepted,
                                accepted / busy,
                                maxBlocks,
                                totalNanos / 1e6 / busy,
                                maxNanos / 1e6,
                                totalNanos / 1e6));
                    }
                    out.check(
                        state.get(com.gregguns.worldgen.PlanKey.of(plan)) != null
                            && state.get(com.gregguns.worldgen.PlanKey.of(plan))
                                .isPlaced(),
                        "pass " + pass + ": placed");
                    out.checkEquals("pass " + pass + ": writes dropped outside a window", 0, rejected);
                    out.checkEquals(
                        "pass " + pass + ": PopulateGuard violations",
                        0,
                        PopulateGuard.violationCount() - before);
                    out.check(accepted > 20_000, "pass " + pass + ": many blocks written: " + accepted);
                    long snap = snapshot(world, plan);
                    if (pass == 0) {
                        first = snap;
                        checkContent(out, world, plan, placer);
                    } else {
                        out.check(snap == first, "the reverse window order should give an identical world");
                    }
                }
            } finally {
                slab(world, plan, true);
            }
            return out.build();
        }

        private void checkContent(ScenarioResult.Builder out, World world, StructurePlan plan, ComplexPlacer placer) {
            ComplexPlan cp = placer.rasterOf(plan)
                .plan();
            int floor = ComplexArena.floor(world, GROUND);
            out.note("level floor y " + floor + " under ground " + GROUND);
            out.check(
                cp.silos()
                    .size() >= 4
                    && cp.silos()
                        .size() <= 10,
                "4 to 10 silos: " + cp.silos()
                    .size());
            for (SiloSlot s : cp.silos()) {
                int sx = plan.anchorX() + s.shaftX(), sz = plan.anchorZ() + s.shaftZ();
                // The controller faces the gallery: (cx, cz) is its column, whichever way the silo is turned.
                int cx = plan.anchorX() + s.controllerX(), cz = plan.anchorZ() + s.controllerZ();
                out.check(world.getBlock(sx, floor + 5, sz) == Blocks.air, "silo bore is open");
                out.check(
                    world.getBlock(sx, GROUND - 1, sz) == Blocks.air || s.variant() != SurfaceVariant.OPEN,
                    "an open silo's bore reaches the surface");
                out.check(
                    world.isAirBlock(plan.anchorX() + s.frontX(), floor + 2, plan.anchorZ() + s.frontZ()),
                    "the walkway in front of the controller is open");
                // "Silo N" hangs on the concrete over the window, in the controller's column, facing the hall (D299).
                int[] sg = com.gregguns.icbm.complex.gen.ComplexSigns.siloSignAt(s);
                int number = com.gregguns.icbm.complex.gen.ComplexSigns.siloOrder(cp)
                    .indexOf(s) + 1;
                TileEntity st = world.getTileEntity(plan.anchorX() + sg[0], floor + sg[1], plan.anchorZ() + sg[2]);
                out.check(
                    world.getBlock(plan.anchorX() + sg[0], floor + sg[1], plan.anchorZ() + sg[2]) == Blocks.wall_sign
                        && world.getBlockMetadata(plan.anchorX() + sg[0], floor + sg[1], plan.anchorZ() + sg[2])
                            == sg[3],
                    "silo " + number + " has its wall sign over the window, facing the hall");
                out.check(
                    st instanceof net.minecraft.tileentity.TileEntitySign
                        && ("Silo " + number).equals(((net.minecraft.tileentity.TileEntitySign) st).signText[1]),
                    "the sign reads Silo " + number);
                TileEntity t = world.getTileEntity(cx, floor + 2, cz);
                boolean hasController = t instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity;
                out.check(
                    hasController == (s.missing() != MissingPart.CONTROLLER),
                    "controller present exactly when it is not the missing part (" + s.missing() + ")");
                siloHatchLayout(world, s, plan.anchorX(), plan.anchorZ(), floor, out);
                boolean complete = com.gregguns.machine.DevStructureBuilder.check(world, cx, floor + 2, cz);
                out.check(!complete, "a silo missing a part must not be formed");
                // Add the part the way a player would, with the item the chests hold: the silo then forms.
                boolean noController = s.missing() == MissingPart.CONTROLLER;
                ItemStack part = com.gregguns.icbm.worldgen.SiloBonusItems.stack(
                    noController ? com.gregguns.icbm.worldgen.Placement.Bonus.SILO_CONTROLLER
                        : com.gregguns.icbm.worldgen.Placement.Bonus.SILO_ENERGY_HATCH,
                    null);
                out.check(part != null, "the chest item for " + s.missing() + " resolves");
                if (part != null) {
                    int[] energy = s.hatchCell(Cell.HATCH_ENERGY);
                    GtMachineHook.placeMte(
                        world,
                        part.getItemDamage(),
                        noController ? cx : plan.anchorX() + energy[0],
                        floor + (noController ? 2 : energy[1]),
                        noController ? cz : plan.anchorZ() + energy[2],
                        net.minecraftforge.common.util.ForgeDirection.values()[FACING_TO_FORGE[s.facing()]]);
                    // A formed silo registers with GT's power-failure tracker, which needs an owner.
                    TileEntity ctl = world.getTileEntity(cx, floor + 2, cz);
                    if (ctl instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity) {
                        gregtech.api.interfaces.tileentity.IGregTechTileEntity g = (gregtech.api.interfaces.tileentity.IGregTechTileEntity) ctl;
                        g.setOwnerName("complex-test");
                        g.setOwnerUuid(java.util.UUID.nameUUIDFromBytes("complex-test".getBytes()));
                    }
                    out.check(
                        com.gregguns.machine.DevStructureBuilder.check(world, cx, floor + 2, cz),
                        "a silo forms once its missing " + s.missing() + " is added");
                    // Its own Launch Control, on top of the right hatch column, is part of it (D288, D299).
                    TileEntity formed = world.getTileEntity(cx, floor + 2, cz);
                    out.check(
                        formed instanceof gregtech.api.interfaces.tileentity.IGregTechTileEntity
                            && ((gregtech.api.interfaces.tileentity.IGregTechTileEntity) formed)
                                .getMetaTileEntity() instanceof com.gregguns.icbm.silo.MTEMissileSilo
                            && ((com.gregguns.icbm.silo.MTEMissileSilo) ((gregtech.api.interfaces.tileentity.IGregTechTileEntity) formed)
                                .getMetaTileEntity()).launchControl() != null,
                        "a formed generated silo has its own Launch Control");
                }
            }
            int ex = plan.anchorX() + cp.escapeX(), ez = plan.anchorZ() + cp.escapeZ();
            out.check(world.getBlock(ex, GROUND, ez) == Blocks.trapdoor, "the escape ladder ends in a trapdoor");
            out.check(world.getBlock(ex, GROUND - 2, ez) == Blocks.ladder, "the escape shaft has a ladder");
        }
    }

    /** Terrain that cannot host a complex is skipped, logged once, and left untouched. */
    private static final class Skips implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.skips";
        }

        @Override
        public String description() {
            return "A complex over water, over ground too low, over a steep ridge, or in a biome off the Overworld"
                + " whitelist is rejected once with a reason, writes nothing, and leaves the terrain as it was;"
                + " flat dry ground in the same place is placed (skip-and-log, D128). Under a village start the"
                + " complex sinks 10 to 20 blocks, the same from opposite corner windows, and is skipped for the"
                + " village only where it cannot sink (D302).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            StructurePlan plan = plan(world);
            String[] names = { "water", "low", "steep", "biome", "flat", "village", "village_reverse", "village_deep" };
            // A village start over the whole box (D302); the real start rule has its own scenario.
            com.gregguns.worldgen.StructureStarts village = (x0, z0, x1,
                z1) -> new com.gregguns.worldgen.StructureStarts.Blocker("village", "scenario start");
            int villageSink = -1;
            try {
                load(world, plan);
                for (String c : names) {
                    if (c.equals("biome") && world.provider.dimensionId != 0) continue; // the whitelist is Overworld
                                                                                        // only
                    boolean underVillage = c.startsWith("village");
                    java.util.function.IntBinaryOperator ground = (x, z) -> c.equals("low") ? 20
                        : c.equals("village_deep") ? 34
                            : c.equals("steep") && ((x >> 3) + (z >> 3) & 1) == 0 ? GROUND + 24 : GROUND; // the check
                                                                                                          // sees
                                                                                                          // only the
                                                                                                          // first
                                                                                                          // window's
                                                                                                          // slice,
                                                                                                          // so the
                                                                                                          // relief
                                                                                                          // repeats
                                                                                                          // within
                                                                                                          // every
                                                                                                          // window
                    // A sunk complex lies up to 20 blocks under the usual floor: the slab reaches that far down.
                    int base = underVillage ? GROUND - ComplexPlacer.SINK_MAX : GROUND;
                    ComplexArena.slab(world, plan, true, (x, z) -> GROUND, 0, base);
                    ComplexArena.slab(world, plan, false, ground, c.equals("water") ? 4 : 0, base);
                    long before = snapshot(world, plan);
                    java.util.List<String> reasons = new java.util.ArrayList<String>();
                    final boolean ocean = c.equals("biome");
                    ComplexPlacer placer = new ComplexPlacer(
                        new WorldBlocks(),
                        new GtMachineHook(),
                        MarkFiller.NONE,
                        new ComplexPlacer.Settings() {

                            public boolean dimensionEnabled(String n) {
                                return true;
                            }

                            public int chancePercent() {
                                return 100;
                            }

                            public java.util.Set<Type> overworldBiomes() {
                                return Collections.singleton(Type.PLAINS);
                            }
                        }).withContents(false)
                            .withSkipListener(reasons::add);
                    placer.enabledIn(world);
                    StructureState state = new StructureState("gregguns_complex_skip_" + c);
                    StructurePopulator populator = com.gregguns.worldgen.PlacementHarnessAccess.populator();
                    int rejected = 0;
                    for (int[] w : com.gregguns.worldgen.PlacementHarnessAccess
                        .windowsOf(plan, c.equals("village_reverse"))) {
                        PopulateWindow window = new PopulateWindow(w[0], w[1]);
                        ClippingSink sink = new ClippingSink(window, new WorldTarget(world));
                        try (PopulateGuard.Scope ignored = PopulateGuard.enter(w[0], w[1])) {
                            populator.populateComplex(
                                plan,
                                placer,
                                window,
                                sink,
                                new WorldColumnReader(world),
                                (x, z) -> Collections
                                    .singleton(ocean && world.provider.dimensionId == 0 ? Type.OCEAN : Type.PLAINS),
                                state,
                                underVillage ? village : com.gregguns.worldgen.StructureStarts.NONE);
                        }
                        rejected += sink.rejectedWrites();
                    }
                    com.gregguns.worldgen.Decision d = state.get(com.gregguns.worldgen.PlanKey.of(plan));
                    out.check(d != null, c + ": a decision was recorded");
                    if (d == null) continue;
                    if (c.equals("flat")) {
                        out.check(d.isPlaced(), "flat dry ground is placed: " + d);
                        out.checkEquals("flat: no skip logged", 0, reasons.size());
                        out.checkEquals("flat: not sunk", 0, d.sink());
                    } else if (c.equals("village") || c.equals("village_reverse")) {
                        out.check(d.isPlaced(), c + ": a complex under a village is kept: " + d);
                        out.check(
                            d.sink() >= ComplexPlacer.SINK_MIN && d.sink() <= ComplexPlacer.SINK_MAX,
                            c + ": sunk 10 to 20 blocks, got " + d.sink());
                        if (villageSink < 0) villageSink = d.sink();
                        else out.checkEquals("the same sink from the opposite corner", villageSink, d.sink());
                        ComplexPlan cp = ComplexLayout.fromSeed(plan.chestSeedBase(), plan.cellX(), plan.cellZ());
                        int floor = ComplexPlacer.floorY(world, d.groundY(), d.sink());
                        int ex = plan.anchorX() + cp.escapeX(), ez = plan.anchorZ() + cp.escapeZ();
                        out.check(
                            world.getBlock(ex, floor + cp.escapeTop(), ez) == Blocks.ladder,
                            c + ": the escape ladder starts at the sunk level (floor y " + floor + ")");
                        out.check(
                            world.getBlock(ex, GROUND, ez) == Blocks.trapdoor,
                            c + ": the trapdoor stays at the surface");
                        out.note(c + " -> sunk " + d.sink() + ", floor y " + floor);
                    } else {
                        out.check(!d.isPlaced(), c + ": rejected, got " + d);
                        out.checkEquals(c + ": logged once", 1, reasons.size());
                        out.checkEquals(c + ": nothing written", before, snapshot(world, plan));
                        out.note(c + " -> " + reasons);
                    }
                    out.checkEquals(c + ": writes dropped outside a window", 0, rejected);
                }
            } finally {
                ComplexArena.slab(world, plan, true, (x, z) -> GROUND, 0, GROUND - ComplexPlacer.SINK_MAX);
            }
            return out.build();
        }
    }
}
