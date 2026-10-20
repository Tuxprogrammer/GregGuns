package com.gregguns.worldgen.camp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import com.gregguns.utility.camp.TileCampFlag;
import com.gregguns.worldgen.Decision;
import com.gregguns.worldgen.StructureHooks;
import com.gregguns.worldgen.StructurePlan;
import com.gregguns.worldgen.StructureState;
import com.gregguns.worldgen.WindowedSink;
import com.gregguns.worldgen.layout.Layout;
import com.gregguns.worldgen.layout.LayoutRegistry;

/**
 * The flag step of a layout structure that carries a camp: today only the Aircraft Carrier (one {@code flag} mark and
 * nine {@code spawn.npc} marks). Installed as the {@link StructureHooks} flag hook. The spawn offsets are computed from
 * the layout with {@link SpawnMarks}, not collected from per-window mark callbacks, so the flag's window sees all nine.
 * Worldgen only places and initializes the flag; it never calls {@code NpcCampPort} (Q12).
 */
// Spec: PLAN-M8 section 2 (Aircraft Carrier: flag, ArmySoldier + Commando, cap 9 persisted), Q12; WGEN-10..12
public final class CarrierFlag implements StructureHooks.FlagHook {

    /** The carrier's NPC cap (PLAN-M8 section 2); saved by the flag tile. */
    public static final int CARRIER_CAP = 9;

    private static final Map<String, List<String>> NAMES = new HashMap<>();

    static {
        NAMES.put("aircraft_carrier", Collections.unmodifiableList(Arrays.asList("ArmySoldier", "Commando")));
    }

    private final LayoutRegistry layouts;
    private final Block flagBlock;

    /**
     * @param layouts  the loaded layouts, for the spawn marks
     * @param flagBlock the Camp Flag block; null when the utility module is off (nothing is placed)
     */
    public CarrierFlag(LayoutRegistry layouts, Block flagBlock) {
        this.layouts = layouts;
        this.flagBlock = flagBlock;
    }

    /**
     * The flag data of a carrier plan, from its layout alone.
     *
     * @param groundY the recorded decision Y (the stamp anchor is one above it)
     * @param x       world X of the flag mark
     * @param y       world Y of the flag mark
     * @param z       world Z of the flag mark
     * @return the spec, or null when the plan's structure carries no camp or has no layout
     */
    public static FlagSpec specFor(LayoutRegistry layouts, StructurePlan plan, int groundY, int x, int y, int z) {
        List<String> names = NAMES.get(plan.id());
        Layout layout = layouts.get(plan.id());
        if (names == null || layout == null) {
            return null;
        }
        int anchorY = groundY + 1;
        int originY = anchorY - layout.ground();
        int[] spawns = SpawnMarks
            .offsets(layout, plan.anchorX(), anchorY, plan.anchorZ(), plan.rotation(), plan.anchorX(), originY, plan.anchorZ());
        return FlagSpec.ofCap(
            x,
            y,
            z,
            plan.anchorX(),
            originY,
            plan.anchorZ(),
            plan.sizeX(),
            plan.sizeZ(),
            spawns,
            names,
            CARRIER_CAP);
    }

    @Override
    public void flagMarked(World world, WindowedSink sink, StructurePlan plan, String planKey, int x, int y, int z) {
        place(StructureState.forWorld(world), sink, plan, planKey, x, y, z);
    }

    /** The hook body over an explicit state, so a scenario can use a scratch state. */
    public void place(StructureState state, WindowedSink sink, StructurePlan plan, String planKey, int x, int y, int z) {
        Decision d = state.get(planKey);
        if (d == null || !d.isPlaced()) {
            return; // the record comes first, or addFlag is ignored
        }
        FlagSpec spec = specFor(layouts, plan, d.groundY(), x, y, z);
        if (spec == null) {
            return;
        }
        CampFlags.place(sink, sink.window(), flagBlock, spec, TileCampFlag::new, CampFlags.into(state, planKey));
    }
}
