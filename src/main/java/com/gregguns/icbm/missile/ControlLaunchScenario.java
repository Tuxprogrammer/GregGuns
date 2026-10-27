package com.gregguns.icbm.missile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.IcbmAvailability;
import com.gregguns.icbm.blast.BlastEngine;
import com.gregguns.icbm.control.ControlStatus;
import com.gregguns.icbm.control.MTELaunchControl;
import com.gregguns.icbm.control.TargetStick;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.machine.GregGunsMTEIDs;
import com.mojang.authlib.GameProfile;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;

/**
 * Exit box "the launch control drives coder 1's launch": a Launch Control beside a real Galacticraft pad refuses a shot
 * for every wrong reason without charging EU, fires a fueled armed T1 at typed coordinates from its button, and then
 * fires a second missile at a target loaded from a GT data stick by a redstone block. Both missiles fly, detonate on
 * their platforms and release every chunk ticket. It lives in this package for the stacking helpers of
 * {@link MissileScenarios}; the launch-control track owns it. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md, launch-sequence.md
public final class ControlLaunchScenario implements Scenario {

    private static final int WARMUP_TICKS = 45;
    private static final int DISTANCE = 70;

    @Override
    public String name() {
        return "icbm.control.launch_to_impact";
    }

    @Override
    public String description() {
        return "A Launch Control beside the pad refuses every wrong shot for free, fires a T1 at typed coordinates, then fires a second missile at a data-stick target by redstone; both detonate on target.";
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        if (!ModuleState.isActive(ModuleId.ICBM) || MissileScenarios.moduleOff()) {
            return ScenarioResult.skip(name(), "the ICBM module is off");
        }
        if (!IcbmAvailability.galacticraftPresent()) {
            return ScenarioResult.skip(name(), "Galacticraft is not loaded (D46)");
        }
        ctx.clearArea(8, 20);
        MissileScenarios.floor(ctx, 6);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                ctx.setBlock(a, 0, b, GCBlocks.landingPad, 0);
            }
        }
        int id = GregGunsMTEIDs.LAUNCH_CONTROL;
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
        ctx.setBlock(3, 0, 0, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + 3, ctx.originY(), ctx.originZ());
        if (tile instanceof IGregTechTileEntity) {
            IGregTechTileEntity base = (IGregTechTileEntity) tile;
            base.setInitialValuesAsNBT(null, (short) id);
            base.setFrontFacing(ForgeDirection.NORTH);
            base.setOwnerName("gregguns-test");
            base.setOwnerUuid(UUID.nameUUIDFromBytes("gregguns-test".getBytes(StandardCharsets.UTF_8)));
        }
        final boolean terrain = IcbmConfig.terrainDamageT1;
        IcbmConfig.terrainDamageT1 = true;
        final World w = ctx.world();
        final FakePlayer player = new FakePlayer(
            (WorldServer) w,
            new GameProfile(UUID.nameUUIDFromBytes("gg-launch".getBytes(StandardCharsets.UTF_8)), "[GGLaunch]"));
        player.setPosition(ctx.originX() + 3.5, ctx.originY() + 1, ctx.originZ() + 2.5);
        final MissileScenarios.Target first = new MissileScenarios.Target(ctx, 0, DISTANCE, 9);
        final MissileScenarios.Target second = new MissileScenarios.Target(ctx, 0, -DISTANCE, 9);
        final ScenarioResult.Builder out = ScenarioResult.builder(name());
        final EntityMissile[] missile = new EntityMissile[1];
        final int[] stage = { 0 };
        final int[] mark = { 0 };
        final long[] euBefore = { 0 };
        final int[] logged = { -1 };
        return ScenarioResult.pending(name(), 3000, ticks -> {
            if (logged[0] != stage[0]) {
                logged[0] = stage[0];
                com.gregguns.GregGuns.LOG.info(
                    "launch_to_impact stage {} at tick {} (blasts {}, missile {})",
                    stage[0],
                    ticks,
                    BlastEngine.instance()
                        .activeBlasts(),
                    missile[0] == null ? "none" : missile[0].phase() + " " + missile[0].impactReport());
            }
            IGregTechTileEntity base = control(ctx);
            MTELaunchControl c = base != null && base.getMetaTileEntity() instanceof MTELaunchControl
                ? (MTELaunchControl) base.getMetaTileEntity()
                : null;
            if (ticks < WARMUP_TICKS) {
                return null;
            }
            if (c == null) {
                return fail(terrain, "the Launch Control is gone after the warm-up");
            }
            int cx = ctx.originX();
            int cy = ctx.originY();
            int cz = ctx.originZ();
            switch (stage[0]) {
                case 0: {
                    if (ticks == WARMUP_TICKS) {
                        java.util.List<String> names = new java.util.ArrayList<>();
                        for (Scenario sc : com.gregguns.core.testing.ScenarioRegistry.SCENARIOS.all()) {
                            names.add(sc.name());
                        }
                        int slot = names.indexOf(name());
                        com.gregguns.GregGuns.LOG.info(
                            "launch_to_impact slot {} of {}; z neighbors {} / {}; {}; {}",
                            slot,
                            names.size(),
                            slot >= 6 ? names.get(slot - 6) : "-",
                            slot + 6 < names.size() ? names.get(slot + 6) : "-",
                            approach(ctx, DISTANCE),
                            approach(ctx, -DISTANCE));
                    }
                    if (w.getBlock(cx, cy, cz) != GCBlocks.landingPadFull) {
                        return ticks > 140 ? fail(terrain, "the 3x3 pad never formed") : null;
                    }
                    // A blast still running from an earlier scenario fills the engine's job slots, and a blast started
                    // while they are full is refused (BlastEngine.start): that dropped the second blast of this
                    // scenario once on a fresh boot. Wait for a quiet engine before launching anything.
                    if (BlastEngine.instance()
                        .activeBlasts() > 0 && ticks < 400) {
                        return null;
                    }
                    int[] b = MissileScenarios
                        .buildStack(ctx, MissileTier.T1, WarheadCasing.T1, "condensed", 3, 0, 1, 0, true, 0);
                    out.checkEquals(
                        "armed on the pad",
                        MissileStacks.ArmResult.OK,
                        MissileStacks.arm(w, b[0], b[1], b[2], null));
                    missile[0] = PadMissiles.find(w, cx, cy, cz);
                    if (missile[0] == null) {
                        return fail(terrain, "no missile on the pad");
                    }
                    base.increaseStoredEnergyUnits(MTELaunchControl.EU_PER_SHOT * 5L, true);
                    euBefore[0] = base.getStoredEU();
                    // Every wrong shot is refused and charges nothing.
                    out.checkEquals("no target", ControlStatus.NO_TARGET, c.fire(null));
                    c.setXText(Integer.toString(cx));
                    c.setZText(Integer.toString(cz + DISTANCE));
                    out.checkEquals("tank not full", ControlStatus.NOT_FUELED, c.fire(null));
                    missile[0].fill(missile[0].capacityMb(), true);
                    // The gate looks at the fuel before the range, so the range refusals come once the tank is full.
                    c.setZText(Integer.toString(cz + 5));
                    out.checkEquals("too close", ControlStatus.TOO_CLOSE, c.fire(null));
                    c.setZText(Integer.toString(cz + 100_000));
                    out.checkEquals("out of range", ControlStatus.OUT_OF_RANGE, c.fire(null));
                    c.setZText(Integer.toString(cz + DISTANCE));
                    out.checkEquals("refusals were free", euBefore[0], base.getStoredEU());
                    out.checkEquals("ready", ControlStatus.READY, c.currentStatus());
                    // Fire from the button as a player standing beside the block.
                    ControlStatus st = c.fire(player);
                    out.checkEquals("button shot", ControlStatus.FIRED, st);
                    out.checkEquals("one shot's EU", euBefore[0] - MTELaunchControl.EU_PER_SHOT, base.getStoredEU());
                    // The countdown (D88) is running, so a second shot is refused as such; the cooldown (40 ticks) ends
                    // before ignition.
                    out.checkEquals("no second shot during the countdown", ControlStatus.COUNTDOWN, c.fire(null));
                    stage[0] = 1;
                    return null;
                }
                case 1: {
                    EntityMissile m = missile[0];
                    if (!m.isDead || (first.removed(w) == 0 && ticks < 1800)) {
                        return ticks > 1800 ? fail(terrain, "the first missile never hit") : null;
                    }
                    // The second launch also needs a free job slot, so the first blast must be done.
                    if (BlastEngine.instance()
                        .activeBlasts() > 0 && ticks < 2000) {
                        return null;
                    }
                    out.check(
                        first.removed(w) > 0,
                        "the first blast broke blocks on its platform" + engineState() + " [" + m.impactReport() + "]");
                    out.checkEquals(
                        "no tickets after the first impact",
                        0,
                        m.tickets()
                            .tickets());
                    // Second missile: target comes from a data stick, the shot from a redstone block.
                    int[] b = MissileScenarios
                        .buildStack(ctx, MissileTier.T1, WarheadCasing.T1, "condensed", 3, 0, 1, 0, true, 0);
                    out.checkEquals(
                        "second armed",
                        MissileStacks.ArmResult.OK,
                        MissileStacks.arm(w, b[0], b[1], b[2], null));
                    missile[0] = PadMissiles.find(w, cx, cy, cz);
                    if (missile[0] == null) {
                        return fail(terrain, "no second missile on the pad");
                    }
                    missile[0].fill(missile[0].capacityMb(), true);
                    ItemStack stick = ItemList.Tool_DataStick.get(1);
                    stick.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
                    TargetStick.write(
                        stick.getTagCompound(),
                        com.gregguns.icbm.control.LaunchTarget.of(cx, cz - DISTANCE),
                        w.provider.dimensionId);
                    base.setInventorySlotContents(MTELaunchControl.SLOT_STICK, stick);
                    c.setXText("");
                    c.setZText("");
                    c.loadFromStick(player);
                    out.checkEquals("target from the stick", TargetStick.Outcome.LOADED, c.lastStick());
                    euBefore[0] = base.getStoredEU();
                    mark[0] = ticks;
                    stage[0] = 2;
                    return null;
                }
                case 2: {
                    // The raised signal is the only trigger: nothing has fired yet.
                    out.check(!missile[0].isDead && missile[0].onPad(), "the second missile waits on the pad");
                    ctx.setBlock(4, 0, 0, Blocks.redstone_block, 0);
                    mark[0] = ticks;
                    stage[0] = 3;
                    return null;
                }
                case 3: {
                    EntityMissile m = missile[0];
                    if (m.onPad() && ticks < mark[0] + 60) {
                        return null;
                    }
                    out.check(!m.onPad() || m.isDead, "redstone launched the second missile");
                    out.checkEquals(
                        "redstone shot's EU",
                        euBefore[0] - MTELaunchControl.EU_PER_SHOT,
                        base.getStoredEU());
                    stage[0] = 4;
                    return null;
                }
                case 4: {
                    EntityMissile m = missile[0];
                    if (!m.isDead || (second.removed(w) == 0 && ticks < 2900)) {
                        return ticks > 2900 ? fail(terrain, "the second missile never hit") : null;
                    }
                    out.check(
                        second.removed(w) > 0,
                        "the second blast broke blocks on the data-stick target" + engineState()
                            + " ["
                            + m.impactReport()
                            + "]");
                    out.checkEquals(
                        "no tickets after the second impact",
                        0,
                        m.tickets()
                            .tickets());
                    out.checkEquals(
                        "no chunks after the second impact",
                        0,
                        m.tickets()
                            .heldChunks());
                    ctx.setBlock(4, 0, 0, Blocks.air, 0);
                    IcbmConfig.terrainDamageT1 = terrain;
                    base.setInventorySlotContents(MTELaunchControl.SLOT_STICK, null);
                    return out.build();
                }
                default:
                    return null;
            }
        });
    }

    private static IGregTechTileEntity control(ScenarioContext ctx) {
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + 3, ctx.originY(), ctx.originZ());
        return tile instanceof IGregTechTileEntity ? (IGregTechTileEntity) tile : null;
    }

    /**
     * Counts the solid blocks over the approach to a target, outside the column the target cleared, and names the
     * first. The scenario grid is 80 blocks apart and the targets are 70 out, so they sit inside the neighbors'
     * arenas: whatever an earlier neighbor left standing there can stop the descending missile short.
     */
    private static String approach(ScenarioContext ctx, int dz) {
        World w = ctx.world();
        int n = 0;
        String first = "";
        int lo = dz > 0 ? 20 : dz - 40;
        int hi = dz > 0 ? dz + 40 : -20;
        for (int z = lo; z <= hi; z++) {
            for (int x = -30; x <= 30; x++) {
                boolean cleared = Math.abs(x) <= 15 + 9 && Math.abs(z - dz) <= 15 + 9;
                if (cleared) {
                    continue;
                }
                for (int y = 0; y < 60; y++) {
                    net.minecraft.block.Block b = w.getBlock(ctx.originX() + x, ctx.originY() + y, ctx.originZ() + z);
                    if (b != Blocks.air) {
                        if (n++ == 0) {
                            first = b.getUnlocalizedName() + " at " + x + "," + y + "," + z;
                        }
                    }
                }
            }
        }
        return n + " solid blocks over the approach to z" + dz + (n > 0 ? ", first " + first : "");
    }

    /** What the blast engine was doing, for a failure message: a refused blast leaves no other trace. */
    private static String engineState() {
        BlastEngine e = BlastEngine.instance();
        return " (blast engine: " + e
            .activeBlasts() + " active, last " + e.lastBlastStage() + ", max " + IcbmConfig.maxActiveBlasts + ")";
    }

    private ScenarioResult fail(boolean terrain, String message) {
        IcbmConfig.terrainDamageT1 = terrain;
        return ScenarioResult.fail(name(), message);
    }
}
