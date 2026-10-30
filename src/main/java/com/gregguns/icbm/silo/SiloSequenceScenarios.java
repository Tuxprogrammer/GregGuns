package com.gregguns.icbm.silo;

import static com.gregguns.icbm.silo.SiloScenarios.CLEAR_HEIGHT;
import static com.gregguns.icbm.silo.SiloScenarios.WARMUP;
import static com.gregguns.icbm.silo.SiloScenarios.build;
import static com.gregguns.icbm.silo.SiloScenarios.busItems;
import static com.gregguns.icbm.silo.SiloScenarios.formed;
import static com.gregguns.icbm.silo.SiloScenarios.fuel;
import static com.gregguns.icbm.silo.SiloScenarios.gate;
import static com.gregguns.icbm.silo.SiloScenarios.hatchMb;
import static com.gregguns.icbm.silo.SiloScenarios.power;
import static com.gregguns.icbm.silo.SiloScenarios.putBus;
import static com.gregguns.icbm.silo.SiloScenarios.silo;
import static com.gregguns.icbm.silo.SiloScenarios.tile;
import static com.gregguns.icbm.silo.SiloScenarios.warhead;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.missile.BlockMissileBody;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.MissileBodies;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.missile.TileMissileBody;
import com.gregguns.icbm.warhead.BlockWarhead;
import com.gregguns.icbm.warhead.TileWarhead;
import com.gregguns.icbm.warhead.WarheadCasing;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;

/**
 * Scenarios of the silo's staged assembly (D285): each part stands in the shaft before its working cycle, in the order
 * body, warhead, fuel; parts may arrive in any order; no step moves a part without power; breaking a part or the silo
 * mid-sequence drops each part exactly once. The silo stands as in {@link SiloScenarios}. Dev environment only. Server
 * side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/missile-silo.md (The assembly sequence)
final class SiloSequenceScenarios {

    private SiloSequenceScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new InOrder());
        registry.register(new AnyOrder());
        registry.register(new BreakMidway());
    }

    // --- helpers ---------------------------------------------------------------------------------------------------

    private static int bx(ScenarioContext ctx) {
        return ctx.originX();
    }

    private static int by(ScenarioContext ctx) {
        return ctx.originY() + SiloScenarios.CY - SiloShape.FLOOR_SURFACE_BELOW_CONTROLLER;
    }

    private static int bz(ScenarioContext ctx) {
        return ctx.originZ() + SiloShape.SHAFT_CENTER_DEPTH;
    }

    /** @return whether a body block stands on the pad's center */
    static boolean bodyStands(ScenarioContext ctx) {
        return ctx.world()
            .getBlock(bx(ctx), by(ctx), bz(ctx)) instanceof BlockMissileBody;
    }

    /** @return whether a warhead block stands anywhere in the shaft's center column */
    static boolean warheadSeated(ScenarioContext ctx) {
        for (int y = by(ctx); y < by(ctx) + SiloShape.HEIGHT; y++) {
            if (ctx.world()
                .getBlock(bx(ctx), y, bz(ctx)) instanceof BlockWarhead) {
                return true;
            }
        }
        return false;
    }

    /** @return whether the warhead block sits right on the standing body's nose */
    private static boolean onTheNose(ScenarioContext ctx) {
        Block b = ctx.world()
            .getBlock(bx(ctx), by(ctx), bz(ctx));
        if (!(b instanceof BlockMissileBody)) {
            return false;
        }
        int noseY = by(ctx) + ((BlockMissileBody) b).missileTier()
            .heightBlocks();
        return ctx.world()
            .getBlock(bx(ctx), noseY, bz(ctx)) instanceof BlockWarhead;
    }

    /** Takes the standing body and any warhead in the column away without drops (a scenario reset, not a player). */
    static void removeBody(ScenarioContext ctx) {
        World w = ctx.world();
        for (int y = by(ctx) + SiloShape.HEIGHT; y >= by(ctx); y--) {
            TileEntity t = w.getTileEntity(bx(ctx), y, bz(ctx));
            if (t instanceof TileWarhead) {
                ((TileWarhead) t).setNoDrop();
                w.setBlockToAir(bx(ctx), y, bz(ctx));
            } else if (t instanceof TileMissileBody) {
                ((TileMissileBody) t).setNoDrop();
                w.setBlockToAir(bx(ctx), y, bz(ctx));
            }
        }
    }

    /** @return whether the controller's last check came to that status key, failure or success */
    static boolean status(MTEMissileSilo s, String key) {
        CheckRecipeResult r = s.getCheckRecipeResult();
        return SimpleCheckRecipeResult.ofFailure(key)
            .equals(r)
            || SimpleCheckRecipeResult.ofSuccess(key)
                .equals(r);
    }

    private static String statusOf(MTEMissileSilo s) {
        CheckRecipeResult r = s.getCheckRecipeResult();
        net.minecraft.nbt.NBTTagCompound tag = r.writeToNBT(new net.minecraft.nbt.NBTTagCompound());
        return r.getID() + tag;
    }

    private static AxisAlignedBB around(ScenarioContext ctx) {
        return AxisAlignedBB.getBoundingBox(
            bx(ctx) - 4,
            by(ctx) - 3,
            bz(ctx) - 4,
            bx(ctx) + 5,
            by(ctx) + SiloShape.HEIGHT + 2,
            bz(ctx) + 5);
    }

    /** @return {bodies, warheads} lying as items around the silo */
    private static int[] items(ScenarioContext ctx) {
        int[] n = new int[2];
        for (Object o : ctx.world()
            .getEntitiesWithinAABB(EntityItem.class, around(ctx))) {
            EntityItem e = (EntityItem) o;
            ItemStack s = e.getEntityItem();
            if (e.isDead || s == null) {
                continue;
            }
            if (MissileBodies.tierOf(s) != null) {
                n[0] += s.stackSize;
            } else if (Block.getBlockFromItem(s.getItem()) instanceof BlockWarhead) {
                n[1] += s.stackSize;
            }
        }
        return n;
    }

    private static void killItems(ScenarioContext ctx) {
        for (Object o : ctx.world()
            .getEntitiesWithinAABB(EntityItem.class, around(ctx))) {
            ((EntityItem) o).setDead();
        }
    }

    private static void drain(ScenarioContext ctx) {
        IGregTechTileEntity energy = tile(ctx, 0, -1, 0);
        if (energy != null) {
            energy.decreaseStoredEnergyUnits(Long.MAX_VALUE / 4, true);
        }
    }

    private static boolean active(MTEMissileSilo s) {
        return s.getBaseMetaTileEntity()
            .isActive();
    }

    private static ScenarioResult finish(ScenarioContext ctx, ScenarioResult.Builder out) {
        EntityMissile m = silo(ctx) == null ? null : silo(ctx).missile();
        if (m != null) {
            m.setDead();
        }
        removeBody(ctx);
        killItems(ctx);
        ctx.clearArea(14, CLEAR_HEIGHT);
        killItems(ctx);
        return out.build();
    }

    // --- scenarios -------------------------------------------------------------------------------------------------

    /**
     * The owner's sequence: a body alone goes on the pad and runs its cycle, then waits; a warhead with no power stays
     * in the bus; with power it is seated on the nose before its cycle; the cycle ends with the armed missile; fuel
     * comes last.
     */
    private static final class InOrder implements Scenario {

        @Override
        public String name() {
            return "icbm.silo.sequence";
        }

        @Override
        public String description() {
            return "A body alone in the Input Bus is stood on the pad, runs its working cycle and waits for a warhead; a warhead arriving with no power stays in the bus; with power it is seated on the body's nose before the mating cycle starts; the cycle ends with the armed missile in the shaft and no blocks left; fuel added last is pumped into it.";
        }

        @Override
        public int areaRadius() {
            return 14;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            build(ctx, ItemList.Hatch_Energy_LuV);
            int[] stage = { 0 };
            int[] mark = { 0 };
            return ScenarioResult.pending(name(), 2400, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                MTEMissileSilo s = silo(ctx);
                if (s == null) {
                    return ScenarioResult.fail(name(), "no silo controller");
                }
                switch (stage[0]) {
                    case 0:
                        out.check(formed(ctx), "the silo forms");
                        power(ctx);
                        putBus(ctx, MissileBodies.body(MissileTier.T1));
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        power(ctx);
                        if (!bodyStands(ctx)) {
                            return ticks > mark[0] + 100 ? ScenarioResult.fail(name(), "the body never went up") : null;
                        }
                        // The body is placed first, then its cycle runs.
                        out.checkEquals("the body left the bus", 0, busItems(ctx));
                        out.checkEquals("the body's cycle runs", SiloSequence.Stage.BODY, s.stage());
                        out.check(active(s), "the silo works while the body stands");
                        out.check(status(s, "gregguns_silo_body_cycle"), "status: setting up the body");
                        out.check(!warheadSeated(ctx), "no warhead yet");
                        out.check(s.missile() == null, "no missile yet");
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        power(ctx);
                        if (s.stage() != SiloSequence.Stage.BODY_DONE) {
                            return ticks > mark[0] + SiloRules.BODY_TICKS + 60
                                ? ScenarioResult.fail(name(), "the body cycle never ended: " + s.stage())
                                : null;
                        }
                        out.check(
                            ticks >= mark[0] + SiloRules.BODY_TICKS - 5,
                            "the body cycle ran its time (" + (ticks - mark[0]) + " ticks)");
                        mark[0] = ticks;
                        stage[0] = 3;
                        return null;
                    case 3:
                        power(ctx);
                        if (ticks < mark[0] + 60) {
                            return null;
                        }
                        out.check(bodyStands(ctx), "the body still stands, waiting");
                        out.check(!active(s), "the silo idles without a warhead");
                        out.check(status(s, "gregguns_silo_no_warhead"), "status: waiting for a warhead");
                        // No power: the warhead must stay in the bus.
                        drain(ctx);
                        putBus(ctx, warhead(WarheadCasing.T1, "condensed"));
                        mark[0] = ticks;
                        stage[0] = 4;
                        return null;
                    case 4:
                        drain(ctx);
                        if (ticks < mark[0] + 100) {
                            return null;
                        }
                        out.checkEquals("no power: the warhead stays in the bus", 1, busItems(ctx));
                        out.check(!warheadSeated(ctx), "no power: nothing is seated on the nose");
                        out.check(s.waitingForPower(), "no power: the silo waits for power");
                        out.checkEquals("no power: the silo never power-failed", 0, s.powerStops());
                        power(ctx);
                        mark[0] = ticks;
                        stage[0] = 5;
                        return null;
                    case 5:
                        power(ctx);
                        if (!warheadSeated(ctx)) {
                            return ticks > mark[0] + 100
                                ? ScenarioResult.fail(name(), "the warhead never went up: " + statusOf(s))
                                : null;
                        }
                        out.check(onTheNose(ctx), "the warhead sits on the body's nose");
                        out.checkEquals("the warhead left the bus", 0, busItems(ctx));
                        out.checkEquals("the mating cycle runs", SiloSequence.Stage.WARHEAD, s.stage());
                        out.check(status(s, "gregguns_silo_warhead_cycle"), "status: mating the warhead");
                        out.check(s.missile() == null, "the stack is not armed before its cycle ends");
                        mark[0] = ticks;
                        stage[0] = 6;
                        return null;
                    case 6:
                        power(ctx);
                        if (s.missile() == null) {
                            return ticks > mark[0] + SiloRules.WARHEAD_TICKS + 60
                                ? ScenarioResult.fail(name(), "the stack was never armed: " + statusOf(s))
                                : null;
                        }
                        out.check(
                            ticks >= mark[0] + SiloRules.WARHEAD_TICKS - 5,
                            "the mating cycle ran its time (" + (ticks - mark[0]) + " ticks)");
                        EntityMissile m = s.missile();
                        out.checkEquals("a T1 stands in the shaft", MissileTier.T1, m.tier());
                        out.checkEquals("it carries the warhead's explosive", "condensed", m.explosiveId());
                        out.checkEquals("its tank is empty", 0, m.fuelMb());
                        out.check(!bodyStands(ctx) && !warheadSeated(ctx), "the blocks became the missile");
                        out.checkEquals("no part dropped as an item", 0, items(ctx)[0] + items(ctx)[1]);
                        out.checkEquals("the sequence is over", SiloSequence.Stage.NONE, s.stage());
                        fuel(ctx, out, "nitrofuel");
                        mark[0] = ticks;
                        stage[0] = 7;
                        return null;
                    case 7:
                        power(ctx);
                        if (s.missile() == null || s.missile()
                            .fuelMb() <= 0) {
                            return ticks > mark[0] + 200 ? ScenarioResult.fail(name(), "the fuel was never pumped")
                                : null;
                        }
                        out.check(hatchMb(ctx) < 16000, "the fuel came from the Input Hatch");
                        out.check(status(s, "gregguns_silo_fueling"), "status: fueling");
                        return finish(ctx, out);
                    default:
                        return null;
                }
            });
        }
    }

    /** Warhead and fuel arrive before the body: they wait; the body still goes first. */
    private static final class AnyOrder implements Scenario {

        @Override
        public String name() {
            return "icbm.silo.sequence_any_order";
        }

        @Override
        public String description() {
            return "A warhead and fuel that arrive before the body wait (the warhead in the bus with the waiting-for-body status, the fuel in the hatch); when the body arrives it goes up first and the warhead stays in the bus through the body's cycle, then is seated and mated, and the fuel goes in last.";
        }

        @Override
        public int areaRadius() {
            return 14;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            build(ctx, ItemList.Hatch_Energy_LuV);
            int[] stage = { 0 };
            int[] mark = { 0 };
            return ScenarioResult.pending(name(), 2400, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                MTEMissileSilo s = silo(ctx);
                if (s == null) {
                    return ScenarioResult.fail(name(), "no silo controller");
                }
                power(ctx);
                switch (stage[0]) {
                    case 0:
                        out.check(formed(ctx), "the silo forms");
                        putBus(ctx, warhead(WarheadCasing.T1, "condensed"));
                        fuel(ctx, out, "nitrofuel");
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        if (ticks < mark[0] + 100) {
                            return null;
                        }
                        out.checkEquals("the warhead waits in the bus", 1, busItems(ctx));
                        out.check(!warheadSeated(ctx) && !bodyStands(ctx), "nothing stands in the shaft");
                        out.checkEquals("the fuel waits in the hatch", 16000, hatchMb(ctx));
                        out.check(status(s, "gregguns_silo_waiting_body"), "status: waiting for a missile body");
                        // The body arrives behind the warhead, in a later slot.
                        putBus(ctx, warhead(WarheadCasing.T1, "condensed"), MissileBodies.body(MissileTier.T1));
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        if (!bodyStands(ctx)) {
                            return ticks > mark[0] + 100 ? ScenarioResult.fail(name(), "the body never went up") : null;
                        }
                        out.check(!warheadSeated(ctx), "the body goes up first, alone");
                        out.checkEquals("the warhead stays in the bus", 1, busItems(ctx));
                        stage[0] = 3;
                        return null;
                    case 3:
                        if (s.stage() == SiloSequence.Stage.BODY) {
                            // All through the body's cycle the warhead stays in the bus.
                            if (warheadSeated(ctx)) {
                                return ScenarioResult.fail(name(), "the warhead went up during the body's cycle");
                            }
                            return ticks > mark[0] + SiloRules.BODY_TICKS + 100
                                ? ScenarioResult.fail(name(), "the body cycle never ended")
                                : null;
                        }
                        if (s.missile() == null) {
                            return ticks > mark[0] + SiloRules.BODY_TICKS + SiloRules.WARHEAD_TICKS + 200
                                ? ScenarioResult.fail(name(), "the missile was never armed: " + statusOf(s))
                                : null;
                        }
                        out.checkEquals("both parts left the bus", 0, busItems(ctx));
                        mark[0] = ticks;
                        stage[0] = 4;
                        return null;
                    case 4:
                        if (s.missile() == null || s.missile()
                            .fuelMb() <= 0) {
                            return ticks > mark[0] + 200 ? ScenarioResult.fail(name(), "the waiting fuel never went in")
                                : null;
                        }
                        out.check(hatchMb(ctx) < 16000, "the fuel that came first went in last");
                        return finish(ctx, out);
                    default:
                        return null;
                }
            });
        }
    }

    /**
     * Mid-sequence breaks: the body broken under a seated warhead drops the body once and the silo brings the warhead
     * down once; the controller broken mid-sequence leaves the parts standing, and breaking them drops each once.
     */
    private static final class BreakMidway implements Scenario {

        @Override
        public String name() {
            return "icbm.silo.sequence_break";
        }

        @Override
        public String description() {
            return "Breaking the body during the mating cycle stops the cycle, drops the body once and brings the seated warhead down as one item; nothing is armed and nothing more drops. Breaking the controller mid-sequence leaves the body and warhead standing in the shaft; breaking them drops each exactly once and the controller holds no parts.";
        }

        @Override
        public int areaRadius() {
            return 14;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (gate() != null) {
                return ScenarioResult.skip(name(), gate());
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            build(ctx, ItemList.Hatch_Energy_LuV);
            int[] stage = { 0 };
            int[] mark = { 0 };
            return ScenarioResult.pending(name(), 3000, ticks -> {
                if (ticks < WARMUP) {
                    return null;
                }
                World w = ctx.world();
                MTEMissileSilo s = silo(ctx);
                if (stage[0] < 4) {
                    power(ctx);
                    if (s == null) {
                        return ScenarioResult.fail(name(), "no silo controller");
                    }
                }
                switch (stage[0]) {
                    case 0:
                        out.check(formed(ctx), "the silo forms");
                        killItems(ctx);
                        putBus(ctx, MissileBodies.body(MissileTier.T1), warhead(WarheadCasing.T1, "condensed"));
                        mark[0] = ticks;
                        stage[0] = 1;
                        return null;
                    case 1:
                        if (s.stage() != SiloSequence.Stage.WARHEAD || !onTheNose(ctx)) {
                            return ticks > mark[0] + SiloRules.BODY_TICKS + 200
                                ? ScenarioResult.fail(name(), "the mating cycle never started")
                                : null;
                        }
                        // A player breaks the body under the seated warhead, mid-cycle.
                        w.setBlockToAir(bx(ctx), by(ctx), bz(ctx));
                        mark[0] = ticks;
                        stage[0] = 2;
                        return null;
                    case 2:
                        if (warheadSeated(ctx) || s.stage() != SiloSequence.Stage.NONE) {
                            return ticks > mark[0] + 60
                                ? ScenarioResult.fail(name(), "the silo never noticed the broken body")
                                : null;
                        }
                        out.check(!active(s), "the cycle stopped with its part gone");
                        mark[0] = ticks;
                        stage[0] = 3;
                        return null;
                    case 3:
                        if (ticks < mark[0] + SiloRules.WARHEAD_TICKS + 40) {
                            return null; // past where the mating cycle would have ended
                        }
                        out.check(s.missile() == null, "nothing was armed");
                        out.checkEquals("the body dropped exactly once", 1, items(ctx)[0]);
                        out.checkEquals("the warhead came down exactly once", 1, items(ctx)[1]);
                        out.checkEquals("the bus is empty: nothing came back twice", 0, busItems(ctx));
                        out.check(status(s, "gregguns_silo_no_body"), "status: no body");
                        killItems(ctx);
                        // Second run: the controller is broken while the stack stands.
                        putBus(ctx, MissileBodies.body(MissileTier.T1), warhead(WarheadCasing.T1, "condensed"));
                        mark[0] = ticks;
                        stage[0] = 4;
                        return null;
                    case 4:
                        power(ctx);
                        if (s == null || s.stage() != SiloSequence.Stage.WARHEAD || !onTheNose(ctx)) {
                            return ticks > mark[0] + SiloRules.BODY_TICKS + 200
                                ? ScenarioResult.fail(name(), "the second mating cycle never started")
                                : null;
                        }
                        w.setBlockToAir(ctx.originX(), ctx.originY() + SiloScenarios.CY, ctx.originZ());
                        mark[0] = ticks;
                        stage[0] = 5;
                        return null;
                    case 5:
                        if (ticks < mark[0] + 60) {
                            return null;
                        }
                        out.check(silo(ctx) == null, "the controller is gone");
                        out.check(bodyStands(ctx) && onTheNose(ctx), "the body and warhead still stand in the shaft");
                        out.checkEquals("nothing dropped yet", 0, items(ctx)[0] + items(ctx)[1]);
                        // The player takes the parts down: the warhead first, then the body.
                        for (int y = by(ctx) + SiloShape.HEIGHT; y > by(ctx); y--) {
                            if (w.getBlock(bx(ctx), y, bz(ctx)) instanceof BlockWarhead) {
                                w.setBlockToAir(bx(ctx), y, bz(ctx));
                            }
                        }
                        w.setBlockToAir(bx(ctx), by(ctx), bz(ctx));
                        mark[0] = ticks;
                        stage[0] = 6;
                        return null;
                    case 6:
                        if (ticks < mark[0] + 20) {
                            return null;
                        }
                        out.checkEquals("the body dropped exactly once", 1, items(ctx)[0]);
                        out.checkEquals("the warhead dropped exactly once", 1, items(ctx)[1]);
                        return finish(ctx, out);
                    default:
                        return null;
                }
            });
        }
    }
}
