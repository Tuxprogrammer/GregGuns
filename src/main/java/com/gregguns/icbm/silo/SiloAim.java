package com.gregguns.icbm.silo;

import net.minecraft.nbt.NBTTagCompound;

import com.gregguns.icbm.control.ControlStatus;
import com.gregguns.icbm.control.LaunchGate;
import com.gregguns.icbm.control.LaunchTarget;
import com.gregguns.icbm.missile.LaunchSite;
import com.gregguns.icbm.missile.MissileStatus;

/**
 * The target a computer sets on a Missile Silo and the pre-check it runs, for the {@code gg_silo} component. A silo has
 * no target of its own for players (the Launch Control aims it), so this is the computer's own aim, kept in the
 * silo's NBT. The check is {@link LaunchGate}, the one the Launch Control runs, with the two inputs a silo does not
 * have fixed: it draws no shot of EU (its hatches power the cycle, and the hatch tier is the site's {@code hold()}) and
 * has no cooldown. Server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_silo), 03-missiles/missile-silo.md (The launch site API)
final class SiloAim {

    private static final String K_SET = "aimSet";
    private static final String K_X = "aimX";
    private static final String K_Z = "aimZ";

    private LaunchTarget target = LaunchTarget.NONE;

    LaunchTarget target() {
        return target;
    }

    void set(LaunchTarget t) {
        target = t;
    }

    /**
     * @param formed whether the silo is formed
     * @param site   the silo as a launch site
     * @return the first problem, or READY; a countdown or ignition in progress is reported as such
     */
    ControlStatus status(boolean formed, LaunchSite site) {
        if (formed && site.countdownTicks() >= 0) {
            return ControlStatus.COUNTDOWN;
        }
        if (formed && site.ignited()) {
            return ControlStatus.IGNITED;
        }
        MissileStatus ms = formed ? site.status() : MissileStatus.NONE;
        int min = ms.present() ? ms.tier()
            .minRange() : 0;
        int max = ms.present() ? site.maxRange() : 0;
        double dist = target.distanceFrom(site.centerX(), site.centerZ());
        ControlStatus st = LaunchGate
            .check(target, formed, true, false, ms.present(), ms.armed(), ms.fueled(), dist, min, max);
        if (st == ControlStatus.READY && site.hold() != com.gregguns.icbm.missile.LaunchResult.OK) {
            st = LaunchGate.fromResult(
                site.hold()
                    .name());
        }
        return st;
    }

    /**
     * Launches through the site: the same check first, then the site's own rules.
     *
     * @param formed whether the silo is formed
     * @param site   the silo as a launch site
     * @return FIRED, or the status that refused
     */
    ControlStatus fire(boolean formed, LaunchSite site) {
        ControlStatus st = status(formed, site);
        if (st != ControlStatus.READY) {
            return st;
        }
        return LaunchGate.fromResult(
            site.launch(target.aimX(), target.aimZ(), null)
                .name());
    }

    void save(NBTTagCompound tag) {
        tag.setBoolean(K_SET, target.isSet());
        tag.setInteger(K_X, target.x());
        tag.setInteger(K_Z, target.z());
    }

    void load(NBTTagCompound tag) {
        target = tag.getBoolean(K_SET) ? LaunchTarget.of(tag.getInteger(K_X), tag.getInteger(K_Z)) : LaunchTarget.NONE;
    }
}
