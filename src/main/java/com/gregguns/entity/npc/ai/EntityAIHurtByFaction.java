package com.gregguns.entity.npc.ai;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.player.EntityPlayer;

import com.gregguns.entity.npc.HasNpcFaction;
import com.gregguns.entity.npc.NpcTargetRules;

/**
 * Retaliation target task: an NPC strikes back at whoever hurt it, except that another NPC counts only when its
 * faction is hostile to the victim's. Hostile NPCs caught in each other's crossfire therefore never infight. Server
 * only.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (section 4), docs/gregguns-spec/10-entities-npcs/spawning.md
public final class EntityAIHurtByFaction extends EntityAIHurtByTarget {

    private final HasNpcFaction owner;

    /**
     * @param host         the NPC; its class must implement {@link HasNpcFaction}
     * @param callsForHelp whether nearby same-class mobs join in; false for NPCs
     * @param <T>          the NPC type
     */
    public <T extends EntityCreature & HasNpcFaction> EntityAIHurtByFaction(T host, boolean callsForHelp) {
        super(host, callsForHelp);
        this.owner = host;
    }

    @Override
    protected boolean isSuitableTarget(EntityLivingBase attacker, boolean needsSight) {
        boolean attackerIsNpc = attacker instanceof HasNpcFaction;
        if (!NpcTargetRules.shouldRetaliate(
            owner.npcFaction(),
            attacker instanceof EntityPlayer,
            attackerIsNpc,
            attackerIsNpc ? ((HasNpcFaction) attacker).npcFaction() : null)) {
            return false;
        }
        if (attackerIsNpc) {
            // Spec: only the faction table decides between two NPCs; the vanilla checks are skipped.
            return attacker.isEntityAlive() && attacker != taskOwner;
        }
        return super.isSuitableTarget(attacker, needsSight);
    }
}
