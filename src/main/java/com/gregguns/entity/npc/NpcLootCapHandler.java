package com.gregguns.entity.npc;

import java.util.Iterator;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import com.gregguns.GregGuns;
import com.gregguns.worldgen.loot.ObtainTier;
import com.gregguns.worldgen.loot.ObtainTierRules;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The last word on a GregGuns NPC's drops (D364): after every other handler, a drop above {@code loot.maxTier} by its
 * whole crafting chain is taken back, the vanilla equipment drop of worn armor included. A GregGuns or GT item with no
 * known tier goes too (fail closed); another mod's item with no known tier (Infernal Mobs' own loot) is that mod's
 * business and stays. Our own drop sites gate fail-closed before this. Server side.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (Loot tier cap); D364
public final class NpcLootCapHandler {

    /** The entity name prefix FML gives every GregGuns entity ({@code modid.name}). */
    private static final String ENTITY_PREFIX = GregGuns.MODID + ".";

    /** @param event the drops event */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrops(LivingDropsEvent event) {
        if (event.entityLiving == null || event.entityLiving.worldObj.isRemote || event.drops == null) {
            return;
        }
        String name = EntityList.getEntityString(event.entityLiving);
        if (name == null || !name.startsWith(ENTITY_PREFIX)) {
            return;
        }
        for (Iterator<EntityItem> it = event.drops.iterator(); it.hasNext();) {
            EntityItem e = it.next();
            if (e != null && removes(e.getEntityItem())) {
                it.remove();
            }
        }
    }

    /**
     * @param stack a drop
     * @return whether the cap takes it back
     */
    public static boolean removes(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        int tier = ObtainTier.of(stack);
        if (tier != ObtainTierRules.UNKNOWN) {
            return !ObtainTier.allowed(stack);
        }
        String reg = Item.itemRegistry.getNameForObject(stack.getItem());
        return reg == null || reg.startsWith(GregGuns.MODID + ":") || reg.startsWith("gregtech:");
    }
}
