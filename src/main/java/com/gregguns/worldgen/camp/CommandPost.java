package com.gregguns.worldgen.camp;

import com.gregguns.worldgen.FlagPos;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.layout.PaletteKeys;
import com.gregguns.worldgen.layout.ResolvedBlock;

/**
 * The command pad around a Military Base's flag, in its faction's color (D353). The flag leaf holds no building, so the
 * pad is the one thing there besides the flag:
 * <ul>
 * <li>a 5 by 5 pad sunk into the ground under and around the flag: a ring of wool with stained clay inside, blue for a
 * garrison and red for Dictator Dave's base;</li>
 * <li>on a red base, four standards at the pad's corners, 3 out from the flag each way: a fence post two high with a
 * red
 * wool block on top;</li>
 * <li>the commander's post: the pad cell 2 east of the flag (camp-local, so it turns with the base), where Dave
 * stands.</li>
 * </ul>
 * Every cell is laid out in camp-local coordinates and clipped to the flag leaf, so a base's pad is the same at every
 * rotation; every write stays in its populate window. Never reads the world. Pure apart from the sink.
 *
 * <p>
 * Usage: {@code CommandPost.write(sink, camp, window, ground, faction, keys)} from {@link CampPlacer#write}; the flag
 * step reads {@link #post} for the commander.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (Military Base: faction and command post), D353
public final class CommandPost {

    /** The commander's NPC registry name (matched through {@code NpcMob.byName}, like the base's soldiers). */
    public static final String COMMANDER = "DictatorDave";

    /** Half-size of the pad: the pad spans the flag column plus this many columns each way. */
    public static final int PAD_REACH = 2;
    /** Distance of the red standards from the flag, each axis. */
    public static final int STANDARD_REACH = 3;
    /** Height of a standard's fence post; the wool sits on top. */
    public static final int STANDARD_POLE = 2;
    /** The commander's post, camp-local offset from the flag along x. */
    public static final int POST_DX = 2;
    /** How far the commander strays from his post when no one is in sight. */
    public static final int POST_RADIUS = 6;

    private CommandPost() {}

    /**
     * Writes this window's part of the pad and, on a red base, of the standards.
     *
     * @param sink    the window-clipped sink
     * @param camp    the placed camp
     * @param window  the populating window
     * @param ground  the ground of each column (the first air block)
     * @param faction the base's faction
     * @param keys    the palette
     * @return blocks written
     */
    public static int write(WindowedSink sink, PlacedCamp camp, PopulateWindow window, CampPlacer.GroundAt ground,
        CampFaction faction, PaletteKeys keys) {
        ResolvedBlock wool = colored(keys.get("minecraft:wool"), faction.woolMeta());
        ResolvedBlock clay = colored(keys.get("minecraft:stained_hardened_clay"), faction.woolMeta());
        ResolvedBlock fence = keys.get("minecraft:fence");
        CampPlan plan = camp.plan();
        CampLeaf leaf = plan.flagLeaf();
        int fx = plan.flagX();
        int fz = plan.flagZ();
        int n = 0;
        for (int dx = -PAD_REACH; dx <= PAD_REACH; dx++) {
            for (int dz = -PAD_REACH; dz <= PAD_REACH; dz++) {
                int lx = fx + dx;
                int lz = fz + dz;
                if (!inLeaf(leaf, lx, lz)) {
                    continue;
                }
                int wx = camp.worldX(lx, lz);
                int wz = camp.worldZ(lx, lz);
                if (!window.contains(wx, wz)) {
                    continue;
                }
                boolean ring = Math.max(Math.abs(dx), Math.abs(dz)) == PAD_REACH;
                n += put(sink, ring ? wool : clay, wx, ground.groundY(wx, wz) - 1, wz);
            }
        }
        if (faction != CampFaction.RED) {
            return n;
        }
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int lx = fx + sx * STANDARD_REACH;
                int lz = fz + sz * STANDARD_REACH;
                if (!inLeaf(leaf, lx, lz)) {
                    continue;
                }
                int wx = camp.worldX(lx, lz);
                int wz = camp.worldZ(lx, lz);
                if (!window.contains(wx, wz)) {
                    continue;
                }
                int g = ground.groundY(wx, wz);
                for (int h = 0; h < STANDARD_POLE; h++) {
                    n += put(sink, fence, wx, g + h, wz);
                }
                n += put(sink, wool, wx, g + STANDARD_POLE, wz);
            }
        }
        return n;
    }

    /**
     * The commander's post as an offset from the flag block, in world axes (it turns with the base).
     *
     * @param camp the placed camp
     * @return {@code {dx, dy, dz}} from the flag
     */
    public static int[] post(PlacedCamp camp) {
        CampPlan plan = camp.plan();
        FlagPos flag = camp.flagPos();
        int lx = plan.flagX() + POST_DX;
        int lz = plan.flagZ();
        return new int[] { camp.worldX(lx, lz) - flag.x, 0, camp.worldZ(lx, lz) - flag.z };
    }

    private static boolean inLeaf(CampLeaf leaf, int lx, int lz) {
        return lx >= leaf.x && lx < leaf.x + leaf.w && lz >= leaf.z && lz < leaf.z + leaf.d;
    }

    private static ResolvedBlock colored(ResolvedBlock base, int meta) {
        return base == null ? null : new ResolvedBlock(base.block, meta, base.fallback);
    }

    private static int put(WindowedSink sink, ResolvedBlock rb, int x, int y, int z) {
        return rb != null && sink.setBlock(x, y, z, rb.block, rb.meta) ? 1 : 0;
    }
}
