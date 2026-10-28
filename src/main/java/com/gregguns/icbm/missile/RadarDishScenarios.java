package com.gregguns.icbm.missile;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.defense.EntityRadarDish;
import com.gregguns.icbm.defense.MTERadar;
import com.gregguns.icbm.defense.RadarRules;
import com.gregguns.machine.DevStructureBuilder;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Headless scenarios of the modeled radar (D125): an unformed controller is inert (the old single-block radar of an
 * old world), the dish stands once when the radar forms, spins while it runs, stops without power and goes when the
 * structure breaks, and a reload never leaves two. They live in the missile package for the shared placing helpers
 * of {@link DefenseScenarios}. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (The small radar, The dish)
final class RadarDishScenarios {

    private static final int WARMUP_TICKS = 50;
    private static final int CY = 5;

    private RadarDishScenarios() {}

    /** @param registry where to add the scenarios */
    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Unformed());
        registry.register(new Lifecycle());
        registry.register(new Reload());
    }

    private abstract static class Base implements Scenario {

        @Override
        public final ScenarioResult run(ScenarioContext ctx) {
            if (!ModuleState.isActive(ModuleId.ICBM) || MissileScenarios.moduleOff()) {
                return ScenarioResult.skip(name(), "the ICBM module is off");
            }
            return go(ctx);
        }

        abstract ScenarioResult go(ScenarioContext ctx);
    }

    /** How many live dishes stand within a few blocks of the radar at the offset. */
    private static int dishes(ScenarioContext ctx, int dx, int dz) {
        double x = ctx.originX() + dx + 0.5;
        double y = ctx.originY() + CY;
        double z = ctx.originZ() + dz + 0.5;
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x - 8, y - 8, z - 8, x + 8, y + 8, z + 8);
        int n = 0;
        for (Object o : ctx.world()
            .getEntitiesWithinAABB(EntityRadarDish.class, box)) {
            if (!((EntityRadarDish) o).isDead) {
                n++;
            }
        }
        return n;
    }

    private static void drain(ScenarioContext ctx, int dx, int dz) {
        IGregTechTileEntity hatch = DefenseScenarios.live(ctx, dx, CY - 1, dz);
        if (hatch != null) {
            hatch.decreaseStoredEnergyUnits(hatch.getStoredEU(), true);
        }
    }

    /** A placed controller with no structure does nothing: what an old world's single-block radar becomes. */
    private static final class Unformed extends Base {

        @Override
        public String name() {
            return "icbm.defense.radar_unformed";
        }

        @Override
        public String description() {
            return "A placed Radar (HV) controller with no structure is inert: it is not formed, not online, reports OFF with an empty sweep and no redstone, and no dish stands.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(8, 24);
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            DefenseScenarios.place(ctx, GregGunsMTEIDs.RADAR_HV, 0, CY, 0);
            return ScenarioResult.pending(name(), 300, ticks -> {
                if (ticks < WARMUP_TICKS + 60) {
                    return null;
                }
                MTERadar r = DefenseScenarios.radar(DefenseScenarios.live(ctx, 0, CY, 0));
                if (r == null) {
                    return ScenarioResult.fail(name(), "the controller is gone");
                }
                out.check(!r.isFormed(), "the controller is not formed");
                out.check(!r.online(), "it is not online");
                out.checkEquals("it reports OFF", RadarRules.State.OFF, r.state());
                out.checkEquals("range reads 0 while offline", 0, r.range());
                out.checkEquals(
                    "the sweep is empty",
                    0,
                    r.sweep()
                        .detected()
                        .size());
                out.checkEquals("no redstone", 0, r.signal());
                out.checkEquals("no dish", 0, dishes(ctx, 0, 0));
                return out.build();
            });
        }
    }

    /** Forms, spawns one dish, spins with power, stops without it, and goes when a casing is broken. */
    private static final class Lifecycle extends Base {

        @Override
        public String name() {
            return "icbm.defense.radar_dish";
        }

        @Override
        public String description() {
            return "A formed radar has exactly one dish; it is still without power, spins with it, stops when the power goes, and is gone once a casing is broken.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(8, 24);
            final World w = ctx.world();
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            DefenseScenarios.place(ctx, GregGunsMTEIDs.RADAR_HV, 0, CY, 0);
            final int[] stage = { 0 };
            final int[] mark = { 0 };
            final float[] angle = { 0 };
            return ScenarioResult.pending(name(), 1500, ticks -> {
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                MTERadar r = DefenseScenarios.radar(DefenseScenarios.live(ctx, 0, CY, 0));
                if (r == null) {
                    return ScenarioResult.fail(name(), "the controller is gone");
                }
                int x = ctx.originX();
                int y = ctx.originY() + CY;
                int z = ctx.originZ();
                if (stage[0] == 0) {
                    if (!DevStructureBuilder.buildRadar(w, x, y, z, ItemList.Hatch_Energy_HV).formed) {
                        return ticks < 400 ? null : ScenarioResult.fail(name(), "the radar did not form");
                    }
                    stage[0] = 1;
                    mark[0] = ticks;
                    return null;
                }
                if (stage[0] == 1) {
                    // Formed, no power: the dish stands but is still.
                    if (r.dish() == null) {
                        return ticks < mark[0] + 200 ? null : ScenarioResult.fail(name(), "no dish after forming");
                    }
                    out.checkEquals("one dish when formed", 1, dishes(ctx, 0, 0));
                    out.check(
                        !r.dish()
                            .spin()
                            .spinning(),
                        "the dish is still without power");
                    out.check(!r.online(), "the radar is not online without power");
                    stage[0] = 2;
                    mark[0] = ticks;
                    return null;
                }
                if (stage[0] == 2) {
                    DefenseScenarios.powerHatch(ctx, 0, CY, 0);
                    if (!(r.online() && r.dish() != null
                        && r.dish()
                            .spin()
                            .spinning())) {
                        return ticks < mark[0] + 400 ? null : ScenarioResult.fail(name(), "never ran and spun");
                    }
                    angle[0] = r.dish()
                        .spin()
                        .angle(w.getTotalWorldTime());
                    stage[0] = 3;
                    mark[0] = ticks;
                    return null;
                }
                if (stage[0] == 3) {
                    DefenseScenarios.powerHatch(ctx, 0, CY, 0);
                    if (ticks < mark[0] + 10) {
                        return null;
                    }
                    float now = r.dish()
                        .spin()
                        .angle(w.getTotalWorldTime());
                    out.check(Math.abs(now - angle[0]) > 20.0F, "the angle moves with the game time");
                    out.checkEquals("still one dish", 1, dishes(ctx, 0, 0));
                    stage[0] = 4;
                    mark[0] = ticks;
                    return null;
                }
                if (stage[0] == 4) {
                    drain(ctx, 0, 0);
                    if (r.online() || r.dish() == null
                        || r.dish()
                            .spin()
                            .spinning()) {
                        return ticks < mark[0] + 300 ? null : ScenarioResult.fail(name(), "never stopped without EU");
                    }
                    out.check(true, "the dish stops when the power goes");
                    // Break a corner of the base, then recheck the structure.
                    w.setBlockToAir(x + 1, y - 1, z + 2);
                    DevStructureBuilder.check(w, x, y, z);
                    stage[0] = 5;
                    mark[0] = ticks;
                    return null;
                }
                if (dishes(ctx, 0, 0) > 0 || r.dish() != null) {
                    if (ticks < mark[0] + 500) {
                        DevStructureBuilder.check(w, x, y, z);
                        return null;
                    }
                    return ScenarioResult.fail(name(), "the dish is still there after the casing broke");
                }
                out.check(!r.isFormed(), "the radar is no longer formed");
                return out.build();
            });
        }
    }

    /** A reload keeps one dish; a dish lost with its chunk is replaced by exactly one. */
    private static final class Reload extends Base {

        @Override
        public String name() {
            return "icbm.defense.radar_dish_reload";
        }

        @Override
        public String description() {
            return "The radar's tile is replaced by one loaded from NBT: the radius survives and there is still exactly one dish. A dish killed as a chunk unload would is replaced by exactly one new one.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(8, 24);
            final World w = ctx.world();
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            DefenseScenarios.place(ctx, GregGunsMTEIDs.RADAR_EV, 0, CY, 0);
            final int[] stage = { 0 };
            final int[] mark = { 0 };
            final int[] id = { 0 };
            return ScenarioResult.pending(name(), 1500, ticks -> {
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                MTERadar r = DefenseScenarios.radar(DefenseScenarios.live(ctx, 0, CY, 0));
                if (r == null) {
                    return ScenarioResult.fail(name(), "the controller is gone");
                }
                int x = ctx.originX();
                int y = ctx.originY() + CY;
                int z = ctx.originZ();
                if (stage[0] == 0) {
                    if (!DevStructureBuilder.buildRadar(w, x, y, z, ItemList.Hatch_Energy_EV).formed) {
                        return ticks < 400 ? null : ScenarioResult.fail(name(), "the radar did not form");
                    }
                    r.setRadius(99);
                    stage[0] = 1;
                    mark[0] = ticks;
                    return null;
                }
                if (stage[0] == 1) {
                    DefenseScenarios.powerHatch(ctx, 0, CY, 0);
                    if (r.dish() == null) {
                        return ticks < mark[0] + 300 ? null : ScenarioResult.fail(name(), "no dish");
                    }
                    id[0] = r.dish()
                        .getEntityId();
                    out.checkEquals("one dish before the reload", 1, dishes(ctx, 0, 0));
                    TileEntity old = w.getTileEntity(x, y, z);
                    net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                    old.writeToNBT(tag);
                    old.invalidate();
                    TileEntity fresh = TileEntity.createAndLoadEntity(tag);
                    w.getChunkFromBlockCoords(x, z)
                        .addTileEntity(fresh);
                    stage[0] = 2;
                    mark[0] = ticks;
                    return null;
                }
                if (stage[0] == 2) {
                    DefenseScenarios.powerHatch(ctx, 0, CY, 0);
                    if (ticks < mark[0] + 150) {
                        return null;
                    }
                    out.checkEquals("radius after the reload", 99, r.protectedRadius());
                    out.checkEquals("still one dish after the reload", 1, dishes(ctx, 0, 0));
                    out.check(
                        r.dish() != null && r.dish()
                            .getEntityId() == id[0],
                        "it is the same dish");
                    // A chunk unload does not save the dish: kill it, and the controller makes one.
                    r.dish()
                        .setDead();
                    stage[0] = 3;
                    mark[0] = ticks;
                    return null;
                }
                DefenseScenarios.powerHatch(ctx, 0, CY, 0);
                if (r.dish() == null) {
                    return ticks < mark[0] + 200 ? null : ScenarioResult.fail(name(), "no new dish after the loss");
                }
                out.checkEquals("exactly one new dish", 1, dishes(ctx, 0, 0));
                out.check(
                    r.dish()
                        .getEntityId() != id[0],
                    "it is a new entity");
                return out.build();
            });
        }
    }
}
