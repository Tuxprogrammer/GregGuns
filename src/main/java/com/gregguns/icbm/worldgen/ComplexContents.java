package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.ComplexLayout;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.LootKeys;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.complex.SiloSlot;
import com.gregguns.icbm.complex.Tunnel;
import com.gregguns.icbm.worldgen.Placement.Bonus;
import com.gregguns.icbm.worldgen.Placement.Kind;
import com.gregguns.worldgen.spawner.SpawnerKind;

/**
 * The activity pass (D36) and the contents pass (D129): turns a layout's marks into {@link Placement}s for one activity
 * level and one ceiling. Pure and stateless. Every roll is a hash of the plan's seed and the mark's own cell, never of
 * call order, so each populate window that rebuilds the list gets the same answer for the cells it owns, and no window
 * ever needs another chunk (D17, never cascading).
 *
 * <p>
 * Usage: {@code ComplexContents.build(plan, level, ceiling, otherComplexCell)}, then apply each placement inside the
 * window it falls in.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md, loot-and-ceilings.md, silo-guard-spawner.md
public final class ComplexContents {

    /** Salts, one per roll family, so two rolls on one cell are independent. */
    private static final long S_COLLAPSE = 0xC011, S_CELL = 0xCE11, S_LIGHT = 0x116, S_ELITE = 0xE11, S_SPAWN_N = 0x59B,
        S_MACHINE = 0x3AC, S_DEAD = 0xDEAD, S_CARRIER = 0xCA2, S_ROLL = 0x201, S_RUBBLE = 0x2B1, S_ABSENT = 0xAB5,
        S_VARIANT = 0x7A2, S_STOCK = 0x570E;

    /**
     * Share of working machines whose output slots hold finished work, by state: staffed, in service, mothballed,
     * gutted (D323). A machine never holds an input or a job in progress.
     */
    static final double[] STOCKED = { 0.8, 0.6, 0.3, 0.1 };

    private ComplexContents() {}

    /**
     * @param plan    the layout
     * @param level   the complex's activity level
     * @param ceiling the complex's ceiling ({@link SiloCeiling#EV} to {@link SiloCeiling#UMV})
     * @param other   the grid cell {cellX, cellZ} of another complex, for a comms data stick, or null for none
     * @return the placements, collapses first so the contents lie on top of the rubble
     */
    public static List<Placement> build(ComplexPlan plan, ActivityLevel level, int ceiling, int[] other) {
        ActivityProfile p = ActivityProfile.of(level);
        long seed = plan.seed();
        List<Placement> collapses = new ArrayList<>();
        List<Placement> out = new ArrayList<>();
        int[] chestCounter = { 0 };
        List<Placement> chests = new ArrayList<>();
        java.util.Set<Integer> collapsedRooms = new java.util.HashSet<>();
        java.util.Map<Integer, RoomState> states = new java.util.HashMap<>();
        Box hall = plan.hall()
            .bounds();
        states.put(
            plan.hall()
                .id(),
            roomState(seed, plan.hall(), level));
        for (Room r : plan.rooms()) {
            if (mayCollapse(r.kind()) && collapseRoll(seed, r.id(), p)) {
                collapsedRooms.add(r.id());
            }
            states.put(r.id(), roomState(seed, r, level));
        }
        // The chest budget (D315): exactly the configured number of chest spots become loot chests.
        Chests c = new Chests(
            ChestBudget.choose(plan, level, states, collapsedRooms, ChestBudget.target(level)),
            chests,
            chestCounter);

        mark(plan, plan.hall(), hall, false, p, seed, ceiling, hall, out, c, level);
        for (Room r : plan.rooms()) {
            boolean collapsed = collapsedRooms.contains(r.id());
            if (collapsed) {
                collapses.add(
                    Placement.at(Kind.COLLAPSE, r.bounds().minX, r.bounds().minY, r.bounds().minZ)
                        .box(r.bounds())
                        .build());
            }
            mark(plan, r, r.bounds(), collapsed, p, seed, ceiling, hall, out, c, level);
        }
        for (Tunnel t : plan.tunnels()) {
            marks(
                t.marks(),
                roomState(seed, t.id(), level),
                t.bounds(),
                null,
                -1,
                false,
                p,
                seed,
                ceiling,
                hall,
                out,
                c,
                level);
        }
        bonuses(plan, p, seed, other, ceiling, chests);
        List<Placement> all = new ArrayList<>(collapses);
        all.addAll(out);
        return keepFrameWalls(all);
    }

    /**
     * Whether a room of this kind may cave in. Guard posts and the airlock never do, and neither does Launch Control:
     * it overlooks the silo gallery and is the complex's goal, so it stays walkable at every level (D252); its lamps,
     * rubble and breaches still follow the level. The halls, the gallery among them, never collapse either: they are
     * marked with {@code collapsed = false}.
     */
    static boolean mayCollapse(RoomKind kind) {
        return kind != RoomKind.GUARD_POST && kind != RoomKind.DECON_AIRLOCK && kind != RoomKind.LAUNCH_CONTROL;
    }

    /**
     * @param seed  the plan seed
     * @param room  a room
     * @param level the complex's level
     * @return whether the room caves in at this level (its contents, notices and stock are then not written)
     */
    public static boolean collapsed(long seed, Room room, ActivityLevel level) {
        return mayCollapse(room.kind()) && collapseRoll(seed, room.id(), ActivityProfile.of(level));
    }

    /** @return whether a room's collapse roll comes up, before {@link #mayCollapse} is asked */
    static boolean collapseRoll(long seed, int roomId, ActivityProfile p) {
        return Mix.unit(Mix.of(seed, S_COLLAPSE, roomId, 0)) < p.collapseChance();
    }

    private static final int[] FRAME_DX = { 0, -1, 0, 1 };
    private static final int[] FRAME_DZ = { 1, 0, -1, 0 };

    /**
     * An item frame needs the wall behind it and open air in front, so the damage pass leaves both cells alone (a
     * breach or rubble there is dropped from the list), and a collapsed armory hangs no frames at all.
     */
    static List<Placement> keepFrameWalls(List<Placement> all) {
        List<Placement> walls = new ArrayList<>();
        for (Placement p : all) {
            if (p.kind() == Kind.FRAME_WALL) {
                walls.add(p);
            }
        }
        if (walls.isEmpty()) {
            return all;
        }
        java.util.Set<Long> cells = new java.util.HashSet<>();
        java.util.Set<Placement> fallen = new java.util.HashSet<>();
        for (Placement f : walls) {
            for (Placement c : all) {
                if (c.kind() == Kind.COLLAPSE && inside(c.box(), f)) {
                    fallen.add(f);
                }
            }
            boolean alongZ = f.facing() == 1 || f.facing() == 3;
            for (int i = 0; i < f.extent(); i++) {
                int x = f.x() + (alongZ ? 0 : ComplexLayout.FRAME_STRIDE * i);
                int z = f.z() + (alongZ ? ComplexLayout.FRAME_STRIDE * i : 0);
                cells.add(wallKey(x, f.y(), z));
                cells.add(wallKey(x - FRAME_DX[f.facing()], f.y(), z - FRAME_DZ[f.facing()]));
            }
        }
        List<Placement> kept = new ArrayList<>(all.size());
        for (Placement p : all) {
            boolean damage = p.kind() == Kind.AIR || p.kind() == Kind.RUBBLE;
            if (fallen.contains(p) || damage && cells.contains(wallKey(p.x(), p.y(), p.z()))) {
                continue;
            }
            kept.add(p);
        }
        return kept;
    }

    /** Whether a frame wall's first cell lies in the interior a cave-in fills. */
    private static boolean inside(Box b, Placement f) {
        return f.x() > b.minX && f.x() < b.maxX - 1 && f.z() > b.minZ && f.z() < b.maxZ - 1;
    }

    private static long wallKey(int x, int y, int z) {
        return ((long) x << 40) ^ ((long) y << 20) ^ z;
    }

    /** The chest budget's picks and the running chest list of one build. */
    private static final class Chests {

        final java.util.Set<Mark> chosen;
        final List<Placement> list;
        final int[] counter;

        Chests(java.util.Set<Mark> chosen, List<Placement> list, int[] counter) {
            this.chosen = chosen;
            this.list = list;
            this.counter = counter;
        }
    }

    private static void mark(ComplexPlan plan, Room r, Box bounds, boolean collapsed, ActivityProfile p, long seed,
        int ceiling, Box hall, List<Placement> out, Chests chests, ActivityLevel level) {
        RoomState state = roomState(seed, r.id(), level);
        // The room's look at its state (D309): worn, lit or cold, grown or bare; whole groups at a time.
        int from = out.size();
        FurnitureWear.room(plan, r.id(), r.kind(), r.marks(), bounds, state, collapsed, seed, ceiling, out);
        if (!collapsed) {
            // What the standing pieces hold, by the same state (D323).
            String table = RoomLoot.table(
                r.kind() == null ? null
                    : r.kind()
                        .name(),
                null);
            FurnitureStock.room(
                r.kind(),
                r.marks(),
                state,
                table,
                SiloLootTables.categoryName(table, ceiling),
                seed,
                new ArrayList<>(out.subList(from, out.size())),
                out);
        }
        marks(
            r.marks(),
            state,
            bounds,
            r.kind() == null ? null
                : r.kind()
                    .name(),
            r.id(),
            collapsed,
            p,
            seed,
            ceiling,
            hall,
            out,
            chests,
            level);
    }

    /**
     * The wood of a room's crates, 0 to 5, by a hash of the plan seed and the room (the room furniture's own woods:
     * spruce, dark oak, birch, oak).
     */
    static int crateWood(long seed, int roomId) {
        int[] woods = { 1, 5, 2, 0, 1, 5 };
        long h = seed * 0x9E3779B97F4A7C15L + roomId * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        return woods[(int) Math.floorMod(h, (long) woods.length)];
    }

    private static void marks(List<Mark> marks, RoomState state, Box bounds, String owner, int roomId,
        boolean collapsed, ActivityProfile p, long seed, int ceiling, Box hall, List<Placement> out, Chests chests,
        ActivityLevel level) {
        int band = depthBand(bounds, hall);
        for (Mark m : marks) {
            long h = Mix.of(seed, S_CELL, m.x() * 4096L + m.z(), m.y());
            double u = Mix.unit(h);
            switch (m.kind()) {
                case LOOT_CHEST: {
                    if (collapsed) {
                        break; // a caved-in room holds neither chests nor crates
                    }
                    if (!chests.chosen.contains(m)) {
                        // a spot the budget left over: an empty wooden crate, never an empty chest (D315)
                        out.add(
                            Placement.at(Kind.CRATE, m.x(), m.y(), m.z())
                                .key(Integer.toString(crateWood(seed, roomId)))
                                .front(m.facing())
                                .build());
                        break;
                    }
                    String table = RoomLoot.table(owner, m.key());
                    Placement c = Placement.at(Kind.CHEST, m.x(), m.y(), m.z())
                        .category(SiloLootTables.categoryName(table, ceiling))
                        .key(table)
                        .chest(chests.counter[0]++, state)
                        .front(m.facing())
                        .build();
                    chests.list.add(c);
                    out.add(c);
                    break;
                }
                case ITEM_FRAME_WALL:
                    // display frames sit outside the chest budget (owner, alpha 7)
                    out.add(
                        Placement.at(Kind.FRAME_WALL, m.x(), m.y(), m.z())
                            .category(SiloLootTables.categoryName(m.key(), ceiling))
                            .key(m.key())
                            .frames(m.facing(), m.extent())
                            .build());
                    break;
                case GUN_DISPLAY:
                    // every display unit of a standing armory, at every level, outside the chest budget (owner, alpha
                    // 7)
                    if (!collapsed) {
                        Placement chest = Placement.at(Kind.CHEST, m.x(), m.y(), m.z())
                            .category(SiloLootTables.categoryName(LootKeys.ARMORY_AMMO, ceiling))
                            .key(LootKeys.ARMORY_AMMO)
                            .chest(chests.counter[0]++, state)
                            .build();
                        chests.list.add(chest);
                        out.add(chest);
                        out.add(
                            Placement.at(Kind.FRAME_WALL, m.x(), ArmoryDisplay.FRAME_Y, m.z())
                                .category(SiloLootTables.categoryName(m.key(), ceiling))
                                .key(m.key())
                                .frames(m.facing(), 1)
                                .build());
                    }
                    break;
                case LIGHT:
                    if (Mix.unit(lightRoll(seed, m, level)) < p.litChance()) {
                        out.add(
                            Placement.at(Kind.LIGHT, m.x(), m.y(), m.z())
                                .build());
                    }
                    break;
                case DAMAGE_CELL:
                    damageCell(m, u, collapsed, p, out);
                    break;
                case FLOOD_CELL:
                    if (p.flooded()) {
                        out.add(
                            Placement.at(Kind.WATER, m.x(), m.y(), m.z())
                                .build());
                    }
                    break;
                case RUBBLE:
                    if (Mix.unit(Mix.of(seed, S_RUBBLE, m.x() * 4096L + m.z(), m.y())) < rubbleMarkChance(level)) {
                        out.add(
                            Placement.at(Kind.RUBBLE, m.x(), m.y(), m.z())
                                .build());
                    }
                    break;
                case SPAWNER:
                    break; // a room's spawners are rolled as a set, in spawners()
                case MACHINE:
                    machine(m, state, p, seed, ceiling, out);
                    break;
                case FURNITURE:
                    break; // a room's furniture wears as a whole, in FurnitureWear.room
                default:
                    break; // signs are the layout's own; blast doors are fixtures (ComplexFixtures)
            }
        }
        if (roomId >= 0) {
            spawners(marks, band, collapsed, p, seed, roomId, ceiling, out);
        }
    }

    /**
     * The roll that decides whether a lamp burns (D231). A lamp's key names its section and its run (a grid line of
     * lamps, with the floor light tiles under them): an abandoned complex lights or kills whole runs, a partly
     * abandoned one whole sections, so the dark parts read as dead circuits rather than scattered bulbs. The share lit
     * stays the D36 rate either way.
     *
     * @return a hash to compare with the level's lit chance
     */
    static long lightRoll(long seed, Mark m, ActivityLevel level) {
        String[] k = m.key()
            .split(":");
        if (k.length == 3 && "lamp".equals(k[0])) {
            try {
                long section = Long.parseLong(k[1]), run = Long.parseLong(k[2]);
                if (level == ActivityLevel.PARTLY_ABANDONED) {
                    return Mix.of(seed, S_LIGHT, section, -1);
                }
                if (level == ActivityLevel.ABANDONED) {
                    return Mix.of(seed, S_LIGHT, section * 4096L + run, -2);
                }
            } catch (NumberFormatException e) {
                // fall through to a roll per lamp
            }
        }
        return Mix.of(seed, S_LIGHT, m.x() * 4096L + m.z(), m.y());
    }

    private static double rubbleMarkChance(ActivityLevel level) {
        return level == ActivityLevel.ABANDONED ? 1.0 : level == ActivityLevel.PARTLY_ABANDONED ? 0.4 : 0.0;
    }

    private static void damageCell(Mark m, double u, boolean collapsed, ActivityProfile p, List<Placement> out) {
        String part = m.key();
        if (collapsed) {
            if ("roof".equals(part)) {
                out.add(
                    Placement.at(Kind.AIR, m.x(), m.y(), m.z())
                        .build());
            } else {
                out.add(
                    Placement.at(Kind.RUBBLE, m.x(), m.y(), m.z())
                        .build());
            }
            return;
        }
        if ("wall".equals(part)) {
            if (u < p.breachChance()) {
                out.add(
                    Placement.at(Kind.AIR, m.x(), m.y(), m.z())
                        .build());
            } else if (u < p.breachChance() + p.rubbleChance()) {
                out.add(
                    Placement.at(Kind.RUBBLE, m.x(), m.y(), m.z())
                        .build());
            }
        } else if ("floor".equals(part)) {
            if (u < p.holeChance()) {
                out.add(
                    Placement.at(Kind.AIR, m.x(), m.y(), m.z())
                        .build());
            } else if (u < p.holeChance() + p.rubbleChance()) {
                out.add(
                    Placement.at(Kind.RUBBLE, m.x(), m.y(), m.z())
                        .build());
            }
        } else if (u < p.breachChance()) { // roof
            out.add(
                Placement.at(Kind.AIR, m.x(), m.y(), m.z())
                    .build());
        }
    }

    /**
     * How many spawners a room holds (D326): a roll per room from the plan seed and the room, against the level's
     * weights ({@link ActivityProfile#spawnerWeights()}). The same in every populate window.
     *
     * @param seed   the plan seed
     * @param roomId the room's section id (the hub included)
     * @param level  the complex's level
     * @return 0 to 2
     */
    public static int spawnersIn(long seed, int roomId, ActivityLevel level) {
        double u = Mix.unit(Mix.of(seed, S_SPAWN_N, roomId, 0));
        double[] w = ActivityProfile.of(level)
            .spawnerWeights();
        double acc = 0;
        for (int n = 0; n < w.length; n++) {
            acc += w[n];
            if (u < acc) {
                return n;
            }
        }
        return w.length - 1;
    }

    /** Lowest ceiling (GT voltage index, LuV) whose elite posts are command posts (D353). */
    public static final int COMMAND_POST_CEILING = 6;

    /**
     * @param ceiling the complex's ceiling (GT voltage index)
     * @return the elite spawner kind at that ceiling: the command post (with the Outcast) from LuV up, else the elite
     *         post
     */
    public static SpawnerKind elitePost(int ceiling) {
        return ceiling >= COMMAND_POST_CEILING ? SpawnerKind.SILO_COMMAND : SpawnerKind.SILO_ELITE;
    }

    /**
     * The spawners of one room (D326): the first {@link #spawnersIn} of its spawner spots, in the spots' order (the
     * layout lists the farthest-apart first). A caved-in room holds none. A spot makes an elite spawner at the deeper
     * bands 40% of the time, never at the abandoned level (D284's weaker holdouts). At a LuV or higher ceiling the
     * elite spawner is a command post, which adds the Outcast (D353).
     */
    private static void spawners(List<Mark> marks, int band, boolean collapsed, ActivityProfile p, long seed,
        int roomId, int ceiling, List<Placement> out) {
        if (collapsed) {
            return;
        }
        int n = spawnersIn(seed, roomId, p.level());
        for (Mark m : marks) {
            if (n <= 0) {
                break;
            }
            if (m.kind() != com.gregguns.icbm.complex.MarkKind.SPAWNER) {
                continue;
            }
            n--;
            boolean elite = p.level() != ActivityLevel.ABANDONED && band >= 2
                && Mix.unit(Mix.of(seed, S_ELITE, m.x() * 4096L + m.z(), m.y())) < 0.4;
            out.add(
                Placement.at(Kind.SPAWNER, m.x(), m.y(), m.z())
                    .spawner(elite ? elitePost(ceiling) : SpawnerKind.SILO_GUARD)
                    .build());
        }
    }

    /**
     * How lived-in a section is (D309). An operational complex is staffed throughout and an abandoned one gutted. In a
     * partly abandoned complex a section is in service when its lamps burn, by the very roll that lights them (D231's
     * whole-section roll), and mothballed when they are dark: a lit room is a staffed room and a dark one is shut, so
     * its lamps, machines, wear, crops and chests all agree. Pure; the same in every populate window.
     *
     * @param seed  the plan seed
     * @param room  a room (the hub included)
     * @param level the complex's level
     * @return the room's state
     */
    public static RoomState roomState(long seed, Room room, ActivityLevel level) {
        return roomState(seed, room.id(), level);
    }

    /** As {@link #roomState(long, Room, ActivityLevel)}, by section id (a room, the hub or a tunnel). */
    public static RoomState roomState(long seed, int section, ActivityLevel level) {
        switch (level) {
            case OPERATIONAL:
                return RoomState.STAFFED;
            case PARTLY_ABANDONED:
                return Mix.unit(Mix.of(seed, S_LIGHT, section, -1)) < ActivityProfile.of(level)
                    .litChance() ? RoomState.IN_SERVICE : RoomState.MOTHBALLED;
            default:
                return RoomState.GUTTED;
        }
    }

    /** Share of machines that are husks in a room in service (D309). */
    static final double IN_SERVICE_DEAD = 0.10;
    /** Share of machines that are husks in a mothballed room (D309). */
    static final double MOTHBALLED_DEAD = 0.90;

    /**
     * @return the chance a machine of a room in this state is a husk: none staffed, 10% in service, 90% mothballed, and
     *         the level's (D284) when gutted
     */
    static double deadChance(RoomState state, ActivityProfile p) {
        switch (state) {
            case STAFFED:
                return 0;
            case IN_SERVICE:
                return IN_SERVICE_DEAD;
            case MOTHBALLED:
                return MOTHBALLED_DEAD;
            default:
                return p.deadMachineChance();
        }
    }

    private static void machine(Mark m, RoomState state, ActivityProfile p, long seed, int ceiling,
        List<Placement> out) {
        MachineRole role;
        try {
            role = MachineRole.valueOf(m.key());
        } catch (IllegalArgumentException e) {
            return; // an unknown role is left empty rather than guessed
        }
        long cell = m.x() * 4096L + m.z();
        long h = Mix.of(seed, S_MACHINE, cell, m.y());
        // Only a gutted room has machines stripped to the floor (D284); a mothballed one keeps its hulls.
        if (state == RoomState.GUTTED && Mix.unit(Mix.of(seed, S_ABSENT, cell, m.y())) < p.absentMachineChance()) {
            return;
        }
        boolean dead = Mix.unit(Mix.of(seed, S_DEAD, cell, m.y())) < deadChance(state, p);
        boolean stocked = !dead && Mix.unit(Mix.of(seed, S_STOCK, cell, m.y())) < STOCKED[state.ordinal()];
        out.add(
            Placement.at(Kind.MACHINE, m.x(), m.y(), m.z())
                .machine(role, machineTier(role, ceiling, h), dead)
                .stocked(stocked)
                .variant((int) (Mix.of(seed, S_VARIANT, cell, m.y()) >>> 33))
                .front(m.facing())
                .build());
    }

    /**
     * @param role    the machine role
     * @param ceiling the ceiling
     * @param h       a hash of the cell
     * @return the voltage index asked for: 0 for roles with no tier; the radar and comms gear at the ceiling or one or
     *         two below it, at least HV (3); every other room machine from the same tier ladder as the metal props
     *         (D330, after D308): LV and MV common, the ceiling's own tier a find. Never above the ceiling. The world
     *         side steps down further when GT has no machine that high.
     */
    public static int machineTier(MachineRole role, int ceiling, long h) {
        switch (role) {
            case NASA_WORKBENCH:
            case LAUNCH_CONTROL:
            case MAGAZINE_CASING:
                return 0;
            case RADAR:
            case COMMS:
                int below = (int) Long.remainderUnsigned(h, 3);
                return Math.max(Math.min(3, ceiling), ceiling - below);
            default:
                return roomMachineTier(ceiling, h);
        }
    }

    /**
     * The balance pass's limit on the free machines of a complex (D330): a room machine's tier is drawn from the metal
     * props' ladder ({@link com.gregguns.icbm.complex.gen.MetalProps#weight}), so most are LV and MV and the ceiling's
     * own tier is a find, while the room keeps every machine its activity level gives it.
     *
     * @param ceiling the complex's ceiling (voltage index)
     * @param h       a hash of the cell
     * @return 1 to the ceiling
     */
    static int roomMachineTier(int ceiling, long h) {
        int c = Math.max(1, ceiling);
        int total = 0;
        for (int t = 1; t <= c; t++) total += com.gregguns.icbm.complex.gen.MetalProps.weight(t, c);
        if (total <= 0) return c;
        long r = Long.remainderUnsigned(h, total);
        for (int t = 1; t <= c; t++) {
            r -= com.gregguns.icbm.complex.gen.MetalProps.weight(t, c);
            if (r < 0) return t;
        }
        return c;
    }

    /** @return depth band 0 (the hall) to 3 from the box's distance to the hall, 30 blocks per band */
    static int depthBand(Box b, Box hall) {
        int dx = Math.max(0, Math.max(hall.minX - (b.maxX - 1), b.minX - (hall.maxX - 1)));
        int dz = Math.max(0, Math.max(hall.minZ - (b.maxZ - 1), b.minZ - (hall.maxZ - 1)));
        int d = Math.max(dx, dz);
        if (d == 0) {
            return 0;
        }
        return 1 + Math.min(2, d / 30);
    }

    /**
     * @param ceiling the complex's ceiling (EV 4 up)
     * @return the missile tier of the salvaged body: the planet's rocket tier, which the ceiling follows (EV is tier 1
     *         and tier 2 is IV), capped at 3
     */
    public static int bodyTier(int ceiling) {
        return Math.max(1, Math.min(3, ceiling - 3));
    }

    /** Complex-wide bonus items, each carried by one hashed chest of a fitting key. */
    private static void bonuses(ComplexPlan plan, ActivityProfile p, long seed, int[] other, int ceiling,
        List<Placement> chests) {
        for (Placement c : chests) {
            String key = c.key();
            c.setCeiling(ceiling);
            if ("fuel".equals(key)) {
                c.addBonus(Bonus.FUEL_CELLS);
            } else if ("salvage".equals(key)) {
                if (p.level() != ActivityLevel.OPERATIONAL || Mix.unit(Mix.of(seed, S_ROLL, c.chestIndex(), 1)) < 0.5) {
                    c.addBonus(Bonus.DAMAGED_BODY);
                }
            } else if ("magazine".equals(key)) {
                c.addBonus(Bonus.CASING_PLATES);
            }
        }
        // one-off carriers
        carry(chests, seed, 10, p.chipChance(), Bonus.SCHEMATIC_CHIP, "safe", "comms", "control");
        if (other != null) {
            Placement stick = carry(chests, seed, 11, p.chipChance() / 2, Bonus.DATA_STICK, "comms");
            if (stick != null) {
                stick.setExtra(other[0] + "," + other[1]);
            }
        }
        int i = 0;
        for (SiloSlot s : plan.silos()) {
            Bonus part = s.missing() == com.gregguns.icbm.complex.MissingPart.CONTROLLER ? Bonus.SILO_CONTROLLER
                : Bonus.SILO_ENERGY_HATCH;
            carry(chests, seed, 20 + i++, p.siloPartChance(), part, "safe", "stockpile", "workshop", "control");
        }
    }

    private static Placement carry(List<Placement> chests, long seed, int salt, double chance, Bonus b,
        String... keys) {
        if (Mix.unit(Mix.of(seed, S_CARRIER, salt, 0)) >= chance) {
            return null;
        }
        List<Placement> fit = new ArrayList<>();
        for (Placement c : chests) {
            for (String k : keys) {
                if (k.equals(c.key())) {
                    fit.add(c);
                    break;
                }
            }
        }
        if (fit.isEmpty()) {
            return null;
        }
        Placement pick = fit.get((int) Long.remainderUnsigned(Mix.of(seed, S_CARRIER, salt, 1), fit.size()));
        pick.addBonus(b);
        return pick;
    }
}
