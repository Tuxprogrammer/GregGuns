package com.gregguns.icbm.compat.oc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.emp.EmpScenarios;
import com.gregguns.machine.GregGunsMTEIDs;

import li.cil.oc.api.Items;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;

/**
 * The live OpenComputers scenario (I4 exit box, GGI-01-02): a real Adapter block beside each ICBM machine, read through
 * OpenComputers' own API the way a computer reads it, with no client. Registered only when OpenComputers is loaded
 * (see {@code IcbmModule}), so it never touches an OC class otherwise. Dev environment only. Server side.
 *
 * <p>
 * No computer runs: a computer reaches a component through its network, which is the Adapter's node here, and
 * {@link Component#invoke} is what the machine's driver calls. Only read-only calls are made, and the machines are
 * placed unformed and uncharged (the silo and the radar tower are controllers without their structure), which is
 * enough for every getter to answer.
 */
// Spec: docs/gregguns-icbm-spec/01-core/golden-dome.md (Components, GGI-01-02)
public final class OcScenarios {

    /** Ticks GT and the Adapter need before a placed block is on the network. */
    private static final int WARMUP = 60;
    /** X spacing between machines, so no Adapter touches a second machine. */
    private static final int SPACING = 4;

    private OcScenarios() {}

    /** @param registry where to add the scenario */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Components());
    }

    /** One machine, the component it must show as, and the read-only call to check. */
    private static final class Case {

        final String label;
        final int mteId;
        final String component;
        final String method;
        final String expectedMethod;
        final String shapeKey;

        Case(String label, int mteId, String component, String method, String shapeKey) {
            this.label = label;
            this.mteId = mteId;
            this.component = component;
            this.method = method;
            this.expectedMethod = method;
            this.shapeKey = shapeKey;
        }
    }

    private static final Case[] CASES = {
        new Case("Launch Control", GregGunsMTEIDs.LAUNCH_CONTROL, "gg_launch_control", "getInfo", "status"),
        new Case("Missile Silo (unformed)", GregGunsMTEIDs.MISSILE_SILO, "gg_silo", "getInfo", "status"),
        new Case("HV Radar", GregGunsMTEIDs.RADAR_HV, "gg_radar", "getContacts", null),
        new Case("Radar Tower (unformed)", GregGunsMTEIDs.RADAR_TOWER, "gg_radar", "getInfo", "range"),
        new Case("Air-Defense Battery", GregGunsMTEIDs.AIR_DEFENSE, "gg_air_defense", "getInfo", "samCount"),
        new Case("EMP Tower", GregGunsMTEIDs.EMP_TOWER, "gg_emp_tower", "getInfo", "maxRadius") };

    private static final class Components implements Scenario {

        @Override
        public String name() {
            return "icbm.oc.components";
        }

        @Override
        public String description() {
            return "A real OpenComputers Adapter beside each ICBM machine exposes the gg_* component (not"
                + " gt_energyContainer) with its callbacks, and one read-only call per machine, made through"
                + " OpenComputers' own Component.invoke, returns the specified shape.";
        }

        @Override
        public int areaRadius() {
            return 30;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!ModuleState.isActive(ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), "the ICBM module is off, so no ICBM driver is registered");
            }
            // Source:
            // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/Items.java
            Block adapter = Items.get("adapter")
                .block();
            if (adapter == null) {
                return ScenarioResult.skip(name(), "OpenComputers has no Adapter block");
            }
            ctx.clearArea(28, 4);
            for (int i = 0; i < CASES.length; i++) {
                EmpScenarios.place(ctx, i * SPACING, 0, 0, CASES[i].mteId);
                ctx.setBlock(i * SPACING + 1, 0, 0, adapter, 0);
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            return ScenarioResult.pending(name(), WARMUP + 100, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                for (int i = 0; i < CASES.length; i++) {
                    check(ctx, out, CASES[i], i * SPACING + 1);
                }
                return out.build();
            });
        }

        private void check(ScenarioContext ctx, ScenarioResult.Builder out, Case c, int adapterDx) {
            TileEntity tile = ctx.world()
                .getTileEntity(ctx.originX() + adapterDx, ctx.originY(), ctx.originZ());
            if (!(tile instanceof Environment)) {
                out.check(false, c.label + ": the Adapter block has an environment, found " + tile);
                return;
            }
            Node adapterNode = ((Environment) tile).node();
            List<String> names = new ArrayList<>();
            Component found = null;
            // Source (a computer sees the components reachable from the Adapter's node):
            // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/network/Node.java
            for (Node n : adapterNode.reachableNodes()) {
                // A computer lists the network-visible components; the drivers wrapped inside the compound (including
                // GTNH's gt_energyContainer) are neighbor-only and reachable from the compound alone.
                if (n instanceof Component && ((Component) n).visibility() == Visibility.Network) {
                    names.add(((Component) n).name());
                    if (c.component.equals(((Component) n).name())) {
                        found = (Component) n;
                    }
                }
            }
            out.check(found != null, c.label + ": the Adapter shows " + c.component + ", components seen: " + names);
            out.check(
                names.size() >= 1 && !names.contains("gt_energyContainer"),
                c.label + ": not named gt_energyContainer: " + names);
            if (found == null) {
                return;
            }
            // Source:
            // https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/network/Component.java
            Collection<String> methods = found.methods();
            out.check(methods.contains(c.expectedMethod), c.label + ": lists " + c.expectedMethod + ": " + methods);
            Object[] reply;
            try {
                reply = found.invoke(c.method, null);
            } catch (Exception | LinkageError e) {
                out.check(false, c.label + ": " + c.method + "() threw " + e);
                return;
            }
            out.check(reply != null && reply.length == 1, c.label + ": " + c.method + "() answers one value");
            if (reply == null || reply.length != 1) {
                return;
            }
            Object value = reply[0];
            if (c.shapeKey == null) {
                // getContacts: an empty list (a Lua table), there is no missile in the air.
                out.check(
                    (value instanceof List && ((List<?>) value).isEmpty())
                        || (value instanceof Object[] && ((Object[]) value).length == 0),
                    c.label + ": getContacts() is an empty list, got " + java.util.Objects.toString(value));
            } else {
                out.check(
                    value instanceof Map && ((Map<?, ?>) value).containsKey(c.shapeKey),
                    c.label + ": " + c.method + "() is a table with '" + c.shapeKey + "', got " + value);
            }
            if ("gg_emp_tower".equals(c.component)) {
                try {
                    Object[] ready = found.invoke("isReady", null);
                    out.checkEquals("EMP Tower: isReady() is false with no EU", Boolean.FALSE, ready[0]);
                    Object[] mode = found.invoke("getMode", null);
                    out.checkEquals("EMP Tower: getMode() is a string", String.class, mode[0].getClass());
                } catch (Exception | LinkageError e) {
                    out.check(false, "EMP Tower: status calls threw " + e);
                }
            }
        }
    }
}
