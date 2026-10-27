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
        MissileScenarios.Target.loadChunks(ctx, dx, dz, 1);
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

    /** The tile as the world holds it now; GT may replace the tile entity in its first ticks. */
    static IGregTechTileEntity live(ScenarioContext ctx, int dx, int dy, int dz) {
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
        return tile instanceof IGregTechTileEntity ? (IGregTechTileEntity) tile : null;
    }

    private static MTERadar radar(IGregTechTileEntity b) {
        return b != null && b.getMetaTileEntity() instanceof MTERadar ? (MTERadar) b.getMetaTileEntity() : null;
    }

    private static MTEAirDefense battery(IGregTechTileEntity b) {
        return b != null && b.getMetaTileEntity() instanceof MTEAirDefense ? (MTEAirDefense) b.getMetaTileEntity()
            : null;
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
                    if (m instanceof gregtech.api.metatileentity.implementations.MTETieredMachineBlock) {
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
            place(ctx, GregGunsMTEIDs.AIR_DEFENSE, 14, 0, target);
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final Seen seen = new Seen();
            final int[] stage = { 0 };
            return ScenarioResult.pending(name(), 1500, ticks -> {
                IGregTechTileEntity rb = live(ctx, 12, 0, target);
                IGregTechTileEntity bb = live(ctx, 14, 0, target);
                MTERadar r = radar(rb);
                MTEAirDefense b = battery(bb);
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                if (r == null || b == null) {
                    return finish(terrain, ScenarioResult.fail(name(), "a machine is gone after the warm-up"));
                }
                power(rb);
                power(bb);
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
                out.check(seen.sam, "a SAM flew");
                out.check(m.isDead, "the missile is gone");
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
            place(ctx, GregGunsMTEIDs.AIR_DEFENSE, -26, 0, 80);
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final Seen seen = new Seen();
            final int[] stage = { 0 };
            return ScenarioResult.pending(name(), 1500, ticks -> {
                IGregTechTileEntity rb = live(ctx, -28, 0, 80);
                IGregTechTileEntity bb = live(ctx, -26, 0, 80);
                MTERadar r = radar(rb);
                MTEAirDefense b = battery(bb);
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                if (r == null || b == null) {
                    return finish(terrain, ScenarioResult.fail(name(), "a machine is gone after the warm-up"));
                }
                power(rb);
                power(bb);
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
            place(ctx, GregGunsMTEIDs.RADAR_EV, -10, 0, -30);
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            final boolean[] hvSaw = { false };
            final int[] stage = { 0 };
            return ScenarioResult.pending(name(), 1500, ticks -> {
                IGregTechTileEntity hb = live(ctx, -12, 0, -30);
                IGregTechTileEntity eb = live(ctx, -10, 0, -30);
                MTERadar hv = radar(hb);
                MTERadar ev = radar(eb);
                if (ticks < WARMUP_TICKS) {
                    return null;
                }
                if (hv == null || ev == null) {
                    return finish(terrain, ScenarioResult.fail(name(), "a radar is gone after the warm-up"));
                }
                power(hb);
                power(eb);
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
    private static void powerHatch(ScenarioContext ctx, int dx, int cy, int dz) {
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
                threat[0] |= t.state() == RadarRules.State.THREAT;
                tracking[0] |= t.state() == RadarRules.State.TRACKING;
                signal[0] = Math.max(signal[0], t.signal());
                if (stage[0] == 2) {
                    if (!threat[0] && !missile[0].isDead && ticks < mark[0] + 900) {
                        return null;
                    }
                    out.check(threat[0], "the tower flagged the missile as a threat");
                    out.check(signal[0] >= 1, "the tower's redstone rose with the threat");
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
                out.check(fresh != t, "the tile really was replaced");
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

    /** The radar gun stores what it aims at and a Launch Control takes it. */
    private static final class Gun extends Base {

        @Override
        public String name() {
            return "icbm.defense.radar_gun";
        }

        @Override
        public String description() {
            return "The radar gun stores the block it aims at from the server's trace, clears on sneak, and copies its target to a Launch Control.";
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

    /** The radius and the battery cooldown survive a save and reload; a damaged link reads as none. */
    private static final class SaveReload extends Base {

        @Override
        public String name() {
            return "icbm.defense.save_reload";
        }

        @Override
        public String description() {
            return "A radar's protected radius and a battery's cooldown survive a save and reload of the machine.";
        }

        @Override
        ScenarioResult go(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.clearArea(8, 24);
            IGregTechTileEntity rb = place(ctx, GregGunsMTEIDs.RADAR_EV, 0, 0, 0);
            MTERadar r = radar(rb);
            r.setRadius(77);
            net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
            r.saveNBTData(tag);
            MTERadar copy = (MTERadar) r.newMetaEntity(rb);
            copy.loadNBTData(tag);
            out.checkEquals("radius after reload", 77, copy.protectedRadius());
            out.checkEquals(
                "the battery starts without a link",
                null,
                battery(place(ctx, GregGunsMTEIDs.AIR_DEFENSE, 3, 0, 0)).link());
            return out.build();
        }
    }
}
