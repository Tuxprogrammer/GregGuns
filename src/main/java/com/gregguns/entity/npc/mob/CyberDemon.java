package com.gregguns.entity.npc.mob;

import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.gregguns.core.config.NpcConfig;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * The Cyberdemon: a fire-immune Nether brute (30 health, 7 melee damage, speed 0.35, follow range 50) that carries the
 * Nether Blaster. It never raises its arms to aim, which is a model matter ({@link #isAiming()} is always false).
 *
 * <p>
 * Gear: spawned by egg, command or camp it rolls its profile, which is the Nether Blaster only; loaded from save data
 * it keeps what it was saved with. Spawning: {@link #getCanSpawnHere()} is the Nether-only, flag-gated check
 * ({@link CyberDemonRules}); eggs and commands skip it. Deviation: Techguns spawned it naturally in the Nether;
 * design 0005 does not. Both sides; the AI runs on the server.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (11. Cyberdemon), BUGS NPC-06 (aggro sound not reproduced)
public class CyberDemon extends NpcEntity {

    /** Maximum health. */
    public static final double HEALTH = 30.0;
    /** Melee attack damage. */
    public static final double ATTACK = 7.0;
    /** Movement speed attribute. */
    public static final double SPEED = 0.35;
    /** Follow range in blocks. */
    public static final double FOLLOW_RANGE = 50.0;

    /**
     * @param world the world; the entity registry calls this when it loads a Cyberdemon
     */
    public CyberDemon(World world) {
        super(world);
        isImmuneToFire = true;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(SPEED);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(FOLLOW_RANGE);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(ATTACK);
    }

    @Override
    protected NpcProfile npcProfile() {
        return NpcProfiles.get("CyberDemon");
    }

    /**
     * Egg, command and camp spawns call this; the base class rolls gear only through {@code addRandomArmor}, which
     * vanilla does not call from here, so the Cyberdemon asks for its roll itself.
     */
    @Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
        IEntityLivingData out = super.onSpawnWithEgg(data);
        if (!worldObj.isRemote && getHeldItem() == null) {
            rollEquipment();
        }
        return out;
    }

    @Override
    public boolean getCanSpawnHere() {
        return CyberDemonRules.canSpawnAt(worldObj.provider.dimensionId, NpcConfig.cyberdemon)
            && super.getCanSpawnHere();
    }

    /** The Cyberdemon keeps its arms down while firing. */
    @Override
    public boolean isAiming() {
        return false;
    }
}
