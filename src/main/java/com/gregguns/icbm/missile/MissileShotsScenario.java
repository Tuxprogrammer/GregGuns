package com.gregguns.icbm.missile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.IcbmAvailability;
import com.gregguns.icbm.control.LaunchTarget;
import com.gregguns.icbm.control.MTELaunchControl;
import com.gregguns.icbm.control.TargetStick;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;

/**
 * {@code testing.icbm_missile_shots}: a visual walkthrough of the I2 content for one connected client, driven by the
 * dev autotest signals. It photographs a T1 and a T2 missile on Galacticraft pads (as blocks, then armed as
 * entities) from 3/4, side and close up, the Launch Control block and its GUI, a launch with the missile tracked
 * along its arc, the impact crater, a cloud of each gas warhead and the plume of a large conventional blast, and
 * opens NEI on a missile body. Labels are {@code icbm2-*}. Not part of {@code test all}: it needs a connected client
 * and Galacticraft. Dev environment only, server side.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/ (blast-visuals), 03-missiles/, 04-launch-control/
public final class MissileShotsScenario implements Scenario {

    static final String NAME = "testing.icbm_missile_shots";
    private static final double EYE = 1.62;
    /** Ticks between camera moves and shots; the software-rendered client is slow. */
    private static final int GAP = 90;
    private static final int DISTANCE = 70;
    private static final int FLIGHT_STEP = 22;
    private static final int FLIGHT_FRAMES = 40;
    private static final String[][] GASES = { { "chemical", "chem" }, { "debilitation", "debil" }, { "smoke", "smoke" },
        { "contagious", "contag" } };

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Photographs T1/T2 missiles on pads, the Launch Control and GUI, a launch, the impact crater, each gas"
            + " cloud, a blast plume and NEI for a connected client. Needs 1 client and Galacticraft; not part of test all.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    private static void signal(EntityPlayerMP p, String command, String... args) {
        p.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }

    /** Puts the eye at (ex, ey, ez) looking at the target point; no ground clamp, the player flies. */
    private static void cam(EntityPlayerMP p, double ex, double ey, double ez, double tx, double ty, double tz) {
        p.capabilities.allowFlying = true;
        p.capabilities.isFlying = true;
        p.sendPlayerAbilities();
        double dx = tx - ex;
        double dz = tz - ez;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(ty - ey, Math.hypot(dx, dz)));
        p.playerNetServerHandler.setPlayerLocation(ex, ey - EYE, ez, yaw, pitch);
    }

    /** A queue of timed actions; an action may add more. */
    private static final class Plan {

        private final List<long[]> due = new ArrayList<>();
        private final List<Runnable> acts = new ArrayList<>();
        /** Where the next {@link #after} lands: each call adds to it, so a chain of calls is cumulative. */
        private long cursor;
        /** The real tick of the action being run. */
        private long now;

        void after(int dt, Runnable r) {
            cursor += dt;
            due.add(new long[] { cursor });
            acts.add(r);
        }

        /** Runs everything due at {@code tick}, earliest first; an action's own calls chain from its due time. */
        void run(long tick) {
            now = tick;
            while (true) {
                int best = -1;
                for (int i = 0; i < due.size(); i++) {
                    if (due.get(i)[0] <= tick && (best < 0 || due.get(i)[0] < due.get(best)[0])) {
                        best = i;
                    }
                }
                if (best < 0) {
                    return;
                }
                long at = due.get(best)[0];
                Runnable r = acts.remove(best);
                due.remove(best);
                cursor = at;
                r.run();
            }
        }

        boolean idle() {
            return acts.isEmpty();
        }
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        if (!ModuleState.isActive(ModuleId.ICBM) || MissileScenarios.moduleOff()) {
            return ScenarioResult.skip(NAME, "the ICBM module is off");
        }
        if (!IcbmAvailability.galacticraftPresent()) {
            return ScenarioResult.skip(NAME, "Galacticraft is not loaded (D46)");
        }
        @SuppressWarnings("unchecked")
        List<EntityPlayerMP> players = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        if (players.isEmpty()) {
            return ScenarioResult.fail(NAME, "needs a connected player");
        }
        final EntityPlayerMP p = players.get(0);
        final World w = ctx.world();
        final int ox = ctx.originX();
        final int oy = ctx.originY();
        final int oz = ctx.originZ();
        // Flat stone floor and clear air around both pads and the control block.
        for (int x = -8; x <= 24; x++) {
            for (int z = -9; z <= 9; z++) {
                for (int y = 0; y <= 16; y++) {
                    w.setBlock(ox + x, oy + y, oz + z, Blocks.air, 0, 2);
                }
                w.setBlock(ox + x, oy - 1, oz + z, Blocks.stone, 0, 2);
            }
        }
        final int t2x = 14;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                ctx.setBlock(a, 0, b, GCBlocks.landingPad, 0);
                ctx.setBlock(t2x + a, 0, b, GCBlocks.landingPad, 0);
            }
        }
        final int cdx = 3;
        final int cdz = -2;
        int id = GregGunsMTEIDs.LAUNCH_CONTROL;
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
        ctx.setBlock(cdx, 0, cdz, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
        TileEntity tile = w.getTileEntity(ox + cdx, oy, oz + cdz);
        if (!(tile instanceof IGregTechTileEntity)) {
            return ScenarioResult.fail(NAME, "the Launch Control did not place");
        }
        final IGregTechTileEntity base = (IGregTechTileEntity) tile;
        base.setInitialValuesAsNBT(null, (short) id);
        base.setFrontFacing(ForgeDirection.NORTH);
        base.setOwnerName("gregguns-test");
        base.setOwnerUuid(UUID.nameUUIDFromBytes("gregguns-test".getBytes(StandardCharsets.UTF_8)));
        // Target: a thick stone block so a crater shows; far end of the arc.
        final int tz = DISTANCE;
        for (int x = -14; x <= 14; x++) {
            for (int z = tz - 14; z <= tz + 14; z++) {
                for (int y = -10; y <= -1; y++) {
                    w.setBlock(ox + x, oy + y, oz + z, Blocks.stone, 0, 2);
                }
                for (int y = 0; y <= 14; y++) {
                    w.setBlock(ox + x, oy + y, oz + z, Blocks.air, 0, 2);
                }
            }
        }
        final boolean terrain = IcbmConfig.terrainDamageT1;
        IcbmConfig.terrainDamageT1 = true;

        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.inventory.clearInventory(null, -1);
        p.clearActivePotions();
        p.setHealth(p.getMaxHealth());
        w.setWorldTime(6000L);
        w.getGameRules()
            .setOrCreateGameRule("doDaylightCycle", "false");

        final EntityMissile[] t1 = { null };
        final EntityMissile[] t2 = { null };
        final ScenarioResult.Builder out = ScenarioResult.builder(NAME);
        final int[] flight = { 0 };
        final int[] craterAirBefore = { 0 };
        final Plan plan = new Plan();
        final double cx = ox + 0.5;
        final double cz = oz + 0.5;
        final double c2x = ox + t2x + 0.5;

        plan.after(5, () -> signal(p, "hud", "off"));
        plan.after(120, () -> {
            if (w.getBlock(ox, oy, oz) != GCBlocks.landingPadFull
                || w.getBlock(ox + t2x, oy, oz) != GCBlocks.landingPadFull) {
                out.check(false, "the 3x3 pads never formed");
            }
            MissileScenarios.buildStack(ctx, MissileTier.T1, WarheadCasing.T1, "condensed", 3, 0, 1, 0, true, 0);
            MissileScenarios.buildStack(ctx, MissileTier.T2, WarheadCasing.T2, "thermobaric", 3, t2x, 1, 0, true, 0);
            cam(p, cx - 5, oy + 5, cz - 7, cx, oy + 3, cz);
        });
        // 1a. blocks (before arming): T1 3/4
        plan.after(GAP, () -> signal(p, "shot", "icbm2-t1-blocks-34"));
        plan.after(10, () -> cam(p, c2x - 6, oy + 6, cz - 8, c2x, oy + 3.5, cz));
        plan.after(GAP, () -> signal(p, "shot", "icbm2-t2-blocks-34"));
        // 1b. arm both; entities
        plan.after(10, () -> {
            out.checkEquals("T1 armed", MissileStacks.ArmResult.OK, MissileStacks.arm(w, ox, oy + 1, oz, null));
            out.checkEquals("T2 armed", MissileStacks.ArmResult.OK, MissileStacks.arm(w, ox + t2x, oy + 1, oz, null));
            t1[0] = PadMissiles.find(w, ox, oy, oz);
            t2[0] = PadMissiles.find(w, ox + t2x, oy, oz);
            out.check(t1[0] != null && t2[0] != null, "both missiles are entities on their pads");
            cam(p, cx - 5, oy + 5, cz - 7, cx, oy + 3, cz);
        });
        plan.after(GAP, () -> signal(p, "shot", "icbm2-t1-34"));
        plan.after(10, () -> cam(p, cx - 10, oy + 3, cz + 0.2, cx, oy + 3, cz));
        plan.after(GAP, () -> signal(p, "shot", "icbm2-t1-side"));
        plan.after(10, () -> cam(p, cx - 2.2, oy + 0.9, cz - 2.2, cx, oy + 1.0, cz));
        plan.after(GAP, () -> signal(p, "shot", "icbm2-t1-close-fins"));
        plan.after(10, () -> cam(p, cx - 2.6, oy + 4.8, cz - 2.6, cx, oy + 4.0, cz));
        plan.after(GAP, () -> signal(p, "shot", "icbm2-t1-close-warhead"));
        plan.after(10, () -> cam(p, c2x - 6, oy + 6, cz - 8, c2x, oy + 3.5, cz));
        plan.after(GAP, () -> signal(p, "shot", "icbm2-t2-34"));
        plan.after(10, () -> cam(p, c2x - 12, oy + 4, cz + 0.2, c2x, oy + 4, cz));
        plan.after(GAP, () -> signal(p, "shot", "icbm2-t2-side"));
        plan.after(10, () -> cam(p, c2x - 2.6, oy + 1.0, cz - 2.6, c2x, oy + 1.2, cz));
        plan.after(GAP, () -> signal(p, "shot", "icbm2-t2-close-fins"));
        plan.after(10, () -> cam(p, c2x - 3.2, oy + 5.8, cz - 3.2, c2x, oy + 5.0, cz));
        plan.after(GAP, () -> signal(p, "shot", "icbm2-t2-close-warhead"));
        // both together
        plan.after(10, () -> cam(p, (cx + c2x) / 2 - 2, oy + 6, cz - 20, (cx + c2x) / 2, oy + 3, cz));
        plan.after(GAP, () -> signal(p, "shot", "icbm2-pads-both"));

        // 2. Launch Control
        plan.after(10, () -> {
            if (t1[0] != null) {
                t1[0].fill(t1[0].capacityMb(), true);
            }
            base.increaseStoredEnergyUnits(MTELaunchControl.EU_PER_SHOT * 5L, true);
            MTELaunchControl c = control(base);
            c.setXText(Integer.toString(ox));
            c.setZText(Integer.toString(oz + tz));
            ItemStack stick = ItemList.Tool_DataStick.get(1);
            stick.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
            TargetStick.write(stick.getTagCompound(), LaunchTarget.of(ox + 40, oz + tz), w.provider.dimensionId);
            base.setInventorySlotContents(MTELaunchControl.SLOT_STICK, stick);
            signal(p, "hud", "on");
            cam(p, ox + cdx + 0.5, oy + 1.8, oz + cdz - 3.5, ox + cdx + 0.5, oy + 0.5, oz + cdz + 0.5);
        });
        plan.after(GAP, () -> signal(p, "shot", "icbm2-control-block"));
        plan.after(10, () -> cam(p, ox + cdx + 4.0, oy + 3.0, oz + cdz - 6.5, ox + 1.5, oy + 1.5, oz));
        plan.after(GAP, () -> signal(p, "shot", "icbm2-control-with-pad"));
        plan.after(10, () -> {
            cam(p, ox + cdx + 0.5, oy + 1.8, oz + cdz - 2.5, ox + cdx + 0.5, oy + 0.5, oz + cdz + 0.5);
            signal(p, "gui", "press", Integer.toString(ox + cdx), Integer.toString(oy), Integer.toString(oz + cdz));
        });
        plan.after(GAP + 60, () -> signal(p, "shot", "icbm2-control-gui"));
        plan.after(10, () -> signal(p, "close"));
        // NEI recipe for the body
        plan.after(20, () -> { signal(p, "gui", "nei", "missile_body_t1"); });
        plan.after(GAP + 60, () -> signal(p, "shot", "icbm2-nei-body-t1"));
        plan.after(10, () -> {
            signal(p, "close");
            signal(p, "hud", "off");
        });

        // 3. launch from the button path
        plan.after(20, () -> cam(p, cx - 7, oy + 3, cz - 11, cx, oy + 3, cz + 4));
        plan.after(GAP, () -> signal(p, "shot", "icbm2-launch-ready"));
        plan.after(10, () -> {
            MTELaunchControl c = control(base);
            c.setXText(Integer.toString(ox));
            c.setZText(Integer.toString(oz + tz));
            out.checkEquals("launch", com.gregguns.icbm.control.ControlStatus.FIRED, c.fire(p));
        });
        plan.after(FLIGHT_STEP, new Runnable() {

            @Override
            public void run() {
                EntityMissile m = t1[0];
                flight[0]++;
                if (m == null || m.isDead || flight[0] > FLIGHT_FRAMES) {
                    return;
                }
                // Track: the player runs ahead of the missile by a fixed offset, looking at it.
                double mx = m.posX;
                double my = m.posY;
                double mz = m.posZ;
                double ex = mx + 12;
                double ey = Math.max(oy + 4, my + 3);
                double ez = Math.min(mz - 6, oz + tz - 20);
                ez = Math.max(ez, mz - 16);
                cam(p, ex, ey, ez, mx, my, mz);
                final int n = flight[0];
                plan.after(12, () -> signal(p, "shot", String.format("icbm2-flight-%02d", n)));
                plan.after(FLIGHT_STEP + 6, this);
            }
        });
        // 4. impact: wait for the missile to be dead, then the crater
        plan.after(FLIGHT_STEP * 3, new Runnable() {

            @Override
            public void run() {
                EntityMissile m = t1[0];
                if (m != null && !m.isDead && plan.now < 3600) {
                    plan.after(20, this);
                    return;
                }
                out.check(m != null && m.isDead, "the missile detonated");
                final double tcx = ox + 0.5;
                final double tcz = oz + tz + 0.5;
                plan.after(15, () -> cam(p, tcx - 22, oy + 14, tcz - 22, tcx, oy, tcz));
                plan.after(GAP + 40, () -> signal(p, "shot", "icbm2-impact-crater-34"));
                plan.after(10, () -> cam(p, tcx, oy + 30, tcz - 2, tcx, oy - 2, tcz));
                plan.after(GAP, () -> signal(p, "shot", "icbm2-impact-crater-top"));
                scheduleGas(p, w, ox, oy, oz, plan, out);
            }
        });

        return ScenarioResult.pending(NAME, 9000, t -> {
            plan.run(t);
            if (!plan.idle()) {
                return null;
            }
            IcbmConfig.terrainDamageT1 = terrain;
            w.getGameRules()
                .setOrCreateGameRule("doDaylightCycle", "true");
            signal(p, "hud", "on");
            signal(p, "view", "0");
            return out.note("flight frames: " + Math.min(flight[0], FLIGHT_FRAMES) + "; read the icbm2-*.png shots")
                .build();
        });
    }

    private static MTELaunchControl control(IGregTechTileEntity base) {
        return (MTELaunchControl) base.getMetaTileEntity();
    }

    /** Gas clouds for each gas warhead, then a large conventional blast's plume; each at its own spot. */
    private static void scheduleGas(EntityPlayerMP p, World w, int ox, int oy, int oz, Plan plan,
        ScenarioResult.Builder out) {
        // Clouds sit along z = -60 on flat stone, 60 blocks apart so one never overlaps the last.
        for (int i = 0; i < GASES.length; i++) {
            final String ex = GASES[i][0];
            final String tag = GASES[i][1];
            final double gx = ox + 0.5 + i * 60 - 90;
            final double gz = oz - 60.5;
            final double gy = oy + 2.0;
            plan.after(20, () -> {
                for (int a = -22; a <= 22; a++) {
                    for (int b = -22; b <= 22; b++) {
                        w.setBlock((int) gx + a, oy - 1, (int) gz + b, Blocks.stone, 0, 2);
                        for (int y = 0; y <= 8; y++) {
                            w.setBlock((int) gx + a, oy + y, (int) gz + b, Blocks.air, 0, 2);
                        }
                    }
                }
                cam(p, gx - 28, oy + 12, gz - 28, gx, oy + 3, gz);
            });
            plan.after(GAP, () -> {
                boolean ok = IcbmBlasts.detonate(w, gx, gy, gz, Explosives.get(ex), 3, null);
                out.check(ok, ex + " blast accepted");
            });
            plan.after(40, () -> signal(p, "shot", "icbm2-gas-" + tag + "-a"));
            plan.after(60, () -> signal(p, "shot", "icbm2-gas-" + tag + "-b"));
            plan.after(30, () -> cam(p, gx - 8, oy + 3, gz - 8, gx + 2, oy + 3, gz + 2));
            plan.after(40, () -> signal(p, "shot", "icbm2-gas-" + tag + "-inside"));
        }
        // Plume: thermobaric on flat stone ground.
        final double px = ox + 0.5;
        final double pz = oz - 150.5;
        plan.after(20, () -> {
            for (int a = -30; a <= 30; a++) {
                for (int b = -30; b <= 30; b++) {
                    for (int y = -8; y <= -1; y++) {
                        w.setBlock((int) px + a, oy + y, (int) pz + b, Blocks.stone, 0, 2);
                    }
                    for (int y = 0; y <= 10; y++) {
                        w.setBlock((int) px + a, oy + y, (int) pz + b, Blocks.air, 0, 2);
                    }
                }
            }
            cam(p, px - 45, oy + 16, pz - 45, px, oy + 10, pz);
        });
        plan.after(GAP, () -> {
            boolean prev = IcbmConfig.terrainDamageT2;
            IcbmConfig.terrainDamageT2 = true;
            out.check(IcbmBlasts.detonate(w, px, oy + 1.0, pz, Explosives.get("thermobaric"), 3, null), "plume blast");
            IcbmConfig.terrainDamageT2 = prev;
        });
        plan.after(80, () -> signal(p, "shot", "icbm2-plume-a"));
        plan.after(100, () -> signal(p, "shot", "icbm2-plume-b"));
        plan.after(120, () -> signal(p, "shot", "icbm2-plume-c"));
        plan.after(80, () -> {});
    }
}
