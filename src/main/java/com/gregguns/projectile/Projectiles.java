package com.gregguns.projectile;

import com.gregguns.GregGuns;

/**
 * Holds the active {@link ProjectileLauncher}. The projectile framework installs its launcher during the guns module's
 * preInit; until then shots are dropped and logged, so a gun never crashes for want of a projectile. Both sides.
 */
public final class Projectiles {

    private static final ProjectileLauncher NONE = (world, shooter, stats, offset, damageScale) -> GregGuns.LOG
        .warn("Shot dropped: no projectile launcher installed");

    private static ProjectileLauncher launcher = NONE;

    private Projectiles() {}

    /** @return the installed launcher, never null */
    public static ProjectileLauncher launcher() {
        return launcher;
    }

    /**
     * Installs the launcher. Called once by the projectile framework during preInit.
     *
     * @param newLauncher the launcher to use for every shot
     */
    public static void install(ProjectileLauncher newLauncher) {
        if (newLauncher == null) {
            throw new IllegalArgumentException("launcher");
        }
        launcher = newLauncher;
    }
}
