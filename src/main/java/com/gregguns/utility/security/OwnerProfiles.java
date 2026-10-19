package com.gregguns.utility.security;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.mojang.authlib.GameProfile;

/**
 * Turns a stored owner into a real player for the places that must never see a null one: the break event of an
 * explosive charge (MUTL-30, GGS-09-10), and the ServerUtilities-aware checks. The owner is the online player when
 * there is one, otherwise a Forge {@code FakePlayer} carrying the stored profile, which ServerUtilities rates by the
 * owner's own team. Server side.
 */
// Spec: docs/gregguns-spec/09-machines-utility/explosive-charges.md (GGS-09-10), docs/techguns-spec/BUGS.md (MUTL-30)
public final class OwnerProfiles {

    private OwnerProfiles() {}

    /**
     * @param world   the world the player acts in
     * @param ownerId the stored owner UUID; may be null
     * @param profile the stored owner profile; may be null
     * @return the online owner, else a fake player for the profile, else a fake player for the UUID with an empty name;
     *         never null, even for an ownerless block
     */
    public static EntityPlayerMP playerFor(WorldServer world, UUID ownerId, GameProfile profile) {
        EntityPlayerMP online = online(ownerId);
        if (online != null) {
            return online;
        }
        GameProfile use = profile;
        if (use == null || use.getId() == null) {
            // Deviation: an ownerless block still gets a real (anonymous) player so no event carries null.
            use = new GameProfile(ownerId != null ? ownerId : UUID.nameUUIDFromBytes(new byte[0]), "[GregGuns]");
        }
        // Source: Forge 1.7.10 net.minecraftforge.common.util.FakePlayerFactory#get(WorldServer, GameProfile)
        return FakePlayerFactory.get(world, use);
    }

    /**
     * @param ownerId the owner UUID; may be null
     * @return the owner when logged in, else null
     */
    public static EntityPlayerMP online(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return null;
        }
        for (Object o : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP p = (EntityPlayerMP) o;
            if (ownerId.equals(p.getUniqueID())) {
                return p;
            }
        }
        return null;
    }

    /**
     * The one check every owner-gated action uses (MUTL-31): is this player the owner, or a ServerUtilities team
     * member of the owner?
     *
     * @param owner  the stored owner UUID; null refuses
     * @param player the acting player; null refuses
     * @return true when authorized
     */
    public static boolean isAuthorized(UUID owner, EntityPlayer player) {
        return player != null && OwnerAccess.isAuthorized(owner, player.getUniqueID(), FriendProviders.current());
    }
}
