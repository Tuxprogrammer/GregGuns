package com.gregguns.utility.turret;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.gregguns.utility.security.FriendProvider;
import com.gregguns.utility.security.SecurityLevel;
import com.gregguns.utility.turret.TurretTargetSelector.Candidate;
import com.gregguns.utility.turret.TurretTargetSelector.Settings;

class TurretTargetSelectorTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID ALLY = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID STRANGER = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    private static final UUID OTHER_OWNER = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    private static final FriendProvider ALLY_OF_OWNER = (o, other) -> o.equals(OWNER) && other.equals(ALLY);

    /** A configurable stub. */
    private static final class Stub implements Candidate {

        boolean mob;
        boolean turret;
        UUID turretOwner;
        boolean player;
        UUID playerId;
        boolean animal;
        boolean tamed;

        static Stub mob() {
            Stub s = new Stub();
            s.mob = true;
            return s;
        }

        static Stub player(UUID id) {
            Stub s = new Stub();
            s.player = true;
            s.playerId = id;
            return s;
        }

        static Stub turret(UUID owner) {
            Stub s = new Stub();
            s.turret = true;
            s.turretOwner = owner;
            return s;
        }

        static Stub animal(boolean tamed) {
            Stub s = new Stub();
            s.animal = true;
            s.tamed = tamed;
            return s;
        }

        @Override
        public boolean isHostileMob() {
            return mob;
        }

        @Override
        public boolean isTurret() {
            return turret;
        }

        @Override
        public UUID turretOwner() {
            return turretOwner;
        }

        @Override
        public boolean isShootablePlayer() {
            return player;
        }

        @Override
        public UUID playerId() {
            return playerId;
        }

        @Override
        public boolean isPassiveAnimal() {
            return animal;
        }

        @Override
        public boolean isTamedOrOwned() {
            return tamed;
        }
    }

    private static Settings settings(SecurityLevel level, boolean animals, FriendProvider friends) {
        return new Settings(level, OWNER, animals, friends);
    }

    @Test
    void anyHostileMobIsAlwaysATarget() {
        for (SecurityLevel level : SecurityLevel.values()) {
            assertTrue(TurretTargetSelector.isTarget(settings(level, false, null), Stub.mob()));
        }
        assertTrue(TurretTargetSelector.isTarget(null, Stub.mob()), "an unlinked turret still shoots mobs");
    }

    @Test
    void anUnlinkedTurretShootsMobsOnly() {
        assertFalse(TurretTargetSelector.isTarget(null, Stub.player(STRANGER)));
        assertFalse(TurretTargetSelector.isTarget(null, Stub.animal(false)));
        assertFalse(TurretTargetSelector.isTarget(null, Stub.turret(OTHER_OWNER)));
    }

    @Test
    void nullCandidateIsNeverATarget() {
        assertFalse(TurretTargetSelector.isTarget(settings(SecurityLevel.OWNER, true, null), null));
    }

    @Test
    void atLevelZeroNoPlayerAndNoTurretIsTargeted() {
        Settings s = settings(SecurityLevel.PUBLIC, false, ALLY_OF_OWNER);
        assertFalse(TurretTargetSelector.isTarget(s, Stub.player(STRANGER)));
        assertFalse(TurretTargetSelector.isTarget(s, Stub.player(OWNER)));
        assertFalse(TurretTargetSelector.isTarget(s, Stub.turret(OTHER_OWNER)));
    }

    @Test
    void theOwnerIsNeverTargetedAtLevelsOneAndTwo() {
        for (SecurityLevel level : new SecurityLevel[] { SecurityLevel.FRIENDS, SecurityLevel.OWNER }) {
            assertFalse(TurretTargetSelector.isTarget(settings(level, true, null), Stub.player(OWNER)));
            assertFalse(TurretTargetSelector.isTarget(settings(level, true, ALLY_OF_OWNER), Stub.player(OWNER)));
        }
    }

    @Test
    void aFriendIsSparedAndAStrangerShotAtLevelsOneAndTwo() {
        for (SecurityLevel level : new SecurityLevel[] { SecurityLevel.FRIENDS, SecurityLevel.OWNER }) {
            Settings s = settings(level, false, ALLY_OF_OWNER);
            assertFalse(TurretTargetSelector.isTarget(s, Stub.player(ALLY)));
            assertTrue(TurretTargetSelector.isTarget(s, Stub.player(STRANGER)));
        }
    }

    @Test
    void doesNotReproduceNPC13_withoutAFriendProviderStrangersAreTargeted() {
        // The Techguns dummy provider made everyone a friend, so its turrets never shot a player.
        Settings s = settings(SecurityLevel.OWNER, false, FriendProvider.NONE);
        assertTrue(TurretTargetSelector.isTarget(s, Stub.player(STRANGER)));
        assertTrue(
            TurretTargetSelector.isTarget(s, Stub.player(ALLY)),
            "no ServerUtilities: only the owner is a friend");
        assertFalse(TurretTargetSelector.isTarget(s, Stub.player(OWNER)));
        assertTrue(TurretTargetSelector.isTarget(settings(SecurityLevel.OWNER, false, null), Stub.player(STRANGER)));
    }

    @Test
    void aThrowingFriendProviderCountsAsNotFriends() {
        FriendProvider broken = (o, other) -> { throw new IllegalStateException("no universe"); };
        assertTrue(TurretTargetSelector.isTarget(settings(SecurityLevel.OWNER, false, broken), Stub.player(STRANGER)));
    }

    @Test
    void otherTurretsFollowTheOwnersFriendship() {
        Settings s = settings(SecurityLevel.OWNER, true, ALLY_OF_OWNER);
        assertFalse(TurretTargetSelector.isTarget(s, Stub.turret(OWNER)), "own turret");
        assertFalse(TurretTargetSelector.isTarget(s, Stub.turret(ALLY)), "an ally's turret");
        assertTrue(TurretTargetSelector.isTarget(s, Stub.turret(OTHER_OWNER)), "a stranger's turret");
    }

    @Test
    void aTurretWithNoOwnerIsNeverTargetedEvenWithAnimalsOn() {
        Settings s = settings(SecurityLevel.OWNER, true, null);
        Stub ownerless = Stub.turret(null);
        ownerless.animal = true;
        assertFalse(TurretTargetSelector.isTarget(s, ownerless));
    }

    @Test
    void animalsAreShotOnlyWithTheToggleAndNeverWhenTamedOrOwned() {
        assertFalse(TurretTargetSelector.isTarget(settings(SecurityLevel.OWNER, false, null), Stub.animal(false)));
        assertTrue(TurretTargetSelector.isTarget(settings(SecurityLevel.OWNER, true, null), Stub.animal(false)));
        assertFalse(TurretTargetSelector.isTarget(settings(SecurityLevel.OWNER, true, null), Stub.animal(true)));
        assertTrue(TurretTargetSelector.isTarget(settings(SecurityLevel.PUBLIC, true, null), Stub.animal(false)));
    }

    @Test
    void anOwnerlessBaseShootsNoPlayerButStillShootsAnimalsWhenAsked() {
        Settings s = new Settings(SecurityLevel.OWNER, null, true, null);
        assertFalse(TurretTargetSelector.isTarget(s, Stub.player(STRANGER)));
        assertTrue(TurretTargetSelector.isTarget(s, Stub.animal(false)));
    }

    @Test
    void anUnknownThingIsNotATarget() {
        assertFalse(TurretTargetSelector.isTarget(settings(SecurityLevel.OWNER, true, null), new Stub()));
    }
}
