package com.gregguns.icbm.complex;

/**
 * An opening between two sections that touch. Neighboring sections keep their own walls, so a door cuts both wall
 * layers: the cells {@code x0..x1}, {@code z0..z1} (inclusive) from {@code y0} to {@code y1} (inclusive).
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Doors)
public final class Door {

    /** What the opening is. */
    public enum Kind {
        /** A room's doorway, framed by the style's accent. */
        DOOR,
        /** A wide opening between two halls, or a hall and the hub, with a lintel. */
        PORTAL,
        /** The niche in a gallery wall in front of a silo's controller. */
        WALKWAY,
        /** A row of clear glass blocks (Launch Control over the gallery); no way through. */
        WINDOW
    }

    public final Kind kind;
    public final int x0, x1, z0, z1, y0, y1;
    /** The two sections joined (ids from {@link ComplexPlan#section}); b is -1 for a silo walkway. */
    public final int a, b;
    /** Side of section {@code a} the door is in: 0 north (-Z), 1 east (+X), 2 south (+Z), 3 west (-X). */
    public final int side;

    Door(Kind kind, int x0, int x1, int z0, int z1, int y0, int y1, int a, int b, int side) {
        this.kind = kind;
        this.x0 = Math.min(x0, x1);
        this.x1 = Math.max(x0, x1);
        this.z0 = Math.min(z0, z1);
        this.z1 = Math.max(z0, z1);
        this.y0 = y0;
        this.y1 = y1;
        this.a = a;
        this.b = b;
        this.side = side;
    }

    /** @return whether the column lies in the opening (through the wall) */
    public boolean containsXZ(int x, int z) {
        return x >= x0 && x <= x1 && z >= z0 && z <= z1;
    }

    /** @return whether the door opens onto the section */
    public boolean joins(int section) {
        return a == section || b == section;
    }

    /** @return true when the door runs through an east-west wall (its cells vary in x) */
    public boolean inXWall() {
        return side == 0 || side == 2;
    }

    @Override
    public String toString() {
        return kind + "[" + x0 + ".." + x1 + "," + z0 + ".." + z1 + ",y" + y0 + ".." + y1 + "]" + a + "-" + b;
    }
}
