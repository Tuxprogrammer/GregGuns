package com.gregguns.machine.repair;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.gregguns.api.CamoChangeable;
import com.gregguns.network.GregGunsNetwork;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.gui.modularui.singleblock.base.MTETieredMachineBlockBaseGui;

/**
 * The Camo Bench panel: the item slot, and a back and a forward arrow for it and for each worn armor piece. Each press
 * sends a {@link PacketCamoCycle}; the server validates it. Labels show the current camo of what the arrows act on.
 * Built on both sides; the button lambdas run on the client.
 */
// Spec: docs/techguns-spec/09-machines-utility/camo-bench.md (GUI),
// docs/gregguns-spec/08-machines-production/modularui2-guis.md (GGS-08-27, GGS-08-29)
// Deviation: the Techguns GUI draws a live player preview and 14 buttons; this one uses GT's frame, text arrows and a
// text line per target. The preview is a visual follow-up.
final class CamoBenchGui extends MTETieredMachineBlockBaseGui<MTECamoBench> {

    private static final String[] ROW_KEYS = { "gregguns.camo.gui.item", "gregguns.camo.gui.helmet",
        "gregguns.camo.gui.chestplate", "gregguns.camo.gui.leggings", "gregguns.camo.gui.boots" };

    private PanelSyncManager syncManager;

    CamoBenchGui(MTECamoBench machine) {
        super(machine);
    }

    @Override
    protected boolean supportsPowerSwitch() {
        return false;
    }

    @Override
    protected boolean supportsMuffler() {
        return false;
    }

    @Override
    protected boolean doesAddGregTechLogo() {
        return false;
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        this.syncManager = syncManager;
        ParentWidget<?> content = new ParentWidget<>().expanded()
            .fullWidth();
        content.child(
            new ItemSlot().slot(machine.inventoryHandler, MTECamoBench.SLOT)
                .pos(0, 0));
        CamoCycle.Target[] targets = CamoCycle.Target.values();
        for (int row = 0; row < targets.length; row++) {
            final CamoCycle.Target target = targets[row];
            int y = row * 14;
            content.child(arrow("<", target, true, 22, y));
            content.child(arrow(">", target, false, 34, y));
            content.child(new TextWidget<>(IKey.dynamic(() -> label(target))).pos(48, y + 3));
        }
        return content;
    }

    private ButtonWidget<?> arrow(String glyph, CamoCycle.Target target, boolean back, int x, int y) {
        return new ButtonWidget<>().size(12, 12)
            .pos(x, y)
            .overlay(IKey.str(glyph))
            .onMousePressed(button -> {
                // Client side: the server decides. The dimension and position let it find and check the bench.
                IGregTechTileEntity base = machine.getBaseMetaTileEntity();
                GregGunsNetwork.channel()
                    .sendToServer(
                        new PacketCamoCycle(
                            base.getWorld().provider.dimensionId,
                            base.getXCoord(),
                            base.getYCoord(),
                            base.getZCoord(),
                            target.ordinal(),
                            back));
                return true;
            });
    }

    private String label(CamoCycle.Target target) {
        ItemStack stack = target == CamoCycle.Target.BENCH_SLOT ? machine.getStackInSlot(MTECamoBench.SLOT)
            : syncManager.getPlayer().inventory.armorInventory[3 - target.armorType()];
        String row = StatCollector.translateToLocal(ROW_KEYS[target.ordinal()]);
        if (stack == null || !(stack.getItem() instanceof CamoChangeable)) {
            return row + ": " + StatCollector.translateToLocal("gregguns.camo.gui.none");
        }
        CamoChangeable item = (CamoChangeable) stack.getItem();
        return row + " "
            + (item.getCamo(stack) + 1)
            + "/"
            + item.camoCount(stack)
            + " "
            + StatCollector.translateToLocal(item.camoNameKey(stack));
    }
}
