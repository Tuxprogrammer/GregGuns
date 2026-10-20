package com.gregguns.worldgen.spawner;

import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the two hole spawner blocks and their tile (worldgen module). Common side.
 *
 * <p>
 * Usage: {@code HoleSpawnerRegistration.block(HoleKind.ZOMBIE)} is the Zombie Hole block (null before preInit).
 */
// Spec: PLAN-M8 G11
public final class HoleSpawnerRegistration {

    /** Tile entity name, {@code gregguns:hole_spawner}. */
    public static final String TILE = "hole_spawner";

    private static BlockHoleSpawner zombie;
    private static BlockHoleSpawner bug;

    private HoleSpawnerRegistration() {}

    /**
     * @param kind a kind; may be null
     * @return its block, or null for null or before {@link #preInit}
     */
    public static BlockHoleSpawner block(HoleKind kind) {
        if (kind == null) {
            return null;
        }
        return kind == HoleKind.ZOMBIE ? zombie : bug;
    }

    /** @param registrar the worldgen module's registrar */
    public static void preInit(ModuleRegistrar registrar) {
        zombie = registrar.registerBlock(HoleKind.ZOMBIE.blockName(), new BlockHoleSpawner(HoleKind.ZOMBIE));
        bug = registrar.registerBlock(HoleKind.BUG.blockName(), new BlockHoleSpawner(HoleKind.BUG));
        registrar.registerTileEntity(TILE, TileHoleSpawner.class);
    }
}
