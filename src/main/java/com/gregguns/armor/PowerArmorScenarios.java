package com.gregguns.armor;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.armor.ArmorTierScenarios.TestPlayer;
import com.gregguns.armor.logic.ArmorBonus;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.PowerRules;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.recipe.ArmorTable;
import com.gregguns.recipe.ProductionTable;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.TierEU;
import gregtech.api.hazards.Hazard;
import gregtech.api.hazards.HazardProtection;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicBatteryBuffer;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;

/**
 * The {@code armor.power.*} in-game scenarios (M3 wave C): the IV Power Armor and Exo Suit with a fake player, the GT
 * battery buffer proof for exact-tier charging (design 0008), the deferred IV recipes, and the special-slot evidence
 * for the M3 exit gate. Dev environment only. Server side.
 */
// Spec: docs/techguns-spec/05-armor/armor-catalog.md sections 2, 5.9, 5.11 and 6;
// docs/gregguns-spec/05-armor/recipes.md
// (IV); hazard-protection.md; docs/design/0008-exact-tier-charging.md; docs/testing/README.md
final class PowerArmorScenarios {

    private static final float EPS = 0.01F;

    private PowerArmorScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new PowerArmor());
        registry.register(new ExoSuit());
        registry.register(new Charger());
        registry.register(new Recipes());
        registry.register(new SlotGate());
    }

    private static boolean near(double expected, double actual) {
        return Math.abs(expected - actual) < EPS;
    }

    private static ItemStack chestOf(TestPlayer p) {
        return p.worn(ArmorSlot.CHESTPLATE);
    }

    private static void fill(TestPlayer p, SetDef set) {
        ElectricGear.charge(
            chestOf(p),
            set.power()
                .capacityEu(),
            set.tier());
    }

    /** Checks that the server wrote neither step height nor a potion (CORE-14, CORE-15). */
    private static void checkNoInterference(ScenarioResult.Builder out, TestPlayer p, float stepBefore) {
        out.check(p.stepHeight == stepBefore, "CORE-14: the server must not write step height, got " + p.stepHeight);
        out.check(
            p.getActivePotionEffects()
                .isEmpty(),
            "CORE-15: armor must not add a potion effect");
    }

    // --- power armor --------------------------------------------------------------------------------------------

    /** T3 Power Armor: IV electric chestplate switches the bonuses and the HEAT, FROST and ELECTRICAL cover. */
    private static final class PowerArmor implements Scenario {

        @Override
        public String name() {
            return "armor.power.power_armor";
        }

        @Override
        public String description() {
            return "Power Armor: empty gives only protection (physical 24 at 25% penetration resistance, no cover); charged gives speed x1.2, knockback 1.0, +5 health, jump 0.24, mining +0.20, full HEAT, FROST and ELECTRICAL cover, never GAS or SPACE; walking, jump and fall cost units; discharged ends it; no step height or potion written.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = ArmorTierScenarios.player(ctx, "[GGPower]");
            ArmorEvents events = ArmorModule.events();
            try {
                SetDef set = ArmorCatalog.T3_POWER;
                float step = p.stepHeight;
                p.wearAll(set);
                events.update(p);
                out.check(near(1.0, ArmorTierScenarios.speedFactor(p)), "empty: no speed bonus");
                out.check(near(0.0, ArmorTierScenarios.knockback(p)), "empty: no knockback resistance");
                out.check(near(20.0, p.getMaxHealth()), "empty: no extra health, got " + p.getMaxHealth());
                out.checkEquals("a crafted chestplate starts empty", 0.0, ArmorTierScenarios.units(p));
                out.checkEquals("vanilla armor bar 6+7+6+5", 24, p.getTotalArmorValue());
                for (Hazard h : new Hazard[] { Hazard.HEAT, Hazard.FROST, Hazard.ELECTRICAL }) {
                    out.check(!HazardProtection.isWearingFullHazmatAgainst(p, h), "empty: no " + h + " cover");
                }
                ArmorTierScenarios.checkHit(out, p, DamageType.PROJECTILE, 24.0, 0.0, 0.25);
                ArmorTierScenarios.checkHit(out, p, DamageType.PROJECTILE, 24.0, 0.25, 0.25);
                ArmorTierScenarios.checkHit(out, p, DamageType.EXPLOSION, 22.0, 0.0, 0.25);
                ArmorTierScenarios.checkHit(out, p, DamageType.POISON, 15.0, 0.0, 0.25);
                ArmorTierScenarios.checkHit(out, p, DamageType.RADIATION, 10.0, 0.0, 0.25);

                fill(p, set);
                out.checkEquals("full charge in units", 3600.0, ArmorTierScenarios.units(p));
                events.update(p);
                out.check(near(1.2, ArmorTierScenarios.speedFactor(p)), "charged: speed x1.2");
                out.check(near(1.0, ArmorTierScenarios.knockback(p)), "charged: knockback resistance 1.0");
                out.check(near(25.0, p.getMaxHealth()), "charged: +5 health, got " + p.getMaxHealth());
                out.check(near(0.24, Armor.bonus(p, ArmorBonus.JUMP)), "charged: jump 0.03 x3 + 0.15");
                out.check(near(0.20, Armor.bonus(p, ArmorBonus.BREAK_SPEED)), "charged: mining +0.20");
                out.check(Armor.bonus(p, ArmorBonus.STEP_ASSIST) > 0.0, "charged: step assist from the boots");
                out.check(near(0.2, Armor.bonus(p, ArmorBonus.FALL_REDUCTION)), "charged: fall reduction 0.2");
                out.check(near(1.0, Armor.bonus(p, ArmorBonus.FREE_FALL_HEIGHT)), "charged: free fall height 1");
                for (Hazard h : new Hazard[] { Hazard.HEAT, Hazard.FROST, Hazard.ELECTRICAL }) {
                    out.check(HazardProtection.isWearingFullHazmatAgainst(p, h), "charged: full cover against " + h);
                }
                out.check(!HazardProtection.isWearingFullHazmatAgainst(p, Hazard.GAS), "no GAS cover");
                out.check(!HazardProtection.isWearingFullHazmatAgainst(p, Hazard.SPACE), "never SPACE");
                ArmorTierScenarios.checkHit(out, p, DamageType.PROJECTILE, 24.0, 0.0, 0.25);
                BreakSpeed dig = new BreakSpeed(p, Blocks.stone, 0, 4.0F, 0, 0, 0);
                MinecraftForge.EVENT_BUS.post(dig);
                out.check(near(4.0 * 1.2, dig.newSpeed), "charged: mining x1.2, got " + dig.newSpeed);
                checkNoInterference(out, p, step);

                // Costs: walking, jump and a long fall.
                double before = ArmorTierScenarios.units(p);
                ArmorPower.tick(p, true, 40L);
                out.check(ArmorTierScenarios.units(p) == before - 4, "walking: 4 pieces cost 4 units");
                before = ArmorTierScenarios.units(p);
                p.motionY = 0.0;
                MinecraftForge.EVENT_BUS.post(new LivingJumpEvent(p));
                out.check(near(0.24, p.motionY), "jump adds 0.24, got " + p.motionY);
                out.check(ArmorTierScenarios.units(p) == before - PowerRules.JUMP_COST, "a jump costs 5 units");
                before = ArmorTierScenarios.units(p);
                LivingFallEvent fall = new LivingFallEvent(p, 10.0F);
                MinecraftForge.EVENT_BUS.post(fall);
                out.check(ArmorTierScenarios.units(p) == before - PowerRules.FALL_COST, "a long fall costs 5 units");
                out.check(fall.distance < 10.0F, "the fall is shortened, got " + fall.distance);

                // The charge is EU: 200 per unit, so four walking pieces cost 800 EU per second.
                double eu = ElectricGear.charge(chestOf(p));
                ArmorPower.tick(p, true, 60L);
                out.check(
                    near(800.0, eu - ElectricGear.charge(chestOf(p))),
                    "walking costs 800 EU per second for the set");

                // Camos: three, on single sheets.
                ItemGregGunsArmor chest = (ItemGregGunsArmor) chestOf(p).getItem();
                out.checkEquals("camo count", 3, chest.camoCount(chestOf(p)));
                chest.setCamo(chestOf(p), 1);
                out.check(
                    chest.getArmorTexture(chestOf(p), p, 1, null)
                        .endsWith("textures/models/armor/t3_power_dark.png"),
                    "camo 1 is Outcast");
                chest.setCamo(chestOf(p), 0);

                // Discharge: every bonus and cover go; protection stays.
                ElectricGear.use(chestOf(p), ElectricGear.charge(chestOf(p)), p);
                events.update(p);
                out.check(near(1.0, ArmorTierScenarios.speedFactor(p)), "discharged: speed back to x1.0");
                out.check(near(20.0, p.getMaxHealth()), "discharged: health back to 20");
                out.check(
                    !HazardProtection.isWearingFullHazmatAgainst(p, Hazard.HEAT),
                    "discharged: HEAT cover is gone");
                out.check(!chest.isBroken(chestOf(p)), "an empty electric chestplate is not broken");
                ArmorTierScenarios.checkHit(out, p, DamageType.PROJECTILE, 24.0, 0.0, 0.25);
                ArmorTierScenarios.checkUnequipped(out, p, events);
            } finally {
                ArmorTierScenarios.finish(p);
            }
            return out.build();
        }
    }

    // --- exo suit -----------------------------------------------------------------------------------------------

    /** T3 Exo Suit: the fastest set; IV electric chestplate. */
    private static final class ExoSuit implements Scenario {

        @Override
        public String name() {
            return "armor.power.exo";
        }

        @Override
        public String description() {
            return "Exo Suit: empty gives only protection (physical 22, 15% penetration resistance); charged gives speed x1.8, knockback 0.80, jump 0.60, mining +0.30, step assist, full HEAT, FROST and ELECTRICAL cover; walking, jump and fall cost units; no extra health; discharged ends it.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = ArmorTierScenarios.player(ctx, "[GGExo]");
            ArmorEvents events = ArmorModule.events();
            try {
                SetDef set = ArmorCatalog.T3_EXO;
                float step = p.stepHeight;
                p.wearAll(set);
                events.update(p);
                out.check(near(1.0, ArmorTierScenarios.speedFactor(p)), "empty: no speed bonus");
                out.checkEquals("vanilla armor bar 6+7+6+4", 23, p.getTotalArmorValue());
                ArmorTierScenarios.checkHit(out, p, DamageType.PROJECTILE, 22.0, 0.0, 0.15);
                ArmorTierScenarios.checkHit(out, p, DamageType.ENERGY, 18.0, 0.0, 0.15);
                ArmorTierScenarios.checkHit(out, p, DamageType.POISON, 10.0, 0.0, 0.15);
                ArmorTierScenarios.checkHit(out, p, DamageType.RADIATION, 6.0, 0.0, 0.15);

                fill(p, set);
                out.checkEquals("full charge in units", 7200.0, ArmorTierScenarios.units(p));
                events.update(p);
                out.check(near(1.8, ArmorTierScenarios.speedFactor(p)), "charged: speed x1.8");
                out.check(near(0.80, ArmorTierScenarios.knockback(p)), "charged: knockback 0.80");
                out.check(near(20.0, p.getMaxHealth()), "no extra health, got " + p.getMaxHealth());
                out.check(near(0.60, Armor.bonus(p, ArmorBonus.JUMP)), "charged: jump 0.10 x3 + 0.30");
                out.check(near(0.30, Armor.bonus(p, ArmorBonus.BREAK_SPEED)), "charged: mining +0.30");
                out.check(Armor.bonus(p, ArmorBonus.STEP_ASSIST) > 0.0, "charged: step assist from the boots");
                out.check(near(0.5, Armor.bonus(p, ArmorBonus.FALL_REDUCTION)), "charged: fall reduction 0.5");
                out.check(near(5.0, Armor.bonus(p, ArmorBonus.FREE_FALL_HEIGHT)), "charged: free fall 5 blocks");
                for (Hazard h : new Hazard[] { Hazard.HEAT, Hazard.FROST, Hazard.ELECTRICAL }) {
                    out.check(HazardProtection.isWearingFullHazmatAgainst(p, h), "charged: full cover against " + h);
                }
                out.check(!HazardProtection.isWearingFullHazmatAgainst(p, Hazard.SPACE), "never SPACE");
                ArmorTierScenarios.checkHit(out, p, DamageType.PROJECTILE, 22.0, 0.0, 0.15);
                BreakSpeed dig = new BreakSpeed(p, Blocks.stone, 0, 4.0F, 0, 0, 0);
                MinecraftForge.EVENT_BUS.post(dig);
                out.check(near(4.0 * 1.3, dig.newSpeed), "charged: mining x1.3, got " + dig.newSpeed);
                checkNoInterference(out, p, step);

                double before = ArmorTierScenarios.units(p);
                ArmorPower.tick(p, true, 40L);
                out.check(ArmorTierScenarios.units(p) == before - 4, "walking: 4 pieces cost 4 units");
                before = ArmorTierScenarios.units(p);
                p.motionY = 0.0;
                MinecraftForge.EVENT_BUS.post(new LivingJumpEvent(p));
                out.check(near(0.60, p.motionY), "jump adds 0.60, got " + p.motionY);
                out.check(ArmorTierScenarios.units(p) == before - PowerRules.JUMP_COST, "a jump costs 5 units");
                before = ArmorTierScenarios.units(p);
                LivingFallEvent fall = new LivingFallEvent(p, 12.0F);
                MinecraftForge.EVENT_BUS.post(fall);
                out.check(ArmorTierScenarios.units(p) == before - PowerRules.FALL_COST, "a long fall costs 5 units");
                out.check(
                    fall.distance <= 12.0F * 0.5F - 5.0F + 0.01F,
                    "fall 12 is halved, then minus 5: " + fall.distance);

                ItemGregGunsArmor chest = (ItemGregGunsArmor) chestOf(p).getItem();
                out.checkEquals("camo count", 3, chest.camoCount(chestOf(p)));
                chest.setCamo(chestOf(p), 2);
                out.check(
                    chest.getArmorTexture(chestOf(p), p, 1, null)
                        .endsWith("t3_exo_green_1.png"),
                    "camo 2 is Dark Green");
                chest.setCamo(chestOf(p), 0);

                ElectricGear.use(chestOf(p), ElectricGear.charge(chestOf(p)), p);
                events.update(p);
                out.check(near(1.0, ArmorTierScenarios.speedFactor(p)), "discharged: speed back to x1.0");
                out.check(
                    !HazardProtection.isWearingFullHazmatAgainst(p, Hazard.ELECTRICAL),
                    "discharged: ELECTRICAL cover is gone");
                ArmorTierScenarios.checkHit(out, p, DamageType.PROJECTILE, 22.0, 0.0, 0.15);
                ArmorTierScenarios.checkUnequipped(out, p, events);
            } finally {
                ArmorTierScenarios.finish(p);
            }
            return out.build();
        }
    }

    // --- GT chargers (exit gate: powered items run on EU through GT chargers) -------------------------------------

    private static ItemList buffer(Tier tier) {
        switch (tier) {
            case HV:
                return ItemList.Battery_Buffer_1by1_HV;
            case EV:
                return ItemList.Battery_Buffer_1by1_EV;
            case IV:
                return ItemList.Battery_Buffer_1by1_IV;
            case LUV:
                return ItemList.Battery_Buffer_1by1_LuV;
            default:
                throw new IllegalArgumentException(tier.toString());
        }
    }

    /** One buffer under test: the tile, the chestplate it holds, and whether it should charge it. */
    private static final class Slot {

        final String label;
        final IGregTechTileEntity tile;
        final ItemStack stack;
        final boolean matching;
        final boolean accepted;

        Slot(String label, IGregTechTileEntity tile, ItemStack stack, boolean matching, boolean accepted) {
            this.label = label;
            this.tile = tile;
            this.stack = stack;
            this.matching = matching;
            this.accepted = accepted;
        }
    }

    private static IGregTechTileEntity place(ScenarioContext ctx, int dx, int dy, int dz, int id) {
        Block block = GregTechAPI.sBlockMachines;
        ctx.setBlock(dx, dy, dz, block, GregTechAPI.METATILEENTITIES[id].getTileEntityBaseType());
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
        if (tile instanceof IGregTechTileEntity) {
            IGregTechTileEntity base = (IGregTechTileEntity) tile;
            base.setInitialValuesAsNBT(null, (short) id);
            return base;
        }
        return null;
    }

    /**
     * Powered chestplates in GT battery buffers: the buffer of the item's exact tier accepts it and charges it; the
     * buffer one tier below and the one above refuse it, and even when the stack is forced into them it stays empty.
     */
    private static final class Charger implements Scenario {

        private static final int WAIT_TICKS = 60;

        @Override
        public String name() {
            return "armor.power.charger";
        }

        @Override
        public String description() {
            return "A HEV (EV), Power Armor (IV) and Exo Suit (IV) chestplate in the Battery Buffer of their own tier is accepted and charges with EU; the buffers one tier below and above refuse it and a forced-in copy stays empty (design 0008).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            SetDef[] sets = { ArmorCatalog.T3_HEV, ArmorCatalog.T3_POWER, ArmorCatalog.T3_EXO };
            java.util.List<Slot> slots = new java.util.ArrayList<>();
            int x = 0;
            for (SetDef set : sets) {
                Tier lower = Tier.values()[set.tier()
                    .ordinal() - 1];
                Tier higher = Tier.values()[set.tier()
                    .ordinal() + 1];
                Tier[] tiers = { set.tier(), lower, higher };
                for (Tier t : tiers) {
                    int id = buffer(t).get(1)
                        .getItemDamage();
                    IGregTechTileEntity tile = place(ctx, x, 1, 0, id);
                    x += 2;
                    if (tile == null || !(tile.getMetaTileEntity() instanceof MTEBasicBatteryBuffer)) {
                        return ScenarioResult.fail(name(), "the " + t + " battery buffer did not place");
                    }
                    boolean matching = t == set.tier();
                    ItemStack stack = new ItemStack(ArmorItems.get(set, ArmorSlot.CHESTPLATE));
                    MTEBasicBatteryBuffer mte = (MTEBasicBatteryBuffer) tile.getMetaTileEntity();
                    String label = set.id() + " in the " + t + " buffer";
                    out.checkEquals(
                        label + ": IC2 tier",
                        set.tier()
                            .voltageIndex(),
                        ((ic2.api.item.IElectricItem) stack.getItem()).getTier(stack));
                    boolean accepted = mte.allowPutStack(tile, 0, ForgeDirection.UP, stack);
                    out.check(
                        accepted == matching,
                        label + (matching ? " must be accepted" : " must be refused (exact tier, 0008)"));
                    // Force the stack in, as a hopper that ignored allowPutStack would, so the charge path is judged
                    // too.
                    tile.setInventorySlotContents(0, stack);
                    ((gregtech.api.metatileentity.BaseMetaTileEntity) tile).setStoredEU(tile.getEUCapacity());
                    slots.add(new Slot(label, tile, stack, matching, accepted));
                }
            }
            return ScenarioResult.pending(name(), WAIT_TICKS + 5, ticks -> {
                if (ticks < WAIT_TICKS) {
                    return null;
                }
                for (Slot s : slots) {
                    double charge = ElectricGear.charge(s.stack);
                    if (s.matching) {
                        out.check(charge > 0.0, s.label + " should have charged, has " + charge + " EU");
                        out.check(charge <= chargeCap(s), s.label + " charged beyond its capacity");
                    } else {
                        out.check(charge == 0.0, s.label + " must stay empty, has " + charge + " EU");
                    }
                }
                return out.build();
            });
        }

        private static double chargeCap(Slot s) {
            return ((ic2.api.item.IElectricItem) s.stack.getItem()).getMaxCharge(s.stack);
        }
    }

    // --- recipes ------------------------------------------------------------------------------------------------

    private static String missingInput(ProductionTable.Row row) {
        for (ProductionTable.Input input : row.inputs()) {
            if (input.isGregGuns() && GameRegistry.findItem(GregGuns.MODID, input.itemName()) == null) {
                return input.itemName();
            }
        }
        return null;
    }

    /**
     * The IV rows exist exactly when every GregGuns input is registered (the deferral pattern); when present they run
     * on the IV Assembler with the sheet's time.
     */
    private static final class Recipes implements Scenario {

        @Override
        public String name() {
            return "armor.power.recipes";
        }

        @Override
        public String description() {
            return "Power Armor and Exo Suit have Assembler recipes at RECIPE_IV once Power Plating and Mechanical Parts T3 are registered (Exo upgrades at any damage), and no recipe while an input is missing.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            int present = 0;
            for (ProductionTable.Row row : ArmorTable.IV_DEFERRED) {
                Item made = GameRegistry.findItem(GregGuns.MODID, row.output());
                out.check(made != null, row.output() + " must be registered");
                String missing = missingInput(row);
                GTRecipe found = null;
                for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                    ItemStack first = recipe.mOutputs == null || recipe.mOutputs.length == 0 ? null
                        : recipe.mOutputs[0];
                    if (made != null && first != null && first.getItem() == made) {
                        found = recipe;
                    }
                }
                if (missing != null) {
                    out.check(found == null, row.id() + ": no recipe while " + missing + " is not registered");
                    out.note(row.id() + " deferred: " + missing + " is not registered in this run");
                    continue;
                }
                present++;
                if (found == null) {
                    out.check(false, row.id() + ": no Assembler recipe makes " + row.output());
                    continue;
                }
                out.checkEquals(row.output() + " EU/t", (int) TierEU.RECIPE_IV, found.mEUt);
                out.checkEquals(row.output() + " ticks", row.seconds() * GTRecipeBuilder.SECONDS, found.mDuration);
                for (ProductionTable.Input in : row.inputs()) {
                    if (in.anyDamage()) {
                        Item base = GameRegistry.findItem(GregGuns.MODID, in.itemName());
                        boolean wildcard = false;
                        for (ItemStack s : found.mInputs) {
                            wildcard |= s != null && s.getItem() == base
                                && s.getItemDamage() == net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE;
                        }
                        out.check(wildcard, row.output() + ": " + in.itemName() + " must match at any damage");
                    }
                }
            }
            out.note(present + " of " + ArmorTable.IV_DEFERRED.size() + " IV rows are active in this run");
            return out.build();
        }
    }

    // --- special slots (exit gate evidence) -----------------------------------------------------------------------

    /**
     * Special-slot evidence beside armor.slots.*: each device's bauble type, and that the state a worn device keeps
     * (charge, pouch contents) is in the stack NBT, so Baubles' own save and sync carry it.
     */
    private static final class SlotGate implements Scenario {

        @Override
        public String name() {
            return "armor.power.slot_gate";
        }

        @Override
        public String description() {
            return "Special-slot gate: each device names one bauble slot type; a charged electric chestplate and a filled Ammo Pouch survive a stack NBT round trip (the Baubles save format) with charge and contents intact.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            TestPlayer p = ArmorTierScenarios.player(ctx, "[GGSlotGate]");
            try {
                ItemStack goggles = new ItemStack(com.gregguns.armor.slots.EquipmentItems.nightVisionGoggles());
                ItemStack pouch = new ItemStack(com.gregguns.armor.slots.EquipmentItems.ammoPouch());
                out.checkEquals(
                    "goggles slot type",
                    "head",
                    com.gregguns.armor.slots.EquipmentItems.nightVisionGoggles()
                        .getBaubleTypes(goggles)[0]);
                out.checkEquals(
                    "pouch slot type",
                    "quiver",
                    com.gregguns.armor.slots.EquipmentItems.ammoPouch()
                        .getBaubleTypes(pouch)[0]);

                // A charged chestplate: the charge is IC2's standard NBT and comes back from a save and load.
                p.wearAll(ArmorCatalog.T3_POWER);
                ItemStack chest = chestOf(p);
                fill(p, ArmorCatalog.T3_POWER);
                double charge = ElectricGear.charge(chest);
                NBTTagCompound saved = new NBTTagCompound();
                chest.writeToNBT(saved);
                ItemStack loaded = ItemStack.loadItemStackFromNBT(saved);
                out.check(loaded != null && loaded.getItem() == chest.getItem(), "the chestplate reloads");
                if (loaded != null) {
                    out.check(
                        near(charge, ElectricGear.charge(loaded)),
                        "charge survives a save: " + charge + " vs " + ElectricGear.charge(loaded));
                }

                // A filled pouch keeps its six slots in the stack NBT.
                Item mag = GameRegistry.findItem(GregGuns.MODID, "mag_9mm_pistol");
                if (mag != null) {
                    com.gregguns.armor.slots.PouchInventory view = new com.gregguns.armor.slots.PouchInventory(
                        pouch,
                        () -> {});
                    view.setInventorySlotContents(0, new ItemStack(mag, 2));
                    view.setInventorySlotContents(5, new ItemStack(mag, 1));
                    view.markDirty();
                    NBTTagCompound pouchSaved = new NBTTagCompound();
                    pouch.writeToNBT(pouchSaved);
                    ItemStack pouchLoaded = ItemStack.loadItemStackFromNBT(pouchSaved);
                    com.gregguns.armor.slots.PouchInventory again = new com.gregguns.armor.slots.PouchInventory(
                        pouchLoaded,
                        () -> {});
                    out.check(
                        again.getStackInSlot(0) != null && again.getStackInSlot(0).stackSize == 2,
                        "slot 0 keeps 2 magazines");
                    out.check(
                        again.getStackInSlot(5) != null && again.getStackInSlot(5).stackSize == 1,
                        "slot 5 keeps 1 magazine");
                } else {
                    out.note("mag_9mm_pistol not registered (guns module off): pouch contents not checked");
                }
            } finally {
                ArmorTierScenarios.finish(p);
            }
            return out.build();
        }
    }
}
