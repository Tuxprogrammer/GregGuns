package com.gregguns.icbm.missile;

import java.util.Collections;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
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
 * Headless scenarios of the modeled radar (D125): an unformed controller is inert, the dish stands once when the radar
 * forms, spins while it runs, stops without power and goes when the
 * structure breaks, and a reload never leaves two. They live in the missile package for the shared placing helpers
 * of {@link DefenseScenarios}. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (The small radar, The dish)
final class RadarDishScenarios {

    private static final int WARMUP_TICKS = 50;
    private static final int CY = 5;
    /** GT's multiblock start-up delay (mStartUpCheck), ticks. */
    private static final int STARTUP_TICKS = 100;

    private RadarDishScenarios() {}

    /** @param registry where to add the scenarios */
    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Unformed());
        registry.register(new Lifecycle());
        registry.register(new Reload(false));
        registry.register(new Reload(true));
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
            ((gregtech.api.metatileentity.BaseMetaTileEntity) hatch).setStoredEU(0L);
        }
    }

    /** A placed controller with no structure does nothing (an unformed controller is inert). */
    private static final class Unformed extends Base {

        @Override
        public String name() {
            return "icbm.defense.radar_unformed";
        }

        @Override
        public String description() {
            return "A placed Radar (HV) controller with no structure is inert: it is not formed, not online, reports OFF with an empty sweep and no redstone (read from a lamp in front, as the world sees it), and no dish stands. A data stick in its slot 0, as an I4 single-block radar saved it, is handed back as an item instead of being destroyed.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(8, 24);
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final World w = ctx.world();
            DefenseScenarios.place(ctx, GregGunsMTEIDs.RADAR_HV, 0, CY, 0);
            // The controller faces north; a lamp in front shows what the world sees.
            ctx.setBlock(0, CY, -1, Blocks.redstone_lamp, 0);
            // An I4 tile as an old world saved it: the single-block radar kept its data stick in slot 0. Round-trip the
            // tile through NBT so the stick arrives the way a chunk load delivers it, before the first tick.
            int x = ctx.originX();
            int y = ctx.originY() + CY;
            int z = ctx.originZ();
            MTERadar before = DefenseScenarios.radar(DefenseScenarios.live(ctx, 0, CY, 0));
            if (before == null) {
                return ScenarioResult.fail(name(), "no radar controller was placed");
            }
            before.mInventory[0] = ItemList.Tool_DataStick.get(1);
            TileEntity old = w.getTileEntity(x, y, z);
            NBTTagCompound tag = new NBTTagCompound();
            old.writeToNBT(tag);
            old.invalidate();
            w.getChunkFromBlockCoords(x, z)
                .addTileEntity(TileEntity.createAndLoadEntity(tag));
            final boolean[] stickHeld = {
                DefenseScenarios.radar(DefenseScenarios.live(ctx, 0, CY, 0)).mInventory[0] != null };
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
                out.checkEquals("no redstone from the controller", 0, r.signal());
                out.checkEquals("the world sees no power in front", 0, DefenseScenarios.neighborPower(ctx, 0, CY, -1));
                out.checkEquals("no dish", 0, dishes(ctx, 0, 0));
                out.check(stickHeld[0], "the reloaded tile held the data stick, as an I4 save does");
                out.check(r.mInventory[0] == null, "the controller no longer holds the stick");
                int sticks = 0;
                AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x - 3, y - 3, z - 3, x + 4, y + 4, z + 4);
                for (Object o : w.getEntitiesWithinAABB(EntityItem.class, box)) {
                    EntityItem item = (EntityItem) o;
                    if (ItemList.Tool_DataStick.isStackEqual(item.getEntityItem(), false, true)) {
                        sticks++;
                    }
                    item.setDead();
                }
                out.checkEquals("the stick was handed back as an item", 1, sticks);
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
            return "A formed radar has exactly one dish; it is still without power (checked after GT's start-up delay), spins with it, stops when the power goes, spins again by itself when it returns, and is gone once a casing is broken.";
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
                    // Formed, no power: the dish stands but is still. GT's controller waits 100 ticks after it loads
                    // before it starts judging the machine, so "not online" only means something after that.
                    if (r.dish() == null) {
                        return ticks < mark[0] + 200 ? null : ScenarioResult.fail(name(), "no dish after forming");
                    }
                    if (ticks < mark[0] + STARTUP_TICKS + 10) {
                        return null;
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
                        IGregTechTileEntity hb = DefenseScenarios.live(ctx, 0, CY - 1, 0);
                        return ticks < mark[0] + 300 ? null
                            : ScenarioResult.fail(
                                name(),
                                "never stopped without EU: online " + r.online()
                                    + " dish "
                                    + r.dish()
                                    + " eu "
                                    + (hb == null ? -1 : hb.getStoredEU())
                                    + " active "
                                    + DefenseScenarios.live(ctx, 0, CY, 0)
                                        .isActive());
                    }
                    out.check(true, "the dish stops when the power goes");
                    stage[0] = 5;
                    mark[0] = ticks;
                    return null;
                }
                if (stage[0] == 5) {
                    // GT turns a machine off on power loss and leaves it off; the radar turns itself back on (D195).
                    DefenseScenarios.powerHatch(ctx, 0, CY, 0);
                    if (!(r.online() && r.dish() != null
                        && r.dish()
                            .spin()
                            .spinning())) {
                        return ticks < mark[0] + 400 ? null
                            : ScenarioResult.fail(name(), "did not restart by itself when the power came back");
                    }
                    out.check(true, "the radar restarts and the dish spins again when the power returns");
                    // Break a corner of the base, then recheck the structure.
                    w.setBlockToAir(x + 1, y - 1, z + 2);
                    DevStructureBuilder.check(w, x, y, z);
                    stage[0] = 6;
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

    /**
     * A reload keeps one dish; a dish lost the way a chunk unload loses it (removed from the world, not killed) is
     * replaced by exactly one. After the tile reload there is no dish for about five seconds, until GT's next
     * structure check forms the controller again, so the waits below are long.
     */
    private static final class Reload extends Base {

        /** Whether the dish stands in the chunk after the controller's (the dish is one block behind it). */
        private final boolean edge;

        Reload(boolean edge) {
            this.edge = edge;
        }

        @Override
        public String name() {
            return edge ? "icbm.defense.radar_dish_reload_edge" : "icbm.defense.radar_dish_reload";
        }

        @Override
        public String description() {
            return "The radar's tile is replaced by one loaded from NBT: the radius survives and there is still exactly one dish. A dish unloaded like a chunk unload does (removed from the world, still not dead) is replaced by exactly one new one"
                + (edge ? ", with the dish standing across a chunk edge from the controller." : ".");
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(edge ? 12 : 8, 24);
            final World w = ctx.world();
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            // The controller faces north, so its dish stands one block south of it: put the controller on the last
            // row of a chunk and the dish lands in the next one.
            int dz = 0;
            if (edge) {
                dz = (15 - (ctx.originZ() & 15)) & 15;
                if (dz > 6) {
                    dz -= 16;
                }
            }
            final int dzr = dz;
            DefenseScenarios.place(ctx, GregGunsMTEIDs.RADAR_EV, 0, CY, dzr);
            final int[] stage = { 0 };
            final int[] mark = { 0 };
            final int[] id = { 0 };
            final EntityRadarDish[] gone = { null };
            return ScenarioResult.pending(name(), 1500, ticks -> {
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                MTERadar r = DefenseScenarios.radar(DefenseScenarios.live(ctx, 0, CY, dzr));
                if (r == null) {
                    return ScenarioResult.fail(name(), "the controller is gone");
                }
                int x = ctx.originX();
                int y = ctx.originY() + CY;
                int z = ctx.originZ() + dzr;
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
                    DefenseScenarios.powerHatch(ctx, 0, CY, dzr);
                    if (r.dish() == null) {
                        return ticks < mark[0] + 300 ? null : ScenarioResult.fail(name(), "no dish");
                    }
                    id[0] = r.dish()
                        .getEntityId();
                    if (edge) {
                        out.check(
                            (((int) Math.floor(r.dish().posZ)) >> 4) != (z >> 4),
                            "the dish stands in another chunk than its controller");
                    }
                    out.checkEquals("one dish before the reload", 1, dishes(ctx, 0, dzr));
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
                    DefenseScenarios.powerHatch(ctx, 0, CY, dzr);
                    if (ticks < mark[0] + 150) {
                        return null;
                    }
                    out.checkEquals("radius after the reload", 99, r.protectedRadius());
                    out.checkEquals("still one dish after the reload", 1, dishes(ctx, 0, dzr));
                    // The old dish may go (its controller read as unformed for a moment) and be replaced: one is the
                    // rule.
                    if (r.dish() == null) {
                        return ticks < mark[0] + 300 ? null : ScenarioResult.fail(name(), "no dish after the reload");
                    }
                    // A chunk unload removes the dish from the world without killing it (and never saves it): do the
                    // same, and the controller must notice and make one.
                    gone[0] = r.dish();
                    w.unloadEntities(Collections.singletonList(gone[0]));
                    stage[0] = 3;
                    mark[0] = ticks;
                    return null;
                }
                DefenseScenarios.powerHatch(ctx, 0, CY, dzr);
                if (stage[0] == 3) {
                    // The world drops the entity on its next tick; until then there is nothing to test.
                    if (w.getEntityByID(gone[0].getEntityId()) == gone[0]) {
                        return ticks < mark[0] + 20 ? null : ScenarioResult.fail(name(), "the dish never unloaded");
                    }
                    out.check(!gone[0].isDead, "the unloaded dish is not dead, as after a chunk unload");
                    stage[0] = 4;
                    mark[0] = ticks;
                    return null;
                }
                if (r.dish() == null || r.dish() == gone[0]) {
                    return ticks < mark[0] + 200 ? null : ScenarioResult.fail(name(), "no new dish after the loss");
                }
                out.checkEquals("exactly one new dish", 1, dishes(ctx, 0, dzr));
                out.check(
                    r.dish()
                        .getEntityId() != id[0],
                    "it is a new entity");
                return out.build();
            });
        }
    }
}
