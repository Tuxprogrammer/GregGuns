package com.gregguns.icbm.complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Grows the geometry of one complex (D229, D230): the hub, the major halls leaving it (most bending once or twice), the
 * silo
 * gallery along one or two of them with Launch Control at its far end, then rooms and minor hallways attached
 * side to side, rooms branching off rooms, until the room count is reached. The rooms then grow into the rock around
 * them until the built footprint covers at least three quarters of the complex. Neighboring sections touch but never
 * share a wall, so each keeps its own one-block wall; a door cuts both.
 *
 * <p>
 * Pure and deterministic: one {@link Random} from the plan seed, lists only (no hash order). Package-private; the entry
 * point is {@link ComplexLayout#fromSeed}.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Shape, Growth)
final class ComplexGrower {

    static final int S = ComplexLayout.SIZE;
    static final int HUB = 0, MAJOR = 1, MINOR = 2, ROOM = 3;
    /** Owner value of a silo footprint in the occupancy map. */
    private static final short SILO = -1;
    /** Owner value of the silo bands kept free beside the gallery hall while the other halls grow. */
    private static final short RESERVED = -2;

    /** A section being grown: x1 and z1 exclusive, like {@link Box}. */
    static final class Node {

        final int id, type;
        final RoomKind kind;
        int x0, z0, x1, z1, h;
        final int parent;
        /** The side of the parent this node hangs off (its outward direction), -1 for the hub. */
        final int dir;
        final int depth;
        boolean gallery;

        Node(int id, int type, RoomKind kind, int[] b, int h, int parent, int dir, int depth) {
            this.id = id;
            this.type = type;
            this.kind = kind;
            this.x0 = b[0];
            this.z0 = b[1];
            this.x1 = b[2];
            this.z1 = b[3];
            this.h = h;
            this.parent = parent;
            this.dir = dir;
            this.depth = depth;
        }

        Box box() {
            return new Box(x0, 0, z0, x1, h, z1);
        }

        boolean hall() {
            return type != ROOM;
        }

        boolean alongX() {
            return dir == 1 || dir == 3;
        }

        /** Lateral range of a side: x for the north and south sides, z for east and west. */
        int latLo(int side) {
            return (side & 1) == 0 ? x0 : z0;
        }

        int latHi(int side) {
            return (side & 1) == 0 ? x1 : z1;
        }

        int length() {
            return alongX() ? x1 - x0 : z1 - z0;
        }

        int area() {
            return (x1 - x0) * (z1 - z0);
        }
    }

    /** A side of a section that new sections can attach to, with a cursor for halls that take many. */
    private static final class Site {

        final Node n;
        final int side;
        int cursor;
        boolean done;

        Site(Node n, int side, int cursor) {
            this.n = n;
            this.side = side;
            this.cursor = cursor;
        }
    }

    final Random rnd;
    private final short[] own = new short[S * S];
    final List<Node> nodes = new ArrayList<Node>();
    final List<Door> doors = new ArrayList<Door>();
    final List<SiloSlot> silos = new ArrayList<SiloSlot>();
    private final List<Site> sites = new ArrayList<Site>();
    int[] escape;
    final List<int[]> radars = new ArrayList<int[]>();
    /** Junction cells (complex-local x, z, owning hall id) where a guard post mark goes (D233). */
    final List<int[]> branchPoints = new ArrayList<int[]>();

    ComplexGrower(long seed) {
        this.rnd = new Random(seed);
    }

    // --- occupancy -----------------------------------------------------------------------------------------------

    boolean free(int x0, int z0, int x1, int z1) {
        if (x0 < 0 || z0 < 0 || x1 > S || z1 > S || x1 <= x0 || z1 <= z0) return false;
        for (int x = x0; x < x1; x++) for (int z = z0; z < z1; z++) if (own[x * S + z] != 0) return false;
        return true;
    }

    private void claim(int x0, int z0, int x1, int z1, short v) {
        for (int x = x0; x < x1; x++) for (int z = z0; z < z1; z++) own[x * S + z] = v;
    }

    /** @return how many columns of the complex are built (a section or a silo stands in them) */
    int coveredColumns() {
        int n = 0;
        for (short v : own) if (v != 0) n++;
        return n;
    }

    /** Box adjacent to a side of {@code p}: {@code lat0} and {@code width} along the side, {@code depth} outward. */
    static int[] adjacent(Node p, int side, int lat0, int width, int depth) {
        switch (side) {
            case 0:
                return new int[] { lat0, p.z0 - depth, lat0 + width, p.z0 };
            case 2:
                return new int[] { lat0, p.z1, lat0 + width, p.z1 + depth };
            case 1:
                return new int[] { p.x1, lat0, p.x1 + depth, lat0 + width };
            default:
                return new int[] { p.x0 - depth, lat0, p.x0, lat0 + width };
        }
    }

    private Node add(int type, RoomKind kind, int[] b, int h, Node parent, int dir) {
        Node n = new Node(
            nodes.size(),
            type,
            kind,
            b,
            h,
            parent == null ? -1 : parent.id,
            dir,
            parent == null ? 0 : type == ROOM ? parent.depth + 1 : parent.depth);
        nodes.add(n);
        claim(b[0], b[1], b[2], b[3], (short) (n.id + 1));
        return n;
    }

    /**
     * Cuts an opening between {@code p} and the node {@code c} touching its {@code side}, centered on where their
     * interiors overlap.
     *
     * @param width lateral cells (0: the whole overlap)
     * @param top   the highest layer cut
     * @return the door, or null when the overlap is too short
     */
    private Door door(Door.Kind kind, Node p, int side, Node c, int width, int top, boolean centered) {
        int lo = Math.max(p.latLo(side), c.latLo(side)) + 1;
        int hi = Math.min(p.latHi(side), c.latHi(side)) - 2;
        int span = hi - lo + 1;
        if (width == 0) width = span;
        if (span < width) return null;
        int a0 = centered ? lo + (span - width) / 2 : lo + rnd.nextInt(span - width + 1);
        int a1 = a0 + width - 1;
        Door d;
        switch (side) {
            case 0:
                d = new Door(kind, a0, a1, p.z0 - 1, p.z0, 1, top, p.id, c.id, 0);
                break;
            case 2:
                d = new Door(kind, a0, a1, p.z1 - 1, p.z1, 1, top, p.id, c.id, 2);
                break;
            case 1:
                d = new Door(kind, p.x1 - 1, p.x1, a0, a1, 1, top, p.id, c.id, 1);
                break;
            default:
                d = new Door(kind, p.x0 - 1, p.x0, a0, a1, 1, top, p.id, c.id, 3);
                break;
        }
        doors.add(d);
        return d;
    }

    private static int portalTop(Node a, Node b) {
        int h = Math.min(a.h, b.h);
        return h <= ComplexLayout.MINOR_HEIGHT ? h - 2 : h - 3;
    }

    // --- the whole growth ----------------------------------------------------------------------------------------

    void grow() {
        Node hub = hub();
        List<Node> majors = majorHalls(hub);
        List<Node> gallery = gallery(majors);
        launchControl(gallery.get(0));
        decon(hub, majors);
        int target = 35 + rnd.nextInt(6);
        int posts = 3 + rnd.nextInt(3);
        // A guard post beside each major hall's mouth at the hub first (a branch point every complex has).
        int postsAtMouths = 0;
        for (Node m : majors) {
            if (postsAtMouths < posts - 1 && m.parent == 0 && guardPost(hub, m.dir, m)) postsAtMouths++;
        }
        for (Node n : nodes) addSites(n);
        // Missile assembly goes beside the gallery first, so it stands by the silos.
        List<RoomKind> queue = new ArrayList<RoomKind>();
        queue.add(RoomKind.MISSILE_ASSEMBLY);
        List<RoomKind> pool = new ArrayList<RoomKind>();
        List<RoomKind> mandatory = new ArrayList<RoomKind>();
        for (RoomKind k : RoomKind.values()) {
            if (k == RoomKind.ASSEMBLY_BAY || k == RoomKind.LAUNCH_CONTROL
                || k == RoomKind.MISSILE_ASSEMBLY
                || k.small()) continue;
            if (k.mandatory) mandatory.add(k);
            else pool.add(k);
        }
        Collections.shuffle(pool, rnd);
        // Rooms so far (posts at the mouths included), the assembly floor, the guard posts still to come.
        int fixed = roomCount() + 1 + posts - postsAtMouths;
        List<RoomKind> rest = new ArrayList<RoomKind>(mandatory);
        for (int i = 0; rest.size() < target - fixed; i++) rest.add(pool.get(i % pool.size()));
        Collections.shuffle(rest, rnd);
        queue.addAll(rest);
        growRooms(queue, gallery.get(0), posts - postsAtMouths, target);
        // Rooms grow to their usual limit; a plan still short of the coverage target lets them grow further.
        inflate();
        for (int step = 0; step < 6
            && coveredColumns() * 100 < (ComplexLayout.MIN_COVERAGE_PERCENT + 3) * S * S; step++) {
            capPercent += 20;
            capLateral += 2;
            capDepth += 2;
            inflate();
        }
        loops();
        escape();
        radars();
    }

    /** @return the guard-post rooms placed so far */
    int posts() {
        int n = 0;
        for (Node x : nodes) if (x.type == ROOM && x.kind == RoomKind.GUARD_POST) n++;
        return n;
    }

    int roomCount() {
        int n = 0;
        for (Node x : nodes) if (x.type == ROOM) n++;
        return n;
    }

    private Node hub() {
        boolean wideX = rnd.nextBoolean();
        int sx = wideX ? ComplexLayout.HUB_LONG : ComplexLayout.HUB_SHORT;
        int sz = wideX ? ComplexLayout.HUB_SHORT : ComplexLayout.HUB_LONG;
        int cx = S / 2 + rnd.nextInt(13) - 6, cz = S / 2 + rnd.nextInt(13) - 6;
        return add(
            HUB,
            RoomKind.ASSEMBLY_BAY,
            new int[] { cx - sx / 2, cz - sz / 2, cx - sx / 2 + sx, cz - sz / 2 + sz },
            ComplexLayout.HUB_HEIGHT,
            null,
            -1);
    }

    /** Room left beyond a major hall's far end, for the rooms that cap it. */
    private static final int END_ROOM = 20;

    /** Distance from a node's side to the complex edge. */
    private static int room(Node n, int side) {
        switch (side) {
            case 0:
                return n.z0;
            case 2:
                return S - n.z1;
            case 1:
                return S - n.x1;
            default:
                return n.x0;
        }
    }

    private List<Node> majorHalls(Node hub) {
        List<Integer> dirs = new ArrayList<Integer>();
        for (int d = 0; d < 4; d++) dirs.add(d);
        Collections.shuffle(dirs, rnd);
        int k = rnd.nextInt(100) < 75 ? 4 : 3;
        List<Node> out = new ArrayList<Node>();
        int w = ComplexLayout.MAJOR_WIDTH;
        for (int i = 0; i < k; i++) {
            int side = dirs.get(i);
            int lo = hub.latLo(side), hi = hub.latHi(side);
            int maxLen = room(hub, side) - END_ROOM;
            // The first hall is long enough to carry the silo gallery.
            int len = i == 0 ? Math.min(maxLen, 50 + rnd.nextInt(13)) : Math.min(maxLen, 36 + rnd.nextInt(36));
            int lat0 = lo + 3 + rnd.nextInt(Math.max(1, hi - lo - w - 6 + 1));
            int[] b = adjacent(hub, side, lat0, w, len);
            if (!free(b[0], b[1], b[2], b[3])) continue;
            Node seg = add(MAJOR, null, b, ComplexLayout.MAJOR_HEIGHT, hub, side);
            branch(hub, side, door(Door.Kind.PORTAL, hub, side, seg, 0, portalTop(hub, seg), true));
            out.add(seg);
            // The gallery hall stays straight; most others bend at their far end, some twice, never back toward the
            // hub, so the halls wind out toward the corners.
            if (i == 0) {
                reserveSiloBands(seg);
                continue;
            }
            Node at = seg;
            for (int bend = 0; bend < 2; bend++) {
                if (rnd.nextInt(100) >= (bend == 0 ? 85 : 40)) break;
                int turn = (at.dir + (rnd.nextBoolean() ? 1 : 3)) & 3;
                if (turn == ((side + 2) & 3)) turn = (turn + 2) & 3;
                int len2 = Math.min(room(at, turn) - END_ROOM, 36 + rnd.nextInt(36));
                if (len2 < 24) break;
                int[] b2 = adjacent(at, turn, segFar(at, w), w, len2);
                if (!free(b2[0], b2[1], b2[2], b2[3])) break;
                Node next = add(MAJOR, null, b2, ComplexLayout.MAJOR_HEIGHT, at, turn);
                branch(at, turn, door(Door.Kind.PORTAL, at, turn, next, 0, portalTop(at, next), true));
                out.add(next);
                at = next;
            }
        }
        // Up to three side branches: a major hall leaving the middle of a long segment, so the halls form a graph.
        int branches = 0;
        for (Node seg : new ArrayList<Node>(out)) {
            if (branches >= 3 || seg.length() < 50 || out.indexOf(seg) == 0 || rnd.nextInt(100) >= 70) continue;
            int turn = (seg.dir + (rnd.nextBoolean() ? 1 : 3)) & 3;
            int mid = (seg.alongX() ? (seg.x0 + seg.x1) : (seg.z0 + seg.z1)) / 2 - w / 2;
            int len = Math.min(room(seg, turn) - END_ROOM, 30 + rnd.nextInt(30));
            if (len < 24) continue;
            int[] b = adjacent(seg, turn, mid, w, len);
            if (!free(b[0], b[1], b[2], b[3])) continue;
            Node t = add(MAJOR, null, b, ComplexLayout.MAJOR_HEIGHT, seg, turn);
            branch(seg, turn, door(Door.Kind.PORTAL, seg, turn, t, 0, portalTop(seg, t), true));
            out.add(t);
            branches++;
        }
        return out;
    }

    /**
     * Keeps the strips a silo deep along both sides of the gallery hall free, so no bend or side branch of another hall
     * grows into them; {@link #gallery} frees them again before it lines the hall.
     */
    private void reserveSiloBands(Node g) {
        for (int side : g.alongX() ? new int[] { 0, 2 } : new int[] { 1, 3 }) {
            int[] b = adjacent(g, side, g.latLo(side), g.latHi(side) - g.latLo(side), ComplexLayout.SILO_SIZE);
            for (int x = Math.max(0, b[0]); x < Math.min(S, b[2]); x++) {
                for (int z = Math.max(0, b[1]); z < Math.min(S, b[3]); z++)
                    if (own[x * S + z] == 0) own[x * S + z] = RESERVED;
            }
        }
    }

    private void releaseSiloBands() {
        for (int i = 0; i < own.length; i++) if (own[i] == RESERVED) own[i] = 0;
    }

    /** The lateral start of the last {@code w} cells of a segment, at its far end. */
    private static int segFar(Node seg, int w) {
        switch (seg.dir) {
            case 0:
                return seg.z0;
            case 2:
                return seg.z1 - w;
            case 1:
                return seg.x1 - w;
            default:
                return seg.x0;
        }
    }

    /**
     * Records a branch point: the floor cell of hall {@code p} beside the opening, two cells in from the wall and two
     * cells clear of the opening's edge, so a guard post there never stands in the way (D233).
     */
    private void branch(Node p, int side, Door d) {
        if (d == null) return;
        int a0 = (side & 1) == 0 ? d.x0 : d.z0, a1 = (side & 1) == 0 ? d.x1 : d.z1;
        int lat = a0 - 2 >= p.latLo(side) + 1 ? a0 - 2 : a1 + 2;
        if (lat > p.latHi(side) - 2) return;
        switch (side) {
            case 0:
                branchPoints.add(new int[] { lat, p.z0 + 2, p.id });
                break;
            case 2:
                branchPoints.add(new int[] { lat, p.z1 - 3, p.id });
                break;
            case 1:
                branchPoints.add(new int[] { p.x1 - 3, lat, p.id });
                break;
            default:
                branchPoints.add(new int[] { p.x0 + 2, lat, p.id });
                break;
        }
    }

    // --- silos ---------------------------------------------------------------------------------------------------

    /** Silo count, 4 to 10, mostly 4 to 6 (D229). Weights in percent. */
    static final int[] SILO_WEIGHTS = { 26, 24, 18, 12, 10, 6, 4 };
    static final int SILO_PITCH = 7;

    static int rollSilos(Random rnd) {
        int r = rnd.nextInt(100);
        for (int i = 0; i < SILO_WEIGHTS.length; i++) {
            r -= SILO_WEIGHTS[i];
            if (r < 0) return 4 + i;
        }
        return 4;
    }

    /** The silo count this growth rolled; {@link #silos} holds exactly this many unless no hall had room. */
    int rolled;

    /**
     * Lines one or two straight major halls with silos on both sides, from the far end inward, each silo's controller
     * facing the hall through a walkway in its wall. Seven or more silos split across two straight halls when a
     * second one exists, the first (Launch Control's) taking the larger half; whatever the second cannot hold goes
     * back to the first, which always has room for ten. A hall that ends up with no silo is not a gallery.
     *
     * @return the gallery halls, the first being the one Launch Control caps
     */
    private List<Node> gallery(List<Node> majors) {
        releaseSiloBands();
        int n = rollSilos(rnd);
        rolled = n;
        // Only a straight hall can carry facing pairs: a bend or a side branch takes one side where the pairs go.
        List<Node> cands = new ArrayList<Node>();
        for (Node m : majors) if (m.parent == 0 && m.length() >= 40 && straight(m)) cands.add(m);
        if (cands.isEmpty()) throw new IllegalStateException("no hall long enough for the silo gallery");
        Node first = cands.get(0);
        Node second = n >= 7 && cands.size() > 1 ? cands.get(1 + rnd.nextInt(cands.size() - 1)) : null;
        List<Node> halls = new ArrayList<Node>();
        halls.add(first);
        int left = n - lineSilos(first, second == null ? n : n - n / 2);
        if (second != null) {
            int placed = lineSilos(second, left);
            left -= placed;
            if (placed > 0) halls.add(second);
        }
        if (left > 0) left -= lineSilos(first, left);
        for (Node m : cands) {
            // Only if the first hall is full: another straight hall takes the rest.
            if (left <= 0 || halls.size() >= 2) break;
            if (halls.contains(m)) continue;
            int placed = lineSilos(m, left);
            if (placed > 0) {
                halls.add(m);
                left -= placed;
            }
        }
        for (Node g : halls) {
            g.gallery = true;
            g.h = ComplexLayout.GALLERY_HEIGHT;
        }
        return halls;
    }

    /** @return whether no major hall leaves {@code m} (no bend at its far end, no side branch) */
    private boolean straight(Node m) {
        for (Node x : nodes) if (x.type == MAJOR && x.parent == m.id) return false;
        return true;
    }

    /** @return how many silos already line hall {@code g} (one walkway each) */
    private int silosOn(Node g) {
        int c = 0;
        for (Door d : doors) if (d.kind == Door.Kind.WALKWAY && d.a == g.id) c++;
        return c;
    }

    /**
     * Lines a hall with up to {@code want} silos, from the far end toward the hub (14 cells kept clear at the hub
     * end): facing pairs first, one on each side, then single silos where only one side has room.
     *
     * @return how many were placed
     */
    private int lineSilos(Node g, int want) {
        boolean alongX = g.alongX();
        int sideA = alongX ? 0 : 3, sideB = alongX ? 2 : 1;
        int placed = 0, before = silosOn(g);
        int len = g.length();
        for (int pass = 0; pass < 2; pass++) {
            // Singles only where a facing pair already stands, so the silos always line both sides.
            if (pass == 1 && before + placed < 2) break;
            for (int k = 0; placed < want && 3 + k * SILO_PITCH + 5 <= len - 14; k++) {
                int off = 3 + k * SILO_PITCH;
                int along = farCoord(g) + (g.dir == 0 || g.dir == 3 ? off : -off - 5);
                int[] a = adjacent(g, sideA, along, 5, 5), b = adjacent(g, sideB, along, 5, 5);
                boolean fa = free(a[0], a[1], a[2], a[3]), fb = free(b[0], b[1], b[2], b[3]);
                if (pass == 0) {
                    if (!fa || !fb) continue;
                    silo(g, sideA, a);
                    placed++;
                    if (placed < want) {
                        silo(g, sideB, b);
                        placed++;
                    }
                } else if (fa) {
                    silo(g, sideA, a);
                    placed++;
                } else if (fb) {
                    silo(g, sideB, b);
                    placed++;
                }
            }
        }
        return placed;
    }

    private void silo(Node g, int side, int[] b) {
        int facing = (side + 2) & 3;
        SiloSlot s = new SiloSlot(
            new Box(b[0], 0, b[1], b[2], ComplexLayout.SILO_HEIGHT, b[3]),
            SurfaceVariant.values()[rnd.nextInt(3)],
            MissingPart.values()[rnd.nextInt(2)],
            facing);
        claim(b[0], b[1], b[2], b[3], SILO);
        silos.add(s);
        // The walkway: the hall's wall cells in front of the silo's three middle columns.
        int dx = s.frontX() - s.controllerX(), dz = s.frontZ() - s.controllerZ();
        doors.add(
            new Door(
                Door.Kind.WALKWAY,
                s.x(1, 0) + dx,
                s.x(3, 0) + dx,
                s.z(1, 0) + dz,
                s.z(3, 0) + dz,
                1,
                3,
                g.id,
                -1,
                side));
    }

    /** The coordinate of a hall's far end wall along its axis (min side for north and west halls). */
    private static int farCoord(Node g) {
        switch (g.dir) {
            case 0:
                return g.z0;
            case 2:
                return g.z1;
            case 1:
                return g.x1;
            default:
                return g.x0;
        }
    }

    /** Launch Control caps the gallery's far end and overlooks it through windows (D232). */
    private Node launchControl(Node g) {
        int side = g.dir;
        RoomKind k = RoomKind.LAUNCH_CONTROL;
        for (int t = 0; t < 4; t++) {
            int len = k.length - t * 2, dep = k.depth - t * 2;
            int lat0 = g.latLo(side) + (g.latHi(side) - g.latLo(side)) / 2 - len / 2;
            int[] b = adjacent(g, side, lat0, len, dep);
            if (!free(b[0], b[1], b[2], b[3])) continue;
            Node lc = add(ROOM, k, b, ComplexLayout.ROOM_HEIGHT, g, side);
            Door d = door(Door.Kind.DOOR, g, side, lc, 3, 4, true);
            // Windows either side of the doorway, across the rest of the shared span.
            int lo = Math.max(g.latLo(side), lc.latLo(side)) + 1;
            int hi = Math.min(g.latHi(side), lc.latHi(side)) - 2;
            int dLo = side == 0 || side == 2 ? d.x0 : d.z0, dHi = side == 0 || side == 2 ? d.x1 : d.z1;
            window(g, side, lo, dLo - 2);
            window(g, side, dHi + 2, hi);
            return lc;
        }
        // The far end is taken: Launch Control opens off the gallery's side instead.
        Site s = new Site(g, g.alongX() ? 0 : 3, g.latLo(g.alongX() ? 0 : 3));
        Node lc = place(s, k, k.length, k.depth);
        if (lc == null) {
            s = new Site(g, g.alongX() ? 2 : 1, g.latLo(g.alongX() ? 2 : 1));
            lc = place(s, k, k.length, k.depth);
        }
        if (lc == null) throw new IllegalStateException("no place for Launch Control");
        return lc;
    }

    private void window(Node g, int side, int a0, int a1) {
        if (a1 < a0) return;
        int top = 6;
        switch (side) {
            case 0:
                doors.add(new Door(Door.Kind.WINDOW, a0, a1, g.z0 - 1, g.z0, 3, top, g.id, -1, 0));
                break;
            case 2:
                doors.add(new Door(Door.Kind.WINDOW, a0, a1, g.z1 - 1, g.z1, 3, top, g.id, -1, 2));
                break;
            case 1:
                doors.add(new Door(Door.Kind.WINDOW, g.x1 - 1, g.x1, a0, a1, 3, top, g.id, -1, 1));
                break;
            default:
                doors.add(new Door(Door.Kind.WINDOW, g.x0 - 1, g.x0, a0, a1, 3, top, g.id, -1, 3));
                break;
        }
    }

    /** The decontamination airlock sits in the corner between the hub and a major hall, opening onto both. */
    private void decon(Node hub, List<Node> majors) {
        RoomKind k = RoomKind.DECON_AIRLOCK;
        List<Node> order = new ArrayList<Node>(majors);
        Collections.shuffle(order, rnd);
        for (Node m : order) {
            if (m.parent != 0) continue;
            int side = m.dir;
            for (boolean high : new boolean[] { true, false }) {
                int lat0 = high ? m.latHi(side) : m.latLo(side) - k.length;
                int[] b = adjacent(hub, side, lat0, k.length, k.depth);
                if (!free(b[0], b[1], b[2], b[3])) continue;
                Node d = add(ROOM, k, b, ComplexLayout.MINOR_HEIGHT, hub, side);
                if (door(Door.Kind.DOOR, hub, side, d, 2, 3, true) == null) {
                    unclaim(d);
                    continue;
                }
                // The second door, into the hall beside it.
                int hs = sideFacing(m, d);
                if (hs >= 0) door(Door.Kind.DOOR, m, hs, d, 2, 3, true);
                return;
            }
        }
    }

    /** @return the side of {@code a} that {@code b} touches, or -1 */
    static int sideFacing(Node a, Node b) {
        if (b.z1 == a.z0 && b.x0 < a.x1 && a.x0 < b.x1) return 0;
        if (b.z0 == a.z1 && b.x0 < a.x1 && a.x0 < b.x1) return 2;
        if (b.x0 == a.x1 && b.z0 < a.z1 && a.z0 < b.z1) return 1;
        if (b.x1 == a.x0 && b.z0 < a.z1 && a.z0 < b.z1) return 3;
        return -1;
    }

    // --- rooms ---------------------------------------------------------------------------------------------------

    private void addSites(Node n) {
        for (int side = 0; side < 4; side++) {
            if (n.type != HUB && side == ((n.dir + 2) & 3)) continue; // the side it hangs off
            int lo = n.latLo(side);
            sites.add(new Site(n, side, lo - (n.hall() ? rnd.nextInt(8) : 0)));
        }
    }

    private int weight(Site s) {
        if (s.done) return 0;
        switch (s.n.type) {
            case HUB:
            case MAJOR:
                return 6;
            case MINOR:
                return 5;
            default:
                return s.n.depth >= 3 || s.n.kind.small() ? 0 : s.n.depth == 1 ? 2 : 1;
        }
    }

    private Site pick() {
        int total = 0;
        for (Site s : sites) total += weight(s);
        if (total == 0) return null;
        int r = rnd.nextInt(total);
        for (Site s : sites) {
            r -= weight(s);
            if (r < 0) return s;
        }
        return null;
    }

    private void growRooms(List<RoomKind> queue, Node gallery, int posts, int target) {
        // The assembly floor first, on the gallery's sides.
        for (int side = 0; side < 4 && queue.get(0) == RoomKind.MISSILE_ASSEMBLY; side++) {
            if (side == ((gallery.dir + 2) & 3) || side == gallery.dir) continue;
            Site s = new Site(gallery, side, gallery.latLo(side));
            if (place(s, RoomKind.MISSILE_ASSEMBLY, RoomKind.MISSILE_ASSEMBLY.length, RoomKind.MISSILE_ASSEMBLY.depth)
                != null) queue.remove(0);
        }
        int minors = 0, maxMinors = 10 + rnd.nextInt(6);
        int postsLeft = posts;
        for (int guard = 0; guard < 6000 && !queue.isEmpty(); guard++) {
            Site s = pick();
            if (s == null) break;
            boolean hallSite = s.n.type == HUB || s.n.type == MAJOR || s.n.type == MINOR && s.side != s.n.dir;
            if (hallSite && minors < maxMinors && rnd.nextInt(100) < (s.n.type == MINOR ? 25 : 40)) {
                Node m = minorHall(s);
                if (m != null) {
                    minors++;
                    if (postsLeft > 0 && guardPost(s.n, s.side, m)) postsLeft--;
                    continue;
                }
            }
            RoomKind k = queue.get(0);
            int len = scale(k.length), dep = scale(k.depth);
            if (s.n.type == MINOR && s.side == s.n.dir) {
                // The far end of a minor hallway: a room caps it.
                len = Math.max(len, 12);
            }
            Node r = place(s, k, len, dep);
            if (r != null) queue.remove(0);
        }
        // Guard posts left over stand beside the major halls' mouths at the hub.
        for (Node m : new ArrayList<Node>(nodes)) {
            if (postsLeft <= 0) break;
            if (m.type == MAJOR && m.parent == 0 && guardPost(nodes.get(0), m.dir, m)) postsLeft--;
        }
        for (Node m : new ArrayList<Node>(nodes)) {
            if (postsLeft <= 0) break;
            if (m.type == MINOR && guardPost(nodes.get(m.parent), m.dir, m)) postsLeft--;
        }
        // Last resort, so a plan never has fewer than three (D233): beside any major hall's mouth, bends and branches.
        for (Node m : new ArrayList<Node>(nodes)) {
            if (postsLeft <= 0 || posts() >= 3) break;
            if (m.type == MAJOR && m.parent != 0 && guardPost(nodes.get(m.parent), m.dir, m)) postsLeft--;
        }
        // Whatever is still queued squeezes in wherever a side has room, smaller.
        for (int guard = 0; guard < 4000 && !queue.isEmpty() && roomCount() < target; guard++) {
            Site s = pick();
            if (s == null) {
                for (Site x : sites) if (x.n.type == ROOM && x.n.depth < 4) x.done = false;
                s = pickAny();
                if (s == null) break;
            }
            RoomKind k = queue.get(0);
            int shrink = guard < 2000 ? 4 : 3;
            if (place(s, k, scale(k.length) * shrink / 5, scale(k.depth) * shrink / 5) != null) queue.remove(0);
        }
    }

    private Site pickAny() {
        List<Site> open = new ArrayList<Site>();
        for (Site s : sites) if (!s.done && !(s.n.type == ROOM && s.n.kind.small())) open.add(s);
        return open.isEmpty() ? null : open.get(rnd.nextInt(open.size()));
    }

    private int scale(int v) {
        return v * (85 + rnd.nextInt(31)) / 100;
    }

    /**
     * Places a room on a site: halls take rooms side by side from the cursor; a room's side takes one where it fits.
     *
     * @return the room, or null (the site is marked done when it has no room left)
     */
    private Node place(Site s, RoomKind k, int len, int dep) {
        Node p = s.n;
        int lo = p.latLo(s.side), hi = p.latHi(s.side);
        int dw = doorWidth(k, p);
        int need = dw + 2;
        if (p.hall()) {
            while (s.cursor < hi - need - 2) {
                for (int t = 0; t < 4; t++) {
                    int w = len - t * len / 8, d = dep - t * dep / 8;
                    if (w < 12 || d < 10) break;
                    // Never hang a room more than half off the hall's side.
                    int lat0 = Math.max(s.cursor, lo - w / 2);
                    if (Math.min(hi, lat0 + w) - Math.max(lo, lat0) - 3 < need) continue;
                    int[] b = clip(adjacent(p, s.side, lat0, w, d));
                    if (b == null || !free(b[0], b[1], b[2], b[3])) continue;
                    Node r = attachRoom(p, s.side, k, b, dw);
                    if (r == null) continue;
                    s.cursor = lat0 + w;
                    return r;
                }
                s.cursor += 3;
            }
            s.done = true;
            return null;
        }
        for (int t = 0; t < 10; t++) {
            int w = len - (t / 2) * len / 8, d = dep - (t / 2) * dep / 8;
            if (w < 12 || d < 10) break;
            int span = hi - lo;
            int lat0 = lo - w + need + 3 + rnd.nextInt(Math.max(1, span + w - 2 * need - 6));
            int[] b = clip(adjacent(p, s.side, lat0, w, d));
            if (b == null || !free(b[0], b[1], b[2], b[3])) continue;
            Node r = attachRoom(p, s.side, k, b, dw);
            if (r != null) return r;
        }
        s.done = true;
        return null;
    }

    private static int doorWidth(RoomKind k, Node parent) {
        if (k.small()) return 2;
        if (k == RoomKind.VEHICLE_BAY && parent.hall()) return 5;
        return 3;
    }

    /**
     * Clips a proposed box to the complex, keeping the side that touches the parent; null when what is left is under
     * the smallest room.
     */
    private static int[] clip(int[] b) {
        int[] c = { Math.max(0, b[0]), Math.max(0, b[1]), Math.min(S, b[2]), Math.min(S, b[3]) };
        return c[2] - c[0] >= 12 && c[3] - c[1] >= 12 ? c : null;
    }

    private Node attachRoom(Node p, int side, RoomKind k, int[] b, int dw) {
        int h = k.small() ? ComplexLayout.MINOR_HEIGHT : ComplexLayout.ROOM_HEIGHT;
        Node r = add(ROOM, k, b, h, p, side);
        int top = k.small() ? 3 : dw >= 5 ? 5 : 4;
        Door d = door(Door.Kind.DOOR, p, side, r, dw, top, p.type == ROOM || rnd.nextBoolean());
        if (d == null) {
            unclaim(r);
            return null;
        }
        if (!k.small()) addSites(r);
        return r;
    }

    private void unclaim(Node r) {
        claim(r.x0, r.z0, r.x1, r.z1, (short) 0);
        nodes.remove(nodes.size() - 1);
    }

    /** A minor hallway off a hall's side, its own sides and far end taking rooms (D230). */
    private Node minorHall(Site s) {
        Node p = s.n;
        int lo = p.latLo(s.side), hi = p.latHi(s.side);
        int w = ComplexLayout.MINOR_WIDTH;
        for (int t = 0; t < 6; t++) {
            int lat0 = Math.max(s.cursor + 6, lo + 2) + rnd.nextInt(12);
            if (lat0 + w > hi - 2) return null;
            int len = Math.min(30 + rnd.nextInt(40), room(p, s.side) - 14);
            if (len < 16) return null;
            int[] b = adjacent(p, s.side, lat0, w, len);
            if (!free(b[0], b[1], b[2], b[3])) continue;
            Node m = add(MINOR, null, b, ComplexLayout.MINOR_HEIGHT, p, s.side);
            branch(p, s.side, door(Door.Kind.PORTAL, p, s.side, m, 0, portalTop(p, m), true));
            addSites(m);
            // Rooms on this side of the parent continue past the hallway.
            s.cursor = Math.max(s.cursor, lat0 - 1);
            return m;
        }
        return null;
    }

    /** A guard post beside a branch's mouth, against the parent hall (D233). */
    private boolean guardPost(Node parent, int side, Node branch) {
        RoomKind k = RoomKind.GUARD_POST;
        int blo = branch.latLo(side), bhi = branch.latHi(side);
        for (boolean high : new boolean[] { rnd.nextBoolean(), true, false }) {
            int lat0 = high ? bhi : blo - k.length;
            int[] b = adjacent(parent, side, lat0, k.length, k.depth);
            if (!free(b[0], b[1], b[2], b[3])) continue;
            if (attachRoom(parent, side, k, b, 2) != null) return true;
        }
        return false;
    }

    // --- coverage --------------------------------------------------------------------------------------------------

    /**
     * Grows every room (and the free far end of every hallway) a cell at a time into the rock around it, up to its
     * kind's limit, until nothing grows. Sides that touch a neighbor stop by themselves, so doors never move.
     */
    private void inflate() {
        boolean grew = true;
        for (int pass = 0; grew && pass < 60; pass++) {
            grew = false;
            for (Node n : nodes) {
                if (n.type == HUB || n.type == MAJOR && (n.gallery || blockedByDoor(n, n.dir))) continue;
                if (n.type == ROOM && n.kind.small()) continue;
                for (int side = 0; side < 4; side++) {
                    if (n.type == ROOM && side == ((n.dir + 2) & 3)) continue;
                    if (n.type != ROOM && side != n.dir) continue;
                    if (!underLimit(n, side)) continue;
                    if (growSide(n, side)) grew = true;
                }
            }
        }
    }

    /** How far a room may grow past its nominal size, per axis, at first, and the hard caps. */
    static final int CAP_PERCENT = 150, CAP_LATERAL = 52, CAP_DEPTH = 46;
    private int capPercent = CAP_PERCENT, capLateral = CAP_LATERAL, capDepth = CAP_DEPTH;

    private boolean underLimit(Node n, int side) {
        if (n.type == MINOR) return n.length() < 72;
        if (n.type == MAJOR) return n.length() < 90 && room(n, n.dir) > 2;
        // Lateral is along the door wall; depth runs away from it.
        boolean lateralAxis = (side & 1) == (n.dir & 1) ? false : true;
        int along = lateralAxis ? ((n.dir & 1) == 0 ? n.x1 - n.x0 : n.z1 - n.z0)
            : ((n.dir & 1) == 0 ? n.z1 - n.z0 : n.x1 - n.x0);
        int cap = lateralAxis ? Math.min(capLateral, n.kind.length * capPercent / 100)
            : Math.min(capDepth, n.kind.depth * capPercent / 100);
        // Rooms stay rooms, not corridors: neither side grows past twice the other.
        int other = (side & 1) == 0 ? n.x1 - n.x0 : n.z1 - n.z0;
        return along < cap && along < 2 * other;
    }

    private boolean growSide(Node n, int side) {
        int[] strip;
        switch (side) {
            case 0:
                strip = new int[] { n.x0, n.z0 - 1, n.x1, n.z0 };
                break;
            case 2:
                strip = new int[] { n.x0, n.z1, n.x1, n.z1 + 1 };
                break;
            case 1:
                strip = new int[] { n.x1, n.z0, n.x1 + 1, n.z1 };
                break;
            default:
                strip = new int[] { n.x0 - 1, n.z0, n.x0, n.z1 };
                break;
        }
        if (!free(strip[0], strip[1], strip[2], strip[3])) return false;
        claim(strip[0], strip[1], strip[2], strip[3], (short) (n.id + 1));
        switch (side) {
            case 0:
                n.z0--;
                break;
            case 2:
                n.z1++;
                break;
            case 1:
                n.x1++;
                break;
            default:
                n.x0--;
                break;
        }
        return true;
    }

    /** Extra doors between rooms that ended up side by side: the plan becomes a graph, not just a tree (D230). */
    private void loops() {
        List<Node> rooms = new ArrayList<Node>();
        for (Node n : nodes) if (n.type == ROOM && !n.kind.small()) rooms.add(n);
        for (int i = 0; i < rooms.size(); i++) {
            for (int j = i + 1; j < rooms.size(); j++) {
                Node a = rooms.get(i), b = rooms.get(j);
                if (a.parent == b.id || b.parent == a.id) continue;
                int side = sideFacing(a, b);
                if (side < 0 || rnd.nextInt(100) >= 15) continue;
                if (blockedByDoor(a, side) || blockedByDoor(b, (side + 2) & 3)) continue;
                door(Door.Kind.DOOR, a, side, b, 3, 4, true);
            }
        }
    }

    /** True when a wall already has a door (one door per wall keeps furnishing simple). */
    private boolean blockedByDoor(Node n, int side) {
        for (Door d : doors) {
            if (d.kind == Door.Kind.WINDOW || d.kind == Door.Kind.WALKWAY) continue;
            if (d.a == n.id && d.side == side || d.b == n.id && ((d.side + 2) & 3) == side) return true;
        }
        return false;
    }

    // --- escape and surface ----------------------------------------------------------------------------------------

    /** The escape ladder at a minor hallway's dead end, else a major hall's, else the hub's corner. */
    private void escape() {
        List<Node> order = new ArrayList<Node>();
        for (Node n : nodes) if (n.type == MINOR) order.add(n);
        for (Node n : nodes) if (n.type == MAJOR) order.add(n);
        for (Node n : order) {
            int side = n.dir;
            if (blockedByDoor(n, side) || hasWindow(n, side)) continue;
            int mid = (n.latLo(side) + n.latHi(side)) / 2;
            int x, z, meta;
            switch (side) {
                case 0:
                    x = mid;
                    z = n.z0 + 1;
                    meta = 3;
                    break;
                case 2:
                    x = mid;
                    z = n.z1 - 2;
                    meta = 2;
                    break;
                case 1:
                    x = n.x1 - 2;
                    z = mid;
                    meta = 4;
                    break;
                default:
                    x = n.x0 + 1;
                    z = mid;
                    meta = 5;
                    break;
            }
            escape = new int[] { x, z, meta, n.h, n.id };
            return;
        }
        Node hub = nodes.get(0);
        escape = new int[] { hub.x0 + 1, hub.z0 + 1, 3, hub.h, 0 };
    }

    private boolean hasWindow(Node n, int side) {
        for (Door d : doors) if (d.kind == Door.Kind.WINDOW && d.a == n.id && d.side == side) return true;
        return false;
    }

    private static final int RADAR_SPACING = 12;

    /** 0 to 2 radar huts on the surface, clear of every silo shaft and the escape column (D215). */
    private void radars() {
        int count = rnd.nextInt(3);
        for (int i = 0; i < count; i++) {
            for (int tries = 0; tries < 24; tries++) {
                int[] at = { 12 + rnd.nextInt(S - 30), 12 + rnd.nextInt(S - 30) };
                if (clearOfSurface(at)) {
                    radars.add(at);
                    break;
                }
            }
        }
    }

    private boolean clearOfSurface(int[] at) {
        for (int[] o : radars) {
            if (Math.abs(o[0] - at[0]) < RADAR_SPACING && Math.abs(o[1] - at[1]) < RADAR_SPACING) return false;
        }
        for (SiloSlot s : silos) {
            Box b = s.bounds();
            if (at[0] + 5 + 3 > b.minX && at[0] - 3 < b.maxX && at[1] + 5 + 3 > b.minZ && at[1] - 3 < b.maxZ) {
                return false;
            }
        }
        return Math.abs(at[0] + 2 - escape[0]) > 6 || Math.abs(at[1] + 2 - escape[1]) > 6;
    }
}
