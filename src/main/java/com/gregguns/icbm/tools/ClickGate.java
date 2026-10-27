package com.gregguns.icbm.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Collapses the several packets one physical right-click can produce into one action (pure logic, both sides, unit
 * tested). In 1.7.10 the client sends the block-use packet and, when nothing client side reports the click as used,
 * also the use-item-in-air packet; a held click repeats every four ticks. The server therefore sees a block use and
 * an air use (or two block uses) from one click.
 *
 * <p>
 * The server-side guard is the robust fix: it does not depend on how the client routes the click, so it also holds if
 * another mod changes the client path. The alternative, consuming the click client side after the block packet leaves,
 * only works for the client that ran our code.
 */
// Decompiled: PlayerControllerMP.onPlayerRightClick / Minecraft.func_147121_ag (1.7.10) behave as described above;
// hedge: read from the MCP-patched sources in the dev environment, not from a published spec.
public final class ClickGate {

    /**
     * Ticks after a block use during which a repeat of that click counts as the same click (the client repeats at 4).
     */
    public static final int WINDOW_TICKS = 6;

    private static final class Mark {

        final long tick;
        final int dim;
        final int x;
        final int y;
        final int z;

        Mark(long tick, int dim, int x, int y, int z) {
            this.tick = tick;
            this.dim = dim;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private final Map<UUID, Mark> marks = new HashMap<>();

    /**
     * Records a block use the item has acted on (or deliberately consumed).
     *
     * @param tick the world time, ticks
     */
    public synchronized void recordBlockUse(UUID player, long tick, int dim, int x, int y, int z) {
        marks.put(player, new Mark(tick, dim, x, y, z));
    }

    /**
     * @return true when this player used this item on the same block a moment ago: the repeat of one click
     */
    public synchronized boolean isRepeatBlockUse(UUID player, long tick, int dim, int x, int y, int z) {
        Mark m = marks.get(player);
        return m != null && recent(m, tick) && m.dim == dim && m.x == x && m.y == y && m.z == z;
    }

    /**
     * @return true when this player used this item on any block a moment ago, so an air use now is the second packet
     *         of the same click and must be ignored
     */
    public synchronized boolean isAirUseOfBlockClick(UUID player, long tick) {
        Mark m = marks.get(player);
        return m != null && recent(m, tick);
    }

    /** Forgets a player (logout). */
    public synchronized void forget(UUID player) {
        marks.remove(player);
    }

    private static boolean recent(Mark m, long tick) {
        long age = tick - m.tick;
        return age >= 0 && age <= WINDOW_TICKS;
    }
}
