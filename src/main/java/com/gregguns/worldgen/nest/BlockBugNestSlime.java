package com.gregguns.worldgen.nest;

import java.util.Random;

import net.minecraft.block.BlockVine;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The climbable slime trail of the Alien Bug Nest: a vine that clings to the wall faces its metadata names (the vine
 * bit layout: 1 south, 2 west, 4 north, 8 east), so a player can climb the entrance shaft. It never spreads and is not
 * tinted by the biome. Both sides.
 */
// Spec: docs/techguns-spec/11-worldgen/structure-notes/alien_bug_nest.md (slime trail, climbable, wall-attached)
public final class BlockBugNestSlime extends BlockVine implements Tiered {

    @Override
    public Tier getTier() {
        return Tier.LV;
    }

    /** A vine spreads on random ticks; the nest trail must stay where worldgen put it. */
    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {}

    @Override
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int meta) {
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return 0xFFFFFF;
    }
}
