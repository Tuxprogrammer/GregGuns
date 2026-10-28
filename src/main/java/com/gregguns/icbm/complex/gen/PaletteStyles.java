package com.gregguns.icbm.complex.gen;

import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.Cell;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.ComplexRaster;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.Tunnel;
import com.gregguns.icbm.complex.palette.ComplexPalette;
import com.gregguns.icbm.complex.palette.Furniture;
import com.gregguns.icbm.complex.palette.RoomStyle;
import com.gregguns.icbm.complex.palette.StyleGroup;
import com.gregguns.icbm.worldgen.ActivityLevel;
import com.gregguns.worldgen.layout.ResolvedBlock;

/**
 * The palette's styles for one complex (D240, D235): one {@link RoomStyle} per section, drawn from the plan seed and
 * the
 * section id, so every window agrees. The hub draws from the hub group, the gallery from the gallery group, the other
 * halls from the major or minor hall group, and a room from its kind. Floor and ceiling patterns take interior
 * coordinates: 0 is the first cell inside the section's wall, and the size is the interior's, so a border of width 1
 * lies along the wall. A wall is one block for the whole section. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Palette)
public final class PaletteStyles implements Styles {

    /** The production factory. */
    public static final Factory FACTORY = PaletteStyles::new;

    private final ComplexRaster raster;
    private final ComplexPlan plan;
    private final RoomStyle[] bySection;
    private final String dimensionKey;
    private final ActivityLevel level;

    PaletteStyles(ComplexRaster raster, String dimensionKey, ActivityLevel level) {
        this.raster = raster;
        this.plan = raster.plan();
        this.bySection = new RoomStyle[plan.sectionCount()];
        this.dimensionKey = dimensionKey;
        this.level = level;
    }

    /** @return the seed of a section's style: the plan seed mixed with the section id */
    static long sectionSeed(long planSeed, int section) {
        long h = planSeed ^ (0x9E3779B97F4A7C15L * (section + 1));
        h ^= h >>> 31;
        return h * 0xBF58476D1CE4E5B9L;
    }

    /** @return the style of a section */
    public RoomStyle style(int section) {
        RoomStyle s = bySection[section];
        if (s == null) {
            s = styleOf(plan, section, dimensionKey, level);
            bySection[section] = s;
        }
        return s;
    }

    /** @return the style a section of a plan draws (pure) */
    public static RoomStyle styleOf(ComplexPlan plan, int section, String dimensionKey, ActivityLevel level) {
        long seed = sectionSeed(plan.seed(), section);
        Object o = plan.section(section);
        if (o instanceof Room) {
            Room r = (Room) o;
            return r == plan.hall() ? ComplexPalette.styleFor(StyleGroup.HUB, seed, dimensionKey, level)
                : ComplexPalette.styleFor(r.kind(), seed, dimensionKey, level);
        }
        Tunnel t = (Tunnel) o;
        if (t.gallery()) return ComplexPalette.styleFor(StyleGroup.GALLERY, seed, dimensionKey, level);
        return ComplexPalette.hallStyle(t.major(), seed, dimensionKey, level);
    }

    @Override
    public ResolvedBlock block(byte role, int x, int y, int z) {
        int id = raster.ownerAt(x, z);
        if (id < 0) return null;
        RoomStyle s = style(id);
        Box b = plan.sectionBox(id);
        switch (role) {
            case Cell.WALL:
                return s.wall();
            case Cell.FLOOR:
                return s
                    .floor(inner(x, b.minX, b.sizeX()), inner(z, b.minZ, b.sizeZ()), span(b.sizeX()), span(b.sizeZ()));
            case Cell.CEILING:
                return s.ceiling(
                    inner(x, b.minX, b.sizeX()),
                    inner(z, b.minZ, b.sizeZ()),
                    span(b.sizeX()),
                    span(b.sizeZ()));
            case Cell.LAMP:
                return s.lamp(false);
            case Cell.DOOR_FRAME: {
                ResolvedBlock a = s.accent();
                return a != null ? a : s.wall();
            }
            case Cell.FURN_TABLE:
                return furniture(s, Furniture.TABLE);
            case Cell.FURN_CONSOLE:
                return furniture(s, Furniture.CONSOLE);
            case Cell.FURN_BUNK:
                return furniture(s, Furniture.BUNK);
            case Cell.FURN_SHELF:
                return furniture(s, Furniture.SHELF);
            default:
                return null;
        }
    }

    /**
     * @return the interior coordinate of world-plan coordinate {@code v} in a section starting at {@code min} with
     *         {@code size} cells including both walls: 0 next to the low wall, {@code size - 3} next to the high one.
     *         A floor cell in a wall column (a doorway) clamps to the nearest interior cell.
     */
    static int inner(int v, int min, int size) {
        return Math.max(0, Math.min(span(size) - 1, v - min - 1));
    }

    /** @return the interior size of a section {@code size} cells across, walls included */
    static int span(int size) {
        return Math.max(1, size - 2);
    }

    /** A style without that piece of furniture uses its accent, then its wall: the layout still reads. */
    private static ResolvedBlock furniture(RoomStyle s, Furniture f) {
        ResolvedBlock b = s.furniture(f);
        if (b == null) b = s.accent();
        return b != null ? b : s.wall();
    }

    @Override
    public ResolvedBlock litLamp(int x, int y, int z) {
        int id = raster.ownerAt(x, z);
        return id < 0 ? null : style(id).lamp(true);
    }
}
