package com.gregguns.icbm.warhead;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.Ing;

/**
 * The Assembler recipe of each inert warhead, the first step of the warhead chain (D282): the casing plates and the
 * casing's Fuze Assembly, at the casing tier's voltage. The inert warhead holds no explosive; a payload and Plastic
 * Explosive charges come after ({@link WarheadRecipeSpec}). Plain data, no Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The warhead chain)
public enum InertWarheadSpec {

    /** HV: four stainless steel plates and a Fuze Mk I. */
    T1(WarheadCasing.T1, 3, Ing.gt("plate", "StainlessSteel", WarheadRecipes.T1_PLATES, Tier.HV, 1.0),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK1, 1)),
    /** EV: eight titanium plates and a Fuze Mk II. */
    T2(WarheadCasing.T2, 4, Ing.gt("plate", "Titanium", WarheadRecipes.T2_PLATES, Tier.EV, 1.0),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK2, 1)),
    /** LuV: eight tungstensteel plates and a Fuze Mk III. */
    T3(WarheadCasing.T3, 6, Ing.gt("plate", "TungstenSteel", WarheadRecipes.T3_PLATES, Tier.IV, 1.0),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK3, 1)),
    /** UV: eight naquadria plates and a Fuze Mk III ({@link Tier} stops at ZPM, which stands in for UV). */
    T4(WarheadCasing.T4, 8, Ing.gt("plate", "Naquadria", WarheadRecipes.T4_PLATES, Tier.ZPM, 2.0),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK3, 1)),
    /** UHV: eight neutronium plates and a Fuze Mk III. */
    T5(WarheadCasing.T5, 9, Ing.gt("plate", "Neutronium", WarheadRecipes.T5_PLATES, Tier.ZPM, 3.0),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK3, 1));

    /**
     * The programmed circuit of every inert warhead recipe. The casing plates and the fuze differ per tier, and no
     * payload or fill recipe takes plates or a fuze, so one number serves all five.
     */
    public static final int CIRCUIT = 2;

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

    /** @return the casing plates, then the fuze */
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
