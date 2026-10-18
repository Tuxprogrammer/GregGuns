package com.gregguns.machine.reaction;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

/**
 * The intensity control both Reaction Chamber GUIs share: a minus button, a readout of the setting, the preferred
 * intensity and the completion count, and a plus button. Input goes only through an {@code allowC2S} sync value whose
 * setter clamps to 0 to 10 (GGS-08-28). Client and server (ModularUI2 builds the same panel on both sides).
 */
// Spec: docs/gregguns-spec/08-machines-production/modularui2-guis.md (GGS-08-27 to GGS-08-29)
public final class ReactionGuiWidgets {

    private static final String INTENSITY_KEY = "rcIntensity";
    private static final String PREFERRED_KEY = "rcPreferred";
    private static final String COMPLETION_KEY = "rcCompletion";
    private static final String REQUIRED_KEY = "rcRequired";

    private ReactionGuiWidgets() {}

    /**
     * Registers the sync values. Call from the GUI's {@code registerSyncValues}.
     *
     * @param sync    the panel's sync manager
     * @param machine the machine
     */
    public static void register(PanelSyncManager sync, ReactionControlled machine) {
        sync.syncValue(INTENSITY_KEY, new IntSyncValue(machine::getIntensity, machine::setIntensity).allowC2S());
        sync.syncValue(
            PREFERRED_KEY,
            new IntSyncValue(
                () -> machine.getReactionState()
                    .preferredIntensity()));
        sync.syncValue(
            COMPLETION_KEY,
            new IntSyncValue(
                () -> machine.getReactionState()
                    .completion()));
        sync.syncValue(
            REQUIRED_KEY,
            new IntSyncValue(
                () -> machine.getReactionState()
                    .required()));
    }

    /**
     * Builds the control row.
     *
     * @param sync the sync manager {@link #register} was called with
     * @return the row
     */
    public static Flow row(PanelSyncManager sync) {
        IntSyncValue intensity = sync.findSyncHandler(INTENSITY_KEY, IntSyncValue.class);
        IntSyncValue preferred = sync.findSyncHandler(PREFERRED_KEY, IntSyncValue.class);
        IntSyncValue completion = sync.findSyncHandler(COMPLETION_KEY, IntSyncValue.class);
        IntSyncValue required = sync.findSyncHandler(REQUIRED_KEY, IntSyncValue.class);
        return Flow.row()
            .coverChildren()
            .childPadding(2)
            .child(
                new ButtonWidget<>().size(12, 12)
                    .overlay(IKey.str("-"))
                    .onMousePressed(button -> {
                        intensity.setIntValue(ReactionState.clampIntensity(intensity.getIntValue() - 1));
                        return true;
                    }))
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        "gregguns.reaction.gui.status",
                        intensity.getIntValue(),
                        preferred.getIntValue(),
                        completion.getIntValue(),
                        required.getIntValue()))
                    .asWidget())
            .child(
                new ButtonWidget<>().size(12, 12)
                    .overlay(IKey.str("+"))
                    .onMousePressed(button -> {
                        intensity.setIntValue(ReactionState.clampIntensity(intensity.getIntValue() + 1));
                        return true;
                    }));
    }
}
