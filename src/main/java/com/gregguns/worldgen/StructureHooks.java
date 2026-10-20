package com.gregguns.worldgen;

import net.minecraft.world.World;

/**
 * Hook points the generator leaves for later pieces. Both default to doing nothing. G11 installs the spawner hook
 * (spawner-hole blocks) and G10 installs the flag hook (camp flag block and {@code StructureState#addFlag}). Server
 * side; called inside the populate window, so a hook must not touch another chunk (GGS-11-08, GGS-11-09).
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md; PLAN-M8 G9, G10, G11
public final class StructureHooks {

    /** A spawner mark of a stamped structure. */
    public interface SpawnerHook {

        /**
         * @param world the world being populated
         * @param sink  the clipped sink for this populate; write only through it
         * @param plan  the plan being built
         * @param tag   the layout mark tag, such as {@code spawner.zombie}
         * @param x     world X inside the window
         * @param y     world Y
         * @param z     world Z inside the window
         */
        void spawnerMarked(World world, WindowedSink sink, StructurePlan plan, String tag, int x, int y, int z);
    }

    /** A camp flag mark of a stamped structure. */
    public interface FlagHook {

        /**
         * @param world   the world being populated
         * @param sink    the clipped sink for this populate; write only through it
         * @param plan    the plan being built
         * @param planKey {@link PlanKey#of(StructurePlan)}, the key for {@code StructureState#addFlag}
         * @param x       world X inside the window
         * @param y       world Y
         * @param z       world Z inside the window
         */
        void flagMarked(World world, WindowedSink sink, StructurePlan plan, String planKey, int x, int y, int z);
    }

    private static volatile SpawnerHook spawner = (w, s, p, t, x, y, z) -> {};
    private static volatile FlagHook flag = (w, s, p, k, x, y, z) -> {};

    private StructureHooks() {}

    /** @param hook the spawner step, or null to restore the no-op */
    public static void setSpawnerHook(SpawnerHook hook) {
        spawner = hook != null ? hook : (w, s, p, t, x, y, z) -> {};
    }

    /** @param hook the flag step, or null to restore the no-op */
    public static void setFlagHook(FlagHook hook) {
        flag = hook != null ? hook : (w, s, p, k, x, y, z) -> {};
    }

    static SpawnerHook spawner() {
        return spawner;
    }

    static FlagHook flag() {
        return flag;
    }
}
