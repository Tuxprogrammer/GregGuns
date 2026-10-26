package com.gregguns.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;

import org.junit.jupiter.api.Test;

import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.SetDef;

/**
 * The GT recycling data of the armor pieces and accessories, derived from their recipe tables. Prefix amounts are
 * stand-in figures (one plate is 100, a ring 20, a nugget 11), so a wrong prefix or count shows in the sum.
 */
class ArmorCompositionTest {

    private static final ToLongFunction<String> AMOUNT = prefix -> {
        switch (prefix) {
            case "plate":
                return 100;
            case "plateDouble":
                return 200;
            case "ring":
                return 20;
            case "nugget":
                return 11;
            case "screw":
                return 7;
            case "stick":
                return 50;
            default:
                return 1;
        }
    };

    private static Map<String, Map<String, Long>> all() {
        return ArmorComposition.all(AMOUNT);
    }

    private static Map<String, Long> of(String name) {
        Map<String, Long> composition = all().get(name);
        assertNotNull(composition, name + " should have recycling data");
        return composition;
    }

    private static List<String> allPieceNames() {
        List<String> names = new ArrayList<>();
        for (SetDef set : ArmorCatalog.ALL) {
            for (ArmorSlot slot : set.pieces()
                .keySet()) {
                names.add(set.pieceName(slot));
            }
        }
        return names;
    }

    @Test
    void everyArmorPieceHasRecyclingData() {
        Map<String, Map<String, Long>> all = all();
        List<String> pieces = allPieceNames();
        assertTrue(pieces.size() >= 29, "the catalog should list every piece, saw " + pieces.size());
        for (String name : pieces) {
            assertTrue(all.containsKey(name), name + " should have recycling data");
        }
    }

    @Test
    void everyAccessoryMadeOfGtMaterialsHasRecyclingData() {
        Map<String, Map<String, Long>> all = all();
        List<ProductionTable.Row> rows = new ArrayList<>(EquipmentTable.ROWS);
        rows.addAll(FlightTable.ROWS);
        for (ProductionTable.Row row : rows) {
            boolean metal = false;
            for (ProductionTable.Input input : row.inputs()) {
                metal |= input.special() == null && !input.isGregGuns()
                    && ComponentComposition.RECYCLED_PREFIXES.contains(input.prefix());
            }
            if (metal) {
                assertTrue(all.containsKey(row.output()), row.id() + " " + row.output());
            }
        }
    }

    @Test
    void everyMaterialOfARecipeComesBack() {
        Map<String, Map<String, Long>> all = all();
        for (ProductionTable.Row row : ArmorComposition.machineRows()) {
            for (ProductionTable.Input input : row.inputs()) {
                if (input.isGregGuns() || input.special() != null
                    || !ComponentComposition.RECYCLED_PREFIXES.contains(input.prefix())) {
                    continue;
                }
                Map<String, Long> composition = all.get(row.output());
                assertNotNull(composition, row.id());
                assertTrue(composition.containsKey(input.material()), row.id() + " should return " + input.material());
            }
        }
    }

    @Test
    void amountsAreTheRecipeInputsTimesGtsPrefixAmounts() {
        // AR-09 Soldier's helmet: 3 steel plates.
        assertEquals(
            300L,
            of(ArmorCatalog.T1_COMBAT.pieceName(ArmorSlot.HELMET)).get("Steel")
                .longValue());
        // AR-10 vest: 3 steel plates.
        assertEquals(
            300L,
            of(ArmorCatalog.T1_COMBAT.pieceName(ArmorSlot.CHESTPLATE)).get("Steel")
                .longValue());
        // AR-25 Beret: one aluminum ring and nothing else metal.
        Map<String, Long> beret = of(ArmorCatalog.T2_BERET.pieceName(ArmorSlot.HELMET));
        assertEquals(1, beret.size());
        assertEquals(
            20L,
            beret.get("Aluminium")
                .longValue());
    }

    @Test
    void handCraftedPiecesReturnTheirPlatesAndNuggets() {
        // AR-01 Scout helmet: one bronze plate in the grid.
        assertEquals(
            100L,
            of(ArmorCatalog.T1_SCOUT.pieceName(ArmorSlot.HELMET)).get("Bronze")
                .longValue());
        // AR-02 Scout chestplate: one bronze plate.
        assertEquals(
            100L,
            of(ArmorCatalog.T1_SCOUT.pieceName(ArmorSlot.CHESTPLATE)).get("Bronze")
                .longValue());
        // AR-05 Miner helmet: two cast iron plates (the dye and the cloth return nothing).
        Map<String, Long> helmet = of(ArmorCatalog.T1_MINER.pieceName(ArmorSlot.HELMET));
        assertEquals(1, helmet.size());
        assertEquals(
            200L,
            helmet.get("CastIron")
                .longValue());
        // AR-08 Miner boots: two cast iron nuggets.
        assertEquals(
            22L,
            of(ArmorCatalog.T1_MINER.pieceName(ArmorSlot.BOOTS)).get("CastIron")
                .longValue());
    }

    @Test
    void anUpgradeReturnsItsBasePieceAndItsOwnPlates() {
        // AR-21 Riot helmet = Combat helmet (3 aluminum plates, AR-13) + 2 lead plates.
        Map<String, Long> riot = of(ArmorCatalog.T2_RIOT.pieceName(ArmorSlot.HELMET));
        assertEquals(
            300L,
            riot.get("Aluminium")
                .longValue());
        assertEquals(
            200L,
            riot.get("Lead")
                .longValue());
        // AR-42 Exo helmet = Advanced Combat helmet (3 epoxy and 2 titanium plates) + 1 tungsten steel plate + one
        // Mechanical Parts T3, which returns its own metal.
        Map<String, Long> exo = of(ArmorCatalog.T3_EXO.pieceName(ArmorSlot.HELMET));
        Map<String, ProductionTable.Row> known = new java.util.HashMap<>(ProductionTable.byOutput());
        known.putAll(ComponentTable.byOutput());
        Map<String, Long> mechanical = ComponentComposition.perItem(known.get(FlightTable.MECHANICAL_3), known, AMOUNT);
        assertEquals(
            300L + mechanical.getOrDefault("EpoxidFiberReinforced", 0L),
            exo.get("EpoxidFiberReinforced")
                .longValue());
        assertEquals(
            200L + mechanical.getOrDefault("Titanium", 0L),
            exo.get("Titanium")
                .longValue());
        assertEquals(
            100L + mechanical.getOrDefault("TungstenSteel", 0L),
            exo.get("TungstenSteel")
                .longValue());
    }

    @Test
    void steamArmorReturnsItsBronzePlates() {
        // Record 0022: hand-crafted from 3, 5, 4 and 2 bronze plates (100 each); the cloth returns nothing.
        long[] plates = { 3, 5, 4, 2 };
        for (ArmorSlot slot : ArmorSlot.values()) {
            Map<String, Long> piece = of(ArmorCatalog.STEAM.pieceName(slot));
            assertEquals(
                plates[slot.ordinal()] * 100L,
                piece.get("Bronze")
                    .longValue(),
                slot.toString());
            assertEquals(1, piece.size(), slot.toString());
        }
    }

    @Test
    void clothLeatherCircuitsAndMachinePartsReturnNothing() {
        Set<String> materials = new HashSet<>();
        for (Map<String, Long> composition : all().values()) {
            materials.addAll(composition.keySet());
        }
        assertFalse(materials.contains("Leather"));
        assertFalse(all().containsKey(ArmorCatalog.HEAVY_CLOTH));
        assertFalse(all().containsKey(ArmorCatalog.TREATED_LEATHER));
    }

    @Test
    void everyAmountIsPositive() {
        for (Map.Entry<String, Map<String, Long>> item : all().entrySet()) {
            assertFalse(
                item.getValue()
                    .isEmpty(),
                item.getKey());
            for (Map.Entry<String, Long> material : item.getValue()
                .entrySet()) {
                assertTrue(material.getValue() > 0, item.getKey() + " " + material.getKey());
            }
        }
    }
}
