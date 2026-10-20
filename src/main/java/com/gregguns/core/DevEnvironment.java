package com.gregguns.core;

import net.minecraft.launchwrapper.Launch;

/**
 * Tells dev-only features (the {@code /gregguns test} command, the postInit self-check) whether they may run.
 *
 * <p>
 * The dev environment is the deobfuscated workspace that {@code runClient} and {@code runServer} launch. Released jars
 * run obfuscated inside the pack, so dev-only features never activate there. The one exception is the explicit opt-in
 * {@link #TEST_HOOKS_PROPERTY}, see {@link #testHooksEnabled()}. Both sides.
 */
public final class DevEnvironment {

    /** Launch blackboard key that FML sets to {@code true} when running in a deobfuscated workspace. */
    private static final String DEOBFUSCATED_KEY = "fml.deobfuscatedEnvironment";

    /** The JVM property ({@code -Dgregguns.testHooks=true}) that turns the test hooks on in a released pack. */
    public static final String TEST_HOOKS_PROPERTY = "gregguns.testHooks";

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

    /**
     * Whether the test hooks may run: the {@code /gregguns test} and {@code build} commands, the scenario
     * registrations, the client autotest signals and the other debug-only registrations.
     *
     * <p>
     * Why it exists: the hooks used to need a deobfuscated workspace, so the real GTNH pack with a production jar could
     * never be tested. This is the explicit opt-in for testers. It is off by default, so a released pack never runs any
     * of it unless the player adds {@code -Dgregguns.testHooks=true} to the JVM arguments. It does not unlock the
     * hooks that need deobfuscated names (the self-check) or that change the item and entity registries (the probe
     * tool gun, the test NPC); those stay on {@link #isActive()}.
     *
     * @return {@code true} in the dev workspace, or when {@code -Dgregguns.testHooks=true} is set
     */
    public static boolean testHooksEnabled() {
        return isActive() || Boolean.getBoolean(TEST_HOOKS_PROPERTY);
    }
}
