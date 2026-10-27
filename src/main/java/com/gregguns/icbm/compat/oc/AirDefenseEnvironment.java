package com.gregguns.icbm.compat.oc;

import java.util.function.Supplier;

import com.gregguns.icbm.component.AirDefenseComponentLogic;
import com.gregguns.icbm.component.AirDefenseSignals;
import com.gregguns.icbm.component.AirDefenseView;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

/**
 * The {@code gg_air_defense} component for the air-defense battery. Callbacks are indirect (server thread,
 * GGI-01-03); {@code fireAt} runs the battery's own engagement checks (GGI-01-04). {@code sam_fired} and
 * {@code sam_hit} go out once per new event (GGI-01-05). Server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_air_defense)
final class AirDefenseEnvironment extends ViewEnvironment<AirDefenseView, AirDefenseComponentLogic> {

    private final AirDefenseSignals signals = new AirDefenseSignals();

    AirDefenseEnvironment(Supplier<AirDefenseView> lookup) {
        super(AirDefenseComponentLogic.COMPONENT, lookup, AirDefenseComponentLogic::new);
    }

    @Override
    public void update() {
        if (!canSignal() || logicOrNull() == null) {
            return;
        }
        AirDefenseView battery = view();
        for (String name : signals.sample(battery.fired(), battery.hits())) {
            signal(name);
        }
    }

    @Callback(doc = "function():table -- linked, samCount, capacity, auto, fired, hits.")
    public Object[] getInfo(Context context, Arguments args) {
        return new Object[] { logic().getInfo() };
    }

    @Callback(doc = "function():number -- SAMs loaded.")
    public Object[] getSamCount(Context context, Arguments args) {
        return new Object[] { logic().getSamCount() };
    }

    @Callback(
        doc = "function(contactId:number):boolean[, string] -- Fires a SAM at a contact of the linked radar; false and a reason when refused.")
    public Object[] fireAt(Context context, Arguments args) {
        return OcReplies.of(logic().fireAt(args.checkInteger(0)));
    }

    @Callback(doc = "function(on:boolean):boolean -- Lets the battery engage by itself or hold fire; returns the flag.")
    public Object[] setAuto(Context context, Arguments args) {
        return new Object[] { logic().setAuto(args.checkBoolean(0)) };
    }
}
