package com.gregguns.utility.security;

import java.util.UUID;

/**
 * The pure ownership rules of the Turret, Rampart and charges. No Minecraft types, no state, both sides.
 *
 * <p>
 * Friend means the owner, or whoever the {@link FriendProvider} names. Without ServerUtilities the provider is
 * {@link FriendProvider#NONE}, so only the owner is a friend and level 1 behaves like level 2 (GGS-09-04). Fixes
 * NPC-13:
 * the Techguns dummy provider said everyone was a friend, so players were never targeted.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md (GGS-09-03, GGS-09-04, GGS-09-05),
// docs/techguns-spec/BUGS.md (NPC-13, MUTL-31)
public final class OwnerAccess {

    private OwnerAccess() {}

    /**
     * @param owner    the owner's UUID; null when the block has no owner
     * @param other    the player's UUID; null never counts
     * @param provider the friend source; null is treated as {@link FriendProvider#NONE}
     * @return true when {@code other} is the owner or a friend of the owner; false for a null UUID, an ownerless
     *         block or a provider that throws
     */
    public static boolean isFriend(UUID owner, UUID other, FriendProvider provider) {
        if (owner == null || other == null) {
            return false;
        }
        if (owner.equals(other)) {
            return true;
        }
        if (provider == null) {
            return false;
        }
        try {
            return provider.isFriend(owner, other);
        } catch (RuntimeException | LinkageError e) {
            // Deviation: a broken optional mod must never throw into a tick (GGS-09-05); the safe answer is no.
            return false;
        }
    }

    /**
     * Whether a player may open the GUI and change the settings of an owned block.
     *
     * @param level    the block's security level
     * @param owner    the owner's UUID; null means ownerless, which anyone may use
     * @param player   the acting player's UUID; null is refused
     * @param provider the friend source
     * @return true when allowed: PUBLIC lets anyone use it, FRIENDS the owner and friends, OWNER the owner only
     */
    public static boolean mayUse(SecurityLevel level, UUID owner, UUID player, FriendProvider provider) {
        if (player == null) {
            return false;
        }
        if (owner == null || level == SecurityLevel.PUBLIC) {
            return true;
        }
        if (level == SecurityLevel.OWNER) {
            return owner.equals(player);
        }
        return isFriend(owner, player, provider);
    }

    /**
     * Whether a player counts as the owner's side for a change such as an explosive charge setting (MUTL-31): the
     * owner, or a team member the provider names. An ownerless block refuses, so a charge nobody placed cannot be
     * configured by a stranger.
     *
     * @param owner    the owner's UUID; null refuses
     * @param player   the acting player's UUID; null refuses
     * @param provider the friend source
     * @return true when the player is the owner or a friend
     */
    public static boolean isAuthorized(UUID owner, UUID player, FriendProvider provider) {
        return isFriend(owner, player, provider);
    }

    /**
     * Whether a Turret with this level shoots a player. Level 0 never does; otherwise every player who is not the
     * owner or a friend is a target.
     *
     * @param level    the base's security level
     * @param owner    the owner's UUID; null means no owner and no player is a target
     * @param player   the candidate's UUID
     * @param provider the friend source
     * @return true when the player is a valid target
     */
    public static boolean isPlayerTarget(SecurityLevel level, UUID owner, UUID player, FriendProvider provider) {
        if (level == SecurityLevel.PUBLIC || owner == null || player == null) {
            return false;
        }
        return !isFriend(owner, player, provider);
    }
}
