package com.gregguns.icbm.warhead;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.icbm.explosive.ExplosiveTier;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.Ing;

/**
 * The payload of every warhead blast (D282): what the Assembler adds to a bare inert warhead to fit it with that blast
 * type, at the blast's tier. A payload holds only the effect materials (fragments, canisters and their fluid, field
 * generators, the fissile core); the explosive is the Plastic Explosive charges loaded afterwards, one charge per unit
 * of fill. Condensed has no payload: Plastic Explosive alone in a T1 casing is plain high explosive. Each blast also
 * has
 * an Assembler fill recipe on the same circuit that loads a full casing of charges. No Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The warhead chain)
public enum WarheadRecipeSpec {

    /** Plain high explosive: no payload, only charges (T1). */
    CONDENSED(WarheadBlast.CONDENSED, 1),
    /** Steel fragments around the charges. */
    SHRAPNEL(WarheadBlast.SHRAPNEL, 3, Ing.part(IcbmPart.FRAGMENTATION_SLEEVE_STEEL, 1)),
    /** A dense lead mass around the charges. */
    ANVIL(WarheadBlast.ANVIL, 4, Ing.gt("plate", "Lead", 12, Tier.LV, 0.5)),
    /** Napalm: two filled canisters for the charges to burst. */
    INCENDIARY(WarheadBlast.INCENDIARY, 5, Ing.part(IcbmPart.NAPALM_CANISTER, 2)),
    /** A dizzying gas: nitrous oxide in two canisters. */
    DEBILITATION(WarheadBlast.DEBILITATION, 6, Ing.part(IcbmPart.DISPERSAL_CANISTER, 2),
        Ing.gas("NitrousOxide", 2000, Tier.LV)),
    /** Pungent ammonia gas in two canisters; a few mustard seeds for a pun when HarvestCraft is there. */
    CHEMICAL(WarheadBlast.CHEMICAL, 7, Ing.part(IcbmPart.DISPERSAL_CANISTER, 2),
        Ing.optionalMod("harvestcraft:mustardseedsItem", 4), Ing.gas("Ammonia", 2000, Tier.HV)),
    /** A smoky fluid and coal dust. */
    SMOKE(WarheadBlast.SMOKE, 8, Ing.part(IcbmPart.DISPERSAL_CANISTER, 2), Ing.gt("dust", "Coal", 8, Tier.LV, 0.2),
        Ing.fluid("Creosote", 2000, Tier.LV)),
    /** A field generator pushes everything out. */
    REPULSIVE(WarheadBlast.REPULSIVE, 9, Ing.gtItem("Field_Generator_HV", 1, Tier.HV, 8.0)),
    /** The same generator with magnets pulls everything in. */
    ATTRACTIVE(WarheadBlast.ATTRACTIVE, 10, Ing.gtItem("Field_Generator_HV", 1, Tier.HV, 8.0),
        Ing.gt("stick", "SteelMagnetic", 4, Tier.LV, 0.5)),
    /** A titanium sleeve of fragments. */
    FRAGMENTATION(WarheadBlast.FRAGMENTATION, 19, Ing.part(IcbmPart.FRAGMENTATION_SLEEVE_TITANIUM, 1)),
    /** A big fuel-air blast: kerosene in two canisters. */
    THERMOBARIC(WarheadBlast.THERMOBARIC, 24, Ing.part(IcbmPart.DISPERSAL_CANISTER, 2), Ing.kerosene(4000)),
    /** Two emitters shatter glass and stun. */
    SONIC(WarheadBlast.SONIC, 21, Ing.gtItem("Emitter_EV", 2, Tier.EV, 6.0)),
    /** Copper liner cones and a titanium penetrator; tungstensteel waits for T3. */
    BREACHING(WarheadBlast.BREACHING, 22, Ing.part(IcbmPart.SHAPED_CHARGE_LINER_CONE, 2),
        Ing.gt("stick", "Titanium", 6, Tier.EV, 1.0)),
    /** The hot T3 blast: napalm and kerosene (LuV). */
    EXOTHERMIC(WarheadBlast.EXOTHERMIC, 1, Ing.part(IcbmPart.NAPALM_CANISTER, 4), Ing.kerosene(8000)),
    /** The cold T3 blast: helium in dispersal canisters (LuV). */
    ENDOTHERMIC(WarheadBlast.ENDOTHERMIC, 2, Ing.part(IcbmPart.DISPERSAL_CANISTER, 4),
        Ing.gas("Helium", 8000, Tier.IV)),
    /** A fission core of uranium-235 and plutonium; the charges are its implosion lens (ZPM). */
    NUCLEAR(WarheadBlast.NUCLEAR, 3, Ing.gt("ingot", "Uranium235", 8, Tier.HV, 6.0),
        Ing.gt("ingot", "Plutonium", 4, Tier.EV, 8.0)),
    /**
     * The antimatter payload (UV, D134): two UV field generators to contain it, a Quantum Eye and a plutonium-241
     * seed.
     */
    ANTIMATTER(WarheadBlast.ANTIMATTER, 1, Ing.gtItem("Field_Generator_UV", 2, Tier.ZPM, 20.0),
        Ing.gtItem("QuantumEye", 1, Tier.ZPM, 15.0), Ing.gt("ingot", "Plutonium241", 4, Tier.EV, 8.0)),
    /** The red matter payload (UHV, D134): two UHV field generators, two Quantum Eyes and two UHV emitters. */
    REDMATTER(WarheadBlast.REDMATTER, 2, Ing.gtItem("Field_Generator_UHV", 2, Tier.ZPM, 30.0),
        Ing.gtItem("QuantumEye", 2, Tier.ZPM, 15.0), Ing.gtItem("Emitter_UHV", 2, Tier.ZPM, 24.0)),
    /** Two field generators and emitters around the flux-compression charges (LuV). */
    EMP(WarheadBlast.EMP, 4, Ing.gtItem("Field_Generator_LuV", 2, Tier.LUV, 12.0),
        Ing.gtItem("Emitter_LuV", 2, Tier.LUV, 10.0)),
    /** An emitter and sixteen Eyes of Ender that the charges pull open (LuV, D80). */
    ENDER(WarheadBlast.ENDER, 5, Ing.gtItem("Emitter_LuV", 1, Tier.LUV, 10.0),
        Ing.gt("gem", "EnderEye", 16, Tier.HV, 2.0)),
    /** Two field generators and Nether Stars hold the blocks off the ground (LuV, D80). */
    ANTIGRAV(WarheadBlast.ANTIGRAV, 6, Ing.gtItem("Field_Generator_LuV", 2, Tier.LUV, 12.0),
        Ing.gt("gem", "NetherStar", 4, Tier.EV, 6.0)),
    /** A Bio Lab culture in biomass. */
    CONTAGIOUS(WarheadBlast.CONTAGIOUS, 23, Ing.bioDish(4), Ing.fluid("Biomass", 2000, Tier.HV));

    // Programmed-circuit numbers are 1 to 24 in GT: the warheads use 1 to 10 and 19 to 24, the parts 11 to 18.
    private final WarheadBlast blast;
    private final int circuit;
    private final List<Ing> inputs;

    WarheadRecipeSpec(WarheadBlast blast, int circuit, Ing... inputs) {
        this.blast = blast;
        this.circuit = circuit;
        this.inputs = Collections.unmodifiableList(Arrays.asList(inputs));
    }

    /** The cost of one Plastic Explosive charge, plate equivalents (D34). */
    public static final double CHARGE_COST = IcbmPart.PLASTIC_EXPLOSIVE.unitCost();

    /** @return the blast this makes a warhead of */
    public WarheadBlast blast() {
        return blast;
    }

    /** @return true when the blast has no payload and is made from charges alone (condensed) */
    public boolean chargesOnly() {
        return inputs.isEmpty();
    }

    /** @return the programmed circuit of the payload recipe and of the fill recipe of this blast */
    public int circuit() {
        return circuit;
    }

    /** @return the payload's ingredients, the fluid (if any) last; empty for condensed */
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

    /** @return the item slots the payload recipe uses: the inert warhead, the payload items and the circuit */
    public int itemSlots() {
        int n = 2;
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

    /** @return the D34 cost of the payload alone, plate equivalents */
    public double payloadCost() {
        double sum = 0.0;
        for (Ing in : inputs) {
            sum += in.cost();
        }
        return sum;
    }

    /** @return the casing of this blast */
    public WarheadCasing casing() {
        return WarheadCasing.forTier(blast.tier());
    }

    /** @return the D34 cost of a full warhead: the inert warhead, the payload and a full casing of charges */
    public double cost() {
        return InertWarheadSpec.of(casing())
            .cost() + payloadCost()
            + blast.fillCap() * CHARGE_COST;
    }
}
