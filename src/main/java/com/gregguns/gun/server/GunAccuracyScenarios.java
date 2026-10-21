package com.gregguns.gun.server;

import java.util.Collections;
import java.util.Random;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.api.Tier;
import com.gregguns.armor.ItemGregGunsArmor;
import com.gregguns.armor.logic.ArmorBonus;
import com.gregguns.armor.logic.ArmorMaterialDef;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.BonusMap;
import com.gregguns.armor.logic.PieceDef;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.GunAccuracy;
import com.gregguns.gun.def.PistolDef;
import com.gregguns.gun.def.SawedOffDef;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.logic.SpreadModel;

/**
 * {@code gun.accuracy.armor}: the shot context the server builds for a player reads the {@code GUN_ACCURACY} bonus of
 * the armor that player wears. No shipped set gives that bonus yet, so the scenario wears a stub one-piece set (built
 * from the armor module's own classes, never registered) on one fake player and nothing on another, then rolls the
 * same seeded shots for both: the armored shooter's offsets are smaller by exactly the bonus, for the pistol's single
 * bullet and for the Sawed Off's pellets. With {@code modules.armor} off the bonus must read 0 even in the stub.
 * Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 5 (armor accuracy bonus)
public final class GunAccuracyScenarios {

    /** The stub piece's bonus: 20 percent tighter spread. */
    static final double STUB_BONUS = 0.2;
    private static final int ROLLS = 2000;

    private GunAccuracyScenarios() {}

    /**
     * Registers the accuracy scenario.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new ArmorAccuracy());
    }

    /** Builds the stub piece once, on first use (the item constructor registers a vanilla armor material). */
    private static final class Stub {

        static final ItemGregGunsArmor CHEST = build();

        private Stub() {}

        private static ItemGregGunsArmor build() {
            SetDef set = new SetDef(
                "test_accuracy",
                Tier.LV,
                ArmorMaterialDef.uniform("test_accuracy", 60, 10.0),
                Collections.singletonList("t1_combat"),
                BonusMap.EMPTY,
                PieceDef.builder(ArmorSlot.CHESTPLATE, "Accuracy Test Vest")
                    .bonuses(BonusMap.of(ArmorBonus.GUN_ACCURACY, STUB_BONUS))
                    .build());
            return new ItemGregGunsArmor(
                set,
                set.pieces()
                    .get(ArmorSlot.CHESTPLATE));
        }
    }

    /** Mean absolute yaw and pitch offset, in degrees, of {@code ROLLS} seeded shots (every projectile counts). */
    private static double meanOffset(GunStats stats, ShotContext context) {
        Random random = new Random(20240601L);
        double sum = 0.0;
        int n = 0;
        for (int i = 0; i < ROLLS; i++) {
            for (ShotOffset offset : SpreadModel.roll(random, stats, context, 0)) {
                sum += Math.abs(offset.yawDegrees()) + Math.abs(offset.pitchDegrees());
                n += 2;
            }
        }
        return sum / n;
    }

    private static final class ArmorAccuracy implements Scenario {

        @Override
        public String name() {
            return "gun.accuracy.armor";
        }

        @Override
        public String description() {
            return "A fake player wearing a stub set that gives GUN_ACCURACY 0.2 shoots 20 percent tighter than one wearing nothing (pistol bullet and Sawed Off pellets alike); with modules.armor off the bonus reads 0.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            FakePlayer bare = GunRig.fakePlayer(ctx, "[GGAccBare]", 0);
            FakePlayer armored = GunRig.fakePlayer(ctx, "[GGAccArmor]", 1);
            // Vanilla armor slots: 1 boots, 2 leggings, 3 chestplate, 4 helmet.
            armored.setCurrentItemOrArmor(3, new ItemStack(Stub.CHEST));

            boolean armorOn = ModuleState.isActive(ModuleId.ARMOR);
            double bonusBare = GunAccuracy.armorBonus(bare);
            double bonusArmored = GunAccuracy.armorBonus(armored);
            out.check(bonusBare == 0.0, "a player in no armor has no accuracy bonus but read " + bonusBare);
            out.check(GunAccuracy.armorBonus(null) == 0.0, "no shooter reads 0");
            if (!armorOn) {
                out.check(bonusArmored == 0.0, "with modules.armor off the bonus must read 0 but read " + bonusArmored);
                return out.build();
            }
            out.check(
                Math.abs(bonusArmored - STUB_BONUS) < 1e-9,
                "the stub vest should give " + STUB_BONUS + " but the bonus read " + bonusArmored);

            for (GunStats stats : new GunStats[] { PistolDef.STATS, SawedOffDef.STATS }) {
                ShotContext without = GunAccuracy.shotContext(bare, false);
                ShotContext with = GunAccuracy.shotContext(armored, false);
                double spreadWithout = SpreadModel.mainSpread(stats, without);
                double spreadWith = SpreadModel.mainSpread(stats, with);
                out.check(
                    Math.abs(spreadWith - spreadWithout * (1.0 - STUB_BONUS)) < 1e-12,
                    stats.id() + " spread should shrink by the bonus: " + spreadWithout + " -> " + spreadWith);
                double measuredWithout = meanOffset(stats, without);
                double measuredWith = meanOffset(stats, with);
                out.check(
                    measuredWith < measuredWithout,
                    stats.id() + " armored shots should land tighter: " + measuredWith + " vs " + measuredWithout);
                out.check(
                    Math.abs(measuredWith / measuredWithout - (1.0 - STUB_BONUS)) < 1e-9,
                    stats.id() + " the same seeded rolls should shrink by exactly the bonus: "
                        + measuredWith / measuredWithout);
            }
            return out.build();
        }
    }
}
