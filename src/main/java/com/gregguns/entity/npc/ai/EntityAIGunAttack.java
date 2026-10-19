package com.gregguns.entity.npc.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcGunProfile;
import com.gregguns.entity.npc.RangedAttackTimer;

/**
 * The gun-driven attack task of an {@link NpcEntity}: feeds the pure {@link RangedAttackTimer} the distance and sight
 * each tick, stands still when it says so (else walks toward the target), and fires one shot per "true" answer, with
 * {@link RangedAttackTimer#lastFactor()} as the shot's distance factor. Mutex bits 3 (move and look). Server only.
 *
 * <p>
 * Deviation: the task ends as soon as the target is gone; Techguns kept running on a stale target until the path
 * ended (NPC-31).
 */
// Spec: docs/techguns-spec/10-entities-npcs/ai.md (section 3), docs/techguns-spec/BUGS.md (NPC-31)
public final class EntityAIGunAttack extends EntityAIBase {

    private static final float LOOK_STEP = 30.0F;
    private static final double MOVE_SPEED = 1.0;

    private final NpcEntity host;
    private final RangedAttackTimer timer;
    private EntityLivingBase target;

    /**
     * @param host    the NPC that shoots
     * @param profile the held gun's AI stats
     */
    public EntityAIGunAttack(NpcEntity host, NpcGunProfile profile) {
        this.host = host;
        this.timer = new RangedAttackTimer(profile);
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase candidate = host.getAttackTarget();
        if (!RangedAttackTimer.shouldStart(candidate != null && candidate.isEntityAlive())) {
            return false;
        }
        target = candidate;
        return true;
    }

    @Override
    public boolean continueExecuting() {
        EntityLivingBase current = host.getAttackTarget();
        return RangedAttackTimer.shouldContinue(current != null && current.isEntityAlive());
    }

    @Override
    public void resetTask() {
        target = null;
        timer.reset();
        host.getNavigator()
            .clearPathEntity();
    }

    @Override
    public void updateTask() {
        EntityLivingBase current = host.getAttackTarget();
        if (current == null) {
            return;
        }
        if (current != target) {
            // The target changed without the task restarting: drop the old per-target state (NPC-31).
            target = current;
            timer.reset();
        }
        double distance = host.getDistance(target.posX, target.boundingBox.minY, target.posZ);
        boolean seen = host.getEntitySenses()
            .canSee(target);
        boolean fire = timer.tick(distance, seen);
        if (timer.holdPosition(distance)) {
            host.getNavigator()
                .clearPathEntity();
        } else {
            host.getNavigator()
                .tryMoveToEntityLiving(target, MOVE_SPEED);
        }
        host.getLookHelper()
            .setLookPositionWithEntity(target, LOOK_STEP, LOOK_STEP);
        if (fire) {
            host.attackEntityWithRangedAttack(target, (float) timer.lastFactor());
        }
    }
}
