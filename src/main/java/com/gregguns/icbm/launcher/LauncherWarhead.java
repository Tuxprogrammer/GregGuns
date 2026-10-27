package com.gregguns.icbm.launcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gregguns.api.Tier;
import com.gregguns.icbm.recipe.IcbmPart;
import com.gregguns.icbm.recipe.Ing;

/**
 * The launcher warheads (D12, D150): ICBM blast types a GregGuns launcher can carry, as plain data. Each is one item of
 * {@value LauncherRules#CHARGES_PER_ITEM} charges that is crafted onto a launcher; every shot then spends one charge
 * and replaces the rocket's or grenade's own blast with the ICBM blast at fill 1. Also holds the one Assembler recipe
 * of each (D69: HV or above, D63 reading: a charge costs about half a rocket's plate equivalents more than a plain
 * rocket). No Minecraft types, both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/launcher-warheads.md
public enum LauncherWarhead {

    /** High explosive for the rocket launchers. */
    HE("he", "condensed", LauncherKind.ROCKET, Tier.HV, 1, Ing.gt("plate", "StainlessSteel", 2, Tier.HV, 1.0),
        Ing.tnt(4), Ing.part(IcbmPart.PLASTIC_EXPLOSIVE, 2), Ing.part(IcbmPart.FUZE_ASSEMBLY_MK1, 1)),
    /** A dense lead slug: heavy, slows what it hits. */
    HEAT("heat", "anvil", LauncherKind.ROCKET, Tier.HV, 2, Ing.gt("plate", "StainlessSteel", 2, Tier.HV, 1.0),
        Ing.tnt(3), Ing.part(IcbmPart.PLASTIC_EXPLOSIVE, 2), Ing.gt("plate", "Lead", 6, Tier.LV, 0.5),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK1, 1)),
    /** Napalm for the rocket launchers. */
    INCENDIARY("incendiary", "incendiary", LauncherKind.ROCKET, Tier.HV, 3,
        Ing.gt("plate", "StainlessSteel", 2, Tier.HV, 1.0), Ing.part(IcbmPart.NAPALM_CANISTER, 1),
        Ing.part(IcbmPart.PLASTIC_EXPLOSIVE, 2), Ing.part(IcbmPart.FUZE_ASSEMBLY_MK1, 1)),
    /** Steel fragments that set everything alight. */
    PHOSPHORUS("phosphorus", "shrapnel", LauncherKind.ROCKET, Tier.HV, 4,
        Ing.gt("plate", "StainlessSteel", 2, Tier.HV, 1.0), Ing.tnt(3), Ing.part(IcbmPart.PLASTIC_EXPLOSIVE, 1),
        Ing.part(IcbmPart.FRAGMENTATION_SLEEVE_STEEL, 1), Ing.part(IcbmPart.FUZE_ASSEMBLY_MK1, 1)),
    /** A shaped charge that bores down (EV). */
    DEMOLITION("demolition", "breaching", LauncherKind.ROCKET, Tier.EV, 1, Ing.gt("plate", "Titanium", 2, Tier.EV, 1.0),
        Ing.industrialTnt(2), Ing.part(IcbmPart.PLASTIC_EXPLOSIVE, 2), Ing.part(IcbmPart.SHAPED_CHARGE_LINER_CONE, 1),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK2, 1)),
    /** The EMP pulse for the Nuke Launcher (LuV). */
    EMP("emp", "emp", LauncherKind.NUKE, Tier.LUV, 1, Ing.gt("plate", "TungstenSteel", 2, Tier.LUV, 1.5),
        Ing.industrialTnt(1), Ing.part(IcbmPart.PLASTIC_EXPLOSIVE, 1), Ing.gtItem("Emitter_LuV", 1, Tier.LUV, 10.0),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK3, 1)),
    /** High explosive for the Grenade Launcher. */
    GRENADE_HE("grenade_he", "condensed", LauncherKind.GRENADE, Tier.HV, 5,
        Ing.gt("plate", "StainlessSteel", 1, Tier.HV, 1.0), Ing.tnt(2), Ing.part(IcbmPart.PLASTIC_EXPLOSIVE, 1),
        Ing.part(IcbmPart.FUZE_ASSEMBLY_MK1, 1)),
    /** A fragmentation grenade charge for the Grenade Launcher (EV). */
    GRENADE_CLUSTER("grenade_cluster", "fragmentation", LauncherKind.GRENADE, Tier.EV, 2,
        Ing.gt("plate", "Titanium", 1, Tier.EV, 1.0), Ing.industrialTnt(2), Ing.part(IcbmPart.PLASTIC_EXPLOSIVE, 2),
        Ing.part(IcbmPart.FRAGMENTATION_SLEEVE_TITANIUM, 1), Ing.part(IcbmPart.FUZE_ASSEMBLY_MK2, 1));

    private final String key;
    private final String blastId;
    private final LauncherKind kind;
    private final Tier tier;
    private final int circuit;
    private final List<Ing> inputs;

    LauncherWarhead(String key, String blastId, LauncherKind kind, Tier tier, int circuit, Ing... inputs) {
        this.key = key;
        this.blastId = blastId;
        this.kind = kind;
        this.tier = tier;
        this.circuit = circuit;
        this.inputs = Collections.unmodifiableList(Arrays.asList(inputs));
    }

    /** @return the item name suffix: the registry name is {@code launcher_warhead_<key>} and so is the texture */
    public String key() {
        return key;
    }

    /** @return the registry name of the item */
    public String itemName() {
        return "launcher_warhead_" + key;
    }

    /** @return the id of the ICBM blast type it detonates */
    public String blastId() {
        return blastId;
    }

    /** @return which launchers carry it */
    public LauncherKind kind() {
        return kind;
    }

    /** @return the Assembler's tier, which is also the item's tier */
    public Tier tier() {
        return tier;
    }

    /** @return the programmed circuit that keeps the recipe unique */
    public int circuit() {
        return circuit;
    }

    /** @return the ingredients; the metal first */
    public List<Ing> inputs() {
        return inputs;
    }

    /**
     * @param key a key as stored in a gun's NBT
     * @return the warhead, or null when unknown (a hand-edited stack)
     */
    public static LauncherWarhead byKey(String key) {
        for (LauncherWarhead w : values()) {
            if (w.key.equals(key)) {
                return w;
            }
        }
        return null;
    }
}
