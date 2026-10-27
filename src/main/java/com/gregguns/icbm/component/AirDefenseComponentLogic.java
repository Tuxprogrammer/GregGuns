package com.gregguns.icbm.component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The calls of the {@code gg_air_defense} component (golden-dome.md) over an {@link AirDefenseView}. Server thread
 * only.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_air_defense)
public final class AirDefenseComponentLogic {

    /** The component name. */
    public static final String COMPONENT = "gg_air_defense";

    private final AirDefenseView battery;

    /** @param battery the battery the component stands for */
    public AirDefenseComponentLogic(AirDefenseView battery) {
        this.battery = battery;
    }

    /** @return whether a radar is linked, the SAM count and capacity, the auto flag and the shot counters */
    public Map<String, Object> getInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("linked", battery.linked());
        m.put("samCount", battery.samCount());
        m.put("capacity", battery.capacity());
        m.put("auto", battery.auto());
        m.put("fired", battery.fired());
        m.put("hits", battery.hits());
        return m;
    }

    /** @return the SAMs loaded */
    public int getSamCount() {
        return battery.samCount();
    }

    /**
     * @param contactId a contact id from the linked radar
     * @return ok when a SAM went, else the battery's reason
     */
    public Reply fireAt(int contactId) {
        return battery.fireAt(contactId);
    }

    /**
     * @param on true to let the battery engage by itself
     * @return the flag now in effect
     */
    public boolean setAuto(boolean on) {
        battery.setAuto(on);
        return battery.auto();
    }
}
