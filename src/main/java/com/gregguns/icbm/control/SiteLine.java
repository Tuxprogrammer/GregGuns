package com.gregguns.icbm.control;

/**
 * The Launch Control screen's "Site" line: which silo or pad the block is bound to. The server encodes it into one
 * string sync value and the client turns it into a lang key and its arguments, so the screen never reads the world.
 * A silo is named by its sign ("Silo 2", the generated complex hangs one above each controller) when it has one, and
 * by the coordinates of its launch point (the shaft's center, or the pad's center) always. No Minecraft types, both
 * sides.
 */
// Spec: docs/gregguns-icbm-spec/04-launch-control/launch-control-block.md (GUI)
public final class SiteLine {

    /** What the block is bound to. */
    public enum Kind {
        /** Neither part of a silo nor touching a pad. */
        NONE,
        /** A Galacticraft pad the block touches. */
        PAD,
        /** The silo whose structure the block is part of. */
        SILO
    }

    /** The line of a block bound to nothing. */
    public static final SiteLine NONE = new SiteLine(Kind.NONE, 0, 0, "");

    private static final String SEP = "|";
    /** Longest sign name the line shows; a sign line holds 15 characters. */
    static final int MAX_NAME = 15;

    private final Kind kind;
    private final int x;
    private final int z;
    private final String name;

    /**
     * @param kind what the block is bound to
     * @param x    X of the launch point
     * @param z    Z of the launch point
     * @param name the silo's sign name, or empty
     */
    public SiteLine(Kind kind, int x, int z, String name) {
        this.kind = kind == null ? Kind.NONE : kind;
        this.x = x;
        this.z = z;
        this.name = clean(name);
    }

    private static String clean(String name) {
        if (name == null) {
            return "";
        }
        // Formatting codes from a sign must not recolor the screen.
        String s = name.replaceAll("§.?", "")
            .trim();
        return s.length() > MAX_NAME ? s.substring(0, MAX_NAME) : s;
    }

    /** @return what the block is bound to */
    public Kind kind() {
        return kind;
    }

    /** @return the silo's sign name, or empty */
    public String name() {
        return name;
    }

    /** @return X of the launch point */
    public int x() {
        return x;
    }

    /** @return Z of the launch point */
    public int z() {
        return z;
    }

    /** @return the sync form: kind, x, z and the name, the name last so a separator in it is harmless */
    public String encode() {
        return kind.name() + SEP + x + SEP + z + SEP + name;
    }

    /**
     * @param s an {@link #encode()} result, or anything else
     * @return the line; {@link #NONE} for anything that does not parse
     */
    public static SiteLine decode(String s) {
        if (s == null) {
            return NONE;
        }
        String[] p = s.split("\\|", 4);
        if (p.length < 4) {
            return NONE;
        }
        try {
            return new SiteLine(Kind.valueOf(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), p[3]);
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

    /** @return the lang key of the line */
    public String langKey() {
        switch (kind) {
            case PAD:
                return "gregguns.icbm.control.gui.site.pad";
            case SILO:
                return name.isEmpty() ? "gregguns.icbm.control.gui.site.silo"
                    : "gregguns.icbm.control.gui.site.silo_named";
            default:
                return "gregguns.icbm.control.gui.site.none";
        }
    }

    /** @return the lang key's arguments: the name first when the key has one, then X and Z */
    public Object[] args() {
        switch (kind) {
            case PAD:
                return new Object[] { x, z };
            case SILO:
                return name.isEmpty() ? new Object[] { x, z } : new Object[] { name, x, z };
            default:
                return new Object[0];
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SiteLine)) {
            return false;
        }
        SiteLine l = (SiteLine) o;
        return kind == l.kind && x == l.x && z == l.z && name.equals(l.name);
    }

    @Override
    public int hashCode() {
        return ((kind.hashCode() * 31 + x) * 31 + z) * 31 + name.hashCode();
    }

    @Override
    public String toString() {
        return encode();
    }
}
