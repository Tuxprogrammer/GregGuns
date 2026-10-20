package com.gregguns.core.command;

import java.util.Arrays;

/**
 * The parsed arguments of {@code /gregguns build <x> <y> <z> [count] [bench]}: the controller's block position and the
 * size
 * (bays or mounts) to build, where 0 means the machine's default. Pure text handling, so it is unit-tested.
 */
public final class BuildArgs {

    /** Largest accepted size; the machine clamps it to its own range. */
    public static final int MAX_COUNT = 64;
    /** The optional last word that adds the Ammunition Works' filling bench. */
    public static final String BENCH_WORD = "bench";

    public final int x;
    public final int y;
    public final int z;
    /** The requested bays or mounts, or 0 for the default. */
    public final int count;
    /** Whether the optional {@code bench} word was given (the Ammunition Works' filling bench). */
    public final boolean bench;

    private BuildArgs(int x, int y, int z, int count, boolean bench) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.count = count;
        this.bench = bench;
    }

    /**
     * @param args the arguments after the subcommand word
     * @return the parsed arguments
     * @throws IllegalArgumentException on a wrong argument count, a non-integer, or a count outside {@code 1..64}
     */
    public static BuildArgs parse(String[] args) {
        boolean bench = args.length > 3 && BENCH_WORD.equalsIgnoreCase(args[args.length - 1]);
        if (bench) {
            args = Arrays.copyOf(args, args.length - 1);
        }
        if (args.length != 3 && args.length != 4) {
            throw new IllegalArgumentException("usage: /gregguns build <x> <y> <z> [count] [bench]");
        }
        int x = integer("x", args[0]);
        int y = integer("y", args[1]);
        int z = integer("z", args[2]);
        int count = 0;
        if (args.length == 4) {
            count = integer("count", args[3]);
            if (count < 1 || count > MAX_COUNT) {
                throw new IllegalArgumentException("count must be within 1.." + MAX_COUNT + ", got " + count);
            }
        }
        return new BuildArgs(x, y, z, count, bench);
    }

    private static int integer(String what, String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " is not an integer: " + text, e);
        }
    }
}
