package com.gregguns.gun.server;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.NetherBlasterDef;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.ammo.def.NetherChargeAmmo;
import com.gregguns.item.gun.GunItems;
import com.gregguns.projectile.NetherBlasterProjectile;
import com.gregguns.recipe.GunLineScenarios;
import com.gregguns.recipe.GunLineTable;

/**
 * The {@code gun.nether_blaster.*} scenarios: a held trigger fires burning bolts that hurt and set a villager alight,
 * one Nether Charge reloads the gun to the charge's own shot count and returns nothing, block fire obeys safe mode,
 * and the LuV Assembly Line recipe. Through the same server handler as the other guns, using {@link GunRig}. Dev
 * environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Nether Blaster); docs/techguns-spec/04-projectiles-fx/
// projectiles.md (3.8); docs/testing/README.md
public final class NetherBlasterScenarios {

    private static final GunStats STATS = NetherBlasterDef.STATS;

    private NetherBlasterScenarios() {}

    /**
     * Registers the Nether Blaster scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fire());
        registry.register(new Reload());
        registry.register(new BlockFire("gun.nether_blaster.fire.block", true));
        registry.register(new BlockFire("gun.nether_blaster.fire.safe", false));
        registry.register(
            GunLineScenarios.recipe(
                "gun.nether_blaster.recipe",
                "The LuV Assembly Line recipe (GR-100) is on GT's line list with 45 s, the LuV EU/t, its research item and a scan at the tier below, and the output is ours; the line's EU/t fits LuV.",
                GunLineTable.NETHER_BLASTER));
    }

    private static GunRig rig(ScenarioContext ctx, int loaded) {
        return new GunRig(ctx, GunItems.get(NetherBlasterDef.NAME), loaded);
    }

    /** Holding the trigger fires a bolt every 8 ticks; the bolts hurt a villager and set it alight. */
    private static final class Fire implements Scenario {

        private static final int HELD_TICKS = 17;
        private static final int CHECK_TICK = 24;

        @Override
        public String name() {
            return "gun.nether_blaster.fire";
        }

        @Override
        public String description() {
            return "Holding the trigger on the Nether Blaster for 17 ticks fires 3 bolts (every 8 ticks), spending 3 of the 10 rounds; each is a NetherBlasterProjectile and a villager 4 blocks ahead is hurt and burning.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            EntityVillager target = new EntityVillager(ctx.world());
            target.setLocationAndAngles(ctx.originX() + 4.5, ctx.originY(), ctx.originZ() + 0.5, 90.0F, 0.0F);
            ctx.spawn(target);
            target.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                .setBaseValue(200.0);
            target.setHealth(200.0F);
            int[] tick = { 0 };
            boolean[] burned = { false };
            boolean[] bolt = { false };
            rig.press();
            return ScenarioResult.pending(name(), CHECK_TICK + 20, ticks -> {
                rig.tick();
                burned[0] |= target.isBurning();
                for (Object o : ctx.world().loadedEntityList) {
                    bolt[0] |= o instanceof NetherBlasterProjectile;
                }
                if (++tick[0] == HELD_TICKS) {
                    rig.release();
                }
                if (tick[0] < CHECK_TICK) {
                    return null;
                }
                int expected = 1 + (HELD_TICKS - 1) / STATS.fireRateTicks();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("rounds spent in " + HELD_TICKS + " held ticks", expected, rig.shots());
                out.checkEquals("rounds left", STATS.magazineSize() - expected, rig.loaded());
                out.checkEquals("bolts launched", expected, rig.bullets());
                out.check(bolt[0], "the bolts should be NetherBlasterProjectile entities");
                out.check(target.getHealth() < target.getMaxHealth(), "the bolt should have hurt the target");
                out.check(burned[0], "the bolt should have set the target alight");
                return out.build();
            });
        }
    }

    /** One Nether Charge reloads the gun to the charge's shot count and nothing comes back. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.nether_blaster.reload";
        }

        @Override
        public String description() {
            return "An empty Nether Blaster with two Nether Charges reloads 35 ticks after the trigger to NetherChargeAmmo.SHOTS_PER_CHARGE (10) rounds, consuming exactly one charge and returning no empty item; the clip equals that constant.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 0);
            rig.give(AmmoIds.NETHER_CHARGE, 2);
            int reloadTicks = STATS.reloadTicks();
            rig.press();
            rig.release();
            rig.tick();
            boolean started = rig.reloading();
            int[] loadedAt = new int[reloadTicks + 3];
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < loadedAt.length) {
                    loadedAt[ticks] = rig.loaded();
                }
                if (ticks < reloadTicks + 2) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the dry trigger should have started a reload");
                out.checkEquals(
                    "the clip is one charge's shots",
                    NetherChargeAmmo.SHOTS_PER_CHARGE,
                    STATS.magazineSize());
                out.checkEquals("rounds one tick before the end", 0, loadedAt[reloadTicks - 1]);
                out.checkEquals(
                    "rounds at the end of the reload",
                    NetherChargeAmmo.SHOTS_PER_CHARGE,
                    loadedAt[reloadTicks]);
                out.checkEquals("charges left of two", 1, rig.count(AmmoIds.NETHER_CHARGE));
                int stacks = 0;
                for (ItemStack s : rig.shooter.inventory.mainInventory) {
                    if (s != null && s != rig.gun) {
                        stacks += s.stackSize;
                    }
                }
                out.checkEquals("items besides the gun: the one charge left, no empty item", 1, stacks);
                return out.build();
            });
        }
    }

    /** Ten bolts at a wall ledge: fire appears with safe mode off and never with it on. */
    private static final class BlockFire implements Scenario {

        private static final int WALL_DX = 6;
        private static final int TICKS = 100;

        private final String name;
        private final boolean blockDamage;

        BlockFire(String name, boolean blockDamage) {
            this.name = name;
            this.blockDamage = blockDamage;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return blockDamage
                ? "With safe mode off (explosions.blockDamage true), ten bolts from a player at a wall, with a ledge so fire can stand, place fire in front of the wall at least once (half of the hits do)."
                : "With safe mode on (explosions.blockDamage false) the same ten bolts place no fire (GUN-05: the ignite flag no longer doubles as block damage).";
        }

        private static boolean fireInFront(ScenarioContext ctx) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (ctx.world()
                        .getBlock(ctx.originX() + WALL_DX - 1, ctx.originY() + dy, ctx.originZ() + dz) == Blocks.fire) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ctx.setBlock(WALL_DX, dy, dz, Blocks.stone, 0);
                }
            }
            // The ledge under the cell in front of the eye-height hit, so fire can be placed there.
            for (int dz = -1; dz <= 1; dz++) {
                ctx.setBlock(WALL_DX - 1, 0, dz, Blocks.stone, 0);
            }
            boolean saved = ExplosionsConfig.blockDamage;
            ExplosionsConfig.blockDamage = blockDamage;
            rig.press();
            int[] tick = { 0 };
            boolean[] sawFire = { false };
            return ScenarioResult.pending(name, TICKS + 20, ticks -> {
                rig.tick();
                sawFire[0] |= fireInFront(ctx);
                if (++tick[0] < TICKS) {
                    return null;
                }
                rig.release();
                ExplosionsConfig.blockDamage = saved;
                for (int dy = 0; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (ctx.world()
                            .getBlock(ctx.originX() + WALL_DX - 1, ctx.originY() + dy, ctx.originZ() + dz)
                            == Blocks.fire) {
                            ctx.setBlock(WALL_DX - 1, dy, dz, Blocks.air, 0);
                        }
                    }
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name);
                out.checkEquals("all ten rounds were fired", STATS.magazineSize(), rig.shots());
                out.checkEquals(blockDamage ? "fire was placed" : "no fire was placed", blockDamage, sawFire[0]);
                return out.build();
            });
        }
    }
}
