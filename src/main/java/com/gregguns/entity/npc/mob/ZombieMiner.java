package com.gregguns.entity.npc.mob;

import net.minecraft.world.World;

import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * A zombie miner that swings a pickaxe or fires a hand cannon. Stats 20.0 health, 4.0 melee, speed 0.20, follow range
 * 50.0. Gear and drops come from its {@link NpcProfiles} entry.
 * Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (3. Zombie Miner),
// docs/gregguns-spec/10-entities-npcs/mob-drops-nei.md
public class ZombieMiner extends ZombieNpc {

    /** Registry name key in {@link NpcProfiles}. */
    public static final String PROFILE = "ZombieMiner";
    /** Max health. */
    public static final double HEALTH = 20.0;
    /** Melee damage. */
    public static final double ATTACK = 4.0;
    /** Movement speed attribute. */
    public static final double SPEED = 0.20;
    /** Follow range in blocks. */
    public static final double FOLLOW = 50.0;

    /**
     * @param world the world
     */
    public ZombieMiner(World world) {
        super(world);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        applyStats(HEALTH, ATTACK, SPEED, FOLLOW);
    }

    @Override
    protected NpcProfile npcProfile() {
        return NpcProfiles.get(PROFILE);
    }
}
