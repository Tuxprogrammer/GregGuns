package com.gregguns.worldgen.spawner;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.gregguns.api.HiddenFromCreativeTab;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A hole spawner block, one instance per {@link HoleKind} (Zombie Hole, Bug Hole). Worldgen places it; players never
 * craft it and the creative tab does not list it. Like a vanilla spawner it drops nothing. One texture on every face.
 * Both sides.
 */
// Spec: PLAN-M8 G11
public final class BlockHoleSpawner extends BlockContainer implements Tiered, HiddenFromCreativeTab {

    private final HoleKind kind;
    private IIcon icon;

    /** @param kind which NPCs the tile places */
    public BlockHoleSpawner(HoleKind kind) {
        super(Material.rock);
        this.kind = kind;
        setHardness(3.0F);
        setResistance(15.0F);
        setStepSound(soundTypeStone);
    }

    /** @return which NPCs this block's tile places */
    public HoleKind kind() {
        return kind;
    }

    @Override
    public Tier getTier() {
        return kind.isSiloPost() ? Tier.EV : Tier.LV;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileHoleSpawner(kind);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icon = register.registerIcon(getTextureName());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return icon;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        return new ArrayList<>();
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }
}
