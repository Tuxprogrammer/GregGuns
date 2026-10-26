package com.gregguns.icbm.warhead;

import com.gregguns.api.Tier;

/**
 * Which GT machine makes each T1 and T2 warhead, and at which voltage, as plain data so the tier gate can be unit
 * tested. Every recipe makes a full-casing warhead (the NEI example for that explosive, D23); the T1 condensed starter
 * (fill 1) is a second, cheaper Assembler recipe. No Minecraft types.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (Recipes)
public enum WarheadRecipeSpec {

    /** T1 crater and push types: the Assembler, HV. */
    CONDENSED(WarheadBlast.CONDENSED, Machine.ASSEMBLER, Tier.HV),
    /** Fragments in the Assembler. */
    SHRAPNEL(WarheadBlast.SHRAPNEL, Machine.ASSEMBLER, Tier.HV),
    /** Anvil fragments in the Assembler. */
    ANVIL(WarheadBlast.ANVIL, Machine.ASSEMBLER, Tier.HV),
    /** Push out in the Assembler. */
    REPULSIVE(WarheadBlast.REPULSIVE, Machine.ASSEMBLER, Tier.HV),
    /** Pull in in the Assembler. */
    ATTRACTIVE(WarheadBlast.ATTRACTIVE, Machine.ASSEMBLER, Tier.HV),
    /** Fire needs a reactor for its payload. */
    INCENDIARY(WarheadBlast.INCENDIARY, Machine.CHEMICAL_REACTOR, Tier.HV),
    /** Gas payloads are made in the Chemical Reactor. */
    DEBILITATION(WarheadBlast.DEBILITATION, Machine.CHEMICAL_REACTOR, Tier.HV),
    /** Poison gas. */
    CHEMICAL(WarheadBlast.CHEMICAL, Machine.CHEMICAL_REACTOR, Tier.HV),
    /** Smoke. */
    SMOKE(WarheadBlast.SMOKE, Machine.CHEMICAL_REACTOR, Tier.HV),
    /** T2 fragmentation: the Implosion Compressor shapes the casing. */
    FRAGMENTATION(WarheadBlast.FRAGMENTATION, Machine.IMPLOSION_COMPRESSOR, Tier.EV),
    /** T2 sonic. */
    SONIC(WarheadBlast.SONIC, Machine.IMPLOSION_COMPRESSOR, Tier.EV),
    /** T2 breaching. */
    BREACHING(WarheadBlast.BREACHING, Machine.IMPLOSION_COMPRESSOR, Tier.EV),
    /** T2 thermobaric. */
    THERMOBARIC(WarheadBlast.THERMOBARIC, Machine.IMPLOSION_COMPRESSOR, Tier.EV),
    /** T2 contagious gas. */
    CONTAGIOUS(WarheadBlast.CONTAGIOUS, Machine.CHEMICAL_REACTOR, Tier.EV);

    /** The GT machine, which picks the recipe map. */
    public enum Machine {
        /** {@code RecipeMaps.assemblerRecipes}. */
        ASSEMBLER,
        /** {@code RecipeMaps.chemicalReactorRecipes}. */
        CHEMICAL_REACTOR,
        /** {@code RecipeMaps.implosionRecipes}. */
        IMPLOSION_COMPRESSOR
    }

    private final WarheadBlast blast;
    private final Machine machine;
    private final Tier machineTier;

    WarheadRecipeSpec(WarheadBlast blast, Machine machine, Tier machineTier) {
        this.blast = blast;
        this.machine = machine;
        this.machineTier = machineTier;
    }

    /** @return the blast this makes a warhead of */
    public WarheadBlast blast() {
        return blast;
    }

    /** @return the machine */
    public Machine machine() {
        return machine;
    }

    /** @return the voltage tier the recipe runs at */
    public Tier machineTier() {
        return machineTier;
    }

    /** @return the tier of the output: HV for T1 warheads, EV for T2 (the tier-map rule, D1) */
    public Tier outputTier() {
        return blast.tier() == com.gregguns.icbm.explosive.ExplosiveTier.T1 ? Tier.HV : Tier.EV;
    }
}
