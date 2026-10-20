package com.gregguns.entity.npc;

import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * The basic Super Mutant: a large {@link NpcEntity} that carries a rocket launcher, AK-47, combat shotgun or laser
 * rifle (profile weights 1, 1, 1, 2) and no armor items. {@link SuperMutantHeavy} and {@link SuperMutantElite} extend
 * it and only change {@link #tier()}. Spawns from camps and eggs only; there is no natural spawn.
 *
 * <p>
 * Fixes NPC-25: each class sets its hitbox with {@code setSize(width, height)} in the constructor, width included, so
 * the box matches the 1.35 to 1.65 times model. Deviation: the sound set (the Cyberdemon's) is not wired yet. Both
 * sides; the AI runs on the server.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (14. Super Mutants), BUGS.md NPC-25
public class SuperMutantBasic extends NpcEntity {

    /**
     * @param world the world; the entity registry calls this when it loads a mutant
     */
    public SuperMutantBasic(World world) {
        super(world);
        setSize(tier().width(), tier().height());
    }

    /** @return the tier numbers of this class; subclasses override it, and it must not read instance fields */
    public SuperMutantTier tier() {
        return SuperMutantTier.BASIC;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        SuperMutantTier t = tier();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(t.health());
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(t.speed());
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(SuperMutantTier.FOLLOW_RANGE);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(t.damage());
    }

    @Override
    public int getTotalArmorValue() {
        return tier().armor();
    }

    @Override
    protected NpcProfile npcProfile() {
        return NpcProfiles.get(tier().profileId());
    }

    /**
     * An egg or command spawn rolls the gear itself: vanilla's egg path does not call {@code addRandomArmor}, and a
     * mutant without its gun would only punch. Camp and natural spawns set the danger first and roll through
     * {@link #rollEquipment()}.
     */
    @Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
        IEntityLivingData out = super.onSpawnWithEgg(data);
        if (!worldObj.isRemote && getHeldItem() == null) {
            rollEquipment();
        }
        return out;
    }
}
