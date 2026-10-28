package com.gregguns.icbm;

import java.util.ArrayList;
import java.util.List;

import com.cleanroommc.modularui.value.sync.ModularSyncManager;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.emp.EmpScenarios;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Scenario support: a machine's screen keeps syncing on the server after its block is broken or its chunk unloads,
 * and every getter on that path must survive the missing tile (the alpha pass lost a server to one). The probe
 * registers a screen's sync values on a recording manager while the machine stands, reads them all, removes the
 * block, and reads them again.
 */
public final class GuiVanishProbe {

    /** Ticks GT needs before a placed machine is a settled tile entity. */
    private static final int WARMUP = 45;

    /** Registers one screen's sync values on the given manager. */
    public interface Registrar {

        /**
         * @param machine the machine behind the screen
         * @param sync    the manager to register the screen's sync values on
         */
        void register(IMetaTileEntity machine, PanelSyncManager sync);
    }

    private GuiVanishProbe() {}

    /** A manager that remembers every sync value registered on it. */
    private static final class Recorder extends PanelSyncManager {

        final List<SyncHandler<?>> handlers = new ArrayList<>();

        Recorder() {
            super(new ModularSyncManager(false), false);
        }

        @Override
        public PanelSyncManager syncValue(String name, int id, SyncHandler<?> handler) {
            handlers.add(handler);
            return this;
        }
    }

    /**
     * @param name        the scenario id
     * @param description what it checks
     * @param mteId       the machine's MTE id
     * @param registrar   registers the machine's screen
     * @return a scenario that removes the machine under its screen's sync path
     */
    public static Scenario scenario(String name, String description, int mteId, Registrar registrar) {
        return new Scenario() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public ScenarioResult run(ScenarioContext ctx) {
                if (!ModuleState.isActive(ModuleId.ICBM)) {
                    return ScenarioResult.skip(name, "the ICBM module is off");
                }
                ctx.clearArea(6, 4);
                EmpScenarios.place(ctx, 0, 0, 0, mteId);
                ScenarioResult.Builder out = ScenarioResult.builder(name);
                return ScenarioResult.pending(name, WARMUP + 20, ticks -> {
                    if (ticks < WARMUP) {
                        return null;
                    }
                    IGregTechTileEntity base = EmpScenarios.live(ctx, 0, 0, 0);
                    IMetaTileEntity mte = base == null ? null : base.getMetaTileEntity();
                    if (mte == null) {
                        return ScenarioResult.fail(name, "the machine is gone after the warm-up");
                    }
                    try {
                        Recorder rec = new Recorder();
                        registrar.register(mte, rec);
                        out.check(rec.handlers.size() >= 3, "the screen registered its sync values");
                        out.check(readAll(rec), "every value reads while the machine stands");
                        ctx.world()
                            .setBlockToAir(ctx.originX(), ctx.originY(), ctx.originZ());
                        out.check(!IcbmSync.live(mte), "the machine reads as gone after its block is removed");
                        boolean survived = true;
                        String failure = "";
                        for (int pass = 0; pass < 2; pass++) {
                            try {
                                readAll(rec);
                            } catch (RuntimeException e) {
                                survived = false;
                                failure = e.toString();
                            }
                        }
                        out.check(
                            survived,
                            "reading every sync value after the block is gone does not throw " + failure);
                    } catch (RuntimeException e) {
                        out.check(false, "probe failed: " + e);
                    }
                    return out.build();
                });
            }
        };
    }

    private static boolean readAll(Recorder rec) {
        for (SyncHandler<?> h : rec.handlers) {
            if (h instanceof ValueSyncHandler) {
                ((ValueSyncHandler<?, ?>) h).updateCacheFromSource(true);
            }
        }
        return true;
    }
}
