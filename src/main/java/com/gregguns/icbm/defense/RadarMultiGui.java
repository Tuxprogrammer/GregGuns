package com.gregguns.icbm.defense;

import java.util.regex.Pattern;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gregguns.icbm.IcbmSync;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * A multiblock radar's controller screen (the small radar and the tower): GT's multiblock screen plus one button that
 * opens a small radar panel with the state line (offline, scanning, tracking or threat), the protected radius field,
 * the range and the contact counts. The numbers reach the
 * client as read-only sync values; the radius field re-checks on the server ({@link RadarPanel#setRadiusText}). Built
 * on both sides.
 *
 * @param <T> the controller class
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (Tower, GUI)
final class RadarMultiGui<T extends MTEMultiBlockBase & RadarPanel> extends MTEMultiBlockBaseGui<T> {

    private static final Pattern DIGITS = Pattern.compile("[0-9]{0,5}");
    private static final int PANEL_W = 150;
    private static final int PANEL_H = 82;

    private static final class Shown {

        int range;
        int detected;
        int incoming;
        int state;
    }

    RadarMultiGui(T radar) {
        super(radar);
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        IPanelHandler radarPanel = syncManager
            .syncedPanel("radarPanel", true, (psm, handler) -> buildRadarPanel(psm, parent));
        return super.createLeftPanelGapRow(parent, syncManager).child(
            new ButtonWidget<>().size(18, 18)
                .overlay(IKey.str("R"))
                .onMousePressed(d -> {
                    if (!radarPanel.isPanelOpen()) {
                        radarPanel.openPanel();
                    } else {
                        radarPanel.closePanel();
                    }
                    return true;
                })
                .tooltipBuilder(t -> t.addLine(IKey.lang("gregguns.icbm.radar_tower.gui.button"))));
    }

    private ModularPanel buildRadarPanel(PanelSyncManager psm, ModularPanel parent) {
        Shown shown = new Shown();
        psm.syncValue("field_radius", 0, IcbmSync.editableText(multiblock::radiusText, multiblock::setRadiusText));
        psm.syncValue("st_range", 0, new IntSyncValue(multiblock::rangeIfOn, v -> shown.range = v));
        psm.syncValue("st_det", 0, new IntSyncValue(multiblock::detectedCount, v -> shown.detected = v));
        psm.syncValue("st_inc", 0, new IntSyncValue(multiblock::incomingCount, v -> shown.incoming = v));
        psm.syncValue(
            "st_state",
            0,
            new IntSyncValue(
                () -> multiblock.state()
                    .ordinal(),
                v -> shown.state = v));
        return new ModularPanel("radarPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(PANEL_W, PANEL_H)
            .child(
                Flow.column()
                    .full()
                    .padding(4)
                    .child(label(() -> tr(multiblock.panelTitleKey())))
                    .child(label(() -> tr(RadarRules.State.values()[shown.state].langKey())))
                    .child(label(() -> trf("gregguns.icbm.radar.gui.range", shown.range)))
                    .child(label(() -> trf("gregguns.icbm.radar.gui.contacts", shown.detected, shown.incoming)))
                    .child(label(() -> tr("gregguns.icbm.radar.gui.radius")))
                    .child(
                        new TextFieldWidget().syncHandler("field_radius", 0)
                            .setPattern(DIGITS)
                            .setMaxLength(5)
                            .size(50, 14)));
    }

    private static IWidget label(java.util.function.Supplier<String> text) {
        return new TextWidget<>(IKey.dynamic(text)).size(PANEL_W - 8, 10)
            .alignment(Alignment.CenterLeft)
            .marginBottom(2);
    }

    private static String tr(String key) {
        return StatCollector.translateToLocal(key);
    }

    private static String trf(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(key, args);
    }
}
