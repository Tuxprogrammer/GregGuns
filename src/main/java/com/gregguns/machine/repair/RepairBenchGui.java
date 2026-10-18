package com.gregguns.machine.repair;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;

import gregtech.common.gui.modularui.singleblock.base.MTETieredMachineBlockBaseGui;

/**
 * The Repair Bench panel: GT's single-block frame with the nine part slots and one button per target (the four worn
 * pieces and the held item). A button press is an {@link InteractionSyncHandler} that runs on the server and calls
 * {@link MTERepairBench#repair}; no GregGuns packet is involved (GGS-08-28). Built on both sides.
 */
// Spec: docs/gregguns-spec/08-machines-production/modularui2-guis.md (GGS-08-27, GGS-08-28, GGS-08-29),
// docs/techguns-spec/09-machines-utility/repair-bench.md (GUI)
// Deviation: the Techguns bench draws its own 176 x 166 texture with wrench icons and a tooltip that lists the missing
// parts per slot. This one uses GT's standard frame and text buttons; the missing-part tooltip is a follow-up.
final class RepairBenchGui extends MTETieredMachineBlockBaseGui<MTERepairBench> {

    private static final String[] LABEL_KEYS = { "gregguns.repair.gui.helmet", "gregguns.repair.gui.chestplate",
        "gregguns.repair.gui.leggings", "gregguns.repair.gui.boots", "gregguns.repair.gui.held" };
    private static final int BUTTON_WIDTH = 32;
    private static final int BUTTON_HEIGHT = 14;

    RepairBenchGui(MTERepairBench machine) {
        super(machine);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        for (int target = 0; target <= MTERepairBench.TARGET_HELD; target++) {
            final int which = target;
            // Source: ModularUI2 InteractionSyncHandler, the server half of a ButtonWidget press.
            syncManager.syncValue(
                "repair",
                which,
                new InteractionSyncHandler()
                    .setOnMousePressed(mouse -> machine.repair(syncManager.getPlayer(), which)));
        }
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
        ParentWidget<?> content = new ParentWidget<>().expanded()
            .fullWidth();
        content.child(new TextWidget<>(IKey.lang("gregguns.repair.gui.title")).pos(0, 0));
        content.child(slotRow());
        for (int target = 0; target <= MTERepairBench.TARGET_HELD; target++) {
            content.child(
                new ButtonWidget<>().syncHandler("repair", target)
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .pos(target * (BUTTON_WIDTH + 1), 38)
                    .overlay(IKey.lang(LABEL_KEYS[target])));
        }
        return content;
    }

    private IWidget slotRow() {
        SlotGroupWidget group = SlotGroupWidget.builder()
            .row("SSSSSSSSS")
            .key('S', index -> new ItemSlot().slot(machine.inventoryHandler, index))
            .build();
        return group.pos(0, 12);
    }
}
