package com.gregguns.worldgen.camp;

import java.util.Random;

import com.gregguns.core.config.WorldgenConfig;

/**
 * Which side holds a camp: the ordinary blue garrison, or Dictator Dave's red regime (D353). The faction is a pure
 * function of the camp's plan seed and the red chance, so every populate window of a base agrees on it with no saved
 * state. It picks the flag cloth (the Camp Flag block's metadata), the wool and clay of the command pad around the
 * flag, the red standards, and whether a commander holds the post. Pure.
 *
 * <p>
 * Usage: {@code CampFaction.of(plan.seed())} in world generation; {@code CampFaction.of(seed, percent)} in tests.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (Military Base: faction and command post), D353
public enum CampFaction {

    /** The ordinary garrison: blue cloth and pad, no commander. */
    BLUE(0, 11),
    /** Dictator Dave's base: red cloth and pad, four red standards and Dave at his post. */
    RED(1, 14);

    /** Salt that keeps the faction roll apart from the plan's size and subdivision rolls. */
    private static final long SALT = 0x52454442415345L;

    private final int flagMeta;
    private final int woolMeta;

    CampFaction(int flagMeta, int woolMeta) {
        this.flagMeta = flagMeta;
        this.woolMeta = woolMeta;
    }

    /** @return the Camp Flag block's metadata for this faction (the renderer tints the cloth by it) */
    public int flagMeta() {
        return flagMeta;
    }

    /** @return the wool and stained clay color of the command pad (vanilla dye order: 11 blue, 14 red) */
    public int woolMeta() {
        return woolMeta;
    }

    /** @return true when a commander (Dictator Dave) holds the post */
    public boolean hasCommander() {
        return this == RED;
    }

    /**
     * @param flagMeta a Camp Flag block's metadata
     * @return its faction; unknown values are blue
     */
    public static CampFaction byFlagMeta(int flagMeta) {
        return flagMeta == RED.flagMeta ? RED : BLUE;
    }

    /**
     * @param seed       the camp plan's seed
     * @param redPercent chance of a red base, 0 to 100
     * @return the faction
     */
    public static CampFaction of(long seed, int redPercent) {
        if (redPercent <= 0) {
            return BLUE;
        }
        if (redPercent >= 100) {
            return RED;
        }
        return new Random(seed ^ SALT).nextInt(100) < redPercent ? RED : BLUE;
    }

    /**
     * @param seed the camp plan's seed
     * @return the faction at the configured chance ({@code worldgen.redBaseChanceMilitaryBase})
     */
    public static CampFaction of(long seed) {
        return of(seed, WorldgenConfig.redBaseChanceMilitaryBase);
    }
}
