package com.gregguns.utility.security;

import java.util.UUID;

/**
 * Answers whether one player counts as a friend of an owner. The ServerUtilities adapter is the real one; tests use
 * stubs. Implementations may throw: {@link OwnerAccess} treats a throw as "not a friend" and never lets it escape.
 * Pure interface, no Minecraft types.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md (GGS-09-03, GGS-09-05)
public interface FriendProvider {

    /** The provider used without ServerUtilities: nobody but the owner is a friend (GGS-09-04). */
    FriendProvider NONE = (owner, other) -> false;

    /**
     * @param owner the owner's UUID, not null
     * @param other the other player's UUID, not null and not equal to the owner
     * @return true when {@code other} is the owner's friend (team status ALLY or higher)
     */
    boolean isFriend(UUID owner, UUID other);
}
