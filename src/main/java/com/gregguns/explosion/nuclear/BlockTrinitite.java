package com.gregguns.explosion.nuclear;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Trinitite: sand fused by the inner fireball of a nuclear blast (D21). Hard glassy rock that drops trinitite shards
 * (macerated, then centrifuged, in GT). Part of the fallout decay record like irradiated earth: it reverts to sand
 * when its blast's record comes due. Never silk-harvestable, so it cannot be placed outside every record.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md (The byproduct chain)
public final class BlockTrinitite extends Block implements com.gregguns.api.Tiered {

    /** Builds the block. */
    public BlockTrinitite() {
        super(Material.rock);
        setHardness(2.0F);
        setResistance(6.0F);
        setStepSound(soundTypeGlass);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    public com.gregguns.api.Tier getTier() {
        return com.gregguns.api.Tier.ZPM;
    }

    @Override
    public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        return false;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(FalloutBlocks.shard(), 1, ItemFalloutProduct.SHARD));
        return drops;
    }
}
