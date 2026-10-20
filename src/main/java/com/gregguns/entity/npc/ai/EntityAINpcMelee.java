package com.gregguns.entity.npc.ai;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.player.EntityPlayer;

import com.gregguns.entity.npc.NpcTargetRules;

/**
 * The melee combat task. Unlike vanilla's class-filtered form it may hit any attack target, which only ever is a
 * player (seek task) or a revenge target (hurt-by task), so a melee NPC fights back against a turret that shot it
 * (fixes NPC-24). It still refuses a player who cannot be damaged. Server only.
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (section 2), docs/techguns-spec/BUGS.md (NPC-24)
public final class EntityAINpcMelee extends EntityAIAttackOnCollide {

    private final EntityCreature host;

    /**
     * @param host       the attacker
     * @param speed      path speed multiplier
     * @param longMemory whether to keep chasing a target out of sight
     */
    public EntityAINpcMelee(EntityCreature host, double speed, boolean longMemory) {
        super(host, speed, longMemory);
        this.host = host;
    }

    private boolean allowed() {
        EntityLivingBase target = host.getAttackTarget();
        if (target == null) {
            return false;
        }
        boolean player = target instanceof EntityPlayer;
        return NpcTargetRules.canMelee(
            player,
            player && ((EntityPlayer) target).capabilities.disableDamage,
            target == host.getAITarget());
    }

    @Override
    public boolean shouldExecute() {
        return allowed() && super.shouldExecute();
    }

    @Override
    public boolean continueExecuting() {
        return allowed() && super.continueExecuting();
    }
}
