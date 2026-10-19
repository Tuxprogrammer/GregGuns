package com.gregguns.projectile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.logic.SpreadModel;

/**
 * The {@code projectile.laser.*} in-game scenarios for the hitscan carrier. The hitscan resolves inside the launch
 * call, so each scenario checks straight after firing, without waiting for ticks. Dev environment only. Server side.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.3); docs/testing/README.md
final class HitscanScenarios {

    private static final float EPS = 0.05F;

    /** A flat 12 damage laser: 7.5 blocks per tick for 5 ticks, so 37.5 blocks of reach and no drop-off inside it. */
    private static final GunStats LASER = GunStats.builder("test_laser")
        .damage(12.0)
        .fire(FireMode.SEMI, 5)
        .magazine(1)
        .ammoType("test_laser_charge")
        .projectile(7.5, 5)
        .projectileKind(ProjectileKind.LASER)
        .build();

    private HitscanScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new LaserHit());
        registry.register(new LaserBlocks());
    }

    private static void shoot(ScenarioContext ctx, FakePlayer from) {
        ShotOffset straight = SpreadModel.roll(ctx.random(), LASER, ShotContext.npc(0.0), 0)[0];
        Projectiles.launcher()
            .launch(ctx.world(), from, LASER, straight, 1.0F);
    }

    private static float lost(EntityZombie z) {
        return z.getMaxHealth() - z.getHealth();
    }

    private static float expectedEnergyLoss(EntityZombie z) {
        double armor = z.getTotalArmorValue();
        return (float) DamageModel
            .resolve(DamageType.ENERGY, LASER.damage(), 0.0, share -> share * DamageModel.vanillaArmorFactor(armor));
    }

    private static List<BeamVisual> beams(ScenarioContext ctx) {
        List<BeamVisual> found = new ArrayList<>();
        for (Object o : ctx.world().loadedEntityList) {
            if (o instanceof BeamVisual) {
                found.add((BeamVisual) o);
            }
        }
        return found;
    }

    private static int carriers(ScenarioContext ctx) {
        int n = 0;
        for (Object o : ctx.world().loadedEntityList) {
            if (o instanceof LaserProjectile && !((Entity) o).isDead
                && (((Entity) o).motionX != 0 || ((Entity) o).motionY != 0 || ((Entity) o).motionZ != 0)) {
                n++;
            }
        }
        return n;
    }

    /** One shot, instantly: the zombie 20 blocks away is hurt and a beam of that length is shown. */
    private static final class LaserHit implements Scenario {

        private static final int RANGE = 20;

        @Override
        public String name() {
            return "projectile.laser.hit";
        }

        @Override
        public String description() {
            return "A laser shot hurts a zombie 20 blocks away at once for the ENERGY figure, shows a beam about that long and flies no bolt.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ProjectileScenarios.corridor(ctx, RANGE + 3);
            FakePlayer gunman = ProjectileScenarios.shooter(ctx);
            EntityZombie target = ProjectileScenarios.zombie(ctx, RANGE);
            List<BeamVisual> before = beams(ctx);
            shoot(ctx, gunman);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(
                ProjectileScenarios.near(expectedEnergyLoss(target), lost(target)),
                "zombie should lose " + expectedEnergyLoss(target) + " at once but lost " + lost(target));
            List<BeamVisual> after = beams(ctx);
            out.check(after.size() == before.size() + 1, "one beam visual should be spawned, got " + after.size());
            if (after.size() > before.size()) {
                float length = after.get(after.size() - 1)
                    .getLength();
                out.check(
                    length > RANGE - 1.0F && length < RANGE + 1.0F,
                    "the beam should end at the zombie, about " + RANGE + " blocks, but is " + length);
            }
            out.check(
                carriers(ctx) == 0,
                "no laser bolt should be flying (a carrier may await its one update, but it must not move)");
            return out.build();
        }
    }

    /** A plant lets the beam through, a wall stops it, and nothing past the wall is hurt (PFX-23, PFX-27). */
    private static final class LaserBlocks implements Scenario {

        @Override
        public String name() {
            return "projectile.laser.blocks";
        }

        @Override
        public String description() {
            return "A laser beam passes tall grass (PFX-27) and hurts the zombie behind it, and is stopped by a wall that spares the zombie beyond (PFX-23).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ProjectileScenarios.corridor(ctx, 14);
            for (int dz = -1; dz <= 1; dz++) {
                ctx.setBlock(2, 0, dz, Blocks.tallgrass, 1);
                for (int dy = 0; dy <= 2; dy++) {
                    ctx.setBlock(8, dy, dz, Blocks.stone, 0);
                }
            }
            FakePlayer gunman = ProjectileScenarios.shooter(ctx);
            EntityZombie inFront = ProjectileScenarios.zombie(ctx, 5);
            shoot(ctx, gunman);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(
                ProjectileScenarios.near(expectedEnergyLoss(inFront), lost(inFront)),
                "the zombie behind the grass should be hurt, lost " + lost(inFront));
            // Take the first zombie out of the way, then shoot at the wall with a zombie beyond it.
            inFront.setDead();
            EntityZombie beyond = ProjectileScenarios.zombie(ctx, 11);
            shoot(ctx, gunman);
            out.check(
                ProjectileScenarios.near(0.0F, lost(beyond)),
                "the zombie behind the wall should be unhurt, lost " + lost(beyond));
            List<BeamVisual> shown = beams(ctx);
            if (shown.size() >= 2) {
                float length = shown.get(shown.size() - 1)
                    .getLength();
                out.check(
                    length > 6.0F && length < 8.5F,
                    "the beam should stop at the wall (7.5 blocks), length " + length);
            } else {
                out.check(false, "two beams should have been shown, got " + shown.size());
            }
            return out.build();
        }
    }
}
