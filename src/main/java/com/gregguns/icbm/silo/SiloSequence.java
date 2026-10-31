package com.gregguns.icbm.silo;

/**
 * The silo's staged assembly as plain data (D286): what the controller remembers of its own work ({@link Stage}), what
 * it sees in the shaft ({@link Shaft}) and the next step it takes ({@link Step}). The parts themselves live in the
 * world, never in the controller: the body is a placed body block on the pad, the warhead a placed warhead block on
 * its nose, exactly as a player stacks them by hand (D31). So breaking the silo, or a part, mid-sequence drops each
 * part
 * once, through the part's own block. No Minecraft types, both sides, so the unit tests pin it.
 *
 * <p>
 * The order is fixed: body, then warhead, then fuel. Each part is placed in the shaft before its working cycle starts;
 * the warhead cycle ends by arming the stack into the missile entity, which the fuel cycles then fill.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (The assembly sequence)
public final class SiloSequence {

    /** The silo's own progress, saved in the controller. */
    public enum Stage {
        /** No body of the silo's stands in the shaft. */
        NONE,
        /** A body stands on the pad; its working cycle has not finished. */
        BODY,
        /** The body's cycle finished; the silo waits for a warhead. */
        BODY_DONE,
        /** A warhead sits on the body's nose; the mating cycle has not finished. Its end arms the missile. */
        WARHEAD
    }

    /** What stands in the shaft, as the controller sees it. */
    public enum Shaft {
        /** Nothing: a body can be placed. */
        EMPTY,
        /** A body block on the pad center with nothing on its nose. */
        BODY,
        /** A body with a warhead on its nose that would arm. */
        STACK,
        /** A body with a warhead on its nose that would not arm (too heavy for the body, or its fuse lit). */
        BAD_STACK,
        /** An armed missile entity, unlaunched. */
        MISSILE,
        /**
         * A missile counting down or climbing out of the shaft (D342): the shaft is busy until the missile has left or
         * stood down, so no part is placed under it.
         */
        LAUNCHING,
        /** Anything else: a body off the pad center, a loose warhead, a block in the way. */
        OTHER
    }

    /** The next thing the controller does. */
    public enum Step {
        /** Pump fuel into the missile (the pump has its own statuses). */
        PUMP,
        /** Take a body out of the bus, stand it on the pad and start the body cycle. */
        PLACE_BODY,
        /** Run (or rerun after an interruption) the body cycle on the body that stands. */
        RUN_BODY,
        /** Take a fitting warhead out of the bus, seat it on the nose and start the mating cycle. */
        PLACE_WARHEAD,
        /** Run (or rerun) the mating cycle on the stack that stands; its end arms the missile. */
        RUN_WARHEAD,
        /** Nothing to do: no body in the bus. */
        NO_BODY,
        /** A warhead waits in the bus for a body. */
        WAIT_BODY,
        /** The body stands, ready; no fitting warhead in the bus. */
        WAIT_WARHEAD,
        /** The warhead on the nose does not fit or is lit: a player must take it off. */
        BAD_WARHEAD,
        /** Something else is in the shaft. */
        BLOCKED,
        /** A launch is under way: wait until the missile has left the shaft or stood down. */
        WAIT_LAUNCH
    }

    private SiloSequence() {}

    /**
     * Squares the remembered stage with the shaft: the world wins. A body that is gone (broken, armed by hand,
     * launched)
     * ends the silo's sequence; a warhead taken off the nose steps back to waiting for one. A body found in the shaft
     * with no sequence of the silo's (stacked by hand, or left by an earlier controller) is adopted: it gets its body
     * cycle like any other, so a rebuilt silo carries on.
     *
     * @param stage what the controller remembers
     * @param shaft what stands in the shaft now
     * @return the stage that matches the shaft
     */
    public static Stage reconcile(Stage stage, Shaft shaft) {
        boolean body = shaft == Shaft.BODY || shaft == Shaft.STACK || shaft == Shaft.BAD_STACK;
        if (!body) {
            return Stage.NONE;
        }
        if (stage == Stage.NONE) {
            return Stage.BODY;
        }
        if (stage == Stage.WARHEAD && shaft == Shaft.BODY) {
            return Stage.BODY_DONE;
        }
        return stage;
    }

    /**
     * @param stage   the reconciled stage ({@link #reconcile})
     * @param shaft   what stands in the shaft
     * @param body    whether the bus holds a missile body
     * @param warhead whether the bus holds a warhead: any warhead while no body stands, a fitting one once it does
     * @return the next step, before the voltage and power checks the controller adds
     */
    public static Step next(Stage stage, Shaft shaft, boolean body, boolean warhead) {
        if (shaft == Shaft.LAUNCHING) {
            return Step.WAIT_LAUNCH;
        }
        if (shaft == Shaft.MISSILE) {
            return Step.PUMP;
        }
        if (shaft == Shaft.OTHER) {
            return Step.BLOCKED;
        }
        switch (stage) {
            case NONE:
                return body ? Step.PLACE_BODY : warhead ? Step.WAIT_BODY : Step.NO_BODY;
            case BODY:
                return Step.RUN_BODY;
            case BODY_DONE:
                if (shaft == Shaft.STACK) {
                    return Step.RUN_WARHEAD; // a warhead seated by hand is mated like the silo's own
                }
                if (shaft == Shaft.BAD_STACK) {
                    return Step.BAD_WARHEAD;
                }
                return warhead ? Step.PLACE_WARHEAD : Step.WAIT_WARHEAD;
            case WARHEAD:
            default:
                return shaft == Shaft.BAD_STACK ? Step.BAD_WARHEAD : Step.RUN_WARHEAD;
        }
    }

    /**
     * @param finished the stage whose working cycle just ended
     * @return the stage after it: a body cycle leaves the body ready for a warhead; a mating cycle ends the sequence
     *         (the
     *         controller arms the stack, and the missile then stands in the shaft)
     */
    public static Stage afterCycle(Stage finished) {
        switch (finished) {
            case BODY:
                return Stage.BODY_DONE;
            case WARHEAD:
                return Stage.NONE;
            default:
                return finished;
        }
    }

    /**
     * @param name a saved stage name
     * @return the stage, or {@link Stage#NONE} for an unknown or empty name
     */
    public static Stage parse(String name) {
        if (name == null || name.isEmpty()) {
            return Stage.NONE;
        }
        try {
            return Stage.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Stage.NONE;
        }
    }
}
