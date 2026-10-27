package com.gregguns.icbm.defense;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;

import gregtech.common.gui.modularui.singleblock.base.MTETieredMachineBlockBaseGui;

/**
 * The air-defense battery panel: a dark screen with the state, the linked radar, the threats in range, the SAM count
 * and
 * the power; a row of SAM slots and the data stick slot. Read-only sync values feed the screen. Built on both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/air-defense-battery.md (GUI)
final class BatteryGui extends MTETieredMachineBlockBaseGui<MTEAirDefense> {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 190;
    private static final int CONTENT = WIDTH - 14;
    private static final int SCREEN_BG = 0xFF1A1A1A;
    private static final int SCREEN_EDGE = 0xFF000000;
    private static final int AMBER = 0xFFFFB000;
    private static final int ALARM = 0xFFFF6060;
    private static final int GOOD = 0xFF55FF55;

    private static final class State {

        int state = SamRules.State.NO_LINK.ordinal();
        int threats;
        int sams;
        long eu;
        String link = "";
    }

    private final State state = new State();

    BatteryGui(MTEAirDefense machine) {
        super(machine);
    }

    @Override
    protected int getBasePanelWidth() {
        return WIDTH;
    }

    @Override
    protected int getBasePanelHeight() {
        return HEIGHT;
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
    protected void registerSyncValues(PanelSyncManager sync) {
        super.registerSyncValues(sync);
        sync.syncValue(
            "st_state",
            0,
            new IntSyncValue(
                () -> machine.state()
                    .ordinal(),
                v -> state.state = v));
        sync.syncValue("st_threats", 0, new IntSyncValue(machine::threats, v -> state.threats = v));
        sync.syncValue("st_sams", 0, new IntSyncValue(machine::samCount, v -> state.sams = v));
        sync.syncValue(
            "st_eu",
            0,
            new LongSyncValue(
                () -> machine.getBaseMetaTileEntity()
                    .getStoredEU(),
                v -> state.eu = v));
        sync.syncValue("st_link", 0, new StringSyncValue(() -> {
            RadarLink l = machine.link();
            return l == null ? "" : l.toString();
        }, v -> state.link = v));
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        ParentWidget<?> content = new ParentWidget<>().expanded()
            .fullWidth();
        ParentWidget<?> screen = new ParentWidget<>().pos(0, 0)
            .size(CONTENT, 49)
            .background(screenDrawable());
        screen.child(text(this::stateText, 4, 3, CONTENT - 8, 9, this::stateColor));
        screen.child(
            text(
                () -> state.link.isEmpty() ? tr("gregguns.icbm.battery.gui.link_none")
                    : trf("gregguns.icbm.battery.gui.link", state.link),
                4,
                12,
                CONTENT - 8,
                9,
                () -> AMBER));
        screen.child(
            text(
                () -> trf("gregguns.icbm.battery.gui.threats", state.threats),
                4,
                21,
                CONTENT - 8,
                9,
                () -> state.threats > 0 ? ALARM : AMBER));
        screen.child(
            text(
                () -> trf("gregguns.icbm.battery.gui.sams", state.sams, SamRules.CAPACITY * 16),
                4,
                30,
                CONTENT - 8,
                9,
                () -> state.sams > 0 ? AMBER : ALARM));
        screen.child(
            text(
                () -> trf("gregguns.icbm.battery.gui.power", state.eu, SamRules.EU_PER_SHOT),
                4,
                39,
                CONTENT - 8,
                9,
                () -> state.eu >= SamRules.EU_PER_SHOT ? AMBER : ALARM));
        content.child(screen);
        for (int i = 0; i < SamRules.CAPACITY; i++) {
            content.child(
                new ItemSlot().slot(
                    new ModularSlot(machine.inventoryHandler, MTEAirDefense.SLOT_SAM + i).filter(MTEAirDefense::isSam))
                    .pos(i * 18, 54));
        }
        content.child(
            new ItemSlot()
                .slot(new ModularSlot(machine.inventoryHandler, MTEAirDefense.SLOT_STICK).filter(RadarUnit::isStick))
                .pos(CONTENT - 18, 54));
        return content;
    }

    private String stateText() {
        return tr(SamRules.State.values()[state.state].langKey());
    }

    private int stateColor() {
        SamRules.State s = SamRules.State.values()[state.state];
        return s == SamRules.State.STANDBY ? GOOD : s == SamRules.State.ENGAGING ? AMBER : ALARM;
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal(key);
    }

    private static String trf(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(key, args);
    }

    private static TextWidget<?> text(Supplier<String> value, int x, int y, int w, int h, IntSupplier color) {
        return new TextWidget<>(IKey.dynamic(value)).pos(x, y)
            .size(w, h)
            .alignment(Alignment.CenterLeft)
            .color(color)
            .shadow(false);
    }

    private static IDrawable screenDrawable() {
        return (ctx, x, y, w, h, theme) -> {
            GuiDraw.drawRect(x, y, w, h, SCREEN_EDGE);
            GuiDraw.drawRect(x + 1, y + 1, w - 2, h - 2, SCREEN_BG);
        };
    }
}
