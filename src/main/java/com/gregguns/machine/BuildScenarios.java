package com.gregguns.machine;

import net.minecraft.tileentity.TileEntity;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Dev-only scenario for {@code /gregguns build} ({@link DevStructureBuilder}): the Ammunition Works, the Armory Gantry
 * and the Rampart each stand as a bare controller, get built, and must report a formed structure.
 */
public final class BuildScenarios {

    private static final int RADIUS = 24;
    private static final int HEIGHT = 12;
    /** Height of the controller above the origin; the floor is two lower. */
    private static final int CY = 3;
    /** Ticks to let GT initialize freshly placed tile entities before the structure check. */
    private static final int WARMUP_TICKS = 45;
    /** Further ticks in which a not-yet-formed machine is rechecked before the scenario fails. */
    private static final int RETRY_TICKS = 40;
    private static final int PHASE_TICKS = WARMUP_TICKS + RETRY_TICKS + 5;

    private static final int[] IDS = { GregGunsMTEIDs.AMMO_WORKS, GregGunsMTEIDs.ARMORY_GANTRY,
        GregGunsMTEIDs.RAMPART };
    private static final String[] NAMES = { "Ammunition Works", "Armory Gantry", "Rampart" };

    private BuildScenarios() {}

    /**
     * @param registry where to add the scenario
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new BuildMultiblocksScenario());
    }

    /** The three multiblocks form after {@code build}. */
    private static final class BuildMultiblocksScenario implements Scenario {

        @Override
        public String name() {
            return "testing.build_multiblocks";
        }

        @Override
        public String description() {
            return "The dev build command (casings from the controller's hologram routine, then the required hatches) forms the Ammunition Works, the Armory Gantry and the Rampart, each from a bare controller; a stone block is not a machine.";
        }

        @Override
        public int areaRadius() {
            return RADIUS;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            for (int id : IDS) {
                if (id >= GregTechAPI.METATILEENTITIES.length || GregTechAPI.METATILEENTITIES[id] == null) {
                    return ScenarioResult
                        .fail(name(), "MTE " + id + " is not registered (needs GG_MODULES=guns,machines,utility)");
                }
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            DevStructureBuilder.Outcome none = DevStructureBuilder
                .build(ctx.world(), ctx.originX(), ctx.originY() - 1, ctx.originZ(), 0);
            out.check(!none.formed, "a floor block is not a machine: " + none);
            int[] phase = { 0 };
            int[] startTick = { -1 };
            return ScenarioResult.pending(name(), IDS.length * PHASE_TICKS + 100, ticks -> {
                int cx = ctx.originX();
                int cy = ctx.originY() + CY;
                int cz = ctx.originZ();
                if (startTick[0] < 0) {
                    ctx.clearArea(RADIUS, HEIGHT);
                    IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[IDS[phase[0]]];
                    ctx.setBlock(0, CY, 0, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
                    TileEntity tile = ctx.world()
                        .getTileEntity(cx, cy, cz);
                    if (!(tile instanceof IGregTechTileEntity)) {
                        return ScenarioResult.fail(name(), "no GT tile entity for " + NAMES[phase[0]]);
                    }
                    ((IGregTechTileEntity) tile).setInitialValuesAsNBT(null, (short) IDS[phase[0]]);
                    DevStructureBuilder.Outcome built = DevStructureBuilder.build(ctx.world(), cx, cy, cz, 0);
                    startTick[0] = ticks;
                    out.note(NAMES[phase[0]] + " first attempt: " + built);
                    return null;
                }
                if (ticks - startTick[0] < WARMUP_TICKS) {
                    return null;
                }
                boolean formed = DevStructureBuilder.check(ctx.world(), cx, cy, cz);
                if (!formed && ticks - startTick[0] < WARMUP_TICKS + RETRY_TICKS) {
                    return null;
                }
                out.check(formed, "the " + NAMES[phase[0]] + " should be formed after build (mMachine)");
                phase[0]++;
                startTick[0] = -1;
                if (phase[0] < IDS.length) {
                    return null;
                }
                ctx.clearArea(RADIUS, HEIGHT);
                return out.build();
            });
        }
    }
}
