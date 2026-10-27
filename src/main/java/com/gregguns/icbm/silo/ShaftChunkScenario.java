package com.gregguns.icbm.silo;

import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.warhead.WarheadBlocks;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadStacks;
import com.gregguns.machine.DevStructureBuilder;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;

/**
 * The shaft-chunk guard (carry-over from I3): a silo built across a chunk border, with the controller's chunk loaded
 * and the shaft's chunk not, must not mate, pump or spawn a missile, and the parts stay in the bus; once the chunk is
 * loaded again it carries on. The controller stands on the last row of its chunk, so the three-wide shaft behind it
 * lies wholly in the next chunk to the south.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (Chunks), I3 review carry-over
final class ShaftChunkScenario implements Scenario {

    /** The harness ticket that pinned the shaft chunk, to pin it again afterwards. */
    private static final net.minecraftforge.common.ForgeChunkManager.Ticket[] PINNED = { null };
    /**
     * How long to keep asking the provider to unload the shaft chunk, ticks. Hodgepodge's chunk scheduler protects a
     * freshly generated chunk (and its neighbors) from unloading for 600 ticks (PROTECTION_TIMEOUT_TICKS), and the
     * scenario area was generated just before this runs; the dev server's chunk was old, so it unloaded at once.
     */
    private static final int UNLOAD_WAIT = 900;
    private static final String FUEL = "fluid.rocketfuelmixc";

    @Override
    public String name() {
        return "icbm.silo.shaft_chunk_guard";
    }

    @Override
    public String description() {
        return "A silo across a chunk border, asked to work while its shaft chunk is unloaded, refuses and neither mates, pumps nor spawns, the parts stay in the bus; once the chunk is loaded it finishes mating and fueling by itself.";
    }

    @Override
    public int areaRadius() {
        return 30;
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        if (SiloScenarios.gate() != null) {
            return ScenarioResult.skip(name(), SiloScenarios.gate());
        }
        ScenarioResult.Builder out = ScenarioResult.builder(name());
        // The controller: x in the middle of its chunk, z on the last row (so the shaft is in the next chunk).
        final int ox = Math.floorMod(8 - ctx.originX(), 16);
        final int oz = Math.floorMod(15 - ctx.originZ(), 16);
        final int wx = ctx.originX() + ox;
        final int wy = ctx.originY() + SiloScenarios.CY;
        final int wz = ctx.originZ() + oz;
        final int shaftCx = wx >> 4;
        final int shaftCz = (wz + 2) >> 4;
        final int ctrlCz = wz >> 4;
        out.check(shaftCz != ctrlCz, "the shaft is in another chunk than the controller");
        for (int a = ox - 10; a <= ox + 10; a++) {
            for (int b = oz - 10; b <= oz + 10; b++) {
                for (int h = 0; h < SiloScenarios.CLEAR_HEIGHT; h++) {
                    ctx.setBlock(a, SiloScenarios.CY - 3 + h, b, h == 0 ? Blocks.stone : Blocks.air, 0);
                }
            }
        }
        ctx.setBlock(
            ox,
            SiloScenarios.CY,
            oz,
            GregTechAPI.sBlockMachines,
            GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.MISSILE_SILO].getTileEntityBaseType());
        ((IGregTechTileEntity) ctx.world()
            .getTileEntity(wx, wy, wz)).setInitialValuesAsNBT(null, (short) GregGunsMTEIDs.MISSILE_SILO);
        out.note("build: " + DevStructureBuilder.buildSilo(ctx.world(), wx, wy, wz, ItemList.Hatch_Energy_LuV));
        final WorldServer world = (WorldServer) ctx.world();
        final WarheadCasing casing = WarheadCasing.T3;
        int[] stage = { 0 };
        int[] mark = { 0 };
        int[] level = { 0 };
        int[] full = { 0 };
        boolean[] stocked = { false };
        return ScenarioResult.pending(name(), 9000, ticks -> {
            if (ticks < SiloScenarios.WARMUP) {
                return null;
            }
            IGregTechTileEntity base = tile(ctx, ox, 0, oz);
            if (base == null || !(base.getMetaTileEntity() instanceof MTEMissileSilo)) {
                return ScenarioResult.fail(name(), "the silo controller is gone");
            }
            MTEMissileSilo silo = (MTEMissileSilo) base.getMetaTileEntity();
            // Power the whole time; fuel in the windows where the chunk is loaded and the silo is meant to work.
            IGregTechTileEntity energy = tile(ctx, ox, -1, oz);
            if (energy != null) {
                energy.increaseStoredEnergyUnits(50_000_000L, true);
            }
            switch (stage[0]) {
                case 0: {
                    out.check(
                        DevStructureBuilder.check(ctx.world(), wx, wy, wz),
                        "the silo forms across the chunk border");
                    // Nothing is loaded into the silo yet: while the chunk is still protected from unloading
                    // (see UNLOAD_WAIT) an idle silo has nothing to mate, so the guard is what gets tested.
                    stage[0] = 1;
                    mark[0] = ticks;
                    return null;
                }
                case 1: {
                    // Unload (retrying: the provider may take many ticks) and only then give the silo its work.
                    unload(world, shaftCx, shaftCz);
                    if (loaded(world, shaftCx, shaftCz)) {
                        return ticks > mark[0] + UNLOAD_WAIT ? stuck(world, shaftCx, shaftCz) : null;
                    }
                    if (!stocked[0]) {
                        IGregTechTileEntity bus = tile(ctx, ox - 1, 0, oz);
                        bus.setInventorySlotContents(0, MissileBodies.body(MissileTier.T3));
                        bus.setInventorySlotContents(
                            1,
                            WarheadStacks.make(
                                net.minecraft.item.Item.getItemFromBlock(WarheadBlocks.of(casing)),
                                casing,
                                "exothermic",
                                1));
                        fill(ctx, ox, oz);
                        full[0] = hatchMb(ctx, ox, oz);
                        out.check(full[0] > 0, "the fuel hatch holds fuel before the check");
                        stocked[0] = true;
                        mark[0] = ticks;
                        return null;
                    }
                    if (ticks < mark[0] + 40) {
                        return null; // let the silo try, by itself, for two seconds with the chunk out
                    }
                    out.check(loaded(world, wx >> 4, ctrlCz), "the controller chunk stays loaded");
                    out.check(busItems(ctx, ox, oz) == 2, "the parts stay in the bus while the chunk is out");
                    // Ask the controller to work right now, while the chunk is out: it must refuse and touch nothing.
                    for (int i = 0; i < 3; i++) {
                        out.check(
                            !silo.checkProcessing()
                                .wasSuccessful(),
                            "a processing check refuses while the shaft is unloaded");
                    }
                    out.check(busItems(ctx, ox, oz) == 2, "the parts stay in the bus");
                    out.check(missiles(world) == 0, "no missile spawned");
                    out.checkEquals("no fuel pumped", full[0], hatchMb(ctx, ox, oz));
                    out.checkEquals("no parts taken out of the bus into a pending cycle", 0, silo.pendingTier());
                    reload(world, shaftCx, shaftCz);
                    mark[0] = ticks;
                    stage[0] = 2;
                    return null;
                }
                case 2: {
                    fill(ctx, ox, oz);
                    if (silo.missile() == null) {
                        return ticks > mark[0] + 2500 ? ScenarioResult.fail(
                            name(),
                            "the silo never mated: " + state(ctx, world, silo, wx, wy, wz, ox, oz, shaftCx, shaftCz))
                            : null;
                    }
                    out.check(true, "with the chunk loaded the silo mated the missile");
                    out.check(busItems(ctx, ox, oz) == 0, "and took the parts from the bus");
                    stage[0] = 3;
                    mark[0] = ticks;
                    return null;
                }
                case 3: {
                    // Wait for the pump to start, then pull the chunk away mid-fill.
                    if (hatchMb(ctx, ox, oz) >= full[0] && ticks < mark[0] + 1500) {
                        return null;
                    }
                    level[0] = hatchMb(ctx, ox, oz);
                    out.check(level[0] < full[0], "the silo started pumping fuel");
                    stage[0] = 4;
                    mark[0] = ticks;
                    return null;
                }
                case 4: {
                    unload(world, shaftCx, shaftCz);
                    if (loaded(world, shaftCx, shaftCz)) {
                        return ticks > mark[0] + UNLOAD_WAIT ? stuck(world, shaftCx, shaftCz) : null;
                    }
                    level[0] = hatchMb(ctx, ox, oz);
                    for (int i = 0; i < 3; i++) {
                        out.check(
                            !silo.checkProcessing()
                                .wasSuccessful(),
                            "a processing check refuses to pump while the shaft is unloaded");
                    }
                    out.checkEquals("no fuel is pumped while the shaft is unloaded", level[0], hatchMb(ctx, ox, oz));
                    reload(world, shaftCx, shaftCz);
                    mark[0] = ticks;
                    stage[0] = 5;
                    return null;
                }
                default: {
                    fill(ctx, ox, oz);
                    EntityMissile m = silo.missile();
                    if (m == null || m.fuelMb() < m.capacityMb()) {
                        return ticks > mark[0] + 3000 ? ScenarioResult.fail(name(), "the silo never finished fueling")
                            : null;
                    }
                    out.check(true, "with the chunk back the silo finished fueling the missile");
                    m.setDead();
                    return out.build();
                }
            }
        });
    }

    /**
     * The chunk would not unload within {@link #UNLOAD_WAIT} ticks. That is a failure, not a skip: the guard cannot be
     * tested, so something other than the generation protection holds the chunk (a ticket the scenario does not know,
     * or a loader), and the scenario says so.
     */
    private static ScenarioResult stuck(WorldServer w, int cx, int cz) {
        reload(w, cx, cz);
        return ScenarioResult.fail(
            "icbm.silo.shaft_chunk_guard",
            "the shaft chunk would not unload in " + UNLOAD_WAIT + " ticks: something holds it");
    }

    private static String state(ScenarioContext ctx, WorldServer w, MTEMissileSilo silo, int wx, int wy, int wz, int ox,
        int oz, int cx, int cz) {
        return "formed=" + DevStructureBuilder.check(w, wx, wy, wz)
            + " mMachine="
            + silo.isFormed()
            + " shaftChunkLoaded="
            + loaded(w, cx, cz)
            + " bus="
            + busItems(ctx, ox, oz)
            + " hatchMb="
            + hatchMb(ctx, ox, oz)
            + " missiles="
            + missiles(w)
            + " active="
            + silo.getBaseMetaTileEntity()
                .isActive()
            + " allowed="
            + silo.getBaseMetaTileEntity()
                .isAllowedToWork()
            + " check="
            + silo.checkProcessing()
                .getDisplayString()
            + " energy="
            + tile(ctx, ox, -1, oz).getStoredEU()
            + " pending="
            + silo.pendingTier();
    }

    private static IGregTechTileEntity tile(ScenarioContext ctx, int dx, int dy, int dz) {
        TileEntity t = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + SiloScenarios.CY + dy, ctx.originZ() + dz);
        return t instanceof IGregTechTileEntity ? (IGregTechTileEntity) t : null;
    }

    private static boolean loaded(WorldServer w, int cx, int cz) {
        return w.theChunkProviderServer.chunkExists(cx, cz);
    }

    /** Queues the chunk for unloading and runs the unload at once; entities in it are saved with it. */
    private static void unload(WorldServer w, int cx, int cz) {
        if (!w.theChunkProviderServer.chunkExists(cx, cz)) {
            return;
        }
        // The scenario harness pins its area with Forge tickets; let go of the shaft chunk so it can unload.
        net.minecraft.world.ChunkCoordIntPair pair = new net.minecraft.world.ChunkCoordIntPair(cx, cz);
        for (net.minecraftforge.common.ForgeChunkManager.Ticket ticket : new java.util.ArrayList<>(
            net.minecraftforge.common.ForgeChunkManager.getPersistentChunksFor(w)
                .get(pair))) {
            PINNED[0] = ticket;
            net.minecraftforge.common.ForgeChunkManager.unforceChunk(ticket, pair);
        }
        // The provider's own queueing: it keeps the spawn area loaded and queues everything else.
        w.theChunkProviderServer.unloadChunksIfNotNearSpawn(cx, cz);
        w.theChunkProviderServer.unloadQueuedChunks();
    }

    /** Loads the chunk again and pins it as the harness had. */
    private static void reload(WorldServer w, int cx, int cz) {
        w.theChunkProviderServer.loadChunk(cx, cz);
        if (PINNED[0] != null) {
            net.minecraftforge.common.ForgeChunkManager
                .forceChunk(PINNED[0], new net.minecraft.world.ChunkCoordIntPair(cx, cz));
        }
    }

    private static int missiles(WorldServer w) {
        int n = 0;
        for (Object o : w.loadedEntityList) {
            if (o instanceof EntityMissile && !((Entity) o).isDead) {
                n++;
            }
        }
        return n;
    }

    private static int busItems(ScenarioContext ctx, int ox, int oz) {
        IGregTechTileEntity bus = tile(ctx, ox - 1, 0, oz);
        int n = 0;
        for (int i = 0; i < bus.getSizeInventory(); i++) {
            if (bus.getStackInSlot(i) != null) {
                n++;
            }
        }
        return n;
    }

    private static int hatchMb(ScenarioContext ctx, int ox, int oz) {
        IGregTechTileEntity hatch = tile(ctx, ox + 1, -1, oz);
        if (hatch == null || !(hatch.getMetaTileEntity() instanceof MTEHatchInput)) {
            return 0;
        }
        FluidStack fs = ((MTEHatchInput) hatch.getMetaTileEntity()).getFillableStack();
        return fs == null ? 0 : fs.amount;
    }

    private static void fill(ScenarioContext ctx, int ox, int oz) {
        IGregTechTileEntity hatch = tile(ctx, ox + 1, -1, oz);
        Fluid f = FluidRegistry.getFluid(FUEL);
        if (hatch != null && f != null
            && hatch.getMetaTileEntity() instanceof MTEHatchInput
            && hatchMb(ctx, ox, oz) == 0) {
            ((MTEHatchInput) hatch.getMetaTileEntity()).setFillableStack(new FluidStack(f, 16000));
        }
    }
}
