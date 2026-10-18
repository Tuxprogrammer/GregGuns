package com.gregguns.armor;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gregguns.GregGuns;
import com.gregguns.armor.logic.RepairSpec;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

/**
 * Matches repair items to a {@link RepairSpec} on GT terms: the metal part is a GT ore prefix and material compared
 * with the unificator, so any mod's plate of that material counts (GGS-09-20, GGS-09-21). Common side.
 */
// Spec: docs/gregguns-spec/09-machines-utility/repair-bench.md (GGS-09-20, GGS-09-21)
public final class ArmorRepair {

    private ArmorRepair() {}

    /**
     * @param spec      the piece's repair recipe; null means the piece cannot be repaired this way
     * @param candidate the stack offered as a repair item
     * @return true when the stack is the spec's cloth part or its metal part
     */
    public static boolean isRepairItem(@Nullable RepairSpec spec, @Nullable ItemStack candidate) {
        if (spec == null || candidate == null) {
            return false;
        }
        Item cloth = GameRegistry.findItem(GregGuns.MODID, spec.clothItem());
        if (cloth != null && candidate.getItem() == cloth) {
            return true;
        }
        ItemStack metal = metalStack(spec);
        // Source:
        // https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L284
        return metal != null && GTOreDictUnificator.isInputStackEqual(candidate, metal);
    }

    /**
     * @param spec a repair recipe
     * @return one unified metal part, or null when the spec has no metal or GT has no such part
     */
    public static @Nullable ItemStack metalStack(RepairSpec spec) {
        if (spec.metalPrefix() == null) {
            return null;
        }
        return GTOreDictUnificator
            .get(OrePrefixes.getPrefix(spec.metalPrefix()), Materials.get(spec.metalMaterial()), 1);
    }
}
