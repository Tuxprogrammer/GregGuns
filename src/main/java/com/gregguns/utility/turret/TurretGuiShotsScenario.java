package com.gregguns.utility.turret;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.GregGuns;
import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.item.gun.GunItems;
import com.gregguns.machine.GregGunsMTEIDs;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * {@code utility.turret_gui_shots}: places three MV Turrets and tells one connected client (Alice by preference) to
 * right-click each and photograph the screen: idle and empty (owner); a stranger's public turret (Alice is not the
 * owner, so the security button is locked); and last, armed with a gun, ammo, a plate, power, a zombie to shoot and
 * the non-default redstone and security modes (owner). It goes last because an armed turret shoots its neighbors'
 * turrets. A visual aid for the dev autotest hooks (see docs/testing/README.md), so
 * it is not part of {@code test all}; the evidence is the {@code turret-gui-*} screenshots. Dev environment only,
 * server side.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md (GUI)
final class TurretGuiShotsScenario implements Scenario {

    static final String NAME = "utility.turret_gui_shots";
    private static final String PLAYER = "Alice";
    /** Ticks before the first case: GT may replace a fresh tile entity in its first ticks. */
    private static final int WARMUP = 60;
    private static final int STEP = 110;
    private static final String[] LABELS = { "idle", "not-owner", "loaded" };

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Opens the Turret screen idle, loaded and as a non-owner on one client and signals screenshots. Needs 1 client with the autotest hooks.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    @Override
    public int areaRadius() {
        return 40;
    }

    @SuppressWarnings("unchecked")
    private static EntityPlayerMP pick() {
        List<EntityPlayerMP> all = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        all.sort(Comparator.comparing(p -> p.getCommandSenderName()));
        for (EntityPlayerMP p : all) {
            if (p.getCommandSenderName()
                .equalsIgnoreCase(PLAYER)) {
                return p;
            }
        }
        return all.isEmpty() ? null : all.get(0);
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }

    private static IGregTechTileEntity base(ScenarioContext ctx, int dx) {
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + dx + 2, ctx.originY(), ctx.originZ());
        return tile instanceof IGregTechTileEntity ? (IGregTechTileEntity) tile : null;
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        EntityPlayerMP a = pick();
        if (a == null) {
            return ScenarioResult.fail(NAME, "needs a connected player (" + PLAYER + " by preference)");
        }
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.TURRET_MV];
        if (prototype == null) {
            return ScenarioResult.fail(NAME, "the MV Turret is not registered (utility module off?)");
        }
        ctx.clearArea(40, 4);
        a.setGameType(WorldSettings.GameType.SURVIVAL);
        a.inventory.clearInventory(null, -1);
        final UUID stranger = UUID.nameUUIDFromBytes("gg-turret-gui-stranger".getBytes(StandardCharsets.UTF_8));
        final int ox = ctx.originX();
        final int oy = ctx.originY();
        final int oz = ctx.originZ();
        for (int i = 0; i < LABELS.length; i++) {
            int dx = i * 12;
            ctx.setBlock(dx + 2, 0, 0, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
            IGregTechTileEntity base = base(ctx, dx);
            if (base == null) {
                return ScenarioResult.fail(NAME, "no GT tile entity at case " + LABELS[i]);
            }
            base.setInitialValuesAsNBT(null, (short) GregGunsMTEIDs.TURRET_MV);
            base.setFrontFacing(ForgeDirection.WEST);
            boolean mine = i != 1;
            base.setOwnerName(mine ? a.getCommandSenderName() : "Brenda");
            base.setOwnerUuid(mine ? a.getUniqueID() : stranger);
        }
        signal(a, "hud", "off");
        final EntityZombie[] zombie = { null };
        final int total = WARMUP + LABELS.length * STEP;
        return ScenarioResult.pending(NAME, total + 200, t -> {
            int i = (t - WARMUP) / STEP;
            int step = (t - WARMUP) % STEP;
            if (t >= WARMUP && i < LABELS.length) {
                int dx = i * 12;
                switch (step) {
                    case 1 -> {
                        a.playerNetServerHandler.setPlayerLocation(ox + dx + 0.5, oy, oz + 0.5, -90.0f, 29.0f);
                        if (i == 2) {
                            arm(base(ctx, dx), a);
                            zombie[0] = new EntityZombie(ctx.world());
                            zombie[0].setLocationAndAngles(ox + dx + 2.5, oy, oz - 7.5, 0.0F, 0.0F);
                            zombie[0].addPotionEffect(new PotionEffect(Potion.fireResistance.id, 4000));
                            zombie[0].addPotionEffect(new PotionEffect(Potion.resistance.id, 4000, 4));
                            ctx.spawn(zombie[0]);
                        }
                        if (i == 1) {
                            // A public turret of a stranger: Alice may open it but is not the owner.
                            base(ctx, dx).setOwnerUuid(stranger);
                        }
                    }
                    case 3 -> signal(a, "look", "-90", "29");
                    case 50 -> signal(a, "use");
                    case 90 -> signal(a, "shot", "turret-gui-" + LABELS[i]);
                    case 100 -> signal(a, "close");
                    case 104 -> {
                        if (zombie[0] != null) {
                            zombie[0].setDead();
                        }
                        ctx.world()
                            .setBlockToAir(ox + dx + 2, oy, oz);
                    }
                    default -> {}
                }
            }
            if (t < total) {
                return null;
            }
            signal(a, "hud", "on");
            return ScenarioResult.builder(NAME)
                .note("signals sent to " + a.getCommandSenderName() + "; read the turret-gui-* screenshots")
                .build();
        });
    }

    /** The loaded case: a gun, ammo, a steel plate, power, and the longest redstone and security texts. */
    private static void arm(IGregTechTileEntity base, EntityPlayerMP owner) {
        base.setInventorySlotContents(MTETurret.GUN_SLOT, new ItemStack(GunItems.get("pistol_9mm")));
        Item mag = GameRegistry.findItem(GregGuns.MODID, "mag_9mm_pistol");
        base.setInventorySlotContents(0, new ItemStack(mag, 4));
        base.setInventorySlotContents(1, new ItemStack(mag, 2));
        Item plate = GameRegistry.findItem(GregGuns.MODID, "turret_armor_steel");
        base.setInventorySlotContents(MTETurret.ARMOR_SLOT, new ItemStack(plate));
        base.increaseStoredEnergyUnits(base.getEUCapacity() / 2, true);
        MTETurret turret = (MTETurret) base.getMetaTileEntity();
        turret.cycleRedstone(owner);
        turret.cycleRedstone(owner);
        turret.cycleSecurity(owner);
    }
}
