package com.gregguns.utility;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.utility.blob.BlockBioBlob;
import com.gregguns.utility.blob.TileBioBlob;
import com.gregguns.utility.camp.BlockCampFlag;
import com.gregguns.utility.camp.TileCampFlag;
import com.gregguns.utility.storage.BlockGregChest;
import com.gregguns.utility.storage.TileGregChest;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * Registers the Reinforced Chest, Weapon Chest, Bio Blob and Camp Flag (chunk m6/world-blocks): blocks, tile entities,
 * chest recipes, scenarios and, on a client, the placeholder renderers. Common side; the client half is reached by name
 * only when the side is the client.
 *
 * <p>
 * Usage: {@code WorldBlockRegistration.bioBlob()} is the block a Biogun projectile spawns (null with the utility
 * module off).
 */
// Spec: docs/ROADMAP.md (M6), docs/techguns-spec/09-machines-utility/misc-tile-entities.md
public final class WorldBlockRegistration {

    /** Bare names, also the registry ids under {@code gregguns:}. */
    public static final String REINFORCED_CHEST = "reinforced_chest";
    /** See {@link #REINFORCED_CHEST}. */
    public static final String WEAPON_CHEST = "weapon_chest";
    /** See {@link #REINFORCED_CHEST}. */
    public static final String BIO_BLOB = "bio_blob";
    /** See {@link #REINFORCED_CHEST}. */
    public static final String CAMP_FLAG = "camp_flag";

    // Block textures that exist today; the chest and flag models have their own sheets and use these for particles.
    private static final String PARTICLE_CHEST = "gregguns:casings/gantry_casing";
    private static final String PARTICLE_FLAG = "gregguns:casings/gantry_beam";

    private static BlockGregChest reinforcedChest;
    private static BlockGregChest weaponChest;
    private static BlockBioBlob bioBlob;
    private static BlockCampFlag campFlag;

    private WorldBlockRegistration() {}

    /** @return the Reinforced Chest; null before registration */
    public static BlockGregChest reinforcedChest() {
        return reinforcedChest;
    }

    /** @return the Weapon Chest; null before registration */
    public static BlockGregChest weaponChest() {
        return weaponChest;
    }

    /** @return the Bio Blob block; null before registration */
    public static BlockBioBlob bioBlob() {
        return bioBlob;
    }

    /** @return the Camp Flag block; null before registration */
    public static BlockCampFlag campFlag() {
        return campFlag;
    }

    /**
     * Registers the four blocks (in {@code UtilityItemOrder} order, via {@link #registerOne}) and their tile
     * entities. Kept for callers that do not care about the creative and NEI order.
     *
     * @param registrar the utility module's registrar
     */
    public static void preInit(ModuleRegistrar registrar) {
        registerOne(registrar, REINFORCED_CHEST);
        registerOne(registrar, WEAPON_CHEST);
        registerOne(registrar, CAMP_FLAG);
        registerOne(registrar, BIO_BLOB);
        registerTileEntities(registrar);
    }

    /**
     * Registers one world block by registry name, so the caller picks the registration order.
     *
     * @param registrar the utility module's registrar
     * @param name      one of the four block names above
     */
    public static void registerOne(ModuleRegistrar registrar, String name) {
        switch (name) {
            case REINFORCED_CHEST:
                reinforcedChest = registrar.registerBlock(name, new BlockGregChest(false));
                reinforcedChest.setBlockTextureName(PARTICLE_CHEST);
                break;
            case WEAPON_CHEST:
                weaponChest = registrar.registerBlock(name, new BlockGregChest(true));
                weaponChest.setBlockTextureName(PARTICLE_CHEST);
                break;
            case CAMP_FLAG:
                campFlag = registrar.registerBlock(name, new BlockCampFlag());
                campFlag.setBlockTextureName(PARTICLE_FLAG);
                break;
            case BIO_BLOB:
                bioBlob = registrar.registerBlock(name, new BlockBioBlob());
                break;
            default:
                throw new IllegalArgumentException("not a world block: " + name);
        }
    }

    /** Registers the tile entities of the four blocks; independent of the block order. */
    public static void registerTileEntities(ModuleRegistrar registrar) {
        registrar.registerTileEntity("chest", TileGregChest.class);
        registrar.registerTileEntity(BIO_BLOB, TileBioBlob.class);
        registrar.registerTileEntity(CAMP_FLAG, TileCampFlag.class);
    }

    /** Client renderers of the world blocks; always registered, see {@code GregGunsModule.initContent}. */
    public static void initContent() {
        if (FMLCommonHandler.instance()
            .getSide()
            .isClient()) {
            registerClient();
        }
    }

    /** Recipes and event handlers. */
    @SuppressWarnings("unchecked")
    public static void init() {
        // Spec: misc-tile-entities.md section 1 (recipes). Deviation: plateIron, since GT's WroughtIron plate has no
        // items.
        CraftingManager.getInstance()
            .getRecipeList()
            .add(
                new ShapedOreRecipe(
                    new ItemStack(reinforcedChest),
                    "n n",
                    " c ",
                    "n n",
                    'n',
                    "nuggetIron",
                    'c',
                    Blocks.chest));
        CraftingManager.getInstance()
            .getRecipeList()
            .add(new ShapedOreRecipe(new ItemStack(weaponChest), "pcp", 'p', "plateIron", 'c', Blocks.chest));
    }

    private static void registerClient() {
        com.gregguns.client.utility.WorldBlocksClient.register();
    }

    /**
     * Dev-only scenarios.
     *
     * @param registry where to add them
     */
    public static void registerScenarios(ScenarioRegistry registry) {
        WorldBlockScenarios.registerAll(registry);
        registry.register(new ChestLookScenario());
    }

    /** @return the four blocks, for tests and the self-check */
    public static Block[] blocks() {
        return new Block[] { reinforcedChest, weaponChest, bioBlob, campFlag };
    }
}
