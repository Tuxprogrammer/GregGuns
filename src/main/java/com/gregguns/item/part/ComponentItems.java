package com.gregguns.item.part;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.Item;

import com.gregguns.api.Tier;
import com.gregguns.core.registry.ModuleRegistrar;

/**
 * Registers the components that {@link PartItems} does not: every {@link PartCatalog} entry whose owner is
 * {@code COMPONENT_ITEMS} is registered here under its catalog name and
 * tier, so the registry and the inventory cannot disagree.
 *
 * <p>
 * Items registered elsewhere keep their registrars ({@link PartItems} for the M1 and M2 parts, {@code ArmorParts}:
 * Heavy Cloth, Gas Mask Filter and the
 * Compressed Air Tank; {@code ReactionParts}: the three Reaction Chamber foci). Names match the texture files.
 *
 * <p>
 * Usage: {@code ComponentItems.get(ComponentItems.RECEIVER_STEEL)}.
 * Spec: docs/gregguns-spec/06-items/recipes.md. Common side, preInit.
 */
public final class ComponentItems {

    /** Registry name of the Carbon Barrel. */
    public static final String BARREL_CARBON = "part_barrel_carbon";
    /** Registry name of the Laser Barrel. */
    public static final String BARREL_LASER = "part_barrel_laser";
    /** Registry name of the Plastic Gun Stock. */
    public static final String STOCK_PLASTIC = "part_stock_plastic";
    /** Registry name of the Carbon Gunstock. */
    public static final String STOCK_CARBON = "part_stock_carbon";
    /** Registry name of the Pump Mechanism. */
    public static final String PUMP_MECHANISM = "part_pump_mechanism";
    /** Registry name of Mechanical Parts T3. */
    public static final String MECHANICAL_3 = "part_mechanical_3";
    /** Registry name of the Steel Receiver. */
    public static final String RECEIVER_STEEL = "part_receiver_steel";
    /** Registry name of the Hardened Receiver. */
    public static final String RECEIVER_HARDENED = "part_receiver_hardened";
    /** Registry name of the Carbon Receiver. */
    public static final String RECEIVER_CARBON = "part_receiver_carbon";
    /** Registry name of the UV Emitter. */
    public static final String UV_EMITTER = "part_uv_emitter";
    /** Registry name of the Sonic Emitter. */
    public static final String SONIC_EMITTER = "part_sonic_emitter";
    /** Registry name of the Rad Emitter. */
    public static final String RAD_EMITTER = "part_rad_emitter";
    /** Registry name of Cybernetic Parts. */
    public static final String CYBERNETIC = "part_cybernetic";
    /** Registry name of Power Plating. */
    public static final String POWER_PLATING = "part_power_plating";
    /** Registry name of the Small Nuclear Warhead. */
    public static final String NUCLEAR_WARHEAD = "part_nuclear_warhead";
    /** Registry name of the Fuel Tank. */
    public static final String TANK_FUEL = "tank_fuel";
    /** Registry name of the Bio Tank. */
    public static final String TANK_BIO = "tank_bio";
    /** Registry name of the Compressed Air Tank (registered by the armor module). */
    public static final String TANK_AIR = "tank_air";
    /** Registry name of the Nuclear Power Cell. */
    public static final String CELL_NUCLEAR = "cell_nuclear";
    /** Registry name of the depleted Nuclear Power Cell. */
    public static final String CELL_NUCLEAR_DEPLETED = "cell_nuclear_depleted";
    /** Registry name of Bandages. */
    public static final String BANDAGES = "bandages";
    /** Registry name of the Medkit. */
    public static final String MEDKIT = "medkit";

    private static final Map<String, Item> REGISTERED = new LinkedHashMap<>();

    private ComponentItems() {}

    /**
     * Registers every component the catalog assigns to this class. Call once from {@code GunsModule.preInit}.
     *
     * @param registrar the guns module's registrar
     */
    public static void register(ModuleRegistrar registrar) {
        for (PartCatalog.Entry entry : PartCatalog.entries()) {
            if (entry.owner() == PartCatalog.Owner.COMPONENT_ITEMS) {
                registerOne(registrar, entry.name());
            }
        }
    }

    /**
     * Registers one component by name, so the caller picks the registration order (see {@code GunsItemOrder}).
     *
     * @param registrar the guns module's registrar
     * @param name      a component name the catalog assigns to this class
     */
    public static void registerOne(ModuleRegistrar registrar, String name) {
        REGISTERED.put(name, registrar.registerItem(name, create(PartCatalog.get(name))));
    }

    private static Item create(PartCatalog.Entry entry) {
        Tier tier = entry.tier();
        switch (entry.kind()) {
            case TANK:
                return tank(entry.name(), tier);
            case CONSUMABLE:
                return new ItemConsumable(
                    tier,
                    BANDAGES.equals(entry.name()) ? ConsumableSpec.BANDAGES : ConsumableSpec.MEDKIT);
            case CELL_HIDDEN:
                return new ItemPartHidden(tier);
            default:
                return new ItemPart(tier);
        }
    }

    /**
     * Builds a tank item for a catalog name; the armor module uses it for the Compressed Air Tank too.
     *
     * @param name a tank's registry name ({@link #TANK_FUEL}, {@link #TANK_BIO} or {@link #TANK_AIR})
     * @param tier its tier
     * @return the tank item with its accepted GT fluids
     */
    public static ItemTank tank(String name, Tier tier) {
        // Spec: docs/gregguns-spec/06-items/fluids.md, default accepted set; RC-56 to RC-58 for the fills.
        // Deviation: the Bio Tank takes BartWorks' E. coli culture fluid (RC-57), made from Biomass in the Bacterial
        // Vat,
        // and Biomass only behind bioGun.acceptBiomass or when the culture cannot be resolved (BioTankFluids). The
        // fluid
        // table's BioDiesel is the Fuel Tank's.
        switch (name) {
            case TANK_FUEL:
                return new ItemTank(
                    tier,
                    name,
                    false,
                    // Source: GT names these materials "Fuel" (Materials.Diesel) and "Gasoline"
                    // (Materials.GasolineRegular),
                    // so Materials.get needs those names; "Diesel" and "GasolineRegular" are only the Java field names.
                    // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L4860-L4868
                    Arrays.asList("Fuel", "Gasoline", "BioDiesel", "Ethanol"));
            case TANK_BIO:
                return new ItemTank(tier, name, false, Arrays.asList("Biomass"), BioTankFluids::accepts);
            case TANK_AIR:
                return new ItemTank(tier, name, true, Arrays.asList("Air"));
            default:
                throw new IllegalArgumentException("not a tank: " + name);
        }
    }

    /**
     * @param registryName a bare registry name owned by this class
     * @return the item, or {@code null} before registration or when the guns module is off
     */
    public static Item get(String registryName) {
        return REGISTERED.get(registryName);
    }
}
