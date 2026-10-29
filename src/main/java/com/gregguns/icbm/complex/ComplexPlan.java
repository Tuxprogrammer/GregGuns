package com.gregguns.icbm.complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A complete silo complex plan in complex-local coordinates (x, z in [0, SIZE), y relative to the level floor). Its
 * sections are the hub, the rooms and the halls, each with its own id: the hub is 0, the others in the order they
 * grew (the major halls first, then rooms and minor hallways interleaved). Nothing relies on that order.
 */
public final class ComplexPlan {

    private final long seed;
    private final int cellX, cellZ;
    private final Room hall;
    private final List<Room> rooms;
    private final List<Tunnel> tunnels;
    private final List<SiloSlot> silos;
    private final List<Door> doors;
    private final int escapeX, escapeZ, escapeMeta, escapeTop;
    private final List<int[]> radarStations;
    private final Object[] byId;

    ComplexPlan(long seed, int cellX, int cellZ, Room hall, List<Room> rooms, List<Tunnel> tunnels,
        List<SiloSlot> silos, List<Door> doors, int[] escape, List<int[]> radarStations) {
        this.seed = seed;
        this.cellX = cellX;
        this.cellZ = cellZ;
        this.hall = hall;
        this.rooms = Collections.unmodifiableList(rooms);
        this.tunnels = Collections.unmodifiableList(tunnels);
        this.silos = Collections.unmodifiableList(silos);
        this.doors = Collections.unmodifiableList(doors);
        this.escapeX = escape[0];
        this.escapeZ = escape[1];
        this.escapeMeta = escape[2];
        this.escapeTop = escape[3];
        this.radarStations = Collections.unmodifiableList(radarStations);
        int n = 1 + rooms.size() + tunnels.size();
        byId = new Object[n];
        byId[hall.id()] = hall;
        for (Room r : rooms) byId[r.id()] = r;
        for (Tunnel t : tunnels) byId[t.id()] = t;
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

    /** The hub (the assembly bay), section 0. Not in {@link #rooms()}. */
    public Room hall() {
        return hall;
    }

    /** Every room, the hub excluded. */
    public List<Room> rooms() {
        return rooms;
    }

    /** The major halls and minor hallways. */
    public List<Tunnel> tunnels() {
        return tunnels;
    }

    public List<SiloSlot> silos() {
        return silos;
    }

    /** Every opening between two sections, and each silo's walkway. */
    public List<Door> doors() {
        return doors;
    }

    /** @return how many sections the plan has (ids 0 to this, exclusive) */
    public int sectionCount() {
        return byId.length;
    }

    /** @return the {@link Room} (the hub included) or {@link Tunnel} with the id */
    public Object section(int id) {
        return byId[id];
    }

    /** @return the box of a section */
    public Box sectionBox(int id) {
        Object o = byId[id];
        return o instanceof Room ? ((Room) o).bounds() : ((Tunnel) o).bounds();
    }

    /** The 1x1 escape ladder column. */
    public int escapeX() {
        return escapeX;
    }

    public int escapeZ() {
        return escapeZ;
    }

    /** Metadata of the escape ladder blocks: 2 to 5, clinging to the solid wall behind the column. */
    public int escapeMeta() {
        return escapeMeta;
    }

    /** @return the height of the section the ladder stands in: the shaft above the level starts at this layer */
    public int escapeTop() {
        return escapeTop;
    }

    /** Radar station surface positions as {x, z}. */
    public List<int[]> radarStations() {
        return radarStations;
    }

    /** All marks of rooms (hall included) and tunnels. */
    public List<Mark> allMarks() {
        List<Mark> out = new ArrayList<Mark>(hall.marks());
        for (Room r : rooms) out.addAll(r.marks());
        for (Tunnel t : tunnels) out.addAll(t.marks());
        return out;
    }
}
