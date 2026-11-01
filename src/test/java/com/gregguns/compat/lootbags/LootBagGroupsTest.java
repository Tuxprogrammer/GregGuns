package com.gregguns.compat.lootbags;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.gregguns.questbook.QuestbookResourcesTest;

/**
 * Checks the loot bag groups that ship in the jar, and the plain-Java rules {@link LootBagGroups} applies to them.
 * Reads
 * the XML with the JDK's DOM parser, as the class does; nothing here loads EnhancedLootBags.
 */
// Spec: docs/gregguns-spec/06-items/pack-loot.md (EnhancedLootBags)
class LootBagGroupsTest {

    private static final String LANG = "src/main/resources/assets/gregguns/lang/en_US.lang";
    private static final String ICONS = "src/main/resources/assets/gregguns/textures/items/";
    /**
     * The only items from other mods a GregGuns bag may hold: precursors that GregGuns' own recipes consume, checked
     * against a recipe dump of the GregGuns rows (owner, 2026-10-29: bags hold GregGuns items and the precursors our
     * recipes use, no food and no generic tier loot). Adding one means finding the GregGuns recipe that consumes it.
     */
    private static final Set<String> PRECURSORS = new HashSet<>(
        Arrays.asList(
            "gregtech:gt.metaitem.01:17019", // aluminium plate: AK-47 and others
            "gregtech:gt.metaitem.01:17028", // titanium plate: Bio Gun and others
            "gregtech:gt.metaitem.01:17300", // bronze plate: hand cannon, steam armor
            "gregtech:gt.metaitem.01:17305", // steel plate: bolt-action rifle and others
            "gregtech:gt.metaitem.01:17306", // stainless steel plate: rockets, AS50
            "gregtech:gt.metaitem.01:17316", // tungstensteel plate: anti-gravity device and others
            "gregtech:gt.metaitem.01:17325", // naquadah alloy plate: nuclear rocket, nuclear cell
            "gregtech:gt.metaitem.01:17372", // HSS-G plate: alien blaster and others
            "gregtech:gt.metaitem.01:17129", // neutronium plate: the Inert T5 Warhead (WarheadRecipes)
            "gregtech:gt.metaitem.01:17327", // naquadria plate: the Inert T4 Warhead (WarheadRecipes)
            "gregtech:gt.metaitem.01:11101", // plutonium-241 ingot: antimatter payload (WarheadRecipeSpec)
            "gregtech:gt.metaitem.01:32677", // UV field generator: antimatter payload
            "gregtech:gt.metaitem.01:32678", // UHV field generator: red matter payload
            "gregtech:gt.metaitem.01:32688", // UHV emitter: red matter payload
            "gregtech:gt.metaitem.01:32724", // quantum eye: antimatter and red matter payloads
            "gregtech:gt.metaitem.01:17874", // polyethylene plate: chainsaw, MAC-10
            "gregtech:gt.metaitem.01:23305", // steel rod: combat shotgun
            "gregtech:gt.metaitem.01:32540", // EV battery: laser, sonic and tesla rifles
            "gregtech:gt.metaitem.01:32602", // HV motor: SAM, casing
            "gregtech:gt.metaitem.01:32603", // EV motor: minigun
            "gregtech:gt.metaitem.01:32604", // IV motor: exo armor
            "gregtech:gt.metaitem.01:32607", // ZPM motor: nuke launcher
            "gregtech:gt.metaitem.01:32702", // MV circuit: explosive charge, night vision goggles
            "gregtech:gt.metaitem.01:32704", // EV circuit: laser rifle, missile fuzes
            "gregtech:gt.metaitem.01:32705", // IV circuit: anti-gravity device and others
            "gregtech:gt.metaitem.01:32706", // LuV circuit: alien blaster, missile fuzes
            "gregtech:gt.metaitem.03:32088", // ZPM circuit: nuclear rocket, nuclear cell
            "minecraft:gunpowder", // the Ammo Press
            "Backpack:tannedLeather")); // the pack's Tanned Leather: the lining of the armor sets

    private static List<LootBagGroups.Group> groups;

    @BeforeAll
    static void load() throws IOException {
        groups = LootBagGroups.parse(LootBagGroups.readResource());
    }

    @Test
    void shippedGroupsAreSound() {
        assertEquals(new ArrayList<String>(), LootBagGroups.problems(groups));
    }

    @Test
    void idsAre1701To1714And1700StaysFree() {
        Set<Integer> expected = new HashSet<>();
        for (int i = 1701; i <= 1714; i++) {
            expected.add(i);
        }
        assertEquals(expected, LootBagGroups.ids(groups));
    }

    @Test
    void everyDropIsAGregGunsItemOrAPrecursorOurRecipesUse() {
        Set<String> names = QuestbookResourcesTest.registeredGregGunsNames();
        List<String> bad = new ArrayList<>();
        for (LootBagGroups.Group g : groups) {
            for (LootBagGroups.Drop d : g.drops) {
                if (d.itemName.startsWith("gregguns:")) {
                    // A metadata variant (the Advanced Explosive Charge is explosive_charge:1) names its base item.
                    if (!names.contains(LootBagGroups.splitItemName(d.itemName)[1])) {
                        bad.add(g.id + ": not a GregGuns name " + d.itemName);
                    }
                } else if (!PRECURSORS.contains(d.itemName)) {
                    bad.add(g.id + ": not a GregGuns recipe input " + d.itemName);
                }
                if (!d.nbt.isEmpty()) bad.add(g.id + ": " + d.identifier + " carries NBT");
                if (d.randomAmount) bad.add(g.id + ": " + d.identifier + " has a random amount");
            }
        }
        assertEquals(new ArrayList<String>(), bad);
    }

    @Test
    void everyGroupHasANameAndAnIcon() throws IOException {
        String lang = new String(Files.readAllBytes(Paths.get(LANG)), StandardCharsets.UTF_8);
        for (LootBagGroups.Group g : groups) {
            assertTrue(lang.contains("\n" + g.name + "="), g.name + " has no English name");
            assertEquals("gregguns:lootbag/" + g.key(), g.icon, g.id + " icon");
            Path png = Paths.get(ICONS + "lootbag/" + g.key() + ".png");
            assertTrue(Files.isRegularFile(png), "missing icon " + png);
        }
    }

    @Test
    void tierBagsChainDownward() {
        // Recruit stands alone; each tier bag above it merges the one below; the ordnance crates start from HV, and the
        // UV and UHV crates stand alone.
        int[][] chain = { { 1701, 0 }, { 1702, 1701 }, { 1703, 1702 }, { 1704, 1703 }, { 1705, 1704 }, { 1706, 1705 },
            { 1707, 1706 }, { 1708, 1707 }, { 1709, 0 }, { 1710, 1704 }, { 1711, 1710 }, { 1712, 1711 }, { 1713, 0 },
            { 1714, 0 } };
        for (int[] link : chain) {
            LootBagGroups.Group g = groups.stream()
                .filter(x -> x.id == link[0])
                .findFirst()
                .get();
            assertEquals(link[1] != 0, g.combineTrash, g.id + " merges a trash group");
            if (link[1] != 0) assertEquals(link[1], g.trashGroup, g.id + " trash group");
        }
    }

    @Test
    void uvAndUhvCratesHoldNoLowerTierLoot() {
        // The LuV and ZPM gate items (D313): LuV Master circuit, HSS-G plate, naquadah alloy plate, ZPM circuit and
        // motor.
        Set<String> lower = new HashSet<>(
            Arrays.asList(
                "gregtech:gt.metaitem.01:32706",
                "gregtech:gt.metaitem.01:17372",
                "gregtech:gt.metaitem.01:17325",
                "gregtech:gt.metaitem.03:32088",
                "gregtech:gt.metaitem.01:32607"));
        for (int id = 1713; id <= 1714; id++) {
            final int want = id;
            LootBagGroups.Group g = groups.stream()
                .filter(x -> x.id == want)
                .findFirst()
                .get();
            assertFalse(g.combineTrash, id + " takes no trash group");
            assertTrue(g.drops.size() >= 4, id + " has enough distinct drops for its draws");
            for (LootBagGroups.Drop d : g.drops) {
                assertFalse(lower.contains(d.itemName), id + " holds lower-tier " + d.itemName);
            }
        }
    }

    @Test
    void problemsCatchesTheUsualMistakes() {
        String xml = "<LootGroups>"
            + group(1701, "gregguns.lootbag.group.a", 1, 2, "true", 1799, drop("x", "gregguns:a", 0))
            + group(1701, "gregguns.lootbag.group.a", 1, 1, "false", 0, drop("y", "gregguns:a", 1))
            + group(15, "pack", 1, 1, "false", 0, drop("z", "bad name", 0) + drop("z", "gregguns:b", 0))
            + "</LootGroups>";
        List<String> p = LootBagGroups.problems(LootBagGroups.parse(xml));
        String all = String.join("\n", p);
        assertTrue(all.contains("1701: MinItems must equal MaxItems"), all);
        assertTrue(all.contains("group id 1701 appears twice"), all);
        assertTrue(all.contains("group name gregguns.lootbag.group.a appears twice"), all);
        assertTrue(all.contains("group 15 is outside"), all);
        assertTrue(all.contains("does not start with"), all);
        assertTrue(all.contains("bad item name bad name"), all);
        assertTrue(all.contains("drop z appears twice"), all);
        assertTrue(all.contains("has only limited drops"), all);
        assertTrue(all.contains("trash group 1799 is not a GregGuns group"), all);
    }

    @Test
    void parseRefusesBrokenXmlAndDoctypes() {
        assertThrows(IllegalArgumentException.class, () -> LootBagGroups.parse("<LootGroups>"));
        assertThrows(
            IllegalArgumentException.class,
            () -> LootBagGroups.parse("<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><LootGroups/>"));
        assertThrows(
            IllegalArgumentException.class,
            () -> LootBagGroups.parse("<LootGroups><LootGroup GroupMetaID=\"x\"/></LootGroups>"));
    }

    @Test
    void itemNamesSplitTheYamcoreWay() {
        assertArrayEquals(new String[] { "gregguns", "m4", null }, LootBagGroups.splitItemName("gregguns:m4"));
        assertArrayEquals(
            new String[] { "gregtech", "gt.metaitem.01", "17305" },
            LootBagGroups.splitItemName("gregtech:gt.metaitem.01:17305"));
        assertNull(LootBagGroups.splitItemName("m4"));
        assertNull(LootBagGroups.splitItemName("a:b:c"));
        assertNull(LootBagGroups.splitItemName(":b"));
        assertNull(LootBagGroups.splitItemName(null));
        assertTrue(LootBagGroups.resolvable("gregtech:gt.metaitem.01:17305", (m, n) -> m.equals("gregtech")));
        assertFalse(LootBagGroups.resolvable("harvestcraft:toastItem", (m, n) -> m.equals("gregtech")));
    }

    @Test
    void reservedRangeIs1700To1799() {
        assertTrue(LootBagGroups.isReservedId(1700));
        assertTrue(LootBagGroups.isReservedId(1799));
        assertFalse(LootBagGroups.isReservedId(1699));
        assertFalse(LootBagGroups.isReservedId(42));
    }

    private static String group(int id, String name, int min, int max, String trash, int trashId, String drops) {
        return "<LootGroup GroupMetaID=\"" + id
            + "\" GroupName=\""
            + name
            + "\" Rarity=\"0\" MinItems=\""
            + min
            + "\" MaxItems=\""
            + max
            + "\" CombineTrashGroup=\""
            + trash
            + "\" TrashGroup=\""
            + trashId
            + "\">"
            + drops
            + "</LootGroup>";
    }

    private static String drop(String id, String item, int limit) {
        return "<Loot Identifier=\"" + id
            + "\" ItemName=\""
            + item
            + "\" Amount=\"1\" NBTTag=\"\" Chance=\"10\" ItemGroup=\"\" LimitedDropCount=\""
            + limit
            + "\" RandomAmount=\"false\"/>";
    }
}
