package com.gregguns.icbm.warhead;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.MissileScenarios;
import com.gregguns.icbm.missile.MissileStacks;
import com.gregguns.icbm.missile.MissileTier;
import com.gregguns.icbm.recipe.IcbmIngredients;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.IcbmParts;
import com.gregguns.icbm.recipe.Ing;

import gregtech.api.enums.GTValues;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

/**
 * Scenarios of the warhead chain (D282): the inert warhead crafts at its casing tier and not below, a payload fits in
 * the Assembler, the Assembler fill matches the payload exactly, the crafting grid loads 1 to 8 charges and refuses
 * what it must, an inert warhead can never be placed or taken by a silo, and a warhead made by the chain arms on a
 * missile like any other. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (The warhead chain)
public final class WarheadChainScenarios {

    private WarheadChainScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Recipes());
        registry.register(new Grid());
        registry.register(new Inert());
        registry.register(new Launch());
    }

    private static ScenarioResult needsModule(String name) {
        if (InertWarheads.item() == null || WarheadBlocks.of(WarheadCasing.T1) == null) {
            return ScenarioResult.fail(name, "the ICBM content is not registered");
        }
        return null;
    }

    private static ItemStack charges(int n) {
        return new ItemStack(IcbmParts.get(IcbmPart.PLASTIC_EXPLOSIVE), n);
    }

    private static ItemStack live(WarheadCasing casing, String id, int fill) {
        return WarheadStacks.make(Item.getItemFromBlock(WarheadBlocks.of(casing)), casing, id, fill);
    }

    /** A crafting grid nobody looks at, for {@link WarheadChargeRecipe}. */
    private static InventoryCrafting grid(ItemStack... stacks) {
        InventoryCrafting g = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        for (int i = 0; i < stacks.length; i++) {
            g.setInventorySlotContents(i, stacks[i]);
        }
        return g;
    }

    /** The GT Assembler, as a machine at one voltage tier would look up a recipe. */
    private static ItemStack assemble(int voltageIndex, ItemStack... items) {
        GTRecipe r = RecipeMaps.assemblerRecipes.findRecipeQuery()
            .items(items)
            .voltage(GTValues.V[voltageIndex])
            .find();
        return r == null || r.mOutputs == null || r.mOutputs.length == 0 ? null : r.mOutputs[0];
    }

    private static boolean sameWarhead(ItemStack s, WarheadCasing casing, String id, int fill) {
        if (s == null || !(s.getItem() instanceof ItemBlockWarhead)
            || ((ItemBlockWarhead) s.getItem()).casing() != casing) {
            return false;
        }
        WarheadData d = WarheadStacks.read(casing, s);
        return d.explosive()
            .equals(id) && d.fill() == fill;
    }

    /** The Assembler steps, through GT's own recipe lookup with real stacks. */
    private static final class Recipes implements Scenario {

        @Override
        public String name() {
            return "icbm.warhead.chain.recipes";
        }

        @Override
        public String description() {
            return "The inert T1 warhead crafts at HV and not at MV; a payload fits; the Assembler fill matches the"
                + " payload exactly.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult missing = needsModule(name());
            if (missing != null) {
                return missing;
            }
            if (!ModuleState.isActive(ModuleId.ICBM)) {
                return ScenarioResult.skip(name(), "the ICBM module is off, so it registers no recipes");
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (InertWarheadSpec spec : InertWarheadSpec.values()) {
                List<ItemStack> in = new ArrayList<>();
                for (Ing i : spec.inputs()) {
                    in.add(IcbmIngredients.stack(i));
                }
                in.add(GTUtility.getIntegratedCircuit(InertWarheadSpec.CIRCUIT));
                ItemStack[] items = in.toArray(new ItemStack[0]);
                ItemStack made = assemble(spec.voltageIndex(), items);
                out.check(
                    InertWarheads.is(made) && made.getItemDamage() == spec.casing()
                        .ordinal() && InertWarheads.payload(made) == null,
                    spec + ": plates and fuze make the bare inert warhead at its tier");
                ItemStack below = assemble(spec.voltageIndex() - 1, items);
                out.check(!InertWarheads.is(below), spec + ": one voltage tier lower makes no inert warhead");
            }
            ItemStack bareT1 = InertWarheads.stack(WarheadCasing.T1, null);
            ItemStack sleeve = new ItemStack(IcbmParts.get(IcbmPart.FRAGMENTATION_SLEEVE_STEEL), 1);
            ItemStack fitted = assemble(
                3,
                bareT1.copy(),
                sleeve,
                GTUtility.getIntegratedCircuit(WarheadRecipeSpec.SHRAPNEL.circuit()));
            out.check(
                InertWarheads.is(fitted) && "shrapnel".equals(InertWarheads.payload(fitted)),
                "a steel sleeve fits the Shrapnel payload to an inert T1 warhead: " + fitted);
            int shrapnelCircuit = WarheadRecipeSpec.SHRAPNEL.circuit();
            out.check(
                sameWarhead(
                    assemble(
                        3,
                        InertWarheads.stack(WarheadCasing.T1, "shrapnel"),
                        charges(5),
                        GTUtility.getIntegratedCircuit(shrapnelCircuit)),
                    WarheadCasing.T1,
                    "shrapnel",
                    5),
                "five charges fill a Shrapnel casing to a full Shrapnel warhead");
            out.check(
                !sameWarhead(
                    assemble(3, bareT1.copy(), charges(5), GTUtility.getIntegratedCircuit(shrapnelCircuit)),
                    WarheadCasing.T1,
                    "shrapnel",
                    5),
                "a bare casing on the Shrapnel circuit makes no Shrapnel warhead (the fill is NBT-sensitive)");
            out.check(
                sameWarhead(
                    assemble(
                        3,
                        bareT1.copy(),
                        charges(5),
                        GTUtility.getIntegratedCircuit(WarheadRecipeSpec.CONDENSED.circuit())),
                    WarheadCasing.T1,
                    "condensed",
                    5),
                "five charges alone fill a bare T1 casing to a full Condensed warhead");
            out.check(
                !sameWarhead(
                    assemble(
                        3,
                        bareT1.copy(),
                        charges(4),
                        GTUtility.getIntegratedCircuit(WarheadRecipeSpec.CONDENSED.circuit())),
                    WarheadCasing.T1,
                    "condensed",
                    5),
                "four charges are not a full T1 casing");
            ItemStack bigFill = assemble(
                9,
                InertWarheads.stack(WarheadCasing.T5, "redmatter"),
                charges(64),
                charges(64),
                GTUtility.getIntegratedCircuit(WarheadRecipeSpec.REDMATTER.circuit()));
            out.check(
                sameWarhead(bigFill, WarheadCasing.T5, "redmatter", 128),
                "128 charges in two stacks fill a red matter casing: " + bigFill);
            return out.build();
        }
    }

    /** The crafting-grid knob. */
    private static final class Grid implements Scenario {

        @Override
        public String name() {
            return "icbm.warhead.chain.grid";
        }

        @Override
        public String description() {
            return "The crafting grid loads 1 to 8 charges, tops up, and refuses a bare T2 casing, overflow and"
                + " strangers.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult missing = needsModule(name());
            if (missing != null) {
                return missing;
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemStack bareT1 = InertWarheads.stack(WarheadCasing.T1, null);
            ItemStack three = WarheadChargeRecipe.result(grid(bareT1, charges(1), charges(1), charges(1)));
            out.check(sameWarhead(three, WarheadCasing.T1, "condensed", 3), "bare T1 + 3 charges: Condensed, 3");
            ItemStack five = WarheadChargeRecipe.result(grid(three, charges(1), charges(1)));
            out.check(sameWarhead(five, WarheadCasing.T1, "condensed", 5), "top up 3 + 2: Condensed, 5");
            out.check(
                WarheadChargeRecipe.result(grid(five, charges(1))) == null,
                "a full warhead takes no more charges");
            out.check(
                WarheadChargeRecipe.result(grid(three.copy(), charges(1), charges(1), charges(1))) == null,
                "a load past the cap is refused whole");
            out.check(
                WarheadChargeRecipe.result(grid(InertWarheads.stack(WarheadCasing.T2, null), charges(1))) == null,
                "a bare T2 casing takes no charges without a payload");
            ItemStack[] eight = new ItemStack[9];
            eight[0] = InertWarheads.stack(WarheadCasing.T2, "thermobaric");
            for (int i = 1; i < 9; i++) {
                eight[i] = charges(1);
            }
            out.check(
                sameWarhead(WarheadChargeRecipe.result(grid(eight)), WarheadCasing.T2, "thermobaric", 8),
                "a Thermobaric T2 casing + 8 charges: Thermobaric, 8");
            out.check(
                WarheadChargeRecipe.result(grid(InertWarheads.stack(WarheadCasing.T1, "shrapnel"))) == null,
                "no charges, no recipe");
            out.check(
                WarheadChargeRecipe.result(
                    grid(InertWarheads.stack(WarheadCasing.T1, "shrapnel"), charges(1), new ItemStack(Blocks.tnt)))
                    == null,
                "anything else in the grid makes no recipe");
            out.check(
                WarheadChargeRecipe.result(
                    grid(
                        InertWarheads.stack(WarheadCasing.T1, null),
                        InertWarheads.stack(WarheadCasing.T1, null),
                        charges(1)))
                    == null,
                "two warheads make no recipe");
            out.check(
                sameWarhead(
                    WarheadChargeRecipe.result(grid(InertWarheads.stack(WarheadCasing.T1, "shrapnel"), charges(1))),
                    WarheadCasing.T1,
                    "shrapnel",
                    1),
                "a Shrapnel casing + 1 charge: Shrapnel, 1");
            return out.build();
        }
    }

    /** An inert warhead is a plain item: nothing places it, launches it or sets it off. */
    private static final class Inert implements Scenario {

        @Override
        public String name() {
            return "icbm.warhead.chain.inert";
        }

        @Override
        public String description() {
            return "The inert warhead is not a block: it cannot be placed, and a silo does not take it as a warhead.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult missing = needsModule(name());
            if (missing != null) {
                return missing;
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Item item = InertWarheads.item();
            out.check(!(item instanceof ItemBlock), "the inert warhead is not a block item");
            Block b = Block.getBlockFromItem(item);
            out.check(!(b instanceof BlockWarhead), "no warhead block stands behind the inert warhead");
            ctx.clearArea(2, 3);
            ctx.setBlock(0, -1, 0, Blocks.stone, 0);
            ItemStack stack = InertWarheads.stack(WarheadCasing.T1, "shrapnel");
            boolean used = item.onItemUse(
                stack,
                null,
                ctx.world(),
                ctx.originX(),
                ctx.originY() - 1,
                ctx.originZ(),
                1,
                0.5F,
                1.0F,
                0.5F);
            out.check(!used, "using it on the ground does nothing");
            out.check(
                ctx.world()
                    .isAirBlock(ctx.originX(), ctx.originY(), ctx.originZ()),
                "nothing was placed");
            out.check(
                InertWarheads.payload(InertWarheads.stack(WarheadCasing.T1, "thermobaric")) == null,
                "a payload of another tier is left off");
            out.check(
                InertWarheads.payload(InertWarheads.stack(WarheadCasing.T1, "condensed")) == null,
                "condensed is not a payload");
            return out.build();
        }
    }

    /** A warhead made by the chain arms on a missile body like any other. */
    private static final class Launch implements Scenario {

        @Override
        public String name() {
            return "icbm.warhead.chain.launch";
        }

        @Override
        public String description() {
            return "A Shrapnel T1 warhead loaded in the crafting grid sits on a T1 body and arms into a missile that"
                + " carries it.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult missing = needsModule(name());
            if (missing != null) {
                return missing;
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ctx.clearArea(4, 8);
            for (int a = -4; a <= 4; a++) {
                for (int c = -4; c <= 4; c++) {
                    ctx.setBlock(a, -1, c, Blocks.stone, 0);
                }
            }
            ItemStack made = WarheadChargeRecipe
                .result(grid(InertWarheads.stack(WarheadCasing.T1, "shrapnel"), charges(1), charges(1)));
            out.check(sameWarhead(made, WarheadCasing.T1, "shrapnel", 2), "the grid made Shrapnel, 2");
            if (made == null) {
                return out.build();
            }
            int[] base = MissileScenarios.buildStack(ctx, MissileTier.T1, null, null, 0, 0, 0, 0, false, 0);
            int top = MissileTier.T1.heightBlocks();
            ctx.setBlock(0, top, 0, WarheadBlocks.of(WarheadCasing.T1), 0);
            TileEntity t = ctx.world()
                .getTileEntity(base[0], base[1] + top, base[2]);
            out.check(t instanceof TileWarhead, "the warhead block stands on the nose");
            if (!(t instanceof TileWarhead)) {
                return out.build();
            }
            ((TileWarhead) t).init(WarheadCasing.T1, made.getTagCompound(), null);
            MissileStacks.Armed armed = MissileStacks.armIntoEntity(ctx.world(), base[0], base[1], base[2], null);
            out.checkEquals("the stack arms", MissileStacks.ArmResult.OK, armed.result);
            EntityMissile m = armed.missile;
            out.check(m != null && "shrapnel".equals(m.explosiveId()), "the missile carries the Shrapnel warhead");
            MissileScenarios.sweep(ctx);
            return out.build();
        }
    }
}
