package com.gregguns.icbm.defense;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.icbm.GuiVanishProbe;
import com.gregguns.machine.GregGunsMTEIDs;

/**
 * Scenarios that read the Air-Defense Battery's and the radars' screen sync values after their block is removed
 * (see {@link GuiVanishProbe}). Dev environment only.
 */
public final class DefenseGuiProbes {

    private DefenseGuiProbes() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(
            GuiVanishProbe.scenario(
                "icbm.defense.battery_gui_vanish",
                "Reading every sync value of the Air-Defense Battery screen after its block is removed does not throw.",
                GregGunsMTEIDs.AIR_DEFENSE,
                (m, sync) -> new BatteryGui((MTEAirDefense) m).registerSyncValues(sync)));
        registry.register(
            GuiVanishProbe.scenario(
                "icbm.defense.radar_hv_gui_vanish",
                "Reading every sync value of the HV Radar settings panel after its block is removed does not throw.",
                GregGunsMTEIDs.RADAR_HV,
                (m, sync) -> new RadarMultiGui<>((MTERadar) m).buildRadarPanel(sync, new ModularPanel("probe"))));
        registry.register(
            GuiVanishProbe.scenario(
                "icbm.defense.radar_ev_gui_vanish",
                "Reading every sync value of the EV Radar settings panel after its block is removed does not throw.",
                GregGunsMTEIDs.RADAR_EV,
                (m, sync) -> new RadarMultiGui<>((MTERadar) m).buildRadarPanel(sync, new ModularPanel("probe"))));
        registry.register(
            GuiVanishProbe.scenario(
                "icbm.defense.radar_tower_gui_vanish",
                "Reading every sync value of the Radar Tower settings panel after its block is removed does not throw.",
                GregGunsMTEIDs.RADAR_TOWER,
                (m, sync) -> new RadarMultiGui<>((MTERadarTower) m).buildRadarPanel(sync, new ModularPanel("probe"))));
    }
}
