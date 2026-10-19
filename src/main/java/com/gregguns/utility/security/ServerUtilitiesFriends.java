package com.gregguns.utility.security;

import java.util.UUID;

import serverutils.lib.EnumTeamStatus;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;

/**
 * Friends from ServerUtilities teams: a player is a friend of the owner when the owner's team rates them ALLY or
 * higher. Every lookup is guarded against an unloaded universe and an unknown UUID.
 *
 * <p>
 * Server side. This class names ServerUtilities types, so it must be loaded only behind
 * {@code Loader.isModLoaded("serverutilities")}; {@link FriendProviders} is the only caller (GGS-09-06).
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md (GGS-09-03, GGS-09-05, GGS-09-06)
public final class ServerUtilitiesFriends implements FriendProvider {

    @Override
    public boolean isFriend(UUID owner, UUID other) {
        // Source (null when the universe is not loaded, unlike Universe.get which throws):
        // https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/Universe.java#L72-L84
        Universe universe = Universe.getNullable();
        if (universe == null) {
            return false;
        }
        // Source (null for an unknown UUID):
        // https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/Universe.java#L560-L568
        ForgePlayer ownerPlayer = universe.getPlayer(owner);
        ForgePlayer otherPlayer = universe.getPlayer(other);
        if (ownerPlayer == null || otherPlayer == null || ownerPlayer.team == null) {
            return false;
        }
        // Source (ENEMY -10, NONE 0, INVITED 10, ALLY 30, MEMBER 50, MOD 80, OWNER 100):
        // https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/EnumTeamStatus.java#L18-L24
        // https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/ForgeTeam.java#L323-L345
        EnumTeamStatus status = ownerPlayer.team.getHighestStatus(otherPlayer);
        return status != null && status.getStatus() >= EnumTeamStatus.ALLY.getStatus();
    }
}
