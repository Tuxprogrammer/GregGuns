package com.gregguns.utility;

import com.gregguns.api.Tier;

import gregtech.api.enums.TierEU;

/**
 * The four turret armor plates as plain data: registry name, armor value, tier and recipe. Both the plate recipes and
 * the Bastion Plating recipes of the Rampart read it, so the sheet's numbers live in one place. No Minecraft types.
 * Both sides.
 */
// Spec: docs/gregguns-spec/09-machines-utility/recipes.md (Turret armor plates, GGS-09-29, GGS-09-30, GGS-09-35)
public enum TurretPlate {

    /**
     * Iron Turret Armor (5): five iron plates in the crafting table, a Steam item. Deviation: the sheet says
     * {@code plate.WroughtIron}, but in GT 5.09.54.205 {@code Materials.WroughtIron} is an alias of Cast Iron
     * ({@code plateWroughtIron} has no items), so the recipe takes {@code plate.CastIron}, as the Miner armor does.
     */
    IRON("turret_armor_iron", 5, Tier.STEAM, "CastIron", 5, null, 0, 0L, 0, 0),
    /** Steel Turret Armor (10): LV Assembler. */
    STEEL("turret_armor_steel", 10, Tier.LV, "Steel", 5, null, 0, TierEU.RECIPE_LV, 200, 14),
    /** Aluminum Turret Armor (15): MV Assembler. */
    ALUMINUM("turret_armor_aluminum", 15, Tier.MV, "Aluminium", 5, null, 0, TierEU.RECIPE_MV, 200, 15),
    /** Carbon Turret Armor (20): EV Assembler, fiber-reinforced epoxy plus the EV gate material. */
    CARBON("turret_armor_carbon", 20, Tier.EV, "EpoxidFiberReinforced", 3, "Titanium", 2, TierEU.RECIPE_EV, 200, 12);

    private final String registryName;
    private final int armorValue;
    private final Tier tier;
    private final String mainMaterial;
    private final int mainCount;
    private final String extraMaterial;
    private final int extraCount;
    private final long recipeEut;
    private final int durationTicks;
    private final int circuit;

    TurretPlate(String registryName, int armorValue, Tier tier, String mainMaterial, int mainCount,
        String extraMaterial, int extraCount, long recipeEut, int durationTicks, int circuit) {
        this.registryName = registryName;
        this.armorValue = armorValue;
        this.tier = tier;
        this.mainMaterial = mainMaterial;
        this.mainCount = mainCount;
        this.extraMaterial = extraMaterial;
        this.extraCount = extraCount;
        this.recipeEut = recipeEut;
        this.durationTicks = durationTicks;
        this.circuit = circuit;
    }

    /** @return the item registry name, for example {@code turret_armor_iron} */
    public String registryName() {
        return registryName;
    }

    /** @return the armor value the plate gives a Turret: 5, 10, 15 or 20 */
    public int armorValue() {
        return armorValue;
    }

    /** @return the tier the plate belongs to; it is also the lowest machine tier that can make it */
    public Tier tier() {
        return tier;
    }

    /** @return the GT material of the main plate input ({@code plate.<material>}) */
    public String mainMaterial() {
        return mainMaterial;
    }

    /** @return how many main plates the recipe takes */
    public int mainCount() {
        return mainCount;
    }

    /** @return the GT material of the second plate input, or null when there is none */
    public String extraMaterial() {
        return extraMaterial;
    }

    /** @return how many second plates the recipe takes; 0 when there is none */
    public int extraCount() {
        return extraCount;
    }

    /** @return true for the one plate made by hand in the crafting table (GGS-09-29) */
    public boolean handMade() {
        return recipeEut == 0L;
    }

    /** @return the Assembler's EU/t ({@code TierEU.RECIPE_<tier>}); 0 for the hand-made plate */
    public long recipeEut() {
        return recipeEut;
    }

    /** @return the Assembler's duration in ticks; 0 for the hand-made plate */
    public int durationTicks() {
        return durationTicks;
    }

    /**
     * @return the programmed circuit of the Assembler recipe; 0 for the hand-made plate. Steel, Aluminum and Carbon use
     *         14, 15 and 12: with no circuit the recipe would also run on the inputs of about 25 pack Assembler recipes
     *         that hold the same plates, and the Carbon plate's inputs were a subset of other GregGuns armor rows.
     *         A dump of GTNH 2.9.0-RC-2 shows no Assembler recipe on these circuits whose inputs match.
     */
    public int circuit() {
        return circuit;
    }

    /**
     * @param registryName an item registry name
     * @return the plate, or null if the name is not a turret armor plate
     */
    public static TurretPlate byRegistryName(String registryName) {
        for (TurretPlate p : values()) {
            if (p.registryName.equals(registryName)) {
                return p;
            }
        }
        return null;
    }
}
