package com.gregguns.icbm.complex.palette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A block named by registry id and meta, with a fallback chain: the first entry whose block exists wins, and the last
 * entry is always a vanilla block, so a chain always resolves. Pure: it holds strings only, and nothing resolves at
 * class load (D241). {@link PaletteResolver} looks the entries up on the server at write time.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md; D241
public final class BlockRef {

    /** The mod id vanilla blocks are registered under. */
    public static final String VANILLA = "minecraft";

    /** Accepts a mod id (Forge ids may contain '|') and a registry name (GT and Ztones names carry '.', ':'). */
    private static final Pattern MOD_OK = Pattern.compile("[A-Za-z0-9_|]+");
    private static final Pattern NAME_OK = Pattern.compile("[A-Za-z0-9_.:]+");

    /** One link of a chain. */
    public static final class Entry {

        public final String modId;
        public final String name;
        public final int meta;
        /** Precomputed: the resolver's cache hashes every styled cell's chain (about 150,000 per complex). */
        private final String key;
        private final int hash;

        Entry(String modId, String name, int meta) {
            this.modId = modId;
            this.name = name;
            this.meta = meta;
            this.key = modId + ":" + name + "@" + meta;
            this.hash = key.hashCode();
        }

        /** @return true when the ids look like registry ids and the meta fits 4 bits */
        public boolean wellFormed() {
            return MOD_OK.matcher(modId)
                .matches()
                && NAME_OK.matcher(name)
                    .matches()
                && meta >= 0
                && meta <= 15;
        }

        /** @return {@code mod:name@meta} */
        public String key() {
            return key;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry other = (Entry) o;
            return other.hash == hash && other.key.equals(key);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return key();
        }
    }

    /** Answers whether a block is registered; the writer's answer is the registry, a unit test's is a set. */
    public interface Lookup {

        boolean has(String modId, String name);
    }

    private final List<Entry> chain;
    /** Precomputed, like {@link Entry}'s: a chain is immutable. */
    private final int hash;

    private BlockRef(List<Entry> chain) {
        this.chain = Collections.unmodifiableList(chain);
        this.hash = chain.hashCode();
    }

    /** @return the entry for a block of mod {@code modId} */
    public static Entry e(String modId, String name, int meta) {
        return new Entry(modId, name, meta);
    }

    /** @return a vanilla entry */
    public static Entry v(String name, int meta) {
        return new Entry(VANILLA, name, meta);
    }

    /**
     * @param entries the preferred block first, a vanilla block last
     * @return the chain
     * @throws IllegalArgumentException when the chain is empty or does not end in a vanilla block
     */
    public static BlockRef of(Entry... entries) {
        if (entries.length == 0 || !VANILLA.equals(entries[entries.length - 1].modId)) {
            throw new IllegalArgumentException("a BlockRef chain must end in a vanilla block");
        }
        List<Entry> l = new ArrayList<>();
        Collections.addAll(l, entries);
        return new BlockRef(l);
    }

    /** @return the chain, preferred first */
    public List<Entry> chain() {
        return chain;
    }

    /** @return the same chain with {@code first} tried before every entry */
    public BlockRef preferring(Entry first) {
        List<Entry> l = new ArrayList<>();
        l.add(first);
        l.addAll(chain);
        return new BlockRef(l);
    }

    /**
     * @param lookup what exists
     * @return the first entry that exists; the last (vanilla) entry when none does
     */
    public Entry resolve(Lookup lookup) {
        for (Entry e : chain) {
            if (lookup.has(e.modId, e.name)) {
                return e;
            }
        }
        return chain.get(chain.size() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        return o instanceof BlockRef && ((BlockRef) o).hash == hash && ((BlockRef) o).chain.equals(chain);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return chain.toString();
    }
}
