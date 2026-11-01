package com.gregguns.icbm.warhead;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.icbm.compat.GalacticraftParts;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.Ing;

/**
 * The Assembler recipe of each inert warhead, the first step of the warhead chain (D282): the casing plates, the
 * casing's Fuze Assembly, the nose cone and hull plates of the rocket of the casing's tier, and its arming electronics
 * (a sensor, an emitter and two circuits of the tier), at the casing tier's voltage (D328: the warhead is the missile's
 * nose and the expensive part of it). The inert warhead holds no explosive; a payload and Plastic
 * Explosive charges come after ({@link WarheadRecipeSpec}). Plain data, no Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The warhead chain)
public enum InertWarheadSpec {

    /** HV: four stainless steel plates, a Fuze Mk I, the Tier 1 rocket's nose cone and hull, HV arming electronics. */
    T1(WarheadCasing.T1, 3, Ing.gt("plate", "StainlessSteel", WarheadRecipes.T1_PLATES, Tier.HV, 1.0),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK1, 1), Ing.modItem(GalacticraftParts.NOSE_T1, 1, Tier.HV, 69.0),
        Ing.modItem(GalacticraftParts.HULL_T1, WarheadRecipes.HULL_PLATES, Tier.HV, 14.0),
        Ing.gtItem("Sensor_HV", 1, Tier.HV, 6.0), Ing.gtItem("Emitter_HV", 1, Tier.HV, 6.0),
        Ing.gt("circuit", "Advanced", 2, Tier.HV, 3.0)),
    /**
     * EV: eight titanium plates, a Fuze Mk II, the Tier 2 rocket's nose cone and hull, EV electronics (D343: every
     * part buildable at EV).
     */
    T2(WarheadCasing.T2, 4, Ing.gt("plate", "Titanium", WarheadRecipes.T2_PLATES, Tier.EV, 1.0),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK2, 1), Ing.modItem(GalacticraftParts.NOSE_T2, 1, Tier.HV, 69.0),
        Ing.modItem(GalacticraftParts.HULL_T2, WarheadRecipes.HULL_PLATES, Tier.EV, 26.0),
        Ing.gtItem("Sensor_EV", 1, Tier.EV, 6.0), Ing.gtItem("Emitter_EV", 1, Tier.EV, 6.0),
        Ing.gt("circuit", "Data", 2, Tier.EV, 5.0)),
    /**
     * LuV: eight tungstensteel plates, a Fuze Mk III, the heavy nose cone, the Tier 4 rocket's hull, LuV electronics.
     */
    T3(WarheadCasing.T3, 6, Ing.gt("plate", "TungstenSteel", WarheadRecipes.T3_PLATES, Tier.IV, 1.0),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK3, 1), Ing.modItem(GalacticraftParts.NOSE_HEAVY, 1, Tier.IV, 123.0),
        Ing.modItem(GalacticraftParts.HULL_T3, WarheadRecipes.HULL_PLATES, Tier.LUV, 18.0),
        Ing.gtItem("Sensor_LuV", 1, Tier.LUV, 10.0), Ing.gtItem("Emitter_LuV", 1, Tier.LUV, 10.0),
        Ing.gt("circuit", "Master", 2, Tier.LUV, 8.0)),
    /**
     * UV: eight naquadria plates, a Fuze Mk III, the Tier 6 rocket's nose cone and hull, UV electronics ({@link Tier}
     * stops at ZPM, which stands in for UV).
     */
    T4(WarheadCasing.T4, 8, Ing.gt("plate", "Naquadria", WarheadRecipes.T4_PLATES, Tier.ZPM, 2.0),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK3, 1), Ing.modItem(GalacticraftParts.NOSE_T4, 1, Tier.ZPM, 333.0),
        Ing.modItem(GalacticraftParts.HULL_T4, WarheadRecipes.HULL_PLATES, Tier.ZPM, 27.0),
        Ing.gtItem("Sensor_UV", 1, Tier.ZPM, 20.0), Ing.gtItem("Emitter_UV", 1, Tier.ZPM, 20.0),
        Ing.gt("circuit", "Superconductor", 2, Tier.ZPM, 12.0)),
    /** UHV: eight neutronium plates, a Fuze Mk III, the Tier 7 rocket's nose cone and hull, UHV electronics. */
    T5(WarheadCasing.T5, 9, Ing.gt("plate", "Neutronium", WarheadRecipes.T5_PLATES, Tier.ZPM, 3.0),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK3, 1), Ing.modItem(GalacticraftParts.NOSE_T5, 1, Tier.ZPM, 459.0),
        Ing.modItem(GalacticraftParts.HULL_T5, WarheadRecipes.HULL_PLATES, Tier.ZPM, 32.0),
        Ing.gtItem("Sensor_UHV", 1, Tier.ZPM, 24.0), Ing.gtItem("Emitter_UHV", 1, Tier.ZPM, 24.0),
        Ing.gt("circuit", "Infinite", 2, Tier.ZPM, 16.0));

    /**
     * The programmed circuit of every inert warhead recipe. The casing plates and the fuze differ per tier, and no
     * payload or fill recipe takes plates or a fuze, so one number serves all five.
     */
    public static final int CIRCUIT = 2;

    /**
     * How many of the inputs the Cutting Machine's scrap step gives back (D283, amended by D328): the casing plates,
     * the fuze, the nose cone and the hull plates. The arming electronics (sensor, emitter, circuits) are spent.
     */
    public static final int RETURNED_INPUTS = 4;

    private final WarheadCasing casing;
    private final int voltageIndex;
    private final List<Ing> inputs;

    InertWarheadSpec(WarheadCasing casing, int voltageIndex, Ing... inputs) {
        this.casing = casing;
        this.voltageIndex = voltageIndex;
        this.inputs = Collections.unmodifiableList(Arrays.asList(inputs));
    }

    /** @return the casing this recipe makes */
    public WarheadCasing casing() {
        return casing;
    }

    /** @return GT's voltage index of the Assembler: 3 (HV), 4 (EV), 6 (LuV), 8 (UV) or 9 (UHV) */
    public int voltageIndex() {
        return voltageIndex;
    }

    /** @return the casing plates, the fuze, the nose cone, the hull plates, then the electronics */
    public List<Ing> inputs() {
        return inputs;
    }

    /** @return the D34 cost of the inert warhead, plate equivalents */
    public double cost() {
        double sum = 0.0;
        for (Ing in : inputs) {
            sum += in.cost();
        }
        return sum;
    }

    /**
     * @param casing a casing
     * @return its inert warhead recipe
     */
    public static InertWarheadSpec of(WarheadCasing casing) {
        return values()[casing.ordinal()];
    }
}
