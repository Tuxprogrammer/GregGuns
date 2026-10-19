package com.gregguns.armor.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The timetable of the {@code armor.showcase} dev scenario: which set and camo the player wears when, and when the
 * client takes each screenshot. Pure (no Minecraft types), so the timing is unit-tested. The scenario turns each step
 * into a server action or a client autotest signal.
 *
 * <p>
 * Every set and every camo gets a front view. A set with a custom worn model also gets a back view, because the boiler
 * packs, back units and vents are on the back.
 */
public final class ShowcasePlan {

    /** What a step does. */
    public enum Kind {
        /** The server dresses the player in the set, in the camo. */
        EQUIP,
        /** The client switches the camera: argument {@code 1} is third person from behind, {@code 2} from the front. */
        VIEW,
        /** The client takes a screenshot labelled with the argument. */
        SHOT
    }

    /** One timed action. */
    public static final class Step {

        private final int tick;
        private final Kind kind;
        private final String setId;
        private final int camo;
        private final String argument;

        Step(int tick, Kind kind, String setId, int camo, String argument) {
            this.tick = tick;
            this.kind = kind;
            this.setId = setId;
            this.camo = camo;
            this.argument = argument;
        }

        /** @return the server tick, counted from the scenario start */
        public int tick() {
            return tick;
        }

        /** @return what to do */
        public Kind kind() {
            return kind;
        }

        /** @return the set the step belongs to */
        public String setId() {
            return setId;
        }

        /** @return the camo index */
        public int camo() {
            return camo;
        }

        /** @return the view number or the screenshot label; empty for {@link Kind#EQUIP} */
        public String argument() {
            return argument;
        }
    }

    /** A set to show. */
    public static final class SetSpec {

        private final String id;
        private final int camoCount;
        private final boolean customModel;

        /**
         * @param id          set id
         * @param camoCount   how many camos it has (at least one)
         * @param customModel whether it has its own worn model, which earns it a back view
         */
        public SetSpec(String id, int camoCount, boolean customModel) {
            if (camoCount < 1) {
                throw new IllegalArgumentException("a set has at least one camo: " + id);
            }
            this.id = id;
            this.camoCount = camoCount;
            this.customModel = customModel;
        }
    }

    /** First action, in ticks: lets the player settle after the teleport. */
    public static final int START = 10;
    /** Ticks from the equip to the front screenshot: the equipment packet and a few frames. */
    public static final int FRONT_DELAY = 12;
    /** Ticks from the equip to the camera switch for the back view. */
    public static final int BACK_VIEW_DELAY = 16;
    /** Ticks from the equip to the back screenshot. */
    public static final int BACK_DELAY = 28;
    /** Ticks one entry takes with only a front view. */
    public static final int FRONT_ONLY_LENGTH = 20;
    /** Ticks one entry takes with a back view too. */
    public static final int WITH_BACK_LENGTH = 34;

    private ShowcasePlan() {}

    /**
     * @param setId a set id
     * @param camo  a camo index
     * @param view  {@code front} or {@code back}
     * @return the screenshot label, for example {@code armor-steam-2-back}
     */
    public static String label(String setId, int camo, String view) {
        return "armor-" + setId + "-" + camo + "-" + view;
    }

    /**
     * Builds the timetable: every set in order, every camo of each, a front view and (for a custom model) a back view.
     *
     * @param sets the sets to show
     * @return the steps in tick order
     */
    public static List<Step> build(List<SetSpec> sets) {
        List<Step> steps = new ArrayList<>();
        int base = START;
        for (SetSpec set : sets) {
            for (int camo = 0; camo < set.camoCount; camo++) {
                steps.add(new Step(base, Kind.EQUIP, set.id, camo, ""));
                steps.add(new Step(base + 1, Kind.VIEW, set.id, camo, "2"));
                steps.add(new Step(base + FRONT_DELAY, Kind.SHOT, set.id, camo, label(set.id, camo, "front")));
                if (set.customModel) {
                    steps.add(new Step(base + BACK_VIEW_DELAY, Kind.VIEW, set.id, camo, "1"));
                    steps.add(new Step(base + BACK_DELAY, Kind.SHOT, set.id, camo, label(set.id, camo, "back")));
                    base += WITH_BACK_LENGTH;
                } else {
                    base += FRONT_ONLY_LENGTH;
                }
            }
        }
        return Collections.unmodifiableList(steps);
    }

    /**
     * @param steps a timetable from {@link #build}
     * @return the tick after which nothing happens, plus a margin for the last screenshot's frames
     */
    public static int endTick(List<Step> steps) {
        return steps.isEmpty() ? START
            : steps.get(steps.size() - 1)
                .tick() + 20;
    }
}
