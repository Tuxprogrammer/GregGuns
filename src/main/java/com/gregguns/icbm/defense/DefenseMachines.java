package com.gregguns.icbm.defense;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.registry.ModuleRegistrar;
import com.gregguns.icbm.recipe.IcbmIngredients;
import com.gregguns.icbm.recipe.Ing;
import com.gregguns.machine.GregGunsMTEIDs;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Creates the radars, the radar tower and the air-defense battery, registers the SAM and the radar gun with the SAM
 * entity, and queues their Assembler recipes. {@link #register()} runs inside GT's MTE window, whatever the module flag
 * says (content always registers); the recipes are added only for an enabled module. Common side.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md, air-defense-battery.md (Recipes)
public final class DefenseMachines {

    /** Entity id 124: the SAM (after the silo doors at 123). */
    private static final int ENTITY_ID_SAM = 124;
    /** Entity id 126: the radar dish (D125, D190). */
    private static final int ENTITY_ID_RADAR_DISH = 126;
    /** Overlay folders (NTM-derived, LGPL, D47) under {@code textures/blocks/}. */
    private static final String ICONS = "machines/thirdparty/ntm/";

    private static Item sam;
    private static Item radarGun;

    private DefenseMachines() {}

    /**
     * Registers the SAM item, the radar gun and the SAM entity (preInit).
     *
     * @param registrar the ICBM module's registrar
     */
    public static void registerItems(ModuleRegistrar registrar) {
        sam = registrar.registerItem(ItemSam.NAME, new ItemSam());
        sam.setTextureName("gregguns:thirdparty/ntm/sam_missile");
        radarGun = registrar.registerItem(ItemRadarGun.NAME, new ItemRadarGun());
        radarGun.setTextureName("gregguns:thirdparty/ntm/radar_gun");
        registrar.registerEntity("sam", EntitySam.class, ENTITY_ID_SAM, 160, 1, true);
        registrar.registerEntity("radar_dish", EntityRadarDish.class, ENTITY_ID_RADAR_DISH, 160, 3, false);
    }

    /** Constructs the machines. Call once, from the ICBM module's init. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L85-L100
        // (construction is legal between GT's preload and postload)
        // The two small-radar controllers (D125, D190).
        new MTERadar(GregGunsMTEIDs.RADAR_HV, "gregguns.icbm.radar.hv", "Radar (HV)", 3, ICONS + "radar_hv/");
        new MTERadar(GregGunsMTEIDs.RADAR_EV, "gregguns.icbm.radar.ev", "Radar (EV)", 4, ICONS + "radar_ev/");
        new MTERadarTower(GregGunsMTEIDs.RADAR_TOWER, "gregguns.icbm.radartower", "Radar Tower");
        new MTEAirDefense(
            GregGunsMTEIDs.AIR_DEFENSE,
            "gregguns.icbm.airdefense",
            "Air-Defense Battery",
            tooltip("battery", "battery"),
            ICONS + "air_defense_battery/");
    }

    private static String[] tooltip(String ignoredName, String prefix) {
        String p = "gregguns.icbm." + prefix + ".tooltip";
        return new String[] { p, p + ".detect", p + ".range", p + ".outputs", p + ".link" };
    }

    /** Queues the recipes and the recycling data; call from the ICBM module's init (enabled module only). */
    public static void registerRecipes() {
        GregTechAPI.sBeforeGTPostload.add(DefenseMachines::addRecipes);
    }

    /** @return the SAM item; null before registration */
    public static Item samItem() {
        return sam;
    }

    /** @return the radar gun item; null before registration */
    public static Item radarGunItem() {
        return radarGun;
    }

    /**
     * @param id     an MTE id
     * @param amount stack size
     * @return that machine as an item stack
     * @throws IllegalStateException if it was not registered
     */
    public static ItemStack stack(int id, int amount) {
        if (GregTechAPI.METATILEENTITIES[id] == null) {
            throw new IllegalStateException("MTE " + id + " is not registered");
        }
        return GregTechAPI.METATILEENTITIES[id].getStackForm(amount);
    }

    private static ItemStack circuits(String name, int n, Tier tier) {
        return IcbmIngredients.stack(Ing.gt("circuit", name, n, tier, 3.0));
    }

    private static ItemStack plates(int n) {
        return IcbmIngredients.stack(Ing.gt("plate", "StainlessSteel", n, Tier.HV, 1.0));
    }

    private static void addRecipes() {
        try {
            machine(
                Materials.StainlessSteel,
                GregGunsMTEIDs.RADAR_HV,
                21,
                600,
                TierEU.RECIPE_HV,
                ItemList.Hull_HV.get(1),
                ItemList.Sensor_HV.get(2),
                ItemList.Emitter_HV.get(1),
                circuits("Advanced", 2, Tier.HV));
            machine(
                Materials.Titanium,
                GregGunsMTEIDs.RADAR_EV,
                22,
                600,
                TierEU.RECIPE_EV,
                ItemList.Hull_EV.get(1),
                ItemList.Sensor_EV.get(2),
                ItemList.Emitter_EV.get(1),
                circuits("Data", 2, Tier.EV));
            machine(
                Materials.TungstenSteel,
                GregGunsMTEIDs.RADAR_TOWER,
                23,
                1200,
                TierEU.RECIPE_IV,
                ItemList.Hull_IV.get(1),
                ItemList.Sensor_IV.get(2),
                ItemList.Emitter_IV.get(2),
                circuits("Elite", 2, Tier.IV));
            machine(
                Materials.StainlessSteel,
                GregGunsMTEIDs.AIR_DEFENSE,
                19,
                900,
                TierEU.RECIPE_HV,
                ItemList.Hull_HV.get(1),
                ItemList.Robot_Arm_HV.get(2),
                ItemList.Sensor_HV.get(1),
                circuits("Advanced", 2, Tier.HV));
            radarCasing();
            ItemStack samStack = new ItemStack(sam, 2);
            sam(samStack);
            ItemStack gunStack = new ItemStack(radarGun, 1);
            ItemStack gun = ItemList.Sensor_HV.get(1);
            ItemStack emitter = ItemList.Emitter_HV.get(1);
            ItemStack circ = circuits("Advanced", 1, Tier.HV);
            ItemStack plate = plates(2);
            if (gun == null || emitter == null || circ == null || plate == null) {
                GregGuns.LOG.error("Skipping the radar gun recipe: a GT input did not resolve");
            } else {
                GTValues.RA.stdBuilder()
                    .circuit(18)
                    .itemInputs(gun, emitter, circ, plate)
                    .itemOutputs(gunStack)
                    .duration(300)
                    .eut(TierEU.RECIPE_HV)
                    .addTo(RecipeMaps.assemblerRecipes);
                GTOreDictUnificator.addItemData(
                    new ItemStack(radarGun, 1),
                    new ItemData(Materials.StainlessSteel, OrePrefixes.plate.getMaterialAmount() * 2));
            }
        } catch (RuntimeException e) {
            GregGuns.LOG.error("Defense recipes failed", e);
        }
    }

    /** Plates one Assembler run of the Radar Casing takes. */
    static final int CASING_PLATES = 6;
    /** Casings one run makes. */
    static final int CASING_YIELD = 4;

    /**
     * The radars' wall: Stainless Steel plate in the Assembler at HV, four blocks a run; it recycles to its plate
     * share.
     */
    private static void radarCasing() {
        ItemStack plate = IcbmIngredients.stack(Ing.gt("plate", "StainlessSteel", CASING_PLATES, Tier.HV, 1.0));
        if (plate == null) {
            GregGuns.LOG.error("Skipping the Radar Casing recipe: Stainless Steel plate did not resolve");
            return;
        }
        ItemStack out = new ItemStack(GregGunsCasings.INSTANCE, CASING_YIELD, GregGunsCasings.RADAR_CASING);
        GTValues.RA.stdBuilder()
            .circuit(16)
            .itemInputs(plate)
            .itemOutputs(out)
            .duration(200)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTOreDictUnificator.addItemData(
            new ItemStack(GregGunsCasings.INSTANCE, 1, GregGunsCasings.RADAR_CASING),
            new ItemData(
                Materials.StainlessSteel,
                OrePrefixes.plate.getMaterialAmount() * CASING_PLATES / CASING_YIELD));
    }

    private static void sam(ItemStack out) {
        ItemStack motor = ItemList.Electric_Motor_HV.get(1);
        ItemStack sensor = ItemList.Sensor_HV.get(1);
        ItemStack plate = plates(4);
        ItemStack circ = circuits("Advanced", 1, Tier.HV);
        if (motor == null || sensor == null || plate == null || circ == null) {
            GregGuns.LOG.error("Skipping the SAM recipe: a GT input did not resolve");
            return;
        }
        GTValues.RA.stdBuilder()
            .circuit(17)
            .itemInputs(plate, motor, sensor, circ)
            .itemOutputs(out)
            .duration(300)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);
        // The plates come back (two per missile); the motor, sensor and circuit do not (D44).
        GTOreDictUnificator.addItemData(
            new ItemStack(sam, 1),
            new ItemData(Materials.StainlessSteel, OrePrefixes.plate.getMaterialAmount() * 2));
    }

    private static void machine(Materials hull, int id, int circuit, int duration, long eut, ItemStack... inputs) {
        for (ItemStack s : inputs) {
            if (s == null) {
                GregGuns.LOG.error("Skipping the recipe of MTE {}: a GT input did not resolve", id);
                return;
            }
        }
        GTValues.RA.stdBuilder()
            .circuit(circuit)
            .itemInputs(inputs)
            .itemOutputs(stack(id, 1))
            .duration(duration)
            .eut(eut)
            .addTo(RecipeMaps.assemblerRecipes);
        // The hull's own casing plates come back (HV stainless steel, EV titanium, IV tungstensteel); electronics do
        // not (D44).
        GTOreDictUnificator.addItemData(stack(id, 1), new ItemData(hull, OrePrefixes.plate.getMaterialAmount() * 8));
    }
}
