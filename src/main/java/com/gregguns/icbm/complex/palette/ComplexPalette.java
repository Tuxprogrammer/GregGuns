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
 * the whole room, drawn once from its style set; the floor and ceiling designs are drawn once too. The room's floor,
 * wall and ceiling are three different blocks (D265): the sets are built that way, and {@link RoomStyle#roles} guards
 * it in any registry.
 *
 * <p>
 * Usage: {@code RoomStyle s = ComplexPalette.styleFor(kind, roomSeed, "moon", level);} then, in the server-side
 * writer, {@code s.floor(x, z, sizeX, sizeZ)} for a block and meta.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md; D240; D244; D265; D266
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
        int wallIndex = rng.nextInt(set.walls.size());
        BlockRef wall = set.walls.get(wallIndex);
        String wallSource = set.id + " wall " + wallIndex;
        int floorIndex = rng.nextInt(set.floors.size());
        int ceilingIndex = rng.nextInt(set.ceilings.size());
        boolean wallLocal = false;
        int floorLocal = -1;

        BlockRef.Entry rock = localRock(dimensionKey, false);
        if (rock != null && group.rugged) {
            BlockRef.Entry brick = localRock(dimensionKey, true);
            // Rolled in a fixed order so the draw count never depends on the dimension. Local stone dresses the wall
            // or the floor, never both: the planet's brick and stone are metas of one block (D265, D266).
            int wallRoll = rng.nextInt(3);
            int floorRoll = rng.nextInt(3);
            if (wallRoll == 0) {
                // The planet's brick, falling back to the set's own wall chain.
                wall = wall.preferring(brick);
                wallSource = dimensionKey + " brick over " + wallSource;
                wallLocal = true;
            } else if (floorRoll < 2) {
                floorLocal = floorRoll;
            }
        }
        boolean worn = level == ActivityLevel.ABANDONED && set.worn != null && rng.nextBoolean();
        // The drawn designs first, then the set's other designs in its order, for the role guard (D265).
        List<RoomStyle.Choice> floors = new ArrayList<>();
        for (int i = 0; i < set.floors.size(); i++) {
            int k = (floorIndex + i) % set.floors.size();
            CellPattern p = set.floors.get(k);
            String source = set.id + " floor " + k + (i == 0 ? "" : " (guard alternate)");
            if (floorLocal >= 0) {
                BlockRef.Entry stone = localRock(dimensionKey, false), brick = localRock(dimensionKey, true);
                p = floorLocal == 0 ? Patterns.localField(p, stone, 2) : Patterns.localChecker(p, stone, brick, 3);
                source += floorLocal == 0 ? " with a " + dimensionKey + " stone field"
                    : " under a " + dimensionKey + " stone and brick checker";
            }
            if (worn) {
                p = Patterns.worn(p, set.worn);
                source += ", worn";
            }
            floors.add(new RoomStyle.Choice(p, source));
        }
        List<RoomStyle.Choice> ceilings = new ArrayList<>();
        for (int i = 0; i < set.ceilings.size(); i++) {
            int k = (ceilingIndex + i) % set.ceilings.size();
            ceilings.add(
                new RoomStyle.Choice(
                    set.ceilings.get(k),
                    set.id + " ceiling " + k + (i == 0 ? "" : " (guard alternate)")));
        }
        return new RoomStyle(
            set.id,
            group,
            wallLocal || floorLocal >= 0,
            wall,
            wallSource,
            floors,
            ceilings,
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
