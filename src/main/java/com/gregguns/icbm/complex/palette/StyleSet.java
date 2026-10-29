package com.gregguns.icbm.complex.palette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One look: several wall choices, floor and ceiling designs, a lamp pair, an accent and furniture. Immutable. */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Style sets); D240
public final class StyleSet {

    public final String id;
    public final Set<StyleGroup> groups;
    public final List<BlockRef> walls;
    public final List<CellPattern> floors;
    public final List<CellPattern> ceilings;
    public final BlockRef lampLit;
    public final BlockRef lampUnlit;
    /** Null for none. */
    public final BlockRef accent;
    /** Swapped into an abandoned room's floor here and there; null for none. */
    public final BlockRef worn;
    public final Map<Furniture, BlockRef> furniture;

    private StyleSet(Builder b) {
        this.id = b.id;
        this.groups = Collections.unmodifiableSet(b.groups);
        this.walls = Collections.unmodifiableList(b.walls);
        this.floors = Collections.unmodifiableList(b.floors);
        this.ceilings = Collections.unmodifiableList(b.ceilings);
        this.lampLit = b.lampLit;
        this.lampUnlit = b.lampUnlit;
        this.accent = b.accent;
        this.worn = b.worn;
        this.furniture = Collections.unmodifiableMap(new EnumMap<>(b.furniture));
    }

    /** @return a builder for set {@code id} */
    public static Builder named(String id, StyleGroup first, StyleGroup... more) {
        return new Builder(id, EnumSet.of(first, more));
    }

    /** Builds a {@link StyleSet}. */
    public static final class Builder {

        private final String id;
        private final Set<StyleGroup> groups;
        private final List<BlockRef> walls = new ArrayList<>();
        private final List<CellPattern> floors = new ArrayList<>();
        private final List<CellPattern> ceilings = new ArrayList<>();
        private BlockRef lampLit;
        private BlockRef lampUnlit;
        private BlockRef accent;
        private BlockRef worn;
        private final Map<Furniture, BlockRef> furniture = new EnumMap<>(Furniture.class);

        private Builder(String id, Set<StyleGroup> groups) {
            this.id = id;
            this.groups = groups;
        }

        public Builder walls(BlockRef... w) {
            Collections.addAll(walls, w);
            return this;
        }

        public Builder floors(CellPattern... f) {
            Collections.addAll(floors, f);
            return this;
        }

        public Builder ceilings(CellPattern... c) {
            Collections.addAll(ceilings, c);
            return this;
        }

        public Builder lamp(BlockRef lit, BlockRef unlit) {
            this.lampLit = lit;
            this.lampUnlit = unlit;
            return this;
        }

        public Builder accent(BlockRef a) {
            this.accent = a;
            return this;
        }

        public Builder worn(BlockRef w) {
            this.worn = w;
            return this;
        }

        public Builder furniture(Furniture f, BlockRef r) {
            furniture.put(f, r);
            return this;
        }

        public StyleSet build() {
            if (walls.isEmpty() || floors.isEmpty() || ceilings.isEmpty() || lampLit == null || lampUnlit == null) {
                throw new IllegalStateException("incomplete style set " + id);
            }
            return new StyleSet(this);
        }
    }
}
