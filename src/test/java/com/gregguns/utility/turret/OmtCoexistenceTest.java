package com.gregguns.utility.turret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;

import org.junit.jupiter.api.Test;

import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcTurret;
import com.gregguns.utility.turret.entity.TurretEntity;

/**
 * Design record 0020: which GregGuns entities an OpenModularTurrets (OMT) turret will shoot. OMT's filter is a class
 * check, so the answer is fixed by our entity hierarchy; this pins it so a refactor cannot change it silently.
 */
// Source:
// https://github.com/GTNewHorizons/OpenModularTurrets/blob/2.4.8/src/main/java/openmodularturrets/util/TurretHeadUtil.java#L108-L145
// (neutrals: EntityAnimal or EntityAmbientCreature; mobs: isCreatureType(EnumCreatureType.monster, false); players:
// EntityPlayerMP that is not the owner, trusted or creative.)
class OmtCoexistenceTest {

    /** What OMT's {@code getTarget} would call a mob or a neutral, by class alone. */
    private static boolean omtWouldShoot(Class<? extends EntityLivingBase> type) {
        return EntityAnimal.class.isAssignableFrom(type) || EntityAmbientCreature.class.isAssignableFrom(type)
            || EnumCreatureType.monster.getCreatureClass()
                .isAssignableFrom(type);
    }

    @Test
    void omtShootsGregGunsHostileNpcsBecauseTheyAreMobs() {
        // Intended: a base defended by OMT turrets is defended against our raiders too.
        assertTrue(omtWouldShoot(NpcEntity.class));
    }

    @Test
    void omtNeverShootsAPlayersGregGunsTurret() {
        assertFalse(omtWouldShoot(TurretEntity.class));
    }

    @Test
    void omtIgnoresTheHostileCampTurretBecauseItIsNotAMob() {
        // Known, accepted gap (0020): the camp turret shoots players but is invisible to OMT. It stays non-IMob so
        // Infernal Mobs leaves it alone (GGS-09-01).
        assertFalse(omtWouldShoot(NpcTurret.class));
    }

    @Test
    void theMonsterCreatureTypeIsStillImob() {
        // The assumption behind the checks above, in case a Forge update changes the monster class.
        assertEquals(net.minecraft.entity.monster.IMob.class, EnumCreatureType.monster.getCreatureClass());
    }
}
