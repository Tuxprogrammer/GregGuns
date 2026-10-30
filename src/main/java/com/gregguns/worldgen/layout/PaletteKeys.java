package com.gregguns.worldgen.layout;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import net.minecraft.block.Block;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Palette keys: the role names layouts use ({@code concrete}, {@code factory_wall}, ...) mapped once, in postInit, to a
 * concrete block and meta (docs/gregguns-spec/11-worldgen/chisel-palette.md, design record 0025).
 *
 * <p>
 * Each role is a chain of candidates, best first: a Chisel variant, sometimes a GregTech block, and last a vanilla
 * stand-in. The first candidate that is registered wins. Every candidate of a role is the same look (a gray steel plate
 * stays a gray steel plate down the chain), so a structure keeps its character on a stripped install. Chisel is a soft
 * dependency read by registry name, never by class (GGS-11-25): with Chisel absent, a Chisel block not registered, or
 * a Chisel meta it defines no variation for ({@link ChiselVariants}), the chain moves on; a Chisel miss while Chisel is
 * loaded logs one warning, never an exception (GGS-11-26, 27, WGEN-24).
 *
 * <p>
 * A role key always takes its own meta: a layout may not write {@code concrete#4}, because meta 4 means something else
 * for each block of the chain ({@link LayoutRegistry} rejects it). A different look is a different role. A layout may
 * also name any block directly as {@code namespace:name} (for example {@code minecraft:iron_bars}); those resolve
 * lazily through the same lookup and are checked up front by {@link LayoutRegistry}. {@code air} is a built-in key.
 */
public final class PaletteKeys {

    /** Finds a Chisel block by registry name ({@code factoryblock}); null when Chisel or the block is absent. */
    public interface ChiselLookup {

        Block field(String name);
    }

    /** Finds a block by registry name ({@code minecraft:stonebrick}); null when unknown. */
    public interface BlockLookup {

        Block byName(String registryName);
    }

    private static final String CHISEL = "chisel";

    // Source: Chisel 2.17.34-GTNH registers each block under its carverHelper name, and its variations by meta:
    // https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java
    // concrete L857-L868 (0 default, 1 block, 4 weathered, 10 asphalt); factoryblock L1171-L1187 (1 rust2, 3 platex,
    // 6 hazard, 13 plating); technical L3805-L3821 (13 grate); laboratoryblock L2185-L2201 (11 large
    // steel); stonebricksmooth L3724-L3744 (5 large bricks, 7 chaotic bricks, 12 hard panel); brickCustom L463-L487 (5
    // aged,
    // 13 sooty); glowstone L1563-L1579 (4 sulphur neon panel); glass_pane L1398-L1406 (2 no border); nation and rebel
    // L2784-L2804 (0 camo); <wood>_planks L4380-L4406 (10 crate); hempcrete L1902-L1917 (wool color order, 14 red).
    // Source: GT concrete metas (0 dark, 8 light, 12 cracked light bricks, 15 smooth light), at 5.09.54.205:
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/resources/assets/gregtech/lang/en_US.lang#L9567-L9582

    /**
     * Role key, its family, then its candidates best first as {@code namespace:name#meta}. The last candidate is the
     * vanilla (or, for the bug nest, GregGuns) block that must exist. A layout's weighted entry draws from one family
     * only (design record 0025; StructurePaletteRulesTest).
     */
    private static final String[][] TABLE = {
        // Concrete: Chisel's warm gray concrete, GregTech's light concrete behind it.
        { "concrete", "concrete", "chisel:concrete#0", "gregtech:gt.blockconcretes#8", "minecraft:stonebrick#0" },
        { "concrete_panel", "concrete", "chisel:concrete#1", "gregtech:gt.blockconcretes#15",
            "minecraft:stonebrick#0" },
        { "concrete_dark", "concrete", "chisel:concrete#4", "gregtech:gt.blockconcretes#12", "minecraft:stonebrick#2" },
        { "asphalt", "concrete", "chisel:concrete#10", "gregtech:gt.blockconcretes#0",
            "minecraft:double_stone_slab#0" },
        // Masonry: gray stone bricks; red brick for the railway house.
        { "brick", "masonry", "chisel:stonebricksmooth#5", "minecraft:stonebrick#0" },
        { "brick_worn", "masonry", "chisel:stonebricksmooth#7", "minecraft:stonebrick#2" },
        { "brick_trim", "masonry", "chisel:stonebricksmooth#12", "minecraft:stonebrick#3" },
        { "red_brick", "red_brick", "chisel:brickCustom#5", "minecraft:brick_block#0" },
        { "red_brick_worn", "red_brick", "chisel:brickCustom#13", "minecraft:brick_block#0" },
        // Steel: gray plate walls and decks, rusted plate, grate floors, dark hull steel, hazard stripes.
        // Walls avoid raw stone and cobblestone: the GT ore pass may overwrite those (open question Q4).
        { "factory_wall", "steel", "chisel:factoryblock#3", "minecraft:double_stone_slab#0" },
        { "plating", "steel", "chisel:factoryblock#13", "minecraft:double_stone_slab#0" },
        { "rust_wall", "rust", "chisel:factoryblock#1", "minecraft:hardened_clay#0" },
        { "factory_floor", "steel", "chisel:technical#13", "minecraft:double_stone_slab#0" },
        { "hull_dark", "steel", "chisel:laboratoryblock#11", "minecraft:stonebrick#0" },
        { "hazard", "hazard", "chisel:factoryblock#6", "minecraft:stained_hardened_clay#4" },
        // Fittings.
        { "lamp", "lamp", "chisel:glowstone#4", "minecraft:glowstone#0" },
        { "glass_pane", "glass", "chisel:glass_pane#2", "minecraft:glass_pane#0" },
        { "crate", "wood", "chisel:spruce_planks#10", "minecraft:planks#1" },
        // Camp: camo nets by biome, sandbags, shipping containers.
        { "camo_woodland", "camo", "minecraft:leaves#4" },
        { "camo_desert", "camo", "chisel:rebel#0", "minecraft:sandstone#0" },
        { "camo_snow", "camo", "minecraft:snow#0" }, { "sandbag", "sandbag", "minecraft:hardened_clay#0" },
        { "container", "container", "chisel:hempcrete#14", "minecraft:wool#14" },
        // The Medium House's metal storage nook (deliberately the real storage blocks).
        { "lead_block", "metal_storage", "chisel:leadblock#0", "minecraft:iron_block#0" },
        { "copper_block", "metal_storage", "chisel:copperblock#0", "minecraft:stained_hardened_clay#1" },
        { "tin_block", "metal_storage", "chisel:tinblock#0", "minecraft:iron_block#0" },
        { "gold_plate", "metal_storage", "chisel:gold_block#1", "minecraft:gold_block#0" },
        // GregGuns' own nest blocks; the vanilla stand-in keeps a nest climbable with the worldgen module off.
        // Spec: docs/techguns-spec/11-worldgen/structure-notes/alien_bug_nest.md (block roles).
        { "bugnest_wall", "bugnest", "gregguns:bug_nest#0", "minecraft:sandstone#2" },
        { "bugnest_egg", "bugnest", "gregguns:bug_nest#1", "minecraft:stained_hardened_clay#13" },
        { "bugnest_slime", "bugnest_slime", "gregguns:bug_nest_slime#0", "minecraft:vine#0" },
        { "air", "air", "minecraft:air#0" }, };

    private final Map<String, ResolvedBlock> roles;
    private final BlockLookup vanilla;
    private final Map<String, ResolvedBlock> direct = new ConcurrentHashMap<>();

    private PaletteKeys(Map<String, ResolvedBlock> roles, BlockLookup vanilla) {
        this.roles = Collections.unmodifiableMap(roles);
        this.vanilla = vanilla;
    }

    /** @return every role key with its candidates, best first, for tests and docs */
    public static Map<String, List<String>> table() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (String[] row : TABLE) {
            out.put(
                row[0],
                Collections.unmodifiableList(
                    Arrays.asList(row)
                        .subList(2, row.length)));
        }
        return Collections.unmodifiableMap(out);
    }

    /** @return the family of a role key ({@code concrete}, {@code steel}, ...), or null for an unknown key */
    public static String family(String key) {
        for (String[] row : TABLE) {
            if (row[0].equals(key)) return row[1];
        }
        return null;
    }

    /**
     * Resolves every role key. {@code chisel} null means Chisel is absent. A role none of whose candidates resolves is
     * dropped (logged), and a layout using it then fails validation instead of crashing worldgen.
     */
    public static PaletteKeys resolve(ChiselLookup chisel, BlockLookup lookup, Consumer<String> warn) {
        Map<String, ResolvedBlock> out = new LinkedHashMap<>();
        for (String[] row : TABLE) {
            String key = row[0];
            ResolvedBlock hit = null;
            for (int i = 2; i < row.length && hit == null; i++) {
                String cand = row[i];
                int hash = cand.indexOf('#');
                String name = cand.substring(0, hash);
                int meta = Integer.parseInt(cand.substring(hash + 1));
                Block block;
                if (name.startsWith(CHISEL + ":")) {
                    if (chisel == null) continue;
                    String field = name.substring(CHISEL.length() + 1);
                    block = chisel.field(field);
                    if (block == null) {
                        warn.accept("Palette key '" + key + "': Chisel block '" + field + "' is null; using the next");
                        continue;
                    }
                    if (!ChiselVariants.isValid(field, meta)) {
                        warn.accept(
                            "Palette key '" + key
                                + "': Chisel block '"
                                + field
                                + "' has no meta "
                                + meta
                                + "; using the next");
                        continue;
                    }
                } else {
                    block = lookup.byName(name);
                    if (block == null) {
                        if (i == row.length - 1) {
                            warn.accept(
                                "Palette key '" + key + "': fallback block " + name + " not found; key unavailable");
                        }
                        continue;
                    }
                }
                hit = new ResolvedBlock(block, meta, i > 2);
            }
            if (hit != null) out.put(key, hit);
        }
        return new PaletteKeys(out, lookup);
    }

    /** The postInit entry point: Chisel is read only when loaded; every block comes from the block registry. */
    public static PaletteKeys resolveForPack(Consumer<String> warn) {
        ChiselLookup chisel = Loader.isModLoaded(CHISEL) ? name -> GameRegistry.findBlock(CHISEL, name) : null;
        return resolve(chisel, PaletteKeys::registered, warn);
    }

    // Source: GameRegistry.findBlock(modId, name) returns null for an unknown block (Forge 1.7.10 GameRegistry);
    // Block.getBlockFromName would hand back the registry's default (air) instead.
    private static Block registered(String registryName) {
        int colon = registryName.indexOf(':');
        if (colon < 0) return null;
        return GameRegistry.findBlock(registryName.substring(0, colon), registryName.substring(colon + 1));
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

    /** Number of role keys that fell back past their first candidate. */
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
