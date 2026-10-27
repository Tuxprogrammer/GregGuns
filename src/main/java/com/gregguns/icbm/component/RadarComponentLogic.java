package com.gregguns.icbm.component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The calls of the {@code gg_radar} component (golden-dome.md) over a {@link RadarView}. The records are built by
 * {@link ContactRecords}, so {@code incoming} follows the predicted impact point (GGI-01-06). Server thread only.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_radar, GGI-01-06)
public final class RadarComponentLogic {

    /** The component name. */
    public static final String COMPONENT = "gg_radar";

    private final RadarView radar;

    /** @param radar the radar the component stands for */
    public RadarComponentLogic(RadarView radar) {
        this.radar = radar;
    }

    /** @return range, protected radius, whether it is online and how many contacts it holds */
    public Map<String, Object> getInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("online", radar.online());
        m.put("range", radar.range());
        m.put("protectedRadius", radar.protectedRadius());
        m.put(
            "contacts",
            radar.contacts()
                .size());
        return m;
    }

    /** @return one record per missile the radar sees */
    public List<Map<String, Object>> getContacts() {
        return ContactRecords
            .records(radar.contacts(), radar.centerX(), radar.centerZ(), radar.protectedRadius(), false);
    }

    /** @return the records of the missiles whose predicted impact is inside the protected circle */
    public List<Map<String, Object>> getIncoming() {
        return ContactRecords
            .records(radar.contacts(), radar.centerX(), radar.centerZ(), radar.protectedRadius(), true);
    }

    /**
     * @param blocks the wanted radius
     * @return the radius now in effect (clamped like the GUI)
     */
    public int setProtectedRadius(int blocks) {
        return radar.setProtectedRadius(blocks);
    }
}
