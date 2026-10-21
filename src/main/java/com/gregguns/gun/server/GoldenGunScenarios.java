package com.gregguns.gun.server;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GoldenGunDef;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.ammo.def.GoldenRoundAmmo;
import com.gregguns.item.gun.GunItems;
import com.gregguns.recipe.GunRecipeScenarios;

/**
 * The {@code gun.golden_gun.*} scenarios: one Golden Round kills an armored zombie outright, takes half of a boss
 * stand-in's health, and the one-round chamber empties and refills from one round. Dev environment only, server side.
 */
// Spec: docs/gregguns-spec/02-guns/index.md (Golden Gun); docs/design/0016-damage-balance-pass.md
public final class GoldenGunScenarios {

    private static final GunStats STATS = GoldenGunDef.STATS;
    private static final float EPS = 0.5F;

    private GoldenGunScenarios() {}

    /**
     * Registers the Golden Gun scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new KillsArmored());
        registry.register(new HalvesBoss());
        registry.register(new Reload());
        registry.register(
            GunRecipeScenarios.upgrade(
                "gun.golden_gun.recipe",
                "The GR-104 upgrade is found with a used 9mm Pistol (any NBT) in slot 0 at LuV and outputs a fresh Golden Gun.",
                GoldenGunDef.DEFINITION.assembly()));
    }

    private static GunRig rig(ScenarioContext ctx, int loaded) {
        return new GunRig(ctx, GunItems.get(GoldenGunDef.NAME), loaded);
    }

    /** A boss stand-in: a zombie that carries the vanilla boss marker. */
    private static final class BossZombie extends EntityZombie implements IBossDisplayData {

        BossZombie(World world) {
            super(world);
        }
    }

    /** A full-health zombie in full diamond armor, 6 blocks east. */
    private static final class KillsArmored implements Scenario {

        @Override
        public String name() {
            return "gun.golden_gun.kill";
        }

        @Override
        public String description() {
            return "One Golden Gun shot kills a full-health zombie in full diamond armor outright; the magazine is then empty.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            EntityZombie target = GunRig.zombie(ctx, 6);
            target.setCurrentItemOrArmor(4, new ItemStack(Items.diamond_helmet));
            target.setCurrentItemOrArmor(3, new ItemStack(Items.diamond_chestplate));
            target.setCurrentItemOrArmor(2, new ItemStack(Items.diamond_leggings));
            target.setCurrentItemOrArmor(1, new ItemStack(Items.diamond_boots));
            target.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                .setBaseValue(500.0);
            target.setHealth(500.0F);
            rig.press();
            rig.release();
            rig.tick();
            return ScenarioResult.pending(name(), 40, ticks -> {
                rig.tick();
                if (!target.isDead && target.getHealth() > 0.0F && ticks < 30) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(target.getTotalArmorValue() > 10, "the zombie should be armored");
                out.check(target.isDead || target.getHealth() <= 0.0F, "the zombie should be dead");
                out.checkEquals("rounds left after one shot", 0, rig.loaded());
                out.checkEquals("bullets launched", 1, rig.bullets());
                return out.build();
            });
        }
    }

    /** A boss stand-in with 2000 health, 6 blocks east. */
    private static final class HalvesBoss implements Scenario {

        @Override
        public String name() {
            return "gun.golden_gun.boss";
        }

        @Override
        public String description() {
            return "One Golden Gun shot takes exactly half of a boss-marked zombie's 2000 maximum health and leaves it alive.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            EntityZombie boss = new BossZombie(ctx.world());
            boss.setLocationAndAngles(ctx.originX() + 6.5, ctx.originY(), ctx.originZ() + 0.5, 90.0F, 0.0F);
            boss.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                .setBaseValue(2000.0);
            boss.setHealth(2000.0F);
            boss.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 2000, 10));
            ctx.spawn(boss);
            rig.press();
            rig.release();
            rig.tick();
            return ScenarioResult.pending(name(), 40, ticks -> {
                rig.tick();
                if (boss.getHealth() >= boss.getMaxHealth() && ticks < 30) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(!boss.isDead, "a boss must survive one Golden Round");
                out.check(
                    Math.abs(boss.getHealth() - 1000.0F) < EPS,
                    "the boss should lose exactly half its 2000 health, but has " + boss.getHealth());
                return out.build();
            });
        }
    }

    /** One Golden Round fills the one-round chamber; the shot empties it again. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.golden_gun.reload";
        }

        @Override
        public String description() {
            return "A Golden Gun fired empty reloads 60 ticks after the trigger from one Golden Round (consumed, nothing returned), to a magazine of one.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            String round = GoldenRoundAmmo.TYPE.roundItemName();
            rig.give(round, 1);
            rig.press();
            rig.release();
            rig.tick();
            int afterShot = rig.loaded();
            int reloadTicks = STATS.reloadTicks();
            // The empty trigger pull starts the reload.
            return ScenarioResult.pending(name(), reloadTicks + 40, ticks -> {
                if (ticks == 6) {
                    rig.press();
                    rig.release();
                }
                rig.tick();
                if (ticks < reloadTicks + 8) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("rounds after the shot", 0, afterShot);
                out.checkEquals("rounds after the reload", 1, rig.loaded());
                out.checkEquals("Golden Rounds left", 0, rig.count(round));
                return out.build();
            });
        }
    }
}
