package com.gregguns.gun.server;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.monster.EntityZombie;

import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.def.GunDefinitions;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ToolDrain;
import com.gregguns.item.gun.ItemEnergyGun;
import com.gregguns.item.gun.ItemGun;
import com.gregguns.item.gun.ItemToolGun;
import com.gregguns.projectile.GenericProjectile;

/**
 * {@code gun.all.spends_ammo}: a regression net over every registered gun. A survival fake player holds the gun with
 * its ammo loaded and pulls the trigger once; the ammo must go down by what the spec says one pull costs. Created after
 * an owner report that the Pulse Rifle spent nothing (it was creative mode, which skips ammo, but the net stays).
 * <ul>
 * <li>magazine guns, including the Bio Gun (its Bio Tank fluid is loaded as rounds) and burst guns: the loaded count
 * drops by {@link GunStats#ammoPerShot()} (one round per pull for a burst line, two for a double-shot akimbo
 * pair);</li>
 * <li>energy guns: the stored EU drops by {@link GunStats#euPerShot()};</li>
 * <li>tool guns: one melee hit drops the loaded count by what {@link ToolDrain#spend} charges for a hit.</li>
 * </ul>
 * Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md; docs/techguns-spec/02-guns/sheets-rifles-shotguns.md (burst: one
// round per pull); docs/testing/README.md
public final class AmmoSpendScenarios {

    private static final int SETTLE_TICKS = 12;

    private AmmoSpendScenarios() {}

    /**
     * Registers the scenario.
     *
     * @param registry where to add it
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new SpendsAmmo());
    }

    private static final class SpendsAmmo implements Scenario {

        @Override
        public String name() {
            return "gun.all.spends_ammo";
        }

        @Override
        public String description() {
            return "For every registered gun, a survival fake player pulls the trigger once and the ammo drops by the cost of one pull: rounds per shot for magazine, burst and Bio Gun, EU per shot for energy guns, the hit cost for tool guns.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            List<String> bad = new ArrayList<>();
            int checked = 0;
            for (GunDefinition def : GunDefinitions.ALL) {
                ItemGun item = com.gregguns.item.gun.GunItems.get(def.name());
                GunStats stats = def.stats();
                GunRig rig = new GunRig(ctx, item, stats.magazineSize());
                if (rig.shooter.capabilities.isCreativeMode) {
                    bad.add(def.name() + ": fake player is in creative mode");
                    continue;
                }
                String problem;
                if (stats.isEnergy()) {
                    problem = energy(rig, (ItemEnergyGun) item, def);
                } else if (def.tool() != null) {
                    problem = tool(ctx, rig, def);
                } else {
                    problem = magazine(rig, stats);
                }
                if (problem != null) {
                    bad.add(def.name() + ": " + problem);
                }
                // Clean up: end the bullets, drop the fake player, never leave drops or targets behind.
                for (GenericProjectile p : rig.seenProjectiles()) {
                    p.setDead();
                }
                ctx.world()
                    .removeEntity(rig.shooter);
                checked++;
            }
            out.check(checked == GunDefinitions.ALL.size(), "every gun was checked, " + checked + " done");
            out.check(bad.isEmpty(), "every gun spends its ammo: " + bad);
            return out.build();
        }

        private static String magazine(GunRig rig, GunStats stats) {
            rig.press();
            rig.release();
            // One tick fires the buffered pull; a wind-up gun gets a few more. Released, so nothing repeats.
            for (int i = 0; i < SETTLE_TICKS && rig.loaded() == stats.magazineSize(); i++) {
                rig.tick();
            }
            for (int i = 0; i < 3; i++) {
                rig.tick();
            }
            int spent = stats.magazineSize() - rig.loaded();
            return spent == stats.ammoPerShot() ? null
                : "one pull spent " + spent + ", expected " + stats.ammoPerShot();
        }

        private static String energy(GunRig rig, ItemEnergyGun item, GunDefinition def) {
            GunStats stats = def.stats();
            ElectricGear.charge(rig.gun, item.capacityEu(), def.tier());
            long before = item.chargeOf(rig.gun);
            rig.press();
            rig.release();
            for (int i = 0; i < SETTLE_TICKS && item.chargeOf(rig.gun) == before; i++) {
                rig.tick();
            }
            for (int i = 0; i < 3; i++) {
                rig.tick();
            }
            long spent = before - item.chargeOf(rig.gun);
            return spent == stats.euPerShot() ? null : "one pull spent " + spent + " EU, expected " + stats.euPerShot();
        }

        private static String tool(ScenarioContext ctx, GunRig rig, GunDefinition def) {
            if (!(rig.gun.getItem() instanceof ItemToolGun)) {
                return "tool definition without an ItemToolGun";
            }
            GunStats stats = def.stats();
            rig.shooter.getAttributeMap()
                .applyAttributeModifiers(rig.gun.getAttributeModifiers());
            // A hit may cost less than a round, so swing enough times for the cost to add up to at least one.
            int cost = Math.max(
                1,
                def.tool()
                    .hitCostMilli());
            int hits = (ToolDrain.MILLI + cost - 1) / cost;
            int expected = 0;
            int remainder = 0;
            int loaded = stats.magazineSize();
            for (int i = 0; i < hits; i++) {
                ToolDrain.Spend s = ToolDrain.spend(loaded - expected, remainder, cost);
                expected += s.rounds();
                remainder = s.remainderMilli();
                EntityZombie target = GunRig.zombie(ctx, 2);
                rig.shooter.attackTargetEntityWithCurrentItem(target);
                target.setDead();
            }
            int spent = loaded - rig.loaded();
            return spent == expected ? null : hits + " hits spent " + spent + " rounds, expected " + expected;
        }
    }
}
