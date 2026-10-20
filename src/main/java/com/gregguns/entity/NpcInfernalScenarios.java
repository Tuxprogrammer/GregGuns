package com.gregguns.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.gregguns.api.Tier;
import com.gregguns.combat.DamageSources;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
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
        registry.register(new HooksScenario());
        registry.register(new HurtPipelineScenario());
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
