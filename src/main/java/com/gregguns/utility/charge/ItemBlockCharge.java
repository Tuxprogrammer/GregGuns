package com.gregguns.utility.charge;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * The item of the explosive charges. On placement it records the clicked face, the owner and the type on the tile, and
 * plays the plant sound on the server. Both sides.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (Placement and shape)
public final class ItemBlockCharge extends ItemBlock {

    /**
     * @param block the charge block
     */
    public ItemBlockCharge(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return ChargeType.fromMeta(damage)
            .meta();
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        // tile.gregguns.explosive_charge / tile.gregguns.advanced_explosive_charge
        return "tile.gregguns." + ChargeType.fromMeta(stack.getItemDamage())
            .itemName();
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ, int metadata) {
        if (!super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileExplosiveCharge) {
            ChargeType type = ChargeType.fromMeta(metadata);
            ((TileExplosiveCharge) tile).init(type, side, player);
            if (!world.isRemote) {
                ((TileExplosiveCharge) tile).playSound(type.plantSound());
            }
        }
        return true;
    }
}
