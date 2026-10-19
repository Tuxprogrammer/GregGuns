package com.gregguns.projectile;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.util.FakePlayer;

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
 * The {@code projectile.tesla.*} in-game scenarios for chain lightning. The shot resolves inside the launch call.
 * Dev environment only. Server side.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/projectiles.md (3.6); docs/testing/README.md
final class TeslaScenarios {

    /** A flat 12 damage Tesla shot: 5 blocks per tick for 10 ticks. */
    private static final GunStats TESLA = GunStats.builder("test_tesla")
        .damage(12.0)
        .fire(FireMode.SEMI, 5)
        .magazine(1)
        .ammoType("test_tesla_charge")
        .projectile(5.0, 10)
        .projectileKind(ProjectileKind.TESLA)
        .build();

    private TeslaScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Chain());
        registry.register(new Wall());
    }

    private static void shoot(ScenarioContext ctx, FakePlayer from) {
        ShotOffset straight = SpreadModel.roll(ctx.random(), TESLA, ShotContext.npc(0.0), 0)[0];
        Projectiles.launcher()
            .launch(ctx.world(), from, TESLA, straight, 1.0F);
    }

    private static float lost(EntityZombie z) {
        return z.getMaxHealth() - z.getHealth();
    }

    private static int beams(ScenarioContext ctx) {
        int n = 0;
        for (Object o : ctx.world().loadedEntityList) {
            if (o instanceof BeamVisual) {
                n++;
            }
        }
        return n;
    }

    /** Five zombies 3 blocks apart in a row: the first and the next three are struck, the fifth is not. */
    private static final class Chain implements Scenario {

        @Override
        public String name() {
            return "projectile.tesla.chain";
        }

        @Override
        public String description() {
            return "A Tesla shot hurts the first zombie and jumps to exactly three more, 3 blocks apart; the fifth is unhurt and four beams are shown.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ProjectileScenarios.corridor(ctx, 24);
            FakePlayer gunman = ProjectileScenarios.shooter(ctx);
            EntityZombie[] row = new EntityZombie[5];
            for (int i = 0; i < row.length; i++) {
                row[i] = ProjectileScenarios.zombie(ctx, 8 + 3 * i);
            }
            int before = beams(ctx);
            shoot(ctx, gunman);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (int i = 0; i < 4; i++) {
                out.check(lost(row[i]) > 0.0F, "zombie " + i + " should be hurt, lost " + lost(row[i]));
            }
            out.check(lost(row[4]) == 0.0F, "the fifth zombie is past three jumps and must be unhurt");
            out.checkEquals("beams shown (one to the primary, one per jump)", before + 4, beams(ctx));
            return out.build();
        }
    }

    /** A wall between two close zombies: the bolt cannot jump through it (PFX-08). */
    private static final class Wall implements Scenario {

        @Override
        public String name() {
            return "projectile.tesla.wall";
        }

        @Override
        public String description() {
            return "A Tesla chain does not jump to a zombie behind a stone wall, and a lone target gets no forced chain (PFX-08).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ProjectileScenarios.corridor(ctx, 16);
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    ctx.setBlock(10, dy, dz, Blocks.stone, 0);
                }
            }
            FakePlayer gunman = ProjectileScenarios.shooter(ctx);
            EntityZombie near = ProjectileScenarios.zombie(ctx, 8);
            EntityZombie behind = ProjectileScenarios.zombie(ctx, 12);
            shoot(ctx, gunman);
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.check(lost(near) > 0.0F, "the zombie before the wall should be hurt");
            out.check(lost(behind) == 0.0F, "the zombie behind the wall must not be chained, lost " + lost(behind));
            return out.build();
        }
    }
}
