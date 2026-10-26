package com.gregguns.icbm.control;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.machine.GregGunsMTEIDs;
import com.mojang.authlib.GameProfile;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;

/**
 * Headless scenarios of the Launch Control: the machine is registered and ticks, the pad is found, the pre-check says
 * the right thing, a data stick carries a target, and redstone fires once per rising edge. The full launch with a real
 * missile is {@code icbm.launch.control_to_impact} in the missile milestone's scenarios. Server side.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md, launch-sequence.md
public final class ControlScenarios {

    /** GT refuses use of a machine younger than this many ticks (BaseMetaTileEntity.isUseableByPlayer). */
    private static final int WARMUP_TICKS = 45;

    private ControlScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Registered());
        registry.register(new PadAndGate());
        registry.register(new Stick());
        registry.register(new Redstone());
        registry.register(new com.gregguns.icbm.missile.ControlLaunchScenario());
    }

    // --- helpers ----------------------------------------------------------------------------------------------------

    private static final class TestPlayer extends FakePlayer {

        TestPlayer(WorldServer world, String name) {
            super(world, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        }
    }

    private static IGregTechTileEntity place(ScenarioContext ctx, int dx, int dy, int dz) {
        int id = GregGunsMTEIDs.LAUNCH_CONTROL;
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

    /** The control block as the world holds it now; GT may replace the tile entity in its first ticks. */
    private static IGregTechTileEntity live(ScenarioContext ctx, int dx, int dy, int dz) {
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
        return tile instanceof IGregTechTileEntity ? (IGregTechTileEntity) tile : null;
    }

    private static MTELaunchControl mte(IGregTechTileEntity base) {
        return base != null && base.getMetaTileEntity() instanceof MTELaunchControl
            ? (MTELaunchControl) base.getMetaTileEntity()
            : null;
    }

    /** A Galacticraft pad (center plus eight) whose center is at the offset. */
    private static void pad(ScenarioContext ctx, int cx, int cy, int cz) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                boolean center = x == 0 && z == 0;
                ctx.setBlock(cx + x, cy, cz + z, center ? GCBlocks.landingPadFull : GCBlocks.landingPad, 0);
            }
        }
    }

    private static ScenarioResult afterWarmup(String name, java.util.function.Supplier<ScenarioResult> result) {
        return ScenarioResult.pending(name, WARMUP_TICKS + 20, ticks -> ticks < WARMUP_TICKS ? null : result.get());
    }

    private static void empty(IGregTechTileEntity base) {
        if (base != null) {
            for (int i = 0; i < base.getSizeInventory(); i++) {
                base.setInventorySlotContents(i, null);
            }
        }
    }

    // --- scenarios --------------------------------------------------------------------------------------------------

    private abstract static class Base implements Scenario {

        @Override
        public final ScenarioResult run(ScenarioContext ctx) {
            if (!ModuleState.isActive(ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), "the ICBM module is off, so the Launch Control does nothing");
            }
            return go(ctx);
        }

        abstract ScenarioResult go(ScenarioContext ctx);
    }

    /** The machine is registered under 20140 as an HV Launch Control and its recipe is in the Assembler map. */
    private static final class Registered extends Base {

        @Override
        public String name() {
            return "icbm.control.registered";
        }

        @Override
        public String description() {
            return "The Launch Control is MTE 20140, an HV machine, with an Assembler recipe at HV.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Object m = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.LAUNCH_CONTROL];
            out.check(m instanceof MTELaunchControl, "MTE 20140 should be an MTELaunchControl");
            if (m instanceof MTELaunchControl) {
                out.checkEquals("tier", 3, (int) ((MTELaunchControl) m).mTier);
                ItemStack stack = ((MTELaunchControl) m).getStackForm(1);
                boolean found = false;
                for (gregtech.api.util.GTRecipe r : gregtech.api.recipe.RecipeMaps.assemblerRecipes.getAllRecipes()) {
                    if (r.mOutputs != null && r.mOutputs.length > 0
                        && r.mOutputs[0] != null
                        && r.mOutputs[0].getItem() == stack.getItem()
                        && r.mOutputs[0].getItemDamage() == stack.getItemDamage()) {
                        found = true;
                        out.checkEquals("recipe voltage", gregtech.api.enums.TierEU.RECIPE_HV, (long) r.mEUt);
                    }
                }
                out.check(found, "the Launch Control needs an Assembler recipe");
            }
            return out.build();
        }
    }

    /** With no pad, no target and no missile the gate names each problem in turn. */
    private static final class PadAndGate extends Base {

        @Override
        public String name() {
            return "icbm.control.pad_and_gate";
        }

        @Override
        public String description() {
            return "No target, then no pad, then (with a pad but no missile) no missile; the pad is found two blocks away and not eight.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(10, 4);
            place(ctx, 0, 0, 0);
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final int[] stage = { 0 };
            return ScenarioResult.pending(name(), WARMUP_TICKS + 60, ticks -> {
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                IGregTechTileEntity base = live(ctx, 0, 0, 0);
                MTELaunchControl c = mte(base);
                if (c == null) {
                    return ScenarioResult.fail(name(), "the Launch Control is gone after the warm-up");
                }
                if (stage[0] == 0) {
                    base.increaseStoredEnergyUnits(MTELaunchControl.EU_PER_SHOT * 3L, true);
                    out.checkEquals("unset target", ControlStatus.NO_TARGET, c.currentStatus());
                    out.check(
                        !c.target()
                            .isSet(),
                        "a fresh block has no target, not (0, 0)");
                    c.setXText("500");
                    out.check(
                        !c.target()
                            .isSet(),
                        "one coordinate is not a target");
                    c.setZText("-700");
                    out.checkEquals("target", LaunchTarget.of(500, -700), c.target());
                    out.checkEquals("no pad", ControlStatus.NO_PAD, c.currentStatus());
                    // A pad eight blocks off is out of reach; one whose center is three blocks off is found.
                    pad(ctx, 8, 0, 0);
                    out.checkEquals("far pad ignored", ControlStatus.NO_PAD, c.currentStatus());
                    pad(ctx, 3, 0, 0);
                    stage[0] = 1;
                    return null;
                }
                ControlStatus st = c.fire(null);
                out.checkEquals("pad found, no missile", ControlStatus.NO_MISSILE, st);
                out.checkEquals("nothing was charged", MTELaunchControl.EU_PER_SHOT * 3L, base.getStoredEU());
                // Without power the block says so before it looks at the missile.
                base.decreaseStoredEnergyUnits(base.getStoredEU(), true);
                out.checkEquals("no power", ControlStatus.NO_POWER, c.fire(null));
                empty(base);
                return out.build();
            });
        }
    }

    /** A data stick saves and loads a target, and refuses a foreign dimension or an empty stick. */
    private static final class Stick extends Base {

        @Override
        public String name() {
            return "icbm.control.data_stick";
        }

        @Override
        public String description() {
            return "Save puts the target on a GT data stick, load reads it back into another block's state; empty and wrong-dimension sticks are refused; a far player cannot press the buttons.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(10, 4);
            place(ctx, 0, 0, 0);
            TestPlayer near = new TestPlayer((WorldServer) ctx.world(), "[GGLaunchNear]");
            near.setPosition(ctx.originX() + 0.5, ctx.originY() + 1, ctx.originZ() + 2.5);
            TestPlayer far = new TestPlayer((WorldServer) ctx.world(), "[GGLaunchFar]");
            far.setPosition(ctx.originX() + 0.5, ctx.originY() + 1, ctx.originZ() + 60.5);
            return afterWarmup(name(), () -> {
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                IGregTechTileEntity base = live(ctx, 0, 0, 0);
                MTELaunchControl c = mte(base);
                if (c == null) {
                    return ScenarioResult.fail(name(), "the Launch Control is gone after the warm-up");
                }
                c.setXText("1234");
                c.setZText("-5678");
                c.saveToStick(near);
                out.checkEquals("no stick yet", TargetStick.Outcome.NO_STICK, c.lastStick());
                ItemStack stick = ItemList.Tool_DataStick.get(1);
                base.setInventorySlotContents(MTELaunchControl.SLOT_STICK, stick);
                c.saveToStick(far);
                out.checkEquals("far player refused", TargetStick.Outcome.NO_STICK, c.lastStick());
                out.check(
                    !TargetStick.read(stick.getTagCompound())
                        .isSet(),
                    "a far player must not write the stick");
                c.saveToStick(near);
                out.checkEquals("saved", TargetStick.Outcome.SAVED, c.lastStick());
                out.checkEquals("on the stick", LaunchTarget.of(1234, -5678), TargetStick.read(stick.getTagCompound()));
                c.setXText("");
                c.setZText("");
                out.check(
                    !c.target()
                        .isSet(),
                    "clearing a field clears the target");
                c.loadFromStick(near);
                out.checkEquals("loaded", TargetStick.Outcome.LOADED, c.lastStick());
                out.checkEquals("target after load", LaunchTarget.of(1234, -5678), c.target());
                // A stick from another dimension.
                int dim = ctx.world().provider.dimensionId;
                TargetStick.write(stick.getTagCompound(), LaunchTarget.of(9, 9), dim + 7);
                c.loadFromStick(near);
                out.checkEquals("foreign dimension", TargetStick.Outcome.WRONG_DIMENSION, c.lastStick());
                out.checkEquals("target unchanged", LaunchTarget.of(1234, -5678), c.target());
                // An empty stick.
                base.setInventorySlotContents(MTELaunchControl.SLOT_STICK, ItemList.Tool_DataStick.get(1));
                c.loadFromStick(near);
                out.checkEquals("empty stick", TargetStick.Outcome.EMPTY_STICK, c.lastStick());
                // Typed junk never becomes a target.
                c.setXText("12abc");
                out.checkEquals("junk ignored", "1234", c.xText());
                c.useMyPosition(far);
                out.checkEquals("far player cannot aim", LaunchTarget.of(1234, -5678), c.target());
                c.useMyPosition(near);
                out.checkEquals(
                    "near player aims at their feet",
                    ctx.originX(),
                    c.target()
                        .x());
                empty(base);
                return out.build();
            });
        }
    }

    /** Redstone fires once per rising edge (ICBM-04-20), not on a held signal and not on load. */
    private static final class Redstone extends Base {

        @Override
        public String name() {
            return "icbm.control.redstone";
        }

        @Override
        public String description() {
            return "A redstone block next to the Launch Control makes one shot attempt, not one per tick; a second pulse makes a second; disabling and re-enabling its work (a controller cover) makes a third.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(10, 4);
            place(ctx, 0, 0, 0);
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final int[] stage = { 0 };
            final int[] mark = { 0 };
            final int[] before = { 0 };
            return ScenarioResult.pending(name(), WARMUP_TICKS + 160, ticks -> {
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                IGregTechTileEntity base = live(ctx, 0, 0, 0);
                MTELaunchControl c = mte(base);
                if (c == null) {
                    return ScenarioResult.fail(name(), "the Launch Control is gone after the warm-up");
                }
                switch (stage[0]) {
                    case 0:
                        c.setXText("200");
                        c.setZText("200");
                        before[0] = c.attempts();
                        ctx.setBlock(1, 0, 0, Blocks.redstone_block, 0);
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        if (ticks < mark[0] + 30) {
                            return null;
                        }
                        out.checkEquals("one attempt for a held signal", before[0] + 1, c.attempts());
                        out.checkEquals("it was refused for want of a pad", ControlStatus.NO_PAD, c.lastShot());
                        ctx.setBlock(1, 0, 0, Blocks.air, 0);
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        if (ticks < mark[0] + 10) {
                            return null;
                        }
                        ctx.setBlock(1, 0, 0, Blocks.redstone_block, 0);
                        mark[0] = ticks;
                        stage[0] = 3;
                        return null;
                    case 3:
                        if (ticks < mark[0] + 30) {
                            return null;
                        }
                        out.checkEquals("a second pulse is a second attempt", before[0] + 2, c.attempts());
                        ctx.setBlock(1, 0, 0, Blocks.air, 0);
                        // What a machine controller cover does: work disabled, then enabled again.
                        base.disableWorking();
                        mark[0] = ticks;
                        stage[0] = 4;
                        return null;
                    case 4:
                        if (ticks < mark[0] + 10) {
                            return null;
                        }
                        out.checkEquals("disabling does not fire", before[0] + 2, c.attempts());
                        base.enableWorking();
                        mark[0] = ticks;
                        stage[0] = 5;
                        return null;
                    default:
                        if (ticks < mark[0] + 10) {
                            return null;
                        }
                        out.checkEquals("enabling again is a third attempt", before[0] + 3, c.attempts());
                        return out.build();
                }
            });
        }
    }

    /** Test hook: the player type used above, so a future scenario can reuse it. */
    static EntityPlayer testPlayer(WorldServer world, String name) {
        return new TestPlayer(world, name);
    }
}
