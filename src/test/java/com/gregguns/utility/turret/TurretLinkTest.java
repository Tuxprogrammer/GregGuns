package com.gregguns.utility.turret;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import com.gregguns.utility.security.SecurityLevel;
import com.gregguns.utility.turret.entity.TurretEntity;

class TurretLinkTest {

    /** A stand-in for a nearby turret entity. */
    private static final class Fake {

        final UUID id;
        boolean dead;

        Fake(UUID id) {
            this.id = id;
        }
    }

    private static Fake find(UUID id, Iterable<Fake> nearby) {
        return TurretLink.find(id, nearby, f -> f.id, f -> !f.dead);
    }

    @Test
    void theLinkIsTheEntityUuid() {
        Fake a = new Fake(UUID.randomUUID());
        Fake b = new Fake(UUID.randomUUID());
        assertSame(b, find(b.id, Arrays.asList(a, b)));
    }

    @Test
    void doesNotReproduceMUTL25_aStaleOrWrongLinkMeansUnlinkedAndNeverThrows() {
        // Techguns cast the entity found by id to NPCTurret and threw when the id belonged to something else. Here the
        // candidates are already turrets, and a UUID that matches none (a zombie's, or a removed turret's) is "none".
        Fake a = new Fake(UUID.randomUUID());
        UUID zombie = UUID.randomUUID();
        assertDoesNotThrow(() -> find(zombie, Collections.singletonList(a)));
        assertNull(find(zombie, Collections.singletonList(a)));
        assertNull(find(null, Collections.singletonList(a)));
        assertNull(find(a.id, null));
        assertNull(find(a.id, Arrays.asList((Fake) null)));
    }

    @Test
    void aDeadTurretIsNotALink() {
        Fake dead = new Fake(UUID.randomUUID());
        dead.dead = true;
        assertNull(find(dead.id, Collections.singletonList(dead)));
    }

    @Test
    void doesNotReproduceNPC12_aTurretWithNoBaseDiesQuietly() {
        // Techguns dereferenced the missing base in onDeath.
        assertDoesNotThrow(() -> TurretLink.notifyDeath(null));
        assertFalse(TurretLink.notifyDeath(null));
    }

    @Test
    void aLinkedHostIsToldOnce() {
        AtomicInteger told = new AtomicInteger();
        TurretHost host = new TurretHost() {

            @Override
            public UUID ownerId() {
                return null;
            }

            @Override
            public SecurityLevel security() {
                return SecurityLevel.PUBLIC;
            }

            @Override
            public boolean targetsAnimals() {
                return false;
            }

            @Override
            public ItemStack mountedGun() {
                return null;
            }

            @Override
            public boolean consumeAmmo() {
                return false;
            }

            @Override
            public int armorValue() {
                return 0;
            }

            @Override
            public boolean isHostValid() {
                return true;
            }

            @Override
            public boolean isTurretActive() {
                return false;
            }

            @Override
            public boolean adopt(TurretEntity turret) {
                return false;
            }

            @Override
            public void onTurretDeath() {
                told.incrementAndGet();
            }
        };
        assertTrue(TurretLink.notifyDeath(host));
        assertEquals(1, told.get());
    }
}
