package com.gregguns;

import java.util.Map;

/**
 * The decision behind GregGuns' {@code @NetworkCheckHandler}: may a remote side that reports these mod versions
 * connect? Plain Java, no Minecraft types, so it is unit-tested.
 *
 * <p>
 * Released jars keep FML's default strictness: the remote must have GregGuns at exactly our version. In the dev
 * environment any remote GregGuns version is accepted, because the buildscript bakes the git state (describe, branch,
 * "-dirty") into the version string, so a server and a client built from different worktrees never match.
 */
public final class RemoteVersionCheck {

    private RemoteVersionCheck() {}

    /**
     * @param dev        true in the development workspace ({@code DevEnvironment.isActive()})
     * @param remoteMods the remote side's mod ids mapped to versions, as FML hands them over
     * @param ownVersion this side's GregGuns version
     * @return false if the remote lacks GregGuns; true in dev for any remote version; otherwise true only on an exact
     *         version match
     */
    public static boolean accepts(boolean dev, Map<String, String> remoteMods, String ownVersion) {
        if (remoteMods == null || !remoteMods.containsKey(GregGuns.MODID)) {
            return false;
        }
        return dev || ownVersion.equals(remoteMods.get(GregGuns.MODID));
    }
}
