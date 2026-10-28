package com.gregguns.icbm.complex.palette;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.worldgen.ActivityLevel;
import com.gregguns.worldgen.layout.ResolvedBlock;

/** Scenarios of the complex palette (I6b). Dev environment only. Server side. */
// Spec: docs/gregguns-icbm-spec/08-worldgen/palette.md (Tests)
// Source: Chisel blocks implement ICarvable, whose getManager(int meta) returns CarvableHelper.getVariation(meta),
// null for a meta with no variation (the block then draws the missing texture), at 2.17.34-GTNH:
// https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/com/cricketcraft/chisel/api/ICarvable.java
// https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/com/cricketcraft/chisel/api/carving/CarvableHelper.java
public final class PaletteScenarios {

    private PaletteScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Resolve());
    }

    private static final class Resolve implements Scenario {

        @Override
        public String name() {
            return "icbm.palette.resolve";
        }

        @Override
        public String description() {
            return "Every block of every style set (walls, floors, ceilings, lamps, accent, worn block, furniture)"
                + " resolves in the live registry, building blocks are full cubes, a Chisel block has a variation at"
                + " its meta, lit lamps emit 15 and dead lamps do not; the note lists which mod supplied what.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            PaletteResolver.clear();
            Map<String, Integer> byMod = new TreeMap<>();
            Map<String, Integer> missing = new TreeMap<>();
            BlockRef.Lookup reg = PaletteResolver.registry();
            for (StyleSet set : ComplexPalette.sets()) {
                check(out, set.id + " lit lamp", set.lampLit, true, reg, byMod, missing);
                check(out, set.id + " dead lamp", set.lampUnlit, false, reg, byMod, missing);
                for (BlockRef w : set.walls) {
                    check(out, set.id + " wall", w, null, reg, byMod, missing);
                }
                for (CellPattern p : set.floors) {
                    for (int x = 0; x < 8; x++) {
                        for (int z = 0; z < 8; z++) {
                            check(out, set.id + " floor", p.at(x, z, 8, 8), null, reg, byMod, missing);
                        }
                    }
                }
                for (CellPattern p : set.ceilings) {
                    for (int x = 0; x < 8; x++) {
                        for (int z = 0; z < 8; z++) {
                            check(out, set.id + " ceiling", p.at(x, z, 8, 8), null, reg, byMod, missing);
                        }
                    }
                }
                if (set.accent != null) {
                    check(out, set.id + " accent", set.accent, null, reg, byMod, missing);
                }
                if (set.worn != null) {
                    check(out, set.id + " worn", set.worn, null, reg, byMod, missing);
                }
                for (Map.Entry<Furniture, BlockRef> f : set.furniture.entrySet()) {
                    // Tables are slabs: furniture is checked for a real block and variation, not for a full cube.
                    check(out, set.id + " furniture " + f.getKey(), f.getValue(), null, reg, byMod, missing);
                }
            }
            for (StyleGroup g : StyleGroup.values()) {
                for (String dim : new String[] { "Overworld", "moon", "mars" }) {
                    RoomStyle s = ComplexPalette.styleFor(g, 11L, dim, ActivityLevel.OPERATIONAL);
                    ResolvedBlock w = s.wall();
                    out.check(w != null && w.block != null, g + "/" + dim + " wall resolves");
                    out.check(s.lamp(true).block != null && s.lamp(false).block != null, g + " lamps resolve");
                }
            }
            out.note("blocks by supplying mod (first chain entry that exists): " + byMod);
            out.note("chain entries not in this registry (fallback used): " + missing);
            return out.build();
        }

        private static void check(ScenarioResult.Builder out, String what, BlockRef ref, Boolean lit,
            BlockRef.Lookup reg, Map<String, Integer> byMod, Map<String, Integer> missing) {
            BlockRef.Entry e = ref.resolve(reg);
            byMod.merge(e.modId, 1, Integer::sum);
            if (!ref.chain()
                .get(0)
                .equals(e)) {
                missing.merge(
                    ref.chain()
                        .get(0)
                        .key(),
                    1,
                    Integer::sum);
            }
            ResolvedBlock r = PaletteResolver.resolve(ref);
            if (r.block == null) {
                out.check(false, what + " resolved to null for " + ref);
                return;
            }
            Block b = r.block;
            Boolean variation = chiselVariation(b, r.meta);
            if (variation != null) {
                out.check(variation, what + " " + e + " has no Chisel variation at meta " + r.meta);
            }
            if (lit != null) {
                if (lit) {
                    out.check(b.getLightValue() == 15, what + " " + e + " emits " + b.getLightValue());
                } else {
                    out.check(b.getLightValue() < 15, what + " " + e + " glows");
                }
            }
            out.check(
                b.isOpaqueCube() || b.renderAsNormalBlock() || what.endsWith("lamp") || what.contains(" furniture "),
                what + " " + e + " not a cube");
        }

        /**
         * @return whether a Chisel block has a variation at {@code meta}; null when the block is not a Chisel
         *         {@code ICarvable} (or Chisel is absent), so there is nothing to check
         */
        private static Boolean chiselVariation(Block b, int meta) {
            if (!implementsByName(b.getClass(), "com.cricketcraft.chisel.api.ICarvable")) {
                return null;
            }
            try {
                Method m = b.getClass()
                    .getMethod("getManager", int.class);
                return m.invoke(b, meta) != null;
            } catch (ReflectiveOperationException | RuntimeException ex) {
                return null;
            }
        }

        private static boolean implementsByName(Class<?> c, String name) {
            for (Class<?> k = c; k != null; k = k.getSuperclass()) {
                for (Class<?> i : k.getInterfaces()) {
                    if (i.getName()
                        .equals(name) || implementsByName(i, name)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
