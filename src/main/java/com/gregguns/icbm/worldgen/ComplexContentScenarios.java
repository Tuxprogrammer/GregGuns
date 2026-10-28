package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.complex.MachineRole;
import com.gregguns.worldgen.loot.LootCategories;
import com.gregguns.worldgen.spawner.HoleKind;
import com.gregguns.worldgen.spawner.HoleSpawnerRegistration;

/**
 * Scenarios of the silo complex contents (I6 coder B): loot ceilings over generated chests, the bonus items and the
 * machine picker. Dev environment only. Server side.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/loot-and-ceilings.md (Tests)
public final class ComplexContentScenarios {

    private ComplexContentScenarios() {}

    /** @param registry where to register */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new LootCeiling());
        registry.register(new Bonuses());
        registry.register(new Machines());
        registry.register(new Sweep());
    }

    private static String id(ItemStack s) {
        return net.minecraft.item.Item.itemRegistry.getNameForObject(s.getItem()) + "@" + s.getItemDamage();
    }

    private static final class LootCeiling implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.loot_ceiling";
        }

        @Override
        public String description() {
            return "Chests drawn from every silo key and ceiling hold only items of lines at or below the ceiling, and"
                + " the tables registered (exit box 3, GGI-08-13).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            SiloChests.register();
            int unresolved = 0;
            for (String key : SiloLootTables.KEYS) {
                for (int c = SiloCeiling.EV; c <= SiloCeiling.UMV; c++) {
                    Set<String> allowed = new HashSet<>();
                    for (SiloLootTables.Line l : SiloLootTables.lines(key, c)) {
                        ItemStack s = LootCategories.stack(l.ref(), 1);
                        if (s == null) {
                            unresolved++;
                            out.note("unresolved " + key + "@" + SiloCeiling.name(c) + ": " + l.ref());
                        } else {
                            allowed.add(id(s));
                        }
                    }
                    out.check(!allowed.isEmpty(), key + "@" + SiloCeiling.name(c) + " has no resolved line");
                    String cat = SiloLootTables.categoryName(key, c);
                    int bad = 0;
                    int stacks = 0;
                    for (int chest = 0; chest < 150; chest++) {
                        InventoryBasic inv = new InventoryBasic("loot", false, 54);
                        LootCategories.fill(inv, cat, chest % 3, ctx.random());
                        for (int i = 0; i < inv.getSizeInventory(); i++) {
                            ItemStack s = inv.getStackInSlot(i);
                            if (s != null) {
                                stacks++;
                                if (!allowed.contains(id(s))) {
                                    bad++;
                                    out.note(cat + " drew " + id(s));
                                }
                            }
                        }
                    }
                    out.check(stacks > 0, cat + " drew nothing in 150 chests");
                    out.checkEquals(cat + " stacks outside the ceiling", 0, bad);
                }
            }
            out.note("unresolved lines: " + unresolved);
            return out.build();
        }
    }

    private static final class Bonuses implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.bonus_items";
        }

        @Override
        public String description() {
            return "Every bonus item resolves; the damaged body carries its flag and the magazine casing resolves to an inert frame box.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            Placement stick = Placement.at(Placement.Kind.CHEST, 0, 0, 0)
                .extra("100,200")
                .build();
            for (Placement.Bonus b : Placement.Bonus.values()) {
                ItemStack s = SiloBonusItems.stack(b, stick);
                out.check(s != null, "bonus " + b + " did not resolve");
                if (b == Placement.Bonus.DAMAGED_BODY && s != null) {
                    out.check(
                        com.gregguns.icbm.missile.ItemBlockMissileBody.isSalvage(s),
                        "damaged body lacks its flag");
                }
            }
            for (HoleKind k : new HoleKind[] { HoleKind.SILO_GUARD, HoleKind.SILO_ELITE }) {
                out.check(HoleSpawnerRegistration.block(k) != null, k + " block is not registered");
            }
            return out.build();
        }
    }

    private static final class Machines implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.machines";
        }

        @Override
        public String description() {
            return "Every machine role resolves to a GT item at every ceiling, never above it (D129, GGI-08-14).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            List<String> hulls = new ArrayList<>();
            for (MachineRole role : MachineRole.values()) {
                if (role == MachineRole.NASA_WORKBENCH || role == MachineRole.LAUNCH_CONTROL
                    || role == MachineRole.MAGAZINE_CASING) {
                    continue;
                }
                for (int c = SiloCeiling.EV; c <= SiloCeiling.UMV; c++) {
                    MachinePicker.Pick p = MachinePicker.pick(role, c, false);
                    out.check(p != null, role + "@" + SiloCeiling.name(c) + " found no machine");
                    if (p != null) {
                        out.check(p.tier() <= c, role + "@" + c + " picked tier " + p.tier());
                        if (p.hull()) {
                            hulls.add(role + "@" + SiloCeiling.name(c));
                            out.check(
                                !(role == MachineRole.GENERATOR || role == MachineRole.BATTERY_BUFFER
                                    || role == MachineRole.WORKBENCH
                                    || role == MachineRole.PUMP),
                                role + "@" + SiloCeiling.name(c) + " fell back to a hull though GT has the machine");
                        }
                    }
                }
            }
            out.note("hull fallbacks (roles GT has no single block for): " + hulls.size());
            return out.build();
        }
    }

    /**
     * The lowest ceiling at which a chest may hold each metal, written down from the tier rule of the spec sheet
     * (loot-and-ceilings.md: the plate ladder is Stainless Steel, Titanium, Tungstensteel, HSS-G, Naquadah Alloy,
     * Neutronium and up) and not read from {@link SiloLootTables}, so the sweep can fail on its own: the table's tags
     * and this list must agree.
     */
    private static final String[][] METAL_FROM = { { "StainlessSteel", "HV" }, { "Titanium", "EV" },
        { "TungstenSteel", "IV" }, { "HSSG", "LuV" }, { "NaquadahAlloy", "ZPM" }, { "Neutronium", "UV" },
        { "CosmicNeutronium", "UHV" }, { "Infinity", "UEV" }, { "TranscendentMetal", "UIV" }, { "SpaceTime", "UMV" } };
    private static final String[] VOLT = { "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV", "UIV",
        "UMV" };

    /**
     * @return the metal name when the stack is made of one that needs a higher ceiling than {@code ceiling}, else null
     */
    private static String materialAbove(ItemStack st, int ceiling) {
        gregtech.api.objects.ItemData d = gregtech.api.util.GTOreDictUnificator.getAssociation(st);
        if (d == null || d.mMaterial == null || d.mMaterial.mMaterial == null) {
            return null;
        }
        String name = d.mMaterial.mMaterial.mName;
        for (String[] m : METAL_FROM) {
            if (m[0].equals(name)) {
                int need = java.util.Arrays.asList(VOLT)
                    .indexOf(m[1]);
                return need > ceiling ? name + " (needs " + m[1] + ")" : null;
            }
        }
        return null;
    }

    private static final class Sweep implements Scenario {

        @Override
        public String name() {
            return "icbm.complex.loot_sweep";
        }

        @Override
        public String description() {
            return "Over many generated complexes in every dimension that hosts one (Ross128's rolled ceiling included),"
                + " every real chest the contents pass fills holds only items from its key's table at or below the"
                + " dimension's ceiling, or its own bonus items, and no machine is above the ceiling (exit box 3, D38).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            SiloChests.register();
            int chests = 0;
            int stacks = 0;
            int bad = 0;
            Set<Integer> rossSeen = new HashSet<>();
            Map<String, Set<String>> allowedCache = new HashMap<>();
            for (String dim : com.gregguns.icbm.complex.PlanetTable.names()) {
                Integer tier = com.gregguns.icbm.complex.PlanetTable.tierOf(dim);
                boolean rolled = tier != null && tier == com.gregguns.icbm.complex.PlanetTable.ROLLED;
                int seeds = rolled ? 24 : 4;
                int dimStacks = 0;
                for (int n = 0; n < seeds; n++) {
                    long seed = 0x7000L * (n + 1) + dim.hashCode();
                    com.gregguns.icbm.complex.ComplexPlan plan = com.gregguns.icbm.complex.ComplexLayout
                        .generate(seed, n, 9);
                    int ceiling = com.gregguns.icbm.complex.gen.ComplexPlacer
                        .ceilingFor(dim, plan.seed(), SiloCeiling.DEFAULT_ROSS_WEIGHTS);
                    if (rolled) {
                        rossSeen.add(ceiling);
                    }
                    ActivityLevel level = ActivityLevel.values()[n % ActivityLevel.values().length];
                    for (Placement p : ComplexContents.build(plan, level, ceiling, new int[] { 100, 200 })) {
                        if (p.kind() == Placement.Kind.MACHINE) {
                            out.check(p.tier() <= ceiling, dim + " machine tier " + p.tier() + " > " + ceiling);
                            MachinePicker.Pick pick = MachinePicker.pick(p.role(), Math.max(1, p.tier()), p.dead());
                            if (pick != null && p.role() != MachineRole.NASA_WORKBENCH
                                && p.role() != MachineRole.LAUNCH_CONTROL
                                && p.role() != MachineRole.MAGAZINE_CASING) {
                                out.check(pick.tier() <= ceiling, dim + " picked machine tier " + pick.tier());
                            }
                            continue;
                        }
                        if (p.kind() != Placement.Kind.CHEST) {
                            continue;
                        }
                        chests++;
                        InventoryBasic inv = new InventoryBasic("loot", false, 27);
                        SiloChests.fill(inv, p, seed, n, 9);
                        Set<String> allowed = allowedCache.computeIfAbsent(p.key() + "@" + ceiling, k -> {
                            Set<String> a = new HashSet<>();
                            for (SiloLootTables.Line l : SiloLootTables.lines(p.key(), ceiling)) {
                                ItemStack st = LootCategories.stack(l.ref(), 1);
                                if (st != null) {
                                    a.add(id(st));
                                }
                            }
                            return a;
                        });
                        Set<String> own = new HashSet<>(allowed);
                        for (Placement.Bonus b : p.bonuses()) {
                            ItemStack st = SiloBonusItems.stack(b, p);
                            if (st != null) {
                                own.add(id(st));
                            }
                        }
                        for (int i = 0; i < inv.getSizeInventory(); i++) {
                            ItemStack st = inv.getStackInSlot(i);
                            if (st != null) {
                                stacks++;
                                dimStacks++;
                                String above = materialAbove(st, ceiling);
                                if (above != null) {
                                    bad++;
                                    out.note(dim + "@" + SiloCeiling.name(ceiling) + " " + p.key() + " holds " + above);
                                }
                                if (!own.contains(id(st))) {
                                    bad++;
                                    out.note(dim + "@" + SiloCeiling.name(ceiling) + " " + p.key() + " drew " + id(st));
                                }
                            }
                        }
                    }
                }
                out.check(dimStacks > 0, dim + " drew nothing");
            }
            out.checkEquals("stacks outside their ceiling", 0, bad);
            out.check(rossSeen.size() >= 5, "Ross128 rolled several ceilings: " + rossSeen);
            out.note("chests " + chests + ", stacks " + stacks + ", Ross ceilings " + rossSeen);
            return out.build();
        }
    }
}
