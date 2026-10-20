package com.gregguns.entity.npc;

import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * The Bandit: a fast, lightly armored human raider that carries a pistol, AK-47, bolt-action rifle, sawed-off shotgun,
 * Thompson or revolver and wears scout armor. Gun AI, loot and equipment come from {@link NpcEntity} and the
 * {@code Bandit} entry of {@link NpcProfiles}; this class only sets the stats.
 *
 * <p>
 * Usage: spawned by its egg or the spawn handler; registered as {@code gregguns.Bandit}. Server and client.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (Bandit), docs/gregguns-spec/10-entities-npcs/index.md
public class Bandit extends NpcEntity {

    /** Maximum health in half hearts. */
    public static final double HEALTH = 20.0;
    /** Melee damage in half hearts. */
    public static final double MELEE_DAMAGE = 5.0;
    /** Movement speed attribute. */
    public static final double SPEED = 0.30;
    /** Follow range in blocks. */
    public static final double FOLLOW_RANGE = 40.0;

    /**
     * @param world the world; the entity registry calls this when it loads a Bandit
     */
    public Bandit(World world) {
        super(world);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        // Base values only: difficulty never scales these (GGS-10-29).
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(SPEED);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(FOLLOW_RANGE);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(MELEE_DAMAGE);
    }

    @Override
    protected NpcProfile npcProfile() {
        return NpcProfiles.get("Bandit");
    }

    /**
     * Vanilla's spawn-egg hook does not roll gear for this base; roll it here for an unarmed Bandit with no spawner
     * danger set. The spawn handler sets the danger and rolls itself, so it is never rolled twice.
     */
    @Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
        IEntityLivingData out = super.onSpawnWithEgg(data);
        if (getHeldItem() == null && danger() == DANGER_UNSET) {
            rollEquipment();
        }
        return out;
    }
}
