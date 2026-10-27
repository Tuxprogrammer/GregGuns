package com.gregguns.icbm.tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.icbm.recipe.Ing;

/**
 * The one Assembler recipe of every handheld tool, as plain data so the HV floor (D69) and the slot limit can be unit
 * tested. Each runs at its tool's tier with its own programmed circuit. No Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/handheld-tools.md (Recipes)
public enum ToolRecipeSpec {

    /** HV remote detonator: an emitter and a data circuit in a stainless shell. */
    REMOTE_HV(Tier.HV, 1, Ing.gt("circuit", "Advanced", 1, Tier.HV, 3.0),
        Ing.gt("plate", "StainlessSteel", 2, Tier.HV, 1.0), Ing.gtItem("Emitter_HV", 1, Tier.HV, 6.0),
        Ing.gt("wireFine", "Electrum", 4, Tier.HV, 0.5)),
    /** EV remote detonator. */
    REMOTE_EV(Tier.EV, 2, Ing.gt("circuit", "Data", 1, Tier.EV, 5.0), Ing.gt("plate", "Titanium", 2, Tier.EV, 1.0),
        Ing.gtItem("Emitter_EV", 1, Tier.EV, 6.0), Ing.gt("wireFine", "Platinum", 4, Tier.EV, 0.5)),
    /** IV remote detonator. */
    REMOTE_IV(Tier.IV, 3, Ing.gt("circuit", "Elite", 1, Tier.IV, 8.0),
        Ing.gt("plate", "TungstenSteel", 2, Tier.IV, 1.0), Ing.gtItem("Emitter_IV", 1, Tier.IV, 6.0),
        Ing.gt("wireFine", "Platinum", 4, Tier.EV, 0.5)),
    /** The defuser: a sensor, shears' worth of steel and an insulated grip. */
    DEFUSER(Tier.HV, 4, Ing.gt("circuit", "Advanced", 1, Tier.HV, 3.0),
        Ing.gt("plate", "StainlessSteel", 2, Tier.HV, 1.0), Ing.gtItem("Sensor_HV", 1, Tier.HV, 6.0),
        Ing.gt("plate", "Rubber", 2, Tier.LV, 1.0));

    private final Tier machineTier;
    private final int circuit;
    private final List<Ing> inputs;

    ToolRecipeSpec(Tier machineTier, int circuit, Ing... inputs) {
        this.machineTier = machineTier;
        this.circuit = circuit;
        this.inputs = Collections.unmodifiableList(Arrays.asList(inputs));
    }

    /** @return the Assembler's tier, the voltage the recipe runs at */
    public Tier machineTier() {
        return machineTier;
    }

    /** @return the programmed circuit number (also a slot of the Assembler's nine) */
    public int circuit() {
        return circuit;
    }

    /** @return the inputs, in order */
    public List<Ing> inputs() {
        return inputs;
    }

    /** @return the detonator tier this recipe makes, or null for the defuser */
    public Tier detonatorTier() {
        return this == DEFUSER ? null : machineTier;
    }
}
