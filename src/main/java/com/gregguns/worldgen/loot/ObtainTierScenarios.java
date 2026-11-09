package com.gregguns.worldgen.loot;

import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.compat.lootbags.LootBagGroups;
import com.gregguns.compat.lootbags.LootBagInjector;
import com.gregguns.compat.vending.VendingCatalog;
import com.gregguns.core.config.LootConfig;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.entity.npc.GunSalvage;
import com.gregguns.entity.npc.NpcDrops;
import com.gregguns.entity.npc.NpcLootCapHandler;
import com.gregguns.entity.npc.loot.EquipmentProfile;
import com.gregguns.entity.npc.loot.InfernalLootTable;
import com.gregguns.entity.npc.loot.LootDrop;
import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;
import com.gregguns.item.gun.ItemGun;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Scenarios of the loot tier cap (D363 to D365): the obtain-tier helper on real pack items, then every GregGuns loot
 * source outside the silo complexes (structure chests, NPC and Infernal drops, Vending trades, quest bags) drawn or
 * listed at the default cap and at the top one. Each restores {@code loot.maxTier} on every exit. Dev environment
 * only; server side.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (Loot tier cap); D363, D364, D365
public final class ObtainTierScenarios {

    /** The default cap (EV) and the top one (UMV), the two every draw runs at. */
    public static final int[] CAPS = { 4, 12 };

    private ObtainTierScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Helper());
        registry.register(new Structures());
        registry.register(new Npcs());
        registry.register(new Vending());
        registry.register(new LootBags());
    }

    /** @return the obtain tier of a loot reference; -1 when this pack does not have it (the audit skips it) */
    public static int obtainOf(String ref) {
        ItemStack s = LootCategories.stack(ref, 1);
        return s == null ? -1 : ObtainTier.of(s);
    }

    /**
     * Fails the result once per distinct problem, up to {@code max} of them, then once for the rest.
     *
     * @param out      the result
     * @param problems what went wrong, repeats included
     * @param max      how many distinct problems to name
     */
    public static void reportAll(ScenarioResult.Builder out, List<String> problems, int max) {
        List<String> distinct = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(problems));
        for (int i = 0; i < distinct.size() && i < max; i++) {
            out.check(false, distinct.get(i));
        }
        if (distinct.size() > max) {
            out.check(false, (distinct.size() - max) + " more");
        }
    }

    private static final class Helper implements Scenario {

        @Override
        public String name() {
            return "loot.obtain.helper";
        }

        @Override
        public String description() {
            return "The obtain-tier helper on real pack items: the white dwarf nugget, a ZPM circuit, a neutronium"
                + " ingot and a ghost circuit are never loot at EV; a stainless plate, an EV motor, an EV circuit and"
                + " a knife are; the cap gate and the NPC cap follow loot.maxTier (D363, D364).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            int saved = LootConfig.maxTier;
            try {
                ObtainTier.clearCache();
                // Source: get(prefix, material, amount) answers GT's unified stack, null when there is none:
                // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L134
                ItemStack nugget = GTOreDictUnificator.get(OrePrefixes.nugget, Materials.WhiteDwarfMatter, 1);
                ItemStack neutronium = GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Neutronium, 1);
                ItemStack plate = GTOreDictUnificator.get(OrePrefixes.plate, Materials.StainlessSteel, 1);
                // Source: get(amount) copies the entry's stack:
                // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L3480
                ItemStack zpm = ItemList.Circuit_Quantummainframe.get(1);
                ItemStack ghost = ItemList.Circuit_BasicElectronic.get(1);
                ItemStack motor = ItemList.Electric_Motor_EV.get(1);
                ItemStack data = ItemList.Circuit_Data.get(1);
                Item knifeItem = GameRegistry.findItem(GregGuns.MODID, "combat_knife");
                ItemStack knife = knifeItem == null ? null : new ItemStack(knifeItem);
                out.check(nugget != null, "the pack has no white dwarf matter nugget");
                out.check(neutronium != null, "the pack has no neutronium ingot");
                out.check(
                    plate != null && zpm != null && ghost != null && motor != null && data != null,
                    "a GT item is missing");
                out.check(knife != null, "no combat knife");
                if (nugget == null || neutronium == null
                    || plate == null
                    || zpm == null
                    || ghost == null
                    || motor == null
                    || data == null
                    || knife == null) {
                    return out.build();
                }
                out.checkEquals("white dwarf nugget tier", 12, ObtainTier.of(nugget));
                out.checkEquals("ZPM circuit tier", 7, ObtainTier.of(zpm));
                out.checkEquals("neutronium ingot tier", 8, ObtainTier.of(neutronium));
                out.checkEquals("ghost circuit tier", ObtainTierRules.UNKNOWN, ObtainTier.of(ghost));
                out.checkEquals("stainless plate tier", 3, ObtainTier.of(plate));
                out.checkEquals("EV motor tier", 4, ObtainTier.of(motor));
                out.checkEquals("EV circuit tier", 4, ObtainTier.of(data));
                out.checkEquals("knife tier", 1, ObtainTier.of(knife));
                LootConfig.maxTier = 4;
                for (ItemStack s : new ItemStack[] { nugget, zpm, neutronium, ghost }) {
                    out.check(!ObtainTier.allowed(s), ObtainAudit.describe(s) + " is loot at EV");
                    out.check(NpcLootCapHandler.removes(s), ObtainAudit.describe(s) + " survives the NPC cap at EV");
                    out.check(ObtainAudit.problem(s, 12) != null, ObtainAudit.describe(s) + " passes the audit at EV");
                }
                for (ItemStack s : new ItemStack[] { plate, motor, data, knife }) {
                    out.check(ObtainTier.allowed(s), ObtainAudit.describe(s) + " is not loot at EV");
                    out.check(!NpcLootCapHandler.removes(s), ObtainAudit.describe(s) + " is taken by the NPC cap");
                    out.check(ObtainAudit.problem(s, 12) == null, ObtainAudit.describe(s) + " fails the audit at EV");
                }
                out.check(!ObtainTier.allowed(motor, 3), "an EV motor passes an HV ceiling");
                out.check(!NpcLootCapHandler.removes(new ItemStack(Items.diamond)), "the NPC cap takes a diamond");
                InventoryBasic inv = new InventoryBasic("gate", false, 2);
                TierGatedInventory gate = new TierGatedInventory(inv, 12);
                gate.setInventorySlotContents(0, zpm.copy());
                gate.setInventorySlotContents(1, motor.copy());
                out.check(inv.getStackInSlot(0) == null, "the inventory gate wrote a ZPM circuit at EV");
                out.check(inv.getStackInSlot(1) != null, "the inventory gate refused an EV motor at EV");
                LootConfig.maxTier = 12;
                out.check(ObtainTier.allowed(zpm), "a ZPM circuit is not loot with the cap at UMV");
                out.check(ObtainTier.allowed(nugget), "the white dwarf nugget is not loot with the cap at UMV");
                out.check(!ObtainTier.allowed(zpm, 6), "a ZPM circuit passes a LuV ceiling");
                out.check(!ObtainTier.allowed(ghost), "a ghost circuit is loot with the cap at UMV");
                out.check(!NpcLootCapHandler.removes(zpm), "the NPC cap takes a ZPM circuit with the cap at UMV");
            } finally {
                LootConfig.maxTier = saved;
            }
            return out.build();
        }
    }

    private static final class Structures implements Scenario {

        @Override
        public String name() {
            return "loot.obtain.structure";
        }

        @Override
        public String description() {
            return "Every structure loot line's whole crafting chain is known and at most its declared tier; 300"
                + " chests per category and richness at the EV and UMV caps hold nothing above the lower of the"
                + " category ceiling and the cap (D363, D364).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            int saved = LootConfig.maxTier;
            try {
                for (LootCategory cat : LootCategoryDefs.all()) {
                    reportAll(out, LootTierCheck.obtainViolations(cat, ObtainTierScenarios::obtainOf), 20);
                }
                for (int cap : CAPS) {
                    LootConfig.maxTier = cap;
                    int stacks = 0;
                    int bad = 0;
                    for (LootCategory cat : LootCategoryDefs.all()) {
                        int ceiling = cat.ceiling()
                            .voltageIndex();
                        for (int richness = 0; richness <= LootDraw.MAX_RICHNESS; richness++) {
                            Random rand = new Random(0x10C0L + 31L * richness + cap);
                            for (int c = 0; c < 300; c++) {
                                InventoryBasic chest = new InventoryBasic("loot", false, 54);
                                LootCategories.fill(chest, cat.name(), richness, rand);
                                for (int i = 0; i < chest.getSizeInventory(); i++) {
                                    ItemStack s = chest.getStackInSlot(i);
                                    if (s == null) continue;
                                    stacks++;
                                    String p = ObtainAudit.problem(s, ceiling);
                                    if (p != null && bad++ < 20) {
                                        out.check(false, cat.name() + " at cap " + cap + ": " + p);
                                    }
                                }
                            }
                        }
                    }
                    out.check(stacks > 0, "no structure chest drew anything at cap " + cap);
                    out.checkEquals("structure stacks above the cap " + cap, 0, bad);
                    out.note("cap " + cap + ": " + stacks + " stacks");
                }
            } finally {
                LootConfig.maxTier = saved;
            }
            return out.build();
        }
    }

    private static final class Npcs implements Scenario {

        @Override
        public String name() {
            return "loot.obtain.npc";
        }

        @Override
        public String description() {
            return "Every NPC profile's table and fixed drops, every carried gun's ammo and salvage, and every Infernal"
                + " drop are known and at most their tier; the NPC cap takes a ZPM circuit at EV and keeps an EV motor (D364).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            int saved = LootConfig.maxTier;
            try {
                int drops = 0;
                for (Map.Entry<String, NpcProfile> e : NpcProfiles.all()
                    .entrySet()) {
                    NpcProfile p = e.getValue();
                    int ceiling = p.ceiling()
                        .voltageIndex();
                    if (p.table() != null) {
                        for (LootDrop d : p.table()
                            .slots()) {
                            if (d != null) {
                                drops++;
                                checkDrop(out, "npc " + e.getKey() + " table", d.ref(), ceiling);
                            }
                        }
                    }
                    for (LootDrop d : p.fixedDrops()) {
                        drops++;
                        // D349's one accepted exception: the Attack Helicopter's Cybernetic Parts, EV over its HV.
                        boolean accepted = "AttackHelicopter".equals(e.getKey()) && d.ref()
                            .equals(GregGuns.MODID + ":part_cybernetic");
                        checkDrop(out, "npc " + e.getKey() + " fixed", d.ref(), accepted ? 4 : ceiling);
                    }
                }
                for (InfernalLootTable.Entry e : InfernalLootTable.entries()) {
                    drops++;
                    checkDrop(
                        out,
                        "infernal",
                        e.drop()
                            .ref(),
                        e.tier()
                            .voltageIndex());
                }
                // Every carried gun's own drop: its ammo or tank, and each salvage part, known and at most the gun.
                java.util.Set<String> guns = new java.util.LinkedHashSet<>();
                for (NpcProfile p : NpcProfiles.all()
                    .values()) {
                    if (p.equipment() != null) {
                        for (EquipmentProfile.Weapon w : p.equipment()
                            .weapons()) {
                            guns.add(w.gun());
                        }
                    }
                }
                for (String name : guns) {
                    ItemStack gunStack = LootCategories.stack(GregGuns.MODID + ":" + name, 1);
                    if (gunStack == null || !(gunStack.getItem() instanceof ItemGun)) {
                        continue;
                    }
                    ItemGun gun = (ItemGun) gunStack.getItem();
                    NpcDrops.GunDrop rule = NpcDrops.gunDrop(name, gun.stats());
                    if (rule == null) {
                        continue;
                    }
                    int gunTier = ObtainTier.of(gunStack);
                    if (rule.ref() != null) {
                        drops++;
                        checkDrop(out, "gun " + name, rule.ref(), gunTier);
                    }
                    for (GunSalvage.Entry part : rule.salvage()) {
                        drops++;
                        checkDrop(out, "gun " + name + " salvage", part.ref(), gunTier);
                    }
                }
                out.check(drops > 0, "no NPC drop lines");
                out.note(drops + " drop lines");
                LootConfig.maxTier = 4;
                ItemStack zpm = ItemList.Circuit_Quantummainframe.get(1);
                ItemStack motor = ItemList.Electric_Motor_EV.get(1);
                out.check(zpm != null && NpcLootCapHandler.removes(zpm), "the NPC cap keeps a ZPM circuit at EV");
                out.check(motor != null && !NpcLootCapHandler.removes(motor), "the NPC cap takes an EV motor at EV");
            } finally {
                LootConfig.maxTier = saved;
            }
            return out.build();
        }

        private static void checkDrop(ScenarioResult.Builder out, String where, String ref, int tier) {
            // The stack the drop handlers make (NpcDrops): gt: parts and ~full tanks included.
            ItemStack s = LootCategories.stack(ref, 1);
            if (s == null) {
                boolean ours = ref.startsWith(GregGuns.MODID + ":") || ref.startsWith("gt:");
                out.check(!ours, where + ": " + ref + " does not resolve");
                if (!ours) {
                    out.note(where + ": " + ref + " is not in this pack");
                }
                return;
            }
            int t = ObtainTier.of(s);
            out.check(t != ObtainTierRules.UNKNOWN, where + ": " + ref + " has no known obtain tier");
            out.check(
                t == ObtainTierRules.UNKNOWN || t <= tier,
                where + ": " + ref + " needs " + ObtainTierRules.name(t) + ", above " + ObtainTierRules.name(tier));
        }
    }

    private static final class Vending implements Scenario {

        @Override
        public String name() {
            return "loot.obtain.vending";
        }

        @Override
        public String description() {
            return "Every Vending trade but the spawn eggs sells an item whose whole crafting chain is known and at most"
                + " the trade's own tier, which its intro quest gates (D365).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            int offers = 0;
            for (VendingCatalog.Offer o : VendingCatalog.build()) {
                if (o.key()
                    .startsWith("egg:")) {
                    continue;
                }
                offers++;
                Item item = GameRegistry.findItem(GregGuns.MODID, o.item());
                if (item == null) {
                    Block b = GameRegistry.findBlock(GregGuns.MODID, o.item());
                    item = b == null ? null : Item.getItemFromBlock(b);
                }
                if (item == null) {
                    out.check(false, o.key() + ": no item " + o.item());
                    continue;
                }
                int t = ObtainTier.of(new ItemStack(item, 1, o.meta()));
                int tier = o.tier()
                    .voltageIndex();
                out.check(t != ObtainTierRules.UNKNOWN, o.key() + " has no known obtain tier");
                out.check(
                    t == ObtainTierRules.UNKNOWN || t <= tier,
                    o.key() + " sells " + ObtainTierRules.name(t) + " at " + ObtainTierRules.name(tier));
            }
            out.check(offers > 0, "no Vending offers");
            out.note(offers + " offers");
            return out.build();
        }
    }

    private static final class LootBags implements Scenario {

        @Override
        public String name() {
            return "loot.obtain.lootbags";
        }

        @Override
        public String description() {
            return "Every drop of the shipped loot bag groups is at most its quest's tier, so none is taken out at load;"
                + " a ZPM circuit fails a LuV bag and passes a ZPM one (D365).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            List<LootBagGroups.Group> groups;
            try {
                groups = LootBagGroups.parse(LootBagGroups.readResource());
            } catch (java.io.IOException e) {
                return ScenarioResult.fail(name(), "cannot read the bag groups: " + e.getMessage());
            }
            int drops = 0;
            for (LootBagGroups.Group g : groups) {
                out.check(LootBagGroups.questTier(g.id) >= 0, "group " + g.id + " has no quest tier");
                for (LootBagGroups.Drop d : g.drops) {
                    if (!LootBagGroups.resolvable(
                        d.itemName,
                        (m, n) -> GameRegistry.findItem(m, n) != null || GameRegistry.findBlock(m, n) != null)) {
                        continue;
                    }
                    drops++;
                    out.check(
                        LootBagInjector.withinBagTier(g.id, d.itemName),
                        "group " + g.id
                            + " ("
                            + ObtainTierRules.name(LootBagGroups.questTier(g.id))
                            + ") holds "
                            + d.itemName);
                }
            }
            out.check(drops > 0, "no bag drops resolved");
            out.note(drops + " drops");
            out.check(
                !LootBagInjector.withinBagTier(1712, "gregtech:gt.metaitem.03:32088"),
                "a LuV bag takes a ZPM circuit");
            out.check(
                LootBagInjector.withinBagTier(1708, "gregtech:gt.metaitem.03:32088"),
                "a ZPM bag refuses a ZPM circuit");
            return out.build();
        }
    }
}
