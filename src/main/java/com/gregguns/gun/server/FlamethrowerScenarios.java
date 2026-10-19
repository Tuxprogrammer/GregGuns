package com.gregguns.gun.server;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.FlamethrowerDef;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.logic.SpreadModel;
import com.gregguns.item.gun.GunItems;
import com.gregguns.projectile.FlamethrowerProjectile;
import com.gregguns.projectile.Projectiles;
import com.gregguns.recipe.GunRecipeScenarios;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

/**
 * The {@code gun.flamethrower.*} scenarios: the Flamethrower fired, reloaded from a full Fuel Tank, refused an empty
 * one, and its flame measured on a target; a flame fired by an NPC; the tank filled by GT's Fluid Canner with no
 * GregGuns recipe (RC-56); and the assembly recipe. Through the same server handler as the other guns, using
 * {@link GunRig}. Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Flamethrower); docs/gregguns-spec/02-guns/fluid-ammo.md
// (GGS-02-30); docs/testing/README.md
public final class FlamethrowerScenarios {

    private static final GunStats STATS = FlamethrowerDef.STATS;

    private FlamethrowerScenarios() {}

    /**
     * Registers the flamethrower scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fire());
        registry.register(new Reload());
        registry.register(new ForcedReload());
        registry.register(new EmptyTank());
        registry.register(new NpcFlame());
        registry.register(new CannerFill());
        registry.register(
            GunRecipeScenarios.assembly(
                "gun.flamethrower.recipe",
                "The Flamethrower is in GT's assembler map (so NEI lists it) at HV with its sheet inputs, not below HV, and the found recipe's output is ours.",
                FlamethrowerDef.DEFINITION.assembly()));
    }

    private static GunRig rig(ScenarioContext ctx, int loaded) {
        return new GunRig(ctx, GunItems.get(FlamethrowerDef.NAME), loaded);
    }

    private static Item tankItem() {
        return GameRegistry.findItem("gregguns", AmmoIds.FUEL_TANK);
    }

    /** @return a Fuel Tank holding 1,000 mB of Diesel */
    private static ItemStack fullTank() {
        ItemStack tank = new ItemStack(tankItem());
        ((IFluidContainerItem) tankItem()).fill(tank, Materials.Diesel.getFluid(1000), true);
        return tank;
    }

    /** @return how many empty Fuel Tanks (no fluid) the rig's shooter carries */
    private static int emptyTanks(GunRig rig) {
        int n = 0;
        for (ItemStack s : rig.shooter.inventory.mainInventory) {
            if (s != null && s.getItem() == tankItem() && ((IFluidContainerItem) tankItem()).getFluid(s) == null) {
                n += s.stackSize;
            }
        }
        return n;
    }

    /** Holding the trigger fires a flame every 2 ticks that hurts and ignites a target in front. */
    private static final class Fire implements Scenario {

        private static final int HELD_TICKS = 9;

        @Override
        public String name() {
            return "gun.flamethrower.fire";
        }

        @Override
        public String description() {
            return "Holding the trigger on the Flamethrower fires every 2 ticks, one round and one flame stream each; the streams are FlamethrowerProjectile entities and a target in front takes damage and burns.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            EntityZombie target = GunRig.zombie(ctx, 4);
            target.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.maxHealth)
                .setBaseValue(200.0);
            target.setHealth(200.0F);
            rig.press();
            return ScenarioResult.pending(name(), HELD_TICKS + 20, ticks -> {
                rig.tick();
                if (ticks < HELD_TICKS) {
                    return null;
                }
                rig.release();
                int expected = 1 + (HELD_TICKS - 1) / STATS.fireRateTicks();
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.checkEquals("rounds spent in " + HELD_TICKS + " held ticks", expected, rig.shots());
                out.checkEquals("rounds left", STATS.magazineSize() - expected, rig.loaded());
                out.checkEquals("flame streams launched", expected, rig.bullets());
                out.check(flameSeen(ctx.world()), "the streams should be FlamethrowerProjectile entities");
                out.check(target.getHealth() < target.getMaxHealth(), "the flame should have hurt the target");
                out.check(target.isBurning(), "a flame fired by a player should set its target alight");
                return out.build();
            });
        }

        /** A stream still in flight (those that hit have ended), or one recorded by the rig as a flame. */
        private static boolean flameSeen(World world) {
            for (Object o : world.loadedEntityList) {
                if (o instanceof FlamethrowerProjectile) {
                    return true;
                }
            }
            return false;
        }
    }

    /** One full Fuel Tank fills an empty gun with 100 rounds after 45 ticks and an empty tank comes back. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.flamethrower.reload";
        }

        @Override
        public String description() {
            return "An empty Flamethrower reloads 45 ticks after the trigger: 100 rounds from one full Fuel Tank, which is consumed, and an empty Fuel Tank comes back.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 0);
            rig.shooter.inventory.addItemStackToInventory(fullTank());
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
                out.checkEquals("rounds one tick before the end", 0, loadedAt[reloadTicks - 1]);
                out.checkEquals("rounds at the end of the reload", 100, loadedAt[reloadTicks]);
                out.checkEquals("full tanks left", 0, rig.count(AmmoIds.FUEL_TANK));
                out.checkEquals("empty tanks returned", 1, emptyTanks(rig));
                out.checkEquals("flames launched", 0, rig.bullets());
                return out.build();
            });
        }
    }

    /** GUN-13 / the spec's "no refund": a forced reload spends the tank whole and loses the fuel left in the gun. */
    private static final class ForcedReload implements Scenario {

        @Override
        public String name() {
            return "gun.flamethrower.reload.forced";
        }

        @Override
        public String description() {
            return "The reload key on a Flamethrower with 60 rounds swaps in a full tank for 100 rounds and returns one empty tank, with no refund of the fuel left.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 60);
            rig.shooter.inventory.addItemStackToInventory(fullTank());
            int reloadTicks = STATS.reloadTicks();
            rig.tick();
            ServerGunHandler.onReloadKey(rig.shooter);
            boolean started = rig.reloading();
            return ScenarioResult.pending(name(), reloadTicks + 20, ticks -> {
                rig.tick();
                if (ticks < reloadTicks + 1) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(started, "the reload key should start a forced reload");
                out.checkEquals("rounds after the swap", 100, rig.loaded());
                out.checkEquals("full tanks left", 0, rig.count(AmmoIds.FUEL_TANK));
                out.checkEquals("empty tanks (one returned, no refund of the 60 rounds)", 1, emptyTanks(rig));
                return out.build();
            });
        }
    }

    /** An empty tank is not ammo: the gun does not reload from it. */
    private static final class EmptyTank implements Scenario {

        @Override
        public String name() {
            return "gun.flamethrower.emptytank";
        }

        @Override
        public String description() {
            return "An empty Flamethrower with only an empty Fuel Tank in the inventory does not start a reload: an empty tank is not a magazine.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, 0);
            rig.give(AmmoIds.FUEL_TANK, 2);
            rig.press();
            rig.release();
            rig.tick();
            rig.tick();
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            out.checkEquals("full tanks counted as ammo", 0, rig.count(AmmoIds.FUEL_TANK));
            out.check(!rig.reloading(), "a reload should not start from empty tanks");
            out.checkEquals("rounds", 0, rig.loaded());
            out.checkEquals("empty tanks kept", 2, emptyTanks(rig));
            return out.build();
        }
    }

    /** A flame fired by an NPC hurts but never ignites (the spec's "NPC flamethrowers never ignite"). */
    private static final class NpcFlame implements Scenario {

        private static final int WAIT = 12;

        @Override
        public String name() {
            return "gun.flamethrower.npc";
        }

        @Override
        public String description() {
            return "A flame fired by a zombie at another zombie 3 blocks away damages it but does not set it alight, while the same flame fired by a player does.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            EntityZombie shooter = GunRig.zombie(ctx, 0);
            EntityZombie target = GunRig.zombie(ctx, 3);
            shooter.rotationYawHead = -90.0F;
            shooter.rotationYaw = -90.0F;
            for (EntityZombie z : new EntityZombie[] { shooter, target }) {
                z.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.maxHealth)
                    .setBaseValue(200.0);
                z.setHealth(200.0F);
            }
            ShotOffset offset = SpreadModel.roll(new java.util.Random(7L), STATS, ShotContext.player(false, 0.0), 0)[0];
            Projectiles.launcher()
                .launch(ctx.world(), shooter, STATS, offset, 1.0F);
            FlamethrowerProjectile[] mine = { null };
            for (Object o : ctx.world().loadedEntityList) {
                if (o instanceof FlamethrowerProjectile && ((FlamethrowerProjectile) o).getShooter() == shooter) {
                    mine[0] = (FlamethrowerProjectile) o;
                }
            }
            return ScenarioResult.pending(name(), WAIT + 10, ticks -> {
                if (ticks < WAIT) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(target.getHealth() < target.getMaxHealth(), "the NPC's flame should have hurt the target");
                out.check(!target.isBurning(), "an NPC's flame must not ignite its target");
                out.check(mine[0] != null, "the launcher should have spawned the NPC's flame");
                out.check(mine[0] != null && !mine[0].ignites(), "an NPC's flame is not an igniting flame");
                return out.build();
            });
        }
    }

    /** GT's Fluid Canner fills an empty tank with 1,000 mB of fuel and empties it again, with no GregGuns recipe. */
    private static final class CannerFill implements Scenario {

        @Override
        public String name() {
            return "gun.flamethrower.tankfill";
        }

        @Override
        public String description() {
            return "GT's Fluid Canner map, with no GregGuns recipe, turns an empty Fuel Tank and 1,000 mB of Diesel into a full tank (RC-56), and a full tank into an empty one and the Diesel; Water is not accepted.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemStack empty = new ItemStack(tankItem());
            GTRecipe fill = RecipeMaps.cannerRecipes.findRecipeQuery()
                .items(empty)
                .fluids(Materials.Diesel.getFluid(1000))
                .voltage(GTValues.V[1])
                .find();
            out.check(fill != null, "the canner should fill an empty tank with 1,000 mB of Diesel");
            if (fill != null) {
                ItemStack made = fill.mOutputs.length > 0 ? fill.mOutputs[0] : null;
                FluidStack inside = made == null ? null : ((IFluidContainerItem) tankItem()).getFluid(made);
                out.check(inside != null && inside.amount == 1000, "the filled tank should hold 1,000 mB");
            }
            GTRecipe water = RecipeMaps.cannerRecipes.findRecipeQuery()
                .items(new ItemStack(tankItem()))
                .fluids(Materials.Water.getFluid(1000))
                .voltage(GTValues.V[1])
                .find();
            out.check(
                water == null || water.mOutputs.length == 0
                    || water.mOutputs[0] == null
                    || ((IFluidContainerItem) tankItem()).getFluid(water.mOutputs[0]) == null,
                "the canner should not fill a Fuel Tank with water");
            GTRecipe drain = RecipeMaps.cannerRecipes.findRecipeQuery()
                .items(fullTank())
                .voltage(GTValues.V[1])
                .find();
            out.check(drain != null, "the canner should empty a full tank");
            if (drain != null) {
                boolean tankBack = false;
                for (ItemStack o : drain.mOutputs) {
                    tankBack |= o != null && o.getItem() == tankItem()
                        && ((IFluidContainerItem) tankItem()).getFluid(o) == null;
                }
                out.check(tankBack, "emptying should return an empty tank");
            }
            return out.build();
        }
    }
}
