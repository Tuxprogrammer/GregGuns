package com.gregguns.icbm.missile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;

/**
 * {@code testing.icbm_gas_ground_shots}: how well a gas cloud reads against the ground from about 30 blocks. For each
 * ground (grass, stone) and each gas it photographs (chemical, contagious), it lays a 61 by 61 floor at the world's
 * own ground level, sets off a full warhead two blocks above it, and has the first player look at the cloud from 30
 * blocks away: once at eye height, once from 12 blocks up, and once more later in the cloud's life. Labels are
 * {@code gasground-<ground>-<gas>-<eye|high|late>}. Needs one connected client; not part of {@code test all}. Dev
 * environment only, server side.
 */
// Spec: docs/gregguns-icbm-spec/07-visuals-audio/blast-visuals.md
public final class GasGroundShotsScenario implements Scenario {

    static final String NAME = "testing.icbm_gas_ground_shots";
    private static final double EYE = 1.62;
    /** Horizontal distance from the camera to the cloud center, blocks. */
    private static final double VIEW = 30.0;
    private static final int HALF = 30;
    /** Spots sit this far apart along x, so one cloud never shows in the next spot's frame. */
    private static final int SPACING = 160;
    private static final String[] GASES = { "chemical", "contagious" };
    private static final String[] GROUNDS = { "grass", "stone" };

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Photographs the chemical and contagious clouds on grass and on stone from 30 blocks (eye height, high,"
            + " late). Needs 1 client; not part of test all.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        if (!ModuleState.isActive(ModuleId.ICBM) || MissileScenarios.moduleOff()) {
            return ScenarioResult.skip(NAME, "the ICBM module is off");
        }
        @SuppressWarnings("unchecked")
        List<EntityPlayerMP> players = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        if (players.isEmpty()) {
            return ScenarioResult.fail(NAME, "needs a connected player");
        }
        final EntityPlayerMP p = players.get(0);
        final World w = ctx.world();
        p.setGameType(WorldSettings.GameType.CREATIVE);
        p.inventory.clearInventory(null, -1);
        p.clearActivePotions();
        w.setWorldTime(6000L);
        w.getGameRules()
            .setOrCreateGameRule("doDaylightCycle", "false");
        final ScenarioResult.Builder out = ScenarioResult.builder(NAME);
        final MissileShotsScenario.Plan plan = new MissileShotsScenario.Plan();
        plan.after(5, () -> MissileShotsScenario.signal(p, "hud", "off"));
        int spot = 0;
        for (String ground : GROUNDS) {
            for (String gas : GASES) {
                schedule(p, w, ctx.originX() + 300 + spot * SPACING, ctx.originZ() + 300, ground, gas, plan, out);
                spot++;
            }
        }
        plan.after(20, () -> MissileShotsScenario.signal(p, "hud", "on"));
        return ScenarioResult.pending(NAME, 9000, t -> {
            plan.run(t);
            return plan.idle() ? out.note("read the gasground-*.png shots")
                .build() : null;
        });
    }

    private static void schedule(EntityPlayerMP p, World w, int cx, int cz, String ground, String gas,
        MissileShotsScenario.Plan plan, ScenarioResult.Builder out) {
        final int[] top = { 0 };
        final String tag = ground + "-" + gas;
        // The camera sits on the diagonal, so it looks across the floor's longest run.
        final double d = VIEW / Math.sqrt(2.0);
        plan.after(20, () -> {
            // Park the player first so the chunks load, then lay the floor at the world's own ground level.
            MissileShotsScenario.cam(p, cx + 0.5 - d, 120, cz + 0.5 - d, cx + 0.5, 60, cz + 0.5);
        });
        plan.after(60, () -> {
            top[0] = w.getHeightValue(cx, cz);
            Block floor = "grass".equals(ground) ? Blocks.grass : Blocks.stone;
            for (int a = -HALF; a <= HALF; a++) {
                for (int b = -HALF; b <= HALF; b++) {
                    int x = cx + a;
                    int z = cz + b;
                    w.setBlock(x, top[0] - 2, z, "grass".equals(ground) ? Blocks.dirt : Blocks.stone, 0, 2);
                    w.setBlock(x, top[0] - 1, z, floor, 0, 2);
                    for (int y = 0; y <= 16; y++) {
                        w.setBlock(x, top[0] + y, z, Blocks.air, 0, 2);
                    }
                }
            }
            MissileShotsScenario
                .cam(p, cx + 0.5 - d, top[0] + EYE, cz + 0.5 - d, cx + 0.5, top[0] + 2.0, cz + 0.5);
        });
        plan.after(120, () -> {
            ExplosiveDef def = Explosives.get(gas);
            out.check(
                IcbmBlasts.detonate(w, cx + 0.5, top[0] + 2.0, cz + 0.5, def, def.maxFill(), null),
                tag + " blast accepted (radius " + def.radius(def.maxFill()) + ")");
        });
        plan.after(60, () -> MissileShotsScenario.signal(p, "shot", "gasground-" + tag + "-eye"));
        plan.after(40, () -> MissileShotsScenario.cam(p, cx + 0.5 - d, top[0] + 12.0, cz + 0.5 - d, cx + 0.5, top[0] + 2.0, cz + 0.5));
        plan.after(50, () -> MissileShotsScenario.signal(p, "shot", "gasground-" + tag + "-high"));
        plan.after(40, () -> MissileShotsScenario.cam(p, cx + 0.5 - d, top[0] + EYE, cz + 0.5 - d, cx + 0.5, top[0] + 2.0, cz + 0.5));
        plan.after(160, () -> MissileShotsScenario.signal(p, "shot", "gasground-" + tag + "-late"));
        plan.after(40, () -> {});
    }
}
