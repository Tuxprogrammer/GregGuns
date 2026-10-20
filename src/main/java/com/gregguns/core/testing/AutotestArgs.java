package com.gregguns.core.testing;

/**
 * Argument parsing and validation for the argument-carrying autotest signals ({@code look <yaw> <pitch>}), kept free of
 * Minecraft types so it is unit-tested. Dev only.
 */
public final class AutotestArgs {

    /** Largest pitch magnitude in degrees; the game clamps the player's pitch to this. */
    public static final float MAX_PITCH = 90f;

    private AutotestArgs() {}

    /**
     * Parses {@code look <yaw> <pitch>} arguments.
     *
     * @param args the arguments after the command word
     * @return {@code {yaw, pitch}} in degrees; yaw is any finite angle, pitch is within {@code -90..90}
     * @throws IllegalArgumentException on a wrong argument count, a non-number, a non-finite value or a pitch out of
     *                                  range
     */
    public static float[] parseLook(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("look needs <yaw> <pitch>, got " + args.length + " arguments");
        }
        float yaw = number("yaw", args[0]);
        float pitch = number("pitch", args[1]);
        if (Math.abs(pitch) > MAX_PITCH) {
            throw new IllegalArgumentException("pitch must be within -90..90, got " + pitch);
        }
        return new float[] { yaw, pitch };
    }

    /**
     * @param args the arguments of a command that takes none ({@code use}, {@code usesneak})
     * @throws IllegalArgumentException if any argument is present
     */
    public static void requireNone(String[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("takes no arguments, got " + args.length);
        }
    }

    private static float number(String what, String text) {
        float value;
        try {
            value = Float.parseFloat(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " is not a number: " + text, e);
        }
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException(what + " must be finite: " + text);
        }
        return value;
    }
}
