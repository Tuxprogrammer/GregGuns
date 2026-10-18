package com.gregguns.recipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.api.Tier;
import com.gregguns.armor.ArmorParts;
import com.gregguns.armor.flight.ItemAntiGravityDevice;
import com.gregguns.armor.flight.ItemGlider;
import com.gregguns.armor.flight.ItemJumpPack;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;
import com.gregguns.recipe.ProductionTable.Target;

/**
 * The flight gear recipes as plain data (Assembler rows): the Glider Backpack and Glider Wing parts, the Glider, the
 * Jump Pack (HV) and the Anti Gravity Device (IV). There is no Jetpack recipe (GGS-05-38). No Minecraft types; both
 * sides.
 */
// Spec: docs/gregguns-spec/05-armor/recipes.md (HV: Glider Backpack, Glider Wing, Glider, Jump Pack; IV: Anti Gravity
// Device); GGS-05-33, GGS-05-39
// Deviation: Mechanical Parts T3 (part_mechanical_3, RC-12) belongs to the component branch (M4) and is not registered
// yet; the Anti Gravity Device recipe is added at postload only once that item exists (FlightRecipes).
public final class FlightTable {

    /** Mechanical Parts T3 registry name (RC-12, area 06). */
    public static final String MECHANICAL_3 = "part_mechanical_3";
    /** Anti Gravity Core registry name (Reaction Chamber, IV). */
    public static final String ANTI_GRAVITY_CORE = "part_anti_gravity_core";

    private static final String CLOTH = ArmorCatalog.HEAVY_CLOTH;

    /** The rows, in sheet order. */
    public static final List<Row> ROWS = Collections.unmodifiableList(
        Arrays.asList(
            new Row(
                "FL-01",
                ArmorParts.GLIDER_BACKPACK,
                1,
                Tier.HV,
                Target.ASSEMBLER,
                18,
                1,
                Input.mod(CLOTH, 7),
                Input.gt("plate", "StainlessSteel", 2)),
            new Row(
                "FL-02",
                ArmorParts.GLIDER_WING,
                1,
                Tier.HV,
                Target.ASSEMBLER,
                18,
                2,
                Input.mod(CLOTH, 6),
                Input.gt("stick", "StainlessSteel", 3)),
            new Row(
                "FL-03",
                ItemGlider.NAME,
                1,
                Tier.HV,
                Target.ASSEMBLER,
                10,
                3,
                Input.mod(ArmorParts.GLIDER_BACKPACK, 1),
                Input.mod(ArmorParts.GLIDER_WING, 2)),
            new Row(
                "FL-04",
                ItemJumpPack.NAME,
                1,
                Tier.HV,
                Target.ASSEMBLER,
                18,
                1,
                Input.gt("plate", "StainlessSteel", 2),
                Input.gt("screw", "StainlessSteel", 2),
                Input.mod(ArmorParts.GLIDER_BACKPACK, 1),
                Input.mod(ArmorParts.TANK_AIR, 2),
                Input.itemList("Electric_Piston_HV", 1),
                Input.gt("circuit", "Advanced", 1)),
            new Row(
                "FL-05",
                ItemAntiGravityDevice.NAME,
                1,
                Tier.IV,
                Target.ASSEMBLER,
                18,
                1,
                Input.gt("plate", "TungstenSteel", 2),
                Input.itemList("Field_Generator_IV", 1),
                Input.gt("circuit", "Elite", 2),
                Input.mod(ANTI_GRAVITY_CORE, 1),
                Input.mod(MECHANICAL_3, 2),
                Input.mod(ArmorParts.GLIDER_BACKPACK, 1))));

    private FlightTable() {}

    /**
     * @return the rows keyed by output name, in table order
     */
    public static Map<String, Row> byOutput() {
        Map<String, Row> map = new LinkedHashMap<>();
        for (Row row : ROWS) {
            map.put(row.output(), row);
        }
        return map;
    }
}
