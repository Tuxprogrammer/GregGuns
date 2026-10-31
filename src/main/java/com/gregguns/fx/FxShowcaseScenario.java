package com.gregguns.fx;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;

import com.gregguns.core.testing.AutotestSignal;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;

/**
 * {@code fx.showcase}: a visual check of every FX for a connected client (see {@code docs/testing/README.md}). Each
 * step spawns one effect at a fixed point in front of the (creative) player and asks the client for screenshots
 * {@code fx-<name>-<ticks>} at 2, 12 and 40 ticks after the spawn. It covers every FXLIST, then every system no list
 * uses.
 *
 * <p>
 * Before the first and after the last effect it sends the {@code fxstats} signal: the client logs Angelica's particle
 * counters (captured, spilled, undecodable) when Angelica is loaded. Under Angelica a run passes only if the
 * undecodable and spilled counts did not grow while captured did (GGS-04-17); the scenario can't see the client's log,
 * so the person or script running it reads the two {@code fxstats} lines. Not part of {@code test all}: it needs a
 * connected client. Dev environment only, server side.
 */
// Spec: docs/gregguns-spec/04-projectiles-fx/particle-rendering.md (Angelica)
final class FxShowcaseScenario implements Scenario {

    private static final String NAME = "fx.showcase";
    /**
     * Ticks per effect: the long-lived blast smoke lives 85 ticks, so the next effect starts after the 40-tick shot.
     */
    private static final int STEP = 60;
    /** Shot times after the spawn, ticks. */
    private static final int[] SHOTS = { 2, 12, 40 };
    /** Ticks before the first effect: the teleport and the stats line settle. */
    private static final int LEAD = 40;

    static void register(ScenarioRegistry registry) {
        registry.register(new FxShowcaseScenario());
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Spawns every FX list and every list-less FX system in front of a connected client and screenshots"
            + " each at +2, +12 and +40 ticks; logs Angelica's particle counters. Needs 1 client; not part of test all.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    /**
     * @param registry the FX data
     * @return every list name, sorted, then every system that no list resolves to, sorted
     */
    static List<String> effects(FxRegistry registry) {
        List<String> out = new ArrayList<>(new TreeSet<>(registry.listNames()));
        Set<ParticleSystemDef> inLists = new HashSet<>();
        for (String list : registry.listNames()) {
            inLists.addAll(registry.resolve(list));
        }
        for (String system : new TreeSet<>(registry.systemNames())) {
            if (!inLists.contains(registry.system(system))) {
                out.add(system);
            }
        }
        return out;
    }

    @Override
    public ScenarioResult run(ScenarioContext ctx) {
        List<EntityPlayerMP> players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        if (players.isEmpty()) {
            return ScenarioResult.fail(NAME, "needs a connected player");
        }
        EntityPlayerMP player = players.get(0);
        List<String> effects = effects(FxResources.shared());
        if (effects.isEmpty()) {
            return ScenarioResult.fail(NAME, "no FX loaded");
        }
        ctx.clearArea(10, 12);
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
            }
        }
        player.inventory.clearInventory(null, -1);
        player.inventoryContainer.detectAndSendChanges();
        player.setGameType(WorldSettings.GameType.CREATIVE);
        long priorTime = player.worldObj.getWorldTime();
        player.worldObj.setWorldTime(6000L);
        double cx = ctx.originX() + 0.5;
        double cy = ctx.originY();
        double cz = ctx.originZ() + 0.5;
        signal(player, "hud", "off");
        signal(player, "view", "0");
        return ScenarioResult.pending(NAME, LEAD + effects.size() * STEP + 200, t -> {
            if (t % 20 == 0) {
                // The player stands 9 blocks south of the effect, looking north at it.
                player.playerNetServerHandler.setPlayerLocation(cx, cy, cz + 9.0, 180.0F, -8.0F);
            }
            if (t == LEAD / 2) {
                signal(player, "fxstats", "before");
            }
            int k = t - LEAD;
            if (k < 0) {
                return null;
            }
            int index = k / STEP;
            int phase = k % STEP;
            if (index >= effects.size()) {
                if (phase == 0) {
                    signal(player, "fxstats", "after");
                }
                if (phase < 20) {
                    return null;
                }
                player.worldObj.setWorldTime(priorTime);
                signal(player, "hud", "on");
                player.setGameType(WorldSettings.GameType.SURVIVAL);
                return ScenarioResult.builder(NAME)
                    .note(
                        effects.size() + " effects, "
                            + effects.size() * SHOTS.length
                            + " shots sent; read the client screenshots fx-*.png and its two fxstats lines")
                    .build();
            }
            String effect = effects.get(index);
            if (phase == 0) {
                FxSender.at(player.worldObj, effect, cx, cy + 2.0, cz, FxSender.RANGE_NEAR);
            }
            for (int at : SHOTS) {
                if (phase == at) {
                    signal(player, "shot", "fx-" + effect + "-" + at);
                }
            }
            return null;
        });
    }

    private static void signal(EntityPlayerMP player, String command, String... args) {
        player.addChatMessage(new ChatComponentText(AutotestSignal.format(command, args)));
    }
}
