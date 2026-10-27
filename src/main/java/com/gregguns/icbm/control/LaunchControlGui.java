package com.gregguns.icbm.control;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
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
import com.gregguns.icbm.IcbmSync;

import gregtech.common.gui.modularui.singleblock.base.MTETieredMachineBlockBaseGui;

/**
 * The Launch Control panel: a dark status screen (state, target, distance and range, expected blast radius, EU), the X
 * and Z fields, "Use my position", the data stick slot with Load and Save, and the Launch button. Every button is an
 * {@link InteractionSyncHandler} whose action runs on the server and re-checks the pressing player; the fields write
 * through string sync values the server parses and clamps ({@link MTELaunchControl#setXText}). The Launch button is
 * locked, gray, with the reason as the tooltip, until the pre-check passes. The status numbers come to the client as
 * read-only sync values, so the screen never reads the world. Built on both sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (GUI)
final class LaunchControlGui extends MTETieredMachineBlockBaseGui<MTELaunchControl> {

    private static final String F_FIRE = "fire";
    private static final String F_ABORT = "abort";
    private static final int ABORT_W = 50;
    private static final String F_POS = "mypos";
    private static final String F_LOAD = "load";
    private static final String F_SAVE = "save";
    private static final int WIDTH = 176;
    /**
     * Panel height. GT's base panel gives the content section this much minus 88 (its border and the player
     * inventory), and the Launch and Abort row ends 128 down, so the panel must leave that much or the row hides
     * behind the inventory slots (it did at 200). A 480 px window at GUI scale 2 is 240 tall, and the title tab needs
     * about 12 above the panel, so 216 is as tall as it can go.
     */
    private static final int HEIGHT = 216;
    private static final int CONTENT = WIDTH - 14;
    private static final int BTN_H = 14;
    private static final int TEXT_DARK = 0xFF404040;
    private static final int TEXT_LIVE = 0xFF202020;
    private static final int TEXT_LOCKED = 0xFF4A4A4A;
    private static final int SCREEN_BG = 0xFF1A1A1A;
    private static final int SCREEN_EDGE = 0xFF000000;
    private static final int AMBER = 0xFFFFB000;
    private static final int ALARM = 0xFFFF6060;
    private static final int GOOD = 0xFF55FF55;
    private static final Pattern COORD = Pattern.compile("-?[0-9]{0,8}");

    /** What the client knows, filled by the read-only sync values. */
    private static final class State {

        int status = ControlStatus.NO_TARGET.ordinal();
        int min;
        int max;
        int distance;
        int radiusTenths;
        int lastShot = -1;
        int countdown = -1;
        int lastStick = -1;
        long eu;
        String target = "";
    }

    private final State state = new State();

    LaunchControlGui(MTELaunchControl machine) {
        super(machine);
    }

    @Override
    public ModularPanel build(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        return super.build(guiData, syncManager, uiSettings);
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
        // Fields: the client writes, the server parses and clamps.
        sync.syncValue("field_x", 0, IcbmSync.editableText(machine::xText, machine::setXText));
        sync.syncValue("field_z", 0, IcbmSync.editableText(machine::zText, machine::setZText));
        // Status: the getters run on the server, the setters fill the state the screen reads.
        sync.syncValue("st_status", 0, new IntSyncValue(() -> snap().status.ordinal(), v -> state.status = v));
        sync.syncValue("st_min", 0, new IntSyncValue(() -> snap().minRange, v -> state.min = v));
        sync.syncValue("st_max", 0, new IntSyncValue(() -> snap().maxRange, v -> state.max = v));
        sync.syncValue("st_dist", 0, new IntSyncValue(() -> snap().distance, v -> state.distance = v));
        sync.syncValue("st_radius", 0, new IntSyncValue(() -> snap().radiusTenths, v -> state.radiusTenths = v));
        sync.syncValue(
            "st_shot",
            0,
            new IntSyncValue(
                () -> machine.lastShot() == null ? -1
                    : machine.lastShot()
                        .ordinal(),
                v -> state.lastShot = v));
        sync.syncValue(
            "st_stick",
            0,
            new IntSyncValue(
                () -> machine.lastStick() == null ? -1
                    : machine.lastStick()
                        .ordinal(),
                v -> state.lastStick = v));
        sync.syncValue(
            "st_eu",
            0,
            new LongSyncValue(
                () -> machine.getBaseMetaTileEntity()
                    .getStoredEU(),
                v -> state.eu = v));
        sync.syncValue(
            "st_target",
            0,
            new StringSyncValue(
                () -> machine.target()
                    .toString(),
                v -> state.target = v));
        sync.syncValue(
            F_FIRE,
            0,
            new InteractionSyncHandler().setOnMousePressed(m -> machine.fireFromButton(sync.getPlayer())));
        sync.syncValue(
            F_ABORT,
            0,
            new InteractionSyncHandler().setOnMousePressed(m -> machine.abortFromButton(sync.getPlayer())));
        sync.syncValue("st_countdown", 0, new IntSyncValue(() -> snap().countdown, v -> state.countdown = v));
        sync.syncValue(
            F_POS,
            0,
            new InteractionSyncHandler().setOnMousePressed(m -> machine.useMyPosition(sync.getPlayer())));
        sync.syncValue(
            F_LOAD,
            0,
            new InteractionSyncHandler().setOnMousePressed(m -> machine.loadFromStick(sync.getPlayer())));
        sync.syncValue(
            F_SAVE,
            0,
            new InteractionSyncHandler().setOnMousePressed(m -> machine.saveToStick(sync.getPlayer())));
    }

    private MTELaunchControl.Snapshot snap() {
        return machine.snapshot();
    }

    // --- layout -----------------------------------------------------------------------------------------------

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        ParentWidget<?> content = new ParentWidget<>().expanded()
            .fullWidth();

        ParentWidget<?> screen = new ParentWidget<>().pos(0, 0)
            .size(CONTENT, 49)
            .background(screenDrawable());
        screen.child(text(this::statusText, 4, 3, CONTENT - 8, 9, Alignment.CenterLeft, this::statusColor));
        screen.child(
            text(
                () -> trf("gregguns.icbm.control.gui.target", state.target),
                4,
                12,
                CONTENT - 8,
                9,
                Alignment.CenterLeft,
                () -> AMBER));
        screen.child(
            text(
                () -> trf(
                    "gregguns.icbm.control.gui.range",
                    LaunchControlFormat.blocks(state.distance),
                    LaunchControlFormat.range(state.min, state.max)),
                4,
                21,
                CONTENT - 8,
                9,
                Alignment.CenterLeft,
                () -> AMBER));
        screen.child(
            text(
                () -> state.radiusTenths > 0
                    ? trf("gregguns.icbm.control.gui.radius", LaunchControlFormat.radius(state.radiusTenths))
                    : tr("gregguns.icbm.control.gui.radius_none"),
                4,
                30,
                CONTENT - 8,
                9,
                Alignment.CenterLeft,
                () -> AMBER));
        screen.child(
            text(
                () -> trf(
                    "gregguns.icbm.control.gui.power",
                    LaunchControlFormat.eu(state.eu),
                    MTELaunchControl.EU_PER_SHOT),
                4,
                39,
                CONTENT - 8,
                9,
                Alignment.CenterLeft,
                () -> state.eu >= MTELaunchControl.EU_PER_SHOT ? AMBER : ALARM));
        content.child(screen);

        // Coordinates.
        content.child(
            label("gregguns.icbm.control.gui.x", "gregguns.icbm.control.gui.x.tip", 0, 54, 10, Alignment.CenterLeft));
        content.child(coordField("field_x", 11, 51));
        content.child(
            label("gregguns.icbm.control.gui.z", "gregguns.icbm.control.gui.z.tip", 84, 54, 10, Alignment.CenterLeft));
        content.child(coordField("field_z", 95, 51));

        // Use my position.
        content.child(
            control(
                F_POS,
                0,
                67,
                CONTENT,
                () -> tr("gregguns.icbm.control.gui.mypos"),
                () -> true,
                () -> "gregguns.icbm.control.gui.mypos.tip",
                null));

        // Data stick.
        content.child(
            new ItemSlot()
                .slot(
                    new ModularSlot(machine.inventoryHandler, MTELaunchControl.SLOT_STICK)
                        .filter(MTELaunchControl::isStick))
                .pos(0, 84));
        content.child(
            control(
                F_LOAD,
                22,
                84,
                70,
                () -> tr("gregguns.icbm.control.gui.load"),
                this::hasStick,
                () -> "gregguns.icbm.control.gui.load.tip",
                () -> hasStick() ? null : "gregguns.icbm.control.gui.stick_missing"));
        content.child(
            control(
                F_SAVE,
                96,
                84,
                CONTENT - 96,
                () -> tr("gregguns.icbm.control.gui.save"),
                this::hasStick,
                () -> "gregguns.icbm.control.gui.save.tip",
                () -> hasStick() ? null : "gregguns.icbm.control.gui.stick_missing"));
        content.child(text(this::stickText, 22, 101, CONTENT - 22, 9, Alignment.CenterLeft, () -> TEXT_DARK));

        // Launch, with Abort beside it while a countdown runs or after ignition.
        content.child(
            control(
                F_ABORT,
                CONTENT - ABORT_W,
                114,
                ABORT_W,
                () -> tr("gregguns.icbm.control.gui.abort"),
                this::canAbort,
                () -> "gregguns.icbm.control.gui.abort.tip",
                () -> ControlStatus.values()[state.status] == ControlStatus.IGNITED
                    ? "gregguns.icbm.control.gui.abort.ignited"
                    : "gregguns.icbm.control.gui.abort.idle"));
        content.child(
            control(
                F_FIRE,
                0,
                114,
                CONTENT - ABORT_W - 4,
                () -> tr("gregguns.icbm.control.gui.fire"),
                this::canFire,
                () -> "gregguns.icbm.control.gui.fire.tip",
                () -> canFire() ? null : ControlStatus.values()[state.status].langKey()));
        return content;
    }

    private boolean canAbort() {
        return ControlStatus.values()[state.status] == ControlStatus.COUNTDOWN;
    }

    private boolean canFire() {
        return ControlStatus.values()[state.status] == ControlStatus.READY;
    }

    private boolean hasStick() {
        ItemStack s = machine.getStackInSlot(MTELaunchControl.SLOT_STICK);
        return MTELaunchControl.isStick(s);
    }

    // --- text -------------------------------------------------------------------------------------------------

    private String statusText() {
        if (ControlStatus.values()[state.status] == ControlStatus.COUNTDOWN) {
            return trf(
                "gregguns.icbm.control.status.countdown_s",
                com.gregguns.icbm.missile.LaunchTimer.seconds(state.countdown));
        }
        if (state.lastShot >= 0 && state.lastShot < ControlStatus.values().length
            && ControlStatus.values()[state.lastShot] == ControlStatus.FIRED
            && ControlStatus.values()[state.status] != ControlStatus.READY) {
            return tr(ControlStatus.FIRED.langKey());
        }
        return tr(ControlStatus.values()[state.status].langKey());
    }

    private int statusColor() {
        ControlStatus s = ControlStatus.values()[state.status];
        return s.isGood() ? GOOD : ALARM;
    }

    private String stickText() {
        if (state.lastStick < 0 || state.lastStick >= TargetStick.Outcome.values().length) {
            return "";
        }
        return tr(TargetStick.Outcome.values()[state.lastStick].langKey());
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal(key);
    }

    private static String trf(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(key, args);
    }

    // --- widgets ----------------------------------------------------------------------------------------------

    private static TextWidget<?> text(Supplier<String> value, int x, int y, int w, int h, Alignment align,
        IntSupplier color) {
        return new TextWidget<>(IKey.dynamic(value)).pos(x, y)
            .size(w, h)
            .alignment(align)
            .color(color)
            .shadow(false);
    }

    private static IWidget label(String key, String tipKey, int x, int y, int w, Alignment align) {
        TextWidget<?> label = text(() -> tr(key), x, y, w, 10, align, () -> TEXT_DARK);
        label.tooltipDynamic(tip -> tip.addLine(IKey.lang(tipKey)));
        return label;
    }

    private static IWidget coordField(String syncName, int x, int y) {
        return new TextFieldWidget().syncHandler(syncName, 0)
            .setPattern(COORD)
            .setMaxLength(9)
            .size(66, 14)
            .pos(x, y);
    }

    private static IDrawable screenDrawable() {
        return (ctx, x, y, w, h, theme) -> {
            GuiDraw.drawRect(x, y, w, h, SCREEN_EDGE);
            GuiDraw.drawRect(x + 1, y + 1, w - 2, h - 2, SCREEN_BG);
        };
    }

    /**
     * A labeled button. A locked one reads gray and its tooltip names the reason first, then what the button does.
     */
    private IWidget control(String name, int x, int y, int w, Supplier<String> text, BooleanSupplier live,
        Supplier<String> tipKey, Supplier<String> lockedKey) {
        return new ButtonWidget<>().syncHandler(name, 0)
            .size(w, BTN_H)
            .pos(x, y)
            .overlay(
                IKey.dynamic(text)
                    .color(() -> live.getAsBoolean() ? TEXT_LIVE : TEXT_LOCKED))
            .tooltipDynamic(tip -> {
                if (!live.getAsBoolean() && lockedKey != null && lockedKey.get() != null) {
                    tip.addLine(IKey.lang(lockedKey.get()));
                }
                tip.addLine(IKey.lang(tipKey.get()));
            });
    }
}
