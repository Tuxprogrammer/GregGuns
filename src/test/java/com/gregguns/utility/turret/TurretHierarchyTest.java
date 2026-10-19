package com.gregguns.utility.turret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;

import org.junit.jupiter.api.Test;

import com.gregguns.combat.TurretAttacker;
import com.gregguns.utility.turret.entity.TurretEntity;

/** GGS-09-01 and GGS-09-02: the class relationships that keep mob mods away from the turret. */
class TurretHierarchyTest {

    @Test
    void theTurretIsNotAMobAnAnimalOrAnAmbientCreature() {
        // Infernal Mobs picks IMob that is not IEntityOwnable; OpenModularTurrets shoots EntityAnimal and
        // EntityAmbientCreature. Staying out of all three keeps both away (GGS-09-01).
        assertFalse(IMob.class.isAssignableFrom(TurretEntity.class));
        assertFalse(EntityAnimal.class.isAssignableFrom(TurretEntity.class));
        assertFalse(EntityAmbientCreature.class.isAssignableFrom(TurretEntity.class));
    }

    @Test
    void theTurretIsOwnableACreatureAndATurretAttacker() {
        assertTrue(IEntityOwnable.class.isAssignableFrom(TurretEntity.class));
        assertTrue(EntityCreature.class.isAssignableFrom(TurretEntity.class));
        assertTrue(TurretAttacker.class.isAssignableFrom(TurretEntity.class));
    }

    @Test
    void theEntityIdIsSixty() {
        assertEquals(60, TurretEntity.ENTITY_ID);
    }
}
