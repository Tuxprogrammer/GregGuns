package com.gregguns.icbm.component;

import java.util.LinkedHashMap;
import java.util.Map;

import com.gregguns.icbm.control.ControlStatus;
import com.gregguns.icbm.control.LaunchTarget;
import com.gregguns.icbm.control.TargetStick;
import com.gregguns.icbm.missile.MissileStatus;

/**
 * The calls of the {@code gg_silo} and {@code gg_launch_control} components (golden-dome.md), as plain Java over a
 * {@link LaunchView}. The OpenComputers class only unpacks arguments. {@link #launch()} goes through the machine's own
 * fire path, the same gate as the button and redstone, so a computer can never bypass a check a player cannot
 * (GGI-01-04): the reply is {@code true} only when the missile went, otherwise {@code false} and the status word the
 * GUI shows (the lowercase {@link ControlStatus} name). Server thread only (GGI-01-03).
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_silo, GGI-01-02 to 05)
public final class LaunchComponentLogic {

    /** The component name of the Missile Silo. */
    public static final String SILO = "gg_silo";
    /** The component name of the Launch Control. */
    public static final String LAUNCH_CONTROL = "gg_launch_control";

    private final LaunchView machine;

    /** @param machine the machine the component stands for */
    public LaunchComponentLogic(LaunchView machine) {
        this.machine = machine;
    }

    /** @return the machine's constants plus the target and the status word */
    public Map<String, Object> getInfo() {
        Map<String, Object> m = new LinkedHashMap<>(machine.launchInfo());
        m.put("status", word(machine.launchStatus()));
        m.put("launches", machine.launches());
        return m;
    }

    /** @return a record of the missile in place, or null (nil) when there is none */
    public Map<String, Object> getMissile() {
        MissileStatus s = machine.missileStatus();
        if (!s.present()) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(
            "tier",
            s.tier()
                .number());
        m.put("armed", s.armed());
        m.put("fueled", s.fueled());
        m.put("guidance", s.guidance());
        m.put("explosive", s.explosive() == null ? "" : s.explosive());
        m.put("fill", s.fill());
        return m;
    }

    /** @return {@code {fluid, amount, required}}: the fuel in the tank and the amount that fills it, mB */
    public Map<String, Object> getFuel() {
        MissileStatus s = machine.missileStatus();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fluid", machine.fuelFluid());
        m.put("amount", s.present() ? s.fuelMb() : 0);
        m.put("required", s.present() ? s.capacityMb() : 0);
        return m;
    }

    /** @return {@code {x, z}} of the stored target, or null (nil) while it is unset */
    public Map<String, Object> getTarget() {
        LaunchTarget t = machine.launchTarget();
        if (!t.isSet()) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", t.x());
        m.put("z", t.z());
        return m;
    }

    /**
     * @param x block X
     * @param z block Z; a missile aims at a column, so a height given to the call is dropped before this
     * @return ok, or {@code bad_target} for a coordinate past the world border
     */
    public Reply setTarget(double x, double z) {
        if (Double.isNaN(x) || Double.isNaN(z)
            || Math.abs(x) > LaunchTarget.LIMIT
            || Math.abs(z) > LaunchTarget.LIMIT) {
            return Reply.refused("bad_target");
        }
        machine.aimAt((int) Math.floor(x), (int) Math.floor(z));
        return Reply.ok();
    }

    /** @return ok, or the stick problem as a lowercase word ({@code no_stick}, {@code empty_stick}, ...) */
    public Reply setTargetFromStick() {
        TargetStick.Outcome o = machine.loadTargetFromStick();
        return o == TargetStick.Outcome.LOADED ? Reply.ok() : Reply.refused(word(o));
    }

    /** @return ok when a launch would go now, else the first problem the GUI would show */
    public Reply canLaunch() {
        ControlStatus s = machine.launchStatus();
        return s == ControlStatus.READY ? Reply.ok() : Reply.refused(word(s));
    }

    /** @return ok when the missile went, else the reason it was refused */
    public Reply launch() {
        ControlStatus s = machine.launchNow();
        return s == ControlStatus.FIRED ? Reply.ok() : Reply.refused(word(s));
    }

    private static String word(Enum<?> e) {
        return e.name()
            .toLowerCase(java.util.Locale.ROOT);
    }
}
