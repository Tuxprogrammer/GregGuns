package com.gregguns.icbm.production;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.icbm.compat.GalacticraftParts;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.Ing;

/**
 * How a missile body is made, as plain data (D8, D27, D281): the chip, the schematic, the NASA Workbench slots and the
 * machine recipe of the guidance-0 body, for each body tier. A body below LuV is built in GT's Assembler; from LuV up
 * it
 * is built on the Assembly Line behind a research scan, because the Assembly Line is a LuV machine in GTNH (D281). The
 * recipe classes resolve the {@link Ing} data to real stacks; the unit tests read the same table. No Minecraft types,
 * both sides.
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
     * recipes). Built in the Assembler at HV, 30 s.
     */
    T1(1, Tier.HV, 400, 600, "craftingLensRed", Ing.gt("compressed", "Steel", 3, Tier.MV, 2.0),
        Ing.modItem(GalacticraftParts.FINS_T1, 4, Tier.HV, 28.0),
        Ing.modItem(GalacticraftParts.ENGINE_T1, 1, Tier.HV, 38.0),
        Arrays.asList(IcbmPart.GUIDANCE_COMPUTER_MK1, IcbmPart.GUIDANCE_COMPUTER_MK2, IcbmPart.GUIDANCE_COMPUTER_MK3),
        0, null, 600, 150, 175,
        Arrays.asList(
            Ing.gtItem("Circuit_Board_Plastic_Advanced", 1, Tier.HV, 2.0),
            Ing.gt("circuit", "Advanced", 2, Tier.HV, 3.0),
            Ing.gt("wireFine", "Gold", 4, Tier.MV, 0.5))),
    /**
     * T2 body: EV chip, four compressed titanium plates, four heavy rocket fins, a heavy rocket engine (D69, D343: the
     * Tier 3 and Tier 4 rockets' parts). Costs: a heavy fin is 2 Mars and 4 asteroid reinforced plates; the heavy
     * engine is 2 Tier 1 engines, 2 boosters and 5 asteroid plates (the GTNH recipes). The reinforced plate's heavy
     * duty
     * ingot needs an IV Assembler, so the body's parts need IV: the top of the T2 band (D5), accepted with the Mars
     * trip (D329). Built in the Assembler at EV, 45 s.
     */
    T2(2, Tier.EV, 600, 600, "craftingLensBlue", Ing.gt("compressed", "Titanium", 4, Tier.EV, 2.5),
        Ing.modItem(GalacticraftParts.FINS_HEAVY, 4, Tier.IV, 68.0),
        Ing.modItem(GalacticraftParts.ENGINE_HEAVY, 1, Tier.IV, 200.0),
        Arrays.asList(IcbmPart.GUIDANCE_COMPUTER_MK2, IcbmPart.GUIDANCE_COMPUTER_MK3), 0, null, 900, 460, 520,
        Arrays.asList(
            Ing.gtItem("Circuit_Board_Epoxy_Advanced", 1, Tier.EV, 3.0),
            Ing.gt("circuit", "Data", 2, Tier.EV, 5.0),
            Ing.gt("wireFine", "Electrum", 4, Tier.HV, 0.5))),
    /**
     * T3 body: LuV chip, the Tier 4 rocket's hull, fins and engine (D343): four Heavy Duty Plates Tier 4 (LuV, the
     * Assembly Line) in the plate slots, four heavy rocket fins and a heavy rocket engine. Every part is buildable at
     * LuV; the Heavy Duty Rocket Fins and Engine Tier 3 it took before need ZPM (their Tier 5 heavy duty plate). The
     * only guidance computer is Mk III (IV), below the body's tier, so a T3 body carries no guidance steps (D70). Built
     * on the Assembly Line at LuV, 60 s, behind a 180 s research scan of a hand-built T3 body at LuV.
     */
    T3(3, Tier.LUV, 800, 600, "craftingLensYellow", Ing.modItem(GalacticraftParts.HULL_T3, 4, Tier.LUV, 18.0),
        Ing.modItem(GalacticraftParts.FINS_HEAVY, 4, Tier.IV, 68.0),
        Ing.modItem(GalacticraftParts.ENGINE_HEAVY, 1, Tier.IV, 200.0), Arrays.asList(IcbmPart.GUIDANCE_COMPUTER_MK3),
        180, Tier.LUV, 1200, 530, 600,
        Arrays.asList(
            Ing.gtItem("Circuit_Board_Multifiberglass", 1, Tier.LUV, 4.0),
            Ing.gt("circuit", "Master", 2, Tier.LUV, 12.0),
            Ing.gt("wireFine", "Platinum", 8, Tier.EV, 0.5)));

    /**
     * The lowest body tier built on the Assembly Line. GT's Assembly Line, with its scanner research, is a LuV machine
     * in GTNH (pack quest 1412), so every body below it is built in the Assembler at its own voltage (D281).
     */
    public static final Tier ASSEMBLY_LINE_TIER = Tier.LUV;
    /** Lubricant in a body's machine recipe (Assembler or Assembly Line), liters. */
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
    private final Ing engine;
    private final List<IcbmPart> computers;
    private final int scanSeconds;
    private final Tier scanTier;
    private final int bodyTicks;
    private final int costMin;
    private final int costMax;
    private final List<Ing> chipInputs;

    MissileBuildSpec(int number, Tier tier, int chipTicks, int engraveTicks, String lens, Ing plates, Ing fins,
        Ing engine, List<IcbmPart> computers, int scanSeconds, Tier scanTier, int bodyTicks, int costMin, int costMax,
        List<Ing> chipInputs) {
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
        this.bodyTicks = bodyTicks;
        this.costMin = costMin;
        this.costMax = costMax;
        this.chipInputs = Collections.unmodifiableList(chipInputs);
    }

    /** @return 1 to 3: the body tier number */
    public int number() {
        return number;
    }

    /** @return the voltage tier of the chip recipe, the schematic recipe and the body's machine recipe */
    public Tier tier() {
        return tier;
    }

    /**
     * @return the highest voltage any part of the body may need: the body's own tier, except T2, whose heavy fins and
     *         engine need IV, the top of the T2 band (D5, D343)
     */
    public Tier partTier() {
        return number == 2 ? Tier.IV : tier;
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

    /**
     * @return the body plates (Workbench slots and the body's machine recipe): GT compressed plates at T1 and T2, the
     *         Tier 4 rocket's heavy duty plates at T3
     */
    public Ing plates() {
        return plates;
    }

    /** @return the four Galacticraft rocket fins, one per fin slot */
    public Ing fins() {
        return fins;
    }

    /** @return the Galacticraft rocket engine */
    public Ing engine() {
        return engine;
    }

    /** @return the guidance computers this body accepts, lowest first */
    public List<IcbmPart> computers() {
        return computers;
    }

    /**
     * @return true when the guidance-0 body is built on the Assembly Line (LuV and up, behind a research scan); false
     *         when it is built in the Assembler, with no research (below LuV, D281)
     */
    public boolean onAssemblyLine() {
        return tier.voltageIndex() >= ASSEMBLY_LINE_TIER.voltageIndex();
    }

    /** @return the research scan time, seconds; 0 for a body built in the Assembler */
    public int scanSeconds() {
        return scanSeconds;
    }

    /** @return the voltage tier of the research scan; null for a body built in the Assembler */
    public Tier scanTier() {
        return scanTier;
    }

    /** @return the length of the body's machine recipe (Assembler or Assembly Line), ticks */
    public int bodyTicks() {
        return bodyTicks;
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
        // A computer below the body's tier (Mk III on a T3 body) fits but gives no steps (D24, D70).
        return Math.max(
            0,
            Math.min(
                MAX_GUIDANCE,
                computer.tier()
                    .voltageIndex() - tier.voltageIndex()));
    }

    /**
     * @return the control circuits the machine wires into the body, in place of the guidance computer (GT's Assembly
     *         Line needs at least four item inputs, and the Assembler recipe keeps the same inputs); the same circuit
     *         family as the tier's chip
     */
    public Ing bodyCircuits() {
        return number == 1 ? Ing.gt("circuit", "Advanced", 2, Tier.HV, 3.0)
            : number == 2 ? Ing.gt("circuit", "Data", 2, Tier.EV, 5.0) : Ing.gt("circuit", "Master", 2, Tier.LUV, 12.0);
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
        return plates.cost() + fins.cost() + engine.cost();
    }

    /**
     * @param computer a guidance computer this body accepts
     * @return the D34 cost of the whole body with that computer
     */
    public double cost(IcbmPart computer) {
        return baseCost() + computer.unitCost();
    }

    /**
     * @param number 1 to 3
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
