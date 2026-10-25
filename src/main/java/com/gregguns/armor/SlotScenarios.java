package com.gregguns.armor;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.armor.oxygen.FallbackOxygenMask;
import com.gregguns.armor.oxygen.FallbackOxygenTanks;
import com.gregguns.armor.oxygen.gc.OxygenMaskGC;
import com.gregguns.armor.oxygen.gc.OxygenTanksGC;
import com.gregguns.armor.slots.AmmoPouch;
import com.gregguns.armor.slots.BaublesCompat;
import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.armor.slots.EquipmentItems;
import com.gregguns.armor.slots.ItemBackDevice;
import com.gregguns.armor.slots.ItemGasMask;
import com.gregguns.armor.slots.ItemNightVisionGoggles;
import com.gregguns.armor.slots.ItemTacticalMask;
import com.gregguns.armor.slots.PouchInventory;
import com.gregguns.armor.slots.SlotEquipment;
import com.gregguns.armor.slots.logic.BaubleSlot;
import com.gregguns.core.config.EquipmentConfig;
import com.gregguns.core.registry.Naming;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.item.gun.PlayerAmmoStore;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.hazards.Hazard;
import gregtech.api.hazards.HazardProtection;

/**
 * The {@code armor.slots.*} in-game scenarios: a fake player wears each special-slot item (Baubles Expanded slots and
 * the helmet slot), the effect is asserted, then the item is taken off and the effect must be gone. Each scenario
 * leaves the Baubles slots and armor slots of its own fake player empty. Dev environment only; server side.
 */
// Spec: docs/gregguns-spec/05-armor/baubles-slots.md; oxygen-gear.md; hazard-protection.md; docs/testing/README.md
final class SlotScenarios {

    private SlotScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Goggles());
        registry.register(new Pouch());
        registry.register(new Back());
        registry.register(new Masks());
        registry.register(new Recipes());
    }

    private static final class Fake extends FakePlayer {

        Fake(ScenarioContext ctx, String name) {
            super((WorldServer) ctx.world(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        }

        void wearHelmet(ItemStack stack) {
            inventory.armorInventory[3] = stack;
        }

        void clean() {
            inventory.armorInventory[3] = null;
            for (BaubleSlot s : BaubleSlot.values()) {
                BaublesCompat.clear(this, s);
            }
            for (int i = 0; i < inventory.mainInventory.length; i++) {
                inventory.mainInventory[i] = null;
            }
        }
    }

    private static ItemStack charged(Item item, Tier tier, double eu) {
        ItemStack stack = new ItemStack(item);
        ElectricGear.charge(stack, eu, tier);
        return stack;
    }

    /** Night Vision Goggles in the head bauble slot: drain, toggle, empty, removal. */
    private static final class Goggles implements Scenario {

        @Override
        public String name() {
            return "armor.slots.goggles";
        }

        @Override
        public String description() {
            return "Goggles in the head slot: night vision and 2 EU/t only while on and charged; off, empty or unequipped means none.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Fake p = new Fake(ctx, "[GGSlotGoggles]");
            SlotEquipment slots = new SlotEquipment();
            try {
                out.check(BaublesCompat.isLoaded(), "Baubles Expanded must be loaded in the dev run");
                ItemNightVisionGoggles item = EquipmentItems.nightVisionGoggles();
                out.checkEquals("head slot type", "head", item.getBaubleTypes(new ItemStack(item))[0]);
                out.check(
                    BaublesCompat.indexes(p, BaubleSlot.HEAD).length > 0,
                    "the pack config must assign a head slot");
                out.check(!slots.tick(p), "nothing worn: no night vision");

                ItemStack goggles = charged(item, Tier.MV, 1000);
                out.check(BaublesCompat.equip(p, BaubleSlot.HEAD, goggles), "goggles must fit the head slot");
                double before = ElectricGear.charge(goggles);
                out.check(slots.tick(p), "worn, on and charged: night vision");
                out.check(
                    Math.abs(before - ElectricGear.charge(goggles) - EquipmentConfig.nightVisionDrainEuPerTick) < 1e-6,
                    "one tick must cost " + EquipmentConfig.nightVisionDrainEuPerTick
                        + " EU, charge "
                        + before
                        + " -> "
                        + ElectricGear.charge(goggles));

                Boolean state = SlotEquipment.toggleNightVision(p);
                out.check(Boolean.FALSE.equals(state), "the key must switch the goggles off");
                before = ElectricGear.charge(goggles);
                out.check(!slots.tick(p), "switched off: no night vision");
                out.check(ElectricGear.charge(goggles) == before, "switched off: no drain");
                state = SlotEquipment.toggleNightVision(p);
                out.check(Boolean.TRUE.equals(state), "the key must switch the goggles back on");

                ElectricGear.use(goggles, ElectricGear.charge(goggles), p);
                out.check(!slots.tick(p), "empty: no night vision");
                ElectricGear.charge(goggles, 100, Tier.MV);
                out.check(slots.tick(p), "recharged: night vision again");

                BaublesCompat.clear(p, BaubleSlot.HEAD);
                out.check(!slots.tick(p), "unequipped: night vision gone");
            } finally {
                p.clean();
            }
            return out.build();
        }
    }

    /** Ammo Pouch in the quiver slot: the reload path finds ammo there first, and loses it on removal. */
    private static final class Pouch implements Scenario {

        @Override
        public String name() {
            return "armor.slots.pouch";
        }

        @Override
        public String description() {
            return "A worn Ammo Pouch's magazines count and are taken before the main inventory; unequipped, they are not seen.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Fake p = new Fake(ctx, "[GGSlotPouch]");
            try {
                Item mag = GameRegistry.findItem(Naming.NAMESPACE, "mag_9mm_pistol");
                out.check(mag != null, "mag_9mm_pistol must exist (guns module)");
                if (mag == null) {
                    return out.build();
                }
                ItemStack pouch = new ItemStack(EquipmentItems.ammoPouch());
                out.checkEquals(
                    "quiver slot type",
                    "quiver",
                    EquipmentItems.ammoPouch()
                        .getBaubleTypes(pouch)[0]);
                out.check(BaublesCompat.equip(p, BaubleSlot.QUIVER, pouch), "pouch must fit the quiver slot");
                int index = AmmoPouch.wornIndex(p);
                out.check(index >= 0, "the worn pouch must be found");

                PouchInventory view = AmmoPouch.wornAt(p, index);
                out.check(!PouchInventory.accepts(new ItemStack(net.minecraft.init.Items.apple)), "only ammo fits");
                view.setInventorySlotContents(0, new ItemStack(mag, 2));
                p.inventory.mainInventory[5] = new ItemStack(mag, 3);

                PlayerAmmoStore store = new PlayerAmmoStore(p);
                out.checkEquals("count pouch + inventory", 5, store.count("mag_9mm_pistol"));
                out.checkEquals("removed", 3, store.remove("mag_9mm_pistol", 3));
                out.checkEquals(
                    "pouch is emptied first (stored in the stack NBT)",
                    0,
                    countIn(AmmoPouch.wornAt(p, index), mag));
                out.checkEquals(
                    "main inventory keeps the rest",
                    2,
                    p.inventory.mainInventory[5] == null ? 0 : p.inventory.mainInventory[5].stackSize);

                AmmoPouch.wornAt(p, index)
                    .setInventorySlotContents(1, new ItemStack(mag, 4));
                out.checkEquals("count again", 6, store.count("mag_9mm_pistol"));
                BaublesCompat.clear(p, BaubleSlot.QUIVER);
                out.checkEquals("unequipped pouch is not seen", 2, store.count("mag_9mm_pistol"));
            } finally {
                p.clean();
            }
            return out.build();
        }

        private static int countIn(PouchInventory inv, Item item) {
            int n = 0;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s != null && s.getItem() == item) {
                    n += s.stackSize;
                }
            }
            return n;
        }
    }

    /** A test back device counts ticks; the fallback Oxygen Tanks top up air only with oxygen gear. */
    private static final class Back implements Scenario {

        @Override
        public String name() {
            return "armor.slots.back";
        }

        @Override
        public String description() {
            return "A wings device ticks while worn and is told once when removed; the fallback Oxygen Tanks add air only with an Oxygen Mask.";
        }

        /** A device for the hook test; not registered. */
        private static final class TestDevice extends ItemBackDevice {

            int ticks;
            int removed;
            int keys;

            @Override
            public Tier getTier() {
                return Tier.HV;
            }

            @Override
            public void onBackTick(EntityPlayer player, ItemStack stack) {
                ticks++;
            }

            @Override
            public void onBackRemoved(EntityPlayer player, Item item) {
                removed++;
            }

            @Override
            public void onBackKey(EntityPlayer player, ItemStack stack, boolean down) {
                keys++;
            }
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Fake p = new Fake(ctx, "[GGSlotBack]");
            SlotEquipment slots = new SlotEquipment();
            try {
                TestDevice device = new TestDevice();
                out.checkEquals("wings slot type", "wings", device.getBaubleTypes(new ItemStack(device))[0]);
                out.check(
                    BaublesCompat.indexes(p, BaubleSlot.WINGS).length > 0,
                    "the pack config must assign a wings slot");
                ItemStack stack = new ItemStack(device);
                out.check(BaublesCompat.equip(p, BaubleSlot.WINGS, stack), "device must fit the wings slot");
                slots.tick(p);
                slots.tick(p);
                out.checkEquals("ticks while worn", 2, device.ticks);
                SlotEquipment.backKey(p, true);
                out.checkEquals("key forwarded", 1, device.keys);
                BaublesCompat.clear(p, BaubleSlot.WINGS);
                slots.tick(p);
                out.checkEquals("no tick once removed", 2, device.ticks);
                out.checkEquals("told once on removal", 1, device.removed);
                slots.tick(p);
                out.checkEquals("not told twice", 1, device.removed);

                // Oxygen items: the fallback pair without Galacticraft, the Galacticraft pair with it (both branches
                // are
                // exercised: runs with and without Galacticraft, see docs/testing/README.md).
                if (EquipmentItems.oxygenTanks() instanceof FallbackOxygenTanks) {
                    ItemStack tanks = new ItemStack(EquipmentItems.oxygenTanks());
                    BaublesCompat.equip(p, BaubleSlot.WINGS, tanks);
                    p.setAir(100);
                    slots.tick(p);
                    out.checkEquals("no oxygen gear: no air", 100, p.getAir());
                    ItemStack mask = new ItemStack(EquipmentItems.oxygenMask());
                    out.check(
                        EquipmentItems.oxygenMask() instanceof FallbackOxygenMask,
                        "fallback mask expected without Galacticraft");
                    BaublesCompat.equip(p, BaubleSlot.HEAD, mask);
                    slots.tick(p);
                    out.checkEquals("mask + tanks: +20 air", 120, p.getAir());
                    out.checkEquals("one charge point used", 1, tanks.getItemDamage());
                    BaublesCompat.clear(p, BaubleSlot.WINGS);
                    slots.tick(p);
                    out.checkEquals("tanks off: no more air", 120, p.getAir());
                } else {
                    out.check(
                        EquipmentItems.oxygenTanks() instanceof OxygenTanksGC,
                        "Galacticraft Oxygen Tanks expected with Galacticraft");
                    out.check(
                        EquipmentItems.oxygenMask() instanceof OxygenMaskGC,
                        "Galacticraft Oxygen Mask expected with Galacticraft");
                    out.check(
                        !(EquipmentItems.oxygenTanks() instanceof FallbackOxygenTanks)
                            && !(EquipmentItems.oxygenMask() instanceof FallbackOxygenMask),
                        "no fallback oxygen items with Galacticraft");
                }
            } finally {
                slots.forget(p);
                p.clean();
            }
            return out.build();
        }
    }

    /** Gas Mask and Tactical Mask in the helmet slot: GT hazard answers and the Tactical Mask's night vision. */
    private static final class Masks implements Scenario {

        @Override
        public String name() {
            return "armor.slots.masks";
        }

        @Override
        public String description() {
            return "Gas Mask: full GAS hazmat while its filter lasts, never SPACE; Tactical Mask: GAS hazmat plus night vision on charge; off, spent or removed means none.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Fake p = new Fake(ctx, "[GGSlotMasks]");
            SlotEquipment slots = new SlotEquipment();
            try {
                out.check(!HazardProtection.isWearingFullGasHazmat(p), "bare: no gas protection");
                ItemGasMask gas = EquipmentItems.gasMask();
                ItemStack mask = new ItemStack(gas);
                p.wearHelmet(mask);
                out.check(HazardProtection.isWearingFullGasHazmat(p), "Gas Mask alone is full gas hazmat");
                out.check(!gas.protectsAgainst(mask, Hazard.SPACE), "never SPACE");
                out.check(!gas.protectsAgainstFully(p, mask, Hazard.ELECTRICAL), "no electrical protection");
                mask.setItemDamage(gas.getMaxDamage() - 1);
                out.check(!HazardProtection.isWearingFullGasHazmat(p), "a spent filter protects against nothing");
                mask.setItemDamage(0);
                out.check(HazardProtection.isWearingFullGasHazmat(p), "a renewed filter protects again");
                p.wearHelmet(null);
                out.check(!HazardProtection.isWearingFullGasHazmat(p), "mask off: no protection");

                ItemTacticalMask tactical = EquipmentItems.tacticalMask();
                ItemStack tm = charged(tactical, Tier.HV, 500);
                p.wearHelmet(tm);
                out.check(HazardProtection.isWearingFullGasHazmat(p), "Tactical Mask is full gas hazmat");
                out.check(slots.tick(p), "Tactical Mask powered: night vision");
                out.check(
                    Math.abs(500 - ElectricGear.charge(tm) - EquipmentConfig.nightVisionDrainEuPerTick) < 1e-6,
                    "night vision drains EU, charge " + ElectricGear.charge(tm));
                out.check(SlotEquipment.hasOxygenGear(p), "powered Tactical Mask is oxygen gear");
                ElectricGear.use(tm, ElectricGear.charge(tm), p);
                out.check(!slots.tick(p), "empty Tactical Mask: no night vision");
                out.check(
                    HazardProtection.isWearingFullGasHazmat(p),
                    "gas protection is passive and survives an empty battery");
                out.check(!SlotEquipment.hasOxygenGear(p), "empty Tactical Mask is not oxygen gear");
                p.wearHelmet(null);
                out.check(!slots.tick(p), "removed: no night vision");
            } finally {
                p.clean();
            }
            return out.build();
        }
    }

    /** Every item of the slots branch has a machine or crafting recipe. */
    private static final class Recipes implements Scenario {

        @Override
        public String name() {
            return "armor.slots.recipes";
        }

        @Override
        public String description() {
            return "Every special-slot item has an Assembler recipe at its tier voltage, and the two refill recipes exist.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (com.gregguns.recipe.ProductionTable.Row row : com.gregguns.recipe.EquipmentTable.ROWS) {
                Item made = GameRegistry.findItem(GregGuns.MODID, row.output());
                boolean found = false;
                for (gregtech.api.util.GTRecipe recipe : gregtech.api.recipe.RecipeMaps.assemblerRecipes
                    .getAllRecipes()) {
                    ItemStack first = recipe.mOutputs == null || recipe.mOutputs.length == 0 ? null
                        : recipe.mOutputs[0];
                    if (first != null && first.getItem() == made) {
                        found = true;
                        out.checkEquals(
                            row.id() + " EU/t",
                            row.tier() == Tier.MV ? (int) gregtech.api.enums.TierEU.RECIPE_MV
                                : (int) gregtech.api.enums.TierEU.RECIPE_HV,
                            recipe.mEUt);
                    }
                }
                out.check(found, row.id() + ": assembler recipe for " + row.output() + " missing");
            }
            out.check(craftingHas(EquipmentItems.gasMask()), "no Gas Mask refilter recipe");
            out.check(craftingHas(EquipmentItems.oxygenTanks()), "no Oxygen Tanks refill recipe");
            return out.build();
        }

        @SuppressWarnings("unchecked")
        private static boolean craftingHas(Item item) {
            for (Object o : net.minecraft.item.crafting.CraftingManager.getInstance()
                .getRecipeList()) {
                ItemStack made = ((net.minecraft.item.crafting.IRecipe) o).getRecipeOutput();
                if (made != null && made.getItem() == item) {
                    return true;
                }
            }
            return false;
        }
    }
}
