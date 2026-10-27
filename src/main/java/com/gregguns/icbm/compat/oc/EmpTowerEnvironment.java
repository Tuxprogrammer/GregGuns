package com.gregguns.icbm.compat.oc;

import java.util.function.Supplier;

import com.gregguns.icbm.component.EmpComponentLogic;
import com.gregguns.icbm.component.SignalTracker;
import com.gregguns.icbm.emp.MTEEmpTower;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

/**
 * The {@code gg_emp_tower} component: unpacks OpenComputers arguments and calls {@link EmpComponentLogic}. Every
 * callback is indirect (the default), so OpenComputers runs it on the server thread (GGI-01-03), and {@code fire}
 * passes the tower's own gate (GGI-01-04). Signals are polled once a tick and sent on transitions only (GGI-01-05),
 * with the component address first (OpenComputers adds it). Server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_emp_tower)
public final class EmpTowerEnvironment extends ViewEnvironment<MTEEmpTower, EmpComponentLogic> {

    private final SignalTracker signals = new SignalTracker();

    EmpTowerEnvironment(Supplier<MTEEmpTower> lookup) {
        super(EmpComponentLogic.COMPONENT, lookup, EmpComponentLogic::new);
    }

    @Override
    public void update() {
        if (!canSignal()) {
            return;
        }
        EmpComponentLogic l = logicOrNull();
        if (l == null) {
            return;
        }
        for (String name : signals.sample(l.isReady(), view().pulses())) {
            signal(name);
        }
    }

    @Callback(doc = "function():table -- Constants: minRadius, maxRadius, euPerArea, cooldownTicks, maxDevices.")
    public Object[] getInfo(Context context, Arguments args) {
        return new Object[] { logic().info() };
    }

    @Callback(doc = "function():boolean -- True when the tower can fire now.")
    public Object[] isReady(Context context, Arguments args) {
        return new Object[] { logic().isReady() };
    }

    @Callback(doc = "function():number -- Ticks until the next pulse may go.")
    public Object[] getCooldown(Context context, Arguments args) {
        return new Object[] { logic().getCooldown() };
    }

    @Callback(doc = "function():string -- The mode: all, missiles or machines.")
    public Object[] getMode(Context context, Arguments args) {
        return new Object[] { logic().getMode() };
    }

    @Callback(doc = "function(mode:string):boolean[, string] -- Sets the mode; false and a reason for a bad name.")
    public Object[] setMode(Context context, Arguments args) {
        return OcReplies.of(logic().setMode(args.checkString(0)));
    }

    @Callback(doc = "function():number -- The pulse radius in blocks.")
    public Object[] getRadius(Context context, Arguments args) {
        return new Object[] { logic().getRadius() };
    }

    @Callback(
        doc = "function(radius:number):number -- Sets the radius, clamped like the GUI; returns the radius now set.")
    public Object[] setRadius(Context context, Arguments args) {
        return new Object[] { logic().setRadius(args.checkInteger(0)) };
    }

    @Callback(
        doc = "function():boolean[, string] -- Fires a pulse through the same checks as the button; false and a reason when refused.")
    public Object[] fire(Context context, Arguments args) {
        return OcReplies.of(logic().fire());
    }
}
