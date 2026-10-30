package com.gregguns.icbm.complex.palette;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.worldgen.layout.ChiselVariants;

/**
 * The light role (the owner's bug: Chisel Technical vents were drawn where a light belongs). Chisel's Technical block
 * has no light value at any meta, in its source (Features.java, TECHNICAL: a plain {@code BlockCarvable}, no
 * {@code setLightLevel}), "ventGlowing" being a texture only. So a lit lamp may only be a block known to glow, and
 * the vents are a sparse ceiling accent.
 */
class PaletteLightsTest {

    /** Blocks that emit light in the pack: Chisel light stone (setLightLevel 1.0, every meta) and vanilla glowstone. */
    private static final Set<String> EMITTERS = new HashSet<>(Arrays.asList("chisel:glowstone", "minecraft:glowstone"));

    private static boolean technical(BlockRef.Entry e) {
        return "chisel".equals(e.modId) && e.name.startsWith("technical");
    }

    @Test
    void everyLitLampChainEmitsAtEveryLevel() {
        for (StyleSet set : ComplexPalette.sets()) {
            List<BlockRef.Entry> chain = set.lampLit.chain();
            for (BlockRef.Entry e : chain) {
                assertTrue(EMITTERS.contains(e.modId + ":" + e.name), set.id + " lit lamp " + e + " is not a light");
                assertTrue(ChiselVariants.isValid(e.name, e.meta), set.id + " lit lamp " + e + " has no variation");
            }
            BlockRef.Entry last = chain.get(chain.size() - 1);
            assertEquals("minecraft:glowstone", last.modId + ":" + last.name, set.id + " lit lamp ends in glowstone");
        }
    }

    @Test
    void withAndWithoutChiselALitLampIsAnEmitter() {
        BlockRef.Lookup noChisel = (m, n) -> !"chisel".equals(m);
        BlockRef.Lookup all = (m, n) -> true;
        for (StyleSet set : ComplexPalette.sets()) {
            for (BlockRef.Lookup l : Arrays.asList(all, noChisel)) {
                BlockRef.Entry e = set.lampLit.resolve(l);
                assertTrue(EMITTERS.contains(e.modId + ":" + e.name), set.id + " resolves to " + e);
            }
        }
    }

    @Test
    void noLampIsATechnicalVent() {
        for (StyleSet set : ComplexPalette.sets()) {
            for (BlockRef r : Arrays.asList(set.lampLit, set.lampUnlit)) {
                for (BlockRef.Entry e : r.chain()) assertFalse(technical(e), set.id + " lamp uses " + e);
            }
        }
    }

    @Test
    void ventsAreASparseCeilingAccent() {
        int sets = 0;
        for (StyleSet set : ComplexPalette.sets()) {
            for (CellPattern p : set.ceilings) {
                boolean vents = false;
                for (BlockRef r : p.refs()) if (r.chain()
                    .get(0)
                    .equals(BlockRef.e("chisel", "technical", 7))) vents = true;
                if (!vents) continue;
                sets++;
                int n = 0, total = 0;
                for (int x = 0; x < 20; x++) {
                    for (int z = 0; z < 20; z++) {
                        total++;
                        if (p.at(x, z, 20, 20)
                            .chain()
                            .get(0)
                            .equals(BlockRef.e("chisel", "technical", 7))) n++;
                    }
                }
                assertTrue(n > 0 && n * 4 <= total, set.id + " vents " + n + " of " + total + " cells");
            }
        }
        assertTrue(sets >= 3, "vents ride the ceilings of several suitable sets, found " + sets);
    }

    @Test
    void sparseKeepsTheBaseAndTheAccentInItsRefs() {
        BlockRef a = BlockRef.of(BlockRef.v("quartz_block", 0)), b = BlockRef.of(BlockRef.v("stone", 0));
        CellPattern p = Patterns.sparse(Patterns.plain(a), b, 5);
        assertTrue(
            p.refs()
                .contains(a)
                && p.refs()
                    .contains(b));
        int accents = 0;
        for (int x = 0; x < 30; x++) for (int z = 0; z < 30; z++) if (p.at(x, z, 30, 30) == b) accents++;
        assertTrue(accents > 90 && accents < 270, "about a fifth: " + accents);
    }
}
