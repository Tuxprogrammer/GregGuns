package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.icbm.complex.FurniturePiece;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.MarkKind;
import com.gregguns.icbm.worldgen.Placement.Kind;

/**
 * The Vending Machine of a dining hall at its room's state (D355). The marks pass stands the machine's casing in all
 * six of its cells; this pass sets its controller, the middle cell of the column on the right as a player facing the
 * machine sees it (the multiblock's shape puts it there). A staffed, in-service or mothballed room (Charlie and Bravo)
 * gets a working machine: GT forms it on its first structure check, and its trades are the pack's database, so it holds
 * nothing of its own. A gutted room (Alpha) gets a dead one: the controller stands, dark, and the top casing of the
 * other column is knocked out, so the structure never forms until a player puts a casing back. A caved-in room is
 * rubble ({@link FurnitureWear}). Pure; the world side resolves each {@link Kind#VENDING} placement only when the
 * Vending Machine mod is loaded.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Room looks); D355
public final class VendingEgg {

    /** Placement keys: the working controller, the dead controller, and the knocked-out casing. */
    public static final String LIVE = "live", DEAD = "dead", GAP = "gap";

    /** World steps of each hanging direction (0 south, 1 west, 2 north, 3 east). */
    private static final int[] DX = { 0, -1, 0, 1 }, DZ = { 1, 0, -1, 0 };

    private VendingEgg() {}

    /**
     * @param hang the way the machine's front faces, as a hanging direction (0 south, 1 west, 2 north, 3 east)
     * @return the hanging direction of the right hand of a player who faces the machine's front
     */
    static int rightOfViewer(int hang) {
        // The player looks the opposite way, (hang + 2); a quarter turn clockwise from that is (hang + 3).
        return (hang + 3) & 3;
    }

    /**
     * Which of a machine's two columns holds the controller. The machine's shape is {@code cc / c~ / cc} seen from the
     * front, the controller ({@code ~}) in the right column, one step from the other along StructureLib's A axis,
     * which is the viewer's right.
     *
     * @param one   one column, {x, z}
     * @param other the other column, {x, z}
     * @param hang  the way the front faces (0 south, 1 west, 2 north, 3 east)
     * @return the controller's column, or null when the columns are not side by side across the facing
     */
    // Source (the shape and the controller's offset, a 1, b 1, c 0):
    // https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/blocks/MTEVendingMachine.java#L93-L106
    // https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/blocks/MTEVendingMachine.java#L561-L593
    // Source (a front facing north lays A to the west, so A is the right hand of a viewer who faces the front):
    // https://github.com/GTNewHorizons/StructureLib/blob/1.4.45/src/main/java/com/gtnewhorizon/structurelib/alignment/enumerable/ExtendedFacing.java#L187-L206
    public static int[] controllerColumn(int[] one, int[] other, int hang) {
        int r = rightOfViewer(hang);
        if (one[0] + DX[r] == other[0] && one[1] + DZ[r] == other[1]) return other;
        if (other[0] + DX[r] == one[0] && other[1] + DZ[r] == one[1]) return one;
        return null;
    }

    /**
     * The placements of a room's Vending Machine at its state (none for a caved-in room, whose pieces are rubble).
     *
     * @param marks     the room's marks
     * @param state     the room's state
     * @param collapsed whether the room caved in
     * @param out       where the placements go
     */
    static void room(List<Mark> marks, RoomState state, boolean collapsed, List<Placement> out) {
        if (collapsed) return;
        Map<Long, List<Mark>> groups = new LinkedHashMap<Long, List<Mark>>();
        for (Mark m : marks) {
            if (m.kind() != MarkKind.FURNITURE || FurniturePiece.of(m.key()) != FurniturePiece.VENDING_MACHINE)
                continue;
            groups.computeIfAbsent(FurniturePiece.groupOf(m), g -> new ArrayList<Mark>())
                .add(m);
        }
        for (List<Mark> g : groups.values()) machine(g, state, out);
    }

    private static void machine(List<Mark> cells, RoomState state, List<Placement> out) {
        List<int[]> columns = new ArrayList<int[]>();
        int bottom = Integer.MAX_VALUE;
        for (Mark m : cells) {
            bottom = Math.min(bottom, m.y());
            boolean seen = false;
            for (int[] c : columns) seen |= c[0] == m.x() && c[1] == m.z();
            if (!seen) columns.add(new int[] { m.x(), m.z() });
        }
        if (columns.size() != 2) return; // not a whole machine: leave the casings as they stand
        int hang = cells.get(0)
            .facing() & 3;
        int[] ctrl = controllerColumn(columns.get(0), columns.get(1), hang);
        if (ctrl == null) return;
        int[] casing = ctrl == columns.get(0) ? columns.get(1) : columns.get(0);
        int front = (hang + 2) & 3; // a machine's front: 0 north, 1 east, 2 south, 3 west
        boolean gutted = state == RoomState.GUTTED;
        out.add(
            Placement.at(Kind.VENDING, ctrl[0], bottom + 1, ctrl[1])
                .key(gutted ? DEAD : LIVE)
                .front(front)
                .build());
        if (gutted) {
            out.add(
                Placement.at(Kind.VENDING, casing[0], bottom + 2, casing[1])
                    .key(GAP)
                    .build());
        }
    }
}
