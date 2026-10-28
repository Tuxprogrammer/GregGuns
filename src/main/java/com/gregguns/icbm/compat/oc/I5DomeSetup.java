package com.gregguns.icbm.compat.oc;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.control.MTELaunchControl;
import com.gregguns.icbm.defense.MTERadar;
import com.gregguns.icbm.missile.DefenseScenarios;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.I5ClientSetups;
import com.gregguns.icbm.missile.MissileScenarios;
import com.gregguns.icbm.missile.MissileStacks;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.missile.PadMissiles;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import li.cil.oc.api.Items;
import li.cil.oc.common.tileentity.traits.Rotatable;
import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;

/**
 * {@code testing.icbm_i5_dome}: a real OpenComputers computer (creative case with a CPU, RAM, an OpenOS floppy and the
 * Lua BIOS, a screen with a keyboard, a disk drive holding the Golden Dome floppy) on an Adapter that touches a Launch
 * Control (with a GC pad and a fueled T1 on it), an HV radar, an air-defense battery and an EMP tower. Dev only; OC
 * types, so it is registered only when OpenComputers is loaded.
 *
 * <pre>
 *   adapter (0,0,0); Launch Control east (1,0,0); pad centre (4,0,0); radar west (-1,0,0);
 *   air defense north (0,0,-1); EMP tower south (0,0,1); case (0,1,0); screen (0,1,1) facing south;
 *   keyboard (0,2,1); disk drive (1,1,0)
 * </pre>
 */
public final class I5DomeSetup extends I5ClientSetups.Setup {

    @Override
    public String name() {
        return "testing.icbm_i5_dome";
    }

    private static Block block(String name) {
        return Items.get(name)
            .block();
    }

    @Override
    protected ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
        ctx.clearArea(14, 8);
        MissileScenarios.sweep(ctx);
        I5ClientSetups.floor(ctx, -8, 12, -8, 8);
        List<EntityPlayerMP> ps = I5ClientSetups.players();
        if (ps.isEmpty()) {
            return ScenarioResult.fail(name(), "needs a player");
        }
        final EntityPlayerMP p = ps.get(0);
        p.setGameType(WorldSettings.GameType.SURVIVAL);
        p.capabilities.isFlying = false;
        p.capabilities.allowFlying = false;
        p.sendPlayerAbilities();
        p.inventory.clearInventory(null, -1);
        DefenseScenarios.place(ctx, GregGunsMTEIDs.LAUNCH_CONTROL, 1, 0, 0);
        DefenseScenarios.place(ctx, GregGunsMTEIDs.RADAR_HV, -1, 0, 0);
        DefenseScenarios.place(ctx, GregGunsMTEIDs.AIR_DEFENSE, 0, 0, -1);
        DefenseScenarios.place(ctx, GregGunsMTEIDs.EMP_TOWER, 0, 0, 1);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                ctx.setBlock(4 + a, 0, b, GCBlocks.landingPad, 0);
            }
        }
        ctx.setBlock(0, 0, 0, block("adapter"), 0);
        ctx.setBlock(0, 1, 0, block("caseCreative"), 0);
        ctx.setBlock(0, 1, 1, block("screen1"), 0);
        ctx.setBlock(0, 2, 1, block("keyboard"), 0);
        ctx.setBlock(1, 1, 0, block("diskDrive"), 0);
        // A second, free-standing Launch Control for the X and Z persistence check lives in testing.icbm_i5_lc.
        final ScenarioResult.Builder res = out;
        final boolean[] built = { false };
        return ScenarioResult.pending(name(), 600, t -> {
            if (t < 60) {
                return null;
            }
            if (!built[0]) {
                built[0] = true;
                face(ctx, 0, 1, 1, ForgeDirection.SOUTH);
                face(ctx, 0, 1, 0, ForgeDirection.SOUTH);
                face(ctx, 0, 2, 1, ForgeDirection.UP);
                IGregTechTileEntity lc = DefenseScenarios.live(ctx, 1, 0, 0);
                lc.setOwnerName(p.getCommandSenderName());
                lc.setOwnerUuid(p.getUniqueID());
                lc.increaseStoredEnergyUnits(MTELaunchControl.EU_PER_SHOT * 10L, true);
                IGregTechTileEntity radar = DefenseScenarios.live(ctx, -1, 0, 0);
                radar.increaseStoredEnergyUnits(100_000_000L, true);
                ((MTERadar) radar.getMetaTileEntity()).setRadius(100);
                IGregTechTileEntity air = DefenseScenarios.live(ctx, 0, 0, -1);
                air.increaseStoredEnergyUnits(100_000_000L, true);
                IGregTechTileEntity emp = DefenseScenarios.live(ctx, 0, 0, 1);
                ((BaseMetaTileEntity) emp).setStoredEU(5_000_000L);
                fill(ctx);
                return null;
            }
            if (t < 80 || !armMissile(ctx, res)) {
                return t > 400 ? ScenarioResult.fail(name(), "the pad never formed or the missile never armed") : null;
            }
            // Beside the screen, looking at it.
            p.setPositionAndUpdate(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 3.5);
            p.playerNetServerHandler
                .setPlayerLocation(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 3.5, 180.0F, -20.0F);
            I5ClientSetups.log(ctx, "dome ready: screen at 0,1,1; player at 0.5,0,3.5; missile on the pad at 4,1,0");
            return res.build();
        });
    }

    private static void face(ScenarioContext ctx, int dx, int dy, int dz, ForgeDirection d) {
        TileEntity t = I5ClientSetups.tile(ctx, dx, dy, dz);
        if (t instanceof Rotatable) {
            ((Rotatable) t).setFromFacing(d);
        }
    }

    private static boolean armMissile(ScenarioContext ctx, ScenarioResult.Builder out) {
        if (ctx.world()
            .getBlock(ctx.originX() + 4, ctx.originY(), ctx.originZ()) != GCBlocks.landingPadFull) {
            return false;
        }
        int[] b = MissileScenarios.buildStack(ctx, MissileTier.T1, WarheadCasing.T1, "condensed", 3, 4, 1, 0, true, 0);
        MissileStacks.arm(ctx.world(), b[0], b[1], b[2], null);
        EntityMissile m = PadMissiles.find(ctx.world(), ctx.originX() + 4, ctx.originY(), ctx.originZ());
        if (m == null) {
            return false;
        }
        m.fill(m.capacityMb(), true);
        return true;
    }

    /** The computer's parts, by the creative case's slot layout (InventorySlots.computer(3)). */
    private static void fill(ScenarioContext ctx) {
        TileEntity t = I5ClientSetups.tile(ctx, 0, 1, 0);
        net.minecraft.inventory.IInventory inv = (net.minecraft.inventory.IInventory) t;
        inv.setInventorySlotContents(0, stack("graphicsCard3"));
        inv.setInventorySlotContents(3, stack("ram6"));
        inv.setInventorySlotContents(4, stack("ram6"));
        inv.setInventorySlotContents(7, stack("openos"));
        inv.setInventorySlotContents(8, stack("cpu3"));
        inv.setInventorySlotContents(9, stack("luaBios"));
        TileEntity drive = I5ClientSetups.tile(ctx, 1, 1, 0);
        ((net.minecraft.inventory.IInventory) drive).setInventorySlotContents(0, GoldenDomeFloppy.stack());
        I5ClientSetups.log(ctx, "computer parts inserted; size " + inv.getSizeInventory());
        ((li.cil.oc.api.internal.Case) t).machine()
            .start();
    }

    private static ItemStack stack(String name) {
        li.cil.oc.api.detail.ItemInfo info = Items.get(name);
        if (info == null) {
            throw new IllegalStateException("OpenComputers has no item " + name);
        }
        return info.createItemStack(1);
    }

    /** Runs one named command against the computer from the potions setup. */
    public static void command(ScenarioContext ctx, EntityPlayerMP p, String cmd) {
        if (cmd.equals("report")) {
            report(ctx);
            return;
        }
        if (cmd.equals("eject")) {
            net.minecraft.inventory.IInventory drive = (net.minecraft.inventory.IInventory) I5ClientSetups
                .tile(ctx, 1, 1, 0);
            ItemStack disk = drive.getStackInSlot(0);
            drive.setInventorySlotContents(0, null);
            if (disk != null) {
                p.inventory.setInventorySlotContents(8, disk);
                p.inventory.currentItem = 8;
                p.inventoryContainer.detectAndSendChanges();
            }
            I5ClientSetups.log(ctx, "ejected the floppy to hotbar 8: " + disk);
        }
    }

    /** Logs the computer's state; called by the potions setup so it can be asked without a restart. */
    public static void report(ScenarioContext ctx) {
        TileEntity t = I5ClientSetups.tile(ctx, 0, 1, 0);
        if (!(t instanceof li.cil.oc.api.internal.Case)) {
            I5ClientSetups.log(ctx, "dome report: no case, found " + t);
            return;
        }
        li.cil.oc.api.machine.Machine m = ((li.cil.oc.api.internal.Case) t).machine();
        net.minecraft.inventory.IInventory inv = (net.minecraft.inventory.IInventory) t;
        if (inv.getStackInSlot(0) == null) {
            inv.setInventorySlotContents(0, stack("graphicsCard3"));
            m.stop();
            I5ClientSetups.log(ctx, "dome report: added the graphics card and stopped the machine");
        } else if (!m.isRunning()) {
            m.start();
            I5ClientSetups.log(ctx, "dome report: started the machine");
        }
        I5ClientSetups.log(
            ctx,
            "dome report: running=" + m.isRunning()
                + " paused="
                + m.isPaused()
                + " lastError="
                + m.lastError()
                + " components="
                + m.components());
        TileEntity sc = I5ClientSetups.tile(ctx, 0, 1, 1);
        I5ClientSetups.log(ctx, "dome report: screen tile " + sc);
        try {
            I5ClientSetups.log(
                ctx,
                "dome report: hasKeyboard=" + sc.getClass()
                    .getMethod("hasKeyboard")
                    .invoke(sc));
        } catch (ReflectiveOperationException e) {
            I5ClientSetups.log(ctx, "dome report: hasKeyboard unavailable " + e);
        }
    }
}
