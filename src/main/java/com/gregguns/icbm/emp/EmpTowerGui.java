package com.gregguns.icbm.emp;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

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
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gregguns.core.config.IcbmConfig;
import com.gregguns.icbm.IcbmSync;

import gregtech.common.gui.modularui.singleblock.base.MTETieredMachineBlockBaseGui;

/**
 * The EMP Tower panel: a dark status screen (state, EU against the cost, cooldown, the last pulse), the radius field,
 * the mode button and the Fire button. Every button runs on the server and re-checks the pressing player
 * ({@link MTEEmpTower#mayUse}); the radius field writes through a string sync value the server parses and clamps. The
 * status numbers reach the client as read-only sync values, so the screen never reads the world. Built on both sides.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/emp-tower.md (GUI)
final class EmpTowerGui extends MTETieredMachineBlockBaseGui<MTEEmpTower> {

    private static final String F_FIRE = "fire";
    private static final String F_MODE = "mode";
    private static final int WIDTH = 176;
    private static final int HEIGHT = 190;
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
    private static final Pattern DIGITS = Pattern.compile("[0-9]{0,4}");

    /** What the client knows, filled by the read-only sync values. */
    private static final class State {

        int gate = EmpRules.Gate.NO_POWER.ordinal();
        long eu;
        long cost;
        int cooldown;
        int mode;
        int devices = -1;
        int missiles = -1;
        int issues = -1;
    }

    private final State state = new State();

    EmpTowerGui(MTEEmpTower machine) {
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
        sync.syncValue(
            "field_radius",
            0,
            IcbmSync.editableText(
                IcbmSync.guard(machine, () -> Integer.toString(machine.radius()), "0"),
                v -> machine.setRadiusFromGui(sync.getPlayer(), parse(v))));
        sync.syncValue(
            "st_gate",
            0,
            new IntSyncValue(
                IcbmSync.guard(
                    machine,
                    () -> machine.gate()
                        .ordinal(),
                    0),
                v -> state.gate = v));
        sync.syncValue(
            "st_eu",
            0,
            new LongSyncValue(
                IcbmSync.guard(
                    machine,
                    () -> machine.getBaseMetaTileEntity()
                        .getStoredEU(),
                    0L),
                v -> state.eu = v));
        sync.syncValue("st_cost", 0, new LongSyncValue(IcbmSync.guard(machine, machine::cost, 0L), v -> state.cost = v));
        sync.syncValue("st_cool", 0, new IntSyncValue(IcbmSync.guard(machine, machine::cooldownLeft, 0), v -> state.cooldown = v));
        sync.syncValue(
            "st_mode",
            0,
            new IntSyncValue(
                IcbmSync.guard(
                    machine,
                    () -> machine.mode()
                        .ordinal(),
                    0),
                v -> state.mode = v));
        sync.syncValue(
            "st_devices",
            0,
            new IntSyncValue(
                IcbmSync.guard(machine, () -> machine.lastResult() == null ? -1 : machine.lastResult().devices, -1),
                v -> state.devices = v));
        sync.syncValue(
            "st_missiles",
            0,
            new IntSyncValue(
                IcbmSync.guard(
                    machine,
                    () -> machine.lastResult() == null ? -1
                        : machine.lastResult().missilesDestroyed + machine.lastResult().missilesFried
                            + machine.lastResult().missilesTriggered,
                    -1),
                v -> state.missiles = v));
        sync.syncValue(
            "st_issues",
            0,
            new IntSyncValue(
                IcbmSync.guard(machine, () -> machine.lastResult() == null ? -1 : machine.lastResult().issues, -1),
                v -> state.issues = v));
        sync.syncValue(
            F_FIRE,
            0,
            new InteractionSyncHandler().setOnMousePressed(m -> machine.fireFromButton(sync.getPlayer())));
        sync.syncValue(
            F_MODE,
            0,
            new InteractionSyncHandler().setOnMousePressed(m -> machine.cycleModeFromButton(sync.getPlayer())));
    }

    private static int parse(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        ParentWidget<?> content = new ParentWidget<>().expanded()
            .fullWidth();

        ParentWidget<?> screen = new ParentWidget<>().pos(0, 0)
            .size(CONTENT, 40)
            .background(screenDrawable());
        screen.child(text(this::statusText, 4, 3, CONTENT - 8, 9, () -> gate().isGood() ? GOOD : ALARM));
        screen.child(
            text(
                () -> trf("gregguns.icbm.emp.gui.power", state.eu, state.cost),
                4,
                13,
                CONTENT - 8,
                9,
                () -> state.eu >= state.cost ? AMBER : ALARM));
        // Four rows of at most 26 characters each (EmpLangTest): the mode shows on its own button, so no row for it.
        screen.child(text(this::lastText, 4, 22, CONTENT - 8, 9, () -> AMBER));
        screen.child(text(this::lastTotalsText, 4, 31, CONTENT - 8, 9, () -> AMBER));
        content.child(screen);

        content.child(
            text(() -> tr("gregguns.icbm.emp.gui.radius"), 0, 47, 40, 10, () -> TEXT_DARK)
                .tooltipDynamic(tip -> tip.addLine(IKey.lang("gregguns.icbm.emp.gui.radius.tip"))));
        content.child(
            new TextFieldWidget().syncHandler("field_radius", 0)
                .setPattern(DIGITS)
                .setMaxLength(4)
                .size(40, 14)
                .pos(42, 44));
        content.child(
            text(() -> trf("gregguns.icbm.emp.gui.max", IcbmConfig.empMaxRadius), 88, 47, 80, 10, () -> TEXT_DARK));

        content.child(
            control(
                F_MODE,
                0,
                62,
                CONTENT,
                () -> trf(
                    "gregguns.icbm.emp.gui.modebtn",
                    tr("gregguns.icbm.emp.mode." + EmpMode.values()[state.mode].id())),
                () -> true,
                () -> "gregguns.icbm.emp.gui.modebtn.tip",
                null));
        content.child(
            control(
                F_FIRE,
                0,
                80,
                CONTENT,
                () -> tr("gregguns.icbm.emp.gui.fire"),
                () -> gate().isGood(),
                () -> "gregguns.icbm.emp.gui.fire.tip",
                () -> gate().isGood() ? null : gate().langKey()));
        return content;
    }

    private EmpRules.Gate gate() {
        return EmpRules.Gate.values()[state.gate];
    }

    private String statusText() {
        if (gate() == EmpRules.Gate.COOLING) {
            return trf("gregguns.icbm.emp.status.cooling_s", state.cooldown / 20 + 1);
        }
        return tr(gate().langKey());
    }

    private String lastText() {
        if (state.devices < 0) {
            return tr("gregguns.icbm.emp.gui.last_none");
        }
        return trf("gregguns.icbm.emp.gui.last", state.devices);
    }

    private String lastTotalsText() {
        return state.devices < 0 ? "" : trf("gregguns.icbm.emp.gui.last_totals", state.issues, state.missiles);
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

    /** A labeled button. A locked one reads gray and its tooltip names the reason first, then what it does. */
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
