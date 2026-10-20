package com.gregguns.entity.npc;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * Dictator Dave, the mini-boss: 100 health, melee 7, speed 0.40, follow range 60, 35 base XP. He fires a golden
 * revolver in bursts of six (the gun's own AI stats) and never despawns. Techguns gave him no boss bar, and neither
 * does this class (he is not an {@code IBossDisplayData}). He appears from spawn eggs and camps only. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (section 13), docs/techguns-spec/BUGS.md (NPC-28)
public class DictatorDave extends NpcEntity {

    /** Maximum health. */
    public static final double MAX_HEALTH = 100.0;
    /** Melee damage in half hearts. */
    public static final double ATTACK_DAMAGE = 7.0;
    /** Movement speed attribute. */
    public static final double SPEED = 0.40;
    /** Follow range in blocks. */
    public static final double FOLLOW_RANGE = 60.0;
    /** Base experience. */
    public static final int XP = 35;

    /**
     * @param world the world; the entity registry calls this when it loads him
     */
    public DictatorDave(World world) {
        super(world);
        experienceValue = XP;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(SPEED);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(FOLLOW_RANGE);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(ATTACK_DAMAGE);
    }

    @Override
    protected NpcProfile npcProfile() {
        return NpcProfiles.get("DictatorDave");
    }

    @Override
    protected boolean isPersistentNpc() {
        return true;
    }
}
