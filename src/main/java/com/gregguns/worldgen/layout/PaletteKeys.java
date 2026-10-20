package com.gregguns.worldgen.layout;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import net.minecraft.block.Block;

import cpw.mods.fml.common.Loader;

/**
 * Palette keys: the role names layouts use ({@code concrete}, {@code factory_wall}, ...) mapped once, in postInit, to a
 * concrete block and meta (docs/gregguns-spec/11-worldgen/chisel-palette.md).
 *
 * <p>
 * This is the only class that names {@code team.chisel.*} (GGS-11-25; a source-scan test enforces it). Chisel is a
 * soft dependency, so the class is read by name through reflection and Chisel never needs to be on the classpath: with
 * Chisel absent, or a field still null, the key resolves to its vanilla fallback and a null logs one warning, never an
 * exception (GGS-11-26, 27, WGEN-24).
 *
 * <p>
 * A layout may also name any block directly as {@code namespace:name} (for example {@code minecraft:iron_bars}); those
 * resolve lazily through the same lookup and are checked up front by {@link LayoutRegistry}. {@code air} is a built-in
 * key.
 */
public final class PaletteKeys {

    /** Reads a Chisel block field by name; returns null when Chisel or the field is absent or unfilled. */
    public interface ChiselLookup {

        Block field(String name);
    }

    /** Finds a block by registry name ({@code minecraft:stonebrick}); null when unknown. */
    public interface BlockLookup {

        Block byName(String registryName);
    }

    // Source: Chisel 2.17.34-GTNH declares each of these as a `public static final` null placeholder that
    // @ObjectHolder fills at registration:
    // https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/init/ChiselBlocks.java#L38
    // (class + ObjectHolder), #L57 concrete, #L70 stonebricksmooth, #L95 factoryblock, #L100 laboratoryblock,
    // #L110 hexPlating, #L112 technical, #L117 warningSign, #L126 leadblock, #L181 lantern, #L54 glass_pane;
    // copperblock, tinblock, iron_block and gold_block are declared in the same file.
    // The Chisel metas below are the first variation of each block; they are cosmetic and unverified in game.
    private static final String CHISEL_BLOCKS_CLASS = "team.chisel.init.ChiselBlocks";

    /** Role key, Chisel field (null for vanilla-only keys), Chisel meta, vanilla fallback name and meta. */
    private static final String[][] TABLE = {
        // key, chiselField, chiselMeta, vanillaName, vanillaMeta
        // Walls avoid raw stone and cobblestone: the GT ore pass may overwrite those (open question Q4).
        { "concrete", "concrete", "0", "minecraft:stonebrick", "0" },
        { "brick", "stonebricksmooth", "0", "minecraft:stonebrick", "0" },
        { "factory_wall", "factoryblock", "0", "minecraft:iron_block", "0" },
        { "factory_floor", "technical", "0", "minecraft:double_stone_slab", "8" },
        { "lab_wall", "laboratoryblock", "0", "minecraft:quartz_block", "0" },
        { "plating", "hexPlating", "0", "minecraft:iron_block", "0" },
        { "hazard", "warningSign", "0", "minecraft:stained_hardened_clay", "4" },
        { "lantern", "lantern", "0", "minecraft:glowstone", "0" },
        { "glass_pane", "glass_pane", "0", "minecraft:glass_pane", "0" },
        { "lead_block", "leadblock", "0", "minecraft:iron_block", "0" },
        { "copper_block", "copperblock", "0", "minecraft:stained_hardened_clay", "1" },
        { "tin_block", "tinblock", "0", "minecraft:iron_block", "0" },
        { "iron_plate", "iron_block", "0", "minecraft:iron_block", "0" },
        { "gold_plate", "gold_block", "0", "minecraft:gold_block", "0" },
        // Vanilla-only roles.
        { "air", null, "0", "minecraft:air", "0" }, };

    private final Map<String, ResolvedBlock> roles;
    private final BlockLookup vanilla;
    private final Map<String, ResolvedBlock> direct = new ConcurrentHashMap<>();
    private final Consumer<String> warn;

    private PaletteKeys(Map<String, ResolvedBlock> roles, BlockLookup vanilla, Consumer<String> warn) {
        this.roles = Collections.unmodifiableMap(roles);
        this.vanilla = vanilla;
        this.warn = warn;
    }

    /**
     * Resolves every role key. {@code chisel} null means Chisel is absent. A vanilla block that does not resolve
     * either drops the key (logged), and a layout using it then fails validation instead of crashing worldgen.
     */
    public static PaletteKeys resolve(ChiselLookup chisel, BlockLookup vanilla, Consumer<String> warn) {
        Map<String, ResolvedBlock> out = new LinkedHashMap<>();
        for (String[] row : TABLE) {
            String key = row[0];
            Block block = null;
            boolean fallback = row[1] != null;
            int meta = Integer.parseInt(row[4]);
            if (chisel != null && row[1] != null) {
                block = chisel.field(row[1]);
                if (block == null) {
                    warn.accept("Palette key '" + key + "': Chisel block '" + row[1] + "' is null; using " + row[3]);
                } else {
                    fallback = false;
                    meta = Integer.parseInt(row[2]);
                }
            }
            if (block == null) {
                block = vanilla.byName(row[3]);
            }
            if (block == null) {
                warn.accept("Palette key '" + key + "': fallback block " + row[3] + " not found; key unavailable");
                continue;
            }
            out.put(key, new ResolvedBlock(block, meta, fallback));
        }
        return new PaletteKeys(out, vanilla, warn);
    }

    /** The postInit entry point: Chisel is read only when loaded, vanilla blocks come from the block registry. */
    public static PaletteKeys resolveForPack(Consumer<String> warn) {
        ChiselLookup chisel = Loader.isModLoaded("chisel") ? PaletteKeys::readChiselField : null;
        return resolve(chisel, Block::getBlockFromName, warn);
    }

    private static Block readChiselField(String name) {
        try {
            Field f = Class.forName(CHISEL_BLOCKS_CLASS)
                .getField(name);
            Object v = f.get(null);
            return v instanceof Block ? (Block) v : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    /** Resolved block for a role key or a {@code namespace:name} direct key; null if neither resolves. */
    public ResolvedBlock get(String key) {
        ResolvedBlock r = roles.get(key);
        if (r != null || key.indexOf(':') < 0) {
            return r;
        }
        r = direct.get(key);
        if (r == null) {
            Block b = vanilla.byName(key);
            if (b == null) {
                return null;
            }
            r = new ResolvedBlock(b, 0, false);
            direct.put(key, r);
        }
        return r;
    }

    public boolean has(String key) {
        return get(key) != null;
    }

    /** Role key names that resolved, for diagnostics. */
    public Iterable<String> roleKeys() {
        return roles.keySet();
    }

    /** Number of role keys that fell back to a vanilla block. */
    public int fallbackCount() {
        int n = 0;
        for (ResolvedBlock r : roles.values()) {
            if (r.fallback) {
                n++;
            }
        }
        return n;
    }
}
