package com.gregguns.projectile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.combat.DamageFalloff;
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
 * The {@code projectile.beam.*} in-game scenarios for the continuous beam. The beam resolves inside the launch call,
 * so each scenario checks straight after firing. Dev environment only. Server side.
 */
// Spec: docs/techguns-spec/02-guns/per-gun-behavior.md (Nuclear Death Ray); docs/testing/README.md
final class BeamScenarios {

    /** 6 damage falling to 1 between 20 and 40 blocks, 35% penetration, as the Nuclear Death Ray. */
    private static final GunStats BEAM = GunStats.builder("test_beam")
        .damage(6.0)
        .damageDrop(20.0, 40.0, 1.0)
        .penetration(0.35)
        .fire(FireMode.AUTO, 1)
        .magazine(1)
        .ammoType("test_beam_charge")
        .projectile(5.0, 10)
        .projectileKind(ProjectileKind.BEAM)
        .build();

    private BeamScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new BeamLine());
    }

    private static void shoot(ScenarioContext ctx, FakePlayer from) {
        ShotOffset straight = SpreadModel.roll(ctx.random(), BEAM, ShotContext.npc(0.0), 0)[0];
        Projectiles.launcher()
            .launch(ctx.world(), from, BEAM, straight, 1.0F);
    }

    private static float lost(EntityZombie z) {
        return z.getMaxHealth() - z.getHealth();
    }

    private static float expected(EntityZombie z, double distance) {
        double raw = DamageFalloff.damageAt(6.0, 1.0, 20.0, 40.0, distance);
        double armor = z.getTotalArmorValue();
        return (float) DamageModel
            .resolve(DamageType.RADIATION, raw, 0.35, share -> share * DamageModel.vanillaArmorFactor(armor));
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

    /** One tick of the beam burns every zombie on its line, with fall-off, stops at a wall, and burns once per tick. */
    private static final class BeamLine implements Scenario {

        @Override
        public String name() {
            return "projectile.beam.line";
        }

        @Override
        public String description() {
            return "A beam tick hurts a near zombie fully and a far one less (6 falling to 1 over 20 to 40 blocks), "
                + "spares the zombie behind a wall (PFX-23), deals nothing more on a second shot in the same tick, "
                + "and shows one blue beam as long as the wall distance.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            MovingBeams.clear();
            ProjectileScenarios.corridor(ctx, 44);
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    ctx.setBlock(34, dy, dz, Blocks.stone, 0);
                }
            }
            FakePlayer gunman = ProjectileScenarios.shooter(ctx);
            EntityZombie near = ProjectileScenarios.zombie(ctx, 8);
            EntityZombie far = ProjectileScenarios.zombie(ctx, 30);
            EntityZombie behind = ProjectileScenarios.zombie(ctx, 38);
            List<BeamVisual> before = beams(ctx);
            shoot(ctx, gunman);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(
                ProjectileScenarios.near(expected(near, 7.4), lost(near)),
                "the near zombie should lose the full figure " + expected(near, 7.4) + " but lost " + lost(near));
            // The far zombie is about 29.4 blocks out; the beam enters its box a little either side of that.
            float lo = expected(far, 30.0);
            float hi = expected(far, 28.8);
            out.check(
                lost(far) >= lo - 0.1F && lost(far) <= hi + 0.1F && lost(far) < lost(near),
                "the far zombie should lose " + lo + " to " + hi + " (less than the near one) but lost " + lost(far));
            out.check(lost(behind) == 0.0F, "the zombie behind the wall should be unhurt, lost " + lost(behind));
            float nearAfterFirst = lost(near);
            shoot(ctx, gunman);
            out.check(
                lost(near) == nearAfterFirst,
                "a second shot in the same tick should deal no more damage, lost " + lost(near));
            List<BeamVisual> after = beams(ctx);
            out.check(
                after.size() == before.size() + 1,
                "one beam visual should be shown for the tick, got " + (after.size() - before.size()));
            if (after.size() > before.size()) {
                BeamVisual v = after.get(after.size() - 1);
                out.check(
                    v.getLength() > 32.0F && v.getLength() < 34.5F,
                    "the beam should stop at the wall: " + v.getLength());
                out.check(v.getStyle() == BeamVisual.STYLE_BLUE, "the beam should be the blue style");
            }
            MovingBeams.clear();
            return out.build();
        }
    }
}
