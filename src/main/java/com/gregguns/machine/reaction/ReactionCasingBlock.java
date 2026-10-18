package com.gregguns.machine.reaction;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.registry.ModuleRegistrar;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Textures;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;

/**
 * The Reaction Casing: the wall block of the Retort, with the hatch texture on casing page 50.
 *
 * <p>
 * One meta (0). It is a plain solid block that borrows iron's hardness; hatches placed in the structure draw the same
 * face through the casing texture id {@link #TEXTURE_ID}. Registered in preInit by {@link #register}; the page
 * registration is common-side because GT keeps the table on both sides. Both sides.
 */
// Spec: docs/gregguns-spec/08-machines-production/metatileentity-framework.md (casing texture pages, GGS-08-04),
// docs/gregguns-spec/08-machines-production/recipes.md (Reaction Chamber Casing)
// Deviation: not a BlockCasingsAbstract subclass. That base registers itself with GT's machine-block table and the
// GregTech casings creative tab; GregGuns needs neither (it never joins GT's casing-update chain), and its own
// registrar gives the block the GregGuns tab, the lang key and the self-check coverage.
public final class ReactionCasingBlock extends Block implements Tiered {

    /** Registry name of the block. */
    public static final String NAME = "casing_reaction";
    /** GregGuns' casing texture page (GGS-08-04). */
    public static final byte TEXTURE_PAGE = 50;
    /** The id a hatch adder passes to {@code casingIndex}: page 50, index 0. */
    public static final int TEXTURE_ID = (TEXTURE_PAGE << 7) | 0;

    private static ReactionCasingBlock instance;

    private ReactionCasingBlock() {
        super(Material.iron);
        setStepSound(soundTypeMetal);
        setHardness(5.0F);
        setResistance(10.0F);
        setHarvestLevel("wrench", 2);
        setBlockTextureName("gregguns:casings/reaction_casing");
    }

    /**
     * Registers the block and its casing texture. Call once from {@code MachinesModule.preInit}.
     *
     * @param registrar the machines module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        instance = registrar.registerBlock(NAME, new ReactionCasingBlock());
        // The registrar sets a default texture name; the art lives under casings/.
        instance.setBlockTextureName("gregguns:casings/reaction_casing");
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1847-L1853
        // (addTexturePage) and Textures.java#L2677 (setCasingTextureForId), as BlockCasingsAbstract#L56-L61 does.
        GTUtility.addTexturePage(TEXTURE_PAGE);
        Textures.BlockIcons.setCasingTextureForId(TEXTURE_ID, TextureFactory.of(instance, 0));
    }

    /** @return the registered block, or {@code null} when the machines module is off */
    public static Block get() {
        return instance;
    }

    @Override
    public Tier getTier() {
        return Tier.EV;
    }

    @Override
    public float getBlockHardness(World world, int x, int y, int z) {
        return Blocks.iron_block.getBlockHardness(world, x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(getTextureName());
    }
}
