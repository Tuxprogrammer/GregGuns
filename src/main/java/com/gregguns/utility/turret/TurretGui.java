package com.gregguns.utility.turret;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;

import gregtech.common.gui.modularui.singleblock.base.MTETieredMachineBlockBaseGui;

/**
 * The Turret panel: nine ammo slots, nine empty-magazine slots, the gun and armor slots, and three buttons (security,
 * redstone, target animals). Every button is an {@link InteractionSyncHandler} whose action runs on the server and
 * re-checks the pressing player ({@link MTETurret#cycleSecurity} and friends), so a modified client cannot change a
 * setting it may not. The settings reach the client through read-only sync values. Built on both sides.
 *
 * <p>
 * This layout is a placeholder: GT's standard frame with plain buttons whose labels are one character and whose
 * tooltips say the rest. The Techguns art (power bar, repair bar, animal icons) is not drawn.
 */
// Spec: docs/techguns-spec/09-machines-utility/turret.md (Container, GUI), docs/gregguns-spec/08-machines-production/
// modularui2-guis.md
// Deviation: GT's frame replaces turretBase_GUI.png; the repair countdown is a tooltip line on the output grid's
// label, not a bar.
final class TurretGui extends MTETieredMachineBlockBaseGui<MTETurret> {

    private static final String SECURITY = "security";
    private static final String REDSTONE = "redstone";
    private static final String ANIMALS = "animals";
    private static final int BUTTON_SIZE = 16;
    private static final String[] SECURITY_LABELS = { "0", "1", "2" };
    private static final String[] REDSTONE_LABELS = { "I", "+", "-" };

    TurretGui(MTETurret machine) {
        super(machine);
    }

    @Override
    protected int getBasePanelHeight() {
        // The nine-by-nine slot grids and the button row need more room than GT's single-row machines.
        return super.getBasePanelHeight() + 24;
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        // Read-only on the server's side of the channel: applyClientSync ignores a server-side call.
        syncManager.syncValue("settings_security", 0, new IntSyncValue(machine::securityId, machine::syncSecurity));
        syncManager.syncValue("settings_redstone", 0, new IntSyncValue(machine::redstoneId, machine::syncRedstone));
        syncManager.syncValue("settings_animals", 0, new IntSyncValue(machine::animalsFlag, machine::syncAnimals));
        syncManager.syncValue("settings_repair", 0, new IntSyncValue(machine::repairTicksForSync, machine::syncRepair));
        syncManager.syncValue(
            SECURITY,
            0,
            new InteractionSyncHandler().setOnMousePressed(mouse -> machine.cycleSecurity(syncManager.getPlayer())));
        syncManager.syncValue(
            REDSTONE,
            0,
            new InteractionSyncHandler().setOnMousePressed(mouse -> machine.cycleRedstone(syncManager.getPlayer())));
        syncManager.syncValue(
            ANIMALS,
            0,
            new InteractionSyncHandler().setOnMousePressed(mouse -> machine.toggleAnimals(syncManager.getPlayer())));
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
        content.child(
            new TextWidget<>(
                IKey.dynamic(
                    () -> machine.logic()
                        .isDead() || machine.repairSecondsLeft() > 0
                            ? net.minecraft.util.StatCollector
                                .translateToLocalFormatted("gregguns.turret.gui.repair", machine.repairSecondsLeft())
                            : net.minecraft.util.StatCollector.translateToLocal("gregguns.turret.gui.title")))
                                .pos(0, 0));
        content.child(grid(MTETurret.AMMO_FIRST, 0, 12, true));
        content.child(grid(MTETurret.OUT_FIRST, 108, 12, false));
        content.child(
            new ItemSlot().slot(single(MTETurret.GUN_SLOT, MTETurret::isGun))
                .pos(63, 12));
        content.child(
            new ItemSlot().slot(single(MTETurret.ARMOR_SLOT, MTETurret::isPlate))
                .pos(81, 12));
        content.child(
            button(
                SECURITY,
                55,
                34,
                () -> SECURITY_LABELS[Math.min(2, machine.securityId())],
                "gregguns.turret.gui.security"));
        content.child(
            button(
                REDSTONE,
                73,
                34,
                () -> REDSTONE_LABELS[Math.min(2, machine.redstoneId())],
                "gregguns.turret.gui.redstone"));
        content.child(
            button(ANIMALS, 91, 34, () -> machine.animalsFlag() == 1 ? "Y" : "N", "gregguns.turret.gui.animals"));
        return content;
    }

    private IWidget button(String name, int x, int y, java.util.function.Supplier<String> label, String tooltipKey) {
        return new ButtonWidget<>().syncHandler(name, 0)
            .size(BUTTON_SIZE, BUTTON_SIZE)
            .pos(x, y)
            .overlay(IKey.dynamic(label))
            .tooltipDynamic(tooltip -> tooltip.addLine(IKey.lang(tooltipKey)));
    }

    private ModularSlot single(int index, java.util.function.Predicate<ItemStack> filter) {
        return new ModularSlot(machine.inventoryHandler, index) {

            @Override
            public int getItemStackLimit(ItemStack stack) {
                return 1;
            }
        }.filter(stack -> filter.test(stack) && (index != MTETurret.GUN_SLOT || machine.mayMount(stack)));
    }

    private IWidget grid(int first, int x, int y, boolean input) {
        ParentWidget<?> group = new ParentWidget<>().pos(x, y)
            .size(54, 54);
        for (int i = 0; i < 9; i++) {
            ModularSlot slot = new ModularSlot(machine.inventoryHandler, first + i);
            if (!input) {
                slot.filter(stack -> false);
            }
            group.child(
                new ItemSlot().slot(slot)
                    .pos((i % 3) * 18, (i / 3) * 18));
        }
        return group;
    }
}
