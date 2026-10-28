package com.gregguns.icbm.compat.oc;

import java.util.function.Supplier;

import com.gregguns.icbm.component.ContactTracker;
import com.gregguns.icbm.component.RadarComponentLogic;
import com.gregguns.icbm.component.RadarView;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

/**
 * The {@code gg_radar} component for the small radars and the tower. Callbacks are indirect (server thread,
 * GGI-01-03). {@code radar_contact}, {@code radar_incoming} and {@code radar_lost} carry the contact id after the
 * component address (OpenComputers adds the address) and go out on changes only (GGI-01-05). Server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_radar)
public final class RadarEnvironment extends ViewEnvironment<RadarView, RadarComponentLogic> {

    private final ContactTracker tracker = new ContactTracker();

    RadarEnvironment(Supplier<RadarView> lookup) {
        super(RadarComponentLogic.COMPONENT, lookup, RadarComponentLogic::new);
    }

    @Override
    public void update() {
        if (!canSignal() || logicOrNull() == null) {
            return;
        }
        RadarView radar = view();
        for (ContactTracker.Signal s : tracker
            .sample(radar.contacts(), radar.centerX(), radar.centerZ(), radar.protectedRadius())) {
            signal(s.name(), s.id());
        }
    }

    @Callback(doc = "function():table -- online, range, protectedRadius and the contact count.")
    public Object[] getInfo(Context context, Arguments args) {
        return new Object[] { logic().getInfo() };
    }

    @Callback(doc = "function():table -- Every missile seen: {id, x, y, z, tier, incoming, impactX, impactZ, eta}.")
    public Object[] getContacts(Context context, Arguments args) {
        return new Object[] { logic().getContacts() };
    }

    @Callback(doc = "function():table -- Only the missiles whose predicted impact is inside the protected circle.")
    public Object[] getIncoming(Context context, Arguments args) {
        return new Object[] { logic().getIncoming() };
    }

    @Callback(doc = "function(r:number):number -- Sets the protected radius, clamped like the GUI; returns it.")
    public Object[] setProtectedRadius(Context context, Arguments args) {
        return new Object[] { logic().setProtectedRadius(args.checkInteger(0)) };
    }
}
