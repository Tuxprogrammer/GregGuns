package com.gregguns.icbm.complex;

import java.util.Collections;
import java.util.List;

/** A complete silo complex plan in complex-local coordinates (x, z in [0, SIZE), y relative to the level floor). */
public final class ComplexPlan {

    private final long seed;
    private final int cellX, cellZ;
    private final Room hall;
    private final List<Room> rooms;
    private final List<Tunnel> tunnels;
    private final List<SiloSlot> silos;
    private final int escapeX, escapeZ;
    private final List<int[]> radarStations;

    ComplexPlan(long seed, int cellX, int cellZ, Room hall, List<Room> rooms, List<Tunnel> tunnels,
        List<SiloSlot> silos, int escapeX, int escapeZ, List<int[]> radarStations) {
        this.seed = seed;
        this.cellX = cellX;
        this.cellZ = cellZ;
        this.hall = hall;
        this.rooms = Collections.unmodifiableList(rooms);
        this.tunnels = Collections.unmodifiableList(tunnels);
        this.silos = Collections.unmodifiableList(silos);
        this.escapeX = escapeX;
        this.escapeZ = escapeZ;
        this.radarStations = Collections.unmodifiableList(radarStations);
    }

    public long seed() {
        return seed;
    }

    public int cellX() {
        return cellX;
    }

    public int cellZ() {
        return cellZ;
    }

    /** The assembly bay. Not in {@link #rooms()}. */
    public Room hall() {
        return hall;
    }

    /** Wing and annex rooms, hall excluded. */
    public List<Room> rooms() {
        return rooms;
    }

    public List<Tunnel> tunnels() {
        return tunnels;
    }

    public List<SiloSlot> silos() {
        return silos;
    }

    /** The 1x1 escape ladder column. */
    public int escapeX() {
        return escapeX;
    }

    public int escapeZ() {
        return escapeZ;
    }

    /** Radar station surface positions as {x, z}. */
    public List<int[]> radarStations() {
        return radarStations;
    }

    /** All marks of rooms (hall included) and tunnels. */
    public List<Mark> allMarks() {
        List<Mark> out = new java.util.ArrayList<Mark>(hall.marks());
        for (Room r : rooms) out.addAll(r.marks());
        for (Tunnel t : tunnels) out.addAll(t.marks());
        return out;
    }
}
