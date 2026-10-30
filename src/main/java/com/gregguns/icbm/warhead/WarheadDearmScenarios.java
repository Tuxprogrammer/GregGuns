package com.gregguns.icbm.warhead;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.core.module.ModuleId;
import com.gregguns.core.module.ModuleState;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.recipe.IcbmIngredients;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.IcbmParts;
import com.gregguns.icbm.recipe.Ing;
import com.gregguns.icbm.tools.ToolRegistration;
import com.mojang.authlib.GameProfile;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

/**
 * Scenarios of de-arming (D283): the crafting grid unloads up to 8 charges per craft with a Defuse Kit and hands them
 * back whole, a build and a de-arm round trip nets nothing, and GT's Cutting Machine empties a full warhead, takes a
 * payload out and scraps a bare casing, each NBT-exactly and at the build step's voltage. Dev environment only. Server
 * side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warheads-and-fill.md (De-arming)
public final class WarheadDearmScenarios {

    private WarheadDearmScenarios() {}

    /** @param registry where to add the scenarios */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Grid());
        registry.register(new RoundTrip());
        registry.register(new Machine());
    }

    private static ScenarioResult gate(String name) {
        if (InertWarheads.item(WarheadCasing.T1) == null || WarheadBlocks.of(WarheadCasing.T1) == null
            || ToolRegistration.defuser() == null) {
            return ScenarioResult.fail(name, "the ICBM content is not registered");
        }
        if (!ModuleState.isActive(ModuleId.ICBM)) {
            return ScenarioResult.skip(name, "the ICBM module is off, so it registers no recipes");
        }
        return null;
    }

    private static Item charge() {
        return IcbmParts.get(IcbmPart.PLASTIC_EXPLOSIVE);
    }

    private static ItemStack charges(int n) {
        return new ItemStack(charge(), n);
    }

    private static ItemStack live(WarheadCasing casing, String id, int fill) {
        return WarheadStacks.make(Item.getItemFromBlock(WarheadBlocks.of(casing)), casing, id, fill);
    }

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

    private static boolean isLive(ItemStack s, WarheadCasing casing, String id, int fill) {
        if (s == null || !(s.getItem() instanceof ItemBlockWarhead)
            || ((ItemBlockWarhead) s.getItem()).casing() != casing) {
            return false;
        }
        WarheadData d = WarheadStacks.read(casing, s);
        return d.explosive()
            .equals(id) && d.fill() == fill;
    }

    private static boolean isInert(ItemStack s, WarheadCasing casing, String payload) {
        if (!InertWarheads.is(s) || InertWarheads.casing(s) != casing) {
            return false;
        }
        String p = InertWarheads.payload(s);
        return payload == null ? p == null && !s.hasTagCompound() : payload.equals(p);
    }

    private static FakePlayer player(ScenarioContext ctx) {
        String name = "gregguns-dearm";
        return new FakePlayer(
            (WorldServer) ctx.world(),
            new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
    }

    private static int count(EntityPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.inventory.mainInventory) {
            if (s != null && s.getItem() == item) {
                n += s.stackSize;
            }
        }
        return n;
    }

    /**
     * Takes the result out of a crafting grid the way a player does: vanilla's crafting slot fires FML's crafted event,
     * takes one of each ingredient and puts the container items back.
     *
     * @return the result taken, or null when the grid makes nothing
     */
    private static ItemStack craft(EntityPlayer p, InventoryCrafting g) {
        ItemStack made = WarheadUnloadRecipe.result(g);
        if (made == null) {
            made = WarheadChargeRecipe.result(g);
        }
        if (made == null) {
            return null;
        }
        InventoryCraftResult out = new InventoryCraftResult();
        out.setInventorySlotContents(0, made.copy());
        SlotCrafting slot = new SlotCrafting(p, g, out, 0, 0, 0);
        slot.onPickupFromSlot(p, made);
        return made;
    }

    private static int findSlot(InventoryCrafting g, Class<?> type) {
        for (int i = 0; i < g.getSizeInventory(); i++) {
            ItemStack s = g.getStackInSlot(i);
            if (s != null && type.isInstance(s.getItem())) {
                return i;
            }
        }
        return -1;
    }

    /** The grid unload, and what it refuses. */
    private static final class Grid implements Scenario {

        @Override
        public String name() {
            return "icbm.warhead.dearm.grid";
        }

        @Override
        public String description() {
            return "A Defuse Kit unloads up to 8 charges per craft, hands them back whole and wears one use; the last"
                + " charge leaves the inert warhead with its payload; strangers and odd tags make no recipe.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult skip = gate(name());
            if (skip != null) {
                return skip;
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemStack kit = ToolRegistration.defuserStack();
            out.check(
                isInert(
                    WarheadUnloadRecipe.result(grid(live(WarheadCasing.T1, "shrapnel", 5), kit.copy())),
                    WarheadCasing.T1,
                    "shrapnel"),
                "Shrapnel T1, 5 charges, + kit: the inert Shrapnel T1 warhead");
            out.check(
                isInert(
                    WarheadUnloadRecipe.result(grid(live(WarheadCasing.T1, "condensed", 3), kit.copy())),
                    WarheadCasing.T1,
                    null),
                "Condensed T1, 3 charges, + kit: the bare inert T1 warhead (condensed has no payload)");
            out.check(
                isLive(
                    WarheadUnloadRecipe.result(grid(kit.copy(), null, live(WarheadCasing.T2, "thermobaric", 20))),
                    WarheadCasing.T2,
                    "thermobaric",
                    12),
                "Thermobaric T2, 20 charges, + kit (any slots): Thermobaric, 12");
            out.checkEquals(
                "charges one unload of a full T2 gives back",
                8,
                WarheadUnloadRecipe.chargesOut(live(WarheadCasing.T2, "thermobaric", 20)));
            out.check(
                WarheadUnloadRecipe.result(grid(live(WarheadCasing.T1, "shrapnel", 5))) == null,
                "no kit, no unload");
            out.check(
                WarheadUnloadRecipe.result(grid(InertWarheads.stack(WarheadCasing.T1, "shrapnel"), kit.copy()))
                    == null,
                "an inert warhead has no charges to unload");
            out.check(
                WarheadUnloadRecipe.result(grid(live(WarheadCasing.T1, "shrapnel", 5), kit.copy(), charges(1)))
                    == null
                    && WarheadChargeRecipe.result(grid(live(WarheadCasing.T1, "shrapnel", 4), kit.copy(), charges(1)))
                        == null,
                "a kit and a charge together make neither an unload nor a load");
            out.check(
                WarheadUnloadRecipe
                    .result(grid(live(WarheadCasing.T1, "shrapnel", 5), kit.copy(), new ItemStack(Blocks.tnt)))
                    == null,
                "anything else in the grid makes no recipe");
            out.check(
                WarheadUnloadRecipe.result(
                    grid(live(WarheadCasing.T1, "shrapnel", 5), live(WarheadCasing.T1, "shrapnel", 5), kit.copy()))
                    == null,
                "two warheads make no recipe");
            out.check(
                WarheadUnloadRecipe.result(grid(live(WarheadCasing.T1, "shrapnel", 5), kit.copy(), kit.copy()))
                    == null,
                "two kits make no recipe");
            ItemStack tagless = new ItemStack(Item.getItemFromBlock(WarheadBlocks.of(WarheadCasing.T1)));
            out.check(
                WarheadUnloadRecipe.result(grid(tagless, kit.copy())) == null,
                "a warhead with no tag gives nothing back (it would read as full)");
            ItemStack foreign = live(WarheadCasing.T1, "shrapnel", 5);
            foreign.getTagCompound()
                .setString(WarheadStacks.K_EXPLOSIVE, "thermobaric");
            out.check(
                WarheadUnloadRecipe.result(grid(foreign, kit.copy())) == null,
                "a T1 casing tagged with a T2 explosive gives nothing back");

            // Through vanilla's crafting slot: the charges reach the player, the kit stays one use down.
            FakePlayer p = player(ctx);
            InventoryCrafting g = grid(live(WarheadCasing.T1, "smoke", 5), kit.copy());
            ItemStack taken = craft(p, g);
            out.check(isInert(taken, WarheadCasing.T1, "smoke"), "the craft gives the inert Smoke T1 warhead");
            out.checkEquals("Plastic Explosive handed back", 5, count(p, charge()));
            int k = findSlot(g, com.gregguns.icbm.tools.ItemDefuser.class);
            out.check(k >= 0, "the kit stays in the grid");
            if (k >= 0) {
                out.checkEquals(
                    "kit uses spent",
                    WarheadDearming.DEFUSER_WEAR_PER_CRAFT,
                    g.getStackInSlot(k)
                        .getItemDamage());
            }
            out.checkEquals("the warhead left the grid", -1, findSlot(g, ItemBlockWarhead.class));

            // The 32nd use breaks the kit.
            ItemStack last = kit.copy();
            last.setItemDamage(last.getMaxDamage());
            InventoryCrafting g2 = grid(live(WarheadCasing.T1, "shrapnel", 2), last);
            craft(p, g2);
            out.checkEquals(
                "a kit on its last use breaks",
                -1,
                findSlot(g2, com.gregguns.icbm.tools.ItemDefuser.class));
            out.checkEquals("its last unload still hands back the charges", 7, count(p, charge()));
            return out.build();
        }
    }

    /** Build in the grid, de-arm in the grid: nothing made, nothing lost. */
    private static final class RoundTrip implements Scenario {

        @Override
        public String name() {
            return "icbm.warhead.dearm.round_trip";
        }

        @Override
        public String description() {
            return "Loading 3, 8, 8 and 1 charges into a Thermobaric T2 casing and unloading it again through the"
                + " crafting slot gives back exactly 20 charges and the inert Thermobaric casing.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult skip = gate(name());
            if (skip != null) {
                return skip;
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            FakePlayer p = player(ctx);
            ItemStack w = InertWarheads.stack(WarheadCasing.T2, "thermobaric");
            int loaded = 0;
            for (int n : new int[] { 3, 8, 8, 1 }) {
                ItemStack[] in = new ItemStack[1 + n];
                in[0] = w;
                for (int i = 1; i <= n; i++) {
                    in[i] = charges(1);
                }
                ItemStack next = craft(p, grid(in));
                out.check(next != null, "a load of " + n + " crafts");
                if (next == null) {
                    return out.build();
                }
                w = next;
                loaded += n;
            }
            out.check(isLive(w, WarheadCasing.T2, "thermobaric", 20), "the loads made Thermobaric, 20");
            out.checkEquals("no charge reached the player while loading", 0, count(p, charge()));
            int crafts = 0;
            ItemStack kit = ToolRegistration.defuserStack();
            while (w != null && !InertWarheads.is(w) && crafts < 10) {
                InventoryCrafting g = grid(w, kit);
                w = craft(p, g);
                int k = findSlot(g, com.gregguns.icbm.tools.ItemDefuser.class);
                kit = k < 0 ? null : g.getStackInSlot(k);
                crafts++;
            }
            out.check(isInert(w, WarheadCasing.T2, "thermobaric"), "unloading ends at the inert Thermobaric casing");
            out.checkEquals("grid unloads to empty 20 charges", WarheadDearming.craftsToEmpty(20), crafts);
            out.checkEquals("charges back equal charges loaded", loaded, count(p, charge()));
            out.checkEquals("kit uses spent", crafts, kit == null ? -1 : kit.getItemDamage());
            return out.build();
        }
    }

    /** GT's Cutting Machine, through GT's own recipe lookup with real stacks. */
    private static final class Machine implements Scenario {

        @Override
        public String name() {
            return "icbm.warhead.dearm.machine";
        }

        @Override
        public String description() {
            return "The Cutting Machine empties a full warhead, takes each payload out and scraps each bare casing, at"
                + " the build step's voltage and not below, giving back exactly the build's items; a partial or bare"
                + " stack never matches a fuller recipe.";
        }

        private static GTRecipe cut(int voltageIndex, ItemStack in) {
            return RecipeMaps.cutterRecipes.findRecipeQuery()
                .items(in)
                .fluids(Materials.Water.getFluid(1000))
                .voltage(GTValues.V[voltageIndex])
                .find();
        }

        private static List<ItemStack> outputs(GTRecipe r) {
            List<ItemStack> l = new ArrayList<>();
            if (r != null && r.mOutputs != null) {
                for (ItemStack s : r.mOutputs) {
                    if (s != null) {
                        l.add(s);
                    }
                }
            }
            return l;
        }

        /** True when the two lists hold the same stacks (item, damage, size and tag), in any order. */
        private static boolean sameStacks(List<ItemStack> a, List<ItemStack> b) {
            if (a.size() != b.size()) {
                return false;
            }
            List<ItemStack> left = new ArrayList<>(b);
            for (ItemStack s : a) {
                ItemStack hit = null;
                for (ItemStack t : left) {
                    if (ItemStack.areItemStacksEqual(s, t)) {
                        hit = t;
                        break;
                    }
                }
                if (hit == null) {
                    return false;
                }
                left.remove(hit);
            }
            return true;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult skip = gate(name());
            if (skip != null) {
                return skip;
            }
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (WarheadRecipeSpec spec : WarheadRecipeSpec.values()) {
                WarheadBlast blast = spec.blast();
                int v = spec.machineVoltageIndex();
                // Step 1: a full warhead gives the casing it was filled from and every charge.
                List<ItemStack> unload = new ArrayList<>();
                unload.add(WarheadRecipes.inertFor(blast));
                for (int size : WarheadDearming.chargeStacks(blast.fillCap())) {
                    unload.add(charges(size));
                }
                ItemStack full = WarheadRecipes.warhead(blast, blast.fillCap());
                out.check(sameStacks(outputs(cut(v, full)), unload), spec + ": a full warhead empties exactly");
                out.check(cut(v - 1, full) == null, spec + ": one voltage tier lower empties nothing");
                out.check(
                    cut(v, WarheadRecipes.warhead(blast, blast.fillCap() - 1)) == null,
                    spec + ": a warhead one charge short is the grid's job, not the cutter's");
                if (spec.chargesOnly()) {
                    continue;
                }
                // Step 2: the payload's items, as the payload recipe took them.
                List<ItemStack> payload = new ArrayList<>();
                payload.add(InertWarheads.stack(spec.casing(), null));
                for (Ing in : WarheadDearming.payloadReturns(spec)) {
                    ItemStack s = IcbmIngredients.stack(in);
                    if (s != null) {
                        payload.add(s);
                    }
                }
                out.check(
                    sameStacks(outputs(cut(v, WarheadRecipes.inertFor(blast))), payload),
                    spec + ": the payload comes out as the payload recipe's items");
            }
            for (InertWarheadSpec spec : InertWarheadSpec.values()) {
                List<ItemStack> parts = new ArrayList<>();
                for (Ing in : WarheadDearming.casingReturns(spec)) {
                    parts.add(IcbmIngredients.stack(in));
                }
                ItemStack bare = InertWarheads.stack(spec.casing(), null);
                out.check(
                    sameStacks(outputs(cut(spec.voltageIndex(), bare)), parts),
                    spec + ": a bare casing scraps into its plates and fuze");
                out.check(cut(spec.voltageIndex() - 1, bare) == null, spec + ": one voltage tier lower scraps nothing");
            }
            // A bare casing must not match a payload's removal and hand out a payload it never had.
            List<ItemStack> bareOut = outputs(cut(3, InertWarheads.stack(WarheadCasing.T1, null)));
            Item sleeve = IcbmParts.get(IcbmPart.FRAGMENTATION_SLEEVE_STEEL);
            boolean sleeveOut = false;
            for (ItemStack s : bareOut) {
                sleeveOut |= s.getItem() == sleeve;
            }
            out.check(!sleeveOut, "a bare T1 casing gives no Shrapnel sleeve");
            // The GT recycler path: the casing metal and the fuze's metal.
            ItemData data = GTOreDictUnificator.getItemData(InertWarheads.stack(WarheadCasing.T1, null));
            out.check(
                data != null && data.mMaterial != null
                    && data.mMaterial.mMaterial == Materials.StainlessSteel
                    && data.mByProducts.length > 0,
                "an inert T1 warhead recycles into stainless steel plus its fuze's metal: " + data);
            ItemData fitted = GTOreDictUnificator.getItemData(InertWarheads.stack(WarheadCasing.T1, "shrapnel"));
            out.check(fitted != null, "a fitted inert warhead also has recycling data (its payload is lost)");
            return out.build();
        }
    }
}
