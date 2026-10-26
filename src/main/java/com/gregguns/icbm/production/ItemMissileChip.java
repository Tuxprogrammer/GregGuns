package com.gregguns.icbm.production;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;
import com.gregguns.icbm.recipe.PartTooltipFormat;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The Missile Chip of one body tier: a plain item with a tier and a tooltip, registered once per tier as
 * {@code missile_chip_t1} and {@code missile_chip_t2}. The Laser Engraver turns a chip into the matching schematic.
 * Deliberately not a {@code GregGunsComponent}, like the ICBM parts, because its recipe only exists for an enabled
 * module. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/03-missiles/build-and-recipes.md (Chip and schematic)
public final class ItemMissileChip extends Item implements Tiered {

    /**
     * @param spec a body tier
     * @return the bare registry name of that tier's item
     */
    public static String name(MissileBuildSpec spec) {
        return "missile_chip_t" + spec.number();
    }

    private final MissileBuildSpec spec;

    /** @param spec which body tier this chip is for */
    public ItemMissileChip(MissileBuildSpec spec) {
        this.spec = spec;
        setMaxStackSize(64);
    }

    @Override
    public Tier getTier() {
        return spec.tier();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        String tierLine = String
            .format(StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"), TierText.colored(spec.tier()));
        String summary = StatCollector.translateToLocal("gregguns.icbm.chip.summary");
        String usedIn = String.format(
            StatCollector.translateToLocal("gregguns.icbm.part.tooltip.used"),
            StatCollector.translateToLocal("gregguns.icbm.chip.used"));
        String hint = StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift");
        tooltip.addAll(PartTooltipFormat.lines(tierLine, summary, usedIn, hint, shift));
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
