package com.gregguns.projectile;

import net.minecraft.block.Block;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.explosion.BlastTicker;
import com.gregguns.explosion.ExplosionRules;
import com.gregguns.explosion.NukeAnnihilation;
import com.gregguns.fx.FxResources;
import com.gregguns.fx.FxStress;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.def.NukeLauncherDef;
import com.gregguns.gun.server.GunRig;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.GunLineScenarios;
import com.gregguns.recipe.GunLineTable;

/**
 * The {@code gun.nuke_launcher.*} scenarios and the nuke case of the FX perf check ({@code fx.stress.nuke}): the
 * reload, the Assembly Line recipe, a nuke fired at zombies (direct hit, the damage ramp at radius 10), a nuke into
 * dirt with {@code explosions.blockDamage} off (safe mode: nothing breaks), and a nuke into dirt with it on (the breaks
 * are time-sliced, so no tick does more than the per-tick cap). Through the same server handler as every gun. Dev
 * environment only. Server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Nuke Launcher);
// docs/gregguns-spec/04-projectiles-fx/m5-exit-gate.md; docs/testing/README.md
public final class NukeLauncherScenarios {

    private static final float EPS = 0.5F;
    private static final int CORRIDOR = 14;
    private static final int HALF_WIDTH = 14;

    private NukeLauncherScenarios() {}

    /**
     * Registers the Nuke Launcher's scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        GunDefinition def = GunDefinitions.byName(NukeLauncherDef.NAME);
        registry.register(new LauncherScenarios.Reload(def, 1));
        registry.register(
            GunLineScenarios.recipe(
                "gun.nuke_launcher.recipe",
                "The Nuke Launcher (GR-102) is on GT's Assembly Line list at ZPM with its sheet inputs, both fluids, its research item, scan time and voltage, and its output is ours.",
                GunLineTable.NUKE_LAUNCHER));
        registry.register(new Fire());
        registry.register(new KillZone());
        registry.register(new Safe());
        registry.register(new Stress());
    }

    // ----- helpers -----------------------------------------------------------------------------------------------

    /** A stone floor wide enough for the blast's witnesses, east of the shooter. */
    private static void floor(ScenarioContext ctx) {
        for (int dx = -1; dx <= 22; dx++) {
            for (int dz = -HALF_WIDTH; dz <= HALF_WIDTH; dz++) {
                ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
            }
        }
    }

    /** A zombie that cannot move and has 2000 health, so only the kill zone kills it. */
    private static EntityZombie tough(ScenarioContext ctx, double dx, double dz) {
        EntityZombie z = LauncherScenarios.sturdy(ctx, dx, dz);
        z.getEntityAttribute(SharedMonsterAttributes.maxHealth)
            .setBaseValue(2000.0);
        z.setHealth(2000.0F);
        return z;
    }

    private static void fillDirt(ScenarioContext ctx, boolean fill) {
        for (int dx = 6; dx <= 12; dx++) {
            for (int dz = -HALF_WIDTH + 2; dz <= HALF_WIDTH - 2; dz++) {
                for (int dy = 0; dy <= 10; dy++) {
                    ctx.setBlock(dx, dy, dz, fill ? Blocks.dirt : Blocks.air, 0);
                }
            }
        }
    }

    private static int countDirt(ScenarioContext ctx) {
        int count = 0;
        for (int dx = 6; dx <= 12; dx++) {
            for (int dz = -HALF_WIDTH + 2; dz <= HALF_WIDTH - 2; dz++) {
                for (int dy = 0; dy <= 10; dy++) {
                    Block b = ctx.world()
                        .getBlock(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
                    if (b == Blocks.dirt) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static double distance(EntityZombie z, RocketProjectile r) {
        double dx = z.posX - r.posX;
        double dy = z.posY - r.posY;
        double dz = z.posZ - r.posZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // ----- fire --------------------------------------------------------------------------------------------------

    /** A nuke into a zombie: the kill zone annihilates, the splash ramp spans radius 10. */
    private static final class Fire implements Scenario {

        @Override
        public String name() {
            return "gun.nuke_launcher.fire";
        }

        @Override
        public String description() {
            return "A Nuke Launcher round fired into a zombie 4 blocks away is a rocket with blast radius 10 and damage 400 (design 0016): the zombie hit dies outright, is thrown far harder than a rocket would, a zombie 3 blocks beside the impact (inside the 6-block kill zone) dies too, one 10 blocks away takes the ramp's reduced share, and the round is spent.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            boolean savedBlocks = ExplosionsConfig.blockDamage;
            boolean savedSelf = ExplosionsConfig.selfDamage;
            LauncherScenarios.restore(false, true);
            GunRig rig = new GunRig(ctx, GunItems.get(NukeLauncherDef.NAME), 1, CORRIDOR);
            floor(ctx);
            EntityZombie front = tough(ctx, 4.0, 0.0);
            EntityZombie near = tough(ctx, 4.0, 3.0);
            EntityZombie far = tough(ctx, 4.0, -10.0);
            LauncherScenarios.Tracker<RocketProjectile> rocket = new LauncherScenarios.Tracker<>(
                ctx,
                RocketProjectile.class);
            LauncherScenarios.pull(rig);
            return ScenarioResult.pending(name(), 60, ticks -> {
                rig.tick();
                RocketProjectile r = rocket.poll(ctx);
                if (r == null || !r.isDead) {
                    if (ticks >= 55) {
                        LauncherScenarios.restore(savedBlocks, savedSelf);
                        return ScenarioResult.fail(name(), "the nuke never exploded");
                    }
                    return null;
                }
                LauncherScenarios.restore(savedBlocks, savedSelf);
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("blast radius", 10.0F, r.getBlastRadius());
                out.check(front.isDead || front.getHealth() <= 0.0F, "the zombie hit should be annihilated");
                double push = Math.abs(front.motionX) + Math.abs(front.motionZ);
                out.check(push > 1.0, "knockback x10 should throw the zombie hard, motion " + push);
                double dNear = distance(near, r);
                out.check(
                    dNear < NukeAnnihilation.killRadius(10.0) && (near.isDead || near.getHealth() <= 0.0F),
                    "inside the kill zone (distance " + dNear + ") the zombie should be annihilated");
                double dFar = distance(far, r);
                float expectFar = LauncherScenarios
                    .lossFor(far, LauncherMath.splashDamage(NukeLauncherDef.STATS.damage(), 10.0, dFar));
                out.check(
                    dFar > LauncherMath.innerRadius(10.0) && expectFar > 0.0F
                        && expectFar < LauncherScenarios.lossFor(far, NukeLauncherDef.STATS.damage())
                        && Math.abs(LauncherScenarios.lost(far) - expectFar) < EPS,
                    "on the ramp (distance " + dFar
                        + ") the zombie should lose "
                        + expectFar
                        + " but lost "
                        + LauncherScenarios.lost(far));
                out.checkEquals("rounds left", 0, rig.loaded());
                out.checkEquals(
                    "floor intact (block damage off)",
                    Blocks.stone,
                    ctx.world()
                        .getBlock(ctx.originX() + 4, ctx.originY() - 1, ctx.originZ()));
                return out.build();
            });
        }
    }

    // ----- kill zone ---------------------------------------------------------------------------------------------

    /** A boss stand-in: a zombie that carries the vanilla boss marker. */
    private static final class BossZombie extends EntityZombie implements IBossDisplayData {

        BossZombie(net.minecraft.world.World world) {
            super(world);
            isImmuneToFire = true;
        }
    }

    /** The kill zone: an armored zombie dies outright, a boss stand-in loses half its maximum health. */
    private static final class KillZone implements Scenario {

        @Override
        public String name() {
            return "gun.nuke_launcher.killzone";
        }

        @Override
        public String description() {
            return "A Nuke Launcher round fired at a zombie in full diamond-grade armor kills it outright (the kill zone ignores armor), and a boss-marked zombie 3 blocks from the impact, inside the 6-block kill zone, loses exactly half its maximum health instead of dying.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            boolean savedBlocks = ExplosionsConfig.blockDamage;
            boolean savedSelf = ExplosionsConfig.selfDamage;
            LauncherScenarios.restore(false, true);
            GunRig rig = new GunRig(ctx, GunItems.get(NukeLauncherDef.NAME), 1, CORRIDOR);
            floor(ctx);
            EntityZombie armored = tough(ctx, 4.0, 0.0);
            armored.setCurrentItemOrArmor(4, new ItemStack(Items.diamond_helmet));
            armored.setCurrentItemOrArmor(3, new ItemStack(Items.diamond_chestplate));
            armored.setCurrentItemOrArmor(2, new ItemStack(Items.diamond_leggings));
            armored.setCurrentItemOrArmor(1, new ItemStack(Items.diamond_boots));
            EntityZombie boss = new BossZombie(ctx.world());
            boss.setPosition(ctx.originX() + 4.5, ctx.originY(), ctx.originZ() + 3.5);
            boss.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                .setBaseValue(2000.0);
            boss.setHealth(2000.0F);
            // Daylight burn is 1 damage a second: a bare zombie ends at 999, not half. The pumpkin and fire immunity
            // stop it.
            boss.setCurrentItemOrArmor(4, new ItemStack(net.minecraft.init.Blocks.pumpkin));
            boss.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 2000, 10));
            ctx.world()
                .spawnEntityInWorld(boss);
            LauncherScenarios.Tracker<RocketProjectile> rocket = new LauncherScenarios.Tracker<>(
                ctx,
                RocketProjectile.class);
            LauncherScenarios.pull(rig);
            return ScenarioResult.pending(name(), 60, ticks -> {
                rig.tick();
                RocketProjectile r = rocket.poll(ctx);
                if (r == null || !r.isDead) {
                    if (ticks >= 55) {
                        LauncherScenarios.restore(savedBlocks, savedSelf);
                        return ScenarioResult.fail(name(), "the nuke never exploded");
                    }
                    return null;
                }
                LauncherScenarios.restore(savedBlocks, savedSelf);
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(
                    armored.getTotalArmorValue() > 10 && (armored.isDead || armored.getHealth() <= 0.0F),
                    "the armored zombie should be annihilated despite its armor");
                double dBoss = distance(boss, r);
                out.check(
                    dBoss < NukeAnnihilation.killRadius(10.0),
                    "the boss stand-in should be inside the kill zone, distance " + dBoss);
                out.check(!boss.isDead, "a boss must survive the kill zone");
                // A pack can rescale a spawned mob's maximum health and refill it (the GTNH 2.9.0-RC-2 pack does, by
                // difficulty: 1.1 times on Hard, 0.9 on Easy), so a full-health boss ends at half of the maximum read
                // here, which is 1000 only when nothing rescales it.
                float expected = (float) (NukeAnnihilation.BOSS_SHARE * boss.getMaxHealth());
                out.check(
                    Math.abs(boss.getHealth() - expected) < EPS,
                    "the boss should be left with half its maximum health " + boss
                        .getMaxHealth() + " (" + expected + "), but has " + boss.getHealth());
                return out.build();
            });
        }
    }

    // ----- safe mode ---------------------------------------------------------------------------------------------

    /** A nuke into a dirt mass with block damage off breaks nothing and queues nothing, but still hurts a zombie. */
    private static final class Safe implements Scenario {

        @Override
        public String name() {
            return "gun.nuke_launcher.safe";
        }

        @Override
        public String description() {
            return "With explosions.blockDamage off (safe mode) a nuke into a 7-deep mass of dirt breaks no block and queues none for BlastTicker, yet a zombie beside the impact still takes the splash.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            boolean savedBlocks = ExplosionsConfig.blockDamage;
            boolean savedSelf = ExplosionsConfig.selfDamage;
            LauncherScenarios.restore(false, true);
            BlastTicker.instance()
                .clear();
            GunRig rig = new GunRig(ctx, GunItems.get(NukeLauncherDef.NAME), 1, CORRIDOR);
            floor(ctx);
            fillDirt(ctx, true);
            int before = countDirt(ctx);
            EntityZombie witness = tough(ctx, 4.0, 2.0);
            LauncherScenarios.Tracker<RocketProjectile> rocket = new LauncherScenarios.Tracker<>(
                ctx,
                RocketProjectile.class);
            LauncherScenarios.pull(rig);
            return ScenarioResult.pending(name(), 60, ticks -> {
                rig.tick();
                RocketProjectile r = rocket.poll(ctx);
                if (r == null || !r.isDead) {
                    if (ticks >= 55) {
                        LauncherScenarios.restore(savedBlocks, savedSelf);
                        fillDirt(ctx, false);
                        return ScenarioResult.fail(name(), "the nuke never exploded");
                    }
                    return null;
                }
                LauncherScenarios.restore(savedBlocks, savedSelf);
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("dirt blocks left", before, countDirt(ctx));
                out.checkEquals(
                    "blocks queued for later ticks",
                    0,
                    BlastTicker.instance()
                        .pendingBlocks());
                out.check(
                    LauncherScenarios.lost(witness) > 0.0F,
                    "the zombie beside the impact should still be hurt, lost " + LauncherScenarios.lost(witness));
                fillDirt(ctx, false);
                return out.build();
            });
        }
    }

    // ----- fx perf check -----------------------------------------------------------------------------------------

    /**
     * The nuke case of the exit-gate perf check: the {@code NukeExplosion} FX list alone and in a salvo stays under
     * the particle ceiling with nothing spilled, and a real nuke into dirt with block damage on never breaks more than
     * the per-tick cap in a tick.
     */
    private static final class Stress implements Scenario {

        private static final int CEILING = 2000;

        @Override
        public String name() {
            return "fx.stress.nuke";
        }

        @Override
        public String description() {
            return "One NukeExplosion FX list, and a salvo of three at once, run 300 ticks under the 2000-particle ceiling: live never passes it and spilled is 0. Then a real nuke fired into a dirt mass with block damage on breaks at most the per-tick cap in any tick after its blast tick (twice the cap in it: the blast's first batch and the ticker's), and BlastTicker drains the rest.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            String[] nuke = { "NukeExplosion" };
            int systems = FxResources.shared()
                .resolve("NukeExplosion")
                .size();
            out.check(systems >= 6, "NukeExplosion should resolve to its systems, got " + systems);
            FxStress.Result one = FxStress.run(FxResources.shared(), nuke, systems, CEILING, 300, 21L);
            FxStress.Result salvo = FxStress.run(FxResources.shared(), nuke, systems * 3, CEILING, 300, 22L);
            out.check(one.peakLive > 0, "a nuke should produce particles (peak " + one.peakLive + ")");
            out.check(one.peakLive <= CEILING, "one nuke: live " + one.peakLive + " within the ceiling " + CEILING);
            out.checkEquals("one nuke spilled", 0, one.spilled);
            out.check(salvo.peakLive <= CEILING, "salvo: live " + salvo.peakLive + " within the ceiling " + CEILING);
            out.checkEquals("salvo spilled", 0, salvo.spilled);
            out.note(
                "one nuke: peak live " + one.peakLive
                    + ", granted "
                    + one.granted
                    + ", trimmed "
                    + one.trimmed
                    + "; salvo of three: peak live "
                    + salvo.peakLive
                    + ", trimmed "
                    + salvo.trimmed);
            ScenarioResult headless = out.build();
            if (!headless.passed()) {
                return headless;
            }
            if (!com.gregguns.explosion.PackExplosionProbe
                .blocksSurvive(ctx.world(), ctx.originX(), ctx.originY(), ctx.originZ())) {
                // The FX half stands on its own; the block half needs a pack that lets explosions break blocks.
                ScenarioResult.Builder partial = ScenarioResult.builder(name());
                for (String m : headless.messages()) {
                    partial.note(m);
                }
                return partial
                    .note("block-breaking half skipped: " + com.gregguns.explosion.PackExplosionProbe.SKIP_REASON)
                    .build();
            }
            return worldPart(ctx);
        }

        private ScenarioResult worldPart(ScenarioContext ctx) {
            boolean savedBlocks = ExplosionsConfig.blockDamage;
            boolean savedSelf = ExplosionsConfig.selfDamage;
            LauncherScenarios.restore(true, true);
            BlastTicker.instance()
                .clear();
            GunRig rig = new GunRig(ctx, GunItems.get(NukeLauncherDef.NAME), 1, CORRIDOR);
            floor(ctx);
            fillDirt(ctx, true);
            int start = countDirt(ctx);
            int[] last = { start };
            int[] diedAt = { -1 };
            int[] worst = { 0 };
            boolean[] overCap = { false };
            LauncherScenarios.Tracker<RocketProjectile> rocket = new LauncherScenarios.Tracker<>(
                ctx,
                RocketProjectile.class);
            LauncherScenarios.pull(rig);
            return ScenarioResult.pending(name(), 120, ticks -> {
                rig.tick();
                RocketProjectile r = rocket.poll(ctx);
                boolean dead = r != null && r.isDead;
                if (dead && diedAt[0] < 0) {
                    diedAt[0] = ticks;
                }
                int now = countDirt(ctx);
                int broken = last[0] - now;
                last[0] = now;
                worst[0] = Math.max(worst[0], broken);
                // The blast tick holds two batches (the blast's own and the ticker's end-of-tick one); every other
                // tick at most one. The poll window can straddle them by a tick.
                int allowed = diedAt[0] >= 0 && ticks <= diedAt[0] + 1 ? 2 * ExplosionRules.BLOCKS_PER_TICK
                    : ExplosionRules.BLOCKS_PER_TICK;
                if (broken > allowed) {
                    overCap[0] = true;
                }
                boolean drained = BlastTicker.instance()
                    .pendingBlocks() == 0;
                if (!dead || !drained) {
                    if (ticks >= 110) {
                        LauncherScenarios.restore(savedBlocks, savedSelf);
                        BlastTicker.instance()
                            .clear();
                        fillDirt(ctx, false);
                        return ScenarioResult.fail(name(), "the nuke never finished breaking blocks");
                    }
                    return null;
                }
                LauncherScenarios.restore(savedBlocks, savedSelf);
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(start - now > 0, "block damage on: the nuke should break dirt, broke " + (start - now));
                out.check(
                    !overCap[0],
                    "no tick may break more than the cap (" + ExplosionRules.BLOCKS_PER_TICK
                        + "; twice in the blast tick), worst tick broke "
                        + worst[0]);
                out.note("broke " + (start - now) + " blocks, worst tick " + worst[0] + ", blast at tick " + diedAt[0]);
                fillDirt(ctx, false);
                return out.build();
            });
        }
    }
}
