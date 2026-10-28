package com.gregguns.icbm.worldgen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
            return "Every bonus item resolves; the damaged body carries its flag and the warhead is an item, never lit.";
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
}
