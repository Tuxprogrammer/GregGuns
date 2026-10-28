package com.gregguns.icbm.launcher;

import java.util.Random;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.NukeLauncherDef;
import com.gregguns.gun.def.RocketLauncherDef;
import com.gregguns.gun.logic.ShotContext;
import com.gregguns.gun.logic.ShotOffset;
import com.gregguns.gun.logic.SpreadModel;
import com.gregguns.icbm.blast.BlastEngine;
import com.gregguns.item.gun.GunItems;
import com.gregguns.projectile.Projectiles;
import com.gregguns.projectile.RocketProjectile;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

/** Dev-only scenario of the launcher warheads (D150). */
// Spec: docs/gregguns-icbm-spec/02-explosives/launcher-warheads.md
final class LauncherWarheadScenarios {

    private LauncherWarheadScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(com.gregguns.explosion.GrenadeScenarios.blockDamageOff(new LoadAndFire()));
        registry.register(new Recipes());
    }

    private static InventoryCrafting grid(ItemStack... stacks) {
        InventoryCrafting g = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer p) {
                return true;
            }
        }, 3, 3);
        for (int i = 0; i < stacks.length; i++) {
            g.setInventorySlotContents(i, stacks[i]);
        }
        return g;
    }

    /** The loading recipe, the cap, the family check, and a rocket that spends a charge and detonates the blast. */
    private static final class LoadAndFire implements Scenario {

        @Override
        public String name() {
            return "icbm.launcher.load_and_fire";
        }

        @Override
        public String description() {
            return "A launcher warhead crafted onto a Rocket Launcher loads four charges; a shot spends one and starts the ICBM blast instead of the rocket's own; a gun of another family refuses it.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!ModuleState.isActive(ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), "the ICBM module is off (Galacticraft missing)");
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            LauncherWarheadRecipe recipe = new LauncherWarheadRecipe();
            ItemStack launcher = new ItemStack(GunItems.get(RocketLauncherDef.NAME));
            ItemStack he = LauncherWarheadRegistration.stack(LauncherWarhead.HE);
            ItemStack emp = LauncherWarheadRegistration.stack(LauncherWarhead.EMP);
            ItemStack nuke = new ItemStack(GunItems.get(NukeLauncherDef.NAME));
            out.check(he != null && emp != null, "the warhead items are not registered");

            out.check(recipe.matches(grid(launcher, he), ctx.world()), "launcher plus HE should match");
            out.check(
                !recipe.matches(grid(launcher, emp), ctx.world()),
                "an EMP warhead does not fit the Rocket Launcher");
            out.check(!recipe.matches(grid(launcher), ctx.world()), "a launcher alone is not a recipe");
            out.check(recipe.matches(grid(nuke, emp), ctx.world()), "the Nuke Launcher takes the EMP warhead");
            ItemStack loaded = recipe.getCraftingResult(grid(launcher, he));
            out.checkEquals(
                "charges after one craft",
                LauncherRules.CHARGES_PER_ITEM,
                LauncherWarheads.charges(loaded));
            out.check(LauncherWarheads.loaded(loaded) == LauncherWarhead.HE, "the loaded warhead is HE");
            out.check(!LauncherWarheads.hasWarhead(launcher), "the input launcher is not changed");
            ItemStack twice = recipe.getCraftingResult(grid(loaded, he));
            out.checkEquals(
                "charges after two crafts",
                2 * LauncherRules.CHARGES_PER_ITEM,
                LauncherWarheads.charges(twice));

            // A zombie fires the loaded launcher straight down: the rocket spends one charge and the blast starts.
            ctx.clearArea(8, 6);
            ctx.setBlock(0, -1, 0, Blocks.stone, 0);
            EntityZombie shooter = ctx.spawnAt(
                new EntityZombie(ctx.world()),
                ctx.originX() + 0.5,
                ctx.originY(),
                ctx.originZ() + 0.5,
                0.0F,
                90.0F);
            shooter.setCurrentItemOrArmor(0, loaded);
            ShotOffset offset = SpreadModel.roll(new Random(5L), RocketLauncherDef.STATS, ShotContext.DEFAULT, 0)[0];
            BlastEngine engine = BlastEngine.instance();
            engine.clear();
            long startsBefore = engine.acceptedStarts();
            // A rocket's own explosion posts ExplosionEvent.Start too; the ICBM blast posts exactly one, so a count of
            // one means the rocket's own blast did not also run (GGI-02-62).
            StartCounter counter = new StartCounter();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(counter);
            Projectiles.launcher()
                .launch(ctx.world(), shooter, RocketLauncherDef.STATS, offset, 1.0F);
            out.checkEquals(
                "charges after one shot",
                LauncherRules.CHARGES_PER_ITEM - 1,
                LauncherWarheads.charges(loaded));
            return ScenarioResult.pending(name(), 80, ticks -> {
                boolean rocket = false;
                for (Object o : ctx.world().loadedEntityList) {
                    rocket |= o instanceof RocketProjectile && !((RocketProjectile) o).isDead;
                }
                if (rocket && ticks < 75) {
                    return null;
                }
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(counter);
                out.checkEquals("blasts started by the one shot", 1L, engine.acceptedStarts() - startsBefore);
                out.checkEquals("the blast type", "condensed", engine.lastExplosiveId());
                out.checkEquals("explosion starts (the rocket's own did not run)", 1, counter.count);
                out.checkEquals("the center damage cap (D154)", 75.0, engine.lastDamageCap());
                out.check(
                    !engine.lastBreaksTerrain(),
                    "explosions.blockDamage is off, so a hand-held launcher blast must not break terrain (D52)");
                engine.clear();
                shooter.setDead();
                return out.build();
            });
        }
    }

    /** One Assembler recipe per item at its tier, and no recycling data (the I3 rubble lesson). */
    private static final class Recipes implements Scenario {

        @Override
        public String name() {
            return "icbm.launcher.recipes";
        }

        @Override
        public String description() {
            return "Every launcher warhead and the antidote has exactly one Assembler recipe at its tier, and no GT item data that could recycle into free materials.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!ModuleState.isActive(ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), "the ICBM module is off (Galacticraft missing)");
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (LauncherWarhead w : LauncherWarhead.values()) {
                ItemStack item = LauncherWarheadRegistration.stack(w);
                int found = 0;
                for (GTRecipe r : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                    if (r.mOutputs != null && r.mOutputs.length > 0
                        && r.mOutputs[0] != null
                        && r.mOutputs[0].getItem() == item.getItem()) {
                        found++;
                        out.checkEquals(
                            w + " voltage",
                            (int) GTValues.VP[w.tier()
                                .voltageIndex()],
                            r.mEUt);
                    }
                }
                out.checkEquals(w + " recipe count", 1, found);
                out.check(GTOreDictUnificator.getAssociation(item) == null, w + " must carry no GT item data");
            }
            ItemStack antidote = new ItemStack(com.gregguns.icbm.antidote.AntidoteRegistration.item());
            int found = 0;
            for (GTRecipe r : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                if (r.mOutputs != null && r.mOutputs.length > 0
                    && r.mOutputs[0] != null
                    && r.mOutputs[0].getItem() == antidote.getItem()) {
                    found++;
                    out.checkEquals(
                        "antidote yield",
                        com.gregguns.icbm.antidote.AntidoteRules.PER_RUN,
                        r.mOutputs[0].stackSize);
                }
            }
            out.checkEquals("antidote recipe count", 1, found);
            out.check(GTOreDictUnificator.getAssociation(antidote) == null, "the antidote must carry no GT item data");
            return out.build();
        }
    }

    /** Counts {@code ExplosionEvent.Start}. Public for FML. */
    public static final class StartCounter {

        int count;

        /** Counts one start. */
        @cpw.mods.fml.common.eventhandler.SubscribeEvent
        public void onStart(net.minecraftforge.event.world.ExplosionEvent.Start event) {
            count++;
        }
    }
}
