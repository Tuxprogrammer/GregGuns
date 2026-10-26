package com.gregguns.icbm.production;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.icbm.compat.GalacticraftParts;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.Ing;

/**
 * How a missile body is made, as plain data (D8, D27): the chip, the schematic, the NASA Workbench slots and the
 * Assembly Line recipe, for each body tier. The recipe classes resolve the {@link Ing} data to real stacks; the unit
 * tests read the same table. No Minecraft types, both sides.
 *
 * <p>
 * The warhead is not a slot: it is stacked on the pad separately (D31, 0018 section 8), which amends D8's slot list.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md
public enum MissileBuildSpec {

    /**
     * T1 body: HV chip, three compressed steel plates, four Galacticraft Tier 1 fins, a Galacticraft Tier 1 engine
     * (D69). Costs: a compressed plate is a plate plus an implosion; a fin is 2 compressed steel and 4 heavy-duty
     * plates;
     * an engine is 2 SDHD120 plates, 4 heavy-duty plates, a canister, a firebox casing and a detector (the GTNH
     * recipes).
     */
    T1(1, Tier.HV, 400, 600, "craftingLensRed", Ing.gt("compressed", "Steel", 3, Tier.MV, 2.0),
        Ing.modItem(GalacticraftParts.FINS_T1, 4, Tier.HV, 28.0), IcbmPart.MISSILE_ENGINE_MK1,
        Arrays.asList(IcbmPart.GUIDANCE_COMPUTER_MK1, IcbmPart.GUIDANCE_COMPUTER_MK2, IcbmPart.GUIDANCE_COMPUTER_MK3),
        60, Tier.HV, 600, 120, 140,
        Arrays.asList(
            Ing.gtItem("Circuit_Board_Plastic_Advanced", 1, Tier.HV, 2.0),
            Ing.gt("circuit", "Advanced", 2, Tier.HV, 3.0),
            Ing.gt("wireFine", "Gold", 4, Tier.MV, 0.5))),
    /**
     * T2 body: EV chip, four compressed titanium plates, four Galacticraft Tier 2 fins, a Galacticraft Tier 2 engine
     * (D69). Costs: a Tier 2 fin is 2 Mars and 4 asteroid reinforced plates; the Tier 2 engine is 2 Tier 1 engines, 2
     * boosters and 5 asteroid plates (the GTNH recipes).
     */
    T2(2, Tier.EV, 600, 600, "craftingLensBlue", Ing.gt("compressed", "Titanium", 4, Tier.EV, 2.5),
        Ing.modItem(GalacticraftParts.FINS_T2, 4, Tier.EV, 68.0), IcbmPart.MISSILE_ENGINE_MK2,
        Arrays.asList(IcbmPart.GUIDANCE_COMPUTER_MK2, IcbmPart.GUIDANCE_COMPUTER_MK3), 120, Tier.EV, 900, 280, 320,
        Arrays.asList(
            Ing.gtItem("Circuit_Board_Epoxy_Advanced", 1, Tier.EV, 3.0),
            Ing.gt("circuit", "Data", 2, Tier.EV, 5.0),
            Ing.gt("wireFine", "Electrum", 4, Tier.HV, 0.5)));

    /** Lubricant in an Assembly Line recipe, liters. */
    public static final int LUBRICANT_LITERS = 250;
    /** The Assembler fits a guidance computer into a body this long, ticks. */
    public static final int FIT_TICKS = 300;
    /** The Laser Engraver runs this long, ticks. */
    public static final int ENGRAVE_TICKS = 600;
    /** The most guidance steps a body can carry (D24); mirrors {@code MissileBodies.MAX_GUIDANCE}. */
    public static final int MAX_GUIDANCE = 3;

    private final int number;
    private final Tier tier;
    private final int chipTicks;
    private final int engraveTicks;
    private final String lens;
    private final Ing plates;
    private final Ing fins;
    private final IcbmPart engine;
    private final List<IcbmPart> computers;
    private final int scanSeconds;
    private final Tier scanTier;
    private final int lineTicks;
    private final int costMin;
    private final int costMax;
    private final List<Ing> chipInputs;

    MissileBuildSpec(int number, Tier tier, int chipTicks, int engraveTicks, String lens, Ing plates, Ing fins,
        IcbmPart engine, List<IcbmPart> computers, int scanSeconds, Tier scanTier, int lineTicks, int costMin,
        int costMax, List<Ing> chipInputs) {
        this.number = number;
        this.tier = tier;
        this.chipTicks = chipTicks;
        this.engraveTicks = engraveTicks;
        this.lens = lens;
        this.plates = plates;
        this.fins = fins;
        this.engine = engine;
        this.computers = Collections.unmodifiableList(computers);
        this.scanSeconds = scanSeconds;
        this.scanTier = scanTier;
        this.lineTicks = lineTicks;
        this.costMin = costMin;
        this.costMax = costMax;
        this.chipInputs = Collections.unmodifiableList(chipInputs);
    }

    /** @return 1 or 2: the body tier number */
    public int number() {
        return number;
    }

    /** @return the voltage tier of the chip recipe, the schematic recipe and the Assembly Line recipe */
    public Tier tier() {
        return tier;
    }

    /** @return the Circuit Assembler recipe length, ticks */
    public int chipTicks() {
        return chipTicks;
    }

    /** @return the Laser Engraver recipe length, ticks */
    public int engraveTicks() {
        return engraveTicks;
    }

    /** @return the ore name of the non-consumed lens the engraver needs */
    public String lens() {
        return lens;
    }

    /** @return the chip's Circuit Assembler inputs */
    public List<Ing> chipInputs() {
        return chipInputs;
    }

    /** @return the body plates (Workbench slots and Assembly Line input) */
    public Ing plates() {
        return plates;
    }

    /** @return the four Galacticraft rocket fins, one per fin slot */
    public Ing fins() {
        return fins;
    }

    /** @return the Galacticraft rocket engine */
    public IcbmPart engine() {
        return engine;
    }

    /** @return the guidance computers this body accepts, lowest first */
    public List<IcbmPart> computers() {
        return computers;
    }

    /** @return the research scan time, seconds */
    public int scanSeconds() {
        return scanSeconds;
    }

    /** @return the voltage tier of the research scan */
    public Tier scanTier() {
        return scanTier;
    }

    /** @return the Assembly Line recipe length, ticks */
    public int lineTicks() {
        return lineTicks;
    }

    /** @return the lowest D34 cost of the body, plate equivalents, that the tests accept */
    public int costMin() {
        return costMin;
    }

    /** @return the highest D34 cost of the body, plate equivalents, that the tests accept */
    public int costMax() {
        return costMax;
    }

    /**
     * @param computer a guidance computer
     * @return how many guidance steps it gives this body: its tier minus the body's tier, or -1 when this body does
     *         not accept it
     */
    public int guidanceOf(IcbmPart computer) {
        if (!computers.contains(computer)) {
            return -1;
        }
        return Math.min(
            MAX_GUIDANCE,
            computer.tier()
                .voltageIndex() - tier.voltageIndex());
    }

    /**
     * @return the control circuits the Assembly Line wires into the body, in place of the guidance computer (GT's line
     *         needs at least four item inputs); the same circuit family as the tier's chip
     */
    public Ing lineCircuits() {
        return number == 1 ? Ing.gt("circuit", "Advanced", 2, Tier.HV, 3.0)
            : Ing.gt("circuit", "Data", 2, Tier.EV, 5.0);
    }

    /**
     * @return the programmed circuit of this tier's guidance-fit Assembler recipes (the inputs, a body, already keep
     *         them apart from every other recipe)
     */
    public int fitCircuit() {
        return number;
    }

    /** @return the D34 cost of the body without its guidance computer, plate equivalents */
    public double baseCost() {
        return plates.cost() + fins.cost() + engine.unitCost();
    }

    /**
     * @param computer a guidance computer this body accepts
     * @return the D34 cost of the whole body with that computer
     */
    public double cost(IcbmPart computer) {
        return baseCost() + computer.unitCost();
    }

    /**
     * @param number 1 or 2
     * @return the spec, or null
     */
    public static MissileBuildSpec byNumber(int number) {
        for (MissileBuildSpec s : values()) {
            if (s.number == number) {
                return s;
            }
        }
        return null;
    }
}
