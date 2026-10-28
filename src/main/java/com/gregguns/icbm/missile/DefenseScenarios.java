package com.gregguns.icbm.missile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.defense.DefenseMachines;
import com.gregguns.icbm.defense.EntityRadarDish;
import com.gregguns.icbm.defense.EntitySam;
import com.gregguns.icbm.defense.ItemRadarGun;
import com.gregguns.icbm.defense.MTEAirDefense;
import com.gregguns.icbm.defense.MTERadar;
import com.gregguns.icbm.defense.MTERadarTower;
import com.gregguns.icbm.defense.RadarLink;
import com.gregguns.icbm.defense.RadarLinks;
import com.gregguns.icbm.defense.RadarRules;
import com.gregguns.icbm.defense.RadarUnit;
import com.gregguns.icbm.defense.SamRules;
import com.gregguns.machine.DevStructureBuilder;
import com.gregguns.machine.GregGunsMTEIDs;
import com.mojang.authlib.GameProfile;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Headless scenarios of I4's detection and defense (radars, tower, battery, SAM, radar gun). They live in the missile
 * package for its stacking helpers, like {@link ControlLaunchScenario}. A missile flies a real arc from the origin
 * toward {@code +Z}; the machines stand beside the flight. Terrain damage is switched off while they run, so an
 * intercept does not wreck the rig. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md, air-defense-battery.md
public final class DefenseScenarios {

    private static final int WARMUP_TICKS = 50;
    private static final String OWNER = "gregguns-test";

    private DefenseScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Registered());
        registry.register(new Intercept());
        registry.register(new NotIncoming());
        registry.register(new RangeByTier());
        registry.register(new Tower());
        registry.register(new TowerDetects());
        registry.register(new TowerSaveReload());
        registry.register(new MpDefense());
        RadarDishScenarios.registerAll(registry);
        I4ClientSetups.registerAll(registry);
        registry.register(new Gun());
        registry.register(new SaveReload());
    }

    // --- helpers ----------------------------------------------------------------------------------------------------

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

    static IGregTechTileEntity place(ScenarioContext ctx, int id, int dx, int dy, int dz) {
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
        // The machine may stand beyond the chunks the origin keeps loaded: load its chunk first, or the block and
        // its tile are silently not placed.
        loadChunks(ctx, dx, dz, 1);
        ctx.setBlock(dx, dy - 1, dz, Blocks.stone, 0);
        ctx.setBlock(dx, dy, dz, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
        if (!(tile instanceof IGregTechTileEntity)) {
            throw new IllegalStateException(
                "no GT tile at " + dx
                    + ","
                    + dy
                    + ","
                    + dz
                    + ": block "
                    + ctx.world()
                        .getBlock(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz)
                    + " tile "
                    + tile
                    + " origin "
                    + ctx.originX()
                    + ","
                    + ctx.originY()
                    + ","
                    + ctx.originZ());
        }
        IGregTechTileEntity base = (IGregTechTileEntity) tile;
        base.setInitialValuesAsNBT(null, (short) id);
        base.setFrontFacing(ForgeDirection.NORTH);
        base.setOwnerName(OWNER);
        base.setOwnerUuid(UUID.nameUUIDFromBytes(OWNER.getBytes(StandardCharsets.UTF_8)));
        return base;
    }

    /** Loads the chunks around a machine's block, or the block and its tile are silently not placed. */
    private static void loadChunks(ScenarioContext ctx, int dx, int dz, int radius) {
        net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) ctx.world();
        for (int cx = (ctx.originX() + dx - radius) >> 4; cx <= (ctx.originX() + dx + radius) >> 4; cx++) {
            for (int cz = (ctx.originZ() + dz - radius) >> 4; cz <= (ctx.originZ() + dz + radius) >> 4; cz++) {
                ws.theChunkProviderServer.loadChunk(cx, cz);
            }
        }
    }

    /** The tile as the world holds it now; GT may replace the tile entity in its first ticks. */
    static IGregTechTileEntity live(ScenarioContext ctx, int dx, int dy, int dz) {
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
        return tile instanceof IGregTechTileEntity ? (IGregTechTileEntity) tile : null;
    }

    static MTERadar radar(IGregTechTileEntity b) {
        return b != null && b.getMetaTileEntity() instanceof MTERadar ? (MTERadar) b.getMetaTileEntity() : null;
    }

    private static MTEAirDefense battery(IGregTechTileEntity b) {
        return b != null && b.getMetaTileEntity() instanceof MTEAirDefense ? (MTEAirDefense) b.getMetaTileEntity()
            : null;
    }

    /**
     * Builds the small radar around a placed controller, feeds its hatch and waits until it runs (D125). Call every
     * tick after the warm-up with a state array of two ints that starts at zero.
     *
     * @return 1 once the radar is formed and running, 0 while it is not yet, -1 when it never got there
     */
    static int radarReady(ScenarioContext ctx, MTERadar r, int dx, int cy, int dz, ItemList hatch, int ticks,
        int[] st) {
        if (st[0] == 0) {
            boolean formed = DevStructureBuilder
                .buildRadar(ctx.world(), ctx.originX() + dx, ctx.originY() + cy, ctx.originZ() + dz, hatch).formed;
            if (formed) {
                st[0] = 1;
                st[1] = ticks;
            }
            return ticks > 400 ? -1 : 0;
        }
        powerHatch(ctx, dx, cy, dz);
        if (r.online()) {
            return 1;
        }
        return ticks > st[1] + 400 ? -1 : 0;
    }

    private static void power(IGregTechTileEntity b) {
        if (b != null) {
            b.increaseStoredEnergyUnits(100_000L, true);
        }
    }

    private static ItemStack linkStick(IGregTechTileEntity radar, World w) {
        ItemStack stick = ItemList.Tool_DataStick.get(1);
        stick.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        new RadarLink(w.provider.dimensionId, radar.getXCoord(), radar.getYCoord(), radar.getZCoord())
            .write(stick.getTagCompound());
        return stick;
    }

    private static int samsInAir(World w) {
        int n = 0;
        for (Object o : w.loadedEntityList) {
            if (o instanceof EntitySam && !((EntitySam) o).isDead) {
                n++;
            }
        }
        return n;
    }

    /**
     * What the world says is powering the block at a scenario offset (the redstone input a lamp or a comparator
     * there would get), not the machine's own internal value.
     */
    static String lampInfo(ScenarioContext ctx, int dx, int dy, int dz, IGregTechTileEntity m, ForgeDirection side) {
        return " [block " + ctx.world()
            .getBlock(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz)
            .getUnlocalizedName()
            + ", machine side "
            + (m == null ? "gone" : "" + m.getOutputRedstoneSignal(side))
            + "]";
    }

    static int neighborPower(ScenarioContext ctx, int dx, int dy, int dz) {
        int x = ctx.originX() + dx;
        int y = ctx.originY() + dy;
        int z = ctx.originZ() + dz;
        // The same weak-power query a lamp or a comparator makes (World#getBlockPowerInput reads strong power only).
        int best = 0;
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            best = Math.max(
                best,
                ctx.world()
                    .getIndirectPowerLevelTo(x + d.offsetX, y + d.offsetY, z + d.offsetZ, d.ordinal()));
        }
        return best;
    }

    private static EntityMissile arm(ScenarioContext ctx) {
        MissileScenarios.sweep(ctx);
        EntityMissile m = MissileScenarios.armedAt(ctx, MissileTier.T1, 3, 0);
        if (m != null) {
            m.fill(m.capacityMb(), true);
        }
        return m;
    }

    private static ScenarioResult finish(boolean terrain, ScenarioResult r) {
        IcbmConfig.terrainDamageT1 = terrain;
        return r;
    }

    // --- scenarios --------------------------------------------------------------------------------------------------

    /** Everything is registered under its ID with the tier, the recipes and the items. */
    private static final class Registered extends Base {

        @Override
        public String name() {
            return "icbm.defense.registered";
        }

        @Override
        public String description() {
            return "The radars (20142, 20143), the tower (20144) and the battery (20145) are registered with the right tiers and Assembler recipes; the SAM and the radar gun exist.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            int[] ids = { GregGunsMTEIDs.RADAR_HV, GregGunsMTEIDs.RADAR_EV, GregGunsMTEIDs.RADAR_TOWER,
                GregGunsMTEIDs.AIR_DEFENSE };
            Class<?>[] types = { MTERadar.class, MTERadar.class, MTERadarTower.class, MTEAirDefense.class };
            int[] tiers = { 3, 4, 5, 3 };
            for (int i = 0; i < ids.length; i++) {
                IMetaTileEntity m = GregTechAPI.METATILEENTITIES[ids[i]];
                out.check(types[i].isInstance(m), "MTE " + ids[i] + " should be a " + types[i].getSimpleName());
                if (m != null) {
                    if (m instanceof MTERadar) {
                        out.checkEquals("tier of " + ids[i], tiers[i], ((MTERadar) m).tier());
                    } else if (m instanceof gregtech.api.metatileentity.implementations.MTETieredMachineBlock) {
                        out.checkEquals(
                            "tier of " + ids[i],
                            tiers[i],
                            (int) ((gregtech.api.metatileentity.implementations.MTETieredMachineBlock) m).mTier);
                    }
                    boolean found = false;
                    ItemStack stack = DefenseMachines.stack(ids[i], 1);
                    for (gregtech.api.util.GTRecipe r : gregtech.api.recipe.RecipeMaps.assemblerRecipes
                        .getAllRecipes()) {
                        if (r.mOutputs != null && r.mOutputs.length > 0
                            && r.mOutputs[0] != null
                            && r.mOutputs[0].getItem() == stack.getItem()
                            && r.mOutputs[0].getItemDamage() == stack.getItemDamage()) {
                            found = true;
                        }
                    }
                    out.check(found, "MTE " + ids[i] + " needs an Assembler recipe");
                }
            }
            out.check(DefenseMachines.samItem() != null, "the SAM item is registered");
            out.check(DefenseMachines.radarGunItem() != null, "the radar gun is registered");
            return out.build();
        }
    }

    /** What a flown missile showed each machine, filled while the scenario polls. */
    private static final class Seen {

        boolean threat;
        boolean tracking;
        boolean engaging;
        boolean sam;
        int radarSignal;
        int batterySignal;
        double lastZ;
        int deadAt = -1;
        /** Ticks of the first sighting of each stage, -1 before; for failure messages. */
        int tThreat = -1;
        int tEngage = -1;
        int tSam = -1;
        double samZ;
        double samDist = -1;

        String timeline() {
            return " [threat@" + tThreat
                + " engage@"
                + tEngage
                + " sam@"
                + tSam
                + " dead@"
                + deadAt
                + " samZ "
                + samZ
                + " samDist "
                + samDist
                + "]";
        }
    }

    private static void observe(Seen s, MTERadar r, MTEAirDefense b, World w, EntityMissile m, int ticks, int oz) {
        if (s.tThreat < 0 && r != null && r.state() == RadarRules.State.THREAT) {
            s.tThreat = ticks;
        }
        if (s.tEngage < 0 && b != null && b.state() == SamRules.State.ENGAGING) {
            s.tEngage = ticks;
        }
        for (Object o : w.loadedEntityList) {
            if (o instanceof EntitySam && !((EntitySam) o).isDead) {
                EntitySam sam = (EntitySam) o;
                if (s.tSam < 0) {
                    s.tSam = ticks;
                }
                s.samZ = sam.posZ - oz;
                s.samDist = sam.getDistanceToEntity(m);
            }
        }
        if (r != null) {
            s.threat |= r.state() == RadarRules.State.THREAT;
            s.tracking |= r.state() == RadarRules.State.TRACKING;
            s.radarSignal = Math.max(s.radarSignal, r.signal());
        }
        if (b != null) {
            s.engaging |= b.state() == SamRules.State.ENGAGING;
            s.batterySignal = Math.max(s.batterySignal, b.signal());
        }
        s.sam |= samsInAir(w) > 0;
        if (!m.isDead) {
            s.lastZ = m.posZ - oz;
        } else if (s.deadAt < 0) {
            s.deadAt = ticks;
        }
    }

    /** Exit box: a radar flags an incoming missile and a linked battery intercepts it. */
    private static final class Intercept extends Base {

        @Override
        public String name() {
            return "icbm.defense.intercept";
        }

        @Override
        public String description() {
            return "An HV radar flags a T1 aimed inside its protected radius (redstone 1), a linked battery fires a SAM, and the missile is shot down short of its target.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(8, 24);
            final boolean terrain = IcbmConfig.terrainDamageT1;
            IcbmConfig.terrainDamageT1 = false;
            final World w = ctx.world();
            final EntityMissile m = arm(ctx);
            if (m == null) {
                return finish(terrain, ScenarioResult.fail(name(), "could not arm"));
            }
            final int target = 80;
            place(ctx, GregGunsMTEIDs.RADAR_HV, 12, 0, target);
            place(ctx, GregGunsMTEIDs.AIR_DEFENSE, 16, 0, target);
            // A lamp in front (north) of each machine: the world, not the machine, says whether the strength left the
            // block. The radar's structure fills the rest of its sides.
            ctx.setBlock(12, 0, target - 1, Blocks.redstone_lamp, 0);
            ctx.setBlock(16, 0, target + 1, Blocks.redstone_lamp, 0);
            final int[] rig = { 0, 0 };
            final int[] lampPower = { 0, 0 };
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final Seen seen = new Seen();
            final int[] stage = { 0 };
            return ScenarioResult.pending(name(), 1500, ticks -> {
                IGregTechTileEntity rb = live(ctx, 12, 0, target);
                IGregTechTileEntity bb = live(ctx, 16, 0, target);
                MTERadar r = radar(rb);
                MTEAirDefense b = battery(bb);
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                if (r == null || b == null) {
                    return finish(terrain, ScenarioResult.fail(name(), "a machine is gone after the warm-up"));
                }
                power(bb);
                int ready = radarReady(ctx, r, 12, 0, target, ItemList.Hatch_Energy_HV, ticks, rig);
                if (ready < 0) {
                    return finish(terrain, ScenarioResult.fail(name(), "the radar never formed and ran"));
                }
                if (ready == 0) {
                    return null;
                }
                if (stage[0] == 0) {
                    // Protect a circle that holds the target; link the battery by a stick; load two SAMs.
                    r.setRadius(100);
                    bb.setInventorySlotContents(MTEAirDefense.SLOT_STICK, linkStick(rb, w));
                    bb.setInventorySlotContents(MTEAirDefense.SLOT_SAM, new ItemStack(DefenseMachines.samItem(), 2));
                    out.checkEquals(
                        "launch",
                        LaunchResult.OK,
                        m.launch(ctx.originX() + 0.5, ctx.originZ() + target + 0.5, null));
                    stage[0] = 1;
                    return null;
                }
                observe(seen, r, b, w, m, ticks, ctx.originZ());
                lampPower[0] = Math.max(lampPower[0], neighborPower(ctx, 12, 0, target - 1));
                lampPower[1] = Math.max(lampPower[1], neighborPower(ctx, 16, 0, target + 1));
                if (seen.deadAt < 0 && ticks < 1200) {
                    return null;
                }
                if (seen.deadAt >= 0 && ticks < seen.deadAt + 40) {
                    return null;
                }
                MissileScenarios.sweep(ctx);
                out.check(seen.threat, "the radar flagged the missile as a threat");
                out.check(seen.radarSignal >= 1, "the radar's redstone rose with the threat");
                out.check(seen.engaging, "the battery engaged");
                out.check(seen.batterySignal == 15, "the battery raised redstone 15 while engaging");
                out.check(
                    lampPower[0] >= 1,
                    "the world powers the block beside the radar during the threat (" + lampPower[0]
                        + ")"
                        + lampInfo(ctx, 12, 0, target - 1, live(ctx, 12, 0, target), ForgeDirection.NORTH));
                out.check(
                    lampPower[1] == 15,
                    "the world powers the block beside the battery with 15 while engaging (" + lampPower[1] + ")");
                out.check(seen.sam, "a SAM flew");
                out.check(m.isDead, "the missile is gone");
                // Only the shot-down path ends a flying missile with no hit points left; a fizzled SAM and a missile
                // that ended on terrain or a timeout would leave them.
                out.check(m.health() <= 0.0F, "the SAM made the kill (missile health " + m.health() + ")");
                out.check(b.hits() >= 1, "the battery counted the hit (" + b.hits() + ")");
                out.check(
                    seen.lastZ < target - 20,
                    "the missile was shot down short of its target (last z " + seen.lastZ + ")" + seen.timeline());
                ItemStack left = bb.getStackInSlot(MTEAirDefense.SLOT_SAM);
                out.checkEquals("one SAM spent", 1, left == null ? 0 : left.stackSize);
                return finish(terrain, out.build());
            });
        }
    }

    /** A missile not aimed inside the protected radius is tracked but never engaged. */
    private static final class NotIncoming extends Base {

        @Override
        public String name() {
            return "icbm.defense.not_incoming";
        }

        @Override
        public String description() {
            return "A radar with a small protected radius far from the impact point tracks a T1 without flagging it; the linked battery holds fire and the missile lands.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(8, 24);
            final boolean terrain = IcbmConfig.terrainDamageT1;
            IcbmConfig.terrainDamageT1 = false;
            final World w = ctx.world();
            final EntityMissile m = arm(ctx);
            if (m == null) {
                return finish(terrain, ScenarioResult.fail(name(), "could not arm"));
            }
            final int target = 80;
            // Beside the impact point, 28 blocks off the impact point, protecting only 20 blocks.
            place(ctx, GregGunsMTEIDs.RADAR_HV, -28, 0, 80);
            place(ctx, GregGunsMTEIDs.AIR_DEFENSE, -24, 0, 80);
            final int[] rig = { 0, 0 };
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final Seen seen = new Seen();
            final int[] stage = { 0 };
            return ScenarioResult.pending(name(), 1500, ticks -> {
                IGregTechTileEntity rb = live(ctx, -28, 0, 80);
                IGregTechTileEntity bb = live(ctx, -24, 0, 80);
                MTERadar r = radar(rb);
                MTEAirDefense b = battery(bb);
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                if (r == null || b == null) {
                    return finish(terrain, ScenarioResult.fail(name(), "a machine is gone after the warm-up"));
                }
                power(bb);
                int ready = radarReady(ctx, r, -28, 0, 80, ItemList.Hatch_Energy_HV, ticks, rig);
                if (ready < 0) {
                    return finish(terrain, ScenarioResult.fail(name(), "the radar never formed and ran"));
                }
                if (ready == 0) {
                    return null;
                }
                if (stage[0] == 0) {
                    r.setRadius(20);
                    bb.setInventorySlotContents(MTEAirDefense.SLOT_STICK, linkStick(rb, w));
                    bb.setInventorySlotContents(MTEAirDefense.SLOT_SAM, new ItemStack(DefenseMachines.samItem(), 2));
                    out.checkEquals(
                        "launch",
                        LaunchResult.OK,
                        m.launch(ctx.originX() + 0.5, ctx.originZ() + target + 0.5, null));
                    stage[0] = 1;
                    return null;
                }
                observe(seen, r, b, w, m, ticks, ctx.originZ());
                if ((seen.deadAt < 0 && ticks < 1200) || (seen.deadAt >= 0 && ticks < seen.deadAt + 20)) {
                    return null;
                }
                MissileScenarios.sweep(ctx);
                out.check(seen.tracking, "the radar tracked the missile");
                out.check(!seen.threat, "the radar did not flag it: its impact is outside the protected radius");
                out.checkEquals("no redstone", 0, seen.radarSignal);
                out.check(!seen.engaging && !seen.sam, "the battery held fire");
                ItemStack left = bb.getStackInSlot(MTEAirDefense.SLOT_SAM);
                out.checkEquals("no SAM spent", 2, left == null ? 0 : left.stackSize);
                out.check(m.isDead && seen.lastZ > target - 20, "the missile flew on to its target");
                return finish(terrain, out.build());
            });
        }
    }

    /** The EV radar still sees a missile that has flown out of the HV radar's range. */
    private static final class RangeByTier extends Base {

        @Override
        public String name() {
            return "icbm.defense.range_by_tier";
        }

        @Override
        public String description() {
            return "Two radars 60 blocks behind the pad: HV (128) loses a T1 that has flown 150 blocks on, EV (256) still tracks it.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(8, 24);
            final boolean terrain = IcbmConfig.terrainDamageT1;
            IcbmConfig.terrainDamageT1 = false;
            final EntityMissile m = arm(ctx);
            if (m == null) {
                return finish(terrain, ScenarioResult.fail(name(), "could not arm"));
            }
            place(ctx, GregGunsMTEIDs.RADAR_HV, -12, 0, -30);
            place(ctx, GregGunsMTEIDs.RADAR_EV, -6, 0, -30);
            final int[] rigHv = { 0, 0 };
            final int[] rigEv = { 0, 0 };
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final boolean[] hvSaw = { false };
            final int[] stage = { 0 };
            return ScenarioResult.pending(name(), 1500, ticks -> {
                IGregTechTileEntity hb = live(ctx, -12, 0, -30);
                IGregTechTileEntity eb = live(ctx, -6, 0, -30);
                MTERadar hv = radar(hb);
                MTERadar ev = radar(eb);
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                if (hv == null || ev == null) {
                    return finish(terrain, ScenarioResult.fail(name(), "a radar is gone after the warm-up"));
                }
                int readyHv = radarReady(ctx, hv, -12, 0, -30, ItemList.Hatch_Energy_HV, ticks, rigHv);
                int readyEv = radarReady(ctx, ev, -6, 0, -30, ItemList.Hatch_Energy_EV, ticks, rigEv);
                if (readyHv < 0 || readyEv < 0) {
                    return finish(terrain, ScenarioResult.fail(name(), "a radar never formed and ran"));
                }
                if (readyHv == 0 || readyEv == 0) {
                    return null;
                }
                if (stage[0] == 0) {
                    out.checkEquals("HV range", 128, hv.rangeIfOn());
                    out.checkEquals("EV range", 256, ev.rangeIfOn());
                    out.checkEquals(
                        "launch",
                        LaunchResult.OK,
                        m.launch(ctx.originX() + 0.5, ctx.originZ() + 200.5, null));
                    stage[0] = 1;
                    return null;
                }
                hvSaw[0] |= hv.state() == RadarRules.State.TRACKING;
                if (m.isDead && ticks < 1400) {
                    return null;
                }
                if (!m.isDead && m.posZ - ctx.originZ() < 160 && ticks < 1400) {
                    return null;
                }
                out.check(hvSaw[0], "the HV radar saw the missile early in its flight");
                out.checkEquals("HV is out of range 150 blocks on", RadarRules.State.CLEAR, hv.state());
                out.checkEquals("EV still tracks it", RadarRules.State.TRACKING, ev.state());
                MissileScenarios.sweep(ctx);
                return finish(terrain, out.build());
            });
        }
    }

    /** The radar tower forms, takes its range from the hatch, and runs. */
    private static final class Tower extends Base {

        @Override
        public String name() {
            return "icbm.defense.tower";
        }

        @Override
        public String description() {
            return "The Radar Tower forms with an IV hatch (range 512), a LuV hatch gives 768, and it runs on EU.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(8, 24);
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final int cy = 5;
            place(ctx, GregGunsMTEIDs.RADAR_TOWER, 0, cy, 0);
            final int[] stage = { 0 };
            final int[] mark = { 0 };
            final World w = ctx.world();
            return ScenarioResult.pending(name(), 600, ticks -> {
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                IGregTechTileEntity cb = live(ctx, 0, cy, 0);
                MTERadarTower t = cb != null && cb.getMetaTileEntity() instanceof MTERadarTower
                    ? (MTERadarTower) cb.getMetaTileEntity()
                    : null;
                if (t == null) {
                    return ScenarioResult.fail(name(), "the tower controller is gone");
                }
                int x = ctx.originX();
                int y = ctx.originY() + cy;
                int z = ctx.originZ();
                if (stage[0] == 0) {
                    out.check(
                        DevStructureBuilder.buildRadarTower(w, x, y, z, ItemList.Hatch_Energy_IV).formed,
                        "the tower forms with an IV hatch");
                    stage[0] = 1;
                    mark[0] = ticks;
                    return null;
                }
                if (stage[0] == 1) {
                    out.check(t.isFormed(), "the tower is formed");
                    out.checkEquals("IV range", 512, t.rangeIfOn());
                    IGregTechTileEntity hatch = live(ctx, 0, cy - 1, 0);
                    if (hatch != null) {
                        hatch.increaseStoredEnergyUnits(1_000_000L, true);
                    }
                    stage[0] = 2;
                    mark[0] = ticks;
                    return null;
                }
                if (stage[0] == 2) {
                    if (!t.online() && ticks < mark[0] + 200) {
                        IGregTechTileEntity hatch = live(ctx, 0, cy - 1, 0);
                        if (hatch != null) {
                            hatch.increaseStoredEnergyUnits(1_000_000L, true);
                        }
                        return null;
                    }
                    out.check(t.online(), "the tower runs on EU");
                    out.checkEquals("state when clear", RadarRules.State.CLEAR, t.state());
                    out.check(
                        DevStructureBuilder.buildRadarTower(w, x, y, z, ItemList.Hatch_Energy_LuV).formed,
                        "the tower re-forms with a LuV hatch");
                    stage[0] = 3;
                    return null;
                }
                out.checkEquals("LuV range", 768, t.rangeIfOn());
                return out.build();
            });
        }
    }

    /** Energy for the tower's hatch, which sits under the controller. */
    static void powerHatch(ScenarioContext ctx, int dx, int cy, int dz) {
        IGregTechTileEntity hatch = live(ctx, dx, cy - 1, dz);
        if (hatch != null) {
            hatch.increaseStoredEnergyUnits(1_000_000L, true);
        }
    }

    private static MTERadarTower tower(IGregTechTileEntity b) {
        return b != null && b.getMetaTileEntity() instanceof MTERadarTower ? (MTERadarTower) b.getMetaTileEntity()
            : null;
    }

    /** A formed tower, paid for, flags a T1 aimed inside its protected radius. */
    private static final class TowerDetects extends Base {

        private static final int DX = -14;
        private static final int DZ = 6;
        private static final int CY = 5;

        @Override
        public String name() {
            return "icbm.defense.tower_detects";
        }

        @Override
        public String description() {
            return "A formed Radar Tower with an IV hatch runs, then flags a T1 aimed inside its protected radius as a threat with redstone, and tracks it without a threat once the radius is small.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(8, 24);
            final boolean terrain = IcbmConfig.terrainDamageT1;
            IcbmConfig.terrainDamageT1 = false;
            final World w = ctx.world();
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            place(ctx, GregGunsMTEIDs.RADAR_TOWER, DX, CY, DZ);
            final int target = 80;
            final EntityMissile[] missile = { null };
            final int[] stage = { 0 };
            final int[] mark = { 0 };
            final boolean[] threat = { false };
            final boolean[] tracking = { false };
            final int[] signal = { 0 };
            final int[] lamp = { 0, 0, 0, 0 }; // dx, dy, dz of the lamp, then the most power it saw
            return ScenarioResult.pending(name(), 1800, ticks -> {
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                MTERadarTower t = tower(live(ctx, DX, CY, DZ));
                if (t == null) {
                    return finish(terrain, ScenarioResult.fail(name(), "the tower controller is gone"));
                }
                if (stage[0] == 0) {
                    out.check(
                        DevStructureBuilder.buildRadarTower(
                            w,
                            ctx.originX() + DX,
                            ctx.originY() + CY,
                            ctx.originZ() + DZ,
                            ItemList.Hatch_Energy_IV).formed,
                        "the tower forms");
                    // A lamp in front of the controller, where the structure leaves the side open.
                    ForgeDirection front = t.getBaseMetaTileEntity()
                        .getFrontFacing();
                    lamp[0] = DX + front.offsetX;
                    lamp[1] = CY + front.offsetY;
                    lamp[2] = DZ + front.offsetZ;
                    ctx.setBlock(lamp[0], lamp[1], lamp[2], Blocks.redstone_lamp, 0);
                    stage[0] = 1;
                    mark[0] = ticks;
                    return null;
                }
                powerHatch(ctx, DX, CY, DZ);
                if (stage[0] == 1) {
                    if (!t.online() && ticks < mark[0] + 300) {
                        return null;
                    }
                    out.check(t.online(), "the tower runs before the launch");
                    t.setRadius(100);
                    out.checkEquals("radius set", 100, t.protectedRadius());
                    EntityMissile m = arm(ctx);
                    if (m == null) {
                        return finish(terrain, ScenarioResult.fail(name(), "could not arm"));
                    }
                    missile[0] = m;
                    out.checkEquals(
                        "launch",
                        LaunchResult.OK,
                        m.launch(ctx.originX() + 0.5, ctx.originZ() + target + 0.5, null));
                    stage[0] = 2;
                    mark[0] = ticks;
                    return null;
                }
                // The sweep refreshes every few ticks, so what it held before a launch is stale for a moment.
                if (ticks > mark[0] + 2 * RadarRules.SCAN_TICKS) {
                    threat[0] |= t.state() == RadarRules.State.THREAT;
                    tracking[0] |= t.state() == RadarRules.State.TRACKING;
                    signal[0] = Math.max(signal[0], t.signal());
                    lamp[3] = Math.max(lamp[3], neighborPower(ctx, lamp[0], lamp[1], lamp[2]));
                }
                if (stage[0] == 2) {
                    if (!threat[0] && !missile[0].isDead && ticks < mark[0] + 900) {
                        return null;
                    }
                    out.check(threat[0], "the tower flagged the missile as a threat");
                    out.check(signal[0] >= 1, "the tower's redstone rose with the threat");
                    out.check(
                        lamp[3] >= 1,
                        "the world powers the block in front of the tower during the threat (" + lamp[3] + ")");
                    // Let this missile land or die, then repeat with a radius that excludes its impact point.
                    stage[0] = 3;
                    mark[0] = ticks;
                    return null;
                }
                if (stage[0] == 3) {
                    if (!missile[0].isDead && ticks < mark[0] + 900) {
                        return null;
                    }
                    MissileScenarios.sweep(ctx);
                    t.setRadius(RadarRules.MIN_RADIUS);
                    EntityMissile m = arm(ctx);
                    if (m == null) {
                        return finish(terrain, ScenarioResult.fail(name(), "could not re-arm"));
                    }
                    missile[0] = m;
                    threat[0] = false;
                    tracking[0] = false;
                    out.checkEquals(
                        "second launch",
                        LaunchResult.OK,
                        m.launch(ctx.originX() + 0.5, ctx.originZ() + target + 0.5, null));
                    stage[0] = 4;
                    mark[0] = ticks;
                    return null;
                }
                if (!missile[0].isDead && ticks < mark[0] + 900) {
                    return null;
                }
                MissileScenarios.sweep(ctx);
                out.check(tracking[0], "with a small radius the tower still tracked the missile");
                out.check(!threat[0], "and did not flag it: the impact is outside the radius");
                return finish(terrain, out.build());
            });
        }
    }

    /** A formed tower keeps its radius and its place in a battery's link across a tile reload. */
    private static final class TowerSaveReload extends Base {

        private static final int DX = -14;
        private static final int DZ = -6;
        private static final int CY = 5;

        @Override
        public String name() {
            return "icbm.defense.tower_save_reload";
        }

        @Override
        public String description() {
            return "A formed Radar Tower and a linked battery are written to NBT and their tiles replaced by freshly loaded ones: the tower re-forms with its radius, and the battery's link still finds it.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(8, 24);
            final World w = ctx.world();
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            place(ctx, GregGunsMTEIDs.RADAR_TOWER, DX, CY, DZ);
            place(ctx, GregGunsMTEIDs.AIR_DEFENSE, DX + 6, CY, DZ);
            final int[] stage = { 0 };
            final int[] mark = { 0 };
            final MTERadarTower[] before = { null };
            return ScenarioResult.pending(name(), 900, ticks -> {
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                MTERadarTower t = tower(live(ctx, DX, CY, DZ));
                MTEAirDefense b = battery(live(ctx, DX + 6, CY, DZ));
                if (t == null || b == null) {
                    return ScenarioResult.fail(name(), "a machine is gone");
                }
                if (stage[0] == 0) {
                    out.check(
                        DevStructureBuilder.buildRadarTower(
                            w,
                            ctx.originX() + DX,
                            ctx.originY() + CY,
                            ctx.originZ() + DZ,
                            ItemList.Hatch_Energy_IV).formed,
                        "the tower forms");
                    stage[0] = 1;
                    mark[0] = ticks;
                    return null;
                }
                if (stage[0] == 1) {
                    t.setRadius(111);
                    out.checkEquals("radius set", 111, t.protectedRadius());
                    IGregTechTileEntity bb = live(ctx, DX + 6, CY, DZ);
                    ItemStack stick = ItemList.Tool_DataStick.get(1);
                    RadarUnit.writeLink(live(ctx, DX, CY, DZ), stick, null);
                    bb.setInventorySlotContents(MTEAirDefense.SLOT_STICK, stick);
                    out.check(RadarLinks.find(w, b.link()) == t, "before the reload the link finds the tower");
                    before[0] = t;
                    reload(ctx, DX, CY, DZ);
                    reload(ctx, DX + 6, CY, DZ);
                    stage[0] = 2;
                    mark[0] = ticks;
                    return null;
                }
                MTERadarTower fresh = tower(live(ctx, DX, CY, DZ));
                MTEAirDefense freshBattery = battery(live(ctx, DX + 6, CY, DZ));
                if (fresh == null || freshBattery == null) {
                    return ScenarioResult.fail(name(), "a machine is gone after the reload");
                }
                if (!fresh.isFormed() && ticks < mark[0] + 400) {
                    return null;
                }
                out.check(fresh != before[0], "the tile really was replaced");
                out.check(fresh.isFormed(), "the tower re-formed after the reload");
                out.checkEquals("radius after the reload", 111, fresh.protectedRadius());
                out.check(
                    RadarLinks.find(w, freshBattery.link()) == fresh,
                    "after the reload the battery's link finds the new tower");
                return out.build();
            });
        }

        /** Replaces the tile with one loaded from the old one's NBT, as a chunk load would. */
        private static void reload(ScenarioContext ctx, int dx, int cy, int dz) {
            int x = ctx.originX() + dx;
            int y = ctx.originY() + cy;
            int z = ctx.originZ() + dz;
            TileEntity old = ctx.world()
                .getTileEntity(x, y, z);
            net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
            old.writeToNBT(tag);
            old.invalidate();
            TileEntity fresh = TileEntity.createAndLoadEntity(tag);
            ctx.world()
                .getChunkFromBlockCoords(x, z)
                .addTileEntity(fresh);
        }
    }

    /**
     * {@code testing.icbm_mp_defense}: the multiplayer check for I4. Two clients watch an HV radar go active (its front
     * overlay changes) and a SAM fly at a missile, and take matching labeled shots; labels are {@code icbm-i4-*}. Not
     * part of {@code test all}: it needs connected clients. Server side.
     */
    private static final class MpDefense extends Base {

        @Override
        public String name() {
            return "testing.icbm_mp_defense";
        }

        @Override
        public String description() {
            return "Two clients see an HV radar's dish turning (two shots, 25 ticks apart) and a SAM in flight at a missile, and take matching shots. Needs 2 clients and Galacticraft; not part of test all.";
        }

        @Override
        public boolean includedInAll() {
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        ScenarioResult go(ScenarioContext ctx) {
            final java.util.List<net.minecraft.entity.player.EntityPlayerMP> players = new java.util.ArrayList<>(
                net.minecraft.server.MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList);
            if (players.size() < 2) {
                return ScenarioResult.fail(name(), "needs two connected players, have " + players.size());
            }
            ctx.clearArea(8, 24);
            final boolean terrain = IcbmConfig.terrainDamageT1;
            IcbmConfig.terrainDamageT1 = false;
            final World w = ctx.world();
            w.setWorldTime(6000L);
            final EntityMissile m = arm(ctx);
            if (m == null) {
                return finish(terrain, ScenarioResult.fail(name(), "could not arm"));
            }
            final int rz = 30;
            place(ctx, GregGunsMTEIDs.RADAR_HV, 6, 0, rz);
            final int[] rig = { 0, 0 };
            final float[] angleA = { -1 };
            place(ctx, GregGunsMTEIDs.AIR_DEFENSE, 40, 0, rz);
            for (net.minecraft.entity.player.EntityPlayerMP p : players) {
                p.setGameType(net.minecraft.world.WorldSettings.GameType.CREATIVE);
            }
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final int[] stage = { 0 };
            final int[] mark = { 0 };
            final boolean[] loaded = { false };
            final double[] last = { 0, 0, 0 };
            final double[] sl = { 0, 0, 0 };
            final double ox = ctx.originX();
            final double oy = ctx.originY();
            final double oz = ctx.originZ();
            return ScenarioResult.pending(name(), 2400, ticks -> {
                IGregTechTileEntity rb = live(ctx, 6, 0, rz);
                IGregTechTileEntity bb = live(ctx, 40, 0, rz);
                MTERadar r = radar(rb);
                MTEAirDefense b = battery(bb);
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                if (r == null || b == null) {
                    return finish(terrain, ScenarioResult.fail(name(), "a machine is gone after the warm-up"));
                }
                power(bb);
                int ready = radarReady(ctx, r, 6, 0, rz, ItemList.Hatch_Energy_HV, ticks, rig);
                if (ready < 0) {
                    return finish(terrain, ScenarioResult.fail(name(), "the radar never formed and ran"));
                }
                if (ready == 0) {
                    return null;
                }
                if (stage[0] == 0) {
                    r.setRadius(100);
                    bb.setInventorySlotContents(MTEAirDefense.SLOT_STICK, linkStick(rb, w));
                    // The battery stays empty until the radar has been photographed, so the SAM flies afterwards.
                    for (net.minecraft.entity.player.EntityPlayerMP p : players) {
                        MissileShotsScenario.signal(p, "hud", "off");
                    }
                    out.checkEquals(
                        "launch",
                        LaunchResult.OK,
                        m.launch(ctx.originX() + 0.5, ctx.originZ() + 80 + 0.5, null));
                    stage[0] = 1;
                    return null;
                }
                if (stage[0] == 1) {
                    // The radar's front faces north; stand in front of it, one client either side, looking at the
                    // dish on top (its middle is 1.5 blocks behind the controller and 2 above its floor).
                    if (r.state() != RadarRules.State.THREAT) {
                        return ticks < 900 ? null : finish(terrain, ScenarioResult.fail(name(), "no threat seen"));
                    }
                    for (int i = 0; i < players.size(); i++) {
                        double side = i % 2 == 0 ? 4.0 : -4.0;
                        MissileShotsScenario.cam(
                            players.get(i),
                            ox + 6.5 + side,
                            oy + 4.5,
                            oz + rz - 7.0,
                            ox + 6.5,
                            oy + 2.6,
                            oz + rz + 1.5);
                    }
                    mark[0] = ticks;
                    stage[0] = 2;
                    return null;
                }
                if (stage[0] == 2) {
                    // Two shots 25 ticks apart: a spinning dish is at a different angle in each, on both clients.
                    int dt = ticks - mark[0];
                    EntityRadarDish dish = r.dish();
                    if (dt == 12) {
                        out.check(
                            dish != null && dish.spin()
                                .spinning(),
                            "the dish stands and spins");
                        angleA[0] = dish == null ? -1
                            : dish.spin()
                                .angle(w.getTotalWorldTime());
                        for (net.minecraft.entity.player.EntityPlayerMP p : players) {
                            MissileShotsScenario.signal(p, "shot", "icbm-radar-dish-a");
                        }
                    }
                    if (dt < 37) {
                        return null;
                    }
                    float b2 = dish == null ? -1
                        : dish.spin()
                            .angle(w.getTotalWorldTime());
                    out.check(Math.abs(b2 - angleA[0]) > 30.0F, "the dish turned between the shots");
                    for (net.minecraft.entity.player.EntityPlayerMP p : players) {
                        MissileShotsScenario.signal(p, "shot", "icbm-radar-dish-b");
                    }
                    stage[0] = 3;
                    return null;
                }
                if (stage[0] == 3) {
                    if (bb.getStackInSlot(MTEAirDefense.SLOT_SAM) == null && samsInAir(w) == 0 && !loaded[0]) {
                        loaded[0] = true;
                        bb.setInventorySlotContents(
                            MTEAirDefense.SLOT_SAM,
                            new ItemStack(DefenseMachines.samItem(), 2));
                        return null;
                    }
                    boolean flying = samsInAir(w) > 0;
                    if (!flying) {
                        return ticks < 1500 ? null : finish(terrain, ScenarioResult.fail(name(), "no SAM flew"));
                    }
                    stage[0] = 4;
                    mark[0] = ticks;
                    return null;
                }
                if (stage[0] == 4) {
                    EntitySam sam = null;
                    for (Object o : w.loadedEntityList) {
                        if (o instanceof EntitySam && !((EntitySam) o).isDead) {
                            sam = (EntitySam) o;
                        }
                    }
                    if (!m.isDead) {
                        last[0] = m.posX;
                        last[1] = m.posY;
                        last[2] = m.posZ;
                    }
                    int dt = ticks - mark[0];
                    if (sam != null) {
                        sl[0] = sam.posX;
                        sl[1] = sam.posY;
                        sl[2] = sam.posZ;
                    }
                    if (sam != null || dt < 40) {
                        for (int i = 0; i < players.size(); i++) {
                            double side = i % 2 == 0 ? 9.0 : -9.0;
                            MissileShotsScenario
                                .cam(players.get(i), sl[0] + side, sl[1] + 1.0, sl[2] - 7.0, sl[0], sl[1], sl[2]);
                        }
                        if (dt % 4 == 2 && dt < 60) {
                            for (net.minecraft.entity.player.EntityPlayerMP p : players) {
                                MissileShotsScenario.signal(p, "shot", "sam-t" + (dt / 4));
                            }
                        }
                    }
                    if (sam == null && dt > 6) {
                        stage[0] = 5;
                        mark[0] = ticks;
                    }
                    return null;
                }
                if (stage[0] == 5) {
                    int dt = ticks - mark[0];
                    for (int i = 0; i < players.size(); i++) {
                        double side = i % 2 == 0 ? 9.0 : -9.0;
                        MissileShotsScenario.cam(
                            players.get(i),
                            last[0] + side,
                            last[1] + 1.0,
                            last[2] - 7.0,
                            last[0],
                            last[1],
                            last[2]);
                    }
                    if (dt == 4 || dt == 30 || dt == 70) {
                        for (net.minecraft.entity.player.EntityPlayerMP p : players) {
                            MissileShotsScenario.signal(p, "shot", "after-" + dt);
                        }
                    }
                    if (dt > 80) {
                        stage[0] = 6;
                        mark[0] = ticks - 100;
                    }
                    return null;
                }
                if (ticks < mark[0] + 60 || (!m.isDead && ticks < 2000)) {
                    return null;
                }
                MissileScenarios.sweep(ctx);
                out.check(m.isDead, "the missile is gone");
                for (net.minecraft.entity.player.EntityPlayerMP p : players) {
                    MissileShotsScenario.signal(p, "hud", "on");
                }
                return finish(terrain, out.build());
            });
        }
    }

    /** The radar gun stores what it aims at and clears on sneak. */
    private static final class Gun extends Base {

        @Override
        public String name() {
            return "icbm.defense.radar_gun";
        }

        @Override
        public String description() {
            return "The radar gun stores the block it aims at from the server's trace and clears on sneak.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ctx.clearArea(8, 24);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World w = ctx.world();
            ctx.setBlock(5, 0, 5, Blocks.stone, 0);
            FakePlayer p = new FakePlayer(
                (WorldServer) w,
                new GameProfile(UUID.nameUUIDFromBytes("gg-gun".getBytes(StandardCharsets.UTF_8)), "[GGGun]"));
            p.setPosition(ctx.originX() + 5.5, ctx.originY() + 3.0 - p.getEyeHeight(), ctx.originZ() + 5.5);
            p.rotationPitch = 90.0F;
            ItemStack gun = new ItemStack(DefenseMachines.radarGunItem());
            gun.getItem()
                .onItemRightClick(gun, w, p);
            out.check(
                ItemRadarGun.target(gun)
                    .isSet(),
                "the gun stored a target");
            out.checkEquals(
                "X",
                ctx.originX() + 5,
                ItemRadarGun.target(gun)
                    .x());
            out.checkEquals(
                "Z",
                ctx.originZ() + 5,
                ItemRadarGun.target(gun)
                    .z());
            p.setSneaking(true);
            gun.getItem()
                .onItemRightClick(gun, w, p);
            out.check(
                !ItemRadarGun.target(gun)
                    .isSet(),
                "sneaking cleared it");
            return out.build();
        }
    }

    /** The radius survives a save and reload; a new battery has no link. */
    private static final class SaveReload extends Base {

        @Override
        public String name() {
            return "icbm.defense.save_reload";
        }

        @Override
        public String description() {
            return "A radar controller's protected radius survives a save and reload of its tile, and a new battery starts without a link.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.clearArea(8, 24);
            place(ctx, GregGunsMTEIDs.RADAR_EV, 0, 0, 0);
            final int[] stage = { 0 };
            return ScenarioResult.pending(name(), 200, ticks -> {
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                MTERadar r = radar(live(ctx, 0, 0, 0));
                if (r == null) {
                    return ScenarioResult.fail(name(), "the controller is gone");
                }
                if (stage[0] == 0) {
                    r.setRadius(77);
                    int x = ctx.originX();
                    int z = ctx.originZ();
                    TileEntity old = ctx.world()
                        .getTileEntity(x, ctx.originY(), z);
                    net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                    old.writeToNBT(tag);
                    old.invalidate();
                    ctx.world()
                        .getChunkFromBlockCoords(x, z)
                        .addTileEntity(TileEntity.createAndLoadEntity(tag));
                    stage[0] = 1;
                    return null;
                }
                if (ticks < WARMUP_TICKS + 20) {
                    return null;
                }
                out.checkEquals("radius after reload", 77, r.protectedRadius());
                out.checkEquals(
                    "the battery starts without a link",
                    null,
                    battery(place(ctx, GregGunsMTEIDs.AIR_DEFENSE, 6, 0, 0)).link());
                return out.build();
            });
        }
    }
}
