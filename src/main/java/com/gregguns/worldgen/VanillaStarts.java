package com.gregguns.worldgen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderGenerate;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureStart;

import com.gregguns.GregGuns;

/**
 * {@link StructureStarts} for a live world: finds the world's own village and temple generators on its chunk provider
 * and answers from their start rule, without generating or loading a chunk (D302).
 *
 * <p>
 * <b>Finding the generators.</b> Any field of the provider's class (and its superclasses) holding a
 * {@link MapGenVillage} or {@link MapGenScatteredFeature}. This needs no provider-specific names, so the vanilla
 * provider and Realistic World Gen's both work. Spacing, separation and the village size come from the instance (RWG
 * builds its villages at spacing 24). A provider with neither has no starts (Galacticraft planets, the Nether).
 *
 * <p>
 * <b>Is the generator on.</b> The vanilla provider builds structures only when the world has map features on. RWG
 * builds villages when its own {@code ConfigRWG.generateVillages} is true and ignores map features. Any other provider
 * counts as on, which errs toward sinking a complex, never toward a village on top of one.
 *
 * <p>
 * <b>Per candidate.</b> {@link StartGrid} gives each region's candidate chunk from the generator's own spacing (read
 * from its most derived class, so a replacement such as Village Names' generator, which declares its own spacing, is
 * read right). For vanilla's own generators the rule is computed exactly: a biome check on the world chunk manager,
 * then the start built as the generator builds it ({@code MapGenBase}'s per-chunk seed, one {@code nextInt}, then
 * {@code MapGenVillage.Start} or {@code MapGenScatteredFeature.Start}); a village counts only when sizeable. A
 * replacement is asked through its own {@code canSpawnStructureAtCoords}, which makes the start chunk exact, but its
 * layout is not reproducible (Village Names draws a different village at the same start in two worlds of one seed),
 * so a box of {@link #REPLACEMENT_HALF} blocks around the start chunk is assumed. Nothing reads a chunk. A start that
 * throws counts as present. Results are cached per start chunk.
 */
// Source: Minecraft 1.7.10 (MCP stable 12): MapGenVillage#canSpawnStructureAtCoords and Start,
// MapGenScatteredFeature#canSpawnStructureAtCoords and Start, MapGenBase#func_151539_a,
// MapGenStructure#func_151538_a, World#setRandomSeed.
// Source:
// https://github.com/GTNewHorizons/VillageNames/blob/4.5.19-GTNH/src/main/java/astrotibs/villagenames/village/MapGenVillageVN.java#L63-L76
// (Village Names, in the pack, replaces the village generator through InitMapGenEvent with its own spacing fields,
// median 40 and range 10 in the pack config, its own biome list and its own Start; #L106-L156, #L158-L180).
// Two real-pack worlds of seed 321 held different Village Names villages at start chunk 19,19 (Village.dat boxes
// 266..359 x 262..355 and 276..397 x 257..377).
// Source:
// https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.2/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L120-L123
// (new MapGenVillage with distance 24) and #L194 (ConfigRWG.generateVillages gates villages).
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Structure starts); D302
public final class VanillaStarts implements StructureStarts {

    /** Blocks around a village's piece box that still count: paths, fields and wandering villagers. */
    public static final int VILLAGE_MARGIN = 16;
    /** Blocks around a temple's box that still count. */
    public static final int TEMPLE_MARGIN = 8;
    /**
     * Vanilla village pieces stay within 112 blocks of the start chunk (StructureVillagePieces); Village Names grows
     * larger villages, so candidates this far out are checked. Only the start's real box decides.
     */
    static final int VILLAGE_REACH = 192;
    /** A temple is one piece at its start chunk. */
    static final int TEMPLE_REACH = 32;

    private static final List<BiomeGenBase> TEMPLE_BIOMES = Arrays.asList(
        BiomeGenBase.desert,
        BiomeGenBase.desertHills,
        BiomeGenBase.jungle,
        BiomeGenBase.jungleHills,
        BiomeGenBase.swampland);

    private static final Map<World, StructureStarts> WORLDS = new WeakHashMap<World, StructureStarts>();
    /** Start chunks checked, by kind and chunk: the box with its margin, or null for none. */
    private final Map<Long, int[]> cache = new LinkedHashMap<Long, int[]>(64, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, int[]> e) {
            return size() > 256;
        }
    };
    private static final int[] NOTHING = new int[0];

    /**
     * Weak, like the cache's key: the generators hold the world too, so nothing here may keep it alive. Only numbers
     * are kept from the generators.
     */
    private final java.lang.ref.WeakReference<World> worldRef;
    private final boolean village;
    private final int villageSpacing, villageSeparation, villageSize;
    private final boolean temple;
    private final int templeSpacing, templeSeparation;
    /** The generators themselves, weakly, so their own start rule can be asked (another mod's replacement too). */
    private final java.lang.ref.WeakReference<MapGenStructure> villageGen, templeGen;

    private VanillaStarts(World world, MapGenVillage village, MapGenScatteredFeature temple) {
        this.worldRef = new java.lang.ref.WeakReference<World>(world);
        this.village = village != null;
        this.villageGen = new java.lang.ref.WeakReference<MapGenStructure>(village);
        this.templeGen = new java.lang.ref.WeakReference<MapGenStructure>(temple);
        // Read from the most derived class first: a replacement (Village Names) declares its own spacing fields.
        this.villageSpacing = village == null ? 32 : intField(village, MapGenVillage.class, 32, "field_82665_g");
        this.villageSeparation = village == null ? 8 : intField(village, MapGenVillage.class, 8, "field_82666_h");
        this.villageSize = village == null ? 0
            : intField(village, MapGenVillage.class, 0, "terrainType", "field_75054_f");
        this.temple = temple != null;
        this.templeSpacing = temple == null ? 32
            : intField(
                temple,
                MapGenScatteredFeature.class,
                32,
                "maxDistanceBetweenScatteredFeatures",
                "field_82669_g");
        this.templeSeparation = temple == null ? 8
            : intField(temple, MapGenScatteredFeature.class, 8, "minDistanceBetweenScatteredFeatures", "field_82670_h");
    }

    /** MapGenStructure#canSpawnStructureAtCoords and MapGenBase#worldObj; null if absent. */
    private static final Method CAN_SPAWN = method("func_75047_a", "canSpawnStructureAtCoords");
    private static final Field GEN_WORLD = field(net.minecraft.world.gen.MapGenBase.class, "field_75039_c", "worldObj");

    private static Method method(String... names) {
        for (String n : names) {
            try {
                Method m = MapGenStructure.class.getDeclaredMethod(n, int.class, int.class);
                m.setAccessible(true);
                return m;
            } catch (ReflectiveOperationException | RuntimeException e) {
                // Try the next name (SRG in the pack, MCP in the dev world).
            }
        }
        return null;
    }

    private static Field field(Class<?> owner, String... names) {
        for (String n : names) {
            try {
                Field f = owner.getDeclaredField(n);
                f.setAccessible(true);
                return f;
            } catch (ReflectiveOperationException | RuntimeException e) {
                // Try the next name.
            }
        }
        return null;
    }

    /**
     * The starts of a server world, found once per world. A client world, or a provider with no village or temple
     * generator that is on, gives {@link StructureStarts#NONE}.
     */
    public static synchronized StructureStarts forWorld(World world) {
        if (!(world instanceof WorldServer)) return NONE;
        StructureStarts s = WORLDS.get(world);
        if (s == null) {
            s = build((WorldServer) world);
            WORLDS.put(world, s);
        }
        return s;
    }

    private static StructureStarts build(WorldServer world) {
        IChunkProvider provider = world.theChunkProviderServer == null ? null
            : world.theChunkProviderServer.currentChunkProvider;
        if (provider == null) return NONE;
        MapGenVillage village = null;
        MapGenScatteredFeature temple = null;
        try {
            for (Class<?> c = provider.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    boolean v = MapGenVillage.class.isAssignableFrom(f.getType());
                    boolean t = MapGenScatteredFeature.class.isAssignableFrom(f.getType());
                    if (!v && !t) continue;
                    f.setAccessible(true);
                    Object o = f.get(provider);
                    if (v && village == null && o instanceof MapGenVillage) village = (MapGenVillage) o;
                    if (t && temple == null && o instanceof MapGenScatteredFeature) temple = (MapGenScatteredFeature) o;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            GregGuns.LOG.warn(
                "Silo complex: could not read the structure generators of {} ({}); villages are not looked for",
                provider.getClass()
                    .getName(),
                e.toString());
            return NONE;
        }
        if (provider instanceof ChunkProviderGenerate && !world.getWorldInfo()
            .isMapFeaturesEnabled()) {
            village = null;
            temple = null;
        }
        if (village != null && !rwgVillagesOn(provider)) village = null;
        if (village == null && temple == null) return NONE;
        if (village != null && village.getClass() != MapGenVillage.class) {
            GregGuns.LOG.info(
                "Silo complex: dimension {} builds villages with {}; its own start rule is asked",
                world.provider.dimensionId,
                village.getClass()
                    .getName());
        }
        return new VanillaStarts(world, village, temple);
    }

    /** @return false only when the provider is RWG's and its config turns villages off */
    private static boolean rwgVillagesOn(IChunkProvider provider) {
        if (!provider.getClass()
            .getName()
            .startsWith("rwg.")) return true;
        try {
            Field f = Class.forName("rwg.config.ConfigRWG")
                .getField("generateVillages");
            return f.getBoolean(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return true;
        }
    }

    /** Reads an int field, from the instance's own class up to {@code owner}, under any of its names. */
    private static int intField(Object o, Class<?> owner, int fallback, String... names) {
        for (Class<?> c = o.getClass(); c != null && owner.isAssignableFrom(c); c = c.getSuperclass()) {
            for (String n : names) {
                try {
                    Field f = c.getDeclaredField(n);
                    if (f.getType() != int.class) continue;
                    f.setAccessible(true);
                    return f.getInt(o);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // Try the next name (MCP in the dev world, SRG in the pack), then the superclass.
                }
            }
        }
        GregGuns.LOG.warn("Silo complex: {} has no field {}; using {}", owner.getSimpleName(), names[0], fallback);
        return fallback;
    }

    @Override
    public Blocker blocking(int minX, int minZ, int maxX, int maxZ) {
        World world = worldRef.get();
        if (world == null) return null;
        Blocker b = !village ? null
            : scan(
                world,
                "village",
                minX,
                minZ,
                maxX,
                maxZ,
                VILLAGE_REACH,
                villageSpacing,
                villageSeparation,
                StartGrid.VILLAGE_SALT,
                VILLAGE_MARGIN);
        if (b == null && temple) {
            b = scan(
                world,
                "temple",
                minX,
                minZ,
                maxX,
                maxZ,
                TEMPLE_REACH,
                templeSpacing,
                templeSeparation,
                StartGrid.TEMPLE_SALT,
                TEMPLE_MARGIN);
        }
        return b;
    }

    private Blocker scan(World world, String kind, int minX, int minZ, int maxX, int maxZ, int reach, int spacing,
        int separation, int salt, int margin) {
        long seed = world.getSeed();
        List<int[]> cands = StartGrid.candidatesIn(
            seed,
            (minX - reach) >> 4,
            (minZ - reach) >> 4,
            (maxX + reach) >> 4,
            (maxZ + reach) >> 4,
            spacing,
            separation,
            salt);
        for (int[] c : cands) {
            int[] box = boxOf(world, kind, c[0], c[1], seed);
            if (box == NOTHING) continue;
            if (box[0] - margin <= maxX && box[2] + margin >= minX
                && box[1] - margin <= maxZ
                && box[3] + margin >= minZ) {
                return new Blocker(
                    kind,
                    "start chunk " + c[0]
                        + ","
                        + c[1]
                        + ", box "
                        + box[0]
                        + ".."
                        + box[2]
                        + " x "
                        + box[1]
                        + ".."
                        + box[3],
                    c[0],
                    c[1]);
            }
        }
        return null;
    }

    /** @return the structure's box {@code minX, minZ, maxX, maxZ} at a candidate chunk, or {@link #NOTHING} */
    private int[] boxOf(World world, String kind, int cx, int cz, long seed) {
        boolean isVillage = kind.equals("village");
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL) ^ (isVillage ? 0 : 0x4000_0000_0000_0000L);
        int[] hit = cache.get(key);
        if (hit != null) return hit;
        int[] box;
        try {
            int[] own = ownRule(isVillage ? villageGen.get() : templeGen.get(), cx, cz);
            box = own != null ? own : isVillage ? villageBox(world, cx, cz, seed) : templeBox(world, cx, cz, seed);
        } catch (RuntimeException | LinkageError e) {
            // A modded piece that fails to build: count the start as there (a complex only sinks deeper for it).
            GregGuns.LOG.warn("Silo complex: the {} start at chunk {},{} failed to build: {}", kind, cx, cz, e);
            box = new int[] { cx * 16 - VILLAGE_REACH, cz * 16 - VILLAGE_REACH, cx * 16 + VILLAGE_REACH,
                cz * 16 + VILLAGE_REACH };
        }
        cache.put(key, box);
        return box;
    }

    /**
     * Half the side of the box assumed around a replacement generator's start chunk. Its start chunk is exact (its own
     * rule), but its layout is not: Village Names drew a different village at the same start in two worlds of the same
     * seed, so its pieces cannot be predicted. Vanilla's pieces stay within 112 blocks of the start; the observed
     * Village
     * Names villages reached 106 blocks out.
     */
    static final int REPLACEMENT_HALF = 128;

    /**
     * Asks a replacement generator (another mod's subclass) whether a candidate is a start, through its own
     * {@code canSpawnStructureAtCoords} (its biome rule, any spawn-free radius). Vanilla's own generators return null
     * here: their rule is computed exactly by {@link #villageBox} and {@link #templeBox}.
     *
     * @return the assumed box around the start chunk, {@link #NOTHING}, or null to use the vanilla rule
     */
    private static int[] ownRule(MapGenStructure gen, int cx, int cz) {
        if (gen == null || gen.getClass() == MapGenVillage.class || gen.getClass() == MapGenScatteredFeature.class) {
            return null;
        }
        int x = cx * 16 + 8, z = cz * 16 + 8;
        int[] assumed = { x - REPLACEMENT_HALF, z - REPLACEMENT_HALF, x + REPLACEMENT_HALF, z + REPLACEMENT_HALF };
        if (CAN_SPAWN == null || GEN_WORLD == null) return assumed;
        try {
            // Its world is set once it has generated a chunk; before that a candidate counts as a start.
            if (GEN_WORLD.get(gen) == null) return assumed;
            return (Boolean) CAN_SPAWN.invoke(gen, cx, cz) ? assumed : NOTHING;
        } catch (java.lang.reflect.InvocationTargetException e) {
            // The generator's own code failed: the caller counts the start as present.
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            return assumed;
        }
    }

    private static final Field STRUCTURE_MAP = field(MapGenStructure.class, "field_75053_d", "structureMap");

    /**
     * Whether the world's village generator itself recorded a start at a chunk, once that chunk's neighborhood has
     * generated (scenarios check a prediction against it).
     *
     * @return true or false, or null when the world has no village generator to ask
     */
    public static Boolean villageRecorded(World world, int cx, int cz) {
        StructureStarts s = forWorld(world);
        if (!(s instanceof VanillaStarts) || STRUCTURE_MAP == null) return null;
        MapGenStructure gen = ((VanillaStarts) s).villageGen.get();
        if (gen == null) return null;
        try {
            Map<?, ?> m = (Map<?, ?>) STRUCTURE_MAP.get(gen);
            return m.containsKey(net.minecraft.world.ChunkCoordIntPair.chunkXZ2Int(cx, cz));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** The seed {@code MapGenBase#func_151539_a} gives its Random before visiting a chunk. */
    static long chunkSeed(int cx, int cz, long seed) {
        Random r = new Random(seed);
        long a = r.nextLong(), b = r.nextLong();
        return cx * a ^ cz * b ^ seed;
    }

    private int[] villageBox(World world, int cx, int cz, long seed) {
        if (!world.getWorldChunkManager()
            .areBiomesViable(cx * 16 + 8, cz * 16 + 8, 0, MapGenVillage.villageSpawnBiomes)) return NOTHING;
        MapGenVillage.Start s = new MapGenVillage.Start(world, structureRandom(cx, cz, seed), cx, cz, villageSize);
        return s.isSizeableStructure() ? box(s) : NOTHING;
    }

    private int[] templeBox(World world, int cx, int cz, long seed) {
        BiomeGenBase b = world.getWorldChunkManager()
            .getBiomeGenAt(cx * 16 + 8, cz * 16 + 8);
        if (!TEMPLE_BIOMES.contains(b)) return NOTHING;
        return box(new MapGenScatteredFeature.Start(world, structureRandom(cx, cz, seed), cx, cz));
    }

    private static int[] box(StructureStart s) {
        StructureBoundingBox bb = s.getBoundingBox();
        return new int[] { bb.minX, bb.minZ, bb.maxX, bb.maxZ };
    }

    /**
     * The Random a structure generator hands to a start at this chunk: {@code MapGenBase} seeds it from two
     * {@code nextLong} values of the world seed and the chunk, and {@code MapGenStructure} draws one {@code nextInt}
     * before the start is built.
     */
    static Random structureRandom(int cx, int cz, long seed) {
        Random r = new Random(chunkSeed(cx, cz, seed));
        r.nextInt();
        return r;
    }
}
