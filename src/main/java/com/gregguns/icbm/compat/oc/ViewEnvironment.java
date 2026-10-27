package com.gregguns.icbm.compat.oc;

import java.util.function.Function;
import java.util.function.Supplier;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.ManagedEnvironment;

/**
 * The shared part of the ICBM component environments: the node, the component name that must win over other drivers,
 * the machine lookup and the signal send.
 *
 * <p>
 * Name: OpenComputers merges every driver that matches a block into one compound component, named after the
 * {@link NamedBlock} with the highest priority. Every GT tile is also an {@code IBasicEnergyContainer}, which GTNH's
 * OpenComputers names {@code gt_energyContainer} at priority -1, so without a name of our own the Adapter would show
 * that and not {@code gg_*}. Ours has priority {@value #PRIORITY}.
 *
 * <p>
 * Lookup: the environment keeps no machine of its own. Each call and each tick asks the {@link Supplier} for the
 * machine at the position, so a machine whose chunk unloaded and came back is the new one, and a machine that is gone
 * refuses (the call raises {@code machine_gone}) instead of acting on a stale copy.
 *
 * <p>
 * Signals: {@code computer.signal} gets the sending node's address prepended by OpenComputers. Our node is only a
 * neighbor of the compound node, so the address must be the compound's (the one the computer lists), and the signal
 * is sent from there.
 *
 * <p>
 * Visibility: the concrete environments are public. OpenComputers generates a call wrapper per callback in its own
 * package ({@code generated.li.cil.oc}), which fails with an IllegalAccessError on a package-private class (found by
 * the {@code icbm.oc.components} scenario).
 *
 * @param <V> the view interface of the machine
 * @param <L> the component logic over that view
 */
// Source (NamedBlock, merge by highest priority):
// https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/scala/li/cil/oc/server/driver/CompoundBlockDriver.scala#L39-L45
// Source (the driver that would win by default, priority -1, "gt_energyContainer"):
// https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/scala/li/cil/oc/integration/gregtech/DriverEnergyContainer.java#L27-L37
// Source (the compound node is the component; wrapped nodes are forced to neighbor visibility):
// https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/scala/li/cil/oc/server/driver/CompoundBlockEnvironment.scala#L17-L28
abstract class ViewEnvironment<V, L> extends ManagedEnvironment implements NamedBlock {

    /** Above GTNH's {@code gt_energyContainer} (-1), so our name is the component's name. */
    static final int PRIORITY = 10;

    private final String component;
    private final Supplier<V> lookup;
    private final Function<V, L> logicFactory;
    private V view;
    private L logic;

    ViewEnvironment(String component, Supplier<V> lookup, Function<V, L> logicFactory) {
        this.component = component;
        this.lookup = lookup;
        this.logicFactory = logicFactory;
        // Source:
        // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/Network.java#L183
        setNode(
            Network.newNode(this, Visibility.Network)
                .withComponent(component)
                .create());
    }

    @Override
    public String preferredName() {
        return component;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    /**
     * Looks the machine up again; a new machine at the position gets a new logic object.
     *
     * @return the logic, or null when the machine is gone
     */
    final L logicOrNull() {
        V current = lookup.get();
        if (current == null) {
            view = null;
            logic = null;
            return null;
        }
        if (current != view) {
            view = current;
            logic = logicFactory.apply(current);
        }
        return logic;
    }

    /** @return the logic for a callback; raises {@code machine_gone} to the program when the machine is gone */
    final L logic() {
        L l = logicOrNull();
        if (l == null) {
            throw new IllegalStateException("machine_gone");
        }
        return l;
    }

    /** @return the machine behind the logic last returned; only valid after {@link #logicOrNull()} gave non-null */
    final V view() {
        return view;
    }

    /** @return true when a tick may sample the machine and send signals */
    final boolean canSignal() {
        return node() != null && node().network() != null;
    }

    /**
     * Sends a {@code computer.signal} from the compound node, so the address OpenComputers prepends is the one the
     * program lists. Falls back to our own node when there is no compound neighbor.
     */
    final void signal(String name, Object... args) {
        Object[] data = new Object[args.length + 1];
        data[0] = name;
        System.arraycopy(args, 0, data, 1, args.length);
        for (Node neighbor : node().neighbors()) {
            neighbor.sendToReachable("computer.signal", data);
            return;
        }
        node().sendToReachable("computer.signal", data);
    }
}
