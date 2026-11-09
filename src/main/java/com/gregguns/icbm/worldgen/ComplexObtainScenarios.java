package com.gregguns.icbm.worldgen;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

import com.gregguns.core.config.LootConfig;
import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.loot.LootCategory;
import com.gregguns.worldgen.loot.LootEntry;
import com.gregguns.worldgen.loot.LootPools;
import com.gregguns.worldgen.loot.LootTierCheck;
import com.gregguns.worldgen.loot.ObtainAudit;
import com.gregguns.worldgen.loot.ObtainTier;
import com.gregguns.worldgen.loot.ObtainTierScenarios;

import gregtech.api.enums.Materials;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

/**
 * Scenarios of the loot tier cap in the silo complexes (D364): every room table line, every furniture pool, every GT
 * machine's stocked output and every armory frame, enumerated or drawn at every complex ceiling from EV to UMV with
 * {@code loot.maxTier} at EV and at UMV. Nothing a complex hands out may sit above the lower of the complex's loot
 * ceiling and the cap, and nothing may have no known obtain tier. Each restores {@code loot.maxTier} on every exit.
 * Dev environment only; server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md; D363, D364
public final class ComplexObtainScenarios {

    private static final int EV = 4;
    private static final int UMV = 12;
    private static final int MAX_REPORTS = 20;

    private ComplexObtainScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Silo());
        registry.register(new Machines());
        registry.register(new Furniture());
        registry.register(new Frames());
    }

    private static void reportAll(ScenarioResult.Builder out, List<String> problems) {
        for (int i = 0; i < problems.size() && i < MAX_REPORTS; i++) {
            out.check(false, problems.get(i));
        }
        if (problems.size() > MAX_REPORTS) {
            out.check(false, (problems.size() - MAX_REPORTS) + " more");
        }
    }

    /** The stack checks of one cap: counts what was drawn, keeps the first problems. */
    private static final class Tally {

        int stacks;
        final List<String> bad = new ArrayList<>();

        void check(String where, ItemStack s, int ceiling) {
            if (s == null) return;
            stacks++;
            String p = ObtainAudit.problem(s, ceiling);
            if (p != null) bad.add(where + ": " + p);
        }
    }

    private abstract static class Base implements Scenario {

        @Override
        public final ScenarioResult run(ScenarioContext ctx) {
            if (!ModuleState.isActive(ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), "the ICBM module is off");
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            int saved = LootConfig.maxTier;
            try {
                ObtainTier.clearCache();
                body(out);
            } finally {
                LootConfig.maxTier = saved;
                ObtainTier.clearCache();
            }
            return out.build();
        }

        abstract void body(ScenarioResult.Builder out);
    }

    private static final class Silo extends Base {

        @Override
        public String name() {
            return "loot.obtain.silo";
        }

        @Override
        public String description() {
            return "Every silo room table line and junk line is known and its chain at most its rung's voltage; 40"
                + " chests per table, ceiling and room state at the EV and UMV caps hold nothing above the lower of"
                + " the ceiling and the cap (D364).";
        }

        @Override
        void body(ScenarioResult.Builder out) {
            SiloChests.register();
            for (String key : SiloLootTables.KEYS) {
                for (int c = EV; c <= UMV; c++) {
                    LootCategory cat = SiloLootTables.category(key, c);
                    reportAll(
                        out,
                        LootTierCheck.obtainViolations(
                            cat.name(),
                            cat.entries(),
                            SiloLootTables::voltageOf,
                            ObtainTierScenarios::obtainOf));
                }
            }
            LootCategory junk = SiloLootTables.junk();
            reportAll(out, LootTierCheck.obtainViolations(junk.name(), junk.entries(), ObtainTierScenarios::obtainOf));
            for (int cap : ObtainTierScenarios.CAPS) {
                LootConfig.maxTier = cap;
                Tally t = new Tally();
                Random rand = new Random(0x5110L + cap);
                for (String key : SiloLootTables.KEYS) {
                    for (int c = EV; c <= UMV; c++) {
                        String category = SiloLootTables.categoryName(key, c);
                        for (RoomState state : RoomState.values()) {
                            for (int n = 0; n < 40; n++) {
                                InventoryBasic chest = new InventoryBasic("silo", false, 27);
                                SiloChests.fillRoom(chest, category, key, state, rand);
                                for (int i = 0; i < chest.getSizeInventory(); i++) {
                                    t.check(category + " " + state, chest.getStackInSlot(i), c);
                                }
                            }
                        }
                    }
                }
                reportAll(out, t.bad);
                out.check(t.stacks > 0, "no silo chest drew anything at cap " + cap);
                out.note("cap " + cap + ": " + t.stacks + " stacks, " + t.bad.size() + " above it");
            }
        }
    }

    private static final class Machines extends Base {

        @Override
        public String name() {
            return "loot.obtain.machines";
        }

        @Override
        public String description() {
            return "Every recipe a stocked GT machine may show in its output slots, over every recipe map and tier LV to"
                + " UMV at the EV and UMV caps, makes nothing above the lower of the machine tier and the cap; an MV"
                + " fluid solidifier never casts white dwarf matter (D364).";
        }

        @Override
        void body(ScenarioResult.Builder out) {
            for (int cap : ObtainTierScenarios.CAPS) {
                LootConfig.maxTier = cap;
                Tally t = new Tally();
                int recipes = 0;
                for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
                    for (int tier = 1; tier <= UMV; tier++) {
                        for (GTRecipe r : GtMachineStock.recipes(map, tier)) {
                            recipes++;
                            for (ItemStack o : r.mOutputs) {
                                if (o != null && o.getItem() != null) {
                                    t.check(map.unlocalizedName + " tier " + tier, o, tier);
                                }
                            }
                        }
                    }
                }
                reportAll(out, t.bad);
                out.check(recipes > 0, "no stockable recipe at cap " + cap);
                out.note("cap " + cap + ": " + recipes + " recipe picks, " + t.stacks + " outputs");
            }
            LootConfig.maxTier = EV;
            boolean whiteDwarf = false;
            for (GTRecipe r : GtMachineStock.recipes(RecipeMaps.fluidSolidifierRecipes, 2)) {
                for (ItemStack o : r.mOutputs) {
                    // Source: getAssociation answers the material GT unified the stack under:
                    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L504
                    ItemData d = o == null ? null : GTOreDictUnificator.getAssociation(o);
                    if (d != null && d.mMaterial != null && d.mMaterial.mMaterial == Materials.WhiteDwarfMatter) {
                        whiteDwarf = true;
                    }
                }
            }
            out.check(!whiteDwarf, "an MV fluid solidifier can hold white dwarf matter at EV");
            out.check(
                !GtMachineStock.recipes(RecipeMaps.maceratorRecipes, 1)
                    .isEmpty(),
                "an LV macerator has nothing to stock at EV");
        }
    }

    private static final class Furniture extends Base {

        @Override
        public String name() {
            return "loot.obtain.furniture";
        }

        @Override
        public String description() {
            return "Every furniture pool and arms pool line is known and its chain at most its declared tier; every line"
                + " a piece of furniture may stock at each ceiling, at the EV and UMV caps, is at most the lower of the"
                + " ceiling and the cap; a rack at EV still holds tools and arms (D364).";
        }

        @Override
        void body(ScenarioResult.Builder out) {
            List<Method> fixed = new ArrayList<>();
            for (Method m : SiloPools.class.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers())
                    && m.getParameterTypes().length == 0
                    && m.getReturnType() == LootPools.Pool.class) {
                    fixed.add(m);
                }
            }
            out.check(fixed.size() > 10, "only " + fixed.size() + " furniture pools found");
            for (Method m : fixed) {
                LootPools.Pool p = pool(out, m);
                if (p != null)
                    reportAll(out, LootTierCheck.obtainViolations(p.name(), p.lines(), ObtainTierScenarios::obtainOf));
            }
            for (int c = EV; c <= UMV; c++) {
                for (LootPools.Pool p : ceilingPools(c)) {
                    reportAll(
                        out,
                        LootTierCheck.obtainViolations(p.name() + " " + c, p.lines(), ObtainTierScenarios::obtainOf));
                }
            }
            for (int cap : ObtainTierScenarios.CAPS) {
                LootConfig.maxTier = cap;
                Tally t = new Tally();
                for (int c = EV; c <= UMV; c++) {
                    List<LootPools.Pool> pools = new ArrayList<>(ceilingPools(c));
                    for (Method m : fixed) {
                        LootPools.Pool p = pool(out, m);
                        if (p != null) pools.add(p);
                    }
                    for (LootPools.Pool p : pools) {
                        for (LootEntry e : FurnitureStocker.pool(p, c)) {
                            t.check(p.name() + " at " + c, LootCategories.stack(e.ref(), 1), c);
                        }
                    }
                    for (LootEntry e : FurnitureStocker.weapons(c)) {
                        t.check("weapons at " + c, LootCategories.stack(e.ref(), 1), c);
                    }
                }
                reportAll(out, t.bad);
                out.check(t.stacks > 0, "no furniture line at cap " + cap);
                out.note("cap " + cap + ": " + t.stacks + " lines");
            }
            LootConfig.maxTier = EV;
            out.check(
                !FurnitureStocker.pool(SiloPools.tools(), EV)
                    .isEmpty(),
                "a tool rack at EV is empty");
            out.check(
                FurnitureStocker.weapons(EV)
                    .size() > 1,
                "a weapon rack at EV holds only a knife");
        }

        private static List<LootPools.Pool> ceilingPools(int c) {
            List<LootPools.Pool> out = new ArrayList<>();
            out.add(SiloPools.missilebody(c, true));
            out.add(SiloArms.guns(c));
            out.add(SiloArms.ammo(c));
            out.add(SiloArms.sidearms(c));
            out.add(SiloArms.sidearmAmmo(c));
            out.add(SiloArms.grenades(c));
            out.add(SiloArms.parts(c));
            return out;
        }

        private static LootPools.Pool pool(ScenarioResult.Builder out, Method m) {
            try {
                return (LootPools.Pool) m.invoke(null);
            } catch (ReflectiveOperationException e) {
                out.check(false, "cannot read pool " + m.getName() + ": " + e);
                return null;
            }
        }
    }

    private static final class Frames extends Base {

        @Override
        public String name() {
            return "loot.obtain.frames";
        }

        @Override
        public String description() {
            return "Armory frames and display chests at every ceiling, at the EV and UMV caps: a framed gun and its"
                + " display ammo are at most the lower of the loot ceiling and the cap, or the frame stays empty; at"
                + " EV some frames still hold a gun (D364).";
        }

        @Override
        void body(ScenarioResult.Builder out) {
            for (int cap : ObtainTierScenarios.CAPS) {
                LootConfig.maxTier = cap;
                Tally t = new Tally();
                int framed = 0;
                int empty = 0;
                int ammoRefused = 0;
                for (int c = EV; c <= UMV; c++) {
                    int loot = ComplexContents.lootTable(c);
                    for (long seed = 1; seed <= 40; seed++) {
                        for (int x = 0; x < 48; x += 3) {
                            String ref = SiloFrames
                                .gunFor(loot, seed * 7919L, x, ArmoryDisplay.FRAME_Y, x / 2, ArmoryDisplay.IS_GUN);
                            ItemStack gun = ref == null ? null : LootCategories.stack(ref, 1);
                            if (gun == null || !ObtainTier.allowed(gun, loot)) {
                                empty++;
                                continue;
                            }
                            framed++;
                            t.check("frame at " + c, gun, loot);
                            for (ArmoryDisplay.Stock s : ArmoryDisplay.stockFor(ref)) {
                                ItemStack a = LootCategories.stack("gregguns:" + s.item(), 1);
                                if (a == null || !ObtainTier.allowed(a, loot)) {
                                    ammoRefused++;
                                } else {
                                    t.check("display at " + c, a, loot);
                                }
                            }
                        }
                    }
                }
                reportAll(out, t.bad);
                out.check(framed > 0, "every frame is empty at cap " + cap);
                out.note(
                    "cap " + cap
                        + ": "
                        + framed
                        + " framed, "
                        + empty
                        + " empty, "
                        + ammoRefused
                        + " display lines refused");
            }
        }
    }
}
