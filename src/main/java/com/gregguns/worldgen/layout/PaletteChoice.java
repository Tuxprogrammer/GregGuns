package com.gregguns.worldgen.layout;

/** One option of a palette entry: a palette key (or {@code namespace:name}), optional meta, rotation kind, weight. */
public final class PaletteChoice {

    /** Meta value meaning "use the key's own default meta". */
    public static final int KEY_META = -1;

    public final String key;
    public final int meta;
    public final RotationKind kind;
    public final int weight;

    public PaletteChoice(String key, int meta, RotationKind kind, int weight) {
        this.key = key;
        this.meta = meta;
        this.kind = kind;
        this.weight = weight;
    }
}
