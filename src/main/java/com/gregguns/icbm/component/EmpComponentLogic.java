package com.gregguns.icbm.component;

import java.util.LinkedHashMap;
import java.util.Map;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.icbm.emp.EmpMode;
import com.gregguns.icbm.emp.EmpRules;

/**
 * The calls of the {@code gg_emp_tower} component (golden-dome.md), as plain Java over an {@link EmpTowerView}. The
 * OpenComputers class does nothing but unpack arguments and call these, so every rule here is unit tested without
 * OpenComputers. {@link #fire()} goes through the tower's own {@code fire()}, the same gate as the button and redstone,
 * so a computer can never bypass a check a player cannot (GGI-01-04). Server thread only (GGI-01-03).
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_emp_tower, GGI-01-02 to 05)
public final class EmpComponentLogic {

    /** The component's name on a computer. */
    public static final String COMPONENT = "gg_emp_tower";

    private final EmpTowerView tower;

    /** @param tower the tower the component stands for */
    public EmpComponentLogic(EmpTowerView tower) {
        this.tower = tower;
    }

    /** @return config-derived constants: radius limits, EU per area, cooldown and device cap */
    public Map<String, Object> info() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("minRadius", EmpRules.MIN_RADIUS);
        m.put("maxRadius", IcbmConfig.empMaxRadius);
        m.put("euPerArea", EmpRules.EU_PER_AREA);
        m.put("cooldownTicks", EmpRules.COOLDOWN_TICKS);
        m.put("maxDevices", IcbmConfig.empMaxDevices);
        return m;
    }

    /** @return true when the tower may fire right now */
    public boolean isReady() {
        return tower.gate()
            .isGood();
    }

    /** @return ticks until the next pulse may go; 0 when the clock allows it */
    public int getCooldown() {
        return tower.cooldownLeft();
    }

    /** @return the mode name: {@code all}, {@code missiles} or {@code machines} */
    public String getMode() {
        return tower.mode()
            .id();
    }

    /**
     * @param name a mode name
     * @return ok, or a refusal for a name that is not a mode
     */
    public Reply setMode(String name) {
        EmpMode mode = EmpMode.byId(name);
        if (mode == null) {
            return Reply.refused("bad_mode");
        }
        tower.setMode(mode);
        return Reply.ok();
    }

    /** @return the pulse radius, blocks */
    public int getRadius() {
        return tower.radius();
    }

    /**
     * @param blocks the requested radius; clamped like the GUI field (ICBM-05-11)
     * @return the radius now in effect
     */
    public int setRadius(int blocks) {
        tower.setRadius(blocks);
        return tower.radius();
    }

    /** @return ok when the pulse went, otherwise the gate's reason */
    public Reply fire() {
        EmpRules.Gate gate = tower.fire();
        return gate.isGood() ? Reply.ok() : Reply.refused(reason(gate));
    }

    /**
     * @param gate a failing gate
     * @return its reason word
     */
    static String reason(EmpRules.Gate gate) {
        switch (gate) {
            case MODULE_OFF:
                return "module_off";
            case COOLING:
                return "cooling";
            case NO_POWER:
                return "no_power";
            default:
                return "ready";
        }
    }
}
