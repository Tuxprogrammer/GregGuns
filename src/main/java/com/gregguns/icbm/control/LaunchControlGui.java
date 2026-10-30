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
 * The Launch Control panel: a dark status screen (state, the silo or pad it is bound to, target, distance and range,
 * expected blast radius, EU; with no target set the state line reports the silo or pad, since the target line already
 * says the target is missing), the X and Z fields, "Use my position", the target slot (Radar Gun) with Load and Save,
 * and the Launch button. Every button is an
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
        /** The verdict without the target, which the status line shows while no target is set. */
        int siteStatus = ControlStatus.NO_PAD.ordinal();
        int min;
        int max;
        int distance;
        int radiusTenths;
        int lastShot = -1;
        int countdown = -1;
        int lastStick = -1;
        long eu;
        /** Whether {@link #eu} is the bound silo's (D300). */
        boolean siloEu;
        String target = "";
        SiteLine site = SiteLine.NONE;
    }

    /** Screen row pitch: six rows of the 8 px game font fill the dark screen. */
    private static final int ROW = 8;
    /** The dark screen's height: six rows under a 3 px margin; the X and Z fields start 2 px below it. */
    private static final int SCREEN_H = 3 + 6 * ROW;

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
        sync.syncValue(
            "st_site_status",
            0,
            new IntSyncValue(() -> snap().siteStatus.ordinal(), v -> state.siteStatus = v));
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
        sync.syncValue("st_eu", 0, new LongSyncValue(IcbmSync.guard(machine, machine::shotEu, 0L), v -> state.eu = v));
        sync.syncValue(
            "st_eu_silo",
            0,
            new IntSyncValue(() -> machine.siloPowered() ? 1 : 0, v -> state.siloEu = v != 0));
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
        sync.syncValue(
            "st_site",
            0,
            new StringSyncValue(() -> snap().site.encode(), v -> state.site = SiteLine.decode(v)));
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

    private LaunchSnapshot snap() {
        return machine.snapshot();
    }

    // --- layout -----------------------------------------------------------------------------------------------

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        ParentWidget<?> content = new ParentWidget<>().expanded()
            .fullWidth();

        ParentWidget<?> screen = new ParentWidget<>().pos(0, 0)
            .size(CONTENT, SCREEN_H)
            .background(screenDrawable());
        screen.child(text(this::statusText, 4, 3, CONTENT - 8, ROW, Alignment.CenterLeft, this::statusColor));
        screen.child(
            text(
                () -> trf(state.site.langKey(), state.site.args()),
                4,
                3 + ROW,
                CONTENT - 8,
                ROW,
                Alignment.CenterLeft,
                () -> AMBER));
        screen.child(
            text(
                () -> !targetSet() ? tr("gregguns.icbm.control.gui.target_none")
                    : trf("gregguns.icbm.control.gui.target", state.target),
                4,
                3 + 2 * ROW,
                CONTENT - 8,
                ROW,
                Alignment.CenterLeft,
                () -> AMBER));
        screen.child(
            text(
                () -> trf(
                    "gregguns.icbm.control.gui.range",
                    LaunchControlFormat.distance(targetSet(), state.distance),
                    LaunchControlFormat.range(state.min, state.max)),
                4,
                3 + 3 * ROW,
                CONTENT - 8,
                ROW,
                Alignment.CenterLeft,
                () -> AMBER));
        screen.child(
            text(
                () -> state.radiusTenths > 0
                    ? trf("gregguns.icbm.control.gui.radius", LaunchControlFormat.radius(state.radiusTenths))
                    : tr("gregguns.icbm.control.gui.radius_none"),
                4,
                3 + 4 * ROW,
                CONTENT - 8,
                ROW,
                Alignment.CenterLeft,
                () -> AMBER));
        screen.child(
            text(
                () -> trf(
                    state.siloEu ? "gregguns.icbm.control.gui.power_silo" : "gregguns.icbm.control.gui.power",
                    LaunchControlFormat.eu(state.eu),
                    MTELaunchControl.EU_PER_SHOT),
                4,
                3 + 5 * ROW,
                CONTENT - 8,
                ROW,
                Alignment.CenterLeft,
                () -> state.eu >= MTELaunchControl.EU_PER_SHOT ? AMBER : ALARM));
        content.child(screen);

        // Coordinates.
        content.child(
            label("gregguns.icbm.control.gui.x", "gregguns.icbm.control.gui.x.tip", 0, 56, 10, Alignment.CenterLeft));
        content.child(coordField("field_x", 11, 53));
        content.child(
            label("gregguns.icbm.control.gui.z", "gregguns.icbm.control.gui.z.tip", 84, 56, 10, Alignment.CenterLeft));
        content.child(coordField("field_z", 95, 53));

        // Use my position.
        content.child(
            control(
                F_POS,
                0,
                68,
                CONTENT,
                () -> tr("gregguns.icbm.control.gui.mypos"),
                () -> true,
                () -> "gregguns.icbm.control.gui.mypos.tip",
                null));

        // Target slot: a Radar Gun (a GT data stick is accepted too, without a mention).
        content.child(
            new ItemSlot()
                .slot(
                    new ModularSlot(machine.inventoryHandler, MTELaunchControl.SLOT_STICK)
                        .filter(MTELaunchControl::isTargetCarrier))
                .overlay(gunGhost())
                .tooltipDynamic(tip -> {
                    if (!hasStick()) {
                        tip.addLine(IKey.lang("gregguns.icbm.control.gui.slot.tip"));
                    }
                })
                .pos(0, 84));
        content.child(
            control(
                F_LOAD,
                22,
                84,
                70,
                () -> tr("gregguns.icbm.control.gui.load"),
                () -> hasStick() && !launchUnderway(),
                () -> "gregguns.icbm.control.gui.load.tip",
                () -> !hasStick() ? "gregguns.icbm.control.gui.stick_missing"
                    : launchUnderway() ? "gregguns.icbm.control.gui.load.locked" : null));
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

    /** @return whether a countdown runs or the missile has left: the target is frozen until the pad is ready again */
    private boolean launchUnderway() {
        ControlStatus s = ControlStatus.values()[state.status];
        return s == ControlStatus.COUNTDOWN || s == ControlStatus.IGNITED;
    }

    /** @return whether the synced target is set */
    private boolean targetSet() {
        return !LaunchTarget.NONE.toString()
            .equals(state.target);
    }

    private boolean canAbort() {
        return ControlStatus.values()[state.status] == ControlStatus.COUNTDOWN;
    }

    private boolean canFire() {
        return ControlStatus.values()[state.status] == ControlStatus.READY;
    }

    private boolean hasStick() {
        ItemStack s = machine.getStackInSlot(MTELaunchControl.SLOT_STICK);
        return MTELaunchControl.isTargetCarrier(s);
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
        return tr(LaunchGate.statusLineKey(ControlStatus.values()[state.status], siteStatus()));
    }

    /** @return the verdict without the target, as synced; NO_PAD for a value out of range */
    private ControlStatus siteStatus() {
        return state.siteStatus >= 0 && state.siteStatus < ControlStatus.values().length
            ? ControlStatus.values()[state.siteStatus]
            : ControlStatus.NO_PAD;
    }

    private int statusColor() {
        ControlStatus s = ControlStatus.values()[state.status];
        if (s == ControlStatus.NO_TARGET) {
            // The target line says what is missing; the status line reports the silo or pad: amber when only the
            // target is missing, red when the site has a problem of its own.
            return siteStatus() == ControlStatus.READY ? AMBER : ALARM;
        }
        return s.isGood() ? GOOD : ALARM;
    }

    private String stickText() {
        if (state.lastStick < 0 || state.lastStick >= TargetStick.Outcome.values().length) {
            return hasStick() ? "" : tr("gregguns.icbm.control.gui.slot.hint");
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

    /** The Radar Gun drawn faintly in the empty target slot, so the slot shows what goes in it. */
    private IDrawable gunGhost() {
        return (ctx, x, y, w, h, theme) -> {
            if (hasStick()) {
                return;
            }
            net.minecraft.item.Item gun = com.gregguns.icbm.defense.DefenseMachines.radarGunItem();
            if (gun == null) {
                return;
            }
            new com.cleanroommc.modularui.drawable.ItemDrawable(gun).draw(ctx, x + 1, y + 1, 16, 16, theme);
            GuiDraw.drawRect(x + 1, y + 1, 16, 16, 0xAA8B8B8B);
        };
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
