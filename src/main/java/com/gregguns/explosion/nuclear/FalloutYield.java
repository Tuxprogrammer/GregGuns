package com.gregguns.explosion.nuclear;

/**
 * The byproduct yields of the fallout chain (D21), as plain data so the numbers can be unit tested and the GT recipes
 * built from them cannot drift. Every entry is an element, a dust size and a chance. The rule the numbers obey: one
 * nuclear warhead's whole fallout returns at most a quarter of the fissile material in its core (8 uranium-235 and 4
 * plutonium ingots), however large the blast, so a warhead is never a net source of fuel ({@link #maxFissileShare}).
 * No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md (The byproduct chain)
public final class FalloutYield {

    /** Dust sizes, as a share of a full dust. */
    public enum Size {

        /** A tiny dust, one ninth. */
        TINY(1.0 / 9.0),
        /** A small dust, one quarter. */
        SMALL(0.25);

        private final double units;

        Size(double units) {
            this.units = units;
        }

        /** @return the share of a full dust */
        public double units() {
            return units;
        }
    }

    /** One possible output of a recipe. */
    public static final class Out {

        private final String element;
        private final Size size;
        private final int chance;

        Out(String element, Size size, int chance) {
            this.element = element;
            this.size = size;
            this.chance = chance;
        }

        /** @return the element's name, as GT spells its material */
        public String element() {
            return element;
        }

        /** @return the dust size */
        public Size size() {
            return size;
        }

        /** @return the chance, in hundredths of a percent (10,000 is certain) */
        public int chance() {
            return chance;
        }

        /** @return the expected full dusts per input item */
        public double expected() {
            return size.units() * chance / 10000.0;
        }
    }

    /** Most blocks of a blast's fallout that can be hot rubble: the inner share of the cap. */
    public static final double HOT_SHARE = 0.3;
    /** Uranium-235 ingots in a nuclear warhead's core (WarheadRecipeSpec.NUCLEAR). */
    public static final int CORE_U235 = 8;
    /** Plutonium ingots in a nuclear warhead's core. */
    public static final int CORE_PU = 4;
    /** The share of the core a blast's fallout may return at most. */
    public static final double MAX_RETURN = 0.25;
    /** Fallout goo blocks one blast makes at most, unless the config says less (the default of the config). */
    public static final int DEFAULT_MAX_GOO = 24;

    /** Irradiated rubble from the outer fallout: the base metals, no fissile material. Centrifuge, HV. */
    public static final Out[] RUBBLE = { out("Lead", Size.SMALL, 5000), out("Thorium", Size.TINY, 3000),
        out("Uranium", Size.TINY, 2000) };
    /** Hot rubble from the inner fallout of a ZPM or better blast. Centrifuge, HV. */
    public static final Out[] HOT_RUBBLE = { out("Lead", Size.SMALL, 5000), out("Thorium", Size.TINY, 4000),
        out("Uranium", Size.TINY, 4000), out("Uranium235", Size.TINY, 80), out("Plutonium", Size.TINY, 50) };
    /** Trinitite once macerated. Centrifuge, HV. */
    public static final Out[] CRUSHED_TRINITITE = { out("Uranium", Size.TINY, 3000), out("Thorium", Size.TINY, 2000),
        out("Plutonium", Size.TINY, 40), out("Uranium235", Size.TINY, 20) };
    /** Fallout goo after the nitric acid bath. Centrifuge, EV. */
    public static final Out[] DISSOLVED_GOO = { out("Technetium", Size.TINY, 4000), out("Promethium", Size.TINY, 4000),
        out("Neptunium", Size.TINY, 3000), out("Plutonium241", Size.TINY, 3000), out("Americium", Size.TINY, 2000),
        out("Curium", Size.TINY, 1000) };
    /** Goo from a ZPM or better blast after the bath: the heavier mix with trace fermium. Centrifuge, EV. */
    public static final Out[] DISSOLVED_HOT_GOO = { out("Neptunium", Size.TINY, 4000),
        out("Plutonium241", Size.TINY, 4000), out("Americium", Size.TINY, 3000), out("Curium", Size.TINY, 2000),
        out("Californium", Size.TINY, 500), out("Fermium", Size.TINY, 100) };
    /** The noble gases the goo bath also gives, liters per bath (both goo kinds). */
    public static final int KRYPTON_LITERS = 200;
    /** Xenon per bath, liters. */
    public static final int XENON_LITERS = 100;

    private FalloutYield() {}

    private static Out out(String element, Size size, int chance) {
        return new Out(element, size, chance);
    }

    /**
     * @param outs    a recipe's outputs
     * @param element an element name
     * @return the expected full dusts of that element per input item
     */
    public static double expected(Out[] outs, String element) {
        double sum = 0.0;
        for (Out o : outs) {
            if (o.element.equals(element)) {
                sum += o.expected();
            }
        }
        return sum;
    }

    /**
     * The most fissile material (uranium-235 plus plutonium, in dusts) one blast's fallout can return: every hot
     * rubble and every trinitite it can make, at the expected rate. Trinitite is bounded by the same block cap.
     *
     * @param maxBlocks the config's per-blast block cap
     * @return the upper bound, dusts
     */
    public static double maxFissileReturn(int maxBlocks) {
        double hot = maxBlocks * HOT_SHARE;
        double perHot = expected(HOT_RUBBLE, "Uranium235") + expected(HOT_RUBBLE, "Plutonium");
        double perTrin = expected(CRUSHED_TRINITITE, "Uranium235") + expected(CRUSHED_TRINITITE, "Plutonium");
        // A block is hot rubble or trinitite, never both, so the larger rate bounds all of the hot share.
        return hot * Math.max(perHot, perTrin);
    }

    /** @return the fissile material in a nuclear core, dusts */
    public static double coreFissile() {
        return CORE_U235 + CORE_PU;
    }

    /**
     * @param maxBlocks the config's per-blast block cap
     * @return the share of the core a blast's fallout can return at most
     */
    public static double maxFissileShare(int maxBlocks) {
        return maxFissileReturn(maxBlocks) / coreFissile();
    }
}
