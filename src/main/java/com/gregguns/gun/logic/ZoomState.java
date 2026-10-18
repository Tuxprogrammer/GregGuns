package com.gregguns.gun.logic;

/**
 * Whether a player is zoomed and by how much. Toggle zoom only (every Techguns gun uses toggle mode).
 *
 * <h2>Driving it</h2>
 * The <b>server</b> holds the authoritative instance, inside {@link GunController}. The right-click sends a plain
 * "toggle zoom" intent; the server calls {@link #toggle} and tells the client the result. The server then derives
 * spread from its own state and ignores any zoom flag a client might send (GUN-12). The client keeps a mirror for
 * the FOV and applies {@link #fovMultiplier} smoothly itself.
 *
 * <p>
 * Deviation: Techguns keeps zoom when switching to another gun with the same factor; GregGuns always resets on a gun
 * switch (see {@link #reset}). Spec: gun-lifecycle.md, "Zoom state machine".
 */
public final class ZoomState {

    private boolean zoomed;

    /**
     * Toggles zoom for the held gun. Does nothing and returns false for a gun without a scope (GUN-47: an unscoped
     * gun is never "zooming").
     *
     * @return true if the zoom state changed
     */
    public boolean toggle(GunStats stats) {
        if (!stats.hasScope()) {
            zoomed = false;
            return false;
        }
        zoomed = !zoomed;
        return true;
    }

    /** Clears zoom: on reload start, weapon switch, dropping the gun, death. */
    public void reset() {
        zoomed = false;
    }

    /** True if zoomed with a gun that has a scope. */
    public boolean isZoomed(GunStats stats) {
        return zoomed && stats.hasScope();
    }

    /** The FOV multiplier to apply: the gun's zoom factor while zoomed, else 1. */
    public double fovMultiplier(GunStats stats) {
        return isZoomed(stats) ? stats.zoomFactor() : 1.0;
    }
}
