package com.gregguns.worldgen;

import java.lang.reflect.Field;
import java.util.ArrayList;
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
 * <b>Per candidate.</b> {@link StartGrid} gives each region's candidate chunk; a candidate within reach of the area is
 * checked for biomes on the world chunk manager (it reads the biome layers or RWG's noise, never a chunk), then its
 * start is built exactly as the generator would build it ({@code MapGenBase}'s per-chunk seeding, then one
 * {@code nextInt}), and its piece box is tested. A village counts only when sizeable, as vanilla's own rule. A start
 * that throws counts as present. Results are cached per start chunk.
 */
// Source: Minecraft 1.7.10 (MCP stable 12): MapGenVillage#canSpawnStructureAtCoords and Start,
// MapGenScatteredFeature#canSpawnStructureAtCoords and Start, MapGenBase#func_151539_a,
// MapGenStructure#func_151538_a, World#setRandomSeed.
// Source:
// https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.2/src/main/java/rwg/world/ChunkGeneratorRealistic.java#L120-L123
// (new MapGenVillage with distance 24) and #L194 (ConfigRWG.generateVillages gates villages).
// Spec: docs/gregguns-icbm-spec/08-worldgen/planets-and-placement.md (Structure starts); D302
public final class VanillaStarts implements StructureStarts {

    /** Blocks around a village's piece box that still count: paths, fields and wandering villagers. */
    public static final int VILLAGE_MARGIN = 16;
    /** Blocks around a temple's box that still count. */
    public static final int TEMPLE_MARGIN = 8;
    /** Village pieces stay within 112 blocks of the start chunk (StructureVillagePieces); a little more for the box. */
    static final int VILLAGE_REACH = 128;
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

    private final World world;
    private final MapGenVillage village;
    private final int villageSpacing, villageSeparation, villageSize;
    private final MapGenScatteredFeature temple;
    private final int templeSpacing, templeSeparation;

    private VanillaStarts(World world, MapGenVillage village, MapGenScatteredFeature temple) {
        this.world = world;
        this.village = village;
        this.villageSpacing = village == null ? 32 : intField(village, MapGenVillage.class, 32, "field_82665_g");
        this.villageSeparation = village == null ? 8 : intField(village, MapGenVillage.class, 8, "field_82666_h");
        this.villageSize = village == null ? 0
            : intField(village, MapGenVillage.class, 0, "terrainType", "field_75054_f");
        this.temple = temple;
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
                "Silo complex: dimension {} builds villages with {}; its start rule is read as vanilla's",
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

    private static int intField(Object o, Class<?> owner, int fallback, String... names) {
        for (String n : names) {
            try {
                Field f = owner.getDeclaredField(n);
                f.setAccessible(true);
                return f.getInt(o);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // Try the next name (MCP in the dev world, SRG in the pack).
            }
        }
        GregGuns.LOG.warn("Silo complex: {} has no field {}; using {}", owner.getSimpleName(), names[0], fallback);
        return fallback;
    }

    @Override
    public Blocker blocking(int minX, int minZ, int maxX, int maxZ) {
        Blocker b = village == null ? null
            : scan(
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
        if (b == null && temple != null) {
            b = scan(
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

    private Blocker scan(String kind, int minX, int minZ, int maxX, int maxZ, int reach, int spacing, int separation,
        int salt, int margin) {
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
            int[] box = boxOf(kind, c[0], c[1], seed);
            if (box == NOTHING) continue;
            if (box[0] - margin <= maxX && box[2] + margin >= minX
                && box[1] - margin <= maxZ
                && box[3] + margin >= minZ) {
                return new Blocker(
                    kind,
                    "start chunk " + c[0]
                        + ","
                        + c[1]
                        + ", pieces "
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
    private int[] boxOf(String kind, int cx, int cz, long seed) {
        boolean isVillage = kind.equals("village");
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL) ^ (isVillage ? 0 : 0x4000_0000_0000_0000L);
        int[] hit = cache.get(key);
        if (hit != null) return hit;
        int[] box;
        try {
            box = isVillage ? villageBox(cx, cz, seed) : templeBox(cx, cz, seed);
        } catch (RuntimeException | LinkageError e) {
            // A modded piece that fails to build: count the start as there (a complex only sinks deeper for it).
            GregGuns.LOG.warn("Silo complex: the {} start at chunk {},{} failed to build: {}", kind, cx, cz, e);
            box = new int[] { cx * 16 - VILLAGE_REACH, cz * 16 - VILLAGE_REACH, cx * 16 + VILLAGE_REACH,
                cz * 16 + VILLAGE_REACH };
        }
        cache.put(key, box);
        return box;
    }

    private int[] villageBox(int cx, int cz, long seed) {
        if (!world.getWorldChunkManager()
            .areBiomesViable(cx * 16 + 8, cz * 16 + 8, 0, MapGenVillage.villageSpawnBiomes)) return NOTHING;
        MapGenVillage.Start s = new MapGenVillage.Start(world, structureRandom(cx, cz, seed), cx, cz, villageSize);
        return s.isSizeableStructure() ? box(s) : NOTHING;
    }

    private int[] templeBox(int cx, int cz, long seed) {
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
        Random r = new Random(seed);
        long a = r.nextLong(), b = r.nextLong();
        r.setSeed(cx * a ^ cz * b ^ seed);
        r.nextInt();
        return r;
    }

    /** @return the candidate start chunks the scan would check for a block area (scenarios, the locate command) */
    public List<int[]> villageCandidates(int minX, int minZ, int maxX, int maxZ) {
        if (village == null) return new ArrayList<int[]>();
        return StartGrid.candidatesIn(
            world.getSeed(),
            (minX - VILLAGE_REACH) >> 4,
            (minZ - VILLAGE_REACH) >> 4,
            (maxX + VILLAGE_REACH) >> 4,
            (maxZ + VILLAGE_REACH) >> 4,
            villageSpacing,
            villageSeparation,
            StartGrid.VILLAGE_SALT);
    }
}
