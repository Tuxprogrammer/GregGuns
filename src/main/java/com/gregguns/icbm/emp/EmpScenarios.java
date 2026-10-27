package com.gregguns.icbm.emp;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

/**
 * In-game scenarios of the EMP Tower and the EMP pulse (D16): registration, the firing gate and triggers, and the exit
 * box (a pulse drains EU and causes maintenance issues within its device cap, with no tick spike in a dense base).
 * The missile and warhead halves live beside the missile scenarios. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/emp-tower.md (Tests)
public final class EmpScenarios {

    /** Ticks GT needs before a placed machine is a settled tile entity. */
    private static final int WARMUP = 45;
    /** A pulse that scans a dense base must finish well inside one tick (50 ms); the bar leaves room for a slow box. */
    private static final long SPIKE_NANOS = 40_000_000L;

    private EmpScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Registered());
        registry.register(new Gate());
        registry.register(new Triggers());
        registry.register(new DensePulse());
        registry.register(new Modes());
    }

    // --- helpers --------------------------------------------------------------------------------------------------

    /** Puts a GT machine with the given MTE id at an offset and returns its tile entity. */
    public static IGregTechTileEntity place(ScenarioContext ctx, int dx, int dy, int dz, int id) {
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
        ctx.setBlock(dx, dy, dz, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
        if (!(tile instanceof IGregTechTileEntity)) {
            throw new IllegalStateException("no GT tile entity at " + dx + "," + dy + "," + dz);
        }
        IGregTechTileEntity base = (IGregTechTileEntity) tile;
        base.setInitialValuesAsNBT(null, (short) id);
        base.setFrontFacing(ForgeDirection.NORTH);
        base.setOwnerName("gregguns-test");
        base.setOwnerUuid(UUID.nameUUIDFromBytes("gregguns-test".getBytes(StandardCharsets.UTF_8)));
        return base;
    }

    /**
     * Puts a GT machine at absolute coordinates, for targets farther from the scenario origin than the scenario area
     * (the context's {@code setBlock} does not reach there).
     *
     * @param world the world
     * @param x     block X
     * @param y     block Y
     * @param z     block Z
     * @param id    the MTE id
     * @return its tile entity
     */
    public static IGregTechTileEntity placeAbs(net.minecraft.world.World world, int x, int y, int z, int id) {
        world.getChunkProvider()
            .provideChunk(x >> 4, z >> 4);
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
        // A machine left from an earlier run stays put when the same block is set again; start from air.
        world.setBlockToAir(x, y, z);
        boolean set = world.setBlock(x, y, z, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType(), 3);
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IGregTechTileEntity)) {
            throw new IllegalStateException(
                "no GT tile entity at " + x
                    + ","
                    + y
                    + ","
                    + z
                    + " (setBlock "
                    + set
                    + ", found "
                    + tile
                    + ", block "
                    + world.getBlock(x, y, z)
                    + ")");
        }
        IGregTechTileEntity base = (IGregTechTileEntity) tile;
        base.setInitialValuesAsNBT(null, (short) id);
        base.setFrontFacing(ForgeDirection.NORTH);
        return base;
    }

    /** The machine as the world holds it now; GT may replace the tile entity in its first ticks. */
    public static IGregTechTileEntity live(ScenarioContext ctx, int dx, int dy, int dz) {
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
        return tile instanceof IGregTechTileEntity ? (IGregTechTileEntity) tile : null;
    }

    public static MTEEmpTower tower(IGregTechTileEntity base) {
        return base != null && base.getMetaTileEntity() instanceof MTEEmpTower ? (MTEEmpTower) base.getMetaTileEntity()
            : null;
    }

    private static void setEu(IGregTechTileEntity base, long eu) {
        ((BaseMetaTileEntity) base).setStoredEU(eu);
    }

    private static int bufferId() {
        return ItemList.Battery_Buffer_1by1_MV.get(1)
            .getItemDamage();
    }

    private static int multiblockId() {
        return ItemList.Machine_Multi_BlastFurnace.get(1)
            .getItemDamage();
    }

    private abstract static class Base implements Scenario {

        @Override
        public final ScenarioResult run(ScenarioContext ctx) {
            if (!ModuleState.isActive(ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), "the ICBM module is off, so the EMP Tower does nothing");
            }
            return go(ctx);
        }

        abstract ScenarioResult go(ScenarioContext ctx);
    }

    // --- scenarios ------------------------------------------------------------------------------------------------

    /** The machine is registered under 20146 as an EV EMP Tower. */
    private static final class Registered implements Scenario {

        @Override
        public String name() {
            return "icbm.emp.registered";
        }

        @Override
        public String description() {
            return "The EMP Tower is registered under MTE 20146 as an EV single block of the EMP Tower class.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            IMetaTileEntity mte = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.EMP_TOWER];
            out.check(mte instanceof MTEEmpTower, "MTE 20146 is the EMP Tower");
            if (mte instanceof MTEEmpTower) {
                out.checkEquals("tier", (byte) 4, ((MTEEmpTower) mte).mTier);
                out.check(
                    ((MTEEmpTower) mte).maxEUStore() >= EmpRules.euCost(IcbmConfig.empMaxRadius),
                    "it stores a pulse at the largest radius");
            }
            return out.build();
        }
    }

    /** The gate: no power, power, cooldown, the EU is taken after the pulse, and the radius is clamped. */
    private static final class Gate extends Base {

        @Override
        public String name() {
            return "icbm.emp.gate";
        }

        @Override
        public String description() {
            return "A tower without the EU refuses and costs nothing; with it, one pulse takes exactly the cost for its radius and starts the cooldown; a second pulse is refused as cooling; the radius clamps both ways.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(6, 4);
            place(ctx, 0, 0, 0, GregGunsMTEIDs.EMP_TOWER);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            return ScenarioResult.pending(name(), WARMUP + 40, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                IGregTechTileEntity base = live(ctx, 0, 0, 0);
                MTEEmpTower t = tower(base);
                if (t == null) {
                    return ScenarioResult.fail(name(), "the EMP Tower is gone after the warm-up");
                }
                t.setRadius(-50);
                out.checkEquals("a negative radius clamps up", EmpRules.MIN_RADIUS, t.radius());
                t.setRadius(1_000_000);
                out.checkEquals("a huge radius clamps down", IcbmConfig.empMaxRadius, t.radius());
                t.setRadius(30);
                setEu(base, t.cost() - 1);
                out.checkEquals("one EU short is refused", EmpRules.Gate.NO_POWER, t.fire());
                out.checkEquals("a refused pulse costs nothing", t.cost() - 1, base.getStoredEU());
                out.checkEquals("and fires nothing", 0, t.pulses());
                setEu(base, t.cost() + 500);
                out.checkEquals("with the EU it fires", EmpRules.Gate.READY, t.fire());
                out.checkEquals("it took exactly the cost of radius 30", 500L, base.getStoredEU());
                out.checkEquals("the cost of radius 30 is 22,500 EU", 22_500L, EmpRules.euCost(30));
                out.checkEquals("cooldown started", EmpRules.COOLDOWN_TICKS, t.cooldownLeft());
                setEu(base, t.cost());
                out.checkEquals("a second pulse is cooling down", EmpRules.Gate.COOLING, t.fire());
                out.checkEquals("one pulse in all", 1, t.pulses());
                out.check(
                    !t.gate()
                        .isGood(),
                    "the shared gate agrees");
                return out.build();
            });
        }
    }

    /** Redstone and a controller cover each fire once per rising edge, and a held signal does not repeat. */
    private static final class Triggers extends Base {

        @Override
        public String name() {
            return "icbm.emp.triggers";
        }

        @Override
        public String description() {
            return "A redstone block next to the tower fires one pulse and a held signal does not repeat it; disabling and re-enabling its work (a controller cover) fires another.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(6, 4);
            place(ctx, 0, 0, 0, GregGunsMTEIDs.EMP_TOWER);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            final int[] stage = { 0 };
            final int[] mark = { 0 };
            return ScenarioResult.pending(name(), WARMUP + 700, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                IGregTechTileEntity base = live(ctx, 0, 0, 0);
                MTEEmpTower t = tower(base);
                if (t == null) {
                    return ScenarioResult.fail(name(), "the EMP Tower is gone after the warm-up");
                }
                switch (stage[0]) {
                    case 0:
                        t.setRadius(8);
                        setEu(base, t.cost() * 4);
                        ctx.setBlock(1, 0, 0, Blocks.redstone_block, 0);
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        if (ticks < mark[0] + 10) {
                            return null;
                        }
                        out.checkEquals("a redstone edge fires once", 1, t.pulses());
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        // The cooldown ends at 200 ticks; a signal still held must not fire again.
                        if (ticks < mark[0] + EmpRules.COOLDOWN_TICKS + 40) {
                            return null;
                        }
                        out.checkEquals("a held signal does not repeat", 1, t.pulses());
                        base.disableWorking();
                        mark[0] = ticks;
                        stage[0] = 3;
                        return null;
                    case 3:
                        if (ticks < mark[0] + 10) {
                            return null;
                        }
                        out.checkEquals("disabling does not fire", 1, t.pulses());
                        base.enableWorking();
                        mark[0] = ticks;
                        stage[0] = 4;
                        return null;
                    case 4:
                        if (ticks < mark[0] + 10) {
                            return null;
                        }
                        out.checkEquals("re-enabling (a cover edge) fires", 2, t.pulses());
                        return out.build();
                    default:
                        return null;
                }
            });
        }
    }

    /** Exit box: a pulse in a dense base drains the nearest devices only, causes one issue, and is quick. */
    private static final class DensePulse extends Base {

        private static final int RADIUS = 20;

        @Override
        public String name() {
            return "icbm.emp.dense_pulse";
        }

        @Override
        public String description() {
            return "Among 169 charged battery buffers and a multiblock, one pulse hits exactly the nearest 64 devices (the cap), draining their EU, none outside the radius and not the tower itself, gives the multiblock exactly one maintenance issue, spends exactly its cost, and takes well under a tick.";
        }

        @Override
        public int areaRadius() {
            return 30;
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(28, 4);
            int oldCap = IcbmConfig.empMaxDevices;
            IcbmConfig.empMaxDevices = 64;
            place(ctx, 0, 0, 0, GregGunsMTEIDs.EMP_TOWER);
            int buffer = bufferId();
            // 13 x 13 buffers east of the tower, one block apart, and one out of range.
            for (int x = 2; x <= 14; x++) {
                for (int z = -6; z <= 6; z++) {
                    place(ctx, x, 0, z, buffer);
                }
            }
            place(ctx, 25, 0, 0, buffer);
            place(ctx, -3, 0, 0, multiblockId());
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            return ScenarioResult.pending(name(), WARMUP + 60, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                IGregTechTileEntity base = live(ctx, 0, 0, 0);
                MTEEmpTower t = tower(base);
                IGregTechTileEntity multi = live(ctx, -3, 0, 0);
                if (t == null || multi == null || !(multi.getMetaTileEntity() instanceof MTEMultiBlockBase)) {
                    IcbmConfig.empMaxDevices = oldCap;
                    return ScenarioResult.fail(name(), "the tower or the multiblock is gone after the warm-up");
                }
                MTEMultiBlockBase mb = (MTEMultiBlockBase) multi.getMetaTileEntity();
                mb.fixAllIssues();
                List<IGregTechTileEntity> devices = new ArrayList<>();
                for (int x = 2; x <= 14; x++) {
                    for (int z = -6; z <= 6; z++) {
                        devices.add(live(ctx, x, 0, z));
                    }
                }
                IGregTechTileEntity far = live(ctx, 25, 0, 0);
                for (IGregTechTileEntity d : devices) {
                    setEu(d, 1000L);
                }
                setEu(far, 1000L);
                t.setRadius(RADIUS);
                setEu(base, t.cost() + 1234);
                out.checkEquals("the tower fires", EmpRules.Gate.READY, t.fire());
                EmpPulse.Result r = t.lastResult();
                out.checkEquals("exactly the cap was hit", 64, r.devices);
                out.checkEquals("the rest inside the radius were skipped by the cap", 170 - 64, r.skippedByCap);
                // The multiblock is the nearest device, so it takes one of the 64 places; the other 63 are buffers of
                // 1,000.
                out.checkEquals("EU drained: 63 buffers of 1,000", 63_000L, r.euDrained);
                out.checkEquals("the tower paid exactly the cost", 1234L, base.getStoredEU());
                out.checkEquals("exactly one maintenance issue", 1, 6 - mb.getRepairStatus());
                out.checkEquals("it was an issue on the multiblock in range", 1, r.issues);
                out.checkEquals("the device out of range keeps its EU", 1000L, far.getStoredEU());
                // Nearest first: no drained device is farther than a device that kept its EU.
                double maxDrained = 0.0;
                double minKept = Double.MAX_VALUE;
                int drained = 0;
                for (IGregTechTileEntity d : devices) {
                    double dx = d.getXCoord() - base.getXCoord();
                    double dy = d.getYCoord() - base.getYCoord();
                    double dz = d.getZCoord() - base.getZCoord();
                    double dist = dx * dx + dy * dy + dz * dz;
                    if (d.getStoredEU() == 0L) {
                        drained++;
                        maxDrained = Math.max(maxDrained, dist);
                    } else {
                        minKept = Math.min(minKept, dist);
                    }
                }
                out.checkEquals("63 buffers are empty", 63, drained);
                out.check(maxDrained <= minKept, "the devices that were drained are the nearest ones");
                out.check(r.tilesScanned >= 170, "it looked at tile entities, not blocks (" + r.tilesScanned + ")");
                out.check(
                    r.nanos < SPIKE_NANOS,
                    "no tick spike: " + (r.nanos / 1_000_000.0) + " ms for " + r.tilesScanned + " tiles");
                IcbmConfig.empMaxDevices = oldCap;
                return out.build();
            });
        }
    }

    /** The modes decide what a pulse reaches. */
    private static final class Modes extends Base {

        @Override
        public String name() {
            return "icbm.emp.modes";
        }

        @Override
        public String description() {
            return "Missiles-only mode leaves a charged buffer alone; machines-only drains it; a buffer in the sender's own tile is spared; a cap of 0 hits nothing.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(8, 4);
            place(ctx, 0, 0, 0, GregGunsMTEIDs.EMP_TOWER);
            place(ctx, 2, 0, 0, bufferId());
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            return ScenarioResult.pending(name(), WARMUP + 20, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                IGregTechTileEntity base = live(ctx, 0, 0, 0);
                IGregTechTileEntity buf = live(ctx, 2, 0, 0);
                if (tower(base) == null || buf == null) {
                    return ScenarioResult.fail(name(), "a machine is gone after the warm-up");
                }
                double cx = base.getXCoord() + 0.5;
                double cy = base.getYCoord() + 0.5;
                double cz = base.getZCoord() + 0.5;
                setEu(buf, 5000L);
                setEu(base, 777L);
                EmpPulse.Result r = EmpPulse
                    .fire(ctx.world(), cx, cy, cz, 10, EmpMode.MISSILES, 64, MissileTable.DEFAULT, null);
                out.checkEquals("missiles-only hits no device", 0, r.devices);
                out.checkEquals("the buffer is untouched", 5000L, buf.getStoredEU());
                r = EmpPulse
                    .fire(ctx.world(), cx, cy, cz, 10, EmpMode.MACHINES, 64, MissileTable.DEFAULT, (TileEntity) base);
                out.checkEquals("machines-only hits the buffer", 1, r.devices);
                out.checkEquals("the buffer is empty", 0L, buf.getStoredEU());
                out.checkEquals("the sender is spared", 777L, base.getStoredEU());
                setEu(buf, 5000L);
                r = EmpPulse.fire(ctx.world(), cx, cy, cz, 10, EmpMode.ALL, 0, MissileTable.DEFAULT, null);
                out.checkEquals("a cap of 0 hits nothing", 0, r.devices);
                out.checkEquals("the buffer keeps its EU", 5000L, buf.getStoredEU());
                return out.build();
            });
        }
    }
}
