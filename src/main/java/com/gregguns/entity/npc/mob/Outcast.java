package com.gregguns.entity.npc.mob;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * The Outcast: a slow, heavily armed soldier in full T3 Power Armor (camo 1 or 2 per piece) that carries one of seven
 * energy or heavy guns. Gun AI, gear and drops come from {@link NpcEntity} and the {@code Outcast} entry of
 * {@link NpcProfiles}; this class only sets the stats. Energy guns draw on {@code ChargeSource.INFINITE}, so the
 * Outcast never runs dry. Egg and camp only (no natural spawn). Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (8. Outcast), docs/gregguns-spec/10-entities-npcs/index.md
public class Outcast extends NpcEntity {

    /** Registry name key in {@link NpcProfiles}. */
    public static final String PROFILE = "Outcast";
    /** Maximum health in half hearts. */
    public static final double HEALTH = 30.0;
    /** Melee damage in half hearts. */
    public static final double MELEE_DAMAGE = 8.0;
    /** Movement speed attribute. */
    public static final double SPEED = 0.20;
    /** Follow range in blocks. */
    public static final double FOLLOW_RANGE = 60.0;

    /**
     * @param world the world; the entity registry calls this when it loads a Outcast
     */
    public Outcast(World world) {
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
        return NpcProfiles.get(PROFILE);
    }
}
