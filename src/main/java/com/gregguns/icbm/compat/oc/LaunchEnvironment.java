package com.gregguns.icbm.compat.oc;

import com.gregguns.icbm.component.LaunchComponentLogic;
import com.gregguns.icbm.component.LaunchView;
import com.gregguns.icbm.component.SignalTracker;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ManagedEnvironment;

/**
 * The {@code gg_silo} and {@code gg_launch_control} component: unpacks arguments and calls
 * {@link LaunchComponentLogic}. Every callback is indirect, so OpenComputers runs it on the server thread (GGI-01-03),
 * and {@code launch} passes the machine's own gate (GGI-01-04). {@code launcher_ready} and {@code launcher_fired} go
 * out on transitions only (GGI-01-05), with the component address first. Server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_silo, gg_launch_control)
final class LaunchEnvironment extends ManagedEnvironment {

    private final LaunchView machine;
    private final LaunchComponentLogic logic;
    private final SignalTracker signals = new SignalTracker("launcher_fired", "launcher_ready");

    LaunchEnvironment(LaunchView machine, String component) {
        this.machine = machine;
        this.logic = new LaunchComponentLogic(machine);
        // Source:
        // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/Network.java#L183
        setNode(
            Network.newNode(this, Visibility.Network)
                .withComponent(component)
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
        boolean ready = logic.canLaunch()
            .isOk();
        for (String name : signals.sample(ready, machine.launches())) {
            node().sendToReachable("computer.signal", name, node().address());
        }
    }

    @Callback(doc = "function():table -- Machine constants, the status word and the launch count.")
    public Object[] getInfo(Context context, Arguments args) {
        return new Object[] { logic.getInfo() };
    }

    @Callback(doc = "function():table|nil -- The missile in place: tier, armed, fueled, guidance, explosive, fill.")
    public Object[] getMissile(Context context, Arguments args) {
        return new Object[] { logic.getMissile() };
    }

    @Callback(doc = "function():table -- {fluid, amount, required}: the tank in mB.")
    public Object[] getFuel(Context context, Arguments args) {
        return new Object[] { logic.getFuel() };
    }

    @Callback(doc = "function():table|nil -- {x, z} of the stored target, or nil.")
    public Object[] getTarget(Context context, Arguments args) {
        return new Object[] { logic.getTarget() };
    }

    @Callback(
        doc = "function(x:number[, y:number], z:number):boolean[, string] -- Sets the target column; the height is ignored.")
    public Object[] setTarget(Context context, Arguments args) {
        double x = args.checkDouble(0);
        double z = args.checkDouble(args.count() >= 3 ? 2 : 1);
        return OcReplies.of(logic.setTarget(x, z));
    }

    @Callback(doc = "function():boolean[, string] -- Copies the target from the data stick in the block.")
    public Object[] setTargetFromStick(Context context, Arguments args) {
        return OcReplies.of(logic.setTargetFromStick());
    }

    @Callback(doc = "function():boolean[, string] -- True when a launch would go now; else the first problem.")
    public Object[] canLaunch(Context context, Arguments args) {
        return OcReplies.of(logic.canLaunch());
    }

    @Callback(
        doc = "function():boolean[, string] -- Launches through the same checks as the button; false and a reason when refused.")
    public Object[] launch(Context context, Arguments args) {
        return OcReplies.of(logic.launch());
    }
}
