package com.gregguns.utility;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * {@code utility.gui_opens.<block>}: a connected survival client right-clicks the block with an empty hand (the
 * autotest
 * {@code use} signal, a vanilla right-click) and the scenario asserts that the server opened a container for that
 * player.
 * Regression for the beta client pass: the Repair Bench, the Camo Bench and the Turret extend GT's tiered machine base,
 * whose {@code onRightclick} returns false, so a click opened nothing and logged nothing. The Turret is owner-only by
 * default for a stranger's block, so {@code turret_stranger} asserts the opposite: no container, a chat refusal
 * instead.
 * Needs one client with the autotest hooks, so it is not part of {@code test all}. Dev environment only, server side.
 */
// Spec: docs/gregguns-spec/09-machines-utility/index.md (GUIs open on a plain right-click)
public final class GuiOpensScenario implements Scenario {

    private static final String PLAYER = "Alice";
    /** GT refuses use of a machine younger than 40 ticks; the click waits this long. */
    private static final int USE_AT = 55;
    private static final int CHECK_AT = 80;
    private static final int END = 100;

    private final String block;
    private final int mteId;
    private final boolean ownedByPlayer;

    private GuiOpensScenario(String block, int mteId, boolean ownedByPlayer) {
        this.block = block;
        this.mteId = mteId;
        this.ownedByPlayer = ownedByPlayer;
    }

    /**
     * @param registry where to add the four scenarios
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new GuiOpensScenario("repair_bench", GregGunsMTEIDs.REPAIR_BENCH_LV, true));
        registry.register(new GuiOpensScenario("camo_bench", GregGunsMTEIDs.CAMO_BENCH, true));
        registry.register(new GuiOpensScenario("turret", GregGunsMTEIDs.TURRET_MV, true));
        registry.register(new GuiOpensScenario("turret_stranger", GregGunsMTEIDs.TURRET_MV, false));
    }

    @Override
    public String name() {
        return "utility.gui_opens." + block;
    }

    @Override
    public String description() {
        return ownedByPlayer
            ? "A survival client right-clicks the " + block + " it owns: the GUI container opens. Needs 1 client."
            : "A survival client right-clicks a stranger's turret: no container opens (owner-only). Needs 1 client.";
    }

    @Override
    public boolean includedInAll() {
        return false;
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

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        EntityPlayerMP a = pick();
        if (a == null) {
            return ScenarioResult.fail(name(), "needs a connected player (" + PLAYER + " by preference)");
        }
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[mteId];
        if (prototype == null) {
            return ScenarioResult.fail(name(), "MTE " + mteId + " is not registered (machines/utility module off?)");
        }
        ctx.clearArea(8, 4);
        // The block sits two blocks east of the player, who stands at the origin and looks at it.
        ctx.setBlock(2, 0, 0, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + 2, ctx.originY(), ctx.originZ());
        if (!(tile instanceof IGregTechTileEntity)) {
            return ScenarioResult.fail(name(), "no GT tile entity at the block position");
        }
        IGregTechTileEntity base = (IGregTechTileEntity) tile;
        base.setInitialValuesAsNBT(null, (short) mteId);
        base.setFrontFacing(ForgeDirection.WEST);
        final UUID owner = ownedByPlayer ? a.getUniqueID()
            : UUID.nameUUIDFromBytes("gg-gui-opens-stranger".getBytes(StandardCharsets.UTF_8));
        base.setOwnerName(ownedByPlayer ? a.getCommandSenderName() : "Brenda");
        base.setOwnerUuid(owner);
        a.setGameType(WorldSettings.GameType.SURVIVAL);
        a.inventory.clearInventory(null, -1);
        a.playerNetServerHandler
            .setPlayerLocation(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, -90.0f, 29.0f);
        final boolean expectOpen = ownedByPlayer;
        return ScenarioResult.pending(name(), END + 100, t -> {
            if (t == 1 && !ownedByPlayer) {
                // The turret starts PUBLIC; lock it as its owner would, then hand the block to a stranger.
                TileEntity live = ctx.world()
                    .getTileEntity(ctx.originX() + 2, ctx.originY(), ctx.originZ());
                IGregTechTileEntity liveBase = (IGregTechTileEntity) live;
                if (liveBase.getMetaTileEntity() instanceof com.gregguns.utility.turret.MTETurret) {
                    com.gregguns.utility.turret.MTETurret turret = (com.gregguns.utility.turret.MTETurret) liveBase
                        .getMetaTileEntity();
                    liveBase.setOwnerUuid(a.getUniqueID());
                    turret.cycleSecurity(a);
                    liveBase.setOwnerUuid(owner);
                }
            }
            if (t == 2) {
                signal(a, "look", "-90", "29");
            } else if (t == USE_AT) {
                signal(a, "use");
            } else if (t == CHECK_AT) {
                boolean open = a.openContainer != null && a.openContainer != a.inventoryContainer;
                if (open == expectOpen) {
                    signal(a, "shot", "gui-open-" + block);
                } else {
                    return ScenarioResult.fail(
                        name(),
                        expectOpen ? "right-click opened no container for the owner"
                            : "a stranger's right-click opened a container");
                }
            } else if (t == CHECK_AT + 10) {
                signal(a, "close");
            }
            if (t < END) {
                return null;
            }
            return ScenarioResult.builder(name())
                .note(
                    expectOpen ? "container opened for the owner; read the gui-open-" + block + " screenshot"
                        : "stranger refused, no container")
                .build();
        });
    }
}
