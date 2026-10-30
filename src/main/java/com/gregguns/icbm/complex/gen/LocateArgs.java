package com.gregguns.icbm.complex.gen;

import java.util.Locale;

/**
 * The arguments of {@code /gregguns locate}: {@code complex [radius_cells] [generate]} lists (and optionally
 * generates), {@code complex tp <cellX> <cellZ>} teleports to a built complex. Parsing is separate from the command so
 * it is unit tested without a server.
 */
final class LocateArgs {

    enum Mode {
        LIST,
        TP
    }

    final Mode mode;
    final int radius;
    final boolean generate;
    final int cellX, cellZ;

    private LocateArgs(Mode mode, int radius, boolean generate, int cellX, int cellZ) {
        this.mode = mode;
        this.radius = radius;
        this.generate = generate;
        this.cellX = cellX;
        this.cellZ = cellZ;
    }

    /**
     * @param args the words after {@code locate}
     * @return the parsed arguments
     * @throws IllegalArgumentException whose message is the lang key of the complaint
     */
    static LocateArgs parse(String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("complex")) {
            throw new IllegalArgumentException("gregguns.locate.usage");
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("tp")) {
            if (args.length != 4) throw new IllegalArgumentException("gregguns.locate.usage");
            try {
                return new LocateArgs(Mode.TP, 0, false, Integer.parseInt(args[2]), Integer.parseInt(args[3]));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("gregguns.locate.usage");
            }
        }
        int radius = ComplexLocator.DEFAULT_RADIUS;
        boolean generate = false;
        for (int i = 1; i < args.length; i++) {
            String a = args[i].toLowerCase(Locale.ROOT);
            if (a.equals("generate")) {
                generate = true;
                continue;
            }
            try {
                radius = Integer.parseInt(a);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("gregguns.locate.usage");
            }
            if (radius < 1 || radius > ComplexLocator.MAX_RADIUS) {
                throw new IllegalArgumentException("gregguns.locate.badradius");
            }
        }
        return new LocateArgs(Mode.LIST, radius, generate, 0, 0);
    }
}
