package com.gregguns.icbm.defense;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
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
    /**
     * Overlay folder of the radars and the battery. Placeholder: the Launch Control's overlays, until the I4 art
     * lands; the art replaces these by path.
     */
    private static final String PLACEHOLDER_ICONS = "machines/thirdparty/ntm/launch_control/";

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
        sam.setTextureName("gregguns:thirdparty/ntm/missile_chip_t1");
        radarGun = registrar.registerItem(ItemRadarGun.NAME, new ItemRadarGun());
        radarGun.setTextureName("gregguns:thirdparty/ntm/missile_schematic_t1");
        registrar.registerEntity("sam", EntitySam.class, ENTITY_ID_SAM, 160, 1, true);
    }

    /** Constructs the machines. Call once, from the ICBM module's init. */
    public static void register() {
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/CommonMetaTileEntity.java#L85-L100
        // (construction is legal between GT's preload and postload)
        new MTERadar(
            GregGunsMTEIDs.RADAR_HV,
            "gregguns.icbm.radar.hv",
            "Radar (HV)",
            3,
            tooltip("radar", "radar"),
            PLACEHOLDER_ICONS);
        new MTERadar(
            GregGunsMTEIDs.RADAR_EV,
            "gregguns.icbm.radar.ev",
            "Radar (EV)",
            4,
            tooltip("radar", "radar"),
            PLACEHOLDER_ICONS);
        new MTERadarTower(GregGunsMTEIDs.RADAR_TOWER, "gregguns.icbm.radartower", "Radar Tower");
        new MTEAirDefense(
            GregGunsMTEIDs.AIR_DEFENSE,
            "gregguns.icbm.airdefense",
            "Air-Defense Battery",
            tooltip("battery", "battery"),
            PLACEHOLDER_ICONS);
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
                GregGunsMTEIDs.RADAR_HV,
                25,
                600,
                TierEU.RECIPE_HV,
                ItemList.Hull_HV.get(1),
                ItemList.Sensor_HV.get(2),
                ItemList.Emitter_HV.get(1),
                circuits("Advanced", 2, Tier.HV));
            machine(
                GregGunsMTEIDs.RADAR_EV,
                26,
                600,
                TierEU.RECIPE_EV,
                ItemList.Hull_EV.get(1),
                ItemList.Sensor_EV.get(2),
                ItemList.Emitter_EV.get(1),
                circuits("Data", 2, Tier.EV));
            machine(
                GregGunsMTEIDs.RADAR_TOWER,
                27,
                1200,
                TierEU.RECIPE_IV,
                ItemList.Hull_IV.get(1),
                ItemList.Sensor_IV.get(2),
                ItemList.Emitter_IV.get(2),
                circuits("Elite", 2, Tier.IV));
            machine(
                GregGunsMTEIDs.AIR_DEFENSE,
                28,
                900,
                TierEU.RECIPE_HV,
                ItemList.Hull_HV.get(1),
                ItemList.Robot_Arm_HV.get(2),
                ItemList.Sensor_HV.get(1),
                circuits("Advanced", 2, Tier.HV));
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
                    .circuit(29)
                    .itemInputs(gun, emitter, circ, plate)
                    .itemOutputs(gunStack)
                    .duration(300)
                    .eut(TierEU.RECIPE_HV)
                    .addTo(RecipeMaps.assemblerRecipes);
            }
        } catch (RuntimeException e) {
            GregGuns.LOG.error("Defense recipes failed", e);
        }
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
            .circuit(30)
            .itemInputs(plate, motor, sensor, circ)
            .itemOutputs(out)
            .duration(300)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static void machine(int id, int circuit, int duration, long eut, ItemStack... inputs) {
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
        // The hull's casing comes back; electronics do not (D44).
        GTOreDictUnificator.addItemData(
            stack(id, 1),
            new ItemData(Materials.StainlessSteel, OrePrefixes.plate.getMaterialAmount() * 8));
    }
}
