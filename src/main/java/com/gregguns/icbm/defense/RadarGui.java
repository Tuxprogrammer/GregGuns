package com.gregguns.icbm.defense;

import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gregguns.icbm.detect.Contact;

import gregtech.api.enums.GTValues;
import gregtech.common.gui.modularui.singleblock.base.MTETieredMachineBlockBaseGui;

/**
 * The radar panel: a dark screen with the state, the range, the contact counts, the nearest contact and its impact
 * point, the power; a field for the protected radius; the data stick slot with Save link. The numbers reach the client
 * as read-only sync values; the radius field and the button re-check on the server ({@link MTERadar#setRadiusText},
 * {@link MTERadar#saveLink}). Built on both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (GUI)
final class RadarGui extends MTETieredMachineBlockBaseGui<MTERadar> {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 190;
    private static final int CONTENT = WIDTH - 14;
    private static final int TEXT_DARK = 0xFF404040;
    private static final int TEXT_LIVE = 0xFF202020;
    private static final int SCREEN_BG = 0xFF1A1A1A;
    private static final int SCREEN_EDGE = 0xFF000000;
    private static final int AMBER = 0xFFFFB000;
    private static final int ALARM = 0xFFFF6060;
    private static final int GOOD = 0xFF55FF55;
    private static final Pattern DIGITS = Pattern.compile("[0-9]{0,5}");

    private static final class State {

        int state = RadarRules.State.OFF.ordinal();
        int range;
        int detected;
        int incoming;
        long eu;
        String nearest = "";
    }

    private final State state = new State();

    RadarGui(MTERadar machine) {
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
        sync.syncValue("field_radius", 0, new StringSyncValue(machine::radiusText, machine::setRadiusText));
        sync.syncValue(
            "st_state",
            0,
            new IntSyncValue(
                () -> machine.state()
                    .ordinal(),
                v -> state.state = v));
        sync.syncValue("st_range", 0, new IntSyncValue(machine::rangeIfOn, v -> state.range = v));
        sync.syncValue(
            "st_det",
            0,
            new IntSyncValue(
                () -> machine.sweep()
                    .detected()
                    .size(),
                v -> state.detected = v));
        sync.syncValue(
            "st_inc",
            0,
            new IntSyncValue(
                () -> machine.sweep()
                    .incoming()
                    .size(),
                v -> state.incoming = v));
        sync.syncValue(
            "st_eu",
            0,
            new LongSyncValue(
                () -> machine.getBaseMetaTileEntity()
                    .getStoredEU(),
                v -> state.eu = v));
        sync.syncValue("st_near", 0, new StringSyncValue(this::nearestText, v -> state.nearest = v));
        sync.syncValue(
            "save_link",
            0,
            new InteractionSyncHandler().setOnMousePressed(m -> machine.saveLink(sync.getPlayer())));
    }

    private String nearestText() {
        java.util.List<Contact> list = machine.sweep()
            .detected();
        if (list.isEmpty()) {
            return "";
        }
        Contact c = list.get(0);
        return "T" + c.tier() + " " + (int) c.impactX() + ", " + (int) c.impactZ();
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        ParentWidget<?> content = new ParentWidget<>().expanded()
            .fullWidth();
        ParentWidget<?> screen = new ParentWidget<>().pos(0, 0)
            .size(CONTENT, 49)
            .background(screenDrawable());
        screen.child(text(this::stateText, 4, 3, CONTENT - 8, 9, this::stateColor));
        screen.child(text(() -> trf("gregguns.icbm.radar.gui.range", state.range), 4, 12, CONTENT - 8, 9, () -> AMBER));
        screen.child(
            text(
                () -> trf("gregguns.icbm.radar.gui.contacts", state.detected, state.incoming),
                4,
                21,
                CONTENT - 8,
                9,
                () -> state.incoming > 0 ? ALARM : AMBER));
        screen.child(
            text(
                () -> state.nearest.isEmpty() ? tr("gregguns.icbm.radar.gui.nearest_none")
                    : trf("gregguns.icbm.radar.gui.nearest", state.nearest),
                4,
                30,
                CONTENT - 8,
                9,
                () -> AMBER));
        screen.child(
            text(
                () -> trf("gregguns.icbm.radar.gui.power", state.eu, RadarRules.euPerTick(GTValues.V[machine.mTier])),
                4,
                39,
                CONTENT - 8,
                9,
                () -> state.state == RadarRules.State.OFF.ordinal() ? ALARM : AMBER));
        content.child(screen);

        TextWidget<?> radiusLabel = text(() -> tr("gregguns.icbm.radar.gui.radius"), 0, 54, 90, 10, () -> TEXT_DARK);
        radiusLabel.tooltipDynamic(tip -> tip.addLine(IKey.lang("gregguns.icbm.radar.gui.radius.tip")));
        content.child(radiusLabel);
        content.child(
            new TextFieldWidget().syncHandler("field_radius", 0)
                .setPattern(DIGITS)
                .setMaxLength(5)
                .size(50, 14)
                .pos(CONTENT - 50, 51));

        content.child(
            new ItemSlot()
                .slot(new ModularSlot(machine.inventoryHandler, MTERadar.SLOT_STICK).filter(MTERadar::isStick))
                .pos(0, 72));
        content.child(
            new ButtonWidget<>().syncHandler("save_link", 0)
                .size(CONTENT - 22, 14)
                .pos(22, 72)
                .overlay(
                    IKey.dynamic(() -> tr("gregguns.icbm.radar.gui.save"))
                        .color(() -> TEXT_LIVE))
                .tooltipDynamic(tip -> tip.addLine(IKey.lang("gregguns.icbm.radar.gui.save.tip"))));
        return content;
    }

    private String stateText() {
        return tr(RadarRules.State.values()[state.state].langKey());
    }

    private int stateColor() {
        RadarRules.State s = RadarRules.State.values()[state.state];
        return s == RadarRules.State.THREAT ? ALARM : s == RadarRules.State.OFF ? ALARM : GOOD;
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
