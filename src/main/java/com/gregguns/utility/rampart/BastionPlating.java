package com.gregguns.utility.rampart;

import com.gregguns.api.Tier;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.utility.TurretPlate;

import gregtech.api.enums.TierEU;

/**
 * The four Bastion Plating metas of {@code gregguns:casings} as plain data: the meta, the turret armor plate each is
 * made from, and the Assembler recipe. The armor value of a plating is the value of its plate (5, 10, 15, 20), and a
 * Rampart takes the value of the lowest meta it contains (GGS-08-78). No Minecraft types. Both sides.
 *
 * <p>
 * Usage: {@code BastionPlating.lowestArmor(lowestMeta)} is the armor value of every mount.
 */
// Spec: docs/gregguns-spec/09-machines-utility/recipes.md (Bastion Plating, GGS-09-35),
// docs/gregguns-spec/08-machines-production/multiblocks.md (GGS-08-78)
public enum BastionPlating {

    /** Iron: armor 5; the Assembler recipe runs at LV although the plate is a Steam item. */
    IRON(GregGunsCasings.BASTION_IRON, TurretPlate.IRON, 1, Tier.LV, TierEU.RECIPE_LV),
    /** Steel: armor 10, LV. */
    STEEL(GregGunsCasings.BASTION_STEEL, TurretPlate.STEEL, 2, Tier.LV, TierEU.RECIPE_LV),
    /** Aluminum: armor 15, MV. */
    ALUMINUM(GregGunsCasings.BASTION_ALUMINUM, TurretPlate.ALUMINUM, 3, Tier.MV, TierEU.RECIPE_MV),
    /** Carbon: armor 20, EV. */
    CARBON(GregGunsCasings.BASTION_CARBON, TurretPlate.CARBON, 4, Tier.EV, TierEU.RECIPE_EV);

    /** Blocks one Assembler run makes from one plate. */
    public static final int YIELD = 4;
    /** Concrete blocks one run takes. */
    public static final int CONCRETE = 2;
    /** Duration of the run in ticks (10 s). */
    public static final int DURATION_TICKS = 200;

    private final int meta;
    private final TurretPlate plate;
    private final int circuit;
    private final Tier recipeTier;
    private final long recipeEut;

    BastionPlating(int meta, TurretPlate plate, int circuit, Tier recipeTier, long recipeEut) {
        this.meta = meta;
        this.plate = plate;
        this.circuit = circuit;
        this.recipeTier = recipeTier;
        this.recipeEut = recipeEut;
    }

    /** @return the meta in {@code gregguns:casings} */
    public int meta() {
        return meta;
    }

    /** @return the turret armor plate the recipe consumes */
    public TurretPlate plate() {
        return plate;
    }

    /** @return the armor value this plating gives a mount: 5, 10, 15 or 20 */
    public int armorValue() {
        return plate.armorValue();
    }

    /** @return the programmed circuit of the Assembler recipe */
    public int circuit() {
        return circuit;
    }

    /** @return the lowest machine tier that can run the recipe */
    public Tier recipeTier() {
        return recipeTier;
    }

    /** @return the Assembler's EU/t ({@code TierEU.RECIPE_<tier>}) */
    public long recipeEut() {
        return recipeEut;
    }

    /**
     * @param meta a casing meta
     * @return the plating with that meta, or null when the meta is not a Bastion Plating
     */
    public static BastionPlating byMeta(int meta) {
        for (BastionPlating p : values()) {
            if (p.meta == meta) {
                return p;
            }
        }
        return null;
    }

    /**
     * @param metas the casing metas of every plating block a structure check passed
     * @return the lowest Bastion Plating among them, or null when none is a plating
     */
    public static BastionPlating lowest(int... metas) {
        BastionPlating found = null;
        for (int meta : metas) {
            BastionPlating p = byMeta(meta);
            if (p != null && (found == null || p.meta < found.meta)) {
                found = p;
            }
        }
        return found;
    }

    /**
     * @param lowestMeta the lowest plating meta a structure contains, or any non-plating value for none
     * @return the armor value of every mount: that plating's value, 0 when there is none
     */
    public static int lowestArmor(int lowestMeta) {
        BastionPlating p = byMeta(lowestMeta);
        return p == null ? 0 : p.armorValue();
    }
}
