package com.gregguns.core;

import net.minecraft.launchwrapper.Launch;

/**
 * Tells dev-only features (the {@code /gregguns test} command, the postInit self-check) whether they may run.
 *
 * <p>
 * The dev environment is the deobfuscated workspace that {@code runClient} and {@code runServer} launch. Released jars
 * run obfuscated inside the pack, so dev-only features never activate there. Both sides.
 */
public final class DevEnvironment {

    /** Launch blackboard key that FML sets to {@code true} when running in a deobfuscated workspace. */
    private static final String DEOBFUSCATED_KEY = "fml.deobfuscatedEnvironment";

    private DevEnvironment() {}

    /**
     * @return {@code true} when running in the deobfuscated development workspace, {@code false} in a released pack or
     *         when the launch blackboard is unavailable (for example in unit tests)
     */
    public static boolean isActive() {
        if (Launch.blackboard == null) {
            return false;
        }
        return Boolean.TRUE.equals(Launch.blackboard.get(DEOBFUSCATED_KEY));
    }
}
