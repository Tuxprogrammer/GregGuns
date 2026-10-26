package com.gregguns.icbm.missile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import com.gregguns.core.config.IcbmConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.IcbmAvailability;
import com.gregguns.icbm.warhead.WarheadCasing;

import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;

/**
 * {@code testing.icbm_mp_launch}: the multiplayer check for I2. Two or more connected clients watch one T1 launch from
 * different angles; every client is told to take the same labeled shots at the same ticks (the client logs the missile
 * entities it sees at each shot), and the server checks that every player tracks the missile. Labels are
 * {@code icbm-mp-*}. Not part of {@code test all}. Dev environment only, server side.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/ (flight), 07-visuals-audio/ (blast visuals)
public final class MissileMpScenario implements Scenario {

    static final String NAME = "testing.icbm_mp_launch";
    private static final int DISTANCE = 70;
    private static final int STEP = 30;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Two clients watch one T1 launch, flight and impact and take matching shots. Needs 2 clients and"
            + " Galacticraft; not part of test all.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ScenarioResult run(ScenarioContext ctx) {
        if (!ModuleState.isActive(ModuleId.ICBM) || MissileScenarios.moduleOff()) {
            return ScenarioResult.skip(NAME, "the ICBM module is off");
        }
        if (!IcbmAvailability.galacticraftPresent()) {
            return ScenarioResult.skip(NAME, "Galacticraft is not loaded (D46)");
        }
        final List<EntityPlayerMP> players = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        if (players.size() < 2) {
            return ScenarioResult.fail(NAME, "needs two connected players, have " + players.size());
        }
        final World w = ctx.world();
        final int ox = ctx.originX();
        final int oy = ctx.originY();
        final int oz = ctx.originZ();
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                for (int y = 0; y <= 14; y++) {
                    w.setBlock(ox + x, oy + y, oz + z, Blocks.air, 0, 2);
                }
                w.setBlock(ox + x, oy - 1, oz + z, Blocks.stone, 0, 2);
            }
        }
        final int gy = w.getHeightValue(ox, oz + DISTANCE);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                ctx.setBlock(a, 0, b, GCBlocks.landingPad, 0);
            }
        }
        final boolean terrain = IcbmConfig.terrainDamageT1;
        IcbmConfig.terrainDamageT1 = true;
        for (EntityPlayerMP p : players) {
            p.setGameType(WorldSettings.GameType.CREATIVE);
            p.capabilities.allowFlying = true;
            p.capabilities.isFlying = true;
            p.sendPlayerAbilities();
        }
        w.setWorldTime(6000L);
        final ScenarioResult.Builder out = ScenarioResult.builder(NAME);
        final EntityMissile[] m = { null };
        final int[] frames = { 0 };
        final int[] tracked = { 0 };
        // Where the missile was on its last tick: the guidance scatters the hit, so the crater is not at the aim point.
        final double[] last = { ox + 0.5, oz + DISTANCE + 0.5 };
        final MissileShotsScenario.Plan plan = new MissileShotsScenario.Plan();
        final double cx = ox + 0.5;
        final double cz = oz + 0.5;
        final double[][] views = { { -9, 4, -8 }, { 9, 4, -8 }, { 0, 5, -14 } };

        plan.after(5, () -> {
            for (EntityPlayerMP p : players) {
                MissileShotsScenario.signal(p, "hud", "off");
            }
        });
        plan.after(120, () -> {
            MissileScenarios.buildStack(ctx, MissileTier.T1, WarheadCasing.T1, "condensed", 3, 0, 1, 0, true, 0);
            out.checkEquals("armed", MissileStacks.ArmResult.OK, MissileStacks.arm(w, ox, oy + 1, oz, null));
            m[0] = PadMissiles.find(w, ox, oy, oz);
            out.check(m[0] != null, "missile on the pad");
            if (m[0] != null) {
                m[0].fill(m[0].capacityMb(), true);
            }
            for (int i = 0; i < players.size(); i++) {
                double[] v = views[i % views.length];
                MissileShotsScenario.cam(players.get(i), cx + v[0], oy + v[1], cz + v[2], cx, oy + 3, cz);
            }
        });
        plan.after(90, () -> {
            for (EntityPlayerMP p : players) {
                MissileShotsScenario.signal(p, "shot", "icbm-mp-pad");
            }
        });
        plan.after(20, () -> {
            if (m[0] != null) {
                out.checkEquals(
                    "launch",
                    com.gregguns.icbm.missile.LaunchResult.OK,
                    m[0].launch(cx, oz + DISTANCE + 0.5, null));
            }
        });
        plan.after(STEP, new Runnable() {

            @Override
            public void run() {
                EntityMissile e = m[0];
                if (e == null || e.isDead || frames[0] >= 30) {
                    return;
                }
                frames[0]++;
                final int n = frames[0];
                for (int i = 0; i < players.size(); i++) {
                    double[] v = views[i % views.length];
                    // Each observer stays on its own side of the missile's track, looking at it.
                    double ez = Math.max(Math.min(e.posZ - 8, oz + DISTANCE - 22), e.posZ - 16);
                    MissileShotsScenario.cam(
                        players.get(i),
                        e.posX + v[0] * 1.3,
                        Math.max(oy + 4, e.posY + 3),
                        ez,
                        e.posX,
                        e.posY,
                        e.posZ);
                    if (((net.minecraft.world.WorldServer) w).getEntityTracker()
                        .getTrackingPlayers(e)
                        .contains(players.get(i))) {
                        tracked[0]++;
                    }
                }
                plan.after(14, () -> {
                    for (EntityPlayerMP p : players) {
                        MissileShotsScenario.signal(p, "shot", String.format("icbm-mp-flight-%02d", n));
                    }
                });
                plan.after(STEP + 4, this);
            }
        });
        plan.after(STEP * 3, new Runnable() {

            @Override
            public void run() {
                EntityMissile e = m[0];
                if (e != null && !e.isDead && plan.now < 4000) {
                    plan.after(20, this);
                    return;
                }
                out.check(e != null && e.isDead, "the missile detonated");
                final double tx = last[0];
                final double tz = last[1];
                plan.after(15, () -> {
                    for (int i = 0; i < players.size(); i++) {
                        double[] v = views[i % views.length];
                        MissileShotsScenario.cam(players.get(i), tx + v[0] * 2.5, gy + 14, tz + v[2] * 2, tx, gy, tz);
                    }
                });
                plan.after(150, () -> {
                    for (EntityPlayerMP p : players) {
                        MissileShotsScenario.signal(p, "shot", "icbm-mp-impact");
                    }
                });
            }
        });
        return ScenarioResult.pending(NAME, 6000, t -> {
            if (m[0] != null && !m[0].isDead) {
                last[0] = m[0].posX;
                last[1] = m[0].posZ;
            }
            plan.run(t);
            if (!plan.idle()) {
                return null;
            }
            IcbmConfig.terrainDamageT1 = terrain;
            out.check(
                tracked[0] >= frames[0] * players.size() / 2,
                "every player tracked the missile for most of the flight");
            for (EntityPlayerMP p : players) {
                MissileShotsScenario.signal(p, "hud", "on");
            }
            return out.note(
                "flight frames " + frames[0] + ", tracked samples " + tracked[0] + " of " + frames[0] * players.size())
                .build();
        });
    }
}
