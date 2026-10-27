package com.gregguns.icbm.compat.oc;

import com.gregguns.icbm.component.ContactTracker;
import com.gregguns.icbm.component.RadarComponentLogic;
import com.gregguns.icbm.component.RadarView;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ManagedEnvironment;

/**
 * The {@code gg_radar} component for the single-block radars and the tower. Callbacks are indirect (server thread,
 * GGI-01-03). {@code radar_contact}, {@code radar_incoming} and {@code radar_lost} carry the contact id after the
 * component address and go out on changes only (GGI-01-05). Server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_radar)
final class RadarEnvironment extends ManagedEnvironment {

    private final RadarView radar;
    private final RadarComponentLogic logic;
    private final ContactTracker tracker = new ContactTracker();

    RadarEnvironment(RadarView radar) {
        this.radar = radar;
        this.logic = new RadarComponentLogic(radar);
        // Source:
        // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/Network.java#L183
        setNode(
            Network.newNode(this, Visibility.Network)
                .withComponent(RadarComponentLogic.COMPONENT)
                .create());
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        if (node() == null || node().network() == null) {
            return;
        }
        for (ContactTracker.Signal s : tracker
            .sample(radar.contacts(), radar.centerX(), radar.centerZ(), radar.protectedRadius())) {
            node().sendToReachable("computer.signal", s.name(), node().address(), s.id());
        }
    }

    @Callback(doc = "function():table -- online, range, protectedRadius and the contact count.")
    public Object[] getInfo(Context context, Arguments args) {
        return new Object[] { logic.getInfo() };
    }

    @Callback(doc = "function():table -- Every missile seen: {id, x, y, z, tier, incoming, impactX, impactZ, eta}.")
    public Object[] getContacts(Context context, Arguments args) {
        return new Object[] { logic.getContacts() };
    }

    @Callback(doc = "function():table -- Only the missiles whose predicted impact is inside the protected circle.")
    public Object[] getIncoming(Context context, Arguments args) {
        return new Object[] { logic.getIncoming() };
    }

    @Callback(doc = "function(r:number):number -- Sets the protected radius, clamped like the GUI; returns it.")
    public Object[] setProtectedRadius(Context context, Arguments args) {
        return new Object[] { logic.setProtectedRadius(args.checkInteger(0)) };
    }
}
