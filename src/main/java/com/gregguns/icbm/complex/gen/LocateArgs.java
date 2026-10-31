package com.gregguns.icbm.complex.gen;

import java.util.Locale;

/**
 * The arguments of {@code /gregguns locate}: {@code <kind> [radius_cells] [generate]} lists (and optionally
 * generates), {@code tp <structure_id> <small|medium|big> <cellX> <cellZ>} teleports to a built site (what a click on a
 * row runs), {@code rebuild <cellX> <cellZ>} clears a built silo complex and writes it again. The kind is {@code complex}, a structure id ({@code military_base}...) or {@code any}. Parsing is separate
 * from the command so it is unit tested without a server.
 */
final class LocateArgs {

    enum Mode {
        LIST,
        TP,
        /** {@code rebuild <cellX> <cellZ>}: clear a built silo complex and write it again ({@link ComplexRebuild}). */
        REBUILD
    }

    final Mode mode;
    final int radius;
    final boolean generate;
    final int cellX, cellZ;
    /** The canonical kind of a listing, or the structure id of a teleport. */
    final String kind;
    final com.gregguns.worldgen.SizeClass sizeClass;

    private LocateArgs(Mode mode, String kind, int radius, boolean generate, int cellX, int cellZ,
        com.gregguns.worldgen.SizeClass sizeClass) {
        this.kind = kind;
        this.sizeClass = sizeClass;
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
        if (args.length == 0) throw new IllegalArgumentException("gregguns.locate.usage");
        if (args[0].equalsIgnoreCase("rebuild")) {
            if (args.length != 3) throw new IllegalArgumentException("gregguns.locate.rebuild.usage");
            try {
                return new LocateArgs(
                    Mode.REBUILD,
                    StructureLocator.COMPLEX,
                    0,
                    false,
                    Integer.parseInt(args[1]),
                    Integer.parseInt(args[2]),
                    null);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("gregguns.locate.rebuild.usage");
            }
        }
        if (args[0].equalsIgnoreCase("tp")) {
            if (args.length != 5) throw new IllegalArgumentException("gregguns.locate.usage");
            String id = StructureLocator.kindOf(args[1]);
            com.gregguns.worldgen.SizeClass sc;
            try {
                sc = com.gregguns.worldgen.SizeClass.valueOf(args[2].toUpperCase(Locale.ROOT));
                if (id == null || id.equals(StructureLocator.ANY)) throw new IllegalArgumentException();
                return new LocateArgs(Mode.TP, id, 0, false, Integer.parseInt(args[3]), Integer.parseInt(args[4]), sc);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("gregguns.locate.usage");
            }
        }
        String kind = StructureLocator.kindOf(args[0]);
        if (kind == null) throw new IllegalArgumentException("gregguns.locate.usage");
        int radius = StructureLocator.DEFAULT_RADIUS;
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
            if (radius < 1 || radius > StructureLocator.MAX_RADIUS) {
                throw new IllegalArgumentException("gregguns.locate.badradius");
            }
        }
        return new LocateArgs(Mode.LIST, kind, radius, generate, 0, 0, null);
    }
}
