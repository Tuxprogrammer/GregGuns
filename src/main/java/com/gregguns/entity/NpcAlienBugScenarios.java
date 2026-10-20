package com.gregguns.entity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.AlienBug;
import com.gregguns.entity.npc.ItemNpcEgg;
import com.gregguns.entity.npc.NpcFaction;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.NpcRegistry;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Scenarios of the Alien Bug (M7 B4). Dev environment only. Server side. Every entity a scenario adds is removed, and
 * kill simulations cancel their drop events so no item entity reaches the world.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (15. Alien Bug),
// docs/gregguns-spec/10-entities-npcs/infernal-mobs.md
final class NpcAlienBugScenarios {

    /** Ticks to wait before targeting a freshly spawned NPC. */
    private static final int SETTLE_TICKS = 5;

    private NpcAlienBugScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new SpawnScenario());
        registry.register(new StatsScenario());
        registry.register(new HitboxFactionScenario());
        registry.register(new MeleeScenario());
    }

    /** Cancels and counts the drops of simulated bug kills. */
    public static final class DropCanceler {

        int events;
        int stacks;
        boolean foreign;

        @SubscribeEvent
        public void onDrops(LivingDropsEvent event) {
            if (event.entityLiving instanceof AlienBug) {
                events++;
                event.setCanceled(true);
                for (EntityItem item : event.drops) {
                    stacks++;
                    String name = Item.itemRegistry.getNameForObject(
                        item.getEntityItem()
                            .getItem());
                    if (!"minecraft:string".equals(name) && !"minecraft:spider_eye".equals(name)) {
                        foreign = true;
                    }
                }
            }
        }
    }

    /** The egg spawns the bug at the block center with no rider, and it is registered under its catalog name. */
    private static final class SpawnScenario implements Scenario {

        @Override
        public String name() {
            return "npc.alien_bug.spawn";
        }

        @Override
        public String description() {
            return "The Alien Bug is registered as gregguns.AlienBug (ID 66); its egg spawns it at the neighbor block"
                + " center, consumes one egg, and leaves no rider or stale entity.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            ctx.clearArea(4, 4);
            ctx.setBlock(0, -1, 0, Blocks.stone, 0);
            out.check(NpcRegistry.isRegistered(NpcMob.ALIEN_BUG), "the Alien Bug should be registered");
            out.check(
                EntityList.createEntityByName(NpcMob.ALIEN_BUG.fullName(), world) instanceof AlienBug,
                "EntityList should create an AlienBug");
            ItemNpcEgg egg = NpcRegistry.egg();
            out.check(egg != null, "the npc_egg item should be registered");
            if (egg == null) {
                return out.build();
            }
            int before = world.loadedEntityList.size();
            FakePlayer player = new FakePlayer(
                (WorldServer) world,
                new GameProfile(UUID.nameUUIDFromBytes("alien_bug_egg".getBytes(StandardCharsets.UTF_8)), "ab_egg"));
            ItemStack stack = new ItemStack(egg, 2, NpcMob.ALIEN_BUG.entityId());
            int x = ctx.originX();
            int y = ctx.originY() - 1;
            int z = ctx.originZ();
            boolean used = egg.onItemUse(stack, player, world, x, y, z, 1, 0.5F, 1.0F, 0.5F);
            out.check(used, "using the Alien Bug egg should succeed");
            out.checkEquals("egg stack after one use", 1, stack.stackSize);
            AlienBug bug = null;
            int bugs = 0;
            for (Object o : world.loadedEntityList) {
                if (o instanceof AlienBug && !((AlienBug) o).isDead) {
                    bug = (AlienBug) o;
                    bugs++;
                }
            }
            out.checkEquals("Alien Bugs in the world", 1, bugs);
            out.checkEquals("entities added", 1, world.loadedEntityList.size() - before);
            if (bug != null) {
                out.check(Math.abs(bug.posX - (x + 0.5)) < 1e-6, "X should be the block center: " + bug.posX);
                out.check(Math.abs(bug.posZ - (z + 0.5)) < 1e-6, "Z should be the block center: " + bug.posZ);
                out.check(bug.riddenByEntity == null, "an egg bug should have no jockey");
                world.removeEntity(bug);
            }
            return out.build();
        }
    }

    /** Attributes are the catalog values at every difficulty; kill drops stay inside the profile. */
    private static final class StatsScenario implements Scenario {

        @Override
        public String name() {
            return "npc.alien_bug.stats";
        }

        @Override
        public String description() {
            return "A fresh Alien Bug has 20 max health, 4 attack, 1.0 speed and 16 follow range at every difficulty"
                + " (GGS-10-29), and a canceled-drop kill simulation yields only string and spider eyes.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            EnumDifficulty prior = world.difficultySetting;
            DropCanceler drops = new DropCanceler();
            MinecraftForge.EVENT_BUS.register(drops);
            try {
                for (EnumDifficulty d : EnumDifficulty.values()) {
                    world.difficultySetting = d;
                    AlienBug bug = new AlienBug(world);
                    out.checkEquals(
                        "max health at " + d,
                        20.0,
                        bug.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                            .getBaseValue());
                    out.checkEquals(
                        "attack at " + d,
                        4.0,
                        bug.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                            .getBaseValue());
                    out.checkEquals(
                        "speed at " + d,
                        1.0,
                        bug.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                            .getBaseValue());
                    out.checkEquals(
                        "follow range at " + d,
                        16.0,
                        bug.getEntityAttribute(SharedMonsterAttributes.followRange)
                            .getBaseValue());
                    out.checkEquals("health at " + d, 20.0F, bug.getHealth());
                }
                world.difficultySetting = EnumDifficulty.NORMAL;
                int kills = 60;
                for (int i = 0; i < kills; i++) {
                    AlienBug bug = new AlienBug(world);
                    bug.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
                    bug.onDeath(DamageSource.generic);
                }
                out.checkEquals("drop events", kills, drops.events);
                out.check(drops.stacks > 0, "60 kills should produce some drops");
                out.check(!drops.foreign, "drops should be only string and spider eyes");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(drops);
                world.difficultySetting = prior;
            }
            return out.build();
        }
    }

    /** Hitbox, faction, base class and animation defaults. */
    private static final class HitboxFactionScenario implements Scenario {

        @Override
        public String name() {
            return "npc.alien_bug.hitbox_faction";
        }

        @Override
        public String description() {
            return "The Alien Bug is a hostile-faction IMob spider with a 1.1 x 1.2 hitbox that never aims and has an"
                + " idle attack clock.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            AlienBug bug = new AlienBug(ctx.world());
            out.checkEquals("width", 1.1F, bug.width);
            out.checkEquals("height", 1.2F, bug.height);
            out.checkEquals("faction", NpcFaction.HOSTILE, bug.npcFaction());
            out.check(bug instanceof IMob, "the bug should be an IMob (infernal eligible)");
            out.check(bug instanceof EntitySpider, "the bug should be a spider (wall climbing)");
            out.check(!bug.isAiming(), "the bug never aims");
            out.checkEquals("idle attack progress", 0.0F, bug.attackProgress(0.0F));
            out.checkEquals("bite clock", 0, bug.attackTimer());
            return out.build();
        }
    }

    /** The bug walks up to a target and bites: damage lands and the bite clock runs. */
    private static final class MeleeScenario implements Scenario {

        private static final int TIMEOUT = 300;

        @Override
        public String name() {
            return "npc.alien_bug.melee";
        }

        @Override
        public String description() {
            return "An Alien Bug hunting a pig 5 blocks away closes in and bites for its 4 attack damage; the bite"
                + " clock starts and drives attackProgress within 0 to 1.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            World world = ctx.world();
            EnumDifficulty prior = world.difficultySetting;
            if (prior == EnumDifficulty.PEACEFUL) {
                world.difficultySetting = EnumDifficulty.NORMAL;
            }
            ctx.clearArea(14, 4);
            for (int fx = -2; fx <= 8; fx++) {
                for (int fz = -3; fz <= 3; fz++) {
                    ctx.setBlock(fx, -1, fz, Blocks.stone, 0);
                }
            }
            AlienBug bug = new AlienBug(world);
            bug.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            ctx.spawn(bug);
            EntityPig victim = new EntityPig(world);
            victim.setLocationAndAngles(ctx.originX() + 5.5, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
            ctx.spawn(victim);
            float start = victim.getHealth();
            boolean[] sawClock = { false };
            float[] progress = { -1.0F };
            return ScenarioResult.pending(name(), TIMEOUT, ticks -> {
                boolean finished = false;
                try {
                    if (ticks < SETTLE_TICKS) {
                        return null;
                    }
                    if (ticks == SETTLE_TICKS) {
                        // A pig, not a fake player: Forge FakePlayers are invulnerable. The melee task only fights back
                        // against a revenge target or a player, so the pig is the revenge target.
                        bug.setRevengeTarget(victim);
                        bug.setAttackTarget(victim);
                        return null;
                    }
                    if (bug.attackTimer() > 0 && !sawClock[0]) {
                        sawClock[0] = true;
                        progress[0] = bug.attackProgress(0.0F);
                    }
                    if (victim.getHealth() >= start) {
                        if (ticks >= TIMEOUT - 1) {
                            finished = true;
                            return ScenarioResult.fail(
                                name(),
                                "the bug never hurt the target: target=" + bug.getAttackTarget()
                                    + " dist="
                                    + bug.getDistanceToEntity(victim)
                                    + " timer="
                                    + bug.attackTimer()
                                    + " path="
                                    + !bug.getNavigator()
                                        .noPath()
                                    + " dead="
                                    + bug.isDead);
                        }
                        return null;
                    }
                    finished = true;
                    ScenarioResult.Builder out = ScenarioResult.builder(name());
                    out.checkEquals("damage dealt", 4.0F, start - victim.getHealth());
                    out.check(sawClock[0], "the bite clock should have started");
                    out.check(progress[0] >= 0.0F && progress[0] <= 1.0F, "attack progress in [0, 1]: " + progress[0]);
                    return out.build();
                } finally {
                    if (finished || ticks >= TIMEOUT - 1) {
                        world.removeEntity(bug);
                        world.removeEntity(victim);
                        world.difficultySetting = prior;
                    }
                }
            });
        }
    }
}
