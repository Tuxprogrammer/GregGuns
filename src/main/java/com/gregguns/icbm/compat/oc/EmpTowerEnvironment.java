package com.gregguns.icbm.compat.oc;

import com.gregguns.icbm.component.EmpComponentLogic;
import com.gregguns.icbm.component.Reply;
import com.gregguns.icbm.component.SignalTracker;
import com.gregguns.icbm.emp.MTEEmpTower;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ManagedEnvironment;

/**
 * The {@code gg_emp_tower} component: unpacks OpenComputers arguments and calls {@link EmpComponentLogic}. Every
 * callback is indirect (the default), so OpenComputers runs it on the server thread (GGI-01-03), and {@code fire}
 * passes the tower's own gate (GGI-01-04). Signals are polled once a tick and sent on transitions only (GGI-01-05),
 * with the component address first. Server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_emp_tower)
final class EmpTowerEnvironment extends ManagedEnvironment {

    private final MTEEmpTower tower;
    private final EmpComponentLogic logic;
    private final SignalTracker signals = new SignalTracker();

    EmpTowerEnvironment(MTEEmpTower tower) {
        this.tower = tower;
        this.logic = new EmpComponentLogic(tower);
        // Source:
        // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/Network.java#L183
        setNode(
            Network.newNode(this, Visibility.Network)
                .withComponent(EmpComponentLogic.COMPONENT)
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
        for (String name : signals.sample(logic.isReady(), tower.pulses())) {
            node().sendToReachable("computer.signal", name, node().address());
        }
    }

    @Callback(doc = "function():table -- Constants: minRadius, maxRadius, euPerArea, cooldownTicks, maxDevices.")
    public Object[] getInfo(Context context, Arguments args) {
        return new Object[] { logic.info() };
    }

    @Callback(doc = "function():boolean -- True when the tower can fire now.")
    public Object[] isReady(Context context, Arguments args) {
        return new Object[] { logic.isReady() };
    }

    @Callback(doc = "function():number -- Ticks until the next pulse may go.")
    public Object[] getCooldown(Context context, Arguments args) {
        return new Object[] { logic.getCooldown() };
    }

    @Callback(doc = "function():string -- The mode: all, missiles or machines.")
    public Object[] getMode(Context context, Arguments args) {
        return new Object[] { logic.getMode() };
    }

    @Callback(doc = "function(mode:string):boolean[, string] -- Sets the mode; false and a reason for a bad name.")
    public Object[] setMode(Context context, Arguments args) {
        return reply(logic.setMode(args.checkString(0)));
    }

    @Callback(doc = "function():number -- The pulse radius in blocks.")
    public Object[] getRadius(Context context, Arguments args) {
        return new Object[] { logic.getRadius() };
    }

    @Callback(
        doc = "function(radius:number):number -- Sets the radius, clamped like the GUI; returns the radius now set.")
    public Object[] setRadius(Context context, Arguments args) {
        return new Object[] { logic.setRadius(args.checkInteger(0)) };
    }

    @Callback(
        doc = "function():boolean[, string] -- Fires a pulse through the same checks as the button; false and a reason when refused.")
    public Object[] fire(Context context, Arguments args) {
        return reply(logic.fire());
    }

    private static Object[] reply(Reply r) {
        return r.isOk() ? new Object[] { true } : new Object[] { false, r.reason() };
    }

}
