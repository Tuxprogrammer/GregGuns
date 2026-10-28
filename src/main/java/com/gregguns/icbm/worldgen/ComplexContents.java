package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.List;

import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.ComplexPlan;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.icbm.complex.Mark;
import com.gregguns.icbm.complex.Room;
import com.gregguns.icbm.complex.RoomKind;
import com.gregguns.icbm.complex.SiloSlot;
import com.gregguns.icbm.complex.Tunnel;
import com.gregguns.icbm.worldgen.Placement.Bonus;
import com.gregguns.icbm.worldgen.Placement.Kind;
import com.gregguns.worldgen.spawner.HoleKind;

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
        S_MACHINE = 0x3AC, S_DEAD = 0xDEAD, S_CARRIER = 0xCA2, S_ROLL = 0x201, S_RUBBLE = 0x2B1;

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
        Box hall = plan.hall()
            .bounds();

        mark(plan.hall(), hall, false, p, seed, ceiling, hall, out, chests, chestCounter, level);
        for (Room r : plan.rooms()) {
            boolean collapsed = r.kind() != RoomKind.GUARD_POST && r.kind() != RoomKind.DECON_AIRLOCK
                && Mix.unit(Mix.of(seed, S_COLLAPSE, r.id(), 0)) < p.collapseChance();
            if (collapsed) {
                collapses.add(
                    Placement.at(Kind.COLLAPSE, r.bounds().minX, r.bounds().minY, r.bounds().minZ)
                        .box(r.bounds())
                        .build());
            }
            mark(r, r.bounds(), collapsed, p, seed, ceiling, hall, out, chests, chestCounter, level);
        }
        for (Tunnel t : plan.tunnels()) {
            marks(t.marks(), t.bounds(), "tunnel", false, p, seed, ceiling, hall, out, chests, chestCounter, level);
        }
        bonuses(plan, p, seed, other, chests);
        List<Placement> all = new ArrayList<>(collapses);
        all.addAll(out);
        return keepFrameWalls(all);
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
                int x = f.x() + (alongZ ? 0 : 2 * i);
                int z = f.z() + (alongZ ? 2 * i : 0);
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

    private static void mark(Room r, Box bounds, boolean collapsed, ActivityProfile p, long seed, int ceiling, Box hall,
        List<Placement> out, List<Placement> chests, int[] counter, ActivityLevel level) {
        marks(
            r.marks(),
            bounds,
            r.kind() == null ? ""
                : r.kind()
                    .name(),
            collapsed,
            p,
            seed,
            ceiling,
            hall,
            out,
            chests,
            counter,
            level);
    }

    private static void marks(List<Mark> marks, Box bounds, String owner, boolean collapsed, ActivityProfile p,
        long seed, int ceiling, Box hall, List<Placement> out, List<Placement> chests, int[] counter,
        ActivityLevel level) {
        int band = depthBand(bounds, hall);
        boolean controlRoom = "LAUNCH_CONTROL".equals(owner);
        for (Mark m : marks) {
            long h = Mix.of(seed, S_CELL, m.x() * 4096L + m.z(), m.y());
            double u = Mix.unit(h);
            switch (m.kind()) {
                case LOOT_CHEST: {
                    int index = counter[0]++;
                    Placement c = Placement.at(Kind.CHEST, m.x(), m.y(), m.z())
                        .category(SiloLootTables.categoryName(m.key(), ceiling))
                        .key(m.key())
                        .chest(index, p.richness())
                        .build();
                    chests.add(c);
                    out.add(c);
                    break;
                }
                case ITEM_FRAME_WALL:
                    out.add(
                        Placement.at(Kind.FRAME_WALL, m.x(), m.y(), m.z())
                            .category(SiloLootTables.categoryName(m.key(), ceiling))
                            .key(m.key())
                            .frames(m.facing(), m.extent())
                            .build());
                    break;
                case LIGHT:
                    if (Mix.unit(Mix.of(seed, S_LIGHT, m.x() * 4096L + m.z(), m.y())) < p.litChance()) {
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
                    spawner(m, band, controlRoom, p, seed, out);
                    break;
                case MACHINE:
                    machine(m, p, seed, ceiling, out);
                    break;
                default:
                    break; // signs and blast doors are the layout's own
            }
        }
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

    private static void spawner(Mark m, int band, boolean controlRoom, ActivityProfile p, long seed,
        List<Placement> out) {
        double chance = p.spawnerChance(band);
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
                .spawner(elite ? HoleKind.SILO_ELITE : HoleKind.SILO_GUARD)
                .build());
    }

    private static void machine(Mark m, ActivityProfile p, long seed, int ceiling, List<Placement> out) {
        MachineRole role;
        try {
            role = MachineRole.valueOf(m.key());
        } catch (IllegalArgumentException e) {
            return; // an unknown role is left empty rather than guessed
        }
        long h = Mix.of(seed, S_MACHINE, m.x() * 4096L + m.z(), m.y());
        boolean dead = Mix.unit(Mix.of(seed, S_DEAD, m.x() * 4096L + m.z(), m.y())) < p.deadMachineChance();
        out.add(
            Placement.at(Kind.MACHINE, m.x(), m.y(), m.z())
                .machine(role, machineTier(role, ceiling, h), dead)
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

    /** Complex-wide bonus items, each carried by one hashed chest of a fitting key. */
    private static void bonuses(ComplexPlan plan, ActivityProfile p, long seed, int[] other, List<Placement> chests) {
        for (Placement c : chests) {
            String key = c.key();
            if ("salvage".equals(key)) {
                if (p.level() != ActivityLevel.OPERATIONAL || Mix.unit(Mix.of(seed, S_ROLL, c.chestIndex(), 1)) < 0.5) {
                    c.addBonus(Bonus.DAMAGED_BODY);
                }
            } else if ("magazine".equals(key)) {
                c.addBonus(Bonus.EMPTY_CASINGS);
                if (Mix.unit(Mix.of(seed, S_ROLL, c.chestIndex(), 2)) < p.intactWarheadChance()) {
                    c.addBonus(Bonus.INTACT_WARHEAD);
                }
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
