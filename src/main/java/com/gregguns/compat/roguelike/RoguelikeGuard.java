package com.gregguns.compat.roguelike;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;

import com.gregguns.GregGuns;
import com.gregguns.worldgen.ComplexWriter;
import com.gregguns.worldgen.StructureGenerator;
import com.gregguns.worldgen.StructurePopulator;
import com.gregguns.worldgen.StructureState;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Roguelike Dungeons yields to silo complexes (owner, 2026-10-30; D303). Its world generator is swapped, at the same
 * weight, for {@link KeepOutGenerator}, which runs the dungeon unchanged unless a complex box lies within its reach,
 * and otherwise hands it a {@link KeepOutEditor}: the dungeon moves its entrance away, and any write that still
 * reaches a box is refused.
 *
 * <p>
 * <b>Reflection only.</b> Roguelike Dungeons is GPL-3; GregGuns does not compile against it. Every class, constructor
 * and method used is looked up by name when the guard installs, and the guard attaches only when all of them resolve.
 * When it cannot attach (a changed Roguelike Dungeons, or FML's registry fields renamed), it logs one warning and
 * leaves Roguelike Dungeons exactly as it was.
 *
 * <p>
 * Called from the ICBM module's postInit, after every mod's preInit has registered its generators, and only when the
 * compat switch {@code compat.roguelikeKeepOutOfComplexes} is on and Roguelike Dungeons is loaded.
 */
// Source:
// https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/Roguelike.java#L34
// (one DungeonGenerator registered at weight 0 in preInit),
// .../DungeonGenerator.java (new WorldEditor(world), new Dungeon(editor), spawnInChunk(random, chunkX, chunkZ)),
// .../dungeon/Dungeon.java#L112-L144 (public static canSpawnInChunk) and #L146-L154 (spawnInChunk),
// .../config/RogueConfig.java#L161-L165 (public static getInt(RogueConfig); LEVELRANGE, LEVELSCATTER).
// Source: Forge 1.7.10-10.13.4.1614 cpw.mods.fml.common.registry.GameRegistry (private static worldGenerators,
// worldGeneratorIndex, sortedGeneratorList; the list is rebuilt when null). GT5-Unofficial 5.09.54.205
// GameRegistryMixin only moves GT's own generator last when the list is rebuilt.
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Roguelike Dungeons keep out); D303
public final class RoguelikeGuard {

    /** Roguelike Dungeons' FML mod id. */
    public static final String MODID = "Roguelike";
    static final String GENERATOR = "greymerk.roguelike.DungeonGenerator";

    private static volatile String status = "not installed";
    private static volatile KeepOutGenerator installed;

    private RoguelikeGuard() {}

    /** @return what the last install did, for the boot check and scenarios */
    public static String status() {
        return status;
    }

    /**
     * Swaps Roguelike Dungeons' generator for the keep-out one. Logs one line either way.
     *
     * @param enabled the compat switch
     * @return whether the guard is now attached
     */
    public static synchronized boolean install(boolean enabled) {
        if (!enabled) {
            status = "off by config";
            GregGuns.LOG.info("Roguelike Dungeons keep-out: off by config (compat.roguelikeKeepOutOfComplexes)");
            return false;
        }
        try {
            Rld rld = new Rld();
            Field gens = GameRegistry.class.getDeclaredField("worldGenerators");
            Field index = GameRegistry.class.getDeclaredField("worldGeneratorIndex");
            Field sorted = GameRegistry.class.getDeclaredField("sortedGeneratorList");
            gens.setAccessible(true);
            index.setAccessible(true);
            sorted.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<IWorldGenerator> set = (Set<IWorldGenerator>) gens.get(null);
            @SuppressWarnings("unchecked")
            Map<IWorldGenerator, Integer> weights = (Map<IWorldGenerator, Integer>) index.get(null);
            IWorldGenerator original = null;
            for (IWorldGenerator g : set) {
                if (g instanceof KeepOutGenerator) {
                    installed = (KeepOutGenerator) g;
                    status = "attached";
                    return true;
                }
                if (g.getClass()
                    .getName()
                    .equals(GENERATOR)) original = g;
            }
            if (original == null) {
                return fail("its world generator " + GENERATOR + " is not registered");
            }
            Integer weight = weights.get(original);
            KeepOutGenerator guard = new KeepOutGenerator(original, rld);
            set.remove(original);
            weights.remove(original);
            set.add(guard);
            weights.put(guard, weight == null ? 0 : weight);
            sorted.set(null, null);
            installed = guard;
            status = "attached";
            GregGuns.LOG.info(
                "Roguelike Dungeons keep-out: attached (weight {}); dungeons keep {} blocks from silo complexes",
                weight,
                KeepOut.WRITE_MARGIN);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return fail(e.toString());
        }
    }

    /** @return whether the keep-out generator is installed in FML's registry now (the boot check, scenarios) */
    public static boolean attached() {
        KeepOutGenerator g = installed;
        if (g == null) return false;
        try {
            Field gens = GameRegistry.class.getDeclaredField("worldGenerators");
            gens.setAccessible(true);
            Set<?> set = (Set<?>) gens.get(null);
            if (!set.contains(g)) return false;
            for (Object o : set) if (o.getClass()
                .getName()
                .equals(GENERATOR)) return false;
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** @return {@code {max, min}} of Roguelike Dungeons' spawn grid (chunks), or null when not attached */
    public static int[] spawnGrid() {
        KeepOutGenerator g = installed;
        return g == null ? null : new int[] { g.rld.spawnMax, g.rld.spawnMin };
    }

    /**
     * Runs the installed keep-out generator on one chunk, as a populate would (scenarios).
     *
     * @return {@code {entrance spots moved, writes refused}}, or null when the guard is not attached
     */
    public static int[] runOn(World world, int chunkX, int chunkZ, Random random) {
        KeepOutGenerator g = installed;
        if (g == null) return null;
        return g.run(random, chunkX, chunkZ, world, null, null);
    }

    private static boolean fail(String why) {
        status = "could not attach: " + why;
        GregGuns.LOG.warn(
            "Roguelike Dungeons keep-out could not attach ({}); Roguelike dungeons may cut into silo complexes."
                + " Roguelike Dungeons is left unchanged.",
            why);
        return false;
    }

    /** The Roguelike Dungeons members the guard uses, all resolved up front. */
    static final class Rld {

        final Class<?> editorInterface;
        final Constructor<?> worldEditor;
        final Constructor<?> dungeon;
        final Method canSpawnInChunk;
        final Method spawnInChunk;
        final int levelRange;
        final int levelScatter;
        /** The spawn grid's region size and least separation in chunks ({@code canSpawnInChunk}). */
        final int spawnMax, spawnMin;

        Rld() throws ReflectiveOperationException {
            editorInterface = Class.forName("greymerk.roguelike.worldgen.IWorldEditor");
            worldEditor = Class.forName("greymerk.roguelike.worldgen.WorldEditor")
                .getConstructor(World.class);
            Class<?> d = Class.forName("greymerk.roguelike.dungeon.Dungeon");
            dungeon = d.getConstructor(editorInterface);
            canSpawnInChunk = d.getMethod("canSpawnInChunk", int.class, int.class, editorInterface);
            spawnInChunk = d.getMethod("spawnInChunk", Random.class, int.class, int.class);
            // Coordinates must expose getX and getZ for the editor's checks.
            Class.forName("greymerk.roguelike.worldgen.Coord")
                .getMethod("getX");
            Class.forName("greymerk.roguelike.worldgen.Coord")
                .getMethod("getZ");
            Class<?> cfg = Class.forName("greymerk.roguelike.config.RogueConfig");
            Method getInt = cfg.getMethod("getInt", cfg);
            levelRange = configInt(cfg, getInt, "LEVELRANGE", 80);
            levelScatter = configInt(cfg, getInt, "LEVELSCATTER", 10);
            int f = configInt(cfg, getInt, "SPAWNFREQUENCY", 10);
            spawnMax = Math.max(8, 32 * f / 10);
            spawnMin = Math.max(2, 8 * f / 10);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private static int configInt(Class<?> cfg, Method getInt, String option, int fallback) {
            try {
                Object o = Enum.valueOf((Class) cfg, option);
                return ((Number) getInt.invoke(null, o)).intValue();
            } catch (ReflectiveOperationException | RuntimeException e) {
                return fallback;
            }
        }
    }

    /** Roguelike Dungeons' generator, kept out of complex boxes. */
    static final class KeepOutGenerator implements IWorldGenerator {

        private final IWorldGenerator original;
        private final Rld rld;
        private final int entranceMargin;
        private final int reach;
        private boolean failedOnce;

        KeepOutGenerator(IWorldGenerator original, Rld rld) {
            this.original = original;
            this.rld = rld;
            this.entranceMargin = KeepOut.entranceMargin(rld.levelRange, rld.levelScatter);
            this.reach = KeepOut.reach(rld.levelRange, rld.levelScatter);
        }

        @Override
        public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator,
            IChunkProvider chunkProvider) {
            run(random, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
        }

        /** @return {@code {entrance spots moved, writes refused}} */
        int[] run(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator,
            IChunkProvider chunkProvider) {
            KeepOut keepOut = null;
            Object editor = null;
            try {
                if (!world.isRemote) {
                    editor = rld.worldEditor.newInstance(world);
                    // Not a spawn chunk: Roguelike Dungeons would do nothing here either.
                    if (!(Boolean) rld.canSpawnInChunk.invoke(null, chunkX, chunkZ, editor)) return new int[2];
                    keepOut = boxesNear(world, chunkX * 16 + 4, chunkZ * 16 + 4);
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                logOnce(e);
                keepOut = null;
            }
            if (keepOut == null || keepOut.isEmpty()) {
                original.generate(random, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
                return new int[2];
            }
            KeepOutEditor handler = new KeepOutEditor(editor, keepOut, entranceMargin, BiomeGenBase.ocean);
            try {
                Object proxy = Proxy.newProxyInstance(
                    rld.editorInterface.getClassLoader(),
                    new Class<?>[] { rld.editorInterface },
                    handler);
                Object dungeon = rld.dungeon.newInstance(proxy);
                rld.spawnInChunk.invoke(dungeon, random, chunkX, chunkZ);
            } catch (ReflectiveOperationException | RuntimeException e) {
                logOnce(e);
            }
            if (handler.refused() > 0 || handler.dodged() > 0) {
                int[] b = keepOut.boxes()
                    .get(0);
                GregGuns.LOG.info(
                    "Roguelike dungeon from chunk {},{}: {} entrance spots near and {} writes into the silo complex"
                        + " at {},{} kept out",
                    chunkX,
                    chunkZ,
                    handler.dodged(),
                    handler.refused(),
                    b[0],
                    b[1]);
            }
            return new int[] { handler.dodged(), handler.refused() };
        }

        private KeepOut boxesNear(World world, int x, int z) {
            ComplexWriter w = StructureGenerator.complexWriter();
            // A complex the ledger placed counts even when new ones are off here; an undecided one counts when the
            // writer is on and its chance roll passes.
            int chance = w != null && w.enabledIn(world) ? w.chancePercent() : 0;
            List<int[]> boxes = StructurePopulator.complexBoxesNear(
                world.getSeed(),
                world.provider.dimensionId,
                x - reach,
                z - reach,
                x + reach,
                z + reach,
                chance,
                StructureState.forWorld(world));
            return new KeepOut(boxes);
        }

        private void logOnce(Exception e) {
            if (failedOnce) return;
            failedOnce = true;
            GregGuns.LOG.warn("Roguelike Dungeons keep-out failed for a dungeon; it may generate unguarded", e);
        }
    }
}
