package com.gregguns.icbm.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.icbm.detect.ContactSource;

/**
 * Builds the record tables {@code getContacts()} and {@code getIncoming()} return (GGI-01-06):
 * {@code {id, x, y, z, tier, incoming, impactX, impactZ, eta}}, with {@code incoming} decided by the predicted impact
 * point (D14) and {@code eta} the ticks until that point at the horizontal speed (-1 when the missile is not moving
 * sideways). No OpenComputers types, server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (getContacts)
public final class ContactRecords {

    private ContactRecords() {}

    /**
     * @param c       a contact
     * @param centerX the protected center X
     * @param centerZ the protected center Z
     * @param radius  the protected radius
     * @return the record
     */
    public static Map<String, Object> record(ContactSource c, double centerX, double centerZ, double radius) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.id());
        m.put("x", c.x());
        m.put("y", c.y());
        m.put("z", c.z());
        m.put("tier", c.tier());
        m.put("incoming", c.incoming(centerX, centerZ, radius));
        m.put("impactX", c.impactX());
        m.put("impactZ", c.impactZ());
        m.put("eta", eta(c));
        return m;
    }

    /**
     * @param contacts     what the radar sees
     * @param centerX      the protected center X
     * @param centerZ      the protected center Z
     * @param radius       the protected radius
     * @param onlyIncoming true for {@code getIncoming()}
     * @return one record per contact (or per incoming contact), in the given order
     */
    public static List<Map<String, Object>> records(List<? extends ContactSource> contacts, double centerX,
        double centerZ, double radius, boolean onlyIncoming) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ContactSource c : contacts) {
            if (!onlyIncoming || c.incoming(centerX, centerZ, radius)) {
                out.add(record(c, centerX, centerZ, radius));
            }
        }
        return out;
    }

    /**
     * @param c a contact
     * @return ticks to the predicted impact point at the horizontal speed, rounded up; -1 when it is not moving
     *         sideways
     */
    public static int eta(ContactSource c) {
        double speed = Math.sqrt(c.vx() * c.vx() + c.vz() * c.vz());
        if (speed < 1e-6) {
            return -1;
        }
        double dx = c.impactX() - c.x();
        double dz = c.impactZ() - c.z();
        return (int) Math.ceil(Math.sqrt(dx * dx + dz * dz) / speed);
    }
}
