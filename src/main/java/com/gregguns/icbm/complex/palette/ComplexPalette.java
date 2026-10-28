package com.gregguns.icbm.complex.palette;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.worldgen.ActivityLevel;

/**
 * Picks the look of a room (D240). {@link #styleFor} is pure and deterministic: the same kind, seed, dimension and
 * activity level always give the same style, and it touches no world and no registry. A room's wall is one block for
 * the whole room, drawn once from its style set; the floor and ceiling designs are drawn once too.
 *
 * <p>
 * Usage: {@code RoomStyle s = ComplexPalette.styleFor(kind, roomSeed, "moon", level);} then, in the server-side
 * writer, {@code s.floor(x, z, sizeX, sizeZ)} for a block and meta.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md; D240; D244
public final class ComplexPalette {

    private static final List<StyleSet> SETS = PaletteSets.build();

    private ComplexPalette() {}

    /** @return every style set */
    public static List<StyleSet> sets() {
        return SETS;
    }

    /**
     * @param kind         the room's kind
     * @param roomSeed     the room's own seed (any long; use the plan seed mixed with the room index)
     * @param dimensionKey a DimensionKey ("Overworld", "moon", "mars" and so on); null reads as the Overworld
     * @param level        the complex's activity level
     * @return the room's style
     */
    public static RoomStyle styleFor(RoomKind kind, long roomSeed, String dimensionKey, ActivityLevel level) {
        return styleFor(StyleGroup.of(kind), roomSeed, dimensionKey, level);
    }

    /**
     * @param major true for a major hall, false for a minor hallway
     * @return the style of a hall
     */
    public static RoomStyle hallStyle(boolean major, long seed, String dimensionKey, ActivityLevel level) {
        return styleFor(major ? StyleGroup.HALL_MAJOR : StyleGroup.HALL_MINOR, seed, dimensionKey, level);
    }

    /** @return the style of a room of a group (the hub, a hall, or any room); see {@link #styleFor} */
    public static RoomStyle styleFor(StyleGroup group, long roomSeed, String dimensionKey, ActivityLevel level) {
        Random rng = new Random(mix(roomSeed, group.ordinal()));
        List<StyleSet> candidates = new ArrayList<>();
        for (StyleSet s : SETS) {
            if (s.groups.contains(group)) {
                candidates.add(s);
            }
        }
        StyleSet set = candidates.get(rng.nextInt(candidates.size()));
        BlockRef wall = set.walls.get(rng.nextInt(set.walls.size()));
        CellPattern floor = set.floors.get(rng.nextInt(set.floors.size()));
        int ceilingIndex = rng.nextInt(set.ceilings.size());
        CellPattern ceiling = set.ceilings.get(ceilingIndex);
        // The other designs in the set's order, starting after the drawn one: a cell whose drawn ceiling shows the
        // wall's block takes the next design's block there (D261).
        List<CellPattern> ceilingAlternates = new ArrayList<>();
        for (int i = 1; i < set.ceilings.size(); i++) {
            ceilingAlternates.add(set.ceilings.get((ceilingIndex + i) % set.ceilings.size()));
        }
        boolean wallLocal = false;
        boolean floorLocal = false;

        BlockRef.Entry rock = localRock(dimensionKey, false);
        if (rock != null && group.rugged) {
            BlockRef.Entry brick = localRock(dimensionKey, true);
            // Rolled in a fixed order so the draw count never depends on the dimension.
            int wallRoll = rng.nextInt(3);
            int floorRoll = rng.nextInt(3);
            if (wallRoll == 0) {
                wall = BlockRef.of(brick, BlockRef.v("stonebrick", 0))
                    .preferring(brick);
                wallLocal = true;
            }
            if (floorRoll == 0) {
                BlockRef field = BlockRef.of(rock, BlockRef.v("stone", 0));
                floor = Patterns.borderField(wall, field, 2);
                floorLocal = true;
            } else if (floorRoll == 1) {
                BlockRef field = BlockRef.of(rock, BlockRef.v("stone", 0));
                floor = Patterns.checker(field, BlockRef.of(brick, BlockRef.v("stonebrick", 0)), 3);
                floorLocal = true;
            }
        }
        if (level == ActivityLevel.ABANDONED && set.worn != null && rng.nextBoolean()) {
            floor = Patterns.worn(floor, set.worn);
        }
        return new RoomStyle(
            set.id,
            group,
            wallLocal || floorLocal,
            wall,
            floor,
            ceiling,
            ceilingAlternates,
            set.lampLit,
            set.lampUnlit,
            set.accent,
            set.furniture);
    }

    /** @return Moon or Mars rock (or their dungeon brick) for those dimensions; null elsewhere */
    static BlockRef.Entry localRock(String dimensionKey, boolean brick) {
        if (dimensionKey == null) {
            return null;
        }
        switch (dimensionKey.toLowerCase(Locale.ROOT)) {
            case "moon":
                return brick ? PaletteSets.MOON_BRICK : PaletteSets.MOON_STONE;
            case "mars":
                return brick ? PaletteSets.MARS_BRICK : PaletteSets.MARS_STONE;
            default:
                return null;
        }
    }

    private static long mix(long seed, int salt) {
        long h = seed ^ (salt * 0x9E3779B97F4A7C15L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return h;
    }
}
