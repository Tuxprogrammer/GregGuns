package com.gregguns.icbm.silo;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.IcbmAvailability;
import com.gregguns.icbm.compat.GalacticraftParts;
import com.gregguns.icbm.control.ControlStatus;
import com.gregguns.icbm.control.MTELaunchControl;
import com.gregguns.icbm.explosive.ExplosiveTier;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.LaunchResult;
import com.gregguns.icbm.missile.LaunchSite;
import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.warhead.WarheadBlocks;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadStacks;
import com.gregguns.machine.DevStructureBuilder;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;

/**
 * Scenarios of the Missile Silo (I3). The silo stands with its controller at {@code (0, CY, 0)} facing north, so the
 * shaft center is two blocks south of it. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (Tests)
public final class SiloScenarios {

    static final int CY = 2;
    /**
     * Layers the scenarios clear above the origin. A launch needs the whole climb column clear: 12 blocks of climb
     * plus the body (6 for a T3) above the pad, which stands about 0.2 above the origin, so the top checked cell
     * is origin + 18. Clearing 18 layers left that cell to the terrain of the slot (dirt at y 118 failed
     * icbm.silo.tiers once the slots shifted); this leaves headroom.
     */
    static final int CLEAR_HEIGHT = 24;
    static final int WARMUP = 50;
    static final int DISTANCE = 300;

    private SiloScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Structure());
        registry.register(new Tiers());
        registry.register(new Refusals());
        registry.register(new T3Launch());
        registry.register(new T3EmpLaunch());
        registry.register(new Fuels());
        registry.register(new PowerLoss());
        registry.register(new UnformedPad());
        registry.register(new HandStack());
        registry.register(new ShaftChunkScenario());
        LaunchSequenceScenarios.registerAll(registry);
        registry.register(new LaunchSequenceMpScenario());
        registry.register(new LaunchControlGuiShots());
        registry.register(new I4SiloSetup());
    }

    static String gate() {
        if (!ModuleState.isActive(ModuleId.ICBM) || !IcbmAvailability.galacticraftPresent()) {
            return "the ICBM module is off (Galacticraft missing)";
        }
        return null;
    }

    /** Clears the area, lays a stone floor, puts the controller down and builds the silo around it. */
    static DevStructureBuilder.Outcome build(ScenarioContext ctx, gregtech.api.enums.ItemList energy) {
        ctx.clearArea(14, CLEAR_HEIGHT);
        for (int a = -12; a <= 12; a++) {
            for (int b = -12; b <= 12; b++) {
                ctx.setBlock(a, CY - 3, b, Blocks.stone, 0);
            }
        }
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.MISSILE_SILO];
        ctx.setBlock(0, CY, 0, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
        TileEntity t = ctx.world()
            .getTileEntity(ctx.originX(), ctx.originY() + CY, ctx.originZ());
        ((IGregTechTileEntity) t).setInitialValuesAsNBT(null, (short) GregGunsMTEIDs.MISSILE_SILO);
        return DevStructureBuilder.buildSilo(ctx.world(), ctx.originX(), ctx.originY() + CY, ctx.originZ(), energy);
    }

    static IGregTechTileEntity tile(ScenarioContext ctx, int dx, int dy, int dz) {
        TileEntity t = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + CY + dy, ctx.originZ() + dz);
        return t instanceof IGregTechTileEntity ? (IGregTechTileEntity) t : null;
    }

    static MTEMissileSilo silo(ScenarioContext ctx) {
        IGregTechTileEntity b = tile(ctx, 0, 0, 0);
        return b != null && b.getMetaTileEntity() instanceof MTEMissileSilo ? (MTEMissileSilo) b.getMetaTileEntity()
            : null;
    }

    static boolean formed(ScenarioContext ctx) {
        return DevStructureBuilder.check(ctx.world(), ctx.originX(), ctx.originY() + CY, ctx.originZ());
    }

    static void power(ScenarioContext ctx) {
        IGregTechTileEntity energy = tile(ctx, 0, -1, 0);
        if (energy != null) {
            energy.increaseStoredEnergyUnits(50_000_000L, true);
        }
    }

    /** Puts 16,000 mB of a fluid in the fuel hatch; fails the scenario when the fluid is not registered. */
    static void fuel(ScenarioContext ctx, ScenarioResult.Builder out, String fluid) {
        IGregTechTileEntity hatch = tile(ctx, 1, -1, 0);
        Fluid f = FluidRegistry.getFluid(fluid);
        out.check(f != null, "the test fluid " + fluid + " is a registered fluid");
        out.check(
            hatch != null && hatch.getMetaTileEntity() instanceof MTEHatchInput,
            "the silo has a fuel hatch where the test fills it");
        if (hatch != null && f != null && hatch.getMetaTileEntity() instanceof MTEHatchInput) {
            ((MTEHatchInput) hatch.getMetaTileEntity()).setFillableStack(new FluidStack(f, 16000));
        }
    }

    /** @return how much the fuel hatch still holds, mB */
    private static int hatchMb(ScenarioContext ctx) {
        IGregTechTileEntity hatch = tile(ctx, 1, -1, 0);
        if (hatch == null || !(hatch.getMetaTileEntity() instanceof MTEHatchInput)) {
            return 0;
        }
        FluidStack fs = ((MTEHatchInput) hatch.getMetaTileEntity()).getFillableStack();
        return fs == null ? 0 : fs.amount;
    }

    private static void putBus(ScenarioContext ctx, ItemStack... stacks) {
        IGregTechTileEntity bus = tile(ctx, -1, 0, 0);
        for (int i = 0; i < stacks.length; i++) {
            bus.setInventorySlotContents(i, stacks[i]);
        }
    }

    /** @return how many stacks the input bus holds, wherever the bus keeps them */
    private static int busItems(ScenarioContext ctx) {
        IGregTechTileEntity bus = tile(ctx, -1, 0, 0);
        int n = 0;
        for (int i = 0; i < bus.getSizeInventory(); i++) {
            if (bus.getStackInSlot(i) != null) {
                n++;
            }
        }
        return n;
    }

    private static ItemStack warhead(WarheadCasing c, String explosive) {
        return WarheadStacks.make(net.minecraft.item.Item.getItemFromBlock(WarheadBlocks.of(c)), c, explosive, 1);
    }

    /** A missile standing on the silo pad the way the mating cycle makes it. */
    static EntityMissile stand(ScenarioContext ctx, MissileTier tier) {
        MTEMissileSilo s = silo(ctx);
        EntityMissile m = EntityMissile.create(tier, ctx.world());
        m.setLocationAndAngles(
            ctx.originX() + 0.5,
            ctx.originY() + CY - 2 + EntityMissile.PAD_SURFACE,
            ctx.originZ() + 2.5,
            0.0F,
            0.0F);
        m.configure(0, "condensed", 1, 1, null);
        ctx.world()
            .spawnEntityInWorld(m);
        return s == null ? null : m;
    }

    // ------------------------------------------------------------------------------------------------------------

    /** The silo forms with glass rings and the pad; hatches go in the glass; a hatch above the glass does not form. */
    private static final class Structure implements Scenario {

        @Override
        public String name() {
            return "icbm.silo.structure";
        }

        @Override
        public String description() {
            return "The Missile Silo forms from a bare controller with glass rings, a Galacticraft pad floor and hatches in the glass; other glass is refused; the walls take Light and Dark Concrete (mixed, not cobblestone or other blocks); a hatch in a wall layer forms it and a hatch in the rim, or a missing pad, does not.";
        }

        @Override
        public int areaRadius() {
            return 14;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            DevStructureBuilder.Outcome first = build(ctx, ItemList.Hatch_Energy_LuV);
            out.note("first attempt: " + first);
            int[] stage = { 0 };
            return ScenarioResult.pending(name(), 600, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                World w = ctx.world();
                int x = ctx.originX();
                int y = ctx.originY() + CY;
                int z = ctx.originZ();
                switch (stage[0]) {
                    case 0:
                        if (!formed(ctx) && ticks < WARMUP + 40) {
                            return null;
                        }
                        out.check(
                            formed(ctx),
                            "the silo forms with glass rings, the pad floor and hatches in the glass");
                        // The glass the builder placed (D115) sits in a pane of the middle ring; other glass is
                        // refused.
                        Block placed = w.getBlock(x - 2, y, z + 1);
                        int placedMeta = w.getBlockMetadata(x - 2, y, z + 1);
                        out.check(
                            SiloGlass.accepts(placed, placedMeta),
                            "the builder placed an accepted glass: " + Block.blockRegistry.getNameForObject(placed));
                        w.setBlock(x - 2, y, z + 1, Blocks.stained_glass, 3, 2);
                        out.check(!formed(ctx), "stained glass in a glass ring does not form the silo");
                        // Vanilla glass is accepted only when neither Tinkers' Construct nor EnderIO is loaded.
                        w.setBlock(x - 2, y, z + 1, Blocks.glass, 0, 2);
                        out.check(
                            formed(ctx) == SiloGlass.fallbackActive(),
                            "vanilla glass in a glass ring is accepted only when neither Tinkers' nor EnderIO is loaded");
                        w.setBlock(x - 2, y, z + 1, placed, placedMeta, 2);
                        out.check(formed(ctx), "the silo forms again with the accepted glass back");
                        // The walls are GT concrete (D116): the builder's Light Concrete, dark, mixed, and a finish.
                        Block conc = GregTechAPI.sBlockConcretes;
                        out.check(
                            w.getBlock(x + 1, y + 3, z) == conc && w.getBlockMetadata(x + 1, y + 3, z) == 8,
                            "the builder placed Light Concrete in the walls");
                        w.setBlock(x + 1, y + 3, z, conc, 0, 2);
                        w.setBlock(x - 1, y + 4, z, conc, 7, 2);
                        w.setBlock(x, y + 5, z + 4, conc, 14, 2);
                        out.check(
                            formed(ctx),
                            "Dark Concrete, Smooth Dark Concrete and Chiseled Light Concrete mix in the walls");
                        w.setBlock(x + 1, y + 3, z, conc, 1, 2);
                        out.check(!formed(ctx), "a cobblestone concrete wall cell does not form the silo");
                        w.setBlock(x + 1, y + 3, z, GregGunsCasings.INSTANCE, GregGunsCasings.GANTRY_CASING, 2);
                        out.check(!formed(ctx), "a Gantry Casing wall cell does not form the silo");
                        w.setBlock(x + 1, y + 3, z, Blocks.stone, 0, 2);
                        out.check(!formed(ctx), "a stone wall cell does not form the silo");
                        w.setBlock(x + 1, y + 3, z, conc, 8, 2);
                        out.check(formed(ctx), "the silo forms again with Light Concrete back");
                        // D117: a bus in a wall layer (layer 8) forms; a bus in the rim does not.
                        int hatchId = ItemList.Hatch_Input_Bus_HV.get(1)
                            .getItemDamage();
                        w.setBlock(x - 1, y + 2, z, GregTechAPI.sBlockMachines, hatchId, 3);
                        ((IGregTechTileEntity) w.getTileEntity(x - 1, y + 2, z))
                            .setInitialValuesAsNBT(null, (short) hatchId);
                        stage[0] = 1;
                        return null;
                    case 1:
                        out.check(formed(ctx), "a hatch in a wall layer forms the silo (D117)");
                        w.setBlock(x - 1, y + 2, z, GregTechAPI.sBlockConcretes, 8, 2);
                        int rimId = ItemList.Hatch_Input_Bus_HV.get(1)
                            .getItemDamage();
                        w.setBlock(x - 1, y + 9, z, GregTechAPI.sBlockMachines, rimId, 3);
                        ((IGregTechTileEntity) w.getTileEntity(x - 1, y + 9, z))
                            .setInitialValuesAsNBT(null, (short) rimId);
                        stage[0] = 3;
                        return null;
                    case 3:
                        out.check(!formed(ctx), "a hatch in the rim does not form the silo (D117)");
                        w.setBlock(x - 1, y + 9, z, GregGunsCasings.INSTANCE, GregGunsCasings.GANTRY_BEAM, 2);
                        out.check(formed(ctx), "the silo forms again once the beam is back");
                        // No pad in the shaft floor.
                        Block pad = GalacticraftParts.findBlock(GalacticraftParts.PAD);
                        Block full = GalacticraftParts.findBlock(GalacticraftParts.PAD_FULL);
                        out.check(pad != null && full != null, "Galacticraft's pad blocks resolve by registry name");
                        Block center = w.getBlock(x, y - 2, z + 2);
                        out.check(center == pad || center == full, "the shaft floor center is a launch pad");
                        w.setBlock(x, y - 2, z + 2, Blocks.stone, 0, 2);
                        stage[0] = 2;
                        return null;
                    default:
                        out.check(!formed(ctx), "the silo does not form without the pad");
                        ctx.clearArea(14, CLEAR_HEIGHT);
                        return out.build();
                }
            });
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    /** The Energy Hatch tier sets the missiles the silo launches, and the silo's factors apply (D70). */
    private static final class Tiers implements Scenario {

        @Override
        public String name() {
            return "icbm.silo.tiers";
        }

        @Override
        public String description() {
            return "An HV hatch launches a T1 and refuses a T3 with the silo-tier status; a LuV hatch launches a T3; the silo's range and scatter factors apply to the same missile and a pad keeps the plain numbers.";
        }

        @Override
        public int areaRadius() {
            return 14;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            build(ctx, ItemList.Hatch_Energy_HV);
            int[] stage = { 0 };
            EntityMissile[] m = new EntityMissile[1];
            return ScenarioResult.pending(name(), 800, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                int x = ctx.originX();
                int z = ctx.originZ();
                switch (stage[0]) {
                    case 0: {
                        out.check(formed(ctx), "the HV silo forms");
                        LaunchSite site = silo(ctx).site();
                        m[0] = stand(ctx, MissileTier.T3);
                        m[0].fill(m[0].capacityMb(), true);
                        out.checkEquals(
                            "a T3 missile stands in the HV silo",
                            MissileTier.T3,
                            site.status()
                                .tier());
                        out.checkEquals("an HV hatch holds back a T3", LaunchResult.SILO_TIER, site.hold());
                        out.checkEquals(
                            "an HV hatch refuses a T3 launch",
                            LaunchResult.SILO_TIER,
                            site.launch(x + 0.5, z + DISTANCE, null));
                        out.check(m[0].onPad(), "the refused T3 stays in the shaft");
                        m[0].setDead();
                        m[0] = stand(ctx, MissileTier.T1);
                        m[0].fill(m[0].capacityMb(), true);
                        out.checkEquals("an HV hatch has no objection to a T1", LaunchResult.OK, site.hold());
                        // The silo's factors against the plain numbers of the same missile (the pad's).
                        double plain = m[0].maxRange();
                        out.checkEquals(
                            "range from the silo is the factor times the pad range",
                            (int) Math.round(
                                MissileTier.T1.baseMaxRange() * IcbmConfig.missileRangeFactor
                                    * IcbmConfig.siloRangeFactor),
                            site.maxRange());
                        out.check(site.maxRange() > plain, "the silo reaches farther than a pad");
                        out.check(
                            com.gregguns.icbm.missile.Scatter
                                .halfWidth(12.0, 0, 100.0, 1000.0, IcbmConfig.siloScatterFactor)
                                < com.gregguns.icbm.missile.Scatter.halfWidth(12.0, 0, 100.0, 1000.0),
                            "the silo scatters less than a pad");
                        out.checkEquals(
                            "an HV hatch launches a T1",
                            LaunchResult.OK,
                            site.launch(x + 0.5, z + 100, null));
                        out.checkEquals(
                            "the launched missile carries the silo's scatter factor",
                            IcbmConfig.siloScatterFactor,
                            m[0].siteScatter());
                        out.checkEquals(
                            "the launched missile carries the silo's range factor",
                            IcbmConfig.siloRangeFactor,
                            m[0].siteRange());
                        net.minecraft.nbt.NBTTagCompound saved = new net.minecraft.nbt.NBTTagCompound();
                        m[0].writeToNBT(saved);
                        out.checkEquals(
                            "the scatter factor is saved with the missile",
                            IcbmConfig.siloScatterFactor,
                            saved.getDouble("siteScatter"));
                        out.checkEquals(
                            "the range factor is saved with the missile",
                            IcbmConfig.siloRangeFactor,
                            saved.getDouble("siteRange"));
                        EntityMissile loaded = EntityMissile.create(MissileTier.T1, ctx.world());
                        loaded.readFromNBT(saved);
                        out.checkEquals(
                            "a reloaded missile carries the scatter factor",
                            IcbmConfig.siloScatterFactor,
                            loaded.siteScatter());
                        out.checkEquals(
                            "a reloaded missile carries the range factor",
                            IcbmConfig.siloRangeFactor,
                            loaded.siteRange());
                        m[0].setDead();
                        // Now a LuV hatch.
                        DevStructureBuilder.buildSilo(ctx.world(), x, ctx.originY() + CY, z, ItemList.Hatch_Energy_LuV);
                        stage[0] = 1;
                        return null;
                    }
                    case 1: {
                        if (ticks < WARMUP + 60) {
                            return null;
                        }
                        out.check(formed(ctx), "the LuV silo forms");
                        LaunchSite site = silo(ctx).site();
                        m[0] = stand(ctx, MissileTier.T3);
                        m[0].fill(m[0].capacityMb(), true);
                        out.checkEquals("a LuV hatch has no objection to a T3", LaunchResult.OK, site.hold());
                        out.checkEquals(
                            "a LuV hatch launches a T3",
                            LaunchResult.OK,
                            site.launch(x + 0.5, z + DISTANCE, null));
                        out.check(!m[0].onPad() || m[0].isDead, "the T3 left the shaft");
                        m[0].setDead();
                        ctx.clearArea(14, CLEAR_HEIGHT);
                        return out.build();
                    }
                    default:
                        return null;
                }
            });
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    /** Wrong cargo and wrong fuel are refused and nothing is lost. */
    private static final class Refusals implements Scenario {

        @Override
        public String name() {
            return "icbm.silo.refusals";
        }

        @Override
        public String description() {
            return "The silo does not mate a body without a warhead or with one too heavy for it, keeps the items in the bus, and does not pump a fluid the missile does not accept.";
        }

        @Override
        public int areaRadius() {
            return 14;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            build(ctx, ItemList.Hatch_Energy_LuV);
            int[] stage = { 0 };
            int[] mark = { 0 };
            EntityMissile[] m = new EntityMissile[1];
            return ScenarioResult.pending(name(), 900, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                switch (stage[0]) {
                    case 0:
                        out.check(formed(ctx), "the silo forms");
                        power(ctx);
                        putBus(ctx, MissileBodies.body(MissileTier.T3));
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        power(ctx);
                        if (ticks < mark[0] + 100) {
                            return null;
                        }
                        out.check(silo(ctx).missile() == null, "no warhead, no mating");
                        out.check(tile(ctx, -1, 0, 0).getStackInSlot(0) != null, "the body stays in the bus");
                        // A T1 body with a T2 warhead.
                        putBus(ctx, MissileBodies.body(MissileTier.T1), warhead(WarheadCasing.T2, "thermobaric"));
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        power(ctx);
                        if (ticks < mark[0] + 100) {
                            return null;
                        }
                        out.check(silo(ctx).missile() == null, "a warhead heavier than the body is not mated");
                        out.check(
                            tile(ctx, -1, 0, 0).getStackInSlot(0) != null
                                && tile(ctx, -1, 0, 0).getStackInSlot(1) != null,
                            "both items stay in the bus");
                        putBus(ctx, (ItemStack) null, (ItemStack) null);
                        // Wrong fuel into a T3 standing in the shaft.
                        m[0] = stand(ctx, MissileTier.T3);
                        fuel(ctx, out, "water");
                        mark[0] = ticks;
                        stage[0] = 3;
                        return null;
                    case 3:
                        power(ctx);
                        if (ticks < mark[0] + 100) {
                            return null;
                        }
                        out.checkEquals("water is not pumped", 0, m[0].fuelMb());
                        out.checkEquals("the hatch kept its water", 16000, hatchMb(ctx));
                        fuel(ctx, out, "nitrofuel");
                        mark[0] = ticks;
                        stage[0] = 4;
                        return null;
                    case 4:
                        power(ctx);
                        if (ticks < mark[0] + 100) {
                            return null;
                        }
                        out.checkEquals("a T1 fuel is not pumped into a T3", 0, m[0].fuelMb());
                        out.checkEquals("the hatch kept its T1 fuel", 16000, hatchMb(ctx));
                        IGregTechTileEntity none = tile(ctx, 1, -1, 0);
                        ((MTEHatchInput) none.getMetaTileEntity()).setFillableStack(null);
                        out.checkEquals(
                            "no fuel, no launch",
                            LaunchResult.NOT_FUELED,
                            silo(ctx).site()
                                .launch(ctx.originX() + 0.5, ctx.originZ() + DISTANCE, null));
                        m[0].setDead();
                        ctx.clearArea(14, CLEAR_HEIGHT);
                        return out.build();
                    default:
                        return null;
                }
            });
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    /** The exit box: the silo forms, loads, fuels and launches a T3 missile without the player touching it. */
    private static final class T3Launch implements Scenario {

        @Override
        public String name() {
            return "icbm.silo.t3_launch";
        }

        @Override
        public String description() {
            return "From a bus holding a T3 body and a warhead and a hatch holding Purple fuel, the silo mates, fuels and, fired by a Launch Control, launches a T3 missile that detonates on target; the silo is intact afterwards.";
        }

        @Override
        public int areaRadius() {
            return 14;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            DevStructureBuilder.Outcome built = build(ctx, ItemList.Hatch_Energy_LuV);
            out.note("build: " + built);
            // The Launch Control, in the silo's reach.
            int id = GregGunsMTEIDs.LAUNCH_CONTROL;
            ctx.setBlock(
                5,
                CY,
                -2,
                GregTechAPI.sBlockMachines,
                GregTechAPI.METATILEENTITIES[id].getTileEntityBaseType());
            IGregTechTileEntity cb = (IGregTechTileEntity) ctx.world()
                .getTileEntity(ctx.originX() + 5, ctx.originY() + CY, ctx.originZ() - 2);
            cb.setInitialValuesAsNBT(null, (short) id);
            cb.setFrontFacing(ForgeDirection.NORTH);
            cb.setOwnerName("gregguns-test");
            cb.setOwnerUuid(UUID.nameUUIDFromBytes("gregguns-test".getBytes(StandardCharsets.UTF_8)));
            WarheadCasing casing = WarheadCasing.T1;
            for (WarheadCasing c : WarheadCasing.values()) {
                if (c.tier()
                    .number() <= ExplosiveTier.T3.number()) {
                    casing = c;
                }
            }
            final WarheadCasing used = casing;
            final String explosive = casing == WarheadCasing.T1 ? "condensed"
                : casing == WarheadCasing.T2 ? "thermobaric" : "exothermic";
            final com.gregguns.icbm.missile.MissileScenarios.Target target = new com.gregguns.icbm.missile.MissileScenarios.Target(
                ctx,
                0,
                DISTANCE,
                9);
            final boolean terrain = IcbmConfig.terrainDamageT1;
            final boolean terrainT3 = IcbmConfig.terrainDamageT3;
            IcbmConfig.terrainDamageT1 = true;
            IcbmConfig.terrainDamageT3 = true;
            int[] stage = { 0 };
            int[] mark = { 0 };
            int[] died = { 0 };
            int[] blocksBefore = { 0 };
            EntityMissile[] m = new EntityMissile[1];
            return ScenarioResult.pending(name(), 6000, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                IGregTechTileEntity cbase = tile(ctx, 5, 0, -2);
                MTELaunchControl c = (MTELaunchControl) cbase.getMetaTileEntity();
                power(ctx);
                if (stage[0] < 3) {
                    fuel(ctx, out, "fluid.rocketfuelmixc");
                }
                switch (stage[0]) {
                    case 0:
                        out.check(formed(ctx), "the silo forms");
                        blocksBefore[0] = countBlocks(ctx);
                        // The player puts a body and a warhead in the bus and nothing else.
                        putBus(ctx, MissileBodies.body(MissileTier.T3), warhead(used, explosive));
                        cbase.increaseStoredEnergyUnits(MTELaunchControl.EU_PER_SHOT * 5L, true);
                        c.setXText(Integer.toString(ctx.originX()));
                        c.setZText(Integer.toString(ctx.originZ() + DISTANCE));
                        out.checkEquals("nothing to fire yet", ControlStatus.NO_MISSILE, c.fire(null));
                        stage[0] = 1;
                        return null;
                    case 1:
                        m[0] = silo(ctx).missile();
                        if (m[0] == null) {
                            return ticks > 1500 ? fail(terrain, out, "the silo never mated the missile") : null;
                        }
                        out.checkEquals("a T3 missile was mated", MissileTier.T3, m[0].tier());
                        out.check(m[0].onPad(), "it stands in the shaft");
                        out.checkEquals("the bus is empty", null, tile(ctx, -1, 0, 0).getStackInSlot(0));
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        if (m[0].fuelMb() < m[0].capacityMb()) {
                            if (ticks > mark[0] + 1500) {
                                return fail(terrain, out, "the silo never fueled the missile");
                            }
                            out.checkEquals("an unfueled missile is not fired", ControlStatus.NOT_FUELED, c.fire(null));
                            return null;
                        }
                        out.checkEquals("the tank is full", m[0].capacityMb(), m[0].fuelMb());
                        out.checkEquals("ready", ControlStatus.READY, c.currentStatus());
                        out.checkEquals("the Launch Control fires the silo", ControlStatus.FIRED, c.fire(null));
                        mark[0] = ticks;
                        stage[0] = 3;
                        return null;
                    case 3:
                        if (!m[0].isDead && ticks < mark[0] + 3000) {
                            return null;
                        }
                        if (m[0].isDead && died[0] == 0) {
                            died[0] = ticks;
                        }
                        // The crater is dug over several ticks (D87), so wait for the first broken block.
                        if (m[0].isDead && target.removed(ctx.world()) == 0 && ticks < died[0] + 400) {
                            return null;
                        }
                        out.check(m[0].isDead, "the missile detonated");
                        out.check(
                            target.removed(ctx.world()) > 0,
                            "the blast broke blocks on the target (the missile ended at " + (int) m[0].posX
                                + ", "
                                + (int) m[0].posY
                                + ", "
                                + (int) m[0].posZ
                                + ", aimed at "
                                + java.util.Arrays.toString(m[0].aimPoint())
                                + "; platform "
                                + target.standing(ctx.world())
                                + " of "
                                + 19 * 19
                                + " stone)");
                        out.checkEquals(
                            "no chunk tickets are left",
                            0,
                            m[0].tickets()
                                .tickets());
                        out.check(formed(ctx), "the silo is still formed after its own launch");
                        out.checkEquals("no block of the silo was damaged", blocksBefore[0], countBlocks(ctx));
                        IcbmConfig.terrainDamageT1 = terrain;
                        IcbmConfig.terrainDamageT3 = terrainT3;
                        ctx.clearArea(14, CLEAR_HEIGHT);
                        return out.build();
                    default:
                        return null;
                }
            });
        }

        private static ScenarioResult fail(boolean terrain, ScenarioResult.Builder out, String why) {
            IcbmConfig.terrainDamageT1 = terrain;
            IcbmConfig.terrainDamageT3 = true;
            return ScenarioResult.fail("icbm.silo.t3_launch", why);
        }

        private static int countBlocks(ScenarioContext ctx) {
            int n = 0;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -1; dz <= 6; dz++) {
                    for (int dy = -3; dy <= 12; dy++) {
                        if (ctx.world()
                            .getBlock(ctx.originX() + dx, ctx.originY() + CY + dy, ctx.originZ() + dz) != Blocks.air) {
                            n++;
                        }
                    }
                }
            }
            return n;
        }
    }

    /**
     * Exit box (D80): the same silo launch as {@link T3Launch}, carrying the EMP warhead over a field of charged
     * battery
     * buffers at the target: the warhead drains the nearest devices within the cap, touches no terrain, and the
     * silo is intact afterwards.
     */
    private static final class T3EmpLaunch implements Scenario {

        /** Buffers sit on a 5 x 5 grid, 8 blocks apart, so every chunk the scattered impact can land in holds some. */
        private static final int GRID = 2;

        @Override
        public String name() {
            return "icbm.silo.t3_emp_launch";
        }

        @Override
        public String description() {
            return "A silo-launched T3 missile carrying the full-fill EMP warhead drains every charged battery buffer in the chunk it lands in (a pulse never loads a chunk), and leaves those buffers standing.";
        }

        @Override
        public int areaRadius() {
            return 14;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            DevStructureBuilder.Outcome built = build(ctx, ItemList.Hatch_Energy_LuV);
            out.note("build: " + built);
            IGregTechTileEntity cb = launchControl(ctx, 5, -2);
            final net.minecraft.world.World w = ctx.world();
            final int tx = ctx.originX();
            final int ty = ctx.originY();
            final int tz = ctx.originZ() + DISTANCE;
            // The context only holds its own area, so the far chunks are held by hand and the floor and the buffers go
            // in directly. The
            // impact scatters, and a pulse never loads a chunk, so the buffers cover every chunk around the target.
            // The chunks are held for the whole run: an unheld far chunk unloads and takes the machines with it.
            com.gregguns.icbm.missile.MissileScenarios.holdFar(ctx, tx, tz, GRID * 8 + 8);
            final java.util.List<int[]> spots = new java.util.ArrayList<>();
            for (int a = -GRID; a <= GRID; a++) {
                for (int b = -GRID; b <= GRID; b++) {
                    int x = tx + a * 8;
                    int z = tz + b * 8;
                    w.setBlock(x, ty - 1, z, Blocks.stone, 0, 3);
                    spots.add(new int[] { x, ty, z });
                }
            }
            final int buffer = ItemList.Battery_Buffer_1by1_MV.get(1)
                .getItemDamage();
            for (int[] p : spots) {
                com.gregguns.icbm.emp.EmpScenarios.placeAbs(w, p[0], p[1], p[2], buffer);
            }
            final boolean terrainT3 = IcbmConfig.terrainDamageT3;
            IcbmConfig.terrainDamageT3 = true;
            int[] stage = { 0 };
            int[] mark = { 0 };
            int[] blocksBefore = { 0 };
            EntityMissile[] m = new EntityMissile[1];
            return ScenarioResult.pending(name(), 6000, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                IGregTechTileEntity cbase = tile(ctx, 5, 0, -2);
                MTELaunchControl c = (MTELaunchControl) cbase.getMetaTileEntity();
                power(ctx);
                if (stage[0] < 3) {
                    fuel(ctx, out, "fluid.rocketfuelmixc");
                }
                switch (stage[0]) {
                    case 0:
                        out.check(formed(ctx), "the silo forms");
                        blocksBefore[0] = countBlocks(ctx);
                        for (int[] p : spots) {
                            net.minecraft.tileentity.TileEntity t = w.getTileEntity(p[0], p[1], p[2]);
                            if (!(t instanceof gregtech.api.metatileentity.BaseMetaTileEntity)) {
                                return fail(terrainT3, "a battery buffer at the target is missing");
                            }
                            ((gregtech.api.metatileentity.BaseMetaTileEntity) t).setStoredEU(10_000L);
                        }
                        putBus(ctx, MissileBodies.body(MissileTier.T3), empWarhead());
                        cbase.increaseStoredEnergyUnits(MTELaunchControl.EU_PER_SHOT * 5L, true);
                        c.setXText(Integer.toString(ctx.originX()));
                        c.setZText(Integer.toString(ctx.originZ() + DISTANCE));
                        stage[0] = 1;
                        return null;
                    case 1:
                        m[0] = silo(ctx).missile();
                        if (m[0] == null) {
                            return ticks > 1500 ? fail(terrainT3, "the silo never mated the missile") : null;
                        }
                        out.checkEquals("it carries the EMP warhead", "emp", m[0].explosiveId());
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        if (m[0].fuelMb() < m[0].capacityMb()) {
                            return ticks > mark[0] + 1500 ? fail(terrainT3, "the silo never fueled the missile") : null;
                        }
                        out.checkEquals("the Launch Control fires the silo", ControlStatus.FIRED, c.fire(null));
                        mark[0] = ticks;
                        stage[0] = 3;
                        return null;
                    case 3:
                        if (!m[0].isDead && ticks < mark[0] + 3000) {
                            return null;
                        }
                        out.check(m[0].isDead, "the missile detonated");
                        // Where it went off: the chunk of the missile's last position holds every buffer a pulse could
                        // reach.
                        int cx = net.minecraft.util.MathHelper.floor_double(m[0].posX) >> 4;
                        int cz = net.minecraft.util.MathHelper.floor_double(m[0].posZ) >> 4;
                        int inChunk = 0;
                        int empty = 0;
                        int standing = 0;
                        for (int[] p : spots) {
                            if ((p[0] >> 4) != cx || (p[2] >> 4) != cz) {
                                continue;
                            }
                            inChunk++;
                            net.minecraft.tileentity.TileEntity t = w.getTileEntity(p[0], p[1], p[2]);
                            if (t instanceof IGregTechTileEntity) {
                                standing++;
                                if (((IGregTechTileEntity) t).getStoredEU() == 0L) {
                                    empty++;
                                }
                            }
                        }
                        out.check(inChunk > 0, "the grid covers the chunk the missile landed in");
                        out.checkEquals("every buffer in that chunk was drained", inChunk, empty);
                        out.checkEquals("the EMP left those buffers standing", inChunk, standing);
                        out.check(formed(ctx), "the silo is still formed after its own launch");
                        out.checkEquals("no block of the silo was damaged", blocksBefore[0], countBlocks(ctx));
                        IcbmConfig.terrainDamageT3 = terrainT3;
                        ctx.clearArea(14, CLEAR_HEIGHT);
                        return out.build();
                    default:
                        return null;
                }
            });
        }

        private static ItemStack empWarhead() {
            return WarheadStacks.make(
                net.minecraft.item.Item.getItemFromBlock(WarheadBlocks.of(WarheadCasing.T3)),
                WarheadCasing.T3,
                "emp",
                64);
        }

        private ScenarioResult fail(boolean terrainT3, String why) {
            IcbmConfig.terrainDamageT3 = terrainT3;
            return ScenarioResult.fail(name(), why);
        }

        private static int countBlocks(ScenarioContext ctx) {
            int n = 0;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -1; dz <= 6; dz++) {
                    for (int dy = -3; dy <= 12; dy++) {
                        if (ctx.world()
                            .getBlock(ctx.originX() + dx, ctx.originY() + CY + dy, ctx.originZ() + dz) != Blocks.air) {
                            n++;
                        }
                    }
                }
            }
            return n;
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    static IGregTechTileEntity launchControl(ScenarioContext ctx, int dx, int dz) {
        int id = GregGunsMTEIDs.LAUNCH_CONTROL;
        ctx.setBlock(dx, CY, dz, GregTechAPI.sBlockMachines, GregTechAPI.METATILEENTITIES[id].getTileEntityBaseType());
        IGregTechTileEntity cb = (IGregTechTileEntity) ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + CY, ctx.originZ() + dz);
        cb.setInitialValuesAsNBT(null, (short) id);
        cb.setFrontFacing(ForgeDirection.NORTH);
        cb.setOwnerName("gregguns-test");
        cb.setOwnerUuid(UUID.nameUUIDFromBytes("gregguns-test".getBytes(StandardCharsets.UTF_8)));
        return cb;
    }

    /** Every fuel a tier lists is a registered fluid, so no fluid check in the suite can pass for the wrong reason. */
    private static final class Fuels implements Scenario {

        @Override
        public String name() {
            return "icbm.silo.fuels";
        }

        @Override
        public String description() {
            return "Every fluid in every missile tier's fuel list is a registered fluid in this installation, and so are the fluids the refusal checks use.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!ModuleState.isActive(ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), "the ICBM module is off");
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (MissileTier t : MissileTier.values()) {
                for (String fuel : t.fuels()) {
                    out.check(FluidRegistry.isFluidRegistered(fuel), t + " fuel " + fuel + " is a registered fluid");
                }
            }
            for (String test : new String[] { "water", "fuel", "nitrofuel", "fluid.rocketfuelmixb",
                "fluid.rocketfuelmixc" }) {
                out.check(FluidRegistry.isFluidRegistered(test), "test fluid " + test + " is registered");
            }
            return out.build();
        }
    }

    /** A mating cycle cut short by a power loss resumes with the parts it already took. */
    private static final class PowerLoss implements Scenario {

        @Override
        public String name() {
            return "icbm.silo.power_loss";
        }

        @Override
        public String description() {
            return "Cutting the power during a mating cycle keeps the body and warhead pending; when the power and the machine are back the same parts finish as a missile and the next parts in the bus are untouched.";
        }

        @Override
        public int areaRadius() {
            return 14;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            build(ctx, ItemList.Hatch_Energy_LuV);
            int[] stage = { 0 };
            int[] mark = { 0 };
            return ScenarioResult.pending(name(), 3000, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                MTEMissileSilo s = silo(ctx);
                IGregTechTileEntity energy = tile(ctx, 0, -1, 0);
                switch (stage[0]) {
                    case 0:
                        out.check(formed(ctx), "the silo forms");
                        power(ctx);
                        putBus(ctx, MissileBodies.body(MissileTier.T1), warhead(WarheadCasing.T1, "condensed"));
                        stage[0] = 1;
                        return null;
                    case 1:
                        power(ctx);
                        if (s.pendingTier() == 0) {
                            return ticks > 400 ? ScenarioResult.fail(name(), "the cycle never started") : null;
                        }
                        out.checkEquals("the cycle took the body", null, tile(ctx, -1, 0, 0).getStackInSlot(0));
                        out.checkEquals("the cycle took the warhead", null, tile(ctx, -1, 0, 0).getStackInSlot(1));
                        // The next parts wait in the bus; they must still be there at the end.
                        putBus(
                            ctx,
                            (ItemStack) null,
                            (ItemStack) null,
                            MissileBodies.body(MissileTier.T1),
                            warhead(WarheadCasing.T1, "condensed"));
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        energy.decreaseStoredEnergyUnits(Long.MAX_VALUE / 4, true);
                        if (ticks < mark[0] + 60) {
                            return null;
                        }
                        out.check(s.missile() == null, "no missile while the power is out");
                        out.checkEquals("the parts stay pending", MissileTier.T1.number(), s.pendingTier());
                        out.check(
                            !s.getBaseMetaTileEntity()
                                .isActive(),
                            "the machine stopped");
                        s.getBaseMetaTileEntity()
                            .enableWorking();
                        mark[0] = ticks;
                        stage[0] = 3;
                        return null;
                    case 3:
                        power(ctx);
                        if (s.missile() == null) {
                            return ticks > mark[0] + 1500 ? ScenarioResult.fail(name(), "the cycle never resumed")
                                : null;
                        }
                        out.checkEquals(
                            "the resumed cycle made a T1",
                            MissileTier.T1,
                            s.missile()
                                .tier());
                        out.checkEquals("nothing is pending any more", 0, s.pendingTier());
                        out.checkEquals("the resume took no new parts from the bus", 2, busItems(ctx));
                        s.missile()
                            .setDead();
                        ctx.clearArea(14, CLEAR_HEIGHT);
                        return out.build();
                    default:
                        return null;
                }
            });
        }
    }

    /** An unformed silo's floor is a plain pad: it must not fire a silo-only missile (D8 step 5, D70). */
    private static final class UnformedPad implements Scenario {

        @Override
        public String name() {
            return "icbm.silo.unformed_pad";
        }

        @Override
        public String description() {
            return "With a T3 standing in a silo whose structure is broken, the Launch Control and the pad API both refuse with the silo-only status and the missile stays; a T1 on the same floor still fires from the pad.";
        }

        @Override
        public int areaRadius() {
            return 14;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            build(ctx, ItemList.Hatch_Energy_LuV);
            // Beside the shaft floor pad (pad reach 4), outside the silo.
            IGregTechTileEntity cb = launchControl(ctx, 3, 0);
            int[] stage = { 0 };
            EntityMissile[] m = new EntityMissile[1];
            return ScenarioResult.pending(name(), 800, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                World w = ctx.world();
                int x = ctx.originX();
                int y = ctx.originY() + CY;
                int z = ctx.originZ();
                MTELaunchControl c = (MTELaunchControl) cb.getMetaTileEntity();
                switch (stage[0]) {
                    case 0: {
                        out.check(formed(ctx), "the silo forms");
                        m[0] = stand(ctx, MissileTier.T3);
                        m[0].fill(m[0].capacityMb(), true);
                        int hatchId = ItemList.Hatch_Input_Bus_HV.get(1)
                            .getItemDamage();
                        w.setBlock(x - 1, y + 9, z, GregTechAPI.sBlockMachines, hatchId, 3);
                        ((IGregTechTileEntity) w.getTileEntity(x - 1, y + 9, z))
                            .setInitialValuesAsNBT(null, (short) hatchId);
                        stage[0] = 1;
                        return null;
                    }
                    case 1: {
                        if (ticks < WARMUP + 60) {
                            return null;
                        }
                        out.check(!formed(ctx), "the silo no longer forms");
                        cb.increaseStoredEnergyUnits(MTELaunchControl.EU_PER_SHOT * 5L, true);
                        c.setXText(Integer.toString(x));
                        c.setZText(Integer.toString(z + DISTANCE));
                        out.checkEquals(
                            "the pad API refuses a silo-only missile",
                            LaunchResult.SILO_ONLY,
                            com.gregguns.icbm.missile.PadMissiles
                                .launch(w, x, y - 2, z + 2, x + 0.5, z + DISTANCE, null));
                        out.checkEquals(
                            "the Launch Control refuses it with the silo-only status",
                            ControlStatus.SILO_ONLY,
                            c.fire(null));
                        out.check(m[0].onPad() && !m[0].isDead, "the T3 still stands on the floor");
                        m[0].setDead();
                        ctx.clearArea(14, CLEAR_HEIGHT);
                        return out.build();
                    }
                    default:
                        return null;
                }
            });
        }
    }

    /** A body clicked onto the pad with the item, not spawned as an entity (D87). */
    private static final class HandStack implements Scenario {

        @Override
        public String name() {
            return "icbm.silo.hand_stack";
        }

        @Override
        public String description() {
            return "A T3 body item placed on a formed silo's floor goes up as a body block, and the silo reports it as not armed; on a free-standing Galacticraft pad the same item is refused.";
        }

        @Override
        public int areaRadius() {
            return 14;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            build(ctx, ItemList.Hatch_Energy_LuV);
            // A free-standing pad, nine single pads that turn into a full one.
            for (int a = 8; a <= 10; a++) {
                for (int b = -9; b <= -7; b++) {
                    ctx.setBlock(a, CY - 2, b, micdoodle8.mods.galacticraft.core.blocks.GCBlocks.landingPad, 0);
                }
            }
            return ScenarioResult.pending(name(), 400, ticks -> {
                if (ticks < WARMUP + 40) {
                    return null;
                }
                World w = ctx.world();
                int x = ctx.originX();
                int y = ctx.originY() + CY;
                int z = ctx.originZ();
                net.minecraft.entity.player.EntityPlayer p = net.minecraftforge.common.util.FakePlayerFactory
                    .getMinecraft((net.minecraft.world.WorldServer) w);
                out.check(formed(ctx), "the silo forms");
                ItemStack free = MissileBodies.body(MissileTier.T3);
                boolean freeStanding = free.getItem()
                    .onItemUse(free, p, w, x + 9, y - 2, z - 8, 1, 0.5F, 1.0F, 0.5F);
                out.check(!freeStanding, "a T3 body is refused on a free-standing pad");
                ItemStack in = MissileBodies.body(MissileTier.T3);
                boolean placed = in.getItem()
                    .onItemUse(in, p, w, x, y - 2, z + 2, 1, 0.5F, 1.0F, 0.5F);
                out.check(placed, "a T3 body is placed on the silo's floor");
                out.check(
                    w.getBlock(x, y - 1, z + 2) instanceof com.gregguns.icbm.missile.BlockMissileBody,
                    "a body block stands in the shaft");
                LaunchSite site = silo(ctx).site();
                out.checkEquals(
                    "the silo reports the stacked T3 as not armed",
                    MissileTier.T3,
                    site.status()
                        .tier());
                out.checkEquals(
                    "and it cannot be fired yet",
                    LaunchResult.NOT_ARMED,
                    site.launch(x + 0.5, z + DISTANCE, null));
                ctx.clearArea(14, CLEAR_HEIGHT);
                return out.build();
            });
        }
    }
}
