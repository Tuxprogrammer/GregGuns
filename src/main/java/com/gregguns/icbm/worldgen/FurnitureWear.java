package com.gregguns.icbm.worldgen;

import java.util.List;

import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.worldgen.Placement.Kind;

/**
 * What the activity level leaves of a room's furniture (D36, D259). An operational complex keeps all of it. In a
 * partly abandoned or abandoned one, a share of the pieces is gone (whole pieces: every layer of a column goes
 * together, so no typewriter floats over a missing desk). A room that caved in has its furniture crushed into rubble.
 * Water basins are part of the room, not furniture, and stay. Pure.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Furnishing); D259
public final class FurnitureWear {

    /** The share of pieces gone in a partly abandoned complex. */
    public static final double PARTLY_LOST = 0.15;
    /** The share of pieces gone in an abandoned complex. */
    public static final double ABANDONED_LOST = 0.35;

    private static final long S_WEAR = 0xF0A2;

    private FurnitureWear() {}

    /** @return the share of furniture gone at a level */
    public static double lostShare(ActivityLevel level) {
        return level == ActivityLevel.ABANDONED ? ABANDONED_LOST
            : level == ActivityLevel.PARTLY_ABANDONED ? PARTLY_LOST : 0;
    }

    /**
     * @param m         a mark; anything but a furniture mark is ignored
     * @param collapsed whether the mark's room caved in
     * @param seed      the plan seed
     * @param out       where a removal (air) or a crushed piece (rubble) goes
     */
    public static void wear(Mark m, boolean collapsed, ActivityLevel level, long seed, List<Placement> out) {
        if (m.kind() != MarkKind.FURNITURE || "BASIN".equals(m.key())) return;
        if (collapsed) {
            out.add(
                Placement.at(Kind.RUBBLE, m.x(), m.y(), m.z())
                    .build());
            return;
        }
        // One roll per column, so a whole piece goes or stays.
        if (Mix.unit(Mix.of(seed, S_WEAR, m.x() * 4096L + m.z(), 0)) < lostShare(level)) {
            out.add(
                Placement.at(Kind.AIR, m.x(), m.y(), m.z())
                    .build());
        }
    }
}
