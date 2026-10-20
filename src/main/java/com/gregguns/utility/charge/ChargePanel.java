package com.gregguns.utility.charge;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;
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
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;

/**
 * The ModularUI2 panel of an Explosive Charge, for both types. Top to bottom: title, a status banner (green "not armed"
 * or red "ARMED" with a live countdown), the fuse stepper with a burn bar, the bore section (depth and width steppers,
 * the world direction and a side diagram of the tunnel), the owner line with a plain-words access line, and the Arm
 * and Disarm buttons. The charge holds no items, so there are no inventory slots; the inventory key closes the screen
 * like any container GUI. Every button is a server-side {@link InteractionSyncHandler} that calls
 * {@link TileExplosiveCharge#pressBy}, which re-checks the owner (MUTL-31). Built on both sides.
 */
// Spec: docs/gregguns-spec/09-machines-utility/explosive-charges.md (GGS-09-40 to GGS-09-44)
// Deviation: replaces the Techguns 176 x 166 texture screen with plus/minus buttons and text rows.
public final class ChargePanel {

    /** Panel width. */
    public static final int WIDTH = 176;
    /** Panel height. */
    public static final int HEIGHT = 200;

    private static final int PAD = 7;
    private static final int CONTENT = WIDTH - 2 * PAD;
    private static final int BTN = 16;

    private static final int TEXT_DARK = 0xFF404040;
    private static final int GLYPH_ON = 0xFF202020;
    private static final int GLYPH_OFF = 0xFF8B8B8B;
    private static final int SCREEN_BG = 0xFF1A1A1A;
    private static final int SCREEN_EDGE = 0xFF000000;
    private static final int AMBER = 0xFFFFB000;
    private static final int ALARM = 0xFFFF5050;

    private ChargePanel() {}

    /**
     * @param tile     the charge
     * @param data     the open context
     * @param sync     where the sync values go
     * @param settings per-UI settings
     * @return the main panel
     */
    public static ModularPanel build(TileExplosiveCharge tile, PosGuiData data, PanelSyncManager sync,
        UISettings settings) {
        final ChargeType type = tile.params()
            .type();
        final ChargeGuiModel.State st = new ChargeGuiModel.State();
        final EntityPlayer player = data.getPlayer();
        st.load(tile.params(), !tile.getWorldObj().isRemote && tile.mayConfigure(player));

        settings.canInteractWith(
            p -> !tile.isInvalid() && tile.getWorldObj()
                .getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == tile
                && p.getDistanceSq(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5) <= 64.0);
        // The recipe viewer (NEI) lists items at the screen edge; this panel is centered and narrow, so it never
        // overlaps.

        registerValues(tile, sync, st, player);
        registerButtons(tile, sync, player);

        ModularPanel panel = ModularPanel.defaultPanel("gregguns_charge", WIDTH, HEIGHT);
        panel.child(
            text(
                () -> StatCollector.translateToLocal("tile.gregguns." + type.itemName() + ".name"),
                PAD,
                6,
                CONTENT,
                10,
                Alignment.CenterLeft,
                () -> TEXT_DARK));

        // Status banner.
        panel.child(
            text(
                () -> st.armed
                    ? StatCollector
                        .translateToLocalFormatted("gregguns.charge.gui.armed", ChargeGuiModel.seconds(st.fuse))
                    : StatCollector.translateToLocal("gregguns.charge.gui.safe"),
                PAD,
                20,
                CONTENT,
                16,
                Alignment.Center,
                () -> 0xFFFFFFFF).background(fill(() -> st.armed ? 0xFFA01818 : 0xFF2E7D32, true)));

        // Fuse.
        int y = 42;
        panel.child(label("gregguns.charge.gui.fuse", PAD, y));
        panel.child(
            text(
                () -> StatCollector.translateToLocalFormatted(
                    "gregguns.charge.gui.fuse_range",
                    ChargeGuiModel.seconds(ChargeType.FUSE_MIN),
                    ChargeGuiModel.seconds(ChargeType.FUSE_MAX)),
                PAD,
                y,
                CONTENT,
                10,
                Alignment.CenterRight,
                () -> TEXT_DARK));
        y += 11;
        panel.child(stepButton(st, type, ChargeParams.BTN_FUSE_DOWN, "-", PAD, y));
        panel.child(
            readout(
                () -> StatCollector
                    .translateToLocalFormatted("gregguns.charge.gui.seconds", ChargeGuiModel.seconds(st.fuse)),
                () -> st.armed ? ALARM : AMBER,
                PAD + BTN + 2,
                y,
                CONTENT - 2 * BTN - 4));
        panel.child(stepButton(st, type, ChargeParams.BTN_FUSE_UP, "+", PAD + CONTENT - BTN, y));
        y += BTN + 3;
        panel.child(
            new Widget<>().pos(PAD, y)
                .size(CONTENT, 5)
                .background(
                    bar(
                        () -> st.armed ? ChargeGuiModel.fuseFraction(st.fuse, st.fuseSet)
                            : ChargeGuiModel.rangeFraction(st.fuse, ChargeType.FUSE_MIN, ChargeType.FUSE_MAX),
                        () -> st.armed ? ALARM : AMBER)));

        // Bore.
        y += 13;
        panel.child(label("gregguns.charge.gui.bore", PAD, y));
        y += 11;
        int boreTop = y;
        panel.child(stepButton(st, type, ChargeParams.BTN_LENGTH_DOWN, "-", PAD, y));
        panel.child(
            readout(
                () -> StatCollector.translateToLocalFormatted("gregguns.charge.gui.depth", st.length),
                () -> AMBER,
                PAD + BTN + 2,
                y,
                56));
        panel.child(stepButton(st, type, ChargeParams.BTN_LENGTH_UP, "+", PAD + BTN + 2 + 56 + 2, y));
        y += BTN + 4;
        if (type.radiusAdjustable()) {
            panel.child(stepButton(st, type, ChargeParams.BTN_RADIUS_DOWN, "-", PAD, y));
        }
        panel.child(
            readout(
                () -> StatCollector.translateToLocalFormatted("gregguns.charge.gui.width", st.radius, st.radius),
                () -> type.radiusAdjustable() ? AMBER : 0xFFB0B0B0,
                PAD + BTN + 2,
                y,
                56));
        if (type.radiusAdjustable()) {
            panel.child(stepButton(st, type, ChargeParams.BTN_RADIUS_UP, "+", PAD + BTN + 2 + 56 + 2, y));
        }
        y += BTN + 3;
        final String dir = ChargeGuiModel.directionName(tile.orientation());
        panel.child(
            text(
                () -> StatCollector.translateToLocalFormatted(
                    "gregguns.charge.gui.direction",
                    StatCollector.translateToLocal("gregguns.charge.gui.dir." + dir)),
                PAD,
                y,
                96,
                10,
                Alignment.CenterLeft,
                () -> TEXT_DARK));
        if (!type.radiusAdjustable()) {
            panel.child(
                text(
                    () -> StatCollector.translateToLocal("gregguns.charge.gui.width_fixed"),
                    PAD,
                    y + 10,
                    96,
                    10,
                    Alignment.CenterLeft,
                    () -> 0xFF606060));
        }
        // Side diagram of the tunnel, to the right of the steppers.
        panel.child(
            new Widget<>().pos(PAD + 100, boreTop)
                .size(CONTENT - 100, 52)
                .background(diagram(st)));

        // Owner and access.
        y = boreTop + 62;
        final String owner = tile.ownerName()
            .isEmpty() ? StatCollector.translateToLocal("gregguns.charge.gui.owner_none") : tile.ownerName();
        panel.child(
            text(
                () -> StatCollector.translateToLocalFormatted("gregguns.charge.gui.owner", owner),
                PAD,
                y,
                CONTENT,
                10,
                Alignment.CenterLeft,
                () -> TEXT_DARK));
        y += 11;
        panel.child(text(() -> {
            switch (ChargeGuiModel.access(st.canEdit, st.armed)) {
                case EDITABLE:
                    return StatCollector.translateToLocal("gregguns.charge.gui.access_edit");
                case FROZEN_ARMED:
                    return StatCollector.translateToLocal("gregguns.charge.gui.access_armed");
                default:
                    return StatCollector.translateToLocal("gregguns.charge.gui.access_view");
            }
        }, PAD, y, CONTENT, 10, Alignment.CenterLeft, () -> st.canEdit ? 0xFF1B5E20 : 0xFF9B1C1C));

        // Arm and Disarm.
        y = HEIGHT - PAD - 18;
        int half = (CONTENT - 4) / 2;
        panel.child(actionButton(st, type, ChargeParams.BTN_ARM, "gregguns.charge.gui.arm", 0xFF9B1C1C, PAD, y, half));
        panel.child(
            actionButton(
                st,
                type,
                ChargeParams.BTN_DISARM,
                "gregguns.charge.gui.disarm",
                0xFF1B5E20,
                PAD + half + 4,
                y,
                half));
        return panel;
    }

    private static void registerValues(TileExplosiveCharge tile, PanelSyncManager sync, ChargeGuiModel.State st,
        EntityPlayer player) {
        // The getters run on the server; the setters run on the client and fill the shared state. Live fuse included.
        sync.syncValue(
            "armed",
            0,
            new IntSyncValue(
                () -> tile.params()
                    .armed() ? 1 : 0,
                v -> st.armed = v != 0));
        sync.syncValue(
            "fuse",
            0,
            new IntSyncValue(
                () -> tile.params()
                    .fuse(),
                v -> st.fuse = v));
        sync.syncValue(
            "fuse_set",
            0,
            new IntSyncValue(
                () -> tile.params()
                    .fuseSet(),
                v -> st.fuseSet = v));
        sync.syncValue(
            "length",
            0,
            new IntSyncValue(
                () -> tile.params()
                    .length(),
                v -> st.length = v));
        sync.syncValue(
            "radius",
            0,
            new IntSyncValue(
                () -> tile.params()
                    .radius(),
                v -> st.radius = v));
        sync.syncValue(
            "can_edit",
            0,
            new IntSyncValue(() -> tile.mayConfigure(player) ? 1 : 0, v -> st.canEdit = v != 0));
    }

    private static void registerButtons(TileExplosiveCharge tile, PanelSyncManager sync, EntityPlayer player) {
        for (int b = 0; b < ChargeParams.BUTTON_COUNT; b++) {
            final int button = b;
            sync.syncValue(
                "press",
                button,
                new InteractionSyncHandler().setOnMousePressed(mouse -> tile.pressBy(player, button)));
        }
    }

    private static IWidget label(String key, int x, int y) {
        return text(() -> StatCollector.translateToLocal(key), x, y, 100, 10, Alignment.CenterLeft, () -> TEXT_DARK);
    }

    private static TextWidget<?> text(Supplier<String> value, int x, int y, int w, int h, Alignment align,
        IntSupplier color) {
        return new TextWidget<>(IKey.dynamic(value)).pos(x, y)
            .size(w, h)
            .alignment(align)
            .color(color)
            .shadow(false);
    }

    /** A dark display with a colored value, like the readout of a GT machine. */
    private static IWidget readout(Supplier<String> value, IntSupplier color, int x, int y, int w) {
        return text(value, x, y, w, BTN, Alignment.Center, color).background(fill(() -> SCREEN_BG, false));
    }

    private static IWidget stepButton(ChargeGuiModel.State st, ChargeType type, int button, String glyph, int x,
        int y) {
        return new ButtonWidget<>().syncHandler("press", button)
            .size(BTN, BTN)
            .pos(x, y)
            .overlay(
                IKey.dynamic(() -> glyph)
                    .color(() -> ChargeGuiModel.enabled(st, type, button) ? GLYPH_ON : GLYPH_OFF))
            .tooltipDynamic(
                tip -> tip.addLine(
                    IKey.lang(
                        ChargeGuiModel.enabled(st, type, button) ? tooltipKey(button) : "gregguns.charge.gui.locked")));
    }

    private static String tooltipKey(int button) {
        switch (button) {
            case ChargeParams.BTN_FUSE_UP:
                return "gregguns.charge.gui.tip.fuse_up";
            case ChargeParams.BTN_FUSE_DOWN:
                return "gregguns.charge.gui.tip.fuse_down";
            case ChargeParams.BTN_LENGTH_UP:
                return "gregguns.charge.gui.tip.depth_up";
            case ChargeParams.BTN_LENGTH_DOWN:
                return "gregguns.charge.gui.tip.depth_down";
            case ChargeParams.BTN_RADIUS_UP:
                return "gregguns.charge.gui.tip.width_up";
            default:
                return "gregguns.charge.gui.tip.width_down";
        }
    }

    private static IWidget actionButton(ChargeGuiModel.State st, ChargeType type, int button, String key, int liveColor,
        int x, int y, int w) {
        return new ButtonWidget<>().syncHandler("press", button)
            .size(w, 18)
            .pos(x, y)
            .overlay(
                IKey.dynamic(() -> StatCollector.translateToLocal(key))
                    .color(() -> ChargeGuiModel.enabled(st, type, button) ? liveColor : GLYPH_OFF))
            .tooltipDynamic(
                tip -> tip
                    .addLine(
                        IKey.lang(
                            ChargeGuiModel.enabled(st, type, button)
                                ? (button == ChargeParams.BTN_ARM ? "gregguns.charge.gui.tip.arm"
                                    : "gregguns.charge.gui.tip.disarm")
                                : "gregguns.charge.gui.locked")));
    }

    /** A solid rectangle whose color is read each frame; unless plain, with a thin black edge. */
    private static IDrawable fill(IntSupplier color, boolean plain) {
        return (ctx, x, y, w, h, theme) -> {
            if (plain) {
                GuiDraw.drawRect(x, y, w, h, color.getAsInt());
            } else {
                GuiDraw.drawRect(x, y, w, h, SCREEN_EDGE);
                GuiDraw.drawRect(x + 1, y + 1, w - 2, h - 2, color.getAsInt());
            }
        };
    }

    private static IDrawable bar(Supplier<Float> fraction, IntSupplier color) {
        return (ctx, x, y, w, h, theme) -> {
            GuiDraw.drawRect(x, y, w, h, SCREEN_EDGE);
            GuiDraw.drawRect(x + 1, y + 1, w - 2, h - 2, SCREEN_BG);
            float f = fraction.get();
            GuiDraw.drawRect(x + 1, y + 1, (w - 2) * f, h - 2, color.getAsInt());
        };
    }

    /**
     * Side view of the tunnel: air above, rock below, the charge on the surface and the bore running straight into the
     * rock. The bore is as wide as the width and as long as the depth, one tick mark per block, so changing a stepper
     * visibly changes the picture. The world direction is the text beside it.
     */
    private static IDrawable diagram(ChargeGuiModel.State st) {
        return (ctx, x, y, w, h, theme) -> {
            int cell = 4;
            int surface = 8;
            GuiDraw.drawRect(x, y, w, surface, 0xFFA9C4DA);
            GuiDraw.drawRect(x, y + surface, w, h - surface, 0xFF77706A);
            int bw = Math.min(w - 2, st.radius * 2 + 2);
            int bx = x + (w - bw) / 2;
            int bh = Math.min(h - surface - 1, st.length * cell);
            GuiDraw.drawRect(bx, y + surface, bw, bh, 0xFF141414);
            for (int i = 1; i < st.length && i * cell < bh; i++) {
                GuiDraw.drawRect(bx, y + surface + i * cell, bw, 1, 0xFF4A4A4A);
            }
            GuiDraw.drawRect(x + w / 2 - 5, y + surface - 4, 10, 4, st.armed ? 0xFFFF3030 : 0xFFD08A00);
        };
    }
}
