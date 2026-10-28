package com.gregguns.icbm.complex.wall;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tier;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;
import com.gregguns.explosion.nuclear.FalloutTooltipFormat;
import com.gregguns.icbm.TooltipWrap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The item of {@link BlockComplexWall}: a block item with the D41 tooltip (tier, summary, details behind Shift). */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md; D205, D41
public final class ItemBlockComplexWall extends ItemBlock {

    /** Lang key of the summary line; {@code .1} is the detail behind Shift. */
    public static final String TIP = "gregguns.icbm.complex_wall.tip";

    /** @param block the wall block */
    public ItemBlockComplexWall(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        boolean shift = GuiScreen.isShiftKeyDown();
        List<String> details = new java.util.ArrayList<String>();
        details.add(StatCollector.translateToLocal(TIP + ".1"));
        TooltipWrap.addAll(
            tooltip,
            FalloutTooltipFormat.lines(
                String.format(
                    StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"),
                    TierText.colored(Tier.EV)),
                StatCollector.translateToLocal(TIP),
                details,
                StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift"),
                shift));
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
