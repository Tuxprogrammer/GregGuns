package com.gregguns.icbm.missile;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.IcbmAvailability;
import com.gregguns.icbm.warhead.WarheadCasing;

import micdoodle8.mods.galacticraft.api.entity.IFuelable;
import micdoodle8.mods.galacticraft.api.tile.IFuelDock;
import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;
import micdoodle8.mods.galacticraft.core.event.EventLandingPadRemoval;
import micdoodle8.mods.galacticraft.core.tile.TileEntityFuelLoader;
import micdoodle8.mods.galacticraft.core.tile.TileEntityLandingPad;
import micdoodle8.mods.galacticraft.core.util.ConfigManagerCore;

/**
 * The Galacticraft half of the missile scenarios: the fuel lists, the pad survival check (D32, exit box 2) and the
 * docking path. These classes name Galacticraft types, so they are loaded only when Galacticraft is present.
 * Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/galacticraft-pad.md, fuel.md
public final class MissileGcScenarios {

    private MissileGcScenarios() {}

    /** @return what {@code removeFuel} gives for a missile (always null: a tank cannot be drained) */
    static Object removeFuel(EntityMissile m) {
        return ((IFuelable) m).removeFuel(100);
    }

    /** Checks the fuel lists against the real loader test (D19: "this fuel or better"). */
    static void fuelLists(ScenarioResult.Builder out, ScenarioContext ctx, EntityMissile t1) {
        out.check(MissileGc.accepts(t1, "nitrofuel"), "T1 takes Cetane-Boosted Diesel");
        out.check(MissileGc.accepts(t1, "fluid.rocketfuelmixb"), "T1 takes RP-1");
        out.check(MissileGc.accepts(t1, "fluid.rocketfuelmixd"), "T1 takes Dense Hydrazine (better fuel)");
        out.check(!MissileGc.accepts(t1, "water"), "T1 does not take water");
        out.check(!MissileGc.accepts(t1, "fuel"), "T1 does not take plain Galacticraft fuel");
        EntityMissile t2 = MissileScenarios.armedAt(ctx, MissileTier.T2, 0, 4);
        if (t2 != null) {
            out.check(!MissileGc.accepts(t2, "nitrofuel"), "T2 does not take Cetane-Boosted Diesel");
            out.check(!MissileGc.accepts(t2, "fluid.rocketfuelmixb"), "T2 does not take RP-1");
            out.check(MissileGc.accepts(t2, "fluid.rocketfuelmixd"), "T2 takes Dense Hydrazine");
            out.check(MissileGc.accepts(t2, "fluid.rocketfuelmixa"), "T2 takes Green (better fuel)");
            // The loader offers generic fuel: the entity's own addFuel is the path it uses.
            Fluid generic = FluidRegistry.getFluid("fuel");
            if (generic != null) {
                out.checkEquals("loader fill fits", 700, ((IFuelable) t2).addFuel(new FluidStack(generic, 700), true));
                out.checkEquals("fuel level after the loader", 700, t2.fuelMb());
            }
            t2.disarm();
        }
    }

    /**
     * A full launch from a real Galacticraft pad with a fuel loader beside it: the pad, the loader and the pad's
     * multi-block cells are still there after the launch and after the impact, the pad docks the missile, the loader
     * accepts the fuel, and removal of a pad that holds a missile is denied (D32, 0018 §9).
     */
    static final class PadSurvives implements Scenario {

        @Override
        public String name() {
            return "icbm.pad.survives";
        }

        @Override
        public String description() {
            return "The Galacticraft pad, fuel loader and pad cells stay intact through a launch; pad removal is denied.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (MissileScenarios.moduleOff()) {
                return ScenarioResult.fail(name(), "the missile content is not registered");
            }
            if (!IcbmAvailability.galacticraftPresent()) {
                return ScenarioResult.skip(name(), "Galacticraft is not loaded (D46)");
            }
            ctx.clearArea(8, 20);
            MissileScenarios.floor(ctx, 6);
            // Nine single pads; the center one turns the 3x3 into a full pad on its next tick.
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    ctx.setBlock(a, 0, b, GCBlocks.landingPad, 0);
                }
            }
            ctx.setBlock(2, 0, 0, GCBlocks.fuelLoader, 0);
            boolean terrain = IcbmConfig.terrainDamageT1;
            IcbmConfig.terrainDamageT1 = false;
            World w = ctx.world();
            int cx = ctx.originX();
            int cy = ctx.originY();
            int cz = ctx.originZ();
            MissileScenarios.Target target = new MissileScenarios.Target(ctx, 0, 70, 9);
            final EntityMissile[] missile = { null };
            final int[] phase = { 0 };
            final int[] launchedAt = { 0 };
            final int[] phaseSince = { 0 };
            final int[] fuelFactor = { 1 };
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            return ScenarioResult.pending(name(), 2400, ticks -> {
                if (phase[0] == 0) {
                    if (w.getBlock(cx, cy, cz) != GCBlocks.landingPadFull) {
                        return ticks > 100 ? fail("the 3x3 pad never formed") : null;
                    }
                    // Stack on the pad: clicking another pad cell must land on the center.
                    int[] center = MissileGc.padCenter(w, cx + 1, cy, cz + 1);
                    out.check(center != null && center[0] == cx && center[2] == cz, "padCenter finds the center cell");
                    int[] base = MissileScenarios
                        .buildStack(ctx, MissileTier.T1, WarheadCasing.T1, "condensed", 3, 0, 1, 0, true, 3);
                    out.checkEquals(
                        "armed on the pad",
                        MissileStacks.ArmResult.OK,
                        MissileStacks.arm(w, base[0], base[1], base[2], null));
                    missile[0] = PadMissiles.find(w, cx, cy, cz);
                    out.check(missile[0] != null, "the pad API finds the missile from the pad cell");
                    if (missile[0] == null) {
                        return finish(out, terrain);
                    }
                    phase[0] = 1;
                    return null;
                }
                EntityMissile m = missile[0];
                TileEntity pad = w.getTileEntity(cx, cy, cz);
                if (phase[0] == 1) {
                    // The pad's own tick docks any IFuelable entity standing in its column.
                    if (!(pad instanceof TileEntityLandingPad) || ((IFuelDock) pad).getDockedEntity() != m) {
                        return ticks > 60 ? fail("the pad never docked the missile") : null;
                    }
                    out.check(true, "the pad docked the missile");
                    EventLandingPadRemoval removal = new EventLandingPadRemoval(w, cx, cy, cz);
                    MinecraftForge.EVENT_BUS.post(removal);
                    out.check(!removal.allow, "pad removal is denied while a missile stands on it");
                    // Fuel through the loader itself: it checks the fluid against RocketFuels, then pours into the
                    // dock.
                    // First a fluid that is not in any missile's list (Galacticraft's own rocket fuel): the loader must
                    // leave the tank empty. (A T1 takes the mixes as "better fuel", so one of them is no refusal.)
                    fuelFactor[0] = ConfigManagerCore.rocketFuelFactor;
                    // The loader pours 2 mB x the factor per tick; speed it up so the test stays short.
                    ConfigManagerCore.rocketFuelFactor = 100;
                    fillLoader(out, w, cx + 2, cy, cz, "fuel");
                    phase[0] = 5;
                    phaseSince[0] = ticks;
                    return null;
                }
                if (phase[0] == 5 || phase[0] == 6) {
                    TileEntity lt = w.getTileEntity(cx + 2, cy, cz);
                    powerLoader(lt);
                    if (phase[0] == 5) {
                        if (ticks - phaseSince[0] < 130) {
                            return null;
                        }
                        out.checkEquals("the loader refuses another tier's fuel", 0, m.fuelMb());
                        out.check(loaderMb(lt) > 0, "the refused fuel is still in the loader (it took nothing)");
                        // A positive fill with a mix (RP-1, the T1 tank's own mix).
                        fillLoader(out, w, cx + 2, cy, cz, "fluid.rocketfuelmixb");
                        phase[0] = 6;
                        phaseSince[0] = ticks;
                        return null;
                    }
                    if (m.fuelMb() < m.capacityMb()) {
                        if (ticks - phaseSince[0] > 300) {
                            ConfigManagerCore.rocketFuelFactor = fuelFactor[0];
                            out.check(false, "the loader never filled the missile (" + m.fuelMb() + ")");
                            return finish(out, terrain);
                        }
                        return null;
                    }
                    ConfigManagerCore.rocketFuelFactor = fuelFactor[0];
                    out.check(true, "the fuel loader filled the tank with a mix");
                    phase[0] = 3;
                    return null;
                }
                if (phase[0] == 3) {
                    out.check(
                        w.getBlock(cx + 2, cy, cz) == GCBlocks.fuelLoader,
                        "the fuel loader stands beside the pad");
                    // What the loader asks of the docked entity before it pours: the RocketFuels test.
                    out.check(
                        MissileGc.accepts(m, "nitrofuel"),
                        "the loader's fuel test passes for RP-1-class fuel on a T1");
                    LaunchResult r = PadMissiles.launch(w, cx, cy, cz, cx + 0.5, cz + 70.5, null);
                    out.checkEquals("launch", LaunchResult.OK, r);
                    if (r != LaunchResult.OK) {
                        return finish(out, terrain);
                    }
                    launchedAt[0] = ticks;
                    phase[0] = 2;
                    return null;
                }
                // Phase 2: every tick of the flight, the pad and its cells must still be there.
                if (w.getBlock(cx, cy, cz) != GCBlocks.landingPadFull || !(pad instanceof TileEntityLandingPad)) {
                    out.check(false, "the pad was removed at tick " + (ticks - launchedAt[0]) + " after launch");
                    return finish(out, terrain);
                }
                for (int a = -1; a <= 1; a++) {
                    for (int b = -1; b <= 1; b++) {
                        if ((a != 0 || b != 0) && w.getBlock(cx + a, cy, cz + b) != GCBlocks.fakeBlock) {
                            out.check(false, "a pad cell is missing at " + a + ", " + b);
                            return finish(out, terrain);
                        }
                    }
                }
                if (w.getBlock(cx + 2, cy, cz) != GCBlocks.fuelLoader) {
                    out.check(false, "the fuel loader was removed");
                    return finish(out, terrain);
                }
                if (!m.isDead || target.removed(w) == 0 && ticks - launchedAt[0] < 850) {
                    return null;
                }
                out.check(true, "pad, loader and cells intact through launch and impact");
                out.check(target.removed(w) > 0, "the missile detonated on the target");
                return finish(out, terrain);
            });
        }

        private ScenarioResult fail(String message) {
            return ScenarioResult.fail(name(), message);
        }

        /** Tops up the loader's energy store; by reflection, because its class chain names Mekanism types. */
        private static void powerLoader(TileEntity loader) {
            try {
                Object storage = loader.getClass()
                    .getField("storage")
                    .get(loader);
                float cap = (Float) storage.getClass()
                    .getMethod("getCapacityGC")
                    .invoke(storage);
                storage.getClass()
                    .getMethod("setEnergyStored", float.class)
                    .invoke(storage, cap);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("cannot power the fuel loader", e);
            }
        }

        /** Fills the loader's tank; fails the scenario when the fluid is not registered or the loader is missing. */
        private static void fillLoader(ScenarioResult.Builder out, World w, int x, int y, int z, String fluidName) {
            TileEntity t = w.getTileEntity(x, y, z);
            Fluid f = FluidRegistry.getFluid(fluidName);
            out.check(f != null, "the test fluid " + fluidName + " is a registered fluid");
            out.check(t instanceof TileEntityFuelLoader, "a fuel loader stands where the test fills it");
            if (t instanceof TileEntityFuelLoader && f != null) {
                FluidTank tank = ((TileEntityFuelLoader) t).fuelTank;
                tank.setFluid(new FluidStack(f, tank.getCapacity()));
            }
        }

        private static int loaderMb(TileEntity t) {
            return t instanceof TileEntityFuelLoader ? ((TileEntityFuelLoader) t).fuelTank.getFluidAmount() : 0;
        }

        private ScenarioResult finish(ScenarioResult.Builder out, boolean terrain) {
            IcbmConfig.terrainDamageT1 = terrain;
            return out.build();
        }
    }
}
