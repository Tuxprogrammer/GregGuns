package com.gregguns.recipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.api.Tier;
import com.gregguns.armor.ArmorParts;
import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.slots.ItemAmmoPouch;
import com.gregguns.armor.slots.ItemGasMask;
import com.gregguns.armor.slots.ItemNightVisionGoggles;
import com.gregguns.armor.slots.ItemTacticalMask;
import com.gregguns.recipe.ProductionTable.Input;
import com.gregguns.recipe.ProductionTable.Row;
import com.gregguns.recipe.ProductionTable.Target;

/**
 * The special-slot equipment recipes as plain data (Assembler rows): Gas Mask Filter, Gas Mask, Night Vision Goggles,
 * Ammo Pouch (MV), Oxygen Mask, Oxygen Tanks, Compressed Air Tank and Tactical Mask (HV). The refilter and refill
 * shapeless recipes are in {@link EquipmentRecipes}. No Minecraft types; both sides.
 */
// Spec: docs/gregguns-spec/05-armor/recipes.md (Gas Mask, refilter and Night Vision Goggles; HV: Oxygen gear, Tactical
// Mask); docs/gregguns-spec/06-items/recipes.md (RC-25, RC-54); GGS-05-29, GGS-05-30, GGS-05-33, GGS-05-39
// Deviation: the Ammo Pouch has no recipe in any spec sheet (recipes.md assumes area 06 supplies it, area 06 does not).
// EQ-03 is new: MV, Aluminum gate, Heavy Cloth and leather like the Techguns tactical gear.
public final class EquipmentTable {

    /** Oxygen Mask registry name (a Galacticraft subclass or the fallback). */
    public static final String OXYGEN_MASK = "oxygen_mask";
    /** Oxygen Tanks registry name. */
    public static final String OXYGEN_TANKS = "oxygen_tanks";

    private static final String CLOTH = ArmorCatalog.HEAVY_CLOTH;

    /** The rows, in sheet order. */
    public static final List<Row> ROWS = Collections.unmodifiableList(
        Arrays.asList(
            // RC-25
            new Row(
                "RC-25",
                ArmorParts.GAS_MASK_FILTER,
                1,
                Tier.MV,
                Target.ASSEMBLER,
                4,
                0,
                Input.itemList("ActivatedCarbonFilterMesh", 1),
                Input.gt("plate", "Aluminium", 1),
                Input.gt("ring", "Aluminium", 1)),
            new Row(
                "EQ-01",
                ItemGasMask.NAME,
                1,
                Tier.MV,
                Target.ASSEMBLER,
                18,
                1,
                Input.gt("plate", "Rubber", 5),
                Input.vanilla("glass_pane", 2),
                Input.gt("plate", "Aluminium", 1),
                Input.mod(ArmorParts.GAS_MASK_FILTER, 1)),
            new Row(
                "EQ-02",
                ItemNightVisionGoggles.NAME,
                1,
                Tier.MV,
                Target.ASSEMBLER,
                18,
                2,
                Input.gt("plate", "Rubber", 2),
                Input.mod(CLOTH, 1),
                Input.gt("circuit", "Good", 2),
                Input.itemList("Sensor_MV", 1),
                Input.vanilla("glass", 2),
                Input.gt("plate", "Aluminium", 1)),
            new Row(
                "EQ-03",
                ItemAmmoPouch.NAME,
                1,
                Tier.MV,
                Target.ASSEMBLER,
                10,
                1,
                Input.mod(CLOTH, 3),
                Input.vanilla("leather", 2),
                Input.gt("plate", "Aluminium", 1)),
            new Row(
                "EQ-04",
                OXYGEN_MASK,
                1,
                Tier.HV,
                Target.ASSEMBLER,
                10,
                1,
                Input.gt("plate", "Polyethylene", 2),
                Input.gt("plate", "Rubber", 2),
                Input.gt("plate", "StainlessSteel", 1)),
            // RC-54
            new Row(
                "RC-54",
                ArmorParts.TANK_AIR,
                4,
                Tier.HV,
                Target.ASSEMBLER,
                10,
                2,
                Input.itemList("Cell_Empty", 4),
                Input.gt("plateDouble", "StainlessSteel", 4),
                Input.gt("ring", "StainlessSteel", 4)),
            new Row(
                "EQ-05",
                OXYGEN_TANKS,
                1,
                Tier.HV,
                Target.ASSEMBLER,
                16,
                1,
                Input.gt("plate", "Polyethylene", 4),
                Input.mod(ArmorParts.TANK_AIR, 2),
                Input.gt("plate", "StainlessSteel", 2)),
            new Row(
                "EQ-06",
                ItemTacticalMask.NAME,
                1,
                Tier.HV,
                Target.ASSEMBLER,
                18,
                1,
                Input.gt("plate", "Polyethylene", 4),
                Input.gt("circuit", "Advanced", 2),
                Input.mod(ItemNightVisionGoggles.NAME, 1),
                Input.mod(ItemGasMask.NAME, 1),
                Input.mod(OXYGEN_MASK, 1))));

    private EquipmentTable() {}

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
