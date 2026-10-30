package com.gregguns.icbm.warhead;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.IcbmParts;
import com.gregguns.icbm.tools.ItemDefuser;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

/**
 * Shapeless crafting, the first step of de-arming (D283) and the mirror of {@link WarheadChargeRecipe}: one live
 * warhead and a Defuse Kit give the same warhead with up to 8 fewer charges, or, once the last charge is out, the inert
 * warhead with the payload it was built with. The charges come back whole: {@link #onCrafted} hands them to the player
 * (the akimbo split's precedent, GUN-26), and the Defuse Kit stays in the grid one use down
 * ({@link ItemDefuser#getContainerItem}). A warhead whose tag lacks its explosive or fill, or names an explosive its
 * casing cannot carry, makes no recipe, so no stack the chain did not make gives anything back. Common side; the
 * charges are given on the server only.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (De-arming)
// Source: Forge 1.7.10 net.minecraft.inventory.SlotCrafting#onPickupFromSlot (fires FML's ItemCraftedEvent with the
// grid still full, then takes one of each stack and puts back a container item that does not leave the grid).
public final class WarheadUnloadRecipe implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting grid, World world) {
        return result(grid) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting grid) {
        return result(grid);
    }

    /**
     * @param grid the crafting grid
     * @return the warhead after one unload, or null when the grid is not exactly one live warhead and one Defuse Kit
     */
    public static ItemStack result(IInventory grid) {
        ItemStack warhead = warhead(grid);
        return warhead == null ? null : unload(warhead);
    }

    /**
     * @param grid the crafting grid
     * @return the one live warhead of a valid unload grid, or null
     */
    static ItemStack warhead(IInventory grid) {
        ItemStack warhead = null;
        boolean defuser = false;
        for (int i = 0; i < grid.getSizeInventory(); i++) {
            ItemStack s = grid.getStackInSlot(i);
            if (s == null) {
                continue;
            }
            if (s.getItem() instanceof ItemDefuser && !defuser) {
                defuser = true;
            } else if (s.getItem() instanceof ItemBlockWarhead && warhead == null) {
                warhead = s;
            } else {
                return null;
            }
        }
        return defuser ? warhead : null;
    }

    /**
     * @param warhead a live warhead stack
     * @return its explosive and fill as the blast reads them, or null when the tag is not one the chain writes: no
     *         explosive or fill key, or an explosive that is unknown or of another tier
     */
    static WarheadData charged(ItemStack warhead) {
        if (warhead == null || !(warhead.getItem() instanceof ItemBlockWarhead)) {
            return null;
        }
        NBTTagCompound tag = warhead.getTagCompound();
        if (tag == null || !tag.hasKey(WarheadStacks.K_EXPLOSIVE)
            || !tag.hasKey(WarheadStacks.K_FILL)
            || tag.getInteger(WarheadStacks.K_FILL) < 1) {
            return null;
        }
        WarheadCasing casing = ((ItemBlockWarhead) warhead.getItem()).casing();
        ExplosiveDef def = Explosives.get(tag.getString(WarheadStacks.K_EXPLOSIVE));
        if (def == null || def.tier() != casing.tier()) {
            return null;
        }
        // The fill is clamped to the cap exactly as the blast reads it, so a hand-edited stack never gives back more
        // charges than its casing holds.
        return WarheadStacks.read(casing, tag);
    }

    /**
     * @param warhead a live warhead stack
     * @return the warhead with {@link WarheadDearming#unloaded} fewer charges, the inert warhead with its payload once
     *         empty, or null when the stack cannot be unloaded
     */
    public static ItemStack unload(ItemStack warhead) {
        WarheadData data = charged(warhead);
        if (data == null) {
            return null;
        }
        WarheadCasing casing = ((ItemBlockWarhead) warhead.getItem()).casing();
        int left = WarheadDearming.fillLeft(data.fill());
        if (left > 0) {
            return WarheadStacks.make(Item.getItemFromBlock(WarheadBlocks.of(casing)), casing, data.explosive(), left);
        }
        String payload = WarheadDearming.payloadOf(data.explosive());
        if (payload != null && InertWarheads.validPayload(casing, payload) == null) {
            // An explosive with no payload recipe of this casing: refuse rather than drop what it was built with.
            return null;
        }
        return InertWarheads.stack(casing, payload);
    }

    /**
     * @param warhead a live warhead stack
     * @return the Plastic Explosive one unload of it gives back; 0 when it cannot be unloaded
     */
    public static int chargesOut(ItemStack warhead) {
        WarheadData data = charged(warhead);
        return data == null || unload(warhead) == null ? 0 : WarheadDearming.unloaded(data.fill());
    }

    /**
     * Hands the unloaded charges to the player, on the server only. FML fires this before the grid gives up its
     * ingredients, so the grid still holds the warhead the result was made from. Anything that does not fit drops at
     * the player's feet.
     *
     * @param event the crafting event
     */
    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.player == null || event.player.worldObj.isRemote
            || event.crafting == null
            || !(event.crafting.getItem() instanceof ItemBlockWarhead || InertWarheads.is(event.crafting))
            || event.craftMatrix == null) {
            return;
        }
        ItemStack warhead = warhead(event.craftMatrix);
        ItemStack expected = warhead == null ? null : unload(warhead);
        // Only the craft this recipe made: the same item and tag as its result (a charge load also crafts a warhead).
        if (expected == null || expected.getItem() != event.crafting.getItem()
            || !ItemStack.areItemStackTagsEqual(expected, event.crafting)) {
            return;
        }
        give(event.player, chargesOut(warhead));
    }

    private static void give(EntityPlayer player, int charges) {
        Item charge = IcbmParts.get(IcbmPart.PLASTIC_EXPLOSIVE);
        if (charge == null) {
            return;
        }
        for (int size : WarheadDearming.chargeStacks(charges)) {
            ItemStack stack = new ItemStack(charge, size);
            if (!player.inventory.addItemStackToInventory(stack)) {
                player.dropPlayerItemWithRandomChoice(stack, false);
            }
        }
    }

    @Override
    public int getRecipeSize() {
        return 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return null;
    }
}
