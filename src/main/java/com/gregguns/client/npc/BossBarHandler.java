package com.gregguns.client.npc;

import net.minecraft.entity.boss.BossStatus;
import net.minecraftforge.client.event.RenderLivingEvent;

import com.gregguns.entity.npc.PsychoSteve;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Shows the vanilla boss bar for Psycho Steve while he is rendered, the way vanilla's dragon and wither do. Techguns
 * implemented the boss interface but never set the status (NPC-22). Client only.
 */
// Spec: docs/techguns-spec/BUGS.md (NPC-22), docs/techguns-spec/10-entities-npcs/per-mob.md (section 12)
public final class BossBarHandler {

    /**
     * @param event the pre-render event of any living entity
     */
    @SubscribeEvent
    public void onRender(RenderLivingEvent.Pre event) {
        if (event.entity instanceof PsychoSteve && !event.entity.isDead) {
            BossStatus.setBossStatus((PsychoSteve) event.entity, true);
        }
    }
}
