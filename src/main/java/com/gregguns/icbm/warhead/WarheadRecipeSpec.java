package com.gregguns.icbm.warhead;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.icbm.explosive.ExplosiveTier;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.Ing;

/**
 * The one Assembler recipe of every T1 and T2 warhead, as plain data so the tier gate, the slot limits and the D34
 * cost can be unit tested (D57). Every recipe runs at its casing tier's voltage (HV for T1, EV for T2) with its own
 * programmed circuit, makes a full-casing warhead (the NEI example for that explosive, D23) and uses inputs at or below
 * that tier; the T1 condensed starter (fill 1) is a second, cheaper recipe of the same blast. No Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Recipes)
public enum WarheadRecipeSpec {

    /** Plain high explosive: TNT, dynamite and plastic explosive. */
    CONDENSED(WarheadBlast.CONDENSED, false, 1, plates(Tier.HV, 4), Ing.tnt(8), Ing.dynamite(2), pe(3), fuze1()),
    /** One charge in a small casing: the cheap way in. */
    CONDENSED_STARTER(WarheadBlast.CONDENSED, true, 2, plates(Tier.HV, 2), Ing.tnt(2), fuze1()),
    /** Steel fragments around a charge. */
    SHRAPNEL(WarheadBlast.SHRAPNEL, false, 3, plates(Tier.HV, 4), Ing.tnt(6), pe(2),
        Ing.part(IcbmPart.FRAGMENTATION_SLEEVE_STEEL, 1), fuze1()),
    /** A dense lead mass around a charge. */
    ANVIL(WarheadBlast.ANVIL, false, 4, plates(Tier.HV, 4), Ing.tnt(6), pe(2),
        Ing.gt("plate", "Lead", 12, Tier.LV, 0.5), fuze1()),
    /** Napalm: two filled canisters and a charge to burst them. */
    INCENDIARY(WarheadBlast.INCENDIARY, false, 5, plates(Tier.HV, 4), Ing.part(IcbmPart.NAPALM_CANISTER, 2), pe(3),
        fuze1()),
    /** A dizzying gas: nitrous oxide in two canisters. */
    DEBILITATION(WarheadBlast.DEBILITATION, false, 6, plates(Tier.HV, 4), Ing.part(IcbmPart.DISPERSAL_CANISTER, 2),
        pe(3), fuze1(), Ing.gas("NitrousOxide", 2000, Tier.LV)),
    /** Pungent ammonia gas in two canisters; a few mustard seeds for a pun when HarvestCraft is there. */
    CHEMICAL(WarheadBlast.CHEMICAL, false, 7, plates(Tier.HV, 4), Ing.part(IcbmPart.DISPERSAL_CANISTER, 2), pe(3),
        fuze1(), Ing.optionalMod("harvestcraft:mustardseedsItem", 4), Ing.gas("Ammonia", 2000, Tier.HV)),
    /** A smoky fluid and coal dust. */
    SMOKE(WarheadBlast.SMOKE, false, 8, plates(Tier.HV, 4), Ing.part(IcbmPart.DISPERSAL_CANISTER, 2), pe(1), fuze1(),
        Ing.gt("dust", "Coal", 8, Tier.LV, 0.2), Ing.fluid("Creosote", 2000, Tier.LV)),
    /** A field generator pushes everything out. */
    REPULSIVE(WarheadBlast.REPULSIVE, false, 9, plates(Tier.HV, 4), pe(2),
        Ing.gtItem("Field_Generator_HV", 1, Tier.HV, 8.0), fuze1()),
    /** The same generator with magnets pulls everything in. */
    ATTRACTIVE(WarheadBlast.ATTRACTIVE, false, 10, plates(Tier.HV, 4), pe(2),
        Ing.gtItem("Field_Generator_HV", 1, Tier.HV, 8.0), Ing.gt("stick", "SteelMagnetic", 4, Tier.LV, 0.5), fuze1()),
    /** A titanium sleeve of fragments around Industrial TNT. */
    FRAGMENTATION(WarheadBlast.FRAGMENTATION, false, 19, plates(Tier.EV, 8), Ing.industrialTnt(10), pe(4),
        Ing.part(IcbmPart.FRAGMENTATION_SLEEVE_TITANIUM, 1), fuze2()),
    /** A big fuel-air blast: kerosene in two canisters. */
    THERMOBARIC(WarheadBlast.THERMOBARIC, false, 24, plates(Tier.EV, 8), Ing.industrialTnt(10), pe(4),
        Ing.part(IcbmPart.DISPERSAL_CANISTER, 2), fuze2(), Ing.kerosene(4000)),
    /** Two emitters shatter glass and stun. */
    SONIC(WarheadBlast.SONIC, false, 21, plates(Tier.EV, 8), Ing.industrialTnt(6), pe(2),
        Ing.gtItem("Emitter_EV", 2, Tier.EV, 6.0), fuze2()),
    /** Copper liner cones and a titanium penetrator; tungstensteel waits for T3. */
    BREACHING(WarheadBlast.BREACHING, false, 22, plates(Tier.EV, 8), Ing.industrialTnt(10), pe(3),
        Ing.part(IcbmPart.SHAPED_CHARGE_LINER_CONE, 2), Ing.gt("stick", "Titanium", 6, Tier.EV, 1.0), fuze2()),
    /** The hot T3 blast: Industrial TNT, napalm and kerosene (LuV). */
    EXOTHERMIC(WarheadBlast.EXOTHERMIC, false, 1, plates(Tier.LUV, 8), Ing.industrialTnt(16), pe(6),
        Ing.part(IcbmPart.NAPALM_CANISTER, 4), fuze3(), Ing.kerosene(8000)),
    /** The cold T3 blast: Industrial TNT and helium in dispersal canisters (LuV). */
    ENDOTHERMIC(WarheadBlast.ENDOTHERMIC, false, 2, plates(Tier.LUV, 8), Ing.industrialTnt(16), pe(6),
        Ing.part(IcbmPart.DISPERSAL_CANISTER, 4), fuze3(), Ing.gas("Helium", 8000, Tier.IV)),
    /** A fission core of uranium-235 and plutonium around an implosion charge (ZPM). */
    NUCLEAR(WarheadBlast.NUCLEAR, false, 3, plates(Tier.LUV, 8), Ing.industrialTnt(12), pe(4),
        Ing.gt("ingot", "Uranium235", 8, Tier.HV, 6.0), Ing.gt("ingot", "Plutonium", 4, Tier.EV, 8.0), fuze3()),
    /**
     * The antimatter warhead (UV, D134): naquadria casing, two UV field generators to contain it, a Quantum Eye, a
     * plutonium-241 seed and an implosion charge.
     */
    ANTIMATTER(WarheadBlast.ANTIMATTER, false, 1, naquadria(8), Ing.industrialTnt(8), pe(4),
        Ing.gtItem("Field_Generator_UV", 2, Tier.ZPM, 20.0), Ing.gtItem("QuantumEye", 1, Tier.ZPM, 15.0),
        Ing.gt("ingot", "Plutonium241", 4, Tier.EV, 8.0), fuze3()),
    /**
     * The red matter warhead (UHV, D134): neutronium casing, two UHV field generators, two Quantum Eyes and two UHV
     * emitters around an implosion charge.
     */
    REDMATTER(WarheadBlast.REDMATTER, false, 2, neutronium(8), Ing.industrialTnt(8), pe(4),
        Ing.gtItem("Field_Generator_UHV", 2, Tier.ZPM, 30.0), Ing.gtItem("QuantumEye", 2, Tier.ZPM, 15.0),
        Ing.gtItem("Emitter_UHV", 2, Tier.ZPM, 24.0), fuze3()),
    /** Two field generators and emitters around a flux-compression charge (LuV). */
    EMP(WarheadBlast.EMP, false, 4, plates(Tier.LUV, 8), Ing.industrialTnt(4), pe(2),
        Ing.gtItem("Field_Generator_LuV", 2, Tier.LUV, 12.0), Ing.gtItem("Emitter_LuV", 2, Tier.LUV, 10.0), fuze3()),
    /** A Bio Lab culture in biomass. */
    CONTAGIOUS(WarheadBlast.CONTAGIOUS, false, 23, plates(Tier.EV, 8), Ing.industrialTnt(6), pe(2), Ing.bioDish(4),
        fuze2(), Ing.fluid("Biomass", 2000, Tier.HV));

    // Programmed-circuit numbers are 1 to 24 in GT: the warheads use 1 to 10 and 19 to 24, the parts 11 to 18.
    private final WarheadBlast blast;
    private final boolean starter;
    private final int circuit;
    private final List<Ing> inputs;

    WarheadRecipeSpec(WarheadBlast blast, boolean starter, int circuit, Ing... inputs) {
        this.blast = blast;
        this.starter = starter;
        this.circuit = circuit;
        this.inputs = Collections.unmodifiableList(Arrays.asList(inputs));
    }

    /** The casing plates: stainless steel for T1, titanium for T2. */
    private static Ing plates(Tier tier, int count) {
        if (tier == Tier.LUV) {
            return Ing.gt("plate", "TungstenSteel", count, Tier.IV, 1.0);
        }
        return tier == Tier.HV ? Ing.gt("plate", "StainlessSteel", count, Tier.HV, 1.0)
            : Ing.gt("plate", "Titanium", count, Tier.EV, 1.0);
    }

    /** The T4 casing plates. {@link Tier} stops at ZPM, which stands in for UV when inputs are checked. */
    private static Ing naquadria(int count) {
        return Ing.gt("plate", "Naquadria", count, Tier.ZPM, 2.0);
    }

    /** The T5 casing plates. {@link Tier} stops at ZPM, which stands in for UHV when inputs are checked. */
    private static Ing neutronium(int count) {
        return Ing.gt("plate", "Neutronium", count, Tier.ZPM, 3.0);
    }

    private static Ing pe(int count) {
        return Ing.part(IcbmPart.PLASTIC_EXPLOSIVE, count);
    }

    private static Ing fuze1() {
        return Ing.part(IcbmPart.FUZE_ASSEMBLY_MK1, 1);
    }

    private static Ing fuze3() {
        return Ing.part(IcbmPart.FUZE_ASSEMBLY_MK3, 1);
    }

    private static Ing fuze2() {
        return Ing.part(IcbmPart.FUZE_ASSEMBLY_MK2, 1);
    }

    /** @return the blast this makes a warhead of */
    public WarheadBlast blast() {
        return blast;
    }

    /** @return true for the cheap one-charge condensed recipe, false for a full-casing warhead */
    public boolean isStarter() {
        return starter;
    }

    /** @return the programmed circuit that keeps the recipe unique in the Assembler */
    public int circuit() {
        return circuit;
    }

    /** @return the ingredients, casing plates first; the fluid, if any, last */
    public List<Ing> inputs() {
        return inputs;
    }

    /**
     * @return the voltage tier the recipe runs at: the casing tier, HV for T1 and EV for T2. For T4 and T5 this is ZPM,
     *         the highest {@link Tier}; use {@link #machineVoltageIndex()} for the real machine
     */
    public Tier machineTier() {
        return outputTier();
    }

    /**
     * @return GT's voltage index of the machine: the tier's, except UV (8) for antimatter and UHV (9) for red matter
     */
    public int machineVoltageIndex() {
        if (blast.tier() == ExplosiveTier.T4) {
            return 8;
        }
        if (blast.tier() == ExplosiveTier.T5) {
            return 9;
        }
        return machineTier().voltageIndex();
    }

    /** @return the tier of the output: HV for T1 warheads, EV for T2 (the tier-map rule, D1) */
    public Tier outputTier() {
        if (blast.tier() == ExplosiveTier.T4 || blast.tier() == ExplosiveTier.T5) {
            return Tier.ZPM;
        }
        if (blast.tier() == ExplosiveTier.T3) {
            return blast == WarheadBlast.NUCLEAR ? Tier.ZPM : Tier.LUV;
        }
        return blast.tier() == ExplosiveTier.T1 ? Tier.HV : Tier.EV;
    }

    /** @return the number of item slots the recipe uses, the circuit included */
    public int itemSlots() {
        int n = 1;
        for (Ing in : inputs) {
            if (!in.isFluid()) {
                n++;
            }
        }
        return n;
    }

    /** @return how many fluid inputs the recipe has */
    public int fluidInputs() {
        int n = 0;
        for (Ing in : inputs) {
            if (in.isFluid()) {
                n++;
            }
        }
        return n;
    }

    /** @return the D34 cost, plate equivalents (the casing included) */
    public double cost() {
        double sum = 0.0;
        for (Ing in : inputs) {
            sum += in.cost();
        }
        return sum;
    }
}
