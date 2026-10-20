package com.gregguns.entity.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Every catalog mob has an entity class and a spawn-egg subtype. SIMPLE_TURRET is exempt on purpose: the hostile camp
 * turret is placed by the turret base only and has no egg. OUTCAST and STORMTROOPER are implemented (M7 B5), so
 * {@link NpcRegistry#NOT_YET_IMPLEMENTED} is empty. The live half (registration, creative listing) is the
 * scenario {@code npc.all_eggs}.
 */
class NpcEggCoverageTest {

    @Test
    void everyMobHasAClassAndAnEggExceptTheExemptions() {
        NpcRegistry.declareMobs();
        assertEquals(
            java.util.Collections.emptyList(),
            NpcRegistry.eggAudit(false),
            "mobs missing a class or an egg subtype");
    }

    @Test
    void theExemptionAndGapListsAreExactlyWhatIsRecorded() {
        assertEquals(java.util.Collections.singleton(NpcMob.SIMPLE_TURRET), NpcRegistry.EGG_EXEMPT);
        assertTrue(NpcRegistry.NOT_YET_IMPLEMENTED.isEmpty());
        NpcRegistry.declareMobs();
        assertEquals(com.gregguns.entity.npc.mob.Outcast.class, NpcRegistry.boundClass(NpcMob.OUTCAST));
        assertEquals(com.gregguns.entity.npc.mob.StormTrooper.class, NpcRegistry.boundClass(NpcMob.STORMTROOPER));
        assertTrue(!NpcMob.SIMPLE_TURRET.hasEgg());
    }
}
