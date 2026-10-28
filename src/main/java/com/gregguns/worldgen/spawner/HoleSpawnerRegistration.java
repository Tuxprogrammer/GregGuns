package com.gregguns.worldgen.spawner;

import java.util.EnumMap;
import java.util.Map;

import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.registry.Naming;

/**
 * Registers the hole spawner blocks, one per {@link HoleKind} and their tile (worldgen module). Common side.
 *
 * <p>
 * Usage: {@code HoleSpawnerRegistration.block(HoleKind.ZOMBIE)} is the Zombie Hole block (null before preInit).
 */
// Spec: PLAN-M8 G11
public final class HoleSpawnerRegistration {

    /** Tile entity name, {@code gregguns:hole_spawner}. */
    public static final String TILE = "hole_spawner";

    private static final Map<HoleKind, BlockHoleSpawner> BLOCKS = new EnumMap<>(HoleKind.class);

    private HoleSpawnerRegistration() {}

    /**
     * @param kind a kind; may be null
     * @return its block, or null for null or before {@link #preInit}
     */
    public static BlockHoleSpawner block(HoleKind kind) {
        if (kind == null) {
            return null;
        }
        return BLOCKS.get(kind);
    }

    /** @param registrar the worldgen module's registrar */
    public static void preInit(ModuleRegistrar registrar) {
        for (HoleKind kind : HoleKind.values()) {
            BlockHoleSpawner block = registrar
                .registerBlock(kind.blockName(), new BlockHoleSpawner(kind), ItemBlockHoleSpawner.class);
            block.setBlockTextureName(Naming.textureName(kind.textureName()));
            BLOCKS.put(kind, block);
        }
        registrar.registerTileEntity(TILE, TileHoleSpawner.class);
    }
}
