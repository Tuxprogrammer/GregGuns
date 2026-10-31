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
    private static final long S_COLLAPSE = 0xC011, S_CELL = 0xCE11, S_LIGHT = 0x116, S_SPAWN = 0x59A, S_ELITE = 0xE11,
        S_MACHINE = 0x3AC, S_DEAD = 0xDEAD, S_CARRIER = 0xCA2, S_ROLL = 0x201, S_RUBBLE = 0x2B1, S_ABSENT = 0xAB5,
        S_HOLD = 0x401D, S_HOLD_N = 0x401E, S_VARIANT = 0x7A2;

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
        if (level == ActivityLevel.ABANDONED) {
            holdouts(plan, seed, collapsedRooms, out);
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
        FurnitureWear.room(plan, r.id(), r.kind(), r.marks(), bounds, state, collapsed, seed, ceiling, out);
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
        boolean controlRoom = "LAUNCH_CONTROL".equals(owner);
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
                    spawner(m, band, controlRoom, p, seed, ceiling, out);
                    break;
                case MACHINE:
                    machine(m, state, p, seed, ceiling, out);
                    break;
                case FURNITURE:
                    break; // a room's furniture wears as a whole, in FurnitureWear.room
                default:
                    break; // signs and blast doors are the layout's own
            }
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
     * @param ceiling the complex's ceiling
     * @return the factor guard spawner chances grow by: 1 at EV, 1.5 at the top (UMV), in even steps
     */
    public static double guardScale(int ceiling) {
        int c = Math.max(SiloCeiling.EV, Math.min(SiloCeiling.UMV, ceiling));
        return 1.0 + 0.5 * (c - SiloCeiling.EV) / (SiloCeiling.UMV - SiloCeiling.EV);
    }

    private static void spawner(Mark m, int band, boolean controlRoom, ActivityProfile p, long seed, int ceiling,
        List<Placement> out) {
        double chance = Math.min(1.0, p.spawnerChance(band) * guardScale(ceiling));
        if ("post".equals(m.key()) && p.level() != ActivityLevel.ABANDONED) {
            chance = Math.min(1.0, chance + 0.25);
        }
        if (controlRoom || "deep".equals(m.key())) {
            chance = Math.max(chance, p.shaftSpawnerChance());
        }
        if (Mix.unit(Mix.of(seed, S_SPAWN, m.x() * 4096L + m.z(), m.y())) >= chance) {
            return;
        }
        boolean elite = "deep".equals(m.key()) || "room".equals(m.key()) && band >= 2
            && Mix.unit(Mix.of(seed, S_ELITE, m.x() * 4096L + m.z(), m.y())) < 0.4;
        out.add(
            Placement.at(Kind.SPAWNER, m.x(), m.y(), m.z())
                .spawner(elite ? SpawnerKind.SILO_ELITE : SpawnerKind.SILO_GUARD)
                .build());
    }

    /**
     * The abandoned level's few holdouts (D284): one or two guard spawners at hashed "post" marks (guard posts and
     * corridor junctions), never in a collapsed room, so a ruin still bites a little. Every other spawner mark is
     * silent at this level. The pick is a hash of the plan's seed and each mark's cell, so it is the same in every
     * populate window.
     */
    private static void holdouts(ComplexPlan plan, long seed, java.util.Set<Integer> collapsed, List<Placement> out) {
        List<Mark> pool = new ArrayList<>();
        addPosts(
            plan.hall()
                .marks(),
            pool);
        for (Room r : plan.rooms()) {
            if (!collapsed.contains(r.id())) {
                addPosts(r.marks(), pool);
            }
        }
        for (Tunnel t : plan.tunnels()) {
            addPosts(t.marks(), pool);
        }
        pool.sort(
            java.util.Comparator.comparingDouble(m -> Mix.unit(Mix.of(seed, S_HOLD, m.x() * 4096L + m.z(), m.y()))));
        int n = Math.min(pool.size(), 1 + (int) (Mix.of(seed, S_HOLD_N, 0, 0) & 1L));
        for (int i = 0; i < n; i++) {
            Mark m = pool.get(i);
            out.add(
                Placement.at(Kind.SPAWNER, m.x(), m.y(), m.z())
                    .spawner(SpawnerKind.SILO_GUARD)
                    .build());
        }
    }

    private static void addPosts(List<Mark> marks, List<Mark> pool) {
        for (Mark m : marks) {
            if (m.kind() == com.gregguns.icbm.complex.MarkKind.SPAWNER && "post".equals(m.key())) {
                pool.add(m);
            }
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
        out.add(
            Placement.at(Kind.MACHINE, m.x(), m.y(), m.z())
                .machine(role, machineTier(role, ceiling, h), dead)
                .variant((int) (Mix.of(seed, S_VARIANT, cell, m.y()) >>> 33))
                .front(m.facing())
                .build());
    }

    /**
     * @param role    the machine role
     * @param ceiling the ceiling
     * @param h       a hash of the cell
     * @return the voltage index asked for: 0 for roles with no tier, else the ceiling or one or two below it, at least
     *         HV (3); never above the ceiling. The world side steps down further when GT has no machine that high.
     */
    public static int machineTier(MachineRole role, int ceiling, long h) {
        switch (role) {
            case NASA_WORKBENCH:
            case LAUNCH_CONTROL:
            case MAGAZINE_CASING:
                return 0;
            default:
                int below = (int) Long.remainderUnsigned(h, 3);
                return Math.max(Math.min(3, ceiling), ceiling - below);
        }
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
