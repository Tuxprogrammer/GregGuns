package com.gregguns.entity.npc;

import net.minecraft.world.World;

/**
 * The Heavy Super Mutant: {@link SuperMutantBasic} with the {@link SuperMutantTier#HEAVY} numbers. Camps and eggs only.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (14. Super Mutants), BUGS.md NPC-25
public class SuperMutantHeavy extends SuperMutantBasic {

    /**
     * @param world the world; the entity registry calls this when it loads a mutant
     */
    public SuperMutantHeavy(World world) {
        super(world);
        // Explicit per class (NPC-25): the hitbox of this tier, width included.
        setSize(tier().width(), tier().height());
    }

    @Override
    public SuperMutantTier tier() {
        return SuperMutantTier.HEAVY;
    }
}
