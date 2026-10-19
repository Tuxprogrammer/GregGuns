package com.gregguns.utility.security;

import com.gregguns.GregGuns;

import cpw.mods.fml.common.Loader;

/**
 * Picks the friend source once: ServerUtilities when it is loaded, otherwise {@link FriendProvider#NONE}. The adapter
 * class is touched only inside the guarded branch, so a pack without ServerUtilities never loads it (GGS-09-06).
 * Server side.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md (GGS-09-04, GGS-09-06)
public final class FriendProviders {

    private static FriendProvider current;

    private FriendProviders() {}

    /** @return the friend provider for this run; never null */
    public static synchronized FriendProvider current() {
        if (current == null) {
            current = create();
        }
        return current;
    }

    private static FriendProvider create() {
        if (Loader.isModLoaded("serverutilities")) {
            try {
                FriendProvider provider = new ServerUtilitiesFriends();
                GregGuns.LOG.info("ServerUtilities found: team ALLY or higher counts as a friend");
                return provider;
            } catch (LinkageError e) {
                GregGuns.LOG
                    .warn("ServerUtilities is loaded but its team API is not usable; only owners are friends", e);
            }
        }
        return FriendProvider.NONE;
    }
}
