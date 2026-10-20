package com.gregguns.entity.npc;

import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import com.gregguns.core.config.NpcConfig;
import com.gregguns.entity.npc.loot.InfernalLootTable;
import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.entity.npc.loot.NpcProfile;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Adds tiered GregGuns items to the drops of GregGuns NPCs that are elite, ultra or infernal. Registered only when
 * Infernal Mobs is loaded. Runs at {@code HIGH}: the Infernal Mobs handler (normal priority) removes the entity from
 * its modifier map after it runs, so a later handler would see no modifiers. Acts only on player kills, as the core
 * does, and only on a GregGuns NPC with a profile. Server side.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/infernal-mobs.md (GGS-10-10 to GGS-10-12), docs/design/0007
public final class InfernalDropsHandler {

    /**
     * @param event the drops event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDrops(LivingDropsEvent event) {
        // Source:
        // https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/events/EntityEventHandler.java#L222-L236
        // That handler runs at normal priority and calls removeEntFromElites last, so this one reads first.
        if (!NpcConfig.infernalTieredLoot || event.entity.worldObj.isRemote
            || !event.recentlyHit
            || !(event.entityLiving instanceof NpcEntity)) {
            return;
        }
        NpcEntity npc = (NpcEntity) event.entityLiving;
        NpcProfile profile = npc.npcProfile();
        int size = InfernalHooks.modifierSize(npc);
        if (profile == null || size <= 0) {
            return;
        }
        List<LootDrop> picks = InfernalLootTable.roll(npc.getRNG(), size, profile.ceiling());
        for (LootDrop pick : picks) {
            ItemStack stack = NpcItems.stack(pick, npc.getRNG());
            if (stack != null && stack.stackSize > 0) {
                EntityItem item = new EntityItem(npc.worldObj, npc.posX, npc.posY, npc.posZ, stack);
                item.delayBeforeCanPickup = 10;
                event.drops.add(item);
            }
        }
    }
}
