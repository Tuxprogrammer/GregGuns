package com.gregguns.gun.logic;

/** What one {@link GunController} call did, for the item glue to act on (spawn, sounds, messages). Immutable. */
public final class GunTickResult {

    private final ShotResult shot;
    private final ReloadStateMachine.Event reload;
    private final boolean outOfAmmo;

    GunTickResult(ShotResult shot, ReloadStateMachine.Event reload, boolean outOfAmmo) {
        this.shot = shot;
        this.reload = reload;
        this.outOfAmmo = outOfAmmo;
    }

    /** The fire outcome. When {@code wasFired()}, spawn the projectiles. */
    public ShotResult shot() {
        return shot;
    }

    /** The reload event of this call (started, completed, canceled, rejected, or none). */
    public ReloadStateMachine.Event reload() {
        return reload;
    }

    /**
     * True once per trigger pull when the gun is empty and the inventory has nothing to reload with: play the "out of
     * ammo" reaction (GUN-11: never every tick).
     */
    public boolean outOfAmmo() {
        return outOfAmmo;
    }
}
