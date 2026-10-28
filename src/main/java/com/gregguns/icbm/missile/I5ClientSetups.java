package com.gregguns.icbm.missile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.explosion.nuclear.FalloutBlocks;
import com.gregguns.icbm.antidote.AntidoteRegistration;
import com.gregguns.icbm.compat.oc.I5DomeSetup;
import com.gregguns.icbm.control.MTELaunchControl;
import com.gregguns.icbm.defense.DefenseMachines;
import com.gregguns.icbm.launcher.LauncherWarhead;
import com.gregguns.icbm.launcher.LauncherWarheadRegistration;
import com.gregguns.icbm.tools.ToolRegistration;
import com.gregguns.icbm.warhead.WarheadBlocks;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadStacks;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Dev-only setups for the I5 client sessions ({@code testing.icbm_i5_*}). Each one builds something, logs "I5C ..."
 * lines with absolute coordinates and leaves it standing for a human-driven client. None is part of {@code test all}.
 * The OpenComputers one lives in a nested class that is only loaded when the mod is present.
 */
public final class I5ClientSetups {

    private I5ClientSetups() {}

    /** @param registry where to add the setups */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Items());
        registry.register(new Antidote());
        registry.register(new Potions());
        registry.register(new FreeLaunchControl());
        if (cpw.mods.fml.common.Loader.isModLoaded("OpenComputers")) {
            registry.register(new I5DomeSetup());
        }
    }

    @SuppressWarnings("unchecked")
    public static List<EntityPlayerMP> players() {
        return new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
    }

    public static void floor(ScenarioContext ctx, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                ctx.setBlock(x, -1, z, Blocks.stone, 0);
            }
        }
    }

    public static net.minecraft.tileentity.TileEntity tile(ScenarioContext ctx, int dx, int dy, int dz) {
        return ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
    }

    public static void log(ScenarioContext ctx, String what) {
        GregGuns.LOG.info("I5C {} origin={},{},{}", what, ctx.originX(), ctx.originY(), ctx.originZ());
    }

    /** Base of the I5 setups: one shared arena, no cleanup bookkeeping, never part of {@code test all}. */
    public abstract static class Setup implements Scenario {

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
            return "I5 client-session setup (dev only); leaves its build standing. Not part of test all.";
        }

        @Override
        public final ScenarioResult run(ScenarioContext ctx) {
            return go(I4Anchor.of(ctx, 0, 0), ScenarioResult.builder(name()));
        }

        protected abstract ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out);
    }

    /** Fills the first player's 36 slots with the I4 and I5 items, in a fixed order (see the log line). */
    private static final class Items extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5_items";
        }

        @Override
        protected ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            ctx.clearArea(14, 8);
            floor(ctx, -10, 14, -6, 14);
            List<EntityPlayerMP> ps = players();
            if (ps.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a player");
            }
            EntityPlayerMP p = ps.get(0);
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            p.capabilities.isFlying = false;
            p.capabilities.allowFlying = false;
            p.sendPlayerAbilities();
            p.inventory.clearInventory(null, -1);
            List<ItemStack> all = new ArrayList<>();
            all.add(DefenseMachines.stack(GregGunsMTEIDs.RADAR_HV, 1));
            all.add(DefenseMachines.stack(GregGunsMTEIDs.RADAR_EV, 1));
            all.add(DefenseMachines.stack(GregGunsMTEIDs.RADAR_TOWER, 1));
            all.add(new ItemStack(GregGunsCasings.INSTANCE, 1, GregGunsCasings.RADAR_CASING));
            all.add(DefenseMachines.stack(GregGunsMTEIDs.AIR_DEFENSE, 1));
            all.add(new ItemStack(DefenseMachines.samItem()));
            all.add(new ItemStack(DefenseMachines.radarGunItem()));
            all.add(ToolRegistration.detonatorStack(Tier.HV));
            all.add(ToolRegistration.detonatorStack(Tier.EV));
            all.add(ToolRegistration.detonatorStack(Tier.IV));
            all.add(ToolRegistration.defuserStack());
            all.add(DefenseMachines.stack(GregGunsMTEIDs.EMP_TOWER, 1));
            all.add(
                WarheadStacks
                    .make(Item.getItemFromBlock(WarheadBlocks.of(WarheadCasing.T3)), WarheadCasing.T3, "emp", 1));
            all.add(new ItemStack(AntidoteRegistration.item(), 4));
            all.add(new ItemStack(FalloutBlocks.trinitite()));
            all.add(new ItemStack(FalloutBlocks.goo(), 1, 0));
            all.add(new ItemStack(FalloutBlocks.rubble(), 1, 0));
            all.add(
                WarheadStacks.make(
                    Item.getItemFromBlock(WarheadBlocks.of(WarheadCasing.T4)),
                    WarheadCasing.T4,
                    "antimatter",
                    1));
            all.add(
                WarheadStacks
                    .make(Item.getItemFromBlock(WarheadBlocks.of(WarheadCasing.T5)), WarheadCasing.T5, "redmatter", 1));
            for (LauncherWarhead w : LauncherWarhead.values()) {
                all.add(LauncherWarheadRegistration.stack(w));
            }
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < all.size() && i < 36; i++) {
                // Hotbar is 0-8, then main rows start at 9; list in that order.
                p.inventory.setInventorySlotContents(i, all.get(i));
                s.append(i)
                    .append('=')
                    .append(
                        all.get(i) == null ? "null"
                            : all.get(i)
                                .getDisplayName())
                    .append("; ");
            }
            p.inventoryContainer.detectAndSendChanges();
            log(ctx, "items " + all.size() + ": " + s);
            return out.build();
        }
    }

    /** Antidote, a speed effect, rubble and the sicknesses, for the antidote check. */
    private static final class Antidote extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5_antidote";
        }

        @Override
        protected ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            ctx.clearArea(14, 8);
            floor(ctx, -10, 14, -6, 14);
            List<EntityPlayerMP> ps = players();
            if (ps.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a player");
            }
            EntityPlayerMP p = ps.get(0);
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            p.capabilities.isFlying = false;
            p.capabilities.allowFlying = false;
            p.sendPlayerAbilities();
            p.inventory.clearInventory(null, -1);
            p.clearActivePotions();
            p.setHealth(p.getMaxHealth());
            p.getFoodStats()
                .addStats(20, 1.0F);
            p.setPositionAndUpdate(ctx.originX() + 0.5, ctx.originY() + 0.0, ctx.originZ() + 0.5);
            p.inventory.setInventorySlotContents(0, new ItemStack(AntidoteRegistration.item(), 6));
            p.inventory.currentItem = 0;
            p.inventoryContainer.detectAndSendChanges();
            p.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 20 * 600, 1));
            sicken(p);
            log(ctx, "antidote: holding 6 antidotes (slot 0), speed + sicknesses applied");
            return out.build();
        }
    }

    /** Applies the harmful effects the antidote must clear, including IC2 radiation. */
    static void sicken(EntityPlayerMP p) {
        p.addPotionEffect(new PotionEffect(Potion.poison.id, 20 * 300, 0));
        p.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 20 * 300, 0));
        p.addPotionEffect(new PotionEffect(Potion.confusion.id, 20 * 300, 0));
        if (ic2.core.IC2Potion.radiation != null) {
            p.addPotionEffect(new PotionEffect(ic2.core.IC2Potion.radiation.id, 20 * 300, 0));
        }
    }

    /** Logs the first player's active effects; {@code sick} applies the sicknesses again first via the name. */
    private static final class Potions extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5_potions";
        }

        @Override
        protected ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            List<EntityPlayerMP> ps = players();
            if (ps.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a player");
            }
            EntityPlayerMP p = ps.get(0);
            java.io.File cmds = new java.io.File("i5cmd.txt");
            java.util.List<String> lines = new ArrayList<>();
            try {
                if (cmds.exists()) {
                    lines = java.nio.file.Files.readAllLines(cmds.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (java.io.IOException e) {
                log(ctx, "cannot read i5cmd.txt " + e);
            }
            for (String line : lines) {
                String cmd = line.trim();
                if (cmd.equals("sicken")) {
                    sicken(p);
                } else if (cmd.equals("rubble")) {
                    p.inventory.setInventorySlotContents(1, new ItemStack(FalloutBlocks.rubble(), 16, 0));
                    p.inventoryContainer.detectAndSendChanges();
                } else if (cmd.equals("heal")) {
                    p.setHealth(p.getMaxHealth());
                    p.getFoodStats()
                        .addStats(20, 1.0F);
                } else if (cmd.startsWith("hold ")) {
                    p.inventory.currentItem = Integer.parseInt(cmd.substring(5));
                    p.inventoryContainer.detectAndSendChanges();
                } else if (cmd.equals("eject") || cmd.equals("report")) {
                    if (cpw.mods.fml.common.Loader.isModLoaded("OpenComputers")) {
                        I5DomeSetup.command(ctx, p, cmd);
                    }
                } else if (cmd.startsWith("tp ")) {
                    String[] a = cmd.split(" ");
                    p.setPositionAndUpdate(
                        ctx.originX() + Double.parseDouble(a[1]),
                        ctx.originY() + Double.parseDouble(a[2]),
                        ctx.originZ() + Double.parseDouble(a[3]));
                    p.playerNetServerHandler.setPlayerLocation(
                        ctx.originX() + Double.parseDouble(a[1]),
                        ctx.originY() + Double.parseDouble(a[2]),
                        ctx.originZ() + Double.parseDouble(a[3]),
                        a.length > 4 ? Float.parseFloat(a[4]) : p.rotationYaw,
                        a.length > 5 ? Float.parseFloat(a[5]) : p.rotationPitch);
                } else if (cmd.startsWith("log")) {
                    log(ctx, "cmd " + cmd);
                }
            }
            StringBuilder s = new StringBuilder();
            for (Object o : p.getActivePotionEffects()) {
                PotionEffect e = (PotionEffect) o;
                s.append(e.getEffectName())
                    .append('/')
                    .append(e.getDuration())
                    .append(' ');
            }
            long until = p.getEntityData()
                .getCompoundTag(net.minecraft.entity.player.EntityPlayer.PERSISTED_NBT_TAG)
                .getLong("gg_antidote_until");
            log(
                ctx,
                "potions: [" + s
                    + "] antidoteUntil="
                    + until
                    + " now="
                    + p.worldObj.getTotalWorldTime()
                    + " hp="
                    + p.getHealth());
            return out.build();
        }
    }

    /** A Launch Control with free sides and a pad, owned by the first player, for the X and Z persistence check. */
    private static final class FreeLaunchControl extends Setup {

        @Override
        public String name() {
            return "testing.icbm_i5_lc";
        }

        @Override
        protected ScenarioResult go(ScenarioContext ctx, ScenarioResult.Builder out) {
            ctx.clearArea(14, 8);
            floor(ctx, -10, 14, -6, 14);
            final List<EntityPlayerMP> ps = players();
            if (ps.isEmpty()) {
                return ScenarioResult.fail(name(), "needs a player");
            }
            final EntityPlayerMP p = ps.get(0);
            p.setGameType(WorldSettings.GameType.SURVIVAL);
            p.capabilities.isFlying = false;
            p.capabilities.allowFlying = false;
            p.sendPlayerAbilities();
            p.setPositionAndUpdate(ctx.originX() - 1.5, ctx.originY(), ctx.originZ() + 0.5);
            final IGregTechTileEntity lc = DefenseScenarios.place(ctx, GregGunsMTEIDs.LAUNCH_CONTROL, 0, 0, 0);
            lc.setOwnerName(p.getCommandSenderName());
            lc.setOwnerUuid(p.getUniqueID());
            return ScenarioResult.pending(name(), 100, t -> {
                if (t < 40) {
                    return null;
                }
                IGregTechTileEntity live = DefenseScenarios.live(ctx, 0, 0, 0);
                live.setOwnerName(p.getCommandSenderName());
                live.setOwnerUuid(p.getUniqueID());
                MTELaunchControl c = (MTELaunchControl) live.getMetaTileEntity();
                log(ctx, "lc at 0,0,0 x=[" + c.xText() + "] z=[" + c.zText() + "] owner " + p.getCommandSenderName());
                return out.build();
            });
        }
    }
}
