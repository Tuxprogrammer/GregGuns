package com.gregguns.worldgen.loot;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gregguns.compat.GhostCircuits;
import com.gregguns.core.config.LootConfig;

import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;

/**
 * The scenario side of the loot tier cap (D363, D364): what is wrong, if anything, with a stack a world source handed
 * out. It asks {@link ObtainTier} and, so a wrong rule in that helper cannot hide itself, also checks a short list of
 * its own: GT's ghost circuits, the circuit ore names of IV and up, and materials no player has before LuV. Dev
 * environment only; server side.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-loot.md (Loot tier cap); D363, D364
public final class ObtainAudit {

    /** Circuit ore names of IV and up, written down here and not read from {@link ObtainTierRules}. */
    private static final Set<String> LATE_CIRCUITS = new HashSet<>(
        Arrays.asList(
            "circuitElite",
            "circuitMaster",
            "circuitUltimate",
            "circuitSuperconductor",
            "circuitInfinite",
            "circuitBio",
            "circuitOptical",
            "circuitExotic",
            "circuitCosmic",
            "circuitTranscendent",
            "circuitIV",
            "circuitLuV",
            "circuitZPM",
            "circuitUV",
            "circuitUHV",
            "circuitUEV",
            "circuitUIV",
            "circuitUMV",
            "circuitUXV",
            "circuitMAX"));

    /** GT materials no player has before LuV, written down here and not read from {@link ObtainTierRules}. */
    private static final Set<String> LATE_MATERIALS = new HashSet<>(
        Arrays.asList(
            "NaquadahAlloy",
            "Naquadria",
            "Trinium",
            "Tritanium",
            "Duranium",
            "Americium",
            "Neutronium",
            "CosmicNeutronium",
            "Infinity",
            "InfinityCatalyst",
            "WhiteDwarfMatter",
            "BlackDwarfMatter",
            "Universium",
            "SpaceTime",
            "TranscendentMetal",
            "MagnetohydrodynamicallyConstrainedStarMatter"));

    private ObtainAudit() {}

    /**
     * @param stack   a stack a world source handed out
     * @param ceiling the source's ceiling (voltage index)
     * @return null when the stack may be there under the current {@code loot.maxTier}, else why not
     */
    public static @Nullable String problem(ItemStack stack, int ceiling) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        int limit = Math.min(ceiling, LootConfig.maxTier);
        String name = describe(stack);
        String flag = redFlag(stack, limit);
        if (flag != null) {
            return name + " " + flag;
        }
        int t = ObtainTier.of(stack);
        if (t == ObtainTierRules.UNKNOWN) {
            return name + " has no known obtain tier";
        }
        if (t > limit) {
            return name + " needs " + ObtainTierRules.name(t) + ", above " + ObtainTierRules.name(limit);
        }
        return null;
    }

    /**
     * The independent list: a ghost circuit anywhere, an IV-and-up circuit under an IV limit, a LuV-and-up material
     * under a LuV limit.
     *
     * @param stack a stack
     * @param limit the lower of the source's ceiling and the cap
     * @return what is wrong, or null
     */
    public static @Nullable String redFlag(ItemStack stack, int limit) {
        if (isGhost(stack)) {
            return "is a ghost circuit";
        }
        if (limit < 5) {
            for (int id : OreDictionary.getOreIDs(stack)) {
                String ore = OreDictionary.getOreName(id);
                if (LATE_CIRCUITS.contains(ore)) {
                    return "is an IV-or-later circuit (" + ore + ")";
                }
            }
        }
        if (limit < 6) {
            // Source: getAssociation answers the prefix and material GT unified the stack under:
            // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L504
            ItemData d = GTOreDictUnificator.getAssociation(stack);
            if (d != null && d.mMaterial != null
                && d.mMaterial.mMaterial != null
                && LATE_MATERIALS.contains(d.mMaterial.mMaterial.mName)) {
                return "is made of " + d.mMaterial.mMaterial.mName + ", a LuV-or-later material";
            }
        }
        return null;
    }

    private static boolean isGhost(ItemStack stack) {
        return GhostCircuits.isGhost(stack);
    }

    /**
     * @param stack a stack
     * @return its registry name and damage, for messages
     */
    public static String describe(ItemStack stack) {
        Object reg = net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem());
        return reg + "@" + stack.getItemDamage();
    }
}
