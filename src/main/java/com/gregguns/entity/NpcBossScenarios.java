package com.gregguns.entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.NpcEntityScenarios.ShotLog;
import com.gregguns.entity.npc.DictatorDave;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcFaction;
import com.gregguns.entity.npc.NpcGunProfile;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.PsychoSteve;
import com.gregguns.item.gun.ItemGun;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * Scenarios of the two mini-bosses, Psycho Steve and Dictator Dave (M7 B3). Dev environment only. Server side.
 *
 * <p>
 * Everything spawned goes through the context so it is removed afterwards; nothing here kills a mob, so no drops
 * exist. Scenarios that need hostile mobs lift a Peaceful world and restore it.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (sections 12 and 13), docs/techguns-spec/BUGS.md (NPC-22,
// NPC-28)
final class NpcBossScenarios {

    private static final int REMOTE_BLOCKS = 140;

    private NpcBossScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new GearScenario());
        registry.register(new SteveAttackScenario());
        registry.register(new DaveAttackScenario());
        registry.register(new PersistScenario());
    }

    /** Builds a boss the way an egg does, rolls its gear at danger 3 and spawns it through the context. */
    private static <T extends EntityLiving> T spawnBoss(ScenarioContext ctx, NpcMob mob, Class<T> type, double dx,
        double dz) {
        Entity e = EntityList.createEntityByName(mob.fullName(), ctx.world());
        if (!type.isInstance(e)) {
            return null;
        }
        T boss = type.cast(e);
        boss.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5 + dz, 0.0F, 0.0F);
        if (boss instanceof NpcEntity) {
            ((NpcEntity) boss).setDanger(3);
            ((NpcEntity) boss).rollEquipment();
        }
        return ctx.spawn(boss);
    }

    private static String heldGun(EntityLiving mob) {
        ItemStack held = mob.getHeldItem();
        return held != null && held.getItem() instanceof ItemGun ? ((ItemGun) held.getItem()).contentName() : null;
    }

    /**
     * A fake player that records hits from {@code attacker}. Forge's FakePlayer is invulnerable, which rejects every
     * hit before any health change or event, so this one overrides that and counts the hits itself.
     */
    private static final class Victim extends FakePlayer {

        int hits;
        float lastDamage;

        Victim(WorldServer world, GameProfile profile) {
            super(world, profile);
        }

        @Override
        public boolean isEntityInvulnerable() {
            return false;
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            if (source.getEntity() instanceof PsychoSteve) {
                hits++;
                lastDamage = amount;
            }
            return false;
        }
    }

    private static Victim victim(ScenarioContext ctx, String name, double dx, double dz) {
        Victim p = new Victim(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        new NetHandlerPlayServer(MinecraftServer.getServer(), new NetworkManager(false), p);
        p.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5 + dz, 0.0F, 0.0F);
        return ctx.spawn(p);
    }

    /** Each boss spawns with its stats, its weapon and armor, its faction, and the boss-bar interface (or none). */
    private static final class GearScenario implements Scenario {

        @Override
        public String name() {
            return "npc.bosses_gear";
        }

        @Override
        public String description() {
            return "Psycho Steve (75 HP, chainsaw, miner armor, boss bar) and Dictator Dave (100 HP, golden revolver,"
                + " no boss bar) spawn with their gear and are hostile.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.clearArea(8, 4);
            PsychoSteve steve = spawnBoss(ctx, NpcMob.PSYCHO_STEVE, PsychoSteve.class, 0, 0);
            DictatorDave dave = spawnBoss(ctx, NpcMob.DICTATOR_DAVE, DictatorDave.class, 4, 0);
            out.check(steve != null, "gregguns.PsychoSteve should create a PsychoSteve");
            out.check(dave != null, "gregguns.DictatorDave should create a DictatorDave");
            if (steve == null || dave == null) {
                return out.build();
            }
            out.checkEquals("steve health", 75.0, (double) steve.getMaxHealth());
            out.checkEquals("dave health", 100.0, (double) dave.getMaxHealth());
            out.checkEquals("steve weapon", "chainsaw", heldGun(steve));
            out.checkEquals("dave weapon", "golden_revolver", heldGun(dave));
            out.check(steve.getEquipmentInSlot(3) != null, "steve should wear a chestplate (miner armor)");
            out.check(dave.getEquipmentInSlot(4) != null, "dave should wear a helmet (beret)");
            out.check(steve.npcFaction() == NpcFaction.HOSTILE, "steve is hostile");
            out.check(dave.npcFaction() == NpcFaction.HOSTILE, "dave is hostile");
            // The boss bar itself is client only (BossStatus); the server-checkable part is the interface the client
            // handler keys on. Dave has no bar (per-mob.md section 13).
            out.check(steve instanceof IBossDisplayData, "steve should be a boss-bar entity (NPC-22)");
            out.check(!(dave instanceof IBossDisplayData), "dave has no boss bar");
            return out.build();
        }
    }

    /** Steve walks up to a player and chainsaws them. */
    private static final class SteveAttackScenario implements Scenario {

        private static final int TIMEOUT = 300;

        @Override
        public String name() {
            return "npc.steve_attacks";
        }

        @Override
        public String description() {
            return "Psycho Steve closes on a player and lands a chainsaw hit, hurting them.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = NpcEntityScenarios.allowMobs(ctx.world());
            ctx.clearArea(14, 4);
            PsychoSteve steve = spawnBoss(ctx, NpcMob.PSYCHO_STEVE, PsychoSteve.class, 0, 0);
            // The pathfinder builds a chunk cache of followRange + 8 blocks and returns no path when it reaches an
            // unloaded chunk (GTNH). Alone, only the arena is loaded, so Steve (follow range 60) never walked. Keep the
            // cache inside the held arena; the victim is 6 blocks away.
            steve.getEntityAttribute(SharedMonsterAttributes.followRange)
                .setBaseValue(8.0);
            Victim victim = victim(ctx, "boss_steve_victim", 6, 0);
            return NpcEntityScenarios.guarded(name(), ctx.world(), prior, TIMEOUT, ticks -> {
                if (ticks == TIMEOUT - 2 && victim.hits == 0) {
                    return ScenarioResult.fail(
                        name(),
                        "steve never hit the victim: alive=" + !steve.isDead
                            + ", distance="
                            + steve.getDistanceToEntity(victim)
                            + ", target="
                            + steve.getAttackTarget()
                            + ", sees="
                            + steve.canEntityBeSeen(victim)
                            + ", noPath="
                            + steve.getNavigator()
                                .noPath()
                            + ", pathNow="
                            + steve.getNavigator()
                                .getPathToEntityLiving(victim)
                            + ", pos="
                            + steve.posX
                            + ","
                            + steve.posY
                            + ","
                            + steve.posZ);
                }
                if (victim.hits == 0) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(steve.getAttackTarget() == victim, "steve should have the player as target");
                out.checkEquals("chainsaw hit damage", PsychoSteve.ATTACK_DAMAGE, (double) victim.lastDamage);
                return out.build();
            });
        }
    }

    /** Dave fires his revolver in bursts at a player in range. */
    private static final class DaveAttackScenario implements Scenario {

        private static final int TIMEOUT = 400;

        @Override
        public String name() {
            return "npc.dave_attacks";
        }

        @Override
        public String description() {
            return "Dictator Dave fires real golden-revolver projectiles at a player in range, a burst delay apart.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EnumDifficulty prior = NpcEntityScenarios.allowMobs(ctx.world());
            ctx.clearArea(14, 4);
            DictatorDave dave = spawnBoss(ctx, NpcMob.DICTATOR_DAVE, DictatorDave.class, 0, 0);
            NpcEntityScenarios.player(ctx, "boss_dave_victim", 10, 0);
            NpcGunProfile profile = NpcGunProfile.forGun("golden_revolver");
            ShotLog log = ShotLog.start();
            return NpcEntityScenarios.guarded(name(), ctx.world(), prior, TIMEOUT, ticks -> {
                if (log.count(dave) < 2) {
                    return null;
                }
                log.stop();
                List<Long> at = log.ticksOf(dave);
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(dave.holdsGun(), "dave should hold a gun");
                long gap = at.get(1) - at.get(0);
                out.check(
                    gap == profile.burstDelay(),
                    "burst gap " + gap + " should equal the burst delay " + profile.burstDelay());
                return out.build();
            });
        }
    }

    /** Both bosses survive a far-away player and a save and load; an ordinary NPC does not (NPC-28). */
    private static final class PersistScenario implements Scenario {

        @Override
        public String name() {
            return "npc.bosses_persist";
        }

        @Override
        public String description() {
            return "With a player 140 blocks away the bosses do not despawn but an ordinary NPC does; both bosses keep"
                + " their gear and health through a save and load (NPC-28).";
        }

        private static void despawnTick(EntityLiving mob) {
            try {
                Method m = ReflectionHelper
                    .findMethod(EntityLiving.class, mob, new String[] { "despawnEntity", "func_70623_bb" });
                m.invoke(mob);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("could not run despawnEntity", e);
            }
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            EnumDifficulty prior = NpcEntityScenarios.allowMobs(world);
            try {
                ctx.clearArea(8, 4);
                PsychoSteve steve = spawnBoss(ctx, NpcMob.PSYCHO_STEVE, PsychoSteve.class, 0, 0);
                DictatorDave dave = spawnBoss(ctx, NpcMob.DICTATOR_DAVE, DictatorDave.class, 3, 0);
                NpcEntity control = new NpcEntity(world);
                control.setLocationAndAngles(ctx.originX() + 0.5 - 3, ctx.originY(), ctx.originZ() + 0.5, 0.0F, 0.0F);
                ctx.spawn(control);
                NpcEntityScenarios.player(ctx, "boss_far_player", REMOTE_BLOCKS, 0);
                despawnTick(steve);
                despawnTick(dave);
                despawnTick(control);
                out.check(control.isDead, "control: an ordinary NPC should despawn (is the test discriminating?)");
                out.check(!steve.isDead, "steve should not despawn");
                out.check(!dave.isDead, "dave should not despawn");

                for (EntityLiving boss : new EntityLiving[] { steve, dave }) {
                    boss.setHealth(boss.getMaxHealth() - 5.0F);
                    NBTTagCompound tag = new NBTTagCompound();
                    boss.writeToNBT(tag);
                    tag.setString("id", EntityList.getEntityString(boss));
                    Entity loaded = EntityList.createEntityFromNBT(tag, world);
                    String label = EntityList.getEntityString(boss);
                    out.check(
                        loaded != null && loaded.getClass() == boss.getClass(),
                        label + " should reload as the same class");
                    if (loaded instanceof EntityLiving) {
                        EntityLiving back = (EntityLiving) loaded;
                        out.checkEquals(label + " health after reload", boss.getHealth(), back.getHealth());
                        out.checkEquals(label + " weapon after reload", heldGun(boss), heldGun(back));
                        back.setLocationAndAngles(
                            ctx.originX() + 0.5,
                            ctx.originY(),
                            ctx.originZ() + 0.5 + 6,
                            0.0F,
                            0.0F);
                        ctx.spawn(back);
                        despawnTick(back);
                        out.check(!back.isDead, label + " should still not despawn after reload");
                    }
                }
            } finally {
                world.difficultySetting = prior;
            }
            return out.build();
        }
    }
}
