package com.gregguns.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.core.config.NpcConfig;
import com.gregguns.core.testing.PackMobGuard;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.core.testing.SkipUnless;
import com.gregguns.entity.npc.InfernalHooks;
import com.gregguns.entity.npc.NpcEntity;
import com.gregguns.entity.npc.NpcItems;
import com.gregguns.entity.npc.NpcMob;
import com.gregguns.entity.npc.loot.InfernalLootTable;
import com.gregguns.entity.npc.loot.LootDrop;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Scenarios of the Infernal Mobs hooks and the NPC damage pipeline (M7 N9). Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/10-entities-npcs/infernal-mobs.md (GGS-10-05, GGS-10-06, GGS-10-10 to GGS-10-13)
final class NpcInfernalScenarios {

    private NpcInfernalScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(
            SkipUnless.of(
                new HooksScenario(),
                () -> com.gregguns.entity.npc.NpcRegistry.isRegistered(NpcMob.NPC_TEST),
                "needs the dev-only test NPC (NpcMob.NPC_TEST), which the pack does not register"));
        registry.register(new InfernalRollScenario());
        registry.register(
            SkipUnless.of(
                new HurtPipelineScenario(),
                () -> com.gregguns.entity.npc.NpcRegistry.isRegistered(NpcMob.NPC_TEST),
                "needs the dev-only test NPC (NpcMob.NPC_TEST), which the pack does not register"));
    }

    /** Every table reference resolves; the soft link is inert without Infernal Mobs; the spawner marker is gated. */
    private static final class HooksScenario implements Scenario {

        @Override
        public String name() {
            return "npc.infernal_hooks";
        }

        @Override
        public String description() {
            return "Infernal loot table refs resolve to items, elite rolls stay at LV, and the Infernal Mobs link and"
                + " spawner opt-out do nothing without the mod.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (InfernalLootTable.Entry e : InfernalLootTable.entries()) {
                out.check(
                    NpcItems.stack(e.drop(), new Random(1)) != null,
                    "unresolved ref " + e.drop()
                        .ref());
            }
            Random rand = new Random(7);
            for (int i = 0; i < 200; i++) {
                for (LootDrop d : InfernalLootTable.roll(rand, 3, Tier.MV)) {
                    out.check(tierOf(d).ordinal() <= Tier.LV.ordinal(), "elite roll above LV: " + d.ref());
                }
            }
            World world = ctx.world();
            Entity npc = EntityList.createEntityByName(NpcMob.NPC_TEST.fullName(), world);
            if (!(npc instanceof NpcEntity)) {
                out.check(false, "the test NPC should be creatable");
                return out.build();
            }
            out.checkEquals("modifier size of a plain NPC", 0, InfernalHooks.modifierSize((NpcEntity) npc));
            if (!InfernalHooks.available()) {
                out.check(!InfernalHooks.markSpawnerNpc((NpcEntity) npc), "marker without Infernal Mobs");
                out.check(
                    npc.getEntityData()
                        .hasNoTags(),
                    "persistent data should stay untouched");
                out.note("Infernal Mobs absent: link inert");
            } else {
                out.note("Infernal Mobs present");
            }
            return out.build();
        }

        private static Tier tierOf(LootDrop d) {
            for (InfernalLootTable.Entry e : InfernalLootTable.entries()) {
                if (e.drop() == d) {
                    return e.tier();
                }
            }
            return Tier.STEAM;
        }
    }

    /**
     * With Infernal Mobs loaded, real GregGuns NPCs (the natural-spawn path: nothing pre-marks them) and NPCs that a
     * GregGuns spawner would place (the spawner opt-out is off by default) roll as infernal at Infernal's own rates,
     * and the opt-out, when on, stops the spawner path only. Never infernal: the Attack Helicopter.
     */
    private static final class InfernalRollScenario implements Scenario {

        private static final NpcMob[] KINDS = { NpcMob.ZOMBIE_SOLDIER, NpcMob.BANDIT, NpcMob.SKELETON_SOLDIER,
            NpcMob.ZOMBIE_MINER };
        private static final int SAMPLE = 400;

        @Override
        public String name() {
            return "npc.infernal_roll";
        }

        @Override
        public String description() {
            return "Natural and spawner-path GregGuns NPCs roll as infernal with Infernal Mobs loaded; the spawner"
                + " opt-out, when on, stops only the spawner path; the helicopter never rolls.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            if (!InfernalHooks.available()) {
                out.note("Infernal Mobs absent: nothing to roll (run on the real-pack harness)");
                return out.build();
            }
            boolean saved = NpcConfig.infernalSpawnerOptOut;
            try {
                NpcConfig.infernalSpawnerOptOut = false;
                int natural = countInfernal(ctx, false);
                int spawner = countInfernal(ctx, true);
                out.note("infernal of " + SAMPLE + ": natural " + natural + ", spawner path " + spawner);
                // Elite rarity 1 in 20 by default: the chance of zero in 400 is about 1e-9.
                out.check(natural > 0, "some natural-path NPCs should roll as infernal, got " + natural);
                out.check(spawner > 0, "some spawner-path NPCs should roll as infernal, got " + spawner);
                NpcConfig.infernalSpawnerOptOut = true;
                out.checkEquals("spawner-path infernal with the opt-out on", 0, countInfernal(ctx, true));
                out.check(countInfernal(ctx, false) > 0, "the opt-out must not touch the natural path");
                NpcConfig.infernalSpawnerOptOut = false;
                out.checkEquals("helicopters that rolled infernal", 0, countHelicopters(ctx));
            } finally {
                NpcConfig.infernalSpawnerOptOut = saved;
            }
            return out.build();
        }

        private static int countInfernal(ScenarioContext ctx, boolean spawnerPath) {
            World world = ctx.world();
            int infernal = 0;
            for (int i = 0; i < SAMPLE; i++) {
                Entity e = EntityList.createEntityByName(KINDS[i % KINDS.length].fullName(), world);
                if (!(e instanceof EntityLivingBase)) {
                    continue;
                }
                e.setPosition(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5);
                if (spawnerPath) {
                    // What a spawner on its origin does before the join, without the tile.
                    InfernalHooks.markSpawnerNpc((EntityLivingBase) e);
                }
                world.spawnEntityInWorld(e);
                if (InfernalHooks.modifierSize((EntityLivingBase) e) > 0) {
                    infernal++;
                }
                InfernalHooks.forget((EntityLivingBase) e);
                world.removeEntity(e);
            }
            return infernal;
        }

        private static int countHelicopters(ScenarioContext ctx) {
            World world = ctx.world();
            int infernal = 0;
            for (int i = 0; i < SAMPLE; i++) {
                Entity e = EntityList.createEntityByName(NpcMob.ATTACK_HELICOPTER.fullName(), world);
                if (!(e instanceof EntityLivingBase)) {
                    return infernal;
                }
                e.setPosition(ctx.originX() + 0.5, ctx.originY() + 20, ctx.originZ() + 0.5);
                world.spawnEntityInWorld(e);
                if (InfernalHooks.modifierSize((EntityLivingBase) e) > 0) {
                    infernal++;
                }
                InfernalHooks.forget((EntityLivingBase) e);
                world.removeEntity(e);
            }
            return infernal;
        }
    }

    /** One NPC-to-NPC ranged hit posts one LivingHurtEvent whose source is indirect with the shooter as attacker. */
    private static final class HurtPipelineScenario implements Scenario {

        @Override
        public String name() {
            return "npc.hurt_pipeline";
        }

        @Override
        public String description() {
            return "A ranged hit on an NPC posts LivingHurtEvent once, as an EntityDamageSourceIndirect whose"
                + " getEntity() is the shooter (GGS-10-05, GGS-10-06).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            World world = ctx.world();
            Entity victim = EntityList.createEntityByName(NpcMob.NPC_TEST.fullName(), world);
            Entity shooter = EntityList.createEntityByName(NpcMob.NPC_TEST.fullName(), world);
            if (victim == null || shooter == null) {
                out.check(false, "the test NPC should be creatable");
                return out.build();
            }
            victim.setPosition(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5);
            shooter.setPosition(ctx.originX() + 8.5, ctx.originY(), ctx.originZ() + 0.5);
            PackMobGuard.protect(victim);
            PackMobGuard.protect(shooter);
            world.spawnEntityInWorld(victim);
            world.spawnEntityInWorld(shooter);
            Counter counter = new Counter(victim);
            MinecraftForge.EVENT_BUS.register(counter);
            try {
                DamageSources.hit(victim, DamageType.PROJECTILE, null, shooter, 1.0F, 0.0);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(counter);
                world.removeEntity(victim);
                world.removeEntity(shooter);
            }
            out.checkEquals("LivingHurtEvent count", 1, counter.sources.size());
            if (!counter.sources.isEmpty()) {
                DamageSource source = counter.sources.get(0);
                out.check(source instanceof EntityDamageSourceIndirect, "the ranged source should be indirect");
                out.check(source.getEntity() == shooter, "getEntity() should be the shooter");
            }
            return out.build();
        }
    }

    /** Collects the sources of hurt events on one entity. */
    public static final class Counter {

        final List<DamageSource> sources = new ArrayList<>();
        private final Entity victim;

        Counter(Entity victim) {
            this.victim = victim;
        }

        /**
         * @param event the hurt event
         */
        @SubscribeEvent
        public void onHurt(LivingHurtEvent event) {
            if (event.entityLiving == victim) {
                sources.add(event.source);
            }
        }
    }
}
