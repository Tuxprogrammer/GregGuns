package com.gregguns.icbm.warhead;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gregguns.core.config.ExplosionsConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.explosion.nuclear.Fallout;
import com.gregguns.explosion.nuclear.FalloutBlocks;
import com.gregguns.explosion.nuclear.FalloutEngine;
import com.gregguns.explosion.nuclear.FalloutMath;
import com.gregguns.explosion.nuclear.FalloutRecipes;
import com.gregguns.explosion.nuclear.FalloutYield;
import com.gregguns.explosion.nuclear.ItemFalloutProduct;
import com.gregguns.explosion.nuclear.NuclearScale;

import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/**
 * Scenarios of the fallout byproduct chain (D21, I5): the GT recipes by recipe-map lookup (tier gates, outputs, no
 * recycling data) and a ZPM-grade fallout over mixed ground (hot earth, trinitite and goo in their zones and caps, goo
 * holding back decay until it is cleaned up). Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/nuclear-and-fallout.md (The byproduct chain)
final class FalloutChainScenarios {

    private FalloutChainScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Products());
        registry.register(new GooAndTrinitite());
    }

    private static boolean off() {
        return FalloutBlocks.goo() == null || WarheadBlocks.of(WarheadCasing.T1) == null;
    }

    /** Recipe-map lookups: each find has its machine, its tier gate and its outputs, and none can be run backwards. */
    private static final class Products implements Scenario {

        @Override
        public String name() {
            return "icbm.nuclear.fallout_products";
        }

        @Override
        public String description() {
            return "Rubble, hot rubble, crushed trinitite and dissolved goo each have a Centrifuge recipe at their tier;"
                + " shards have a Macerator recipe; goo has a Chemical Reactor recipe at LuV with nitric acid; the"
                + " outputs name the D21 elements; none of the items carries GT item data. These are recipe-map lookups: exit box 3's \"process in GT machines\" means the recipes exist at their tier, not that a machine was run.";
        }

        private static GTRecipe centrifuge(ItemStack in, long voltage) {
            return RecipeMaps.centrifugeRecipes.findRecipeQuery()
                .items(in)
                .voltage(voltage)
                .find();
        }

        private void outputs(ScenarioResult.Builder out, String label, ItemStack in, long voltage, long tooLow,
            FalloutYield.Out[] expect) {
            GTRecipe r = centrifuge(in, voltage);
            out.check(r != null, label + " has a Centrifuge recipe");
            if (r == null) {
                return;
            }
            out.check(centrifuge(in, tooLow) == null, label + " is not centrifuged one tier below");
            for (FalloutYield.Out o : expect) {
                ItemStack want = FalloutRecipes.dust(o);
                if (want == null) {
                    continue; // a material the pack lacks is left out of the recipe too
                }
                boolean found = false;
                for (ItemStack got : r.mOutputs) {
                    found |= got != null && GTUtility.areStacksEqual(got, want);
                }
                out.check(found, label + " yields " + o.element() + " (" + o.size() + ")");
            }
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (off()) {
                return ScenarioResult.fail(name(), "the ICBM content is not registered");
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemStack rubble = new ItemStack(FalloutBlocks.rubble(), 1, 0);
            ItemStack hot = new ItemStack(FalloutBlocks.rubble(), 1, 1);
            ItemStack shard = new ItemStack(FalloutBlocks.shard(), 1, ItemFalloutProduct.SHARD);
            ItemStack crushed = new ItemStack(FalloutBlocks.shard(), 1, ItemFalloutProduct.CRUSHED);
            ItemStack goo = new ItemStack(FalloutBlocks.gooItem(), 1, ItemFalloutProduct.GOO);
            ItemStack hotGoo = new ItemStack(FalloutBlocks.gooItem(), 1, ItemFalloutProduct.HOT_GOO);
            ItemStack dissolved = new ItemStack(FalloutBlocks.gooItem(), 1, ItemFalloutProduct.DISSOLVED);
            ItemStack dissolvedHot = new ItemStack(FalloutBlocks.gooItem(), 1, ItemFalloutProduct.DISSOLVED_HOT);

            outputs(out, "rubble", rubble, TierEU.RECIPE_HV, TierEU.RECIPE_MV, FalloutYield.RUBBLE);
            outputs(out, "hot rubble", hot, TierEU.RECIPE_HV, TierEU.RECIPE_MV, FalloutYield.HOT_RUBBLE);
            outputs(
                out,
                "crushed trinitite",
                crushed,
                TierEU.RECIPE_HV,
                TierEU.RECIPE_MV,
                FalloutYield.CRUSHED_TRINITITE);
            outputs(out, "dissolved goo", dissolved, TierEU.RECIPE_EV, TierEU.RECIPE_HV, FalloutYield.DISSOLVED_GOO);
            outputs(
                out,
                "dissolved hot goo",
                dissolvedHot,
                TierEU.RECIPE_EV,
                TierEU.RECIPE_HV,
                FalloutYield.DISSOLVED_HOT_GOO);

            GTRecipe mac = RecipeMaps.maceratorRecipes.findRecipeQuery()
                .items(shard)
                .voltage(TierEU.RECIPE_HV)
                .find();
            out.check(mac != null && mac.mOutputs.length > 0, "a trinitite shard is macerated at HV");
            out.check(
                RecipeMaps.maceratorRecipes.findRecipeQuery()
                    .items(shard)
                    .voltage(TierEU.RECIPE_MV)
                    .find() == null,
                "a trinitite shard is not macerated one tier below (D69: HV floor)");
            out.check(
                mac != null && GTUtility.areStacksEqual(mac.mOutputs[0], crushed),
                "macerated trinitite is crushed trinitite");

            for (int hotGooFlag = 0; hotGooFlag < 2; hotGooFlag++) {
                ItemStack in = hotGooFlag == 0 ? goo : hotGoo;
                ItemStack outStack = hotGooFlag == 0 ? dissolved : dissolvedHot;
                net.minecraftforge.fluids.FluidStack acid = Materials.NitricAcid.getFluid(1000);
                GTRecipe bath = RecipeMaps.chemicalReactorRecipes.findRecipeQuery()
                    .items(in)
                    .fluids(acid)
                    .voltage(TierEU.RECIPE_LuV)
                    .find();
                out.check(bath != null, (hotGooFlag == 0 ? "goo" : "hot goo") + " has a LuV acid bath");
                out.check(
                    bath != null && bath.mOutputs.length > 0 && GTUtility.areStacksEqual(bath.mOutputs[0], outStack),
                    "the bath makes the dissolved form");
                out.check(
                    RecipeMaps.chemicalReactorRecipes.findRecipeQuery()
                        .items(in)
                        .fluids(acid)
                        .voltage(TierEU.RECIPE_IV)
                        .find() == null,
                    "the acid bath needs LuV");
            }
            // The I3 lesson: no GT item data on any of it, so no unification, macerator or arc route into fuel.
            ItemStack[] checked = { rubble, hot, shard, crushed, goo, hotGoo, dissolved, dissolvedHot,
                new ItemStack(FalloutBlocks.trinitite()), new ItemStack(FalloutBlocks.goo(), 1, 0),
                new ItemStack(FalloutBlocks.goo(), 1, 1), new ItemStack(FalloutBlocks.irradiated(), 1, 0),
                new ItemStack(FalloutBlocks.irradiated(), 1, FalloutBlocks.HOT_BIT) };
            for (ItemStack s : checked) {
                out.check(GTOreDictUnificator.getItemData(s) == null, s.getDisplayName() + " carries no GT item data");
                out.check(
                    RecipeMaps.arcFurnaceRecipes.findRecipeQuery()
                        .items(s)
                        .voltage(TierEU.RECIPE_UV)
                        .find() == null,
                    s.getDisplayName() + " has no Arc Furnace recipe");
            }
            out.check(
                RecipeMaps.maceratorRecipes.findRecipeQuery()
                    .items(rubble)
                    .voltage(TierEU.RECIPE_UV)
                    .find() == null,
                "rubble cannot be macerated");
            return out.build();
        }
    }

    /** A ZPM-grade fallout over sand and grass: zones, caps, the goo ledger, goo holding decay back, then release. */
    private static final class GooAndTrinitite implements Scenario {

        private static final int Y_BELOW = 8;

        @Override
        public String name() {
            return "icbm.nuclear.fallout_goo";
        }

        @Override
        public String description() {
            return "A ZPM-grade fallout of blast radius 20 leaves hot earth, trinitite and goo only in the inner"
                + " fireball, goo within its cap and ledger; while goo stands its chunk does not decay, and once it"
                + " is cleaned up everything reverts.";
        }

        @Override
        public int areaRadius() {
            return 40;
        }

        private static int[] tally(World w, int ox, int oy, int oz, int span, boolean byGooChunk) {
            // {irradiated, hot, trinitite, goo, hotGoo, irradiated in goo chunks, outside inner}
            int[] t = new int[7];
            java.util.Set<Long> gooChunks = new java.util.HashSet<>();
            if (byGooChunk) {
                for (int dx = -span; dx <= span; dx++) {
                    for (int dz = -span; dz <= span; dz++) {
                        for (int dy = 6; dy >= -Y_BELOW; dy--) {
                            if (w.getBlock(ox + dx, oy + dy, oz + dz) == FalloutBlocks.goo()) {
                                gooChunks.add((((long) ((ox + dx) >> 4)) << 32) | (((oz + dz) >> 4) & 0xFFFFFFFFL));
                            }
                        }
                    }
                }
            }
            for (int dx = -span; dx <= span; dx++) {
                for (int dz = -span; dz <= span; dz++) {
                    for (int dy = 6; dy >= -Y_BELOW; dy--) {
                        Block b = w.getBlock(ox + dx, oy + dy, oz + dz);
                        if (b == FalloutBlocks.irradiated()) {
                            t[0]++;
                            if (FalloutBlocks.isHot(w.getBlockMetadata(ox + dx, oy + dy, oz + dz))) {
                                t[1]++;
                            }
                            if (gooChunks
                                .contains((((long) ((ox + dx) >> 4)) << 32) | (((oz + dz) >> 4) & 0xFFFFFFFFL))) {
                                t[5]++;
                            }
                        } else if (b == FalloutBlocks.trinitite()) {
                            t[2]++;
                        } else if (b == FalloutBlocks.goo()) {
                            t[3]++;
                            if ((w.getBlockMetadata(ox + dx, oy + dy, oz + dz) & 1) == 1) {
                                t[4]++;
                            }
                        }
                    }
                }
            }
            return t;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (off()) {
                return ScenarioResult.fail(name(), "the ICBM content is not registered");
            }
            final World w = ctx.world();
            final int ox = ctx.originX();
            final int oy = ctx.originY();
            final int oz = ctx.originZ();
            final double blast = 20.0;
            final int disc = NuclearScale.falloutRadius(blast, ExplosionsConfig.falloutMaxRadius);
            final int span = disc + 4;
            final double inner = FalloutMath.INNER_SHARE * disc;
            ctx.clearArea(span, 12);
            for (int dx = -span; dx <= span; dx++) {
                for (int dz = -span; dz <= span; dz++) {
                    boolean sand = (dx + dz & 1) == 0;
                    ctx.setBlock(dx, -1, dz, sand ? Blocks.sand : Blocks.grass, 0);
                    ctx.setBlock(dx, -2, dz, sand ? Blocks.sand : Blocks.dirt, 0);
                    ctx.setBlock(dx, -3, dz, sand ? Blocks.sand : Blocks.dirt, 0);
                    ctx.setBlock(dx, -4, dz, Blocks.stone, 0);
                }
            }
            final boolean savedFallout = ExplosionsConfig.fallout;
            ExplosionsConfig.fallout = true;
            FalloutEngine.instance()
                .clearGoo(w); // an earlier scenario's goo is not this one's
            FalloutEngine.clear(w);
            if (!Fallout.start(w, ox + 0.5, oy, oz + 0.5, blast, Fallout.GRADE_ZPM)) {
                ExplosionsConfig.fallout = savedFallout;
                return ScenarioResult.fail(name(), "fallout did not start");
            }
            final int[] phase = { 0, 0 };
            final int[] held = { 0 };
            final ScenarioResult.Builder out = ScenarioResult.builder(name());
            return ScenarioResult.pending(name(), 20 * 60 * 5, ticks -> {
                FalloutEngine fe = FalloutEngine.instance();
                if (phase[0] == 0) {
                    if (fe.busy(w) || ticks < 120) {
                        return null;
                    }
                    int[] t = tally(w, ox, oy, oz, span, false);
                    out.check(t[0] > 0, "fallout left irradiated earth, found " + t[0]);
                    out.check(t[1] > 0, "the inner fireball of a ZPM-grade blast has hot earth, found " + t[1]);
                    out.check(t[2] > 0, "sand in the inner fireball became trinitite, found " + t[2]);
                    out.check(t[3] > 0, "a ZPM-grade blast leaves some goo");
                    out.check(
                        t[3] <= ExplosionsConfig.falloutMaxGoo,
                        "goo " + t[3] + " within the cap " + ExplosionsConfig.falloutMaxGoo);
                    out.check(t[4] == t[3], "goo from a ZPM-grade blast is all hot goo");
                    out.check(fe.gooBlocks(w) == t[3], "the goo ledger holds " + fe.gooBlocks(w) + ", goo is " + t[3]);
                    out.check(
                        t[0] + t[2] + t[3] <= ExplosionsConfig.falloutMaxBlocks,
                        "all fallout blocks within the per-blast cap");
                    int bad = 0;
                    int sandBad = 0;
                    int trinNear = 0;
                    for (int dx = -span; dx <= span; dx++) {
                        for (int dz = -span; dz <= span; dz++) {
                            double dist = Math.sqrt(dx * dx + dz * dz);
                            for (int dy = 6; dy >= -Y_BELOW; dy--) {
                                int x = ox + dx;
                                int y = oy + dy;
                                int z = oz + dz;
                                Block b = w.getBlock(x, y, z);
                                boolean hotEarth = b == FalloutBlocks.irradiated()
                                    && FalloutBlocks.isHot(w.getBlockMetadata(x, y, z));
                                if ((hotEarth || b == FalloutBlocks.trinitite() || b == FalloutBlocks.goo())
                                    && dist > inner + 1.0) {
                                    bad++;
                                }
                                // D155: trinitite thins out smoothly (chance 1 - (d/inner)^2), so only the inner half,
                                // where the chance is at least 75%, is expected to be mostly trinitite.
                                if (dy == -1 && dist <= inner / 2.0) {
                                    if (b == FalloutBlocks.trinitite()) {
                                        trinNear++;
                                    } else if (b == FalloutBlocks.irradiated()
                                        && FalloutBlocks.variantOf(w.getBlockMetadata(x, y, z)) == FalloutBlocks.SAND) {
                                            sandBad++;
                                        }
                                }
                            }
                        }
                    }
                    out.check(bad == 0, bad + " hot, trinitite or goo blocks lie outside the inner fireball");
                    out.check(
                        trinNear > sandBad,
                        "most top sand within half the inner fireball became trinitite (D155), trinitite " + trinNear
                            + ", plain sand "
                            + sandBad);
                    // A hot earth block mines into hot rubble.
                    for (int dx = -span; dx <= span && phase[1] == 0; dx++) {
                        for (int dz = -span; dz <= span && phase[1] == 0; dz++) {
                            int meta = w.getBlockMetadata(ox + dx, oy - 1, oz + dz);
                            if (w.getBlock(ox + dx, oy - 1, oz + dz) == FalloutBlocks.irradiated()
                                && FalloutBlocks.isHot(meta)) {
                                ItemStack drop = FalloutBlocks.irradiated()
                                    .getDrops(w, ox + dx, oy - 1, oz + dz, meta, 0)
                                    .get(0);
                                out.check(drop.getItemDamage() == 1, "hot earth mines into hot rubble");
                                phase[1] = 1;
                            }
                        }
                    }
                    held[0] = tally(w, ox, oy, oz, span, true)[5];
                    out.check(held[0] > 0, "irradiated earth stands in the goo's chunk");
                    FalloutEngine.expireAll(w);
                    phase[0] = 1;
                    phase[1] = ticks;
                    return null;
                }
                if (phase[0] == 1) {
                    // Decay runs for the chunks without goo; the goo chunk must keep its earth.
                    if (ticks - phase[1] < 700) {
                        return null;
                    }
                    int[] t = tally(w, ox, oy, oz, span, true);
                    out.check(t[5] == held[0], "goo holds its chunks' irradiated earth: " + t[5] + " of " + held[0]);
                    out.check(t[3] == fe.gooBlocks(w), "the goo is untouched by decay");
                    out.check(
                        t[0] - t[5] == 0,
                        (t[0] - t[5]) + " irradiated blocks outside the goo's chunk should have decayed");
                    int cleaned = NuclearScenarios.clearGooIn(w, ox, oy, oz, span);
                    out.check(cleaned == t[3] && fe.gooBlocks(w) == 0, "cleaning the goo empties the ledger");
                    FalloutEngine.expireAll(w);
                    phase[0] = 2;
                    phase[1] = ticks;
                    return null;
                }
                int[] t = tally(w, ox, oy, oz, span, false);
                if ((t[0] > 0 || t[2] > 0) && ticks - phase[1] < 900) {
                    FalloutEngine.expireAll(w);
                    return null;
                }
                ExplosionsConfig.fallout = savedFallout;
                out.check(t[0] == 0, "with the goo gone every irradiated block reverts, " + t[0] + " remain");
                out.check(t[2] == 0, "trinitite reverts to sand, " + t[2] + " remain");
                return out.build();
            });
        }
    }
}
