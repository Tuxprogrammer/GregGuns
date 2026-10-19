package com.gregguns.utility;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.gregguns.utility.storage.ContainerGregChest;
import com.gregguns.utility.storage.TileGregChest;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The FML GUI dispatcher for the utility blocks. FML keeps one handler per mod, so {@code EquipmentGuiHandler}
 * forwards every id from {@link #FIRST_ID} up to here. The charge and chest chunks each fill their own case and touch
 * nothing else.
 */
// Spec: docs/gregguns-spec/09-machines-utility/explosive-charges.md, docs/gregguns-spec/07-blocks/storage-chests.md
public final class UtilityGuis {

    /** First GUI id owned by the utility module; lower ids belong to the armor module. */
    public static final int FIRST_ID = 30;
    /** The explosive charge settings GUI; x, y, z are the block position. */
    public static final int CHARGE = 30;
    /** The Reinforced Chest and Weapon Chest GUI; x, y, z are the block position. */
    public static final int CHEST = 31;

    private UtilityGuis() {}

    /**
     * @param id an FML GUI id
     * @return true when this class owns the id
     */
    public static boolean owns(int id) {
        return id >= FIRST_ID;
    }

    /**
     * Server half: the container for a GUI.
     *
     * @param id     the GUI id
     * @param player the opening player
     * @param world  the player's world
     * @param x      block x
     * @param y      block y
     * @param z      block z
     * @return the container, or null for an id with no GUI yet
     */
    public static Object serverElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        switch (id) {
            case CHARGE:
                return new com.gregguns.utility.charge.ChargeContainer(world, x, y, z);
            case CHEST:
                return world.getTileEntity(x, y, z) instanceof TileGregChest
                    ? new ContainerGregChest(player, (TileGregChest) world.getTileEntity(x, y, z))
                    : null;
            default:
                return null;
        }
    }

    /**
     * Client half: the screen for a GUI.
     *
     * @param id     the GUI id
     * @param player the opening player
     * @param world  the player's world
     * @param x      block x
     * @param y      block y
     * @param z      block z
     * @return the screen, or null for an id with no GUI yet
     */
    @SideOnly(Side.CLIENT)
    public static Object clientElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        switch (id) {
            case CHARGE:
                return new com.gregguns.client.utility.ChargeGui(
                    new com.gregguns.utility.charge.ChargeContainer(world, x, y, z));
            case CHEST:
                return world.getTileEntity(x, y, z) instanceof TileGregChest
                    ? new com.gregguns.client.utility.GuiGregChest(
                        new ContainerGregChest(player, (TileGregChest) world.getTileEntity(x, y, z)))
                    : null;
            default:
                return null;
        }
    }
}
