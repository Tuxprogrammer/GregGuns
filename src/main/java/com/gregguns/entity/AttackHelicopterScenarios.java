package com.gregguns.entity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioPoller;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.NpcEntityScenarios.ShotLog;
import com.gregguns.entity.npc.AttackHelicopter;
import com.gregguns.entity.npc.HasNpcFaction;
import com.gregguns.entity.npc.HelicopterRules;
import com.gregguns.entity.npc.ItemNpcEgg;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.projectile.GenericProjectile;
import com.gregguns.projectile.RocketProjectile;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Scenarios of the Attack Helicopter (M7 B4): its stats from a spawn egg, flight without falling, fire at a player and
 * the crash. Dev environment only. Server side. Each one removes what it spawned, cancels drop events so no item
 * entities stay in the world, and restores the world difficulty (Peaceful would remove the helicopter).
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (16. Attack Helicopter), ai.md (section 6)
final class AttackHelicopterScenarios {

    /** Blocks above the origin the helicopter starts at, well clear of the ground. */
    private static final int ALTITUDE = 30;
    /** Ticks to wait before hurting a freshly spawned NPC. */
    private static final int SETTLE_TICKS = 5;

    private AttackHelicopterScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new SpawnStatsScenario());
        registry.register(new AirborneScenario());
        registry.register(new FiresScenario());
        registry.register(new CrashScenario());
    }

    // --- helpers ----------------------------------------------------------------------------------------------

    private static EnumDifficulty allowMobs(World world) {
        EnumDifficulty prior = world.difficultySetting;
        if (prior == EnumDifficulty.PEACEFUL) {
            world.difficultySetting = EnumDifficulty.NORMAL;
        }
        return prior;
    }

    /** Restores the difficulty when the scenario answers, throws or runs out of ticks. */
    private static ScenarioResult guarded(String name, World world, EnumDifficulty prior, int timeout,
        ScenarioPoller poller) {
        return ScenarioResult.pending(name, timeout, ticks -> {
            boolean done = true;
            try {
                ScenarioResult answer = poller.poll(ticks);
                done = answer != null || ticks >= timeout;
                return answer;
            } finally {
                if (done) {
                    world.difficultySetting = prior;
                }
            }
        });
    }

    /** Spawns the helicopter through the egg path, high above the origin. */
    private static AttackHelicopter helicopter(ScenarioContext ctx) {
        Object spawned = ItemNpcEgg.spawn(
            ctx.world(),
            NpcMob.ATTACK_HELICOPTER,
            ctx.originX() + 0.5,
            ctx.originY() + ALTITUDE,
            ctx.originZ() + 0.5,
            null);
        return (AttackHelicopter) spawned;
    }

    /** A player the tracker can serve (see NpcEntityScenarios); sturdy enough to survive a whole cycle. */
    private static EntityPlayer player(ScenarioContext ctx, String name, double dx, double dz) {
        FakePlayer p = new FakePlayer(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
        p.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5 + dz, 0.0F, 0.0F);
        p.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(1000.0);
        p.setHealth(1000.0F);
        return ctx.spawn(p);
    }

    /** Cancels and records the drops of every helicopter, so no item entity reaches the world. */
    public static final class DropCatcher {

        final List<List<ItemStack>> kills = new ArrayList<>();

        @SubscribeEvent
        public void onDrops(LivingDropsEvent event) {
            if (event.entityLiving instanceof AttackHelicopter) {
                List<ItemStack> stacks = new ArrayList<>();
                for (EntityItem e : event.drops) {
                    stacks.add(e.getEntityItem());
                }
                kills.add(stacks);
                event.setCanceled(true);
            }
        }

        static DropCatcher start() {
            DropCatcher catcher = new DropCatcher();
            MinecraftForge.EVENT_BUS.register(catcher);
            return catcher;
        }

        void stop() {
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    // --- scenarios --------------------------------------------------------------------------------------------

    /** The egg spawns a 100-health, 10 by 4 boss that is hostile to turrets but in no faction. */
    private static final class SpawnStatsScenario implements Scenario {

        @Override
        public String name() {
            return "npc.attack_helicopter.spawn_stats";
        }

        @Override
        public String description() {
            return "The egg path spawns an Attack Helicopter with 100 health, a 10 by 4 hitbox, 50 XP, no faction,"
                + " IMob and boss bar markers, and a persistent, non-despawning profile.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = allowMobs(ctx.world());
            try {
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                AttackHelicopter heli = helicopter(ctx);
                out.check(heli != null, "the egg path should spawn the helicopter");
                if (heli == null) {
                    return out.build();
                }
                try {
                    out.check(heli.getMaxHealth() == 100.0F, "max health should be 100, was " + heli.getMaxHealth());
                    out.check(heli.getHealth() == 100.0F, "health should be 100, was " + heli.getHealth());
                    out.check(heli.width == 10.0F && heli.height == 4.0F, "size should be 10 x 4");
                    out.check(!(heli instanceof HasNpcFaction), "the helicopter is in no faction");
                    out.check(heli instanceof IMob, "the helicopter should be an IMob");
                    out.check(heli instanceof IBossDisplayData, "the helicopter should show a boss bar");
                    out.check(
                        net.minecraft.entity.EntityList.getEntityString(heli)
                            .equals(NpcMob.ATTACK_HELICOPTER.fullName()),
                        "it should be registered as " + NpcMob.ATTACK_HELICOPTER.fullName());
                    out.check(!heli.isAiming(), "an idle helicopter is not aiming");
                    out.check(heli.deathProgress(0.0F) == 0.0F, "death progress starts at 0");
                } finally {
                    ctx.despawn(heli);
                }
                return out.build();
            } finally {
                ctx.world().difficultySetting = prior;
            }
        }
    }

    /** With nobody around it flies, never lands and takes no fall damage. */
    private static final class AirborneScenario implements Scenario {

        private static final int TICKS = 120;

        @Override
        public String name() {
            return "npc.attack_helicopter.airborne";
        }

        @Override
        public String description() {
            return "An idle helicopter flies to waypoints for 120 ticks without touching the ground or losing health,"
                + " and a fall of 30 blocks hurts nothing.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = allowMobs(ctx.world());
            AttackHelicopter heli = helicopter(ctx);
            if (heli == null) {
                ctx.world().difficultySetting = prior;
                return ScenarioResult.fail(name(), "the egg path should spawn the helicopter");
            }
            boolean[] landed = { false };
            double[] low = { Double.MAX_VALUE };
            double[] travelled = { 0 };
            double startX = heli.posX;
            double startZ = heli.posZ;
            return guarded(name(), ctx.world(), prior, TICKS + 20, ticks -> {
                landed[0] |= heli.onGround;
                low[0] = Math.min(low[0], heli.posY);
                travelled[0] = Math.max(travelled[0], Math.hypot(heli.posX - startX, heli.posZ - startZ));
                if (ticks < TICKS) {
                    return null;
                }
                heli.fall(30.0F);
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(!heli.isDead, "the helicopter should still be alive");
                out.check(!landed[0], "the helicopter should never touch the ground");
                out.check(
                    low[0] > ctx.originY() + HelicopterRules.MIN_ALTITUDE - 2,
                    "it should hold altitude, lowest was " + (low[0] - ctx.originY()) + " above the origin");
                // Full health, not a literal 100: EnderZoo rescales a joining hostile's max health by the difficulty
                // (0.9 on
                // easy, which these scenarios set) and heals it to the new maximum.
                out.check(
                    heli.getHealth() == heli.getMaxHealth(),
                    "no fall damage, health was " + heli.getHealth() + " of " + heli.getMaxHealth());
                out.check(travelled[0] > 0.5, "it should fly toward its waypoints, moved " + travelled[0]);
                out.check(heli.attackCounter() == 0, "with no target the attack counter stays 0");
                ctx.despawn(heli);
                return out.build();
            });
        }
    }

    /** At a player in range it fires seven bullets and a rocket, with itself as the shooter. */
    private static final class FiresScenario implements Scenario {

        @Override
        public String name() {
            return "npc.attack_helicopter.fires";
        }

        @Override
        public String description() {
            return "Against a player 30 blocks away the helicopter locks on, aims, and fires a burst of machine-gun"
                + " bullets followed by a rocket; every shot names the helicopter as shooter (NPC-11).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = allowMobs(ctx.world());
            AttackHelicopter heli = helicopter(ctx);
            if (heli == null) {
                ctx.world().difficultySetting = prior;
                return ScenarioResult.fail(name(), "the egg path should spawn the helicopter");
            }
            heli.setPosition(ctx.originX() + 0.5, ctx.originY() + 25, ctx.originZ() + 0.5);
            EntityPlayer target = player(ctx, "heli_target", 30, 0);
            ShotLog log = ShotLog.start();
            boolean[] aimed = { false };
            return guarded(name(), ctx.world(), prior, 400, ticks -> {
                aimed[0] |= heli.isAiming();
                int rockets = 0;
                int bullets = 0;
                boolean allShooter = true;
                for (GenericProjectile p : log.shots) {
                    if (p.getShooter() != heli) {
                        allShooter = false;
                        continue;
                    }
                    if (p instanceof RocketProjectile) {
                        rockets++;
                    } else {
                        bullets++;
                    }
                }
                if (rockets < 1 || bullets < 7) {
                    return null;
                }
                log.stop();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(heli.getAttackTarget() == target, "the helicopter should target the player");
                out.check(aimed[0], "the helicopter should be aiming while it engages");
                out.check(allShooter, "every shot should name the helicopter as its shooter");
                out.check(bullets >= 7, "a burst is seven bullets, saw " + bullets);
                out.check(rockets >= 1, "the burst ends with a rocket, saw " + rockets);
                out.check(!heli.isDead, "the helicopter should survive its own volley");
                ctx.despawn(heli);
                ctx.despawn(target);
                return out.build();
            });
        }
    }

    /** Killing it starts a 100-tick crash: progress climbs to 1, the entity vanishes and its drops roll once. */
    private static final class CrashScenario implements Scenario {

        @Override
        public String name() {
            return "npc.attack_helicopter.crash";
        }

        @Override
        public String description() {
            return "A killed helicopter stays alive in a dying state while deathProgress rises 0 to 1 over 100 ticks,"
                + " sinks as it spins, then is removed; its fixed drops roll once (cancelled).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = allowMobs(ctx.world());
            AttackHelicopter heli = helicopter(ctx);
            if (heli == null) {
                ctx.world().difficultySetting = prior;
                return ScenarioResult.fail(name(), "the egg path should spawn the helicopter");
            }
            DropCatcher drops = DropCatcher.start();
            float[] last = { 0.0F };
            float[] peak = { 0.0F };
            boolean[] monotonic = { true };
            boolean[] aliveWhileDying = { true };
            double[] startY = { 0.0 };
            int[] killedAt = { -1 };
            return guarded(name(), ctx.world(), prior, 400, ticks -> {
                if (ticks == SETTLE_TICKS) {
                    startY[0] = heli.posY;
                    heli.attackEntityFrom(DamageSource.generic, 1000.0F);
                    killedAt[0] = ticks;
                }
                if (killedAt[0] >= 0 && !heli.isDead) {
                    float now = heli.deathProgress(0.0F);
                    monotonic[0] &= now >= last[0];
                    last[0] = now;
                    peak[0] = Math.max(peak[0], now);
                    aliveWhileDying[0] &= heli.getHealth() <= 0.0F;
                }
                if (!heli.isDead || killedAt[0] < 0) {
                    return null;
                }
                drops.stop();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(monotonic[0], "death progress should never fall");
                out.check(peak[0] >= 0.95F, "death progress should reach 1, peaked at " + peak[0]);
                out.check(aliveWhileDying[0], "the helicopter should stay a dying entity until it is removed");
                int span = ticks - killedAt[0];
                out.check(
                    span >= HelicopterRules.CRASH_TICKS - 2 && span <= HelicopterRules.CRASH_TICKS + 3,
                    "the crash should take about 100 ticks, took " + span);
                out.check(heli.posY < startY[0] - 5.0, "it should sink while crashing");
                out.check(drops.kills.size() == 1, "the drops should roll exactly once, rolled " + drops.kills.size());
                if (drops.kills.size() == 1) {
                    int iron = 0;
                    for (ItemStack s : drops.kills.get(0)) {
                        if (s.getItem() == Items.iron_ingot || net.minecraft.block.Block.getBlockFromItem(s.getItem())
                            == net.minecraft.init.Blocks.iron_block) {
                            iron += s.stackSize;
                        }
                    }
                    out.check(iron >= 4 && iron <= 7, "4 to 7 iron blocks should drop, saw " + iron);
                }
                return out.build();
            });
        }
    }
}
