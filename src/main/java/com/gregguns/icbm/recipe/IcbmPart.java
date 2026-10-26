package com.gregguns.icbm.recipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;

/**
 * The ICBM hardware parts the warhead recipes share, as plain data: registry name, tier, what the part is for,
 * and its own Assembler recipe. Each is one GregGuns item ({@code IcbmParts}); {@code PartRecipes} registers the
 * recipe. Every part runs at its own tier's voltage with a unique programmed circuit. Both sides, no Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Parts)
public enum IcbmPart {

    /** The one explosive part, used by every T1 and T2 warhead in different counts. */
    PLASTIC_EXPLOSIVE("plastic_explosive", Tier.HV, 1.1, 4, 11,
        Arrays.asList(Ing.gt("plate", "Polytetrafluoroethylene", 2, Tier.MV, 1.0), Ing.industrialTnt(2))),
    /** Trigger set of every T1 warhead. */
    FUZE_ASSEMBLY_MK1("fuze_assembly_mk1", Tier.HV, 5.0, 1, 12,
        Arrays.asList(
            Ing.gt("circuit", "Advanced", 1, Tier.HV, 3.0),
            Ing.gt("plate", "Steel", 1, Tier.LV, 1.0),
            Ing.gt("wireFine", "Copper", 4, Tier.LV, 0.25))),
    /** Trigger set of every T2 warhead. */
    FUZE_ASSEMBLY_MK2("fuze_assembly_mk2", Tier.EV, 8.0, 1, 13,
        Arrays.asList(
            Ing.gt("circuit", "Data", 1, Tier.EV, 5.0),
            Ing.gt("plate", "Titanium", 1, Tier.EV, 1.0),
            Ing.gt("wireFine", "Electrum", 4, Tier.HV, 0.5))),
    /** The copper cone of a breaching warhead. */
    SHAPED_CHARGE_LINER_CONE("shaped_charge_liner_cone", Tier.HV, 1.0, 2, 14,
        Arrays.asList(Ing.gt("plate", "Copper", 2, Tier.LV, 1.0))),
    /** Steel fragments around a T1 warhead. */
    FRAGMENTATION_SLEEVE_STEEL("fragmentation_sleeve_steel", Tier.HV, 4.0, 1, 15,
        Arrays.asList(Ing.gt("round", "Steel", 16, Tier.LV, 0.15), Ing.gt("plate", "Steel", 1, Tier.LV, 1.0))),
    /** Titanium fragments around a T2 warhead. */
    FRAGMENTATION_SLEEVE_TITANIUM("fragmentation_sleeve_titanium", Tier.EV, 7.0, 1, 16,
        Arrays.asList(Ing.gt("round", "Titanium", 16, Tier.EV, 0.375), Ing.gt("plate", "Titanium", 1, Tier.EV, 1.0))),
    /** Holds the fluid fill of the gas, smoke and fuel-air warheads. */
    DISPERSAL_CANISTER("dispersal_canister", Tier.HV, 2.5, 2, 17,
        Arrays.asList(
            Ing.gt("plate", "Aluminium", 2, Tier.MV, 1.0),
            Ing.gt("plate", "Rubber", 1, Tier.LV, 1.0),
            Ing.gtItem("Cell_Empty", 2, Tier.LV, 0.25))),
    /** A Dispersal Canister filled with gasoline; the name carries the theme. */
    NAPALM_CANISTER("napalm_canister", Tier.HV, 3.5, 1, 18,
        Arrays.asList(Ing.part(DISPERSAL_CANISTER, 1), Ing.fluid("Gasoline", 1000, Tier.HV))),
    /** Guidance computer, HV: the base for a T1 body (I2). */
    GUIDANCE_COMPUTER_MK1("guidance_computer_mk1", Tier.HV, 5.0, 1, 21,
        Arrays.asList(
            Ing.gt("circuit", "Advanced", 1, Tier.HV, 3.0),
            Ing.gt("plate", "Steel", 1, Tier.LV, 1.0),
            Ing.gt("wireFine", "Copper", 4, Tier.LV, 0.25))),
    /** Guidance computer, EV: the base for a T2 body, one step of guidance on a T1 body (I2). */
    GUIDANCE_COMPUTER_MK2("guidance_computer_mk2", Tier.EV, 8.0, 1, 22,
        Arrays.asList(
            Ing.gt("circuit", "Data", 1, Tier.EV, 5.0),
            Ing.gt("plate", "Titanium", 1, Tier.EV, 1.0),
            Ing.gt("wireFine", "Electrum", 4, Tier.HV, 0.5))),
    /** Guidance computer, IV: the best a T1 or T2 body takes (I2). */
    GUIDANCE_COMPUTER_MK3("guidance_computer_mk3", Tier.IV, 11.0, 1, 23,
        Arrays.asList(
            Ing.gt("circuit", "Elite", 1, Tier.IV, 8.0),
            Ing.gt("plate", "TungstenSteel", 1, Tier.IV, 1.0),
            Ing.gt("wireFine", "Platinum", 4, Tier.EV, 0.5)));

    private final String registryName;
    private final Tier tier;
    private final double unitCost;
    private final int outputCount;
    private final int circuit;
    private final List<Ing> inputs;

    IcbmPart(String registryName, Tier tier, double unitCost, int outputCount, int circuit, List<Ing> inputs) {
        this.registryName = registryName;
        this.tier = tier;
        this.unitCost = unitCost;
        this.outputCount = outputCount;
        this.circuit = circuit;
        this.inputs = Collections.unmodifiableList(inputs);
    }

    /** @return the registry name without the mod id; also the texture and lang key name */
    public String registryName() {
        return registryName;
    }

    /** @return the part's tier, which is also the voltage of its Assembler recipe */
    public Tier tier() {
        return tier;
    }

    /** @return the D34 cost of one part, plate equivalents */
    public double unitCost() {
        return unitCost;
    }

    /** @return how many parts one recipe makes */
    public int outputCount() {
        return outputCount;
    }

    /** @return the programmed circuit that keeps this recipe unique */
    public int circuit() {
        return circuit;
    }

    /** @return the ingredients, fluid last */
    public List<Ing> inputs() {
        return inputs;
    }

    /**
     * @return the icon's name under {@code textures/items}: the licensed NTM item textures sit in
     *         {@code thirdparty/ntm}
     *         (D47), and only the liner cone's icon is our own
     */
    public String iconName() {
        return this == SHAPED_CHARGE_LINER_CONE ? registryName : "thirdparty/ntm/" + registryName;
    }

    /** @return the lang key suffix of the "used in" line of the tooltip */
    public String usedInKey() {
        return "gregguns.icbm.part.used." + registryName;
    }

    /**
     * @param registryName a registry name
     * @return the part, or null
     */
    public static IcbmPart byRegistryName(String registryName) {
        for (IcbmPart p : values()) {
            if (p.registryName.equals(registryName)) {
                return p;
            }
        }
        return null;
    }
}
