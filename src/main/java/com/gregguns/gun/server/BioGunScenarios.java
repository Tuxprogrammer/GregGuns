package com.gregguns.gun.server;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.gregguns.compat.BartWorksCulture;
import com.gregguns.core.config.BioGunConfig;
import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.BioGunDef;
import com.gregguns.gun.logic.GunCatalog.AmmoIds;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.GunItems;
import com.gregguns.projectile.BiogunProjectile;
import com.gregguns.recipe.BioCultureRecipes;
import com.gregguns.recipe.GunRecipeScenarios;
import com.gregguns.utility.WorldBlockRegistration;
import com.gregguns.utility.blob.TileBioBlob;

import bartworks.API.recipe.BartWorksRecipeMaps;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.GTUtility;

/**
 * The {@code gun.bio_gun.*} scenarios: the left button's level-1 glob that hurts and poisons (Poison IV), the right
 * button's charge giving levels 1 to 3 that spend that many rounds, the reload from a full Bio Tank, blob placement
 * under safe mode and a protection veto (PFX-28), and the EV assembly row with its tier gate. Through the same server
 * handler as the other guns, using {@link GunRig}. Dev environment only, server side.
 */
// Spec: docs/techguns-spec/02-guns/sheets-energy-special.md (Bio Gun); docs/techguns-spec/04-projectiles-fx/
// projectiles.md (3.5); docs/techguns-spec/02-guns/per-gun-behavior.md (Bio Gun); docs/testing/README.md
public final class BioGunScenarios {

    private static final GunStats STATS = BioGunDef.STATS;

    /** Set by the blob scenario that tests a protection mod's veto; read by {@link Veto}. */
    private static volatile boolean vetoPlacement;
    private static boolean vetoRegistered;

    private BioGunScenarios() {}

    /**
     * Registers the Bio Gun scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Fire());
        registry.register(new Charge());
        registry.register(new Reload());
        registry.register(new CultureRecipe());
        registry.register(new VatRecipeSelected());
        registry.register(new TankRejectsBiomass());
        registry.register(
            new Blob(
                "gun.bio_gun.blob",
                "A level 2 glob from a player fires at a wall with safe mode off: a size 2 Bio Blob sticks to the wall.",
                true,
                false,
                2));
        registry.register(
            new Blob(
                "gun.bio_gun.blob.safe",
                "The same glob with safe mode on (explosions.blockDamage false) leaves no blob (PFX-28).",
                false,
                false,
                0));
        registry.register(
            new Blob(
                "gun.bio_gun.blob.veto",
                "The same glob with safe mode off but BlockEvent.PlaceEvent cancelled by a protection mod leaves no blob (PFX-28).",
                true,
                true,
                0));
        registry.register(
            GunRecipeScenarios.assembly(
                "gun.bio_gun.recipe",
                "The EV assembly row (GR-65) is in GT's assembler map (so NEI lists it) with its sheet inputs, not below EV, and the found recipe's output is ours.",
                BioGunDef.DEFINITION.assembly()));
        registry.register(
            GunRecipeScenarios.tierGate(
                "gun.bio_gun.tiergate",
                "The Bio Gun row carries titanium, the EV gate material; an EV assembler finds it and an HV assembler does not.",
                BioGunDef.DEFINITION.assembly()));
    }

    private static GunRig rig(ScenarioContext ctx, int loaded) {
        return new GunRig(ctx, GunItems.get(BioGunDef.NAME), loaded);
    }

    private static Item tankItem() {
        return GameRegistry.findItem("gregguns", AmmoIds.BIO_TANK);
    }

    /** @return the Bio Tank's real ammo: BartWorks' E. coli culture fluid, or {@code null} if it cannot be resolved */
    private static Fluid cultureFluid() {
        return BartWorksCulture.eColiFluid();
    }

    /** @return a Bio Tank holding 1,000 mB of the E. coli culture fluid (Biomass when the culture cannot resolve) */
    private static ItemStack fullTank() {
        ItemStack tank = new ItemStack(tankItem());
        Fluid culture = cultureFluid();
        FluidStack fluid = culture != null ? new FluidStack(culture, 1000) : Materials.Biomass.getFluid(1000);
        ((IFluidContainerItem) tankItem()).fill(tank, fluid, true);
        return tank;
    }

    private static int emptyTanks(GunRig rig) {
        int n = 0;
        for (ItemStack s : rig.shooter.inventory.mainInventory) {
            if (s != null && s.getItem() == tankItem()) {
                FluidStack fluid = ((IFluidContainerItem) tankItem()).getFluid(s);
                if (fluid == null) {
                    n += s.stackSize;
                }
            }
        }
        return n;
    }

    /** Adds every glob in the world that is not yet in {@code seen}; returns the new ones. */
    private static List<BiogunProjectile> newGlobs(World world, List<BiogunProjectile> seen) {
        List<BiogunProjectile> fresh = new ArrayList<>();
        for (Object o : world.loadedEntityList) {
            if (o instanceof BiogunProjectile && !seen.contains(o)) {
                fresh.add((BiogunProjectile) o);
                seen.add((BiogunProjectile) o);
            }
        }
        return fresh;
    }

    /** Holding the left button fires level 1 globs that hurt and give Poison IV. */
    private static final class Fire implements Scenario {

        private static final int HELD_TICKS = 8;
        private static final int CHECK_TICK = 16;

        @Override
        public String name() {
            return "gun.bio_gun.fire";
        }

        @Override
        public String description() {
            return "Holding the left button on the Bio Gun fires every 6 ticks, one round and one level-1 BiogunProjectile each; a villager 4 blocks ahead takes damage and gets Poison IV for at most 100 ticks.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            // A villager, not a zombie (vanilla zombies are immune to poison) or a pig (too short for an eye-height
            // shot).
            EntityVillager target = new EntityVillager(ctx.world());
            target.setLocationAndAngles(ctx.originX() + 4.5, ctx.originY(), ctx.originZ() + 0.5, 90.0F, 0.0F);
            ctx.spawn(target);
            target.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                .setBaseValue(200.0);
            target.setHealth(200.0F);
            List<BiogunProjectile> seen = new ArrayList<>();
            // Globs left alive by earlier scenarios (other charge levels) must not count as this run's.
            newGlobs(ctx.world(), seen);
            int[] maxLevel = { 0 };
            int[] tick = { 0 };
            rig.press();
            return ScenarioResult.pending(name(), CHECK_TICK + 20, ticks -> {
                rig.tick();
                for (BiogunProjectile glob : newGlobs(ctx.world(), seen)) {
                    maxLevel[0] = Math.max(maxLevel[0], glob.level());
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
                out.checkEquals("globs launched", expected, rig.bullets());
                out.checkEquals("every left-button glob is level 1", 1, maxLevel[0]);
                out.check(target.getHealth() < target.getMaxHealth(), "the glob should have hurt the target");
                PotionEffect poison = target.getActivePotionEffect(Potion.poison);
                out.check(poison != null, "the target should be poisoned");
                if (poison != null) {
                    out.checkEquals("Poison IV is amplifier 3", 3, poison.getAmplifier());
                    out.check(
                        poison.getDuration() > 0 && poison.getDuration() <= 100,
                        "poison lasts at most 100 ticks");
                }
                return out.build();
            });
        }
    }

    /** Holding the right button 10, 20 and 35 ticks fires levels 1, 2 and 3 and spends that many rounds. */
    private static final class Charge implements Scenario {

        private static final int[] HOLDS = { 10, 20, 35 };
        private static final int WARM = 8;

        @Override
        public String name() {
            return "gun.bio_gun.charge";
        }

        @Override
        public String description() {
            return "Holding the right button on the Bio Gun for 10, 20 and 35 ticks and releasing fires a glob of level 1, 2 and 3 that spends exactly that many rounds from the 30 loaded (GUN-02).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            GunRig rig = rig(ctx, STATS.magazineSize());
            List<BiogunProjectile> seen = new ArrayList<>();
            // Globs left alive by earlier scenarios (other charge levels) must not count as this run's.
            newGlobs(ctx.world(), seen);
            int[] levels = new int[HOLDS.length];
            int[] spent = new int[HOLDS.length];
            // phase, ticks in the phase, whether the button is down
            int[] st = { 0, 0, 0 };
            return ScenarioResult.pending(name(), 3 * (HOLDS[2] + WARM) + 20, ticks -> {
                rig.tick();
                if (st[0] >= HOLDS.length) {
                    ScenarioResult.Builder out = ScenarioResult.builder(name());
                    for (int i = 0; i < HOLDS.length; i++) {
                        out.checkEquals("level after a " + HOLDS[i] + " tick hold", i + 1, levels[i]);
                        out.checkEquals("rounds spent after a " + HOLDS[i] + " tick hold", i + 1, spent[i]);
                    }
                    out.checkEquals("rounds left", STATS.magazineSize() - 6, rig.loaded());
                    return out.build();
                }
                st[1]++;
                if (st[2] == 0) {
                    if (st[1] >= WARM) {
                        ServerGunHandler.onCharge(rig.shooter, true);
                        st[2] = 1;
                        st[1] = 0;
                    }
                    return null;
                }
                if (st[1] >= HOLDS[st[0]]) {
                    int before = rig.loaded();
                    ServerGunHandler.onCharge(rig.shooter, false);
                    spent[st[0]] = before - rig.loaded();
                    for (BiogunProjectile glob : newGlobs(ctx.world(), seen)) {
                        levels[st[0]] = glob.level();
                    }
                    st[0]++;
                    st[1] = 0;
                    st[2] = 0;
                }
                return null;
            });
        }
    }

    /** One full Bio Tank fills an empty gun with 30 rounds after 45 ticks and an empty tank comes back. */
    private static final class Reload implements Scenario {

        @Override
        public String name() {
            return "gun.bio_gun.reload";
        }

        @Override
        public String description() {
            return "An empty Bio Gun reloads 45 ticks after the trigger: 30 rounds from one full Bio Tank (E. coli culture fluid), which is consumed, and an empty Bio Tank comes back.";
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
                out.check(cultureFluid() != null, "the tank should be filled with the E. coli culture fluid");
                out.check(started, "the dry trigger should have started a reload");
                out.checkEquals("rounds one tick before the end", 0, loadedAt[reloadTicks - 1]);
                out.checkEquals("rounds at the end of the reload", 30, loadedAt[reloadTicks]);
                out.checkEquals("full tanks left", 0, rig.count(AmmoIds.BIO_TANK));
                out.checkEquals("empty tanks returned", 1, emptyTanks(rig));
                return out.build();
            });
        }
    }

    /**
     * The vat recipe that makes the Bio Tank's culture fluid is in BartWorks' map, with the dish as its special item.
     */
    private static final class CultureRecipe implements Scenario {

        @Override
        public String name() {
            return "gun.bio_gun.culture_recipe";
        }

        @Override
        public String description() {
            return "bacterialVatRecipes holds the Biomass to E. coli culture fluid recipe: the E. coli petri dish as its special item, 1,000 mB Biomass and 100 mB distilled water in, 1,000 mB culture fluid out, EV, glass tier 4.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Fluid culture = cultureFluid();
            ItemStack dish = BartWorksCulture.eColiPetriDish();
            out.check(culture != null, "BartWorks' E. coli culture fluid should resolve");
            out.check(dish != null, "BartWorks' E. coli petri dish should resolve");
            if (culture == null || dish == null) {
                return out.build();
            }
            GTRecipe found = null;
            for (GTRecipe recipe : BartWorksRecipeMaps.bacterialVatRecipes.getAllRecipes()) {
                if (recipe.mFluidOutputs != null && recipe.mFluidOutputs.length > 0
                    && recipe.mFluidOutputs[0] != null
                    && recipe.mFluidOutputs[0].getFluid() == culture) {
                    found = recipe;
                }
            }
            out.check(found != null, "bacterialVatRecipes should hold a recipe whose output is the culture fluid");
            if (found == null) {
                return out.build();
            }
            out.check(
                found.mSpecialItems instanceof ItemStack
                    && ItemStack.areItemStacksEqual(dish, (ItemStack) found.mSpecialItems),
                "the E. coli petri dish should be the recipe's special item");
            out.checkEquals("output mB", BioCultureRecipes.AMOUNT, found.mFluidOutputs[0].amount);
            out.checkEquals("EU/t", (int) TierEU.RECIPE_EV, found.mEUt);
            out.checkEquals("duration", BioCultureRecipes.SECONDS_TAKEN * GTRecipeBuilder.SECONDS, found.mDuration);
            out.checkEquals(
                "glass tier",
                BioCultureRecipes.GLASS_TIER,
                found.getMetadataOrDefault(GTRecipeConstants.GLASS, 0));
            boolean biomass = false;
            boolean water = false;
            for (FluidStack in : found.mFluidInputs) {
                if (in == null) {
                    continue;
                }
                biomass |= in.isFluidEqual(Materials.Biomass.getFluid(1)) && in.amount == BioCultureRecipes.AMOUNT;
                water |= in.isFluidEqual(GTModHandler.getDistilledWater(1)) && in.amount == BioCultureRecipes.WATER;
            }
            out.check(biomass, "the recipe should take 1,000 mB Biomass");
            out.check(water, "the recipe should take 100 mB distilled water");
            return out.build();
        }
    }

    /**
     * The vat picks our recipe, not BartWorks' "Biomass 100 mB to Fermented Biomass" row, when the E. coli dish is in
     * the controller. The map is special-slot sensitive, so a recipe whose special item differs from the controller's
     * is never a match, even when its inputs are a subset of ours.
     */
    // Source (the map is built with useSpecialSlot and specialSlotSensitive):
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/API/recipe/BartWorksRecipeMaps.java#L46-L54
    // Source (the filter that drops a recipe whose special item is not the slot's):
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMapBackend.java#L562-L570
    // Source (the Fermenter's rows are copied into the vat with another culture's dish as the special item):
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L716-L725
    // Source (the query API: items, fluids, specialSlot, find):
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/FindRecipeQuery.java#L116-L139
    // Source (the vat controller feeds its slot item to the same query):
    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/tileentities/multis/MTEBioVat.java#L328-L332
    private static final class VatRecipeSelected implements Scenario {

        @Override
        public String name() {
            return "gun.bio_gun.vat_recipe_selected";
        }

        @Override
        public String description() {
            return "findRecipeQuery on bacterialVatRecipes with circuit 2, 1,000 mB Biomass, 100 mB distilled water and the E. coli dish as the special item returns the GregGuns culture recipe; with no dish it does not.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Fluid culture = cultureFluid();
            ItemStack dish = BartWorksCulture.eColiPetriDish();
            out.check(culture != null, "BartWorks' E. coli culture fluid should resolve");
            out.check(dish != null, "BartWorks' E. coli petri dish should resolve");
            if (culture == null || dish == null) {
                return out.build();
            }
            ItemStack[] items = { GTUtility.getIntegratedCircuit(BioCultureRecipes.CIRCUIT) };
            FluidStack[] fluids = { Materials.Biomass.getFluid(BioCultureRecipes.AMOUNT),
                GTModHandler.getDistilledWater(BioCultureRecipes.WATER) };
            GTRecipe withDish = BartWorksRecipeMaps.bacterialVatRecipes.findRecipeQuery()
                .items(items)
                .fluids(fluids)
                .specialSlot(dish)
                .find();
            out.check(withDish != null, "the vat should find a recipe for the dish, Biomass and water");
            out.check(
                withDish != null && withDish.mFluidOutputs != null
                    && withDish.mFluidOutputs.length > 0
                    && withDish.mFluidOutputs[0] != null
                    && withDish.mFluidOutputs[0].getFluid() == culture,
                "the recipe found with the E. coli dish should make the culture fluid, not Fermented Biomass");
            GTRecipe noDish = BartWorksRecipeMaps.bacterialVatRecipes.findRecipeQuery()
                .items(items)
                .fluids(fluids)
                .specialSlot(null)
                .find();
            out.check(
                noDish == null || noDish.mFluidOutputs == null
                    || noDish.mFluidOutputs.length == 0
                    || noDish.mFluidOutputs[0] == null
                    || noDish.mFluidOutputs[0].getFluid() != culture,
                "without the dish the vat should not make the culture fluid");
            return out.build();
        }
    }

    /** With bioGun.acceptBiomass off a Bio Tank takes the culture fluid but not Biomass. */
    private static final class TankRejectsBiomass implements Scenario {

        @Override
        public String name() {
            return "gun.bio_gun.tank_rejects_biomass";
        }

        @Override
        public String description() {
            return "With bioGun.acceptBiomass false an empty Bio Tank takes 1,000 mB of the E. coli culture fluid and 0 mB of Biomass; with it true the tank takes Biomass too.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Fluid culture = cultureFluid();
            out.check(culture != null, "BartWorks' E. coli culture fluid should resolve");
            if (culture == null) {
                return out.build();
            }
            IFluidContainerItem tank = (IFluidContainerItem) tankItem();
            boolean saved = BioGunConfig.acceptBiomass;
            try {
                BioGunConfig.acceptBiomass = false;
                out.checkEquals(
                    "culture fluid accepted",
                    1000,
                    tank.fill(new ItemStack(tankItem()), new FluidStack(culture, 1000), false));
                out.checkEquals(
                    "Biomass accepted with the switch off",
                    0,
                    tank.fill(new ItemStack(tankItem()), Materials.Biomass.getFluid(1000), false));
                BioGunConfig.acceptBiomass = true;
                out.checkEquals(
                    "Biomass accepted with the switch on",
                    1000,
                    tank.fill(new ItemStack(tankItem()), Materials.Biomass.getFluid(1000), false));
            } finally {
                BioGunConfig.acceptBiomass = saved;
            }
            return out.build();
        }
    }

    /** Cancels every block place event while {@link #vetoPlacement} is set: a stand-in for a claim mod. */
    public static final class Veto {

        /**
         * @param event the place event
         */
        @SubscribeEvent
        public void onPlace(BlockEvent.PlaceEvent event) {
            if (vetoPlacement) {
                event.setCanceled(true);
            }
        }
    }

    /** A level 2 glob at a wall: the blob is placed, or not, by safe mode and by a protection veto. */
    private static final class Blob implements Scenario {

        private static final int WALL_DX = 6;
        private static final int HOLD = 20;
        private static final int WARM = 8;

        private final String name;
        private final String description;
        private final boolean blockDamage;
        private final boolean veto;
        private final int expectedSize;

        Blob(String name, String description, boolean blockDamage, boolean veto, int expectedSize) {
            this.name = name;
            this.description = description;
            this.blockDamage = blockDamage;
            this.veto = veto;
            this.expectedSize = expectedSize;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return description;
        }

        private static int blobSize(ScenarioContext ctx) {
            for (int dy = 0; dy <= 2; dy++) {
                int x = ctx.originX() + WALL_DX - 1;
                int y = ctx.originY() + dy;
                int z = ctx.originZ();
                if (ctx.world()
                    .getBlock(x, y, z) == WorldBlockRegistration.bioBlob()) {
                    TileEntity tile = ctx.world()
                        .getTileEntity(x, y, z);
                    return tile instanceof TileBioBlob ? ((TileBioBlob) tile).size() : -1;
                }
            }
            return 0;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (!vetoRegistered) {
                MinecraftForge.EVENT_BUS.register(new Veto());
                vetoRegistered = true;
            }
            GunRig rig = rig(ctx, STATS.magazineSize());
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ctx.setBlock(WALL_DX, dy, dz, Blocks.stone, 0);
                }
            }
            boolean savedBlockDamage = ExplosionsConfig.blockDamage;
            ExplosionsConfig.blockDamage = blockDamage;
            vetoPlacement = veto;
            int[] tick = { 0 };
            return ScenarioResult.pending(name(), WARM + HOLD + 40, ticks -> {
                rig.tick();
                tick[0]++;
                if (tick[0] == WARM) {
                    ServerGunHandler.onCharge(rig.shooter, true);
                }
                if (tick[0] == WARM + HOLD) {
                    ServerGunHandler.onCharge(rig.shooter, false);
                }
                boolean fired = tick[0] > WARM + HOLD;
                if (!fired || !(rig.bulletsGone() && rig.bullets() > 0 || tick[0] >= WARM + HOLD + 30)) {
                    return null;
                }
                int size = blobSize(ctx);
                ExplosionsConfig.blockDamage = savedBlockDamage;
                vetoPlacement = false;
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                out.check(WorldBlockRegistration.bioBlob() != null, "the utility module (Bio Blob block) must be on");
                out.checkEquals("level 2 spent two rounds", STATS.magazineSize() - 2, rig.loaded());
                out.checkEquals("blob size at the wall (0 = none)", expectedSize, size);
                return out.build();
            });
        }
    }
}
