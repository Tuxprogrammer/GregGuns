package com.gregguns.worldgen.camp;

import java.util.function.Supplier;

import net.minecraft.block.Block;

import com.gregguns.utility.camp.TileCampFlag;
import com.gregguns.worldgen.FlagPos;
import com.gregguns.worldgen.PlanLedger;
import com.gregguns.worldgen.PopulateWindow;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.WindowedSink;

/**
 * The flag post-step: places the Camp Flag block, initializes its tile with the cap and NPC names, and records the
 * position. Worldgen only places and initializes; it never spawns an NPC and never touches {@code NpcCampPort}
 * (Q12). The flag's own tick spawns later, only in loaded chunks.
 *
 * <p>
 * The tile is built here and attached through the sink, so nothing reads a tile back out of the world (a read that
 * can return null is WGEN-12; the same hole is closed for a supplier that fails). The step writes only when the flag
 * column is inside the populate window, so exactly one window places it.
 */
// Spec: camp_parts.md (Flag); fixes WGEN-10, WGEN-11, WGEN-12; PLAN-M8 Q12
public final class CampFlags {

    private CampFlags() {}

    /** A post-step the generator runs after the blocks of a populate window are down. */
    public interface FlagPostStep {

        /** @return true when this call placed and initialized the flag */
        boolean run(WindowedSink sink, PopulateWindow window);
    }

    /** Receives the flag position once it is placed (G6 {@code StructureState.addFlag}). */
    public interface Recorder {

        void record(FlagPos pos);
    }

    /** Records into the saved state under a plan key. The plan must already have a decision. */
    public static Recorder into(StructureState state, String planKey) {
        return pos -> state.addFlag(planKey, pos);
    }

    /** Records into a ledger (tests). */
    public static Recorder into(PlanLedger ledger, String planKey) {
        return pos -> ledger.addFlag(planKey, pos);
    }

    /**
     * @param sink      the window-clipped sink
     * @param window    the populating window
     * @param flagBlock the Camp Flag block; null when the utility module is off (nothing is placed)
     * @param spec      where and with what to initialize it
     * @param tiles     makes a fresh tile; tests pass a stand-in
     * @param recorder  told the position after a successful placement
     * @return true when the flag was placed and initialized in this window
     */
    public static boolean place(WindowedSink sink, PopulateWindow window, Block flagBlock, FlagSpec spec,
        Supplier<TileCampFlag> tiles, Recorder recorder) {
        if (flagBlock == null || !window.contains(spec.x, spec.z)) {
            return false;
        }
        if (!sink.setBlock(spec.x, spec.y, spec.z, flagBlock, 0)) {
            return false; // the block did not go down: no tile setup (WGEN-12)
        }
        TileCampFlag tile = tiles.get();
        if (tile == null) {
            return false; // WGEN-12: never dereference a missing tile
        }
        // The cap is passed explicitly so it is computed once, here, and saved by the flag (WGEN-11).
        tile.init(
            spec.sizeX,
            spec.sizeZ,
            spec.offsetX,
            spec.offsetY,
            spec.offsetZ,
            spec.spawnPoints(),
            spec.names(),
            spec.cap);
        if (!sink.setTile(spec.x, spec.y, spec.z, tile)) {
            return false;
        }
        recorder.record(new FlagPos(spec.x, spec.y, spec.z));
        return true;
    }
}
