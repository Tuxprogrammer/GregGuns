package com.gregguns.icbm.missile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.defense.DefenseMachines;
import com.gregguns.icbm.defense.MTERadar;
import com.gregguns.icbm.defense.MTERadarTower;
import com.gregguns.icbm.emp.EmpScenarios;
import com.gregguns.icbm.emp.MTEEmpTower;
import com.gregguns.icbm.tools.ToolRegistration;
import com.gregguns.icbm.warhead.TileWarhead;
import com.gregguns.icbm.warhead.WarheadBlocks;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadStacks;
import com.gregguns.machine.DevStructureBuilder;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/**
 * Dev-only setups for the I4 batched client session. Each one builds something, logs "I4C ..." lines with the absolute
 * coordinates, and leaves it standing for a human-driven client. None is part of {@code test all}.
 */
public final class I4ClientSetups {

    private I4ClientSetups() {}

    /** @param registry where to add the setups */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Tools());
        registry.register(new Radars());
        registry.register(new Launch());
        registry.register(new Emp());
        registry.register(new TowerSetup());
        registry.register(new Report());
        registry.register(new Items());
        registry.register(new Aim("unlit", 3.5, 0.25, 6.5, 2.2));
        registry.register(new Aim("lit", -2.5, 0.25, 6.5, 2.2));
        registry.register(new Aim("missile", 9.5, 1.5, 6.5, 3.0));
        registry.register(new Aim("air", 3.5, 25.0, 12.5, 1.0));
        registry.register(new Aim("radarhv", 6.5, 1.6, 31.5, 9.0));
        registry.register(new Aim("radarev", 14.5, 1.6, 31.5, 9.0));
        registry.register(new Aim("emp", 0.5, 0.8, -9.5, 4.0));
        registry.register(new Aim("tower", -7.5, 6.0, 14.5, 12.0));
        registry.register(new Aim("silo", 80.5, -1.0, 2.5, 9.0));
    }

    @SuppressWarnings("unchecked")
    private static List<EntityPlayerMP> players() {
        return new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
    }

    private abstract static class Setup implements Scenario {

        @Override
        public boolean includedInAll() {
            return false;
        }

        @Override
        public int areaRadius() {
            return 30;
        }

        @Override
        public String description() {
            return "I4 client-session setup (dev only); leaves its build standing. Not part of test all.";
        }

        @Override
        public final ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            return go(I4Anchor.of(ctx, 0, 0), out);
        }

        abstract ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out);

        static void log(ScenarioContext ctx, String what) {
            GregGuns.LOG.info("I4C {} origin={},{},{}", what, ctx.originX(), ctx.originY(), ctx.originZ());
        }
    }

    private static void floor(ScenarioContext ctx, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                ctx.setBlock(x, -1, z, Blocks.stone, 0);
            }
        }
    }

    private static void power(IGregTechTileEntity b) {
        if (b != null) {
            b.increaseStoredEnergyUnits(100_000_000L, true);
        }
    }

    /** Warheads to link and light, a lit warhead to put out and a standing missile to take apart. */
    private static final class Tools extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i4_tools";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            ctx.clearArea(14, 8);
            floor(ctx, -10, 14, -6, 14);
            List<EntityPlayerMP> ps = players();
            if (ps.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a player");
            }
            EntityPlayerMP p = ps.get(0);
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            p.inventory.clearInventory(null, -1);
            p.inventory.setInventorySlotContents(0, ToolRegistration.detonatorStack(Tier.HV));
            p.inventory.setInventorySlotContents(1, ToolRegistration.defuserStack());
            p.inventoryContainer.detectAndSendChanges();
            for (int dx : new int[] { 3, -3 }) {
                ctx.setBlock(dx, 0, 6, WarheadBlocks.of(WarheadCasing.T1), 0);
                TileEntity t = ctx.world()
                    .getTileEntity(ctx.originX() + dx, ctx.originY(), ctx.originZ() + 6);
                ItemStack stack = WarheadStacks
                    .make(Item.getItemFromBlock(WarheadBlocks.of(WarheadCasing.T1)), WarheadCasing.T1, "condensed", 1);
                ((TileWarhead) t).init(WarheadCasing.T1, stack.getTagCompound(), p);
                if (dx < 0) {
                    ((TileWarhead) t).arm(100000, p);
                }
            }
            EntityMissile m = EntityMissile.create(MissileTier.T1, ctx.world());
            m.setLocationAndAngles(
                ctx.originX() + 9.5,
                ctx.originY() + EntityMissile.PAD_SURFACE,
                ctx.originZ() + 6.5,
                0.0F,
                0.0F);
            m.configure(0, "condensed", 1, 1, null);
            ctx.world()
                .spawnEntityInWorld(m);
            log(
                ctx,
                "tools: unlit warhead at +3,0,+6; LIT warhead at -3,0,+6; standing missile at +9.5,+6.5; player "
                    + p.getCommandSenderName());
            return out.build();
        }
    }

    /** HV and EV radars (built, powered, dish turning) with a lamp in front of each. Pending so the machines tick. */
    private static final class Radars extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i4_radars";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            ctx.clearArea(14, 8);
            final int rz = 30;
            floor(ctx, 2, 16, rz - 6, rz + 4);
            DefenseScenarios.place(ctx, GregGunsMTEIDs.RADAR_HV, 6, 0, rz);
            DefenseScenarios.place(ctx, GregGunsMTEIDs.RADAR_EV, 14, 0, rz);
            ctx.setBlock(6, 0, rz - 1, Blocks.redstone_lamp, 0);
            ctx.setBlock(14, 0, rz - 1, Blocks.redstone_lamp, 0);
            final int[] hv = { 0, 0 };
            final int[] ev = { 0, 0 };
            return ScenarioResult.pending(name(), 600, t -> {
                if (t < 60) {
                    return null;
                }
                MTERadar rh = DefenseScenarios.radar(DefenseScenarios.live(ctx, 6, 0, rz));
                MTERadar re = DefenseScenarios.radar(DefenseScenarios.live(ctx, 14, 0, rz));
                int a = DefenseScenarios.radarReady(ctx, rh, 6, 0, rz, ItemList.Hatch_Energy_HV, t, hv);
                int b = DefenseScenarios.radarReady(ctx, re, 14, 0, rz, ItemList.Hatch_Energy_EV, t, ev);
                if (a < 0 || b < 0) {
                    return ScenarioResult.fail(name(), "a radar never formed and ran");
                }
                if (a == 0 || b == 0) {
                    return null;
                }
                rh.setRadius(100);
                re.setRadius(100);
                log(ctx, "radar at dx=6 dz=" + rz + " range=" + rh.rangeIfOn());
                log(ctx, "radar at dx=14 dz=" + rz + " range=" + re.rangeIfOn());
                return out.build();
            });
        }
    }

    /** Arms a missile at (3,0,0) and launches it 80 blocks south; logs every machine's state while it flies. */
    private static final class Launch extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i4_launch";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            MissileScenarios.sweep(ctx);
            final EntityMissile m = MissileScenarios.armedAt(ctx, MissileTier.T1, 3, 0);
            if (m == null) {
                return ScenarioResult.fail(name(), "could not arm");
            }
            m.fill(m.capacityMb(), true);
            out.checkEquals("launch", LaunchResult.OK, m.launch(ctx.originX() + 0.5, ctx.originZ() + 80 + 0.5, null));
            final World w = ctx.world();
            return ScenarioResult.pending(name(), 3000, t -> {
                if (t % 20 == 0) {
                    StringBuilder s = new StringBuilder(
                        "flight t=" + t
                            + " dead="
                            + m.isDead
                            + " pos="
                            + (int) m.posX
                            + ","
                            + (int) m.posY
                            + ","
                            + (int) m.posZ);
                    for (int dx : new int[] { 6, 14 }) {
                        IGregTechTileEntity b = DefenseScenarios.live(ctx, dx, 0, 30);
                        if (b != null && b.getMetaTileEntity() instanceof MTERadar) {
                            MTERadar r = (MTERadar) b.getMetaTileEntity();
                            s.append(" radar")
                                .append(dx)
                                .append("[state=")
                                .append(r.state())
                                .append(" signal=")
                                .append(r.signal())
                                .append(" lamp=")
                                .append(
                                    w.getBlock(ctx.originX() + dx, ctx.originY(), ctx.originZ() + 29)
                                        == Blocks.lit_redstone_lamp)
                                .append("]");
                        }
                    }
                    log(ctx, s.toString());
                }
                return m.isDead && t > 100 ? out.build() : null;
            });
        }
    }

    /** An EMP Tower with buffers (one holding a charged battery), a battery buffer and a multiblock. */
    private static final class Emp extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i4_emp";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            ctx.clearArea(14, 8);
            floor(ctx, -8, 24, -18, -4);
            int buffer = ItemList.Battery_Buffer_1by1_MV.get(1)
                .getItemDamage();
            int mb = ItemList.Machine_Multi_BlastFurnace.get(1)
                .getItemDamage();
            final int tz = -10;
            EmpScenarios.place(ctx, 0, 0, tz, GregGunsMTEIDs.EMP_TOWER);
            for (int x = 3; x <= 5; x++) {
                EmpScenarios.place(ctx, x, 0, tz, buffer);
            }
            EmpScenarios.place(ctx, -4, 0, tz, mb);
            EmpScenarios.place(ctx, 20, 0, tz, buffer);
            return ScenarioResult.pending(name(), 120, t -> {
                if (t < 60) {
                    return null;
                }
                IGregTechTileEntity tower = EmpScenarios.live(ctx, 0, 0, tz);
                ((BaseMetaTileEntity) tower).setStoredEU(5_000_000L);
                for (int x : new int[] { 3, 4, 5, 20 }) {
                    IGregTechTileEntity b = EmpScenarios.live(ctx, x, 0, tz);
                    ((BaseMetaTileEntity) b).setStoredEU(1000L);
                }
                ItemStack battery = ItemList.Battery_RE_MV_Lithium.get(1);
                ic2.api.item.ElectricItem.manager.charge(battery, Double.MAX_VALUE, Integer.MAX_VALUE, true, false);
                ((gregtech.api.metatileentity.implementations.MTEBasicBatteryBuffer) EmpScenarios.live(ctx, 4, 0, tz)
                    .getMetaTileEntity()).mInventory[0] = battery;
                ((gregtech.api.metatileentity.implementations.MTEMultiBlockBase) EmpScenarios.live(ctx, -4, 0, tz)
                    .getMetaTileEntity()).fixAllIssues();
                log(ctx, "emp tower at 0,0," + tz + "; buffers x=3,4(battery),5; multiblock x=-4; far buffer x=20");
                return out.build();
            });
        }
    }

    /** The Radar Tower, formed and powered, and an air-defense battery linked to it. */
    private static final class TowerSetup extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i4_tower";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            ctx.clearArea(14, 24);
            final int cy = 5;
            final int tz = 14;
            floor(ctx, -16, 0, 6, 24);
            DefenseScenarios.place(ctx, GregGunsMTEIDs.RADAR_TOWER, -8, cy, tz);
            return ScenarioResult.pending(name(), 300, t -> {
                if (t < 60) {
                    return null;
                }
                if (t == 60) {
                    boolean formed = DevStructureBuilder.buildRadarTower(
                        ctx.world(),
                        ctx.originX() - 8,
                        ctx.originY() + cy,
                        ctx.originZ() + tz,
                        ItemList.Hatch_Energy_IV).formed;
                    log(ctx, "tower formed=" + formed + " controller at -8," + cy + "," + tz);
                    return null;
                }
                if (t < 120) {
                    return null;
                }
                power(DefenseScenarios.live(ctx, -8, cy - 1, tz));
                MTERadarTower tw = (MTERadarTower) DefenseScenarios.live(ctx, -8, cy, tz)
                    .getMetaTileEntity();
                log(
                    ctx,
                    "tower formed=" + tw.isFormed() + " online=" + tw.online() + " radius=" + tw.protectedRadius());
                return out.build();
            });
        }
    }

    /** Gives the first player every new I4 item and machine, and stands a T3 EMP warhead block on a floor. */
    private static final class Items extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i4_items";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            ctx.clearArea(14, 8);
            floor(ctx, -10, 14, -6, 14);
            List<EntityPlayerMP> ps = players();
            if (ps.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a player");
            }
            EntityPlayerMP p = ps.get(0);
            p.setGameType(WorldSettings.GameType.CREATIVE);
            p.inventory.clearInventory(null, -1);
            ItemStack[] all = { new ItemStack(DefenseMachines.samItem()), new ItemStack(DefenseMachines.radarGunItem()),
                ToolRegistration.detonatorStack(Tier.HV), ToolRegistration.detonatorStack(Tier.EV),
                ToolRegistration.detonatorStack(Tier.IV), ToolRegistration.defuserStack(),
                DefenseMachines.stack(GregGunsMTEIDs.RADAR_HV, 1), DefenseMachines.stack(GregGunsMTEIDs.RADAR_EV, 1),
                DefenseMachines.stack(GregGunsMTEIDs.RADAR_TOWER, 1),
                DefenseMachines.stack(GregGunsMTEIDs.AIR_DEFENSE, 1),
                DefenseMachines.stack(GregGunsMTEIDs.EMP_TOWER, 1),
                DefenseMachines.stack(GregGunsMTEIDs.MISSILE_SILO, 1),
                DefenseMachines.stack(GregGunsMTEIDs.LAUNCH_CONTROL, 1), WarheadStacks
                    .make(Item.getItemFromBlock(WarheadBlocks.of(WarheadCasing.T3)), WarheadCasing.T3, "emp", 1) };
            for (int i = 0; i < all.length; i++) {
                p.inventory.setInventorySlotContents(i, all[i]);
            }
            p.inventoryContainer.detectAndSendChanges();
            ctx.setBlock(0, 0, 6, WarheadBlocks.of(WarheadCasing.T3), 0);
            TileEntity t = ctx.world()
                .getTileEntity(ctx.originX(), ctx.originY(), ctx.originZ() + 6);
            ItemStack stack = WarheadStacks
                .make(Item.getItemFromBlock(WarheadBlocks.of(WarheadCasing.T3)), WarheadCasing.T3, "emp", 1);
            ((TileWarhead) t).init(WarheadCasing.T3, stack.getTagCompound(), p);
            log(ctx, "items: " + all.length + " given; T3 EMP warhead at 0,0,6");
            return out.build();
        }
    }

    /** Logs the state of everything the setups built. */
    private static final class Report extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i4_report";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            World w = ctx.world();
            for (int dx : new int[] { 6, 14 }) {
                IGregTechTileEntity b = DefenseScenarios.live(ctx, dx, 0, 30);
                if (b != null && b.getMetaTileEntity() instanceof MTERadar) {
                    MTERadar r = (MTERadar) b.getMetaTileEntity();
                    log(
                        ctx,
                        "report radar" + dx
                            + " state="
                            + r.state()
                            + " signal="
                            + r.signal()
                            + " radius="
                            + r.protectedRadius());
                }
            }
            final int tz = -10;
            IGregTechTileEntity tb = EmpScenarios.live(ctx, 0, 0, tz);
            if (tb != null && tb.getMetaTileEntity() instanceof MTEEmpTower) {
                MTEEmpTower t = (MTEEmpTower) tb.getMetaTileEntity();
                log(
                    ctx,
                    "report emp radius=" + t.radius()
                        + " mode="
                        + t.mode()
                        + " pulses="
                        + t.pulses()
                        + " stored="
                        + tb.getStoredEU()
                        + " last="
                        + (t.lastResult() == null ? "none"
                            : "devices=" + t.lastResult().devices
                                + " eu="
                                + t.lastResult().euDrained
                                + " issues="
                                + t.lastResult().issues));
                for (int x : new int[] { 3, 4, 5, 20 }) {
                    IGregTechTileEntity b = EmpScenarios.live(ctx, x, 0, tz);
                    log(ctx, "report buffer x=" + x + " eu=" + b.getStoredEU());
                }
                IGregTechTileEntity buf = EmpScenarios.live(ctx, 4, 0, tz);
                ItemStack bat = ((gregtech.api.metatileentity.implementations.MTEBasicBatteryBuffer) buf
                    .getMetaTileEntity()).mInventory[0];
                log(
                    ctx,
                    "report battery charge="
                        + (bat == null ? "none" : String.valueOf(ic2.api.item.ElectricItem.manager.getCharge(bat))));
                gregtech.api.metatileentity.implementations.MTEMultiBlockBase mb = (gregtech.api.metatileentity.implementations.MTEMultiBlockBase) EmpScenarios
                    .live(ctx, -4, 0, tz)
                    .getMetaTileEntity();
                log(ctx, "report multiblock issues=" + (6 - mb.getRepairStatus()));
            }
            IGregTechTileEntity tw = DefenseScenarios.live(ctx, -8, 5, 14);
            if (tw != null && tw.getMetaTileEntity() instanceof MTERadarTower) {
                MTERadarTower t = (MTERadarTower) tw.getMetaTileEntity();
                log(
                    ctx,
                    "report tower formed=" + t.isFormed()
                        + " online="
                        + t.online()
                        + " radius="
                        + t.protectedRadius()
                        + " state="
                        + t.state());
            }
            return out.build();
        }
    }

    /** Puts the first player 'dist' blocks north of a point (relative to the origin), looking at it. */
    private static final class Aim extends Setup {

        private final String id;
        private final double x;
        private final double y;
        private final double z;
        private final double dist;

        Aim(String id, double x, double y, double z, double dist) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dist = dist;
        }

        @Override
        public String name() {
            return "testing.icbm_i4_aim_" + id;
        }

        @Override
        ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            for (EntityPlayerMP p : players()) {
                double tx = ctx.originX() + x;
                double ty = ctx.originY() + y;
                double tz = ctx.originZ() + z;
                boolean ground = id.equals("unlit") || id.equals("lit") || id.equals("missile");
                MissileShotsScenario.cam(
                    p,
                    tx,
                    ground ? ctx.originY() + 1.62 : ty + 1.62 + (id.equals("air") ? 0 : 1.0),
                    tz - dist,
                    tx,
                    ty,
                    tz);
            }
            return out.build();
        }
    }
}
