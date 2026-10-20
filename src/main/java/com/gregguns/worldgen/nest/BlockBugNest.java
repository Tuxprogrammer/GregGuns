package com.gregguns.worldgen.nest;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Alien Bug Nest's two solid blocks in one metadata block: meta 0 is the hard resin of the walls, meta 1 the egg
 * cluster. Worldgen places both; players can mine them (resin is hard, the eggs are soft). Both sides.
 */
// Spec: docs/techguns-spec/11-worldgen/structure-notes/alien_bug_nest.md (block roles: nest wall, egg)
public final class BlockBugNest extends Block implements Tiered {

    /** Meta of the hard resin wall. */
    public static final int META_RESIN = 0;
    /** Meta of the egg cluster. */
    public static final int META_EGGS = 1;

    private IIcon resin;
    private IIcon eggs;

    /** Creates the block; {@link NestBlockRegistration} names it. */
    public BlockBugNest() {
        super(Material.rock);
        setHardness(3.0F);
        setResistance(10.0F);
        setStepSound(soundTypeStone);
    }

    @Override
    public Tier getTier() {
        return Tier.LV;
    }

    @Override
    public float getBlockHardness(net.minecraft.world.World world, int x, int y, int z) {
        return world.getBlockMetadata(x, y, z) == META_EGGS ? 0.6F : blockHardness;
    }

    @Override
    public int damageDropped(int meta) {
        return meta == META_EGGS ? META_EGGS : META_RESIN;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        // Two textures for two metas; the block's own texture name is the resin (the self-check looks for that one).
        resin = register.registerIcon(getTextureName());
        eggs = register.registerIcon(getTextureName()
            .replace("bugnestblock", "bugnesteggs"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return meta == META_EGGS ? eggs : resin;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, META_RESIN));
        list.add(new ItemStack(item, 1, META_EGGS));
    }
}
