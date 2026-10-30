package com.gregguns.icbm.warhead;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gregguns.icbm.recipe.Ing;

/**
 * The rules of de-arming a warhead (D283), the build chain run backwards one step at a time. Pure, no Minecraft types.
 * <ol>
 * <li><b>Unload charges</b> (crafting grid, a live warhead and a Defuse Kit): each craft takes out up to
 * {@link #MAX_PER_CRAFT} Plastic Explosive, the same most a grid load puts in. The last charge out leaves the inert
 * warhead with the payload it was built with (bare for condensed). The Cutting Machine empties a full warhead at
 * once.</li>
 * <li><b>Remove the payload</b> (Cutting Machine): the payload's items come back; its fluid does not.</li>
 * <li><b>Scrap the casing</b> (Cutting Machine): the casing plates and the Fuze Assembly come back.</li>
 * </ol>
 * Charges and items come back whole, so a build and a de-arm never make or lose an item; only the payload's fluid and
 * the cutting fluid are spent.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (De-arming)
public final class WarheadDearming {

    /** The most charges one crafting-grid unload takes out: the same as the most one grid load puts in. */
    public static final int MAX_PER_CRAFT = WarheadCharging.MAX_PER_CRAFT;
    /** Defuse Kit uses one grid unload spends. */
    public static final int DEFUSER_WEAR_PER_CRAFT = 1;
    /** Item outputs of GT's Cutting Machine ({@code cutterRecipes} {@code maxIO(2, 4, 1, 0)}). */
    public static final int CUTTER_ITEM_OUTPUTS = 4;
    /** The most items one output slot holds. */
    public static final int MAX_STACK = 64;
    /** Ticks of every Cutting Machine de-arm step with lubricant (GT doubles them for water). */
    public static final int CUTTER_TICKS = 200;

    private WarheadDearming() {}

    /**
     * @param fill the charges in the warhead
     * @return the charges one grid unload takes out: all of them up to {@link #MAX_PER_CRAFT}; 0 when empty
     */
    public static int unloaded(int fill) {
        return fill < 1 ? 0 : Math.min(MAX_PER_CRAFT, fill);
    }

    /**
     * @param fill the charges in the warhead
     * @return the charges left after one grid unload; 0 means the result is the inert warhead
     */
    public static int fillLeft(int fill) {
        return Math.max(0, fill - unloaded(fill));
    }

    /**
     * @param fill the charges in the warhead
     * @return how many grid unloads empty it
     */
    public static int craftsToEmpty(int fill) {
        return fill < 1 ? 0 : (fill + MAX_PER_CRAFT - 1) / MAX_PER_CRAFT;
    }

    /**
     * The payload the inert warhead keeps once the last charge is out: the blast it was built with.
     *
     * @param explosive the live warhead's explosive id
     * @return that id as a payload, or null for condensed (charges alone, no payload) or an empty id
     */
    public static String payloadOf(String explosive) {
        if (explosive == null || explosive.isEmpty() || WarheadData.DEFAULT_EXPLOSIVE.equals(explosive)) {
            return null;
        }
        return explosive;
    }

    /**
     * @param charges Plastic Explosive to hand back
     * @return the stack sizes, each at most {@link #MAX_STACK}; empty for none
     */
    public static List<Integer> chargeStacks(int charges) {
        List<Integer> out = new ArrayList<>();
        for (int left = charges; left > 0; left -= MAX_STACK) {
            out.add(Math.min(left, MAX_STACK));
        }
        return out;
    }

    /**
     * What the Cutting Machine gives back for a payload: every item the payload recipe took, at its count. The fluid
     * (gas, fuel, biomass) is spent: canisters come back as they went in, and the fluid poured beside them does not.
     *
     * @param spec a blast's payload recipe
     * @return the item ingredients, in recipe order; empty for condensed, which has no payload
     */
    public static List<Ing> payloadReturns(WarheadRecipeSpec spec) {
        List<Ing> out = new ArrayList<>();
        for (Ing in : spec.inputs()) {
            if (!in.isFluid()) {
                out.add(in);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * @param spec a payload recipe
     * @return the fluid ingredients the Cutting Machine does not give back
     */
    public static List<Ing> payloadLosses(WarheadRecipeSpec spec) {
        List<Ing> out = new ArrayList<>();
        for (Ing in : spec.inputs()) {
            if (in.isFluid()) {
                out.add(in);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * @param spec an inert warhead recipe
     * @return what scrapping the bare casing gives back: its plates and its Fuze Assembly, all of them
     */
    public static List<Ing> casingReturns(InertWarheadSpec spec) {
        return spec.inputs();
    }

    /**
     * @param spec a blast
     * @return the Cutting Machine's item outputs when it empties a full warhead of that blast: the inert warhead and
     *         the charges in stacks of at most 64
     */
    public static int unloadOutputs(WarheadRecipeSpec spec) {
        return 1 + chargeStacks(
            spec.blast()
                .fillCap()).size();
    }

    /**
     * @param spec a blast
     * @return the Cutting Machine's item outputs when it takes the payload out: the bare inert warhead and the items
     */
    public static int payloadOutputs(WarheadRecipeSpec spec) {
        return 1 + payloadReturns(spec).size();
    }
}
