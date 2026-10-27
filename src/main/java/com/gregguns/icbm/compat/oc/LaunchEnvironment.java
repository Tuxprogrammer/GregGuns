package com.gregguns.icbm.compat.oc;

import java.util.function.Supplier;

import com.gregguns.icbm.component.LaunchComponentLogic;
import com.gregguns.icbm.component.LaunchView;
import com.gregguns.icbm.component.SignalTracker;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

/**
 * The {@code gg_silo} and {@code gg_launch_control} component: unpacks arguments and calls
 * {@link LaunchComponentLogic}. Every callback is indirect, so OpenComputers runs it on the server thread (GGI-01-03),
 * and {@code launch} passes the machine's own gate (GGI-01-04). {@code launcher_ready} and {@code launcher_fired} go
 * out on transitions only (GGI-01-05), with the component address first (OpenComputers adds it), sampled every 10
 * ticks. Server side.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (gg_silo, gg_launch_control)
final class LaunchEnvironment extends ViewEnvironment<LaunchView, LaunchComponentLogic> {

    private static final int SAMPLE_TICKS = 10;

    private final SignalTracker signals = new SignalTracker("launcher_fired", "launcher_ready");
    private int tick;

    LaunchEnvironment(Supplier<LaunchView> lookup, String component) {
        super(component, lookup, LaunchComponentLogic::new);
    }

    @Override
    public void update() {
        // The ready word recomputes the full status (silo and pad searches), so sample twice a second, not every tick.
        if (!canSignal() || ++tick % SAMPLE_TICKS != 0) {
            return;
        }
        LaunchComponentLogic l = logicOrNull();
        if (l == null) {
            return;
        }
        boolean ready = l.canLaunch()
            .isOk();
        for (String name : signals.sample(ready, view().launches())) {
            signal(name);
        }
    }

    @Callback(doc = "function():table -- Machine constants, the status word and the launch count.")
    public Object[] getInfo(Context context, Arguments args) {
        return new Object[] { logic().getInfo() };
    }

    @Callback(doc = "function():table|nil -- The missile in place: tier, armed, fueled, guidance, explosive, fill.")
    public Object[] getMissile(Context context, Arguments args) {
        return new Object[] { logic().getMissile() };
    }

    @Callback(doc = "function():table -- {fluid, amount, required}: the tank in mB.")
    public Object[] getFuel(Context context, Arguments args) {
        return new Object[] { logic().getFuel() };
    }

    @Callback(doc = "function():table|nil -- {x, z} of the stored target, or nil.")
    public Object[] getTarget(Context context, Arguments args) {
        return new Object[] { logic().getTarget() };
    }

    @Callback(
        doc = "function(x:number[, y:number], z:number):boolean[, string] -- Sets the target column; the height is ignored.")
    public Object[] setTarget(Context context, Arguments args) {
        double x = args.checkDouble(0);
        double z = args.checkDouble(args.count() >= 3 ? 2 : 1);
        return OcReplies.of(logic().setTarget(x, z));
    }

    @Callback(doc = "function():boolean[, string] -- Copies the target from the data stick in the block.")
    public Object[] setTargetFromStick(Context context, Arguments args) {
        return OcReplies.of(logic().setTargetFromStick());
    }

    @Callback(doc = "function():boolean[, string] -- True when a launch would go now; else the first problem.")
    public Object[] canLaunch(Context context, Arguments args) {
        return OcReplies.of(logic().canLaunch());
    }

    @Callback(
        doc = "function():boolean[, string] -- Launches through the same checks as the button; false and a reason when refused.")
    public Object[] launch(Context context, Arguments args) {
        return OcReplies.of(logic().launch());
    }
}
