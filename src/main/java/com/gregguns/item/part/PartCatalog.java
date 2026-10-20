package com.gregguns.item.part;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gregguns.api.Tier;

/**
 * The component inventory of area 06 as plain data: every GregGuns crafting component, its tier, which class
 * registers it, which kind of item it is, and where its recipe lives. The unit tests, the registration loop in
 * {@link PartItems}, {@link ComponentItems} and the dev self-check's allowlist all read this one table, so they cannot
 * drift apart.
 *
 * <p>
 * Items the spec maps to a GT part (plates, wires, circuits, plastic, rubber, motors, the coil and so on) are not
 * here and are never registered: recipes use the GT item through the unificator ({@link #GT_MAPPED}). Pure data, both
 * sides.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (component index),
// docs/gregguns-spec/06-items/components-and-materials.md (part by part), GGS-06-10, GGS-06-47
public final class PartCatalog {

    /** What kind of item a component is, which decides the class that registers it. */
    public enum Kind {
        /** A plain part ({@link ItemPart}). */
        PART,
        /** A 1,000 mB fluid container ({@link ItemTank}). */
        TANK,
        /** A use-to-heal consumable ({@link ItemConsumable}). */
        CONSUMABLE,
        /** A nuclear power cell ({@link ItemPart}), not an electric item. */
        CELL,
        /** A cell that is only made by use, hidden from the creative tab ({@link ItemPartHidden}). */
        CELL_HIDDEN
    }

    /** The class that registers the item, so the registry name is never added twice. */
    public enum Owner {
        /** {@link PartItems} (M1 and M2 parts), in the guns module. */
        PART_ITEMS,
        /** {@link ComponentItems} (the rest of the component index), in the guns module. */
        COMPONENT_ITEMS,
        /** {@code com.gregguns.armor.ArmorParts}, in the armor module. */
        ARMOR_PARTS
    }

    /** One component. */
    public static final class Entry {

        private final String name;
        private final Tier tier;
        private final Kind kind;
        private final Owner owner;
        private final String recipe;
        private final String noRecipeReason;

        Entry(String name, Tier tier, Kind kind, Owner owner, String recipe, String noRecipeReason) {
            this.name = name;
            this.tier = tier;
            this.kind = kind;
            this.owner = owner;
            this.recipe = recipe;
            this.noRecipeReason = noRecipeReason;
        }

        /** @return the bare registry name, for example {@code "part_receiver_iron"} */
        public String name() {
            return name;
        }

        /** @return the D1 tier of the component */
        public Tier tier() {
            return tier;
        }

        /** @return the kind of item */
        public Kind kind() {
            return kind;
        }

        /** @return the class that registers it */
        public Owner owner() {
            return owner;
        }

        /** @return where the recipe lives, for example {@code "RC-13 ComponentTable"}; null with no recipe */
        public String recipe() {
            return recipe;
        }

        /** @return the reason the component has no recipe (the self-check allowlist), or {@code null} if it has one */
        public String noRecipeReason() {
            return noRecipeReason;
        }
    }

    /**
     * Techguns items that the spec maps to a GT part. GregGuns registers none of them; this list is the checked
     * statement of that rule (GGS-06-10, GGS-06-44).
     */
    public static final List<String> GT_MAPPED = Collections.unmodifiableList(
        Arrays.asList(
            "plate (Tin, Copper, Iron, Bronze, Steel, Titanium, Lead): OrePrefixes.plate",
            "ingot Titanium, titanium ore: OrePrefixes.ingot and the GTNH ore system",
            "wire (copper, gold): OrePrefixes.wireFine",
            "circuit boards: OrePrefixes.circuit by tier material",
            "rubber, raw rubber: OrePrefixes.plate Rubber and RubberRaw",
            "plastic sheet, raw plastic: OrePrefixes.plate Polyethylene",
            "yellowcake, enriched uranium: dust Uranium and Uranium235",
            "TGX: Materials.TNT",
            "Electric Engine: ItemList.Electric_Motor_<tier>",
            "Coil: folded into the emitter and motor recipes",
            "carbon fibers, carbon plate: plate.EpoxidFiberReinforced"));

    private static final List<Entry> ENTRIES;
    private static final Map<String, Entry> BY_NAME;

    static {
        List<Entry> list = new ArrayList<>();
        // Gun parts. The first five were registered by M1 and M2; their names are kept.
        list.add(older("part_barrel_hardened", Tier.LV, "RC-05 ProductionTable"));
        list.add(older("part_mechanical_2", Tier.LV, "RC-11 ProductionTable"));
        list.add(older("part_barrel_iron", Tier.LV, "RC-04 ProductionTable"));
        list.add(older("part_mechanical_1", Tier.STEAM, "RC-03 PartCraftingRecipes"));
        list.add(older("part_stock_wooden", Tier.STEAM, "RC-02 PartCraftingRecipes"));
        list.add(older("part_barrel_stone", Tier.STEAM, "RC-01 PartCraftingRecipes"));
        list.add(part("part_barrel_carbon", Tier.EV, "RC-06 ComponentTable"));
        list.add(part("part_barrel_laser", Tier.EV, "RC-07 ComponentTable"));
        list.add(part("part_stock_plastic", Tier.MV, "RC-08 ComponentTable"));
        list.add(part("part_stock_carbon", Tier.EV, "RC-09 ComponentTable"));
        list.add(part("part_pump_mechanism", Tier.LV, "RC-10 ComponentTable"));
        list.add(part("part_mechanical_3", Tier.EV, "RC-12 ComponentTable"));
        list.add(older("part_receiver_iron", Tier.LV, "RC-13 ProductionTable"));
        list.add(part("part_receiver_steel", Tier.MV, "RC-14 ComponentTable"));
        list.add(part("part_receiver_hardened", Tier.HV, "RC-15 ComponentTable"));
        list.add(part("part_receiver_carbon", Tier.EV, "RC-16 ComponentTable"));
        // Emitters and electronics.
        list.add(part("part_uv_emitter", Tier.EV, "RC-17 ComponentTable"));
        list.add(part("part_sonic_emitter", Tier.EV, "RC-18 ComponentTable"));
        list.add(part("part_rad_emitter", Tier.ZPM, "RC-19 ComponentTable (Assembly Line)"));
        list.add(part("part_cybernetic", Tier.EV, "RC-20 ComponentTable"));
        // Plating and cloth.
        list.add(part("part_power_plating", Tier.IV, "RC-24 ComponentTable"));
        list.add(part("part_nuclear_warhead", Tier.ZPM, "RC-26 ComponentTable (Assembly Line)"));
        // Tanks, cells and consumables.
        list.add(new Entry("tank_fuel", Tier.HV, Kind.TANK, Owner.COMPONENT_ITEMS, "RC-52 ComponentTable", null));
        list.add(new Entry("tank_bio", Tier.EV, Kind.TANK, Owner.COMPONENT_ITEMS, "RC-53 ComponentTable", null));
        list.add(
            new Entry(
                "cell_nuclear",
                Tier.ZPM,
                Kind.CELL,
                Owner.COMPONENT_ITEMS,
                "RC-59 and RC-60 ComponentTable",
                null));
        list.add(
            new Entry(
                "cell_nuclear_depleted",
                Tier.ZPM,
                Kind.CELL_HIDDEN,
                Owner.COMPONENT_ITEMS,
                null,
                "made by use: a reload consumes cell_nuclear and returns this (GGS-06-42)"));
        list.add(new Entry("bandages", Tier.LV, Kind.CONSUMABLE, Owner.COMPONENT_ITEMS, "RC-61 ComponentTable", null));
        list.add(new Entry("medkit", Tier.MV, Kind.CONSUMABLE, Owner.COMPONENT_ITEMS, "RC-62 ComponentTable", null));
        // Registered by other classes; reused, never re-added. Their recipes are written by their owners.
        list.add(
            new Entry(
                "part_heavy_cloth",
                Tier.STEAM,
                Kind.PART,
                Owner.ARMOR_PARTS,
                "RC-21 ArmorParts (crafting table)",
                null));
        list.add(
            new Entry("part_gas_mask_filter", Tier.MV, Kind.PART, Owner.ARMOR_PARTS, "RC-25 EquipmentTable", null));
        list.add(new Entry("tank_air", Tier.HV, Kind.TANK, Owner.ARMOR_PARTS, "RC-54 EquipmentTable", null));
        // Registered by ArmorParts on the m3 branches (armor-t2-t3, flight-gear), with their own recipes.
        list.add(new Entry("part_treated_leather", Tier.LV, Kind.PART, Owner.ARMOR_PARTS, "RC-22 armor (m3)", null));
        list.add(
            new Entry("part_steam_armor_plating", Tier.EV, Kind.PART, Owner.ARMOR_PARTS, "RC-23 armor (m3)", null));
        list.add(
            new Entry(
                "part_glider_backpack",
                Tier.HV,
                Kind.PART,
                Owner.ARMOR_PARTS,
                "flight-gear Assembler (m3)",
                null));
        list.add(
            new Entry("part_glider_wing", Tier.HV, Kind.PART, Owner.ARMOR_PARTS, "flight-gear Assembler (m3)", null));
        list.add(
            new Entry(
                "part_laser_focus",
                Tier.EV,
                Kind.PART,
                Owner.COMPONENT_ITEMS,
                "RC-64 ComponentTable (Laser Engraver)",
                null));
        list.add(
            new Entry(
                "part_anti_gravity_core",
                Tier.IV,
                Kind.PART,
                Owner.COMPONENT_ITEMS,
                "RC-65 ComponentTable (Assembler)",
                null));
        ENTRIES = Collections.unmodifiableList(list);
        Map<String, Entry> byName = new LinkedHashMap<>();
        for (Entry entry : list) {
            if (byName.put(entry.name(), entry) != null) {
                throw new IllegalStateException("duplicate component " + entry.name());
            }
        }
        BY_NAME = Collections.unmodifiableMap(byName);
    }

    private PartCatalog() {}

    private static Entry part(String name, Tier tier, String recipe) {
        return new Entry(name, tier, Kind.PART, Owner.COMPONENT_ITEMS, recipe, null);
    }

    private static Entry older(String name, Tier tier, String recipe) {
        return new Entry(name, tier, Kind.PART, Owner.PART_ITEMS, recipe, null);
    }

    /** @return every component, in registration order */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    /**
     * @param name a bare registry name
     * @return the entry, or {@code null} when the name is not a component
     */
    public static Entry get(String name) {
        return BY_NAME.get(name);
    }

    /**
     * @param name a bare registry name
     * @return the component's tier, or {@code null} when the name is not a component
     */
    public static Tier tierOf(String name) {
        Entry entry = BY_NAME.get(name);
        return entry == null ? null : entry.tier();
    }

    /**
     * @return the self-check allowlist: components that have no recipe, with the reason, keyed by bare registry name
     */
    public static Map<String, String> noRecipeAllowlist() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Entry entry : ENTRIES) {
            if (entry.noRecipeReason() != null) {
                out.put(entry.name(), entry.noRecipeReason());
            }
        }
        return out;
    }
}
