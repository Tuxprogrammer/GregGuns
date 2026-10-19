package com.gregguns.utility.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** GGS-09-03 to GGS-09-05, NPC-13 and MUTL-31: the pure ownership and friend rules. */
class OwnerAccessTest {

    private static final UUID OWNER = new UUID(1, 1);
    private static final UUID ALLY = new UUID(2, 2);
    private static final UUID STRANGER = new UUID(3, 3);

    /** A stub for ServerUtilities: ALLY is on the owner's team. */
    private static final FriendProvider TEAM = (owner, other) -> owner.equals(OWNER) && other.equals(ALLY);

    @Test
    void levelZeroLetsAnyoneUseItAndNeverTargetsPlayers() {
        assertTrue(OwnerAccess.mayUse(SecurityLevel.PUBLIC, OWNER, STRANGER, TEAM));
        assertFalse(OwnerAccess.isPlayerTarget(SecurityLevel.PUBLIC, OWNER, STRANGER, TEAM));
        assertFalse(OwnerAccess.isPlayerTarget(SecurityLevel.PUBLIC, OWNER, OWNER, TEAM));
    }

    @Test
    void levelOneAllowsOwnerAndFriendsOnly() {
        assertTrue(OwnerAccess.mayUse(SecurityLevel.FRIENDS, OWNER, OWNER, TEAM));
        assertTrue(OwnerAccess.mayUse(SecurityLevel.FRIENDS, OWNER, ALLY, TEAM));
        assertFalse(OwnerAccess.mayUse(SecurityLevel.FRIENDS, OWNER, STRANGER, TEAM));
    }

    @Test
    void levelTwoAllowsTheOwnerOnly() {
        assertTrue(OwnerAccess.mayUse(SecurityLevel.OWNER, OWNER, OWNER, TEAM));
        assertFalse(OwnerAccess.mayUse(SecurityLevel.OWNER, OWNER, ALLY, TEAM));
        assertFalse(OwnerAccess.mayUse(SecurityLevel.OWNER, OWNER, STRANGER, TEAM));
    }

    @Test
    void targetsAreStrangersAtLevelsOneAndTwoNeverTheOwnerOrAFriend() {
        for (SecurityLevel level : new SecurityLevel[] { SecurityLevel.FRIENDS, SecurityLevel.OWNER }) {
            assertFalse(OwnerAccess.isPlayerTarget(level, OWNER, OWNER, TEAM), level + " owner");
            assertFalse(OwnerAccess.isPlayerTarget(level, OWNER, ALLY, TEAM), level + " ally");
            assertTrue(OwnerAccess.isPlayerTarget(level, OWNER, STRANGER, TEAM), level + " stranger");
        }
    }

    @Test
    void withoutAProviderOnlyTheOwnerIsAFriendSoLevelOneActsLikeLevelTwo() {
        // GGS-09-04
        for (FriendProvider none : new FriendProvider[] { FriendProvider.NONE, null }) {
            assertTrue(OwnerAccess.isFriend(OWNER, OWNER, none));
            assertFalse(OwnerAccess.isFriend(OWNER, ALLY, none));
            for (UUID p : new UUID[] { OWNER, ALLY, STRANGER }) {
                assertEquals(
                    OwnerAccess.mayUse(SecurityLevel.OWNER, OWNER, p, none),
                    OwnerAccess.mayUse(SecurityLevel.FRIENDS, OWNER, p, none));
                assertEquals(
                    OwnerAccess.isPlayerTarget(SecurityLevel.OWNER, OWNER, p, none),
                    OwnerAccess.isPlayerTarget(SecurityLevel.FRIENDS, OWNER, p, none));
            }
        }
    }

    @Test
    void doesNotReproduceNPC13_strangersAreTargetedWhenNoFriendModIsPresent() {
        // Techguns' dummy provider said everyone was a friend, so a level 1 or 2 turret never shot players.
        assertTrue(OwnerAccess.isPlayerTarget(SecurityLevel.FRIENDS, OWNER, STRANGER, FriendProvider.NONE));
        assertTrue(OwnerAccess.isPlayerTarget(SecurityLevel.OWNER, OWNER, STRANGER, FriendProvider.NONE));
    }

    @Test
    void aProviderThatThrowsMeansNotAFriend() {
        // GGS-09-05: no missing universe or odd state may throw into the tick.
        FriendProvider boom = (owner, other) -> {
            throw new NullPointerException("ServerUtilities Universe == null!");
        };
        FriendProvider linkage = (owner, other) -> { throw new NoClassDefFoundError("serverutils/lib/data/Universe"); };
        assertFalse(OwnerAccess.isFriend(OWNER, STRANGER, boom));
        assertFalse(OwnerAccess.isFriend(OWNER, STRANGER, linkage));
        assertTrue(OwnerAccess.isFriend(OWNER, OWNER, boom), "the owner needs no provider");
        assertTrue(OwnerAccess.isPlayerTarget(SecurityLevel.FRIENDS, OWNER, STRANGER, boom));
        assertFalse(OwnerAccess.mayUse(SecurityLevel.FRIENDS, OWNER, STRANGER, boom));
    }

    @Test
    void nullUuidsNeverCountAsFriends() {
        assertFalse(OwnerAccess.isFriend(null, STRANGER, TEAM));
        assertFalse(OwnerAccess.isFriend(OWNER, null, TEAM));
        assertFalse(OwnerAccess.isFriend(null, null, TEAM));
        assertFalse(OwnerAccess.mayUse(SecurityLevel.PUBLIC, OWNER, null, TEAM));
        assertFalse(OwnerAccess.isPlayerTarget(SecurityLevel.OWNER, OWNER, null, TEAM));
        assertFalse(OwnerAccess.isAuthorized(OWNER, null, TEAM));
        assertFalse(OwnerAccess.isAuthorized(null, OWNER, TEAM));
    }

    @Test
    void anOwnerlessBlockTargetsNoPlayerAndAnyoneMayUseIt() {
        assertFalse(OwnerAccess.isPlayerTarget(SecurityLevel.OWNER, null, STRANGER, TEAM));
        assertTrue(OwnerAccess.mayUse(SecurityLevel.OWNER, null, STRANGER, TEAM));
    }

    @Test
    void doesNotReproduceMUTL31_aStrangerIsNotAuthorizedButAnAllyIs() {
        assertTrue(OwnerAccess.isAuthorized(OWNER, OWNER, TEAM));
        assertTrue(OwnerAccess.isAuthorized(OWNER, ALLY, TEAM));
        assertFalse(OwnerAccess.isAuthorized(OWNER, STRANGER, TEAM));
        assertFalse(OwnerAccess.isAuthorized(OWNER, ALLY, FriendProvider.NONE));
    }

    @Test
    void theProviderIsAskedOnlyForOtherPlayers() {
        Set<UUID> asked = new HashSet<>();
        FriendProvider spy = (owner, other) -> {
            asked.add(other);
            return false;
        };
        OwnerAccess.isFriend(OWNER, OWNER, spy);
        assertTrue(asked.isEmpty());
        OwnerAccess.isFriend(OWNER, STRANGER, spy);
        assertEquals(1, asked.size());
    }

    @Test
    void securityLevelIdsAreStableAndUnknownIdsFallBackToPublic() {
        assertEquals(0, SecurityLevel.PUBLIC.id());
        assertEquals(1, SecurityLevel.FRIENDS.id());
        assertEquals(2, SecurityLevel.OWNER.id());
        assertEquals(SecurityLevel.PUBLIC, SecurityLevel.fromId(7));
        assertEquals(SecurityLevel.PUBLIC, SecurityLevel.fromId(-1));
        assertEquals(SecurityLevel.FRIENDS, SecurityLevel.PUBLIC.next());
        assertEquals(SecurityLevel.OWNER, SecurityLevel.FRIENDS.next());
        assertEquals(SecurityLevel.PUBLIC, SecurityLevel.OWNER.next());
    }
}
