package com.gregguns.utility.charge;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * The container of the charge GUI: it has no slots. It stays open while the charge is still there and within 8 blocks.
 * Button presses do not go through the container; they are {@link PacketChargeButton}s. Both sides.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (Container and GUI)
public final class ChargeContainer extends Container {

    private static final double REACH_SQUARED = 8.0 * 8.0;

    private final World world;
    private final int x;
    private final int y;
    private final int z;

    /**
     * @param world the world
     * @param x     charge x
     * @param y     charge y
     * @param z     charge z
     */
    public ChargeContainer(World world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** @return the charge being edited, or null when it is gone */
    public TileExplosiveCharge charge() {
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileExplosiveCharge ? (TileExplosiveCharge) tile : null;
    }

    /** @return charge x */
    public int x() {
        return x;
    }

    /** @return charge y */
    public int y() {
        return y;
    }

    /** @return charge z */
    public int z() {
        return z;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        TileExplosiveCharge charge = charge();
        return charge != null && !charge.params()
            .armed() && player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= REACH_SQUARED;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
        return null;
    }
}
