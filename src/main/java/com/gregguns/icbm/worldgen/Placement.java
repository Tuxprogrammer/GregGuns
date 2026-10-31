package com.gregguns.icbm.worldgen;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.gregguns.icbm.complex.Box;
import com.gregguns.icbm.complex.MachineRole;

/**
 * One decision of the filler for one cell (or one box) of a complex, in complex-local coordinates. The activity pass
 * turns the layout's marks into these; the world side turns each into blocks inside the populate window. Its position,
 * kind and machine data are fixed at
 * build time; only a chest's bonuses, its data stick payload and its complex's ceiling are set afterwards, by the
 * contents pass, before the list is handed out.
 *
 * <p>
 * A mark with no placement is simply left as the shell built it (air for a light, an intact wall for a damage cell).
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/activity-levels.md (Placements)
public final class Placement {

    /** What to build. */
    public enum Kind {
        /** Remove the block (a breach in a wall, a hole in a floor or roof). */
        AIR,
        /** A rubble block. */
        RUBBLE,
        /** A water block (the flooded pump room). */
        WATER,
        /** A loot chest; {@link #category()} names its table. */
        CHEST,
        /** A wall of item frames, each holding a gun drawn from {@link #category()}. */
        FRAME_WALL,
        /** A silo guard spawner of {@link #spawner()}. */
        SPAWNER,
        /** A machine for {@link #role()} at {@link #tier()}; {@link #dead()} makes it a husk. */
        MACHINE,
        /** A lit light fixture. */
        LIGHT,
        /** A cave-in: fill part of {@link #box()} with rubble. */
        COLLAPSE,
        /**
         * A wooden crate with no inventory: a chest spot the chest budget left over (D315). {@link #key()} is the wood
         * (0 to 5) and {@link #facing()} the way the spot faces.
         */
        CRATE,
        /**
         * One block by registry name, {@link #block()} at {@link #meta()}: a room's look at its activity level (D309),
         * written over what the marks pass put there (a cold furnace, a crop, coarse dirt, a metal prop, a rubble
         * pile). A name that does not resolve this run tries {@link #fallback()}, then writes nothing.
         */
        BLOCK
    }

    /** Extra things a chest may hold beyond its table (D17, D36, D128). */
    public enum Bonus {
        /** A damaged missile body of the planet's rocket tier (at most 3), for GT recycling. */
        DAMAGED_BODY,
        /** Cells of rocket fuel, the best rung the ceiling allows (the fuel depot's stock). */
        FUEL_CELLS,
        /** The rare Tier 1 missile schematic chip. */
        SCHEMATIC_CHIP,
        /** The Missile Silo controller the silo is missing. */
        SILO_CONTROLLER,
        /** The Energy Hatch the silo is missing. */
        SILO_ENERGY_HATCH,
        /** A data stick holding another complex's coordinates; see {@link #extra()}. */
        DATA_STICK,
        /** Stainless Steel plates, the stock of a Tier 1 warhead casing (inert; never a warhead, D17). */
        CASING_PLATES
    }

    private final Kind kind;
    private final int x;
    private final int y;
    private final int z;
    private final String category;
    private final com.gregguns.worldgen.spawner.SpawnerKind spawner;
    private final MachineRole role;
    private final int tier;
    private final boolean dead;
    private final int chestIndex;
    private final int richness;
    private final RoomState roomState;
    private final Set<Bonus> bonuses = EnumSet.noneOf(Bonus.class);
    private String extra;
    private int ceiling = SiloCeiling.EV;
    private final String key;
    private final Box box;
    private final int facing;
    private final int extent;
    private final int variant;
    private final String block;
    private final int meta;
    private final String fallback;

    private Placement(Builder b) {
        this.kind = b.kind;
        this.x = b.x;
        this.y = b.y;
        this.z = b.z;
        this.category = b.category;
        this.spawner = b.spawner;
        this.role = b.role;
        this.tier = b.tier;
        this.dead = b.dead;
        this.chestIndex = b.chestIndex;
        this.richness = b.richness;
        this.roomState = b.roomState;
        this.bonuses.addAll(b.bonuses);
        this.extra = b.extra;
        this.key = b.key;
        this.box = b.box;
        this.facing = b.facing;
        this.extent = b.extent;
        this.variant = b.variant;
        this.block = b.block;
        this.meta = b.meta;
        this.fallback = b.fallback;
    }

    static Builder at(Kind kind, int x, int y, int z) {
        return new Builder(kind, x, y, z);
    }

    /** @return what to build */
    public Kind kind() {
        return kind;
    }

    /** @return complex-local X */
    public int x() {
        return x;
    }

    /** @return complex-local Y (0 is the level floor) */
    public int y() {
        return y;
    }

    /** @return complex-local Z */
    public int z() {
        return z;
    }

    /** @return the loot category name of a chest or frame wall, else null */
    public String category() {
        return category;
    }

    /** @return the spawner kind of a spawner, else null */
    public com.gregguns.worldgen.spawner.SpawnerKind spawner() {
        return spawner;
    }

    /** @return the role of a machine, else null */
    public MachineRole role() {
        return role;
    }

    /** @return the GT voltage index of a machine, never above the ceiling; 0 for the rest */
    public int tier() {
        return tier;
    }

    /**
     * @return for a machine, which of its role's machines comes first
     *         ({@link MachinePicker#pick(MachineRole, int, boolean, int)})
     */
    public int variant() {
        return variant;
    }

    /** @return the registry name ({@code mod:name}) of a {@link Kind#BLOCK}, else null */
    public String block() {
        return block;
    }

    /** @return the metadata of a {@link Kind#BLOCK} */
    public int meta() {
        return meta;
    }

    /** @return the registry name a {@link Kind#BLOCK} falls back to (at meta 0) when its own is absent, or null */
    public String fallback() {
        return fallback;
    }

    /** @return true for a machine that is a dead husk */
    public boolean dead() {
        return dead;
    }

    /** @return the chest's stable index inside the complex, for its loot seed; -1 for the rest */
    public int chestIndex() {
        return chestIndex;
    }

    /** @return chest richness 0 to 2 */
    public int richness() {
        return richness;
    }

    /** @return the state of the room a loot chest stands in, or null for a chest placed without one */
    public RoomState roomState() {
        return roomState;
    }

    /** @return the extra items a chest holds; never null, read only */
    public Set<Bonus> bonuses() {
        return Collections.unmodifiableSet(bonuses);
    }

    void addBonus(Bonus b) {
        bonuses.add(b);
    }

    void setExtra(String v) {
        extra = v;
    }

    /** @return the ceiling of the chest's complex, which its bonus items scale with; EV until the contents pass says */
    public int ceiling() {
        return ceiling;
    }

    void setCeiling(int v) {
        ceiling = v;
    }

    /** @return the layout's loot key of a chest or frame wall ({@code safe}, {@code armory} and so on), else null */
    public String key() {
        return key;
    }

    /** @return a free text payload: for a data stick, the other complex's cell as {@code cellX,cellZ}; else null */
    public String extra() {
        return extra;
    }

    /** @return the box of a collapse, else null */
    public Box box() {
        return box;
    }

    /** @return the hanging direction of a frame wall (0 south, 1 west, 2 north, 3 east), else 0 */
    public int facing() {
        return facing;
    }

    /** @return the side a machine's front prefers: 0 north, 1 east, 2 south, 3 west (the machine's room door side) */
    public int front() {
        return facing;
    }

    /** @return the frame count of a frame wall, one every second cell along it (+X or +Z), else 1 */
    public int extent() {
        return extent;
    }

    @Override
    public String toString() {
        return kind + "@"
            + x
            + ","
            + y
            + ","
            + z
            + (category != null ? " " + category : "")
            + (spawner != null ? " " + spawner : "")
            + (role != null ? " " + role + "/" + tier + (dead ? " dead" : "") : "")
            + (block != null ? " " + block + ":" + meta : "");
    }

    /** Builds a placement. */
    static final class Builder {

        private final Kind kind;
        private final int x;
        private final int y;
        private final int z;
        private String category;
        private com.gregguns.worldgen.spawner.SpawnerKind spawner;
        private MachineRole role;
        private int tier;
        private boolean dead;
        private int chestIndex = -1;
        private int richness;
        private RoomState roomState;
        private final Set<Bonus> bonuses = EnumSet.noneOf(Bonus.class);
        private String extra;
        private String key;
        private Box box;
        private int facing;
        private int extent = 1;
        private int variant;
        private String block;
        private int meta;
        private String fallback;

        private Builder(Kind kind, int x, int y, int z) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Builder category(String v) {
            category = v;
            return this;
        }

        Builder spawner(com.gregguns.worldgen.spawner.SpawnerKind v) {
            spawner = v;
            return this;
        }

        Builder machine(MachineRole r, int t, boolean d) {
            role = r;
            tier = t;
            dead = d;
            return this;
        }

        Builder chest(int index, int rich) {
            chestIndex = index;
            richness = rich;
            return this;
        }

        /** A loot chest of a room in {@code state}: its richness follows the state ({@link RoomLootDraw}). */
        Builder chest(int index, RoomState state) {
            chestIndex = index;
            roomState = state;
            richness = RoomLootDraw.profile(state)
                .richness();
            return this;
        }

        Builder bonus(Bonus b) {
            bonuses.add(b);
            return this;
        }

        Builder key(String v) {
            key = v;
            return this;
        }

        Builder extra(String v) {
            extra = v;
            return this;
        }

        Builder frames(int facingDir, int count) {
            facing = facingDir;
            extent = count;
            return this;
        }

        Builder front(int compassIndex) {
            facing = compassIndex;
            return this;
        }

        Builder variant(int v) {
            variant = v;
            return this;
        }

        Builder block(String ref, int m, String fallbackRef) {
            block = ref;
            meta = m;
            fallback = fallbackRef;
            return this;
        }

        Builder box(Box v) {
            box = v;
            return this;
        }

        Placement build() {
            return new Placement(this);
        }
    }
}
