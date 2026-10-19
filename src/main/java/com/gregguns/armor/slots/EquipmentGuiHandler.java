package com.gregguns.armor.slots;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.gregguns.utility.UtilityGuis;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Vanilla/FML GUI handler for the Ammo Pouch ({@link AmmoPouch#GUI_HELD}, {@link AmmoPouch#GUI_WORN}). FML keeps one
 * {@code IGuiHandler} per mod, so another module that needs this kind of GUI must add its ids here instead of
 * registering a second handler (machines use ModularUI2 and are not affected). Server and client; the client GUI class
 * is only loaded when {@link #getClientGuiElement} runs.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md (GGS-05-03)
// Source: FML 1.7.10 cpw.mods.fml.common.network.NetworkRegistry#registerGuiHandler (one handler per mod instance)
public final class EquipmentGuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (UtilityGuis.owns(id)) {
            return UtilityGuis.serverElement(id, player, world, x, y, z);
        }
        PouchInventory pouch = pouchFor(id, player, x);
        return pouch == null ? null : new ContainerAmmoPouch(player, pouch, id, x);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (UtilityGuis.owns(id)) {
            return UtilityGuis.clientElement(id, player, world, x, y, z);
        }
        PouchInventory pouch = pouchFor(id, player, x);
        return pouch == null ? null
            : new com.gregguns.client.armor.GuiAmmoPouch(new ContainerAmmoPouch(player, pouch, id, x));
    }

    private static PouchInventory pouchFor(int id, EntityPlayer player, int index) {
        if (id == AmmoPouch.GUI_HELD) {
            return AmmoPouch.heldAt(player, index);
        }
        if (id == AmmoPouch.GUI_WORN) {
            return AmmoPouch.wornAt(player, index);
        }
        return null;
    }
}
