package com.gregguns.entity.npc.mob;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * The Commando: a soldier in full T2 Commando armor that carries the M4 (the silenced variant is not built yet). Gun
 * AI, gear and drops come from {@link NpcEntity} and the {@code Commando} entry of {@link NpcProfiles}; this class
 * only sets the stats. Camps and eggs only (no natural spawn). Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (Commando), docs/gregguns-spec/10-entities-npcs/index.md
public class Commando extends NpcEntity {

    /** Registry name key in {@link NpcProfiles}. */
    public static final String PROFILE = "Commando";
    /** Maximum health in half hearts. */
    public static final double HEALTH = 30.0;
    /** Melee damage in half hearts. */
    public static final double MELEE_DAMAGE = 5.0;
    /** Movement speed attribute. */
    public static final double SPEED = 0.35;
    /** Follow range in blocks. */
    public static final double FOLLOW_RANGE = 75.0;

    /**
     * @param world the world; the entity registry calls this when it loads a Commando
     */
    public Commando(World world) {
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
