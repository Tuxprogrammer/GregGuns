package com.gregguns.icbm.missile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.IcbmAvailability;
import com.gregguns.icbm.warhead.TileWarhead;
import com.gregguns.icbm.warhead.WarheadBlocks;
import com.gregguns.icbm.warhead.WarheadCasing;
import com.gregguns.icbm.warhead.WarheadStacks;

/**
 * Scenarios of the missiles: stacking and arming, the fuel rules, a launch that detonates on target, the chunk
 * tickets, the launch rules, the save round trip, and the pad (in {@link MissileGcScenarios}). Dev environment only.
 * Server side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/ (all sheets)
public final class MissileScenarios {

    private MissileScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new StackAndArm());
        registry.register(new FuelRules());
        registry.register(new LaunchRules());
        registry.register(new LaunchAndDetonate(MissileTier.T1));
        registry.register(new LaunchAndDetonate(MissileTier.T2));
        registry.register(new ChunkTickets());
        registry.register(new SaveRoundTrip());
        registry.register(new Timeout());
        registry.register(new RestartAdopt());
        registry.register(new TargetHeight());
        registry.register(new ShotDown());
        registry.register(new PadSurvives());
        registry.register(new MissileShotsScenario());
        registry.register(new MissileShotsScenario(true));
        registry.register(new MissileMpScenario());
        I3ItemShotsScenarios.registerAll(registry);
    }

    static boolean moduleOff() {
        return !MissileBlocks.registered();
    }

    /** Builds a body (with its fillers) and a warhead on its nose; returns the base position. */
    static int[] buildStack(ScenarioContext ctx, MissileTier tier, WarheadCasing casing, String explosive, int fill,
        int dx, int dy, int dz, boolean onPad, int guidance) {
        ctx.setBlock(dx, dy, dz, MissileBlocks.body(tier), 0);
        World w = ctx.world();
        int x = ctx.originX() + dx;
        int y = ctx.originY() + dy;
        int z = ctx.originZ() + dz;
        TileEntity t = w.getTileEntity(x, y, z);
        if (t instanceof TileMissileBody) {
            ((TileMissileBody) t).init(guidance, onPad, null);
        }
        for (int i = 1; i < tier.heightBlocks(); i++) {
            ctx.setBlock(dx, dy + i, dz, MissileBlocks.filler(), 0);
            TileEntity f = w.getTileEntity(x, y + i, z);
            if (f instanceof com.gregguns.icbm.warhead.TileWarheadFiller) {
                ((com.gregguns.icbm.warhead.TileWarheadFiller) f).setOffset(0, -i, 0);
            }
        }
        if (casing != null) {
            ctx.setBlock(dx, dy + tier.heightBlocks(), dz, WarheadBlocks.of(casing), 0);
            TileEntity wt = w.getTileEntity(x, y + tier.heightBlocks(), z);
            if (wt instanceof TileWarhead) {
                ItemStack s = WarheadStacks
                    .make(Item.getItemFromBlock(WarheadBlocks.of(casing)), casing, explosive, fill);
                ((TileWarhead) wt).init(casing, s.getTagCompound(), null);
            }
        }
        return new int[] { x, y, z };
    }

    /** A stone floor under the stack, so a non-pad body has something to stand on. */
    static void floor(ScenarioContext ctx, int radius) {
        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                ctx.setBlock(a, -1, b, Blocks.stone, 0);
            }
        }
    }

    /** Kills every missile entity near the origin: arming spawns entities the context does not track. */
    static void sweep(ScenarioContext ctx) {
        World w = ctx.world();
        net.minecraft.util.AxisAlignedBB box = net.minecraft.util.AxisAlignedBB
            .getBoundingBox(ctx.originX() - 100, 0, ctx.originZ() - 100, ctx.originX() + 100, 255, ctx.originZ() + 300);
        for (Object o : new ArrayList<Object>(w.getEntitiesWithinAABB(EntityMissile.class, box))) {
            EntityMissile m = (EntityMissile) o;
            m.tickets()
                .release();
            m.setDead();
        }
    }

    /** Arms a fresh stack at the origin and returns the missile entity, or null. */
    static EntityMissile armed(ScenarioContext ctx, MissileTier tier, int guidance) {
        return armedAt(ctx, tier, guidance, 0);
    }

    /** Arms a fresh stack {@code dx} blocks east of the origin and returns the missile entity, or null. */
    static EntityMissile armedAt(ScenarioContext ctx, MissileTier tier, int guidance, int dx) {
        floor(ctx, 5);
        WarheadCasing c = tier == MissileTier.T1 ? WarheadCasing.T1 : WarheadCasing.T2;
        String ex = tier == MissileTier.T1 ? "condensed" : "thermobaric";
        int[] base = buildStack(ctx, tier, c, ex, 3, dx, 0, 0, false, guidance);
        return MissileStacks.armIntoEntity(ctx.world(), base[0], base[1], base[2], null).missile;
    }

    /** Logs where a missile is and what holds its chunks, every 20 ticks, but only while it is in a bad state. */
    static void trace(World w, EntityMissile m, int ticks) {
        if (ticks % 20 != 0 || m.isDead) {
            return;
        }
        int cx = net.minecraft.util.MathHelper.floor_double(m.posX) >> 4;
        int cz = net.minecraft.util.MathHelper.floor_double(m.posZ) >> 4;
        boolean forced = net.minecraftforge.common.ForgeChunkManager.getPersistentChunksFor(w)
            .containsKey(new net.minecraft.world.ChunkCoordIntPair(cx, cz));
        if (w.getChunkProvider()
            .chunkExists(cx, cz) && m.addedToChunk) {
            return;
        }
        com.gregguns.GregGuns.LOG.info(
            "[missile trace] t={} phase={} pos={},{},{} chunk={},{} loaded={} forced={} added={} held={} dead={}",
            ticks,
            m.phase(),
            (int) m.posX,
            (int) m.posY,
            (int) m.posZ,
            cx,
            cz,
            w.getChunkProvider()
                .chunkExists(cx, cz),
            forced,
            m.addedToChunk,
            m.tickets()
                .heldChunks(),
            m.isDead);
    }

    private abstract static class Base implements Scenario {

        ScenarioResult needsModule() {
            return moduleOff() ? ScenarioResult.fail(name(), "the missile content is not registered") : null;
        }
    }

    /** A body, fillers and a warhead stack up and arm into one entity; bad stacks are refused. */
    private static final class StackAndArm extends Base {

        @Override
        public String name() {
            return "icbm.missile.stack_and_arm";
        }

        @Override
        public String description() {
            return "Body plus warhead arm into a missile entity of the same tier; a missing or too heavy warhead is refused.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            sweep(ctx);
            ctx.clearArea(8, 20);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World w = ctx.world();
            floor(ctx, 8);
            // 1. No warhead.
            int[] a = buildStack(ctx, MissileTier.T1, null, null, 0, 0, 0, 0, false, 0);
            out.checkEquals(
                "no warhead",
                MissileStacks.ArmResult.NO_WARHEAD,
                MissileStacks.arm(w, a[0], a[1], a[2], null));
            // 2. A T2 warhead on a T1 body.
            int[] b = buildStack(ctx, MissileTier.T1, WarheadCasing.T2, "thermobaric", 5, 4, 0, 0, false, 0);
            out.checkEquals(
                "T2 warhead is too heavy for a T1 body",
                MissileStacks.ArmResult.WRONG_WARHEAD,
                MissileStacks.arm(w, b[0], b[1], b[2], null));
            // 3. A broken filler cell.
            // A body missing a filler cell cannot come from play (breaking any cell breaks the whole body), only from a
            // damaged save or a command: build it that way.
            int[] c = buildStack(ctx, MissileTier.T2, WarheadCasing.T2, "thermobaric", 5, -4, 0, 0, false, 0);
            w.setBlock(c[0], c[1] + 1, c[2], Blocks.air, 0, 2);
            // The filler's own break rule took the body down with it; that is the designed behavior, so check it.
            out.checkEquals("breaking a filler breaks the whole body", Blocks.air, w.getBlock(c[0], c[1], c[2]));
            out.checkEquals(
                "nothing left to arm",
                MissileStacks.ArmResult.NOT_BODY,
                MissileStacks.arm(w, c[0], c[1], c[2], null));
            // 4. Good stacks: T1 with a T1 warhead, T2 body with a T1 warhead (lighter fits).
            int[] d = buildStack(ctx, MissileTier.T1, WarheadCasing.T1, "condensed", 3, 0, 0, 4, false, 2);
            MissileStacks.Armed armed = MissileStacks.armIntoEntity(w, d[0], d[1], d[2], null);
            out.checkEquals("T1 arms", MissileStacks.ArmResult.OK, armed.result);
            EntityMissile m = armed.missile;
            out.check(m instanceof EntityMissileT1, "a T1 missile entity stands where the stack was");
            if (m != null) {
                out.checkEquals(
                    "explosive carried over",
                    "condensed",
                    m.status()
                        .explosive());
                out.checkEquals(
                    "fill carried over",
                    3,
                    m.status()
                        .fill());
                out.checkEquals(
                    "guidance carried over",
                    2,
                    m.status()
                        .guidance());
                out.check(
                    m.onPad() && !m.status()
                        .fueled(),
                    "unlaunched and not fueled");
                out.checkEquals("the body block is gone", Blocks.air, w.getBlock(d[0], d[1], d[2]));
                out.checkEquals("the warhead block is gone", Blocks.air, w.getBlock(d[0], d[1] + 3, d[2]));
                out.checkEquals("the filler is gone", Blocks.air, w.getBlock(d[0], d[1] + 1, d[2]));
                // Taking it apart returns the items.
                m.disarm();
                out.check(m.isDead, "disarm removes the entity");
                int items = 0;
                for (Object o : w.loadedEntityList) {
                    if (o instanceof net.minecraft.entity.item.EntityItem) {
                        ItemStack s = ((net.minecraft.entity.item.EntityItem) o).getEntityItem();
                        if (MissileBodies.tierOf(s) == MissileTier.T1 && MissileBodies.guidanceOf(s) == 2) {
                            items++;
                        }
                    }
                }
                out.check(items >= 1, "disarm drops the body with its guidance");
            }
            return out.build();
        }
    }

    /** The tank, fixed per tier; and (with Galacticraft) the fuel lists that gate the loader. */
    private static final class FuelRules extends Base {

        @Override
        public String name() {
            return "icbm.missile.fuel_rules";
        }

        @Override
        public String description() {
            return "The tank is fixed per tier, takes fuel only on the pad, and Galacticraft's lists gate each tier.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            sweep(ctx);
            ctx.clearArea(6, 20);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            EntityMissile m1 = armed(ctx, MissileTier.T1, 0);
            if (m1 == null) {
                return ScenarioResult.fail(name(), "could not arm a T1");
            }
            out.checkEquals("T1 tank", 2000, m1.capacityMb());
            out.checkEquals("first pour", 500, m1.fill(500, true));
            out.checkEquals("the tank takes only what fits", 1500, m1.fill(5000, true));
            out.checkEquals("a full tank takes nothing", 0, m1.fill(100, true));
            out.check(
                m1.status()
                    .fueled(),
                "the status says fueled");
            out.checkEquals("the missile is not drainable", null, removeFuel(m1));
            if (IcbmAvailability.galacticraftPresent()) {
                MissileGcScenarios.fuelLists(out, ctx, m1);
            } else {
                out.note("Galacticraft absent: fuel list checks skipped (D46)");
            }
            m1.disarm();
            return out.build();
        }

        private static Object removeFuel(EntityMissile m) {
            return IcbmAvailability.galacticraftPresent() ? MissileGcScenarios.removeFuel(m) : null;
        }
    }

    /** Every refusal of the launch, through the pad API. */
    private static final class LaunchRules extends Base {

        @Override
        public String name() {
            return "icbm.missile.launch_rules";
        }

        @Override
        public String description() {
            return "Launch is refused with no missile, no fuel, too close, too far, a bad target, past the in-flight cap, under a roof and without a chunk ticket; a stack that is not armed.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            sweep(ctx);
            ctx.clearArea(6, 20);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World w = ctx.world();
            int px = ctx.originX();
            int py = ctx.originY();
            int pz = ctx.originZ();
            out.checkEquals(
                "nothing on the pad",
                LaunchResult.NO_MISSILE,
                PadMissiles.launch(w, px, py, pz, px + 100, pz, null));
            floor(ctx, 5);
            buildStack(ctx, MissileTier.T1, WarheadCasing.T1, "condensed", 3, 0, 0, 0, false, 0);
            out.checkEquals(
                "stacked, not armed",
                LaunchResult.NOT_ARMED,
                PadMissiles.launch(w, px, py, pz, px + 100, pz, null));
            MissileStatus unarmed = PadMissiles.status(w, px, py, pz);
            out.check(unarmed.present() && !unarmed.armed(), "status sees a body that is not armed");
            EntityMissile m = armed(ctx, MissileTier.T1, 0);
            if (m == null) {
                return ScenarioResult.fail(name(), "could not arm a T1");
            }
            out.check(
                PadMissiles.status(w, px, py, pz)
                    .armed(),
                "status sees the missile");
            out.checkEquals(
                "not fueled",
                LaunchResult.NOT_FUELED,
                PadMissiles.launch(w, px, py, pz, px + 100, pz, null));
            m.fill(m.capacityMb(), true);
            out.checkEquals("too close", LaunchResult.TOO_CLOSE, PadMissiles.launch(w, px, py, pz, px + 5, pz, null));
            out.checkEquals(
                "too far",
                LaunchResult.OUT_OF_RANGE,
                PadMissiles.launch(w, px, py, pz, px + m.maxRange() + 50, pz, null));
            out.checkEquals(
                "NaN target",
                LaunchResult.BAD_TARGET,
                PadMissiles.launch(w, px, py, pz, Double.NaN, pz, null));
            int cap = IcbmConfig.maxMissilesInFlight;
            IcbmConfig.maxMissilesInFlight = 0;
            try {
                out.checkEquals(
                    "over the in-flight cap",
                    LaunchResult.OVER_CAP,
                    PadMissiles.launch(w, px, py, pz, px + 100, pz, null));
            } finally {
                IcbmConfig.maxMissilesInFlight = cap;
            }
            // A roof inside the climb column refuses the launch (ICBM-04-26).
            ctx.setBlock(0, 8, 0, Blocks.stone, 0);
            out.checkEquals(
                "a roof over the pad",
                LaunchResult.BLOCKED,
                PadMissiles.launch(w, px, py, pz, px + 100, pz, null));
            ctx.setBlock(0, 8, 0, Blocks.air, 0);
            // Forge out of tickets: the launch is refused and the missile keeps nothing.
            List<net.minecraftforge.common.ForgeChunkManager.Ticket> hoard = new ArrayList<>();
            for (int i = 0; i < 2000; i++) {
                net.minecraftforge.common.ForgeChunkManager.Ticket t = net.minecraftforge.common.ForgeChunkManager
                    .requestTicket(
                        com.gregguns.GregGuns.instance,
                        w,
                        net.minecraftforge.common.ForgeChunkManager.Type.NORMAL);
                if (t == null) {
                    break;
                }
                hoard.add(t);
            }
            try {
                out.checkEquals(
                    "no chunk ticket left",
                    LaunchResult.NO_TICKET,
                    PadMissiles.launch(w, px, py, pz, px + 100, pz, null));
            } finally {
                for (net.minecraftforge.common.ForgeChunkManager.Ticket t : hoard) {
                    net.minecraftforge.common.ForgeChunkManager.releaseTicket(t);
                }
            }
            out.checkEquals(
                "a refused launch holds no ticket",
                0,
                m.tickets()
                    .tickets());
            out.check(m.onPad(), "every refusal leaves the missile on the pad");
            out.check(
                PadMissiles.expectedRadius(w, px, py, pz, px + 25, pz)
                    > PadMissiles.expectedRadius(w, px, py, pz, px + 900, pz),
                "a closer target gets the bigger fuel bonus (D20)");
            m.disarm();
            return out.build();
        }
    }

    /** Lays a stone platform around a target and counts what a blast removes. */
    static final class Target {

        final int dx;
        final int dz;
        final int radius;
        final List<int[]> stones = new ArrayList<>();

        Target(ScenarioContext ctx, int dx, int dz, int radius) {
            this.dx = dx;
            this.dz = dz;
            this.radius = radius;
            for (int a = -radius; a <= radius; a++) {
                for (int b = -radius; b <= radius; b++) {
                    ctx.setBlock(dx + a, -1, dz + b, Blocks.stone, 0);
                    stones.add(new int[] { ctx.originX() + dx + a, ctx.originY() - 1, ctx.originZ() + dz + b });
                }
            }
        }

        int removed(World w) {
            int n = 0;
            for (int[] s : stones) {
                if (w.getBlock(s[0], s[1], s[2]) != Blocks.stone) {
                    n++;
                }
            }
            return n;
        }
    }

    /** Build, stack, fuel, launch, fly and detonate on target (exit box 1), with tier-by-tier checks. */
    private static final class LaunchAndDetonate extends Base {

        private final MissileTier tier;

        LaunchAndDetonate(MissileTier tier) {
            this.tier = tier;
        }

        @Override
        public String name() {
            return "icbm.missile.launch_detonate_t" + tier.number();
        }

        @Override
        public String description() {
            return "A T" + tier.number() + " missile launches from a stack, flies its arc and detonates on its target.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            sweep(ctx);
            ctx.clearArea(8, 20);
            boolean terrain = IcbmConfig.terrainDamage(tier.number());
            setTerrain(true);
            EntityMissile m = armed(ctx, tier, MissileBodies.MAX_GUIDANCE);
            if (m == null) {
                setTerrain(terrain);
                return ScenarioResult.fail(name(), "could not arm");
            }
            m.fill(m.capacityMb(), true);
            World w = ctx.world();
            Target target = new Target(ctx, 0, 70, 9);
            int px = ctx.originX();
            int py = ctx.originY();
            int pz = ctx.originZ();
            EntityPlayer player = ctx.player();
            LaunchResult r = PadMissiles.launch(w, px, py, pz, px + 0.5, pz + 70.5, player);
            if (r != LaunchResult.OK) {
                setTerrain(terrain);
                return ScenarioResult.fail(name(), "launch refused: " + r);
            }
            final boolean[] flew = { false };
            final double[] maxY = { 0 };
            return ScenarioResult.pending(name(), 900, ticks -> {
                trace(w, m, ticks);
                if (!m.isDead) {
                    flew[0] |= m.phase() == FlightPhase.ARC;
                    maxY[0] = Math.max(maxY[0], m.posY);
                    if (ticks >= 880) {
                        setTerrain(terrain);
                        return ScenarioResult.fail(
                            name(),
                            "still flying after " + ticks
                                + " ticks: phase "
                                + m.phase()
                                + " at "
                                + (int) m.posX
                                + ", "
                                + (int) m.posY
                                + ", "
                                + (int) m.posZ
                                + " aimed at "
                                + java.util.Arrays.toString(m.aimPoint()));
                    }
                    return null;
                }
                // The missile is gone: the blast now runs over a few ticks; wait for terrain to break.
                if (target.removed(w) == 0 && ticks < 880) {
                    return null;
                }
                setTerrain(terrain);
                if (target.removed(w) == 0) {
                    return ScenarioResult.fail(
                        name(),
                        "the missile ended but broke nothing; last aim " + java.util.Arrays.toString(m.aimPoint())
                            + " y "
                            + maxY[0]);
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(flew[0], "it reached the arc phase");
                out.check(maxY[0] > py + 20, "it climbed well above the pad (" + maxY[0] + ")");
                out.check(target.removed(w) > 0, "the blast broke blocks on the target platform");
                out.checkEquals(
                    "no tickets after impact",
                    0,
                    m.tickets()
                        .tickets());
                out.checkEquals(
                    "no chunks after impact",
                    0,
                    m.tickets()
                        .heldChunks());
                return out.build();
            });
        }

        private void setTerrain(boolean on) {
            switch (tier) {
                case T1:
                    IcbmConfig.terrainDamageT1 = on;
                    break;
                default:
                    IcbmConfig.terrainDamageT2 = on;
                    break;
            }
        }
    }

    /** Never more than two chunks and one ticket in flight; all released on impact, timeout and unload (D25). */
    private static final class ChunkTickets extends Base {

        @Override
        public String name() {
            return "icbm.missile.chunk_tickets";
        }

        @Override
        public String description() {
            return "A flying missile holds at most two chunks on one ticket, and releases all on impact, timeout and unload.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            sweep(ctx);
            ctx.clearArea(8, 20);
            EntityMissile m = armed(ctx, MissileTier.T1, MissileBodies.MAX_GUIDANCE);
            if (m == null) {
                return ScenarioResult.fail(name(), "could not arm");
            }
            m.fill(m.capacityMb(), true);
            World w = ctx.world();
            Target target = new Target(ctx, 0, 200, 9);
            boolean terrain = IcbmConfig.terrainDamageT1;
            IcbmConfig.terrainDamageT1 = false;
            LaunchResult r = PadMissiles.launch(
                w,
                ctx.originX(),
                ctx.originY(),
                ctx.originZ(),
                ctx.originX() + 0.5,
                ctx.originZ() + 200.5,
                null);
            if (r != LaunchResult.OK) {
                IcbmConfig.terrainDamageT1 = terrain;
                return ScenarioResult.fail(name(), "launch refused: " + r);
            }
            final int[] maxChunks = { 0 };
            final int[] maxTickets = { 0 };
            final java.util.Set<String> seen = new java.util.HashSet<>();
            final boolean[] midFlightDone = { false };
            final boolean[] keptByCallback = { false };
            return ScenarioResult.pending(name(), 1200, ticks -> {
                trace(w, m, ticks);
                if (!m.isDead) {
                    maxChunks[0] = Math.max(
                        maxChunks[0],
                        m.tickets()
                            .heldChunks());
                    maxTickets[0] = Math.max(
                        maxTickets[0],
                        m.tickets()
                            .tickets());
                    if (m.tickets()
                        .ticket() != null) {
                        seen.add(
                            m.tickets()
                                .ticket()
                                .getChunkList()
                                .toString());
                    }
                    if (!midFlightDone[0] && m.phase() == FlightPhase.ARC && m.posZ > ctx.originZ() + 100) {
                        // Unload: the world hands the ticket back on load; a live flight keeps it (adopt).
                        midFlightDone[0] = true;
                        List<net.minecraftforge.common.ForgeChunkManager.Ticket> loaded = new ArrayList<>();
                        loaded.add(
                            m.tickets()
                                .ticket());
                        MissileTickets.onTicketsLoaded(loaded, w);
                        keptByCallback[0] = m.tickets()
                            .ticket() != null
                            && m.tickets()
                                .heldChunks() >= 1;
                    }
                    if (ticks >= 1150) {
                        IcbmConfig.terrainDamageT1 = terrain;
                        return ScenarioResult.fail(
                            name(),
                            "still flying after " + ticks
                                + " ticks: phase "
                                + m.phase()
                                + " at "
                                + (int) m.posX
                                + ", "
                                + (int) m.posY
                                + ", "
                                + (int) m.posZ);
                    }
                    return null;
                }
                IcbmConfig.terrainDamageT1 = terrain;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(
                    maxChunks[0] >= 1 && maxChunks[0] <= 2,
                    "never more than two chunks held (" + maxChunks[0] + ")");
                out.check(maxTickets[0] == 1, "exactly one ticket while flying (" + maxTickets[0] + ")");
                out.check(keptByCallback[0], "the load callback handed a live missile its ticket back");
                out.check(midFlightDone[0], "the flight reached the test point");
                out.checkEquals(
                    "tickets after the end",
                    0,
                    m.tickets()
                        .tickets());
                out.checkEquals(
                    "chunks after the end",
                    0,
                    m.tickets()
                        .heldChunks());
                out.check(seen.size() >= 3, "the window slid across several chunks (" + seen.size() + ")");
                return out.build();
            });
        }
    }

    /** Common flight set-up of the tickets scenarios: a fueled, launched T1 on a long flight over a flat floor. */
    private abstract static class LongFlight extends Base {

        EntityMissile launched(ScenarioContext ctx, ScenarioResult.Builder out) {
            sweep(ctx);
            ctx.clearArea(8, 20);
            EntityMissile m = armed(ctx, MissileTier.T1, MissileBodies.MAX_GUIDANCE);
            if (m == null) {
                return null;
            }
            m.fill(m.capacityMb(), true);
            LaunchResult r = PadMissiles.launch(
                ctx.world(),
                ctx.originX(),
                ctx.originY(),
                ctx.originZ(),
                ctx.originX() + 0.5,
                ctx.originZ() + 200.5,
                null);
            out.checkEquals("launch", LaunchResult.OK, r);
            return r == LaunchResult.OK ? m : null;
        }
    }

    /**
     * The timeout branch really fires: a short timeout ends the flight in the air and drops the ticket (ICBM-03-62).
     */
    private static final class Timeout extends LongFlight {

        @Override
        public String name() {
            return "icbm.missile.timeout";
        }

        @Override
        public String description() {
            return "With a short missileTimeoutTicks the missile detonates in the air before its target and releases its ticket.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            boolean terrain = IcbmConfig.terrainDamageT1;
            int timeout = IcbmConfig.missileTimeoutTicks;
            IcbmConfig.terrainDamageT1 = false;
            // Negative: the safety net fires this many ticks before the planned arrival.
            IcbmConfig.missileTimeoutTicks = -40;
            EntityMissile m = launched(ctx, out);
            if (m == null) {
                IcbmConfig.terrainDamageT1 = terrain;
                IcbmConfig.missileTimeoutTicks = timeout;
                return ScenarioResult.fail(name(), "could not launch");
            }
            final double[] lastY = { 0 };
            final int[] maxTickets = { 0 };
            return ScenarioResult.pending(name(), 1200, ticks -> {
                if (!m.isDead) {
                    lastY[0] = m.posY;
                    maxTickets[0] = Math.max(
                        maxTickets[0],
                        m.tickets()
                            .tickets());
                    return ticks >= 1150 ? finish(out, terrain, timeout, "never ended") : null;
                }
                out.check(lastY[0] > ctx.originY() + 3, "it ended in the air, before the ground (" + lastY[0] + ")");
                out.checkEquals("one ticket while flying", 1, maxTickets[0]);
                out.checkEquals(
                    "ticket released by the timeout",
                    0,
                    m.tickets()
                        .tickets());
                return finish(out, terrain, timeout, null);
            });
        }

        private ScenarioResult finish(ScenarioResult.Builder out, boolean terrain, int timeout, String failure) {
            IcbmConfig.terrainDamageT1 = terrain;
            IcbmConfig.missileTimeoutTicks = timeout;
            return failure == null ? out.build() : ScenarioResult.fail(name(), failure);
        }
    }

    /**
     * Restart and unload: Forge re-binds the ticket to the missile and calls our callback; a live flight keeps it, and
     * a saved and reloaded entity flies on to its target holding one ticket again (D25).
     */
    private static final class RestartAdopt extends LongFlight {

        @Override
        public String name() {
            return "icbm.missile.restart_adopt";
        }

        @Override
        public String description() {
            return "The load callback hands a live in-flight missile its ticket back, releases a stray one, and a reloaded missile finishes its flight.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World w = ctx.world();
            boolean terrain = IcbmConfig.terrainDamageT1;
            IcbmConfig.terrainDamageT1 = false;
            Target target = new Target(ctx, 0, 200, 9);
            EntityMissile m = launched(ctx, out);
            if (m == null) {
                IcbmConfig.terrainDamageT1 = terrain;
                return ScenarioResult.fail(name(), "could not launch");
            }
            final EntityMissile[] live = { m };
            final int[] step = { 0 };
            final int[] maxTickets = { 0 };
            return ScenarioResult.pending(name(), 1500, ticks -> {
                EntityMissile cur = live[0];
                if (ticks >= 1450) {
                    IcbmConfig.terrainDamageT1 = terrain;
                    return ScenarioResult.fail(name(), "still running at step " + step[0]);
                }
                if (step[0] == 0) {
                    if (cur.phase() != FlightPhase.ARC || cur.posZ < ctx.originZ() + 60) {
                        return null;
                    }
                    // 1. The world loaded: Forge hands our callback the missile's own ticket, and a stray one.
                    net.minecraftforge.common.ForgeChunkManager.Ticket own = cur.tickets()
                        .ticket();
                    net.minecraftforge.common.ForgeChunkManager.Ticket stray = net.minecraftforge.common.ForgeChunkManager
                        .requestTicket(
                            com.gregguns.GregGuns.instance,
                            w,
                            net.minecraftforge.common.ForgeChunkManager.Type.NORMAL);
                    List<net.minecraftforge.common.ForgeChunkManager.Ticket> loaded = new ArrayList<>();
                    loaded.add(own);
                    if (stray != null) {
                        loaded.add(stray);
                        net.minecraftforge.common.ForgeChunkManager.forceChunk(
                            stray,
                            new net.minecraft.world.ChunkCoordIntPair(ctx.originX() >> 4, ctx.originZ() >> 4));
                    }
                    MissileTickets.onTicketsLoaded(loaded, w);
                    out.check(
                        cur.tickets()
                            .ticket() == own
                            && own.getChunkList()
                                .size() >= 1,
                        "a live in-flight missile keeps its ticket and chunks");
                    out.check(
                        stray == null || stray.getChunkList()
                            .isEmpty(),
                        "a ticket that is not a flying missile's is released");
                    // 2. Save, drop the entity, and read it back into a new one, as a restart does.
                    NBTTagCompound tag = new NBTTagCompound();
                    cur.writeToNBT(tag);
                    cur.tickets()
                        .release();
                    cur.setDead();
                    EntityMissileT1 copy = new EntityMissileT1(w);
                    copy.readFromNBT(tag);
                    w.spawnEntityInWorld(copy);
                    // Forge re-binds the saved ticket to the reloaded entity before the callback runs.
                    net.minecraftforge.common.ForgeChunkManager.Ticket rebound = net.minecraftforge.common.ForgeChunkManager
                        .requestTicket(
                            com.gregguns.GregGuns.instance,
                            w,
                            net.minecraftforge.common.ForgeChunkManager.Type.ENTITY);
                    if (rebound == null) {
                        IcbmConfig.terrainDamageT1 = terrain;
                        return ScenarioResult.fail(name(), "no ticket for the re-bind");
                    }
                    rebound.bindEntity(copy);
                    // Forge saved the ticket with the missile's chunk forced; the re-bound one carries it.
                    net.minecraftforge.common.ForgeChunkManager.forceChunk(
                        rebound,
                        new net.minecraft.world.ChunkCoordIntPair(
                            net.minecraft.util.MathHelper.floor_double(copy.posX) >> 4,
                            net.minecraft.util.MathHelper.floor_double(copy.posZ) >> 4));
                    List<net.minecraftforge.common.ForgeChunkManager.Ticket> again = new ArrayList<>();
                    again.add(rebound);
                    MissileTickets.onTicketsLoaded(again, w);
                    out.check(
                        copy.tickets()
                            .ticket() == rebound,
                        "the reloaded missile adopted the re-bound ticket");
                    out.checkEquals("reloaded phase", FlightPhase.ARC, copy.phase());
                    live[0] = copy;
                    step[0] = 1;
                    return null;
                }
                maxTickets[0] = Math.max(
                    maxTickets[0],
                    cur.tickets()
                        .tickets());
                if (!cur.isDead || target.removed(w) == 0 && ticks < 1400) {
                    return null;
                }
                IcbmConfig.terrainDamageT1 = terrain;
                out.checkEquals("one ticket after the reload", 1, maxTickets[0]);
                out.check(target.removed(w) > 0, "the reloaded missile still hit its target");
                out.checkEquals(
                    "no tickets after impact",
                    0,
                    cur.tickets()
                        .tickets());
                return out.build();
            });
        }
    }

    /** The target chunk is loaded once at launch, and the arc aims at the real surface there. */
    private static final class TargetHeight extends Base {

        @Override
        public String name() {
            return "icbm.missile.target_height";
        }

        @Override
        public String description() {
            return "Launching at a far, unloaded chunk loads it once and aims at its surface height.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            sweep(ctx);
            ctx.clearArea(6, 20);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            EntityMissile m = armed(ctx, MissileTier.T1, 0);
            if (m == null) {
                return ScenarioResult.fail(name(), "could not arm");
            }
            m.fill(m.capacityMb(), true);
            World w = ctx.world();
            double tz = ctx.originZ() + m.maxRange() - 20;
            int cx = ctx.originX() >> 4;
            int cz = (int) tz >> 4;
            boolean loadedBefore = w.getChunkProvider()
                .chunkExists(cx, cz);
            LaunchResult r = PadMissiles
                .launch(w, ctx.originX(), ctx.originY(), ctx.originZ(), ctx.originX() + 0.5, tz, null);
            out.checkEquals("launch", LaunchResult.OK, r);
            if (r == LaunchResult.OK) {
                double[] aim = m.aimPoint();
                int ax = net.minecraft.util.MathHelper.floor_double(aim[0]);
                int az = net.minecraft.util.MathHelper.floor_double(aim[2]);
                out.check(
                    w.getChunkProvider()
                        .chunkExists(ax >> 4, az >> 4),
                    "the target chunk is loaded after the launch (it was " + (loadedBefore ? "" : "not ") + "before)");
                out.checkEquals("the aim height is the surface there", (double) w.getHeightValue(ax, az), aim[1]);
            }
            m.tickets()
                .release();
            m.setDead();
            return out.build();
        }
    }

    /** The saved entity resumes: phase, tick, fuel, target and owner survive (ICBM-03-04, -23, -42, -43). */
    private static final class SaveRoundTrip extends Base {

        @Override
        public String name() {
            return "icbm.missile.save_round_trip";
        }

        @Override
        public String description() {
            return "A missile written to NBT mid-warmup and read back keeps its phase, timer, fuel, warhead and health.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            sweep(ctx);
            ctx.clearArea(6, 20);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            EntityMissile m = armed(ctx, MissileTier.T2, 2);
            if (m == null) {
                return ScenarioResult.fail(name(), "could not arm");
            }
            m.fill(m.capacityMb(), true);
            m.launch(ctx.originX() + 400.5, ctx.originZ() + 0.5, ctx.player());
            for (int i = 0; i < 10; i++) {
                m.onUpdate();
            }
            NBTTagCompound tag = new NBTTagCompound();
            m.writeToNBT(tag);
            EntityMissileT2 copy = new EntityMissileT2(ctx.world());
            copy.readFromNBT(tag);
            out.checkEquals("phase", FlightPhase.WARMUP, copy.phase());
            out.checkEquals("fuel", m.capacityMb(), copy.fuelMb());
            out.checkEquals(
                "guidance",
                2,
                copy.status()
                    .guidance());
            out.checkEquals(
                "explosive",
                "thermobaric",
                copy.status()
                    .explosive());
            out.checkEquals("planned distance", m.plannedDistance(), copy.plannedDistance());
            // A huge health value in an old save is clamped to this tier's maximum, whatever order keys load in.
            tag.setFloat("health", 99999.0F);
            EntityMissileT2 hurt = new EntityMissileT2(ctx.world());
            hurt.readFromNBT(tag);
            NBTTagCompound again = new NBTTagCompound();
            hurt.writeToNBT(again);
            out.checkEquals("health clamped by tier", MissileTier.T2.health(), again.getFloat("health"));
            m.tickets()
                .release();
            m.setDead();
            return out.build();
        }
    }

    /** Lethal damage always ends a flying missile with a detonation (ICBM-03-03); the pad one is not hurt by fire. */
    private static final class ShotDown extends Base {

        @Override
        public String name() {
            return "icbm.missile.shot_down";
        }

        @Override
        public String description() {
            return "Damage that takes a flying missile's health to zero detonates it; on the pad only a sneaking player's hit disarms it.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            sweep(ctx);
            ctx.clearArea(8, 20);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            boolean terrain = IcbmConfig.terrainDamageT1;
            IcbmConfig.terrainDamageT1 = false;
            try {
                shootDown(ctx, out);
            } finally {
                IcbmConfig.terrainDamageT1 = terrain;
            }
            return out.build();
        }

        private void shootDown(ScenarioContext ctx, ScenarioResult.Builder out) {
            EntityMissile m = armed(ctx, MissileTier.T1, 0);
            if (m == null) {
                out.check(false, "could not arm");
                return;
            }
            out.check(
                !m.attackEntityFrom(net.minecraft.util.DamageSource.generic, 1000.0F),
                "a pad missile shrugs off damage");
            out.check(!m.isDead, "still there");
            m.fill(m.capacityMb(), true);
            m.launch(ctx.originX() + 0.5, ctx.originZ() + 100.5, null);
            for (int i = 0; i < IcbmConfig.countdownTicks + 5; i++) {
                m.onUpdate();
            }
            out.check(m.phase() == FlightPhase.CLIMB, "flying (" + m.phase() + ")");
            m.attackEntityFrom(net.minecraft.util.DamageSource.generic, MissileTier.T1.health() + 1.0F);
            out.check(m.isDead, "lethal damage ends the missile");
            out.checkEquals(
                "tickets released",
                0,
                m.tickets()
                    .tickets());
        }
    }

    /**
     * The pad scenario. A thin wrapper with no Galacticraft types, so it registers when Galacticraft is absent; the
     * real scenario class is only loaded after the presence check.
     */
    private static final class PadSurvives extends Base {

        @Override
        public String name() {
            return "icbm.pad.survives";
        }

        @Override
        public String description() {
            return "The Galacticraft pad, fuel loader and pad cells stay intact through a launch; pad removal is denied.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (needsModule() != null) {
                return needsModule();
            }
            if (!IcbmAvailability.galacticraftPresent()) {
                return ScenarioResult.skip(name(), "Galacticraft is not loaded (D46)");
            }
            return new MissileGcScenarios.PadSurvives().run(ctx);
        }
    }
}
