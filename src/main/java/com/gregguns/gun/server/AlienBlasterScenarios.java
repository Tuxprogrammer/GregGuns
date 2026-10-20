package com.gregguns.gun.server;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ExplosionEvent;

import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.fx.FxResources;
import com.gregguns.gun.def.AlienBlasterDef;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.logic.SpreadModel;
import com.gregguns.item.gun.GunItems;
import com.gregguns.projectile.AlienBlasterProjectile;
import com.gregguns.projectile.Projectiles;
import com.gregguns.recipe.GunRecipeScenarios;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.EntityRegistry;

/**
 * The {@code gun.alien_blaster.*} scenarios: the full-auto fire that spends exactly one shot's EU per bolt, the fire
 * damage and burn of a hit, the tiny explosion that never breaks a block (and the face fire a player's bolt starts),
 * the trail FX that is really sent for a registered, tracked bolt (PFX-01), and the LuV assembly row with its tier
 * gate. Through the same server handler as the other guns, using {@link GunRig}. Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Alien Blaster); docs/techguns-spec/04-projectiles-fx/
// projectiles.md (3.9); explosions.md; docs/gregguns-spec/02-guns/energy-electric-items.md; docs/testing/README.md
public final class AlienBlasterScenarios {

    private static final GunStats STATS = AlienBlasterDef.STATS;
    private static final int HELD_TICKS = 17;
    private static final double TOLERANCE = 0.25;
    /** The most the burning of a hit target may add on top of the bolt within the scenario's few ticks. */
    private static final double BURN_ALLOWANCE = 1.25;

    /** The explosions started by alien bolts, recorded by {@link Recorder}: power, then 1 if it breaks blocks. */
    private static final List<float[]> BLASTS = new ArrayList<>();
    private static boolean recorderRegistered;

    private AlienBlasterScenarios() {}

    /**
     * Registers the Alien Blaster scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fire());
        registry.register(new Hit());
        registry.register(new Blast());
        registry.register(new Trail());
        registry.register(
            GunRecipeScenarios.assembly(
                "gun.alien_blaster.recipe",
                "The LuV assembly row (GR-95) is in GT's assembler map (so NEI lists it) with its sheet inputs, not below LuV, and the found recipe's output is ours.",
                AlienBlasterDef.DEFINITION.assembly()));
        registry.register(
            GunRecipeScenarios.tierGate(
                "gun.alien_blaster.tiergate",
                "The Alien Blaster row carries HSS-G, the LuV gate material; a LuV assembler finds it and an IV assembler does not.",
                AlienBlasterDef.DEFINITION.assembly()));
    }

    private static GunRig chargedRig(ScenarioContext ctx, int shots, int corridor) {
        GunRig rig = new GunRig(ctx, GunItems.get(AlienBlasterDef.NAME), 0, corridor);
        ElectricGear.charge(rig.gun, (double) STATS.euPerShot() * shots, AlienBlasterDef.DEFINITION.tier());
        return rig;
    }

    private static long stored(ItemStack gun) {
        return (long) Math.floor(ElectricGear.charge(gun));
    }

    /** Adds every alien bolt in the world that is not in {@code before} or {@code seen}; returns the new ones. */
    private static List<AlienBlasterProjectile> newBolts(ScenarioContext ctx, Map<AlienBlasterProjectile, Boolean> seen,
        Map<AlienBlasterProjectile, Boolean> before) {
        List<AlienBlasterProjectile> fresh = new ArrayList<>();
        for (Object o : ctx.world().loadedEntityList) {
            if (o instanceof AlienBlasterProjectile && !before.containsKey(o)
                && seen.put((AlienBlasterProjectile) o, Boolean.TRUE) == null) {
                fresh.add((AlienBlasterProjectile) o);
            }
        }
        return fresh;
    }

    private static Map<AlienBlasterProjectile, Boolean> bolts(ScenarioContext ctx) {
        Map<AlienBlasterProjectile, Boolean> now = new IdentityHashMap<>();
        for (Object o : ctx.world().loadedEntityList) {
            if (o instanceof AlienBlasterProjectile) {
                now.put((AlienBlasterProjectile) o, Boolean.TRUE);
            }
        }
        return now;
    }

    private static EntityZombie tankyZombie(ScenarioContext ctx, int dx) {
        EntityZombie z = GunRig.zombie(ctx, dx);
        z.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(400.0);
        z.setHealth(400.0F);
        z.hurtResistantTime = 0;
        return z;
    }

    private static ShotOffset straight(ScenarioContext ctx) {
        return SpreadModel.roll(ctx.random(), STATS, ShotContext.npc(0.0), 0)[0];
    }

    /** Holding the trigger fires an alien bolt every 8 ticks, each spending exactly one shot's EU. */
    private static final class Fire implements Scenario {

        @Override
        public String name() {
            return "gun.alien_blaster.fire";
        }

        @Override
        public String description() {
            return "Holding the trigger on a charged Alien Blaster fires every 8 ticks: each shot launches one AlienBlasterProjectile and spends exactly 100,000,000 EU from the stack, with no reload and no round change.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = chargedRig(ctx, 6, 12);
            long start = stored(rig.gun);
            Map<AlienBlasterProjectile, Boolean> before = bolts(ctx);
            Map<AlienBlasterProjectile, Boolean> seen = new IdentityHashMap<>();
            rig.press();
            return ScenarioResult.pending(name(), HELD_TICKS + 10, ticks -> {
                rig.tick();
                newBolts(ctx, seen, before);
                if (ticks < HELD_TICKS) {
                    return null;
                }
                rig.release();
                int expected = 1 + (HELD_TICKS - 1) / STATS.fireRateTicks();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("bolts launched", expected, rig.bullets());
                out.checkEquals("of which alien bolts", expected, seen.size());
                out.checkEquals("EU spent", (long) expected * STATS.euPerShot(), start - stored(rig.gun));
                out.check(STATS.isEnergy(), "an energy gun");
                out.check(!STATS.hasScope(), "the gun has no scope");
                return out.build();
            });
        }
    }

    /** A bolt from a player hurts a zombie by the FIRE figure with the gun's penetration, and sets it alight. */
    private static final class Hit implements Scenario {

        private static final int RANGE = 6;

        @Override
        public String name() {
            return "gun.alien_blaster.hit";
        }

        @Override
        public String description() {
            return "One Alien Blaster bolt from a player (straight, no spread) hits a zombie 6 blocks away for its 16 FIRE damage after the damage model with 20 percent penetration, with no drop-off, and sets it alight; the direct target is left out of the tiny blast.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = new GunRig(ctx, GunItems.get(AlienBlasterDef.NAME), 0, RANGE + 4);
            EntityZombie z = tankyZombie(ctx, RANGE);
            Projectiles.launcher()
                .launch(ctx.world(), rig.shooter, STATS, straight(ctx), 1.0F);
            int flight = (int) Math.ceil(RANGE / STATS.projectileSpeed()) + 4;
            return ScenarioResult.pending(name(), flight + 10, ticks -> {
                if (ticks < flight) {
                    return null;
                }
                double armor = z.getTotalArmorValue();
                float expected = (float) DamageModel.resolve(
                    DamageType.FIRE,
                    STATS.damage(),
                    STATS.penetration(),
                    share -> share * DamageModel.vanillaArmorFactor(armor));
                float lost = z.getMaxHealth() - z.getHealth();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(
                    lost >= expected - TOLERANCE && lost <= expected + BURN_ALLOWANCE,
                    "zombie should lose " + expected + " (plus at most one burn tick) but lost " + lost);
                out.check(z.isBurning(), "a player's bolt sets the target alight");
                return out.build();
            });
        }
    }

    /** Listens for explosions started by alien bolts. */
    public static final class Recorder {

        /**
         * @param event the explosion about to run
         */
        @SubscribeEvent
        public void onStart(ExplosionEvent.Start event) {
            if (event.explosion.exploder instanceof AlienBlasterProjectile) {
                synchronized (BLASTS) {
                    BLASTS.add(new float[] { event.explosion.explosionSize, event.explosion.isSmoking ? 1 : 0 });
                }
            }
        }
    }

    /** The bolt ends in a power 0.5 explosion that breaks nothing, and a player's bolt burns the face it hits. */
    private static final class Blast implements Scenario {

        private static final int WALL = 6;

        @Override
        public String name() {
            return "gun.alien_blaster.blast";
        }

        @Override
        public String description() {
            return "A player's Alien Blaster bolt into a wooden wall ends in a power 0.5 explosion with block breaking off, the wall is still standing, and fire is on the face it hit exactly when explosions.blockDamage is on.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!recorderRegistered) {
                recorderRegistered = true;
                MinecraftForge.EVENT_BUS.register(new Recorder());
            }
            synchronized (BLASTS) {
                BLASTS.clear();
            }
            GunRig rig = new GunRig(ctx, GunItems.get(AlienBlasterDef.NAME), 0, WALL + 4);
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ctx.setBlock(WALL, dy, dz, Blocks.planks, 0);
                }
            }
            Projectiles.launcher()
                .launch(ctx.world(), rig.shooter, STATS, straight(ctx), 1.0F);
            int flight = (int) Math.ceil(WALL / STATS.projectileSpeed()) + 4;
            return ScenarioResult.pending(name(), flight + 10, ticks -> {
                if (ticks < flight) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                int n;
                float size = 0;
                boolean smoking = true;
                synchronized (BLASTS) {
                    n = BLASTS.size();
                    if (n > 0) {
                        size = BLASTS.get(0)[0];
                        smoking = BLASTS.get(0)[1] > 0;
                    }
                }
                out.checkEquals("explosions started by the bolt", 1, n);
                out.checkEquals("explosion power", (double) AlienBlasterProjectile.BLAST_SIZE, (double) size);
                out.check(!smoking, "the blast breaks no blocks");
                out.check(
                    ctx.world()
                        .getBlock(ctx.originX() + WALL, ctx.originY() + 1, ctx.originZ()) == Blocks.planks,
                    "the wall is still standing");
                boolean fire = ctx.world()
                    .getBlock(ctx.originX() + WALL - 1, ctx.originY() + 1, ctx.originZ()) == Blocks.fire;
                out.check(
                    fire == ExplosionsConfig.blockDamage,
                    "fire on the hit face should be " + ExplosionsConfig.blockDamage + " (explosions.blockDamage)");
                return out.build();
            });
        }
    }

    /** The trail FX is really sent, for a bolt that is a registered, tracked entity (PFX-01). */
    private static final class Trail implements Scenario {

        @Override
        public String name() {
            return "gun.alien_blaster.trail";
        }

        @Override
        public String description() {
            return "Firing the Alien Blaster sends the AlienBlasterTrail FX on the bolt's second tick while it is alive (PFX-01 fix): the bolt is a registered tracked entity and the FX list resolves to its three systems.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = chargedRig(ctx, 2, 12);
            Map<AlienBlasterProjectile, Boolean> before = bolts(ctx);
            Map<AlienBlasterProjectile, Boolean> seen = new IdentityHashMap<>();
            AlienBlasterProjectile[] bolt = { null };
            boolean[] aliveWhenSent = { false };
            rig.press();
            return ScenarioResult.pending(name(), 20, ticks -> {
                rig.tick();
                for (AlienBlasterProjectile b : newBolts(ctx, seen, before)) {
                    if (bolt[0] == null) {
                        bolt[0] = b;
                    }
                }
                if (bolt[0] != null && bolt[0].trailSentTick() >= 0 && !aliveWhenSent[0]) {
                    aliveWhenSent[0] = !bolt[0].isDead;
                }
                if (bolt[0] == null || bolt[0].trailSentTick() < 0 && !bolt[0].isDead) {
                    return null;
                }
                rig.release();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                AlienBlasterProjectile b = bolt[0];
                out.checkEquals("tick the trail was sent", AlienBlasterProjectile.TRAIL_TICK, b.trailSentTick());
                out.check(aliveWhenSent[0], "the bolt was alive when the trail was sent");
                EntityRegistry.EntityRegistration reg = EntityRegistry.instance()
                    .lookupModSpawn(AlienBlasterProjectile.class, false);
                out.check(reg != null, "the bolt is a registered mod entity (Techguns never registered it: PFX-01)");
                if (reg != null) {
                    out.check(reg.getTrackingRange() > 0, "the bolt has a tracking range");
                    out.check(reg.sendsVelocityUpdates(), "the bolt's tracker sends velocity updates");
                }
                int systems = FxResources.shared()
                    .resolve(AlienBlasterProjectile.TRAIL_FX)
                    .size();
                out.checkEquals("systems in the trail FX list", 3, systems);
                return out.build();
            });
        }
    }
}
